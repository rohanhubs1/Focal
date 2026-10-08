package com.focal.app.ui.components

expect object PlatformPermissionsHelper {
    fun openAccessibilitySettings()
    fun openOverlaySettings()
    fun openUsageAccessSettings()
    fun openNotificationSettings()
    fun isAccessibilityEnabled(): Boolean
    fun isOverlayEnabled(): Boolean
    fun isUsageAccessEnabled(): Boolean
    fun isNotificationPermissionGranted(): Boolean
    fun openNewBrowserTab()
    fun getRealDailyScreenTimeMinutes(): Int
}
