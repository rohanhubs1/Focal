package com.focal.app.ui.components

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

actual object PlatformPermissionsHelper {
    var appContext: Context? = null

    actual fun openAccessibilitySettings() {
        try {
            appContext?.let { ctx ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    actual fun openOverlaySettings() {
        try {
            appContext?.let { ctx ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(intent)
                }
            }
        } catch (_: Exception) {}
    }

    actual fun openUsageAccessSettings() {
        try {
            appContext?.let { ctx ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    actual fun openNotificationSettings() {
        try {
            appContext?.let { ctx ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(intent)
                }
            }
        } catch (_: Exception) {}
    }

    actual fun isAccessibilityEnabled(): Boolean {
        return try {
            val ctx = appContext ?: return false
            val enabledServices = Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            enabledServices.contains("FocalAccessibilityService") || enabledServices.contains(ctx.packageName)
        } catch (_: Exception) {
            false
        }
    }

    actual fun isOverlayEnabled(): Boolean {
        return try {
            val ctx = appContext ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(ctx)
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    actual fun isUsageAccessEnabled(): Boolean {
        return try {
            val ctx = appContext ?: return false
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    ctx.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    ctx.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    actual fun isNotificationPermissionGranted(): Boolean {
        return try {
            val ctx = appContext ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    ctx,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            }
        } catch (_: Exception) {
            true
        }
    }

    actual fun openNewBrowserTab() {
        try {
            appContext?.let { ctx ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("about:blank")
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
}
