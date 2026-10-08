package com.example.myapplication.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.focal.app.data.FocalRepository
import com.focal.app.model.InstalledApp

object AndroidAppPickerManager {

    fun loadInstalledAppsIntoRepository(context: Context) {
        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = pm.queryIntentActivities(mainIntent, 0)

            val installedApps = resolvedApps.mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg != context.packageName) {
                    val label = resolveInfo.loadLabel(pm).toString()
                    val cat = getAppCategory(pm, pkg)
                    val iconHash = "hash_${pkg.hashCode()}"
                    InstalledApp(
                        packageName = pkg,
                        appName = label,
                        category = cat,
                        iconHash = iconHash,
                        isSystemApp = isSystemPackage(pm, pkg)
                    )
                } else null
            }.distinctBy { it.packageName }.sortedBy { it.appName }

            if (installedApps.isNotEmpty()) {
                FocalRepository.instance.syncChildInstalledApps(installedApps)
            }
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }

    private fun getAppCategory(pm: PackageManager, packageName: String): String {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val lowerPkg = packageName.lowercase()

            when {
                lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("snapchat") || lowerPkg.contains("tiktok") || lowerPkg.contains("reddit") -> "Social"
                lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("primevideo") || lowerPkg.contains("twitch") || lowerPkg.contains("hulu") -> "Video"
                lowerPkg.contains("game") || lowerPkg.contains("pubg") || lowerPkg.contains("roblox") || lowerPkg.contains("clash") || lowerPkg.contains("chess") -> "Games"
                lowerPkg.contains("amazon") || lowerPkg.contains("flipkart") || lowerPkg.contains("ebay") || lowerPkg.contains("myntra") -> "Shopping"
                lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("discord") || lowerPkg.contains("signal") -> "Messaging"
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        when (appInfo.category) {
                            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> "Video"
                            ApplicationInfo.CATEGORY_GAME -> "Games"
                            ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_IMAGE -> "Other"
                            else -> "Other"
                        }
                    } else "Other"
                }
            }
        } catch (_: Exception) {
            "Other"
        }
    }

    private fun isSystemPackage(pm: PackageManager, packageName: String): Boolean {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            false
        }
    }

    class PackageChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return
            val action = intent.action
            if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REMOVED) {
                loadInstalledAppsIntoRepository(context)
            }
        }
    }

    fun registerPackageReceiver(context: Context) {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
            context.registerReceiver(PackageChangeReceiver(), filter)
        } catch (_: Exception) {}
    }
}

