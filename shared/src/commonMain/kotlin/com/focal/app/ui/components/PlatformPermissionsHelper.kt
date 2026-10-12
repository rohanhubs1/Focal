package com.focal.app.ui.components

expect object PlatformPermissionsHelper {
    fun openAccessibilitySettings()
    fun openOverlaySettings()
    fun openUsageAccessSettings()
    fun openNotificationSettings()
    fun openBatteryOptimizationSettings()
    fun openExactAlarmSettings()
    fun openDeviceAdminSettings()
    fun openSecuritySettings()
    fun openNfcSettings()
    fun openAppDetailsSettings()

    fun isAccessibilityEnabled(): Boolean
    fun isOverlayEnabled(): Boolean
    fun isUsageAccessEnabled(): Boolean
    fun isNotificationPermissionGranted(): Boolean
    fun isBatteryOptimizationDisabled(): Boolean
    fun isExactAlarmPermissionGranted(): Boolean
    fun isDeviceAdminActive(): Boolean
    fun hasNfcHardware(): Boolean
    fun isNfcEnabled(): Boolean

    fun openNewBrowserTab()
    fun getRealDailyScreenTimeMinutes(): Int

    fun markSetupPageOpened()
    fun isSetupPageGraceActive(): Boolean

    fun copyToClipboard(text: String)
}

@androidx.compose.runtime.Composable
expect fun rememberDeviceAdminLauncher(onResult: (Boolean) -> Unit): () -> Unit

