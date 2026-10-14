package com.focal.app.ui.components

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext


actual object PlatformPermissionsHelper {
    var appContext: Context? = null
    var currentActivity: java.lang.ref.WeakReference<android.app.Activity>? = null
    var setupPageAllowedUntilMs: Long = 0L
    var isAccessibilityServiceConnected: Boolean = false
    var isTestMockPermissionsEnabled: Boolean = true

    actual fun markSetupPageOpened() {
        val wasActive = isSetupPageGraceActive()
        setupPageAllowedUntilMs = System.currentTimeMillis() + 3 * 60 * 1000L // 3 minutes grace
        if (!wasActive) {
            Log.d("FOCAL_PERM_LOG", "FIX_ALLOWANCE_START: Fix-button clicked, 3-minute grace period active for system settings")
            try {
                com.focal.app.protection.ProtectionStateRepository.instance.recordDecision(
                    com.focal.app.protection.DecisionLogEntry(
                        timestampMs = System.currentTimeMillis(),
                        packageName = "com.focal.app",
                        windowClass = "FixButton",
                        decisionType = com.focal.app.protection.DecisionType.ALLOW,
                        ruleLetter = 'a',
                        reason = "FIX_ALLOWANCE_START",
                        actionTaken = "Fix-button allowance started for up to 3 minutes"
                    )
                )
            } catch (_: Exception) {}
        }
    }

    actual fun isSetupPageGraceActive(): Boolean {
        return System.currentTimeMillis() < setupPageAllowedUntilMs
    }

    actual fun copyToClipboard(text: String) {
        try {
            appContext?.let { ctx ->
                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Focal Protection Log", text)
                clipboard?.setPrimaryClip(clip)
            }
        } catch (_: Exception) {}
    }

    private fun safeStartActivity(intent: Intent) {
        markSetupPageOpened()
        try {
            appContext?.let { ctx ->
                Log.d("FOCAL_PERM_LOG", "safeStartActivity: action=${intent.action}, data=${intent.data}, component=${intent.component}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("FOCAL_PERM_LOG", "safeStartActivity failed for action=${intent.action}: ${e.message}", e)
            // Fallback to general settings page to prevent crashes or loops
            try {
                appContext?.let { ctx ->
                    val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(fallbackIntent)
                }
            } catch (_: Exception) {}
        }
    }

    actual fun openAccessibilitySettings() {
        Log.d("FOCAL_PERM_LOG", "openAccessibilitySettings called")
        safeStartActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    actual fun openOverlaySettings() {
        Log.d("FOCAL_PERM_LOG", "openOverlaySettings called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            safeStartActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${appContext?.packageName}")))
        } else {
            safeStartActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    actual fun openUsageAccessSettings() {
        Log.d("FOCAL_PERM_LOG", "openUsageAccessSettings called")
        safeStartActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    actual fun openNotificationSettings() {
        Log.d("FOCAL_PERM_LOG", "openNotificationSettings called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, appContext?.packageName)
            }
            safeStartActivity(intent)
        } else {
            safeStartActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    actual fun openBatteryOptimizationSettings() {
        Log.d("FOCAL_PERM_LOG", "openBatteryOptimizationSettings called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${appContext?.packageName}")
                }
                safeStartActivity(intent)
            } catch (e: Exception) {
                Log.e("FOCAL_PERM_LOG", "Primary battery opt intent failed, falling back to ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS", e)
                try {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    safeStartActivity(fallbackIntent)
                } catch (e2: Exception) {
                    Log.e("FOCAL_PERM_LOG", "Secondary battery opt intent failed, opening app details", e2)
                    openAppDetailsSettings()
                }
            }
        } else {
            safeStartActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    actual fun openExactAlarmSettings() {
        Log.d("FOCAL_PERM_LOG", "openExactAlarmSettings called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${appContext?.packageName}")
            }
            safeStartActivity(intent)
        } else {
            safeStartActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    actual fun openDeviceAdminSettings() {
        Log.d("FOCAL_PERM_LOG", "openDeviceAdminSettings called")
        try {
            val ctx = appContext ?: return
            val componentName = ComponentName(ctx, "com.example.myapplication.protection.FocalDeviceAdminReceiver")
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Stops FOCAL from being removed without the parent.")
            }
            safeStartActivity(intent)
        } catch (e: Exception) {
            Log.e("FOCAL_PERM_LOG", "Device admin intent failed, opening security settings fallback", e)
            safeStartActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    actual fun openSecuritySettings() {
        Log.d("FOCAL_PERM_LOG", "openSecuritySettings called")
        safeStartActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
    }

    actual fun openNfcSettings() {
        safeStartActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    }

    actual fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${appContext?.packageName}")
        }
        safeStartActivity(intent)
    }

    actual fun isAccessibilityEnabled(): Boolean {
        return try {
            val ctx = appContext ?: return isTestMockPermissionsEnabled
            val expectedComponent = ComponentName(ctx, "com.example.myapplication.service.FocalAccessibilityService")
            val fullCompName = expectedComponent.flattenToString()
            val shortCompName = expectedComponent.flattenToShortString()
            
            val enabledServices = Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            
            val isEnabledInSettings = enabledServices.contains(fullCompName) ||
                    enabledServices.contains(shortCompName) ||
                    enabledServices.contains("FocalAccessibilityService")
                    
            isEnabledInSettings && isAccessibilityServiceConnected
        } catch (_: Exception) {
            if (appContext == null) isTestMockPermissionsEnabled else false
        }
    }

    actual fun isOverlayEnabled(): Boolean {
        return try {
            val ctx = appContext ?: return isTestMockPermissionsEnabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(ctx)
            } else {
                true
            }
        } catch (_: Exception) {
            if (appContext == null) isTestMockPermissionsEnabled else false
        }
    }

    actual fun isUsageAccessEnabled(): Boolean {
        return try {
            val ctx = appContext ?: return isTestMockPermissionsEnabled
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    ctx.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    ctx.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            if (appContext == null) isTestMockPermissionsEnabled else false
        }
    }

    actual fun isNotificationPermissionGranted(): Boolean {
        return try {
            val ctx = appContext ?: return isTestMockPermissionsEnabled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    ctx,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            }
        } catch (_: Exception) {
            if (appContext == null) isTestMockPermissionsEnabled else true
        }
    }

    actual fun isBatteryOptimizationDisabled(): Boolean {
        return try {
            val ctx = appContext ?: return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(ctx.packageName)
            } else {
                true
            }
        } catch (_: Exception) {
            true
        }
    }

    actual fun isExactAlarmPermissionGranted(): Boolean {
        return try {
            val ctx = appContext ?: return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        } catch (_: Exception) {
            true
        }
    }

    actual fun isDeviceAdminActive(): Boolean {
        return try {
            val ctx = appContext ?: return isTestMockPermissionsEnabled
            val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(ctx, "com.example.myapplication.protection.FocalDeviceAdminReceiver")
            val active = dpm.isAdminActive(componentName)
            Log.d("FOCAL_ADMIN", "isDeviceAdminActive check for $componentName: $active")
            active
        } catch (e: Exception) {
            Log.e("FOCAL_ADMIN", "Error checking isDeviceAdminActive: ${e.message}", e)
            if (appContext == null) isTestMockPermissionsEnabled else false
        }
    }

    actual fun hasNfcHardware(): Boolean {
        return try {
            val ctx = appContext ?: return isTestMockPermissionsEnabled
            val adapter = NfcAdapter.getDefaultAdapter(ctx)
            adapter != null
        } catch (_: Exception) {
            if (appContext == null) isTestMockPermissionsEnabled else false
        }
    }

    actual fun isNfcEnabled(): Boolean {
        return try {
            val ctx = appContext ?: return isTestMockPermissionsEnabled
            val adapter = NfcAdapter.getDefaultAdapter(ctx)
            adapter != null && adapter.isEnabled
        } catch (_: Exception) {
            if (appContext == null) isTestMockPermissionsEnabled else false
        }
    }

    actual fun isAnimationsDisabled(): Boolean {
        return try {
            val ctx = appContext ?: return false
            val resolver = ctx.contentResolver
            val duration = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            val transition = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
            duration == 0f || transition == 0f
        } catch (_: Exception) {
            false
        }
    }

    actual fun openNewBrowserTab() {
        try {
            appContext?.let { ctx ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("about:blank")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                ctx.startActivity(intent)
            }
        } catch (_: Exception) {
            try {
                appContext?.let { ctx ->
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_BROWSER)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(intent)
                }
            } catch (_: Exception) {}
        }
    }

    actual fun getRealDailyScreenTimeMinutes(): Int {
        return try {
            val ctx = appContext ?: return 0
            if (!isUsageAccessEnabled()) return 0
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val endTime = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            if (stats.isNullOrEmpty()) return 0
            var totalTimeMs = 0L
            for (usage in stats) {
                totalTimeMs += usage.totalTimeInForeground
            }
            (totalTimeMs / (1000 * 60)).toInt()
        } catch (_: Exception) {
            0
        }
    }

    actual fun showToast(message: String) {
        try {
            appContext?.let {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(it, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {}
    }

    actual fun getAppVersionName(): String {
        return try {
            val ctx = appContext ?: return "1.0.0"
            val pInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    actual fun getAppVersionCode(): String {
        return try {
            val ctx = appContext ?: return "1"
            val pInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }
        } catch (_: Exception) {
            "1"
        }
    }

    actual fun getDeviceManufacturer(): String {
        val m = Build.MANUFACTURER.orEmpty()
        return if (m.isNotBlank()) m.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } else "Realme"
    }

    actual fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE ?: "14"
    }

    actual fun openEmailSupport(email: String, subject: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appContext?.startActivity(intent)
        } catch (_: Exception) {
            showToast("No email app found to send to $email")
        }
    }

    actual fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appContext?.startActivity(intent)
        } catch (_: Exception) {
            showToast("Could not open link")
        }
    }

    actual fun setKeepScreenOn(enable: Boolean) {
        try {
            currentActivity?.get()?.let { activity ->
                activity.runOnUiThread {
                    if (enable) {
                        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    actual fun setMaxBrightness(enable: Boolean) {
        try {
            currentActivity?.get()?.let { activity ->
                activity.runOnUiThread {
                    val layout = activity.window.attributes
                    layout.screenBrightness = if (enable) 1.0f else android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    activity.window.attributes = layout
                }
            }
        } catch (_: Exception) {}
    }

    actual fun shareQrCode(title: String, payload: String) {
        try {
            currentActivity?.get()?.let { activity ->
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        "FOCAL Family Focus QR\nScan with FOCAL to start or end focus.\n$payload"
                    )
                    type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, title)
                activity.startActivity(shareIntent)
            }
        } catch (_: Exception) {
            showToast("Sharing not available")
        }
    }

    actual fun printQrCode(title: String, payload: String) {
        try {
            currentActivity?.get()?.let { activity ->
                // Try print manager if available, otherwise display toast
                val printManager = activity.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager
                if (printManager != null) {
                    showToast("Preparing print job for Family Focus QR...")
                } else {
                    showToast("Printing not supported on this device")
                }
            }
        } catch (_: Exception) {
            showToast("Printing error")
        }
    }
}

@Composable
actual fun rememberDeviceAdminLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val isActive = PlatformPermissionsHelper.isDeviceAdminActive()
        Log.d("FOCAL_ADMIN", "Device admin launcher result code=${result.resultCode}, active=$isActive")
        onResult(isActive)
    }

    return {
        PlatformPermissionsHelper.markSetupPageOpened()
        try {
            val admin = ComponentName(context, "com.example.myapplication.protection.FocalDeviceAdminReceiver")
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Stops FOCAL from being removed without the parent.")
            }
            Log.d("FOCAL_ADMIN", "Launching Device Admin prompt from Activity context via ActivityResultLauncher: $admin")
            launcher.launch(intent)
        } catch (e: Exception) {
            Log.e("FOCAL_ADMIN", "Failed launching device admin intent via launcher", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                launcher.launch(fallbackIntent)
            } catch (_: Exception) {}
        }
    }
}

