package com.focal.app.firebase

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Base64
import com.example.myapplication.receiver.PackageChangeNotifier
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.FocalRepository
import com.focal.app.model.InstalledApp
import com.focal.app.model.UserRole
import com.focal.app.protection.ProtectionEngine
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

object ChildAppScanner {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Connect package change notifier
        PackageChangeNotifier.onPackageAdded = { context, packageName ->
            onPackageAdded(context, packageName)
        }
        PackageChangeNotifier.onPackageRemoved = { context, packageName ->
            onPackageRemoved(context, packageName)
        }
    }

    fun init(context: Context) {
        // Ensure listeners are wired
        PackageChangeNotifier.onPackageAdded = { ctx, pkg ->
            onPackageAdded(ctx, pkg)
        }
        PackageChangeNotifier.onPackageRemoved = { ctx, pkg ->
            onPackageRemoved(ctx, pkg)
        }
    }

    fun syncApps(context: Context, targetChildId: String? = null) {
        val childId = resolveChildId(targetChildId)
        if (childId.isBlank()) return

        scope.launch {
            try {
                val scannedApps = scanLaunchableApps(context, childId)
                if (scannedApps.isEmpty()) return@launch

                // Chunk in batches of 300 to stay well under Firestore's 500 limit
                val chunks = scannedApps.chunked(300)
                for (chunk in chunks) {
                    val batch = firestore.batch()
                    for (app in chunk) {
                        val docRef = firestore.collection("children").document(childId)
                            .collection("apps").document(app.packageName)
                        val docData = hashMapOf(
                            "packageName" to app.packageName,
                            "label" to app.appName,
                            "iconPng" to app.iconPng,
                            "category" to app.category,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        batch.set(docRef, docData)
                    }
                    batch.commit()
                }

                // Also update in-memory repository if child
                FocalRepository.instance.syncChildInstalledApps(scannedApps)
            } catch (_: Throwable) {}
        }
    }

    fun onPackageAdded(context: Context, packageName: String, targetChildId: String? = null) {
        val childId = resolveChildId(targetChildId)
        if (childId.isBlank()) return

        scope.launch {
            try {
                val pm = context.packageManager
                if (isExcludedPackage(context, pm, packageName)) return@launch

                val appInfo = try {
                    pm.getApplicationInfo(packageName, 0)
                } catch (_: Exception) {
                    return@launch
                }

                val label = appInfo.loadLabel(pm).toString()
                val iconPng = getAppIconBase64Png(pm, appInfo)
                val category = getAppCategory(pm, appInfo, packageName)

                val docRef = firestore.collection("children").document(childId)
                    .collection("apps").document(packageName)

                val docData = hashMapOf(
                    "packageName" to packageName,
                    "label" to label,
                    "iconPng" to iconPng,
                    "category" to category,
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(docData)
            } catch (_: Throwable) {}
        }
    }

    fun onPackageRemoved(context: Context, packageName: String, targetChildId: String? = null) {
        val childId = resolveChildId(targetChildId)
        if (childId.isBlank()) return

        scope.launch {
            try {
                firestore.collection("children").document(childId)
                    .collection("apps").document(packageName).delete()
            } catch (_: Throwable) {}
        }
    }

    fun scanLaunchableApps(context: Context, childId: String): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        val excluded = buildExcludedPackages(context, pm)

        return resolveInfos.mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName
            if (pkg in excluded || isExcludedPackage(context, pm, pkg)) {
                null
            } else {
                val label = ri.loadLabel(pm).toString().trim().ifBlank { pkg }
                val appInfo = ri.activityInfo.applicationInfo
                val iconPng = getAppIconBase64Png(pm, appInfo)
                val cat = getAppCategory(pm, appInfo, pkg)
                InstalledApp(
                    packageName = pkg,
                    appName = label,
                    category = cat,
                    iconPng = iconPng,
                    childId = childId,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    private fun resolveChildId(providedId: String?): String {
        if (!providedId.isNullOrBlank()) return providedId
        val saved = FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "")
        if (saved.isNotBlank()) return saved
        val repo = FocalRepository.instance
        if (repo.currentUser.role == UserRole.CHILD && repo.currentUser.id.isNotBlank()) {
            return repo.currentUser.id
        }
        return ""
    }

    private fun buildExcludedPackages(context: Context, pm: PackageManager): Set<String> {
        val excluded = mutableSetOf<String>()

        // 1. FOCAL itself
        excluded.add(context.packageName)
        excluded.add("com.focal.app")
        excluded.add("com.rohan.focalapplication")
        excluded.add("com.example.myapplication")

        // 2. Dialer / Phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val telecom = context.getSystemService(TelecomManager::class.java)
                telecom?.defaultDialerPackage?.let { excluded.add(it) }
            } catch (_: Exception) {}
        }
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            val dialApps = pm.queryIntentActivities(dialIntent, 0)
            dialApps.forEach { excluded.add(it.activityInfo.packageName) }
        } catch (_: Exception) {}
        excluded.addAll(ProtectionEngine.DIALER_AND_CALL_PACKAGES)

        // 3. SMS
        try {
            val defaultSms = Telephony.Sms.getDefaultSmsPackage(context)
            if (!defaultSms.isNullOrBlank()) excluded.add(defaultSms)
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
            val smsApps = pm.queryIntentActivities(smsIntent, 0)
            smsApps.forEach { excluded.add(it.activityInfo.packageName) }
        } catch (_: Exception) {}

        // 4. Settings
        try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            val settingsApps = pm.queryIntentActivities(settingsIntent, 0)
            settingsApps.forEach { excluded.add(it.activityInfo.packageName) }
        } catch (_: Exception) {}
        excluded.addAll(ProtectionEngine.SETTINGS_PACKAGES)

        // 5. Home Launchers
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val homeApps = pm.queryIntentActivities(homeIntent, 0)
            homeApps.forEach { excluded.add(it.activityInfo.packageName) }
        } catch (_: Exception) {}
        excluded.addAll(ProtectionEngine.LAUNCHER_PACKAGES)

        return excluded
    }

    private fun isExcludedPackage(context: Context, pm: PackageManager, packageName: String): Boolean {
        val lower = packageName.lowercase()
        if (lower == context.packageName.lowercase() ||
            lower.contains("com.focal.app") ||
            lower.contains("com.rohan.focalapplication") ||
            lower.contains("myapplication")
        ) {
            return true
        }

        if (ProtectionEngine.DIALER_AND_CALL_PACKAGES.contains(lower) ||
            ProtectionEngine.SETTINGS_PACKAGES.contains(lower) ||
            ProtectionEngine.LAUNCHER_PACKAGES.contains(lower) ||
            ProtectionEngine.EMERGENCY_PACKAGES.contains(lower)
        ) {
            return true
        }

        if (lower.contains("incallui") || lower.contains("telecom") || lower.contains("settings") || lower.contains("dialer")) {
            return true
        }

        return false
    }

    fun getAppIconBase64Png(pm: PackageManager, appInfo: ApplicationInfo): String {
        return try {
            val drawable: Drawable = appInfo.loadIcon(pm)
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null &&
                drawable.bitmap.width == 48 && drawable.bitmap.height == 48
            ) {
                drawable.bitmap
            } else {
                val bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, 48, 48)
                drawable.draw(canvas)
                bmp
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            var bytes = outputStream.toByteArray()

            // Ensure strictly < 8 KB (8192 bytes)
            if (bytes.size > 7000) {
                val altStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, altStream)
                bytes = altStream.toByteArray()
            }

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            ""
        }
    }

    private fun getAppCategory(pm: PackageManager, appInfo: ApplicationInfo, packageName: String): String {
        val lowerPkg = packageName.lowercase()
        return when {
            lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("snapchat") ||
                    lowerPkg.contains("tiktok") || lowerPkg.contains("reddit") || lowerPkg.contains("twitter") ||
                    lowerPkg.contains("x.android") || lowerPkg.contains("threads") -> "Social"

            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("primevideo") ||
                    lowerPkg.contains("twitch") || lowerPkg.contains("hulu") || lowerPkg.contains("disney") ||
                    lowerPkg.contains("hotstar") || lowerPkg.contains("spotify") -> "Video"

            lowerPkg.contains("game") || lowerPkg.contains("pubg") || lowerPkg.contains("roblox") ||
                    lowerPkg.contains("clash") || lowerPkg.contains("chess") || lowerPkg.contains("minecraft") ||
                    lowerPkg.contains("subway") || lowerPkg.contains("candy") -> "Games"

            lowerPkg.contains("amazon") || lowerPkg.contains("flipkart") || lowerPkg.contains("ebay") ||
                    lowerPkg.contains("myntra") || lowerPkg.contains("meesho") -> "Shopping"

            lowerPkg.contains("chrome") || lowerPkg.contains("browser") || lowerPkg.contains("firefox") ||
                    lowerPkg.contains("opera") || lowerPkg.contains("edge") -> "Browsers"

            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (appInfo.category) {
                        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> "Video"
                        ApplicationInfo.CATEGORY_GAME -> "Games"
                        else -> "Other"
                    }
                } else {
                    "Other"
                }
            }
        }
    }
}
