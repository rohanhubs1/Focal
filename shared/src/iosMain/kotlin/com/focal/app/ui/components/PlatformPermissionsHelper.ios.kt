package com.focal.app.ui.components

actual object PlatformPermissionsHelper {
    actual fun openAccessibilitySettings() {}
    actual fun openOverlaySettings() {}
    actual fun openUsageAccessSettings() {}
    actual fun openNotificationSettings() {}
    actual fun isAccessibilityEnabled(): Boolean = true
    actual fun isOverlayEnabled(): Boolean = true
    actual fun isUsageAccessEnabled(): Boolean = true
    actual fun isNotificationPermissionGranted(): Boolean = true
    actual fun openNewBrowserTab() {}
    actual fun getRealDailyScreenTimeMinutes(): Int = 0
}
