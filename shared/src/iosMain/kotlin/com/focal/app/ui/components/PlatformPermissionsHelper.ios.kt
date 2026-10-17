package com.focal.app.ui.components

actual object PlatformPermissionsHelper {
    actual fun openAccessibilitySettings() {}
    actual fun openOverlaySettings() {}
    actual fun openUsageAccessSettings() {}
    actual fun openNotificationSettings() {}
    actual fun openBatteryOptimizationSettings() {}
    actual fun openExactAlarmSettings() {}
    actual fun openDeviceAdminSettings() {}
    actual fun openSecuritySettings() {}
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
    actual fun isAnimationsDisabled(): Boolean = false

    actual fun openNewBrowserTab() {}
    actual fun getRealDailyScreenTimeMinutes(): Int = 0

    actual fun markSetupPageOpened() {}
    actual fun isSetupPageGraceActive(): Boolean = false
    actual fun copyToClipboard(text: String) {}
    actual fun showToast(message: String) {}
    actual fun getAppVersionName(): String = "1.0.0"
    actual fun getAppVersionCode(): String = "1"
    actual fun getDeviceManufacturer(): String = "Apple"
    actual fun getAndroidVersion(): String = "iOS"

    actual fun openEmailSupport(email: String, subject: String) {}
    actual fun openUrl(url: String) {}
    actual fun setKeepScreenOn(enable: Boolean) {}
    actual fun setMaxBrightness(enable: Boolean) {}
    actual fun shareQrCode(title: String, payload: String) {}
    actual fun printQrCode(title: String, payload: String) {}
    actual fun testBlockScreen() {}
    actual fun isCameraPermissionGranted(): Boolean = true
    actual fun vibrate(durationMs: Long) {}
}

@androidx.compose.runtime.Composable
actual fun rememberDeviceAdminLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    return { onResult(true) }
}

@androidx.compose.runtime.Composable
actual fun rememberCameraPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    return { onResult(true) }
}

