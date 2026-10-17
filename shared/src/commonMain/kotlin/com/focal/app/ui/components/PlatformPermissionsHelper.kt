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
    fun isAnimationsDisabled(): Boolean

    fun openNewBrowserTab()
    fun getRealDailyScreenTimeMinutes(): Int

    fun markSetupPageOpened()
    fun isSetupPageGraceActive(): Boolean

    fun copyToClipboard(text: String)
    fun showToast(message: String)
    fun getAppVersionName(): String
    fun getAppVersionCode(): String
    fun getDeviceManufacturer(): String
    fun getAndroidVersion(): String

    fun openEmailSupport(email: String, subject: String)
    fun openUrl(url: String)
    fun setKeepScreenOn(enable: Boolean)
    fun setMaxBrightness(enable: Boolean)
    fun shareQrCode(title: String, payload: String)
    fun printQrCode(title: String, payload: String)
    fun testBlockScreen()
    fun isCameraPermissionGranted(): Boolean
    fun vibrate(durationMs: Long = 50L)
}

@androidx.compose.runtime.Composable
expect fun rememberDeviceAdminLauncher(onResult: (Boolean) -> Unit): () -> Unit

@androidx.compose.runtime.Composable
expect fun rememberCameraPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit

