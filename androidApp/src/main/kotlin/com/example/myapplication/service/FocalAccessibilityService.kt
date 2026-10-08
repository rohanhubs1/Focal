package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.myapplication.MainActivity
import com.focal.app.engine.FocusEngine
import com.focal.app.model.SessionState

class FocalAccessibilityService : AccessibilityService() {

    companion object {
        var isServiceRunning = false
            private set

        private var lastBlockedPackage: String? = null
        private var lastBlockedTimeMs: Long = 0L
        private const val DEBOUNCE_THRESHOLD_MS = 1500L

        private val CRITICAL_SYSTEM_PACKAGES = setOf(
            "com.android.phone",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.settings",
            "com.android.systemui",
            "com.android.calculator2",
            "com.google.android.calculator",
            "com.android.camera",
            "com.google.android.GoogleCamera",
            "com.android.contacts",
            "com.google.android.contacts",
            "com.android.mms",
            "com.google.android.apps.messaging",

            // Indian & International Banking / UPI Apps (Never interfere or block)
            "com.sbi.lotusintouch",
            "com.csam.yonosbi",
            "com.hdfcbank.mobilebanking",
            "com.icicibank.mobilebanking",
            "net.one97.paytm",
            "com.phonepe.app",
            "com.google.android.apps.nbu.paisa.user",
            "com.cred.club",
            "in.org.npci.upiapp",
            "com.axis.mobile"
        )

        private val LAUNCHER_PACKAGES = setOf(
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.oppo.launcher",
            "com.huawei.android.launcher",
            "com.oneplus.launcher",
            "com.android.launcher",
            "com.android.launcher3",
            "com.android.systemui"
        )

        // Common browser package names
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.google.android.apps.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser",
            "com.brave.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.duckduckgo.mobile.android"
        )

        // Well-known app package mappings
        val APP_NAME_TO_PACKAGE = mapOf(
            "Instagram" to "com.instagram.android",
            "YouTube" to "com.google.android.youtube",
            "Facebook" to "com.facebook.katana",
            "Snapchat" to "com.snapchat.android",
            "Netflix" to "com.netflix.mediaclient",
            "Games" to "com.supercell.clashofclans",
            "TikTok" to "com.zhiliaoapp.musically",
            "Twitter" to "com.twitter.android",
            "X" to "com.twitter.android",
            "Reddit" to "com.reddit.frontpage",
            "Discord" to "com.discord",
            "Pinterest" to "com.pinterest",
            "Telegram" to "org.telegram.messenger",
            "WhatsApp" to "com.whatsapp"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        com.focal.app.data.FocalRepository.instance.reportProtectionRestored()
        FocusEngine.instance.checkAndUpdatePermissions()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceRunning = false
        com.focal.app.data.FocalRepository.instance.reportProtectionDisabled("Accessibility Service Disabled")
        FocusEngine.instance.checkAndUpdatePermissions()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        com.focal.app.data.FocalRepository.instance.reportProtectionDisabled("Accessibility Service Stopped")
        FocusEngine.instance.checkAndUpdatePermissions()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val focusEngine = FocusEngine.instance
        if (focusEngine.sessionState != SessionState.ACTIVE || !focusEngine.isProtectionActive) return

        val currentSession = focusEngine.currentSession ?: return
        val packageName = event.packageName?.toString() ?: return
        val lowerPkg = packageName.lowercase()

        // Skip our own app, banking apps, dialer, settings, screenshot UI, & launcher packages
        if (lowerPkg == applicationContext.packageName.lowercase() ||
            CRITICAL_SYSTEM_PACKAGES.contains(lowerPkg) ||
            lowerPkg.contains("screenshot") ||
            lowerPkg.contains("smartcapture") ||
            lowerPkg.contains("screenrecorder") ||
            lowerPkg.contains("screenrecord") ||
            lowerPkg.contains("systemui") ||
            lowerPkg.contains("capture") ||
            lowerPkg.contains("markup") ||
            lowerPkg.contains("incallui") ||
            lowerPkg.contains("keyguard")) return

        if (LAUNCHER_PACKAGES.contains(lowerPkg)) {
            // User returned to home screen
            lastBlockedPackage = null
            return
        }

        // Only inspect on Window State Changed to avoid infinite loops from scroll/view events
        val eventType = event.eventType
        val isWindowStateEvent = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

        // Fetch active policy
        val repo = com.focal.app.data.FocalRepository.instance
        val activePolicy = repo.policies.firstOrNull { it.id == currentSession.policyId }

        // Check Always-Allowed List (Dialer, SOS, FOCAL, Settings, or custom allowed)
        val alwaysAllowed = activePolicy?.alwaysAllowedApps ?: emptyList()
        if (alwaysAllowed.any { lowerPkg.contains(it.lowercase()) }) return

        // 1. Direct Package / App Name Blocking
        val blockedApps = activePolicy?.blockedApps ?: listOf("Instagram", "YouTube", "Facebook", "Snapchat", "Netflix", "Games")
        for (appName in blockedApps) {
            val pkg = APP_NAME_TO_PACKAGE[appName]?.lowercase() ?: appName.lowercase()
            if (pkg.length >= 3 && lowerPkg.contains(pkg)) {
                if (isWindowStateEvent) {
                    interceptAndBlockApp(appName, packageName)
                }
                return
            }
        }

        // 2. Category-Based Blocking (Newly installed apps in blocked category are blocked automatically)
        val blockedCategories = activePolicy?.blockedCategories ?: emptyList()
        if (blockedCategories.isNotEmpty()) {
            val installedApp = repo.childInstalledApps.find { it.packageName.lowercase() == lowerPkg }
            if (installedApp != null && blockedCategories.contains(installedApp.category)) {
                if (isWindowStateEvent) {
                    interceptAndBlockApp("${installedApp.appName} (${installedApp.category})", packageName)
                }
                return
            }
        }

        // 3. Subdomain Website Blocking inside browser
        if (BROWSER_PACKAGES.contains(packageName)) {
            val blockedWebsites = activePolicy?.blockedWebsites ?: listOf("instagram.com", "youtube.com", "facebook.com", "netflix.com", "reddit.com")
            inspectAndBlockWebsites(rootInActiveWindow, blockedWebsites, packageName)
        }
    }

    private fun inspectAndBlockWebsites(nodeInfo: AccessibilityNodeInfo?, blockedWebsites: List<String>, packageName: String) {
        if (nodeInfo == null) return

        try {
            val text = nodeInfo.text?.toString()?.lowercase() ?: ""
            if (text.isNotBlank()) {
                val repo = com.focal.app.data.FocalRepository.instance
                if (repo.isDomainBlocked(text, blockedWebsites)) {
                    val cleanDomain = repo.normalizeDomain(text)
                    interceptAndBlockApp("Website: $cleanDomain", packageName)
                    return
                }
            }

            for (i in 0 until nodeInfo.childCount) {
                inspectAndBlockWebsites(nodeInfo.getChild(i), blockedWebsites, packageName)
            }
        } catch (_: Exception) {
            // Ignore node recycling errors
        }
    }

    private fun interceptAndBlockApp(targetName: String, targetPackage: String) {
        val focusEngine = FocusEngine.instance

        // Throttle duplicate accessibility events for the exact same package
        val now = System.currentTimeMillis()
        if (targetPackage == lastBlockedPackage && (now - lastBlockedTimeMs) < DEBOUNCE_THRESHOLD_MS) {
            return
        }

        lastBlockedPackage = targetPackage
        lastBlockedTimeMs = now

        // Trigger Focus Engine distraction counter & set overlay state
        focusEngine.triggerBlockedAppAttempt(targetName)

        // Smoothly bring FOCAL MainActivity with BlockedAppOverlay to front without flickering loop
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("EXTRA_BLOCKED_APP", targetName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        isServiceRunning = false
        FocusEngine.instance.checkAndUpdatePermissions()
    }
}
