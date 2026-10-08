package com.focal.app.ui.components

actual object PlatformPermissionsHelper {
    actual fun openAccessibilitySettings() {}
    actual fun openOverlaySettings() {}
    actual fun openUsageAccessSettings() {}
    actual fun openNotificationSettings() {}
    actual fun openBatteryOptimizationSettings() {}
    actual fun openExactAlarmSettings() {}
    actual fun openDeviceAdminSettings() {}
    actual fun openNfcSettings() {}
    actual fun openAppDetailsSettings() {}

    actual fun isAccessibilityEnabled(): Boolean = true
    actual fun isOverlayEnabled(): Boolean = true
    actual fun isUsageAccessEnabled(): Boolean = true
    actual fun isNotificationPermissionGranted(): Boolean = true
    actual fun isBatteryOptimizationDisabled(): Boolean = true
    actual fun isExactAlarmPermissionGranted(): Boolean = true
    actual fun isDeviceAdminActive(): Boolean = true
    actual fun hasNfcHardware(): Boolean = true
    actual fun isNfcEnabled(): Boolean = true

    actual fun openNewBrowserTab() {}
    actual fun getRealDailyScreenTimeMinutes(): Int = 0
}
