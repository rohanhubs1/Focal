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

actual object PlatformPermissionsHelper {
    var appContext: Context? = null

    private fun safeStartActivity(intent: Intent) {
        try {
            appContext?.let { ctx ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
        } catch (_: Exception) {
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
        safeStartActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    actual fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            safeStartActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${appContext?.packageName}")))
        } else {
            safeStartActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    actual fun openUsageAccessSettings() {
        safeStartActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    actual fun openNotificationSettings() {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${appContext?.packageName}")
            }
            safeStartActivity(intent)
        } else {
            safeStartActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    actual fun openExactAlarmSettings() {
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
        try {
            val ctx = appContext ?: return
            val componentName = ComponentName(ctx.packageName, "com.example.myapplication.receiver.FocalDeviceAdminReceiver")
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "FOCAL Device Admin stops protection from being removed silently by child.")
            }
            safeStartActivity(intent)
        } catch (_: Exception) {
            safeStartActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
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
            val ctx = appContext ?: return false
            val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(ctx.packageName, "com.example.myapplication.receiver.FocalDeviceAdminReceiver")
            dpm.isAdminActive(componentName)
        } catch (_: Exception) {
            false
        }
    }

    actual fun hasNfcHardware(): Boolean {
        return try {
            val ctx = appContext ?: return false
            val adapter = NfcAdapter.getDefaultAdapter(ctx)
            adapter != null
        } catch (_: Exception) {
            false
        }
    }

    actual fun isNfcEnabled(): Boolean {
        return try {
            val ctx = appContext ?: return false
            val adapter = NfcAdapter.getDefaultAdapter(ctx)
            adapter != null && adapter.isEnabled
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
}
