package com.focal.app.protection

import com.focal.app.model.FocusPolicyData

object ProtectionEngine {

    val DIALER_AND_CALL_PACKAGES = setOf(
        "com.android.phone",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.server.telecom",
        "com.android.incallui",
        "com.google.android.incallui",
        "com.samsung.android.incallui",
        "com.coloros.incallui",
        "com.vivo.incallui",
        "com.oppo.incallui",
        "com.miui.incallui"
    )

    val EMERGENCY_PACKAGES = setOf(
        "com.android.emergency",
        "com.samsung.android.emergency",
        "com.sec.android.app.emergency"
    )

    val KEYBOARD_PACKAGES = setOf(
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.touchtype.swiftkey",
        "com.baidu.input",
        "com.iflytek.inputmethod",
        "com.sohu.inputmethod.sogou"
    )

    val SETTINGS_PACKAGES = setOf(
        "com.android.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.oplus.settings",
        "com.coloros.settings",
        "com.coloros.safecenter",
        "com.oplus.safecenter",
        "com.oplus.battery",
        "com.coloros.oppoguardelf",
        "com.coloros.securitypermission",
        "com.oppo.safe",
        "com.oplus.appdetail",
        "com.coloros.appdetail",
        "com.oplus.uninstall",
        "com.samsung.android.sm",
        "com.samsung.android.sm_cn",
        "com.samsung.android.lool",
        "com.miui.securitycenter",
        "com.miui.cleanmaster",
        "com.vivo.permissionmanager",
        "com.iqoo.secure"
    )

    val INSTALLER_AND_LAUNCHER_PACKAGES = setOf(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.sec.android.app.uninstaller",
        "com.oppo.launcher",
        "com.oplus.launcher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.google.android.apps.nexuslauncher",
        "com.oneplus.launcher",
        "com.android.launcher",
        "com.android.launcher3",
        "com.coloros.safecenter",
        "com.oplus.safecenter",
        "com.coloros.oppoguardelf",
        "com.coloros.securitypermission",
        "com.oplus.uninstall",
        "com.android.vending"
    )

    val LAUNCHER_PACKAGES = setOf(
        "com.oppo.launcher",
        "com.oplus.launcher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.google.android.apps.nexuslauncher",
        "com.oneplus.launcher",
        "com.android.launcher",
        "com.android.launcher3",
        "com.nothing.launcher",
        "com.huawei.android.launcher",
        "com.vivo.launcher",
        "com.lge.launcher3",
        "com.zte.handset.launcher",
        "com.motorola.launcher3"
    )

    val KNOWN_BROWSERS = setOf(
        "com.android.chrome",
        "com.google.android.apps.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.sec.android.app.sbrowser",
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.duckduckgo.mobile.android",
        "com.heytap.browser",
        "com.nearme.browser",
        "com.vivo.browser",
        "com.mi.globalbrowser",
        "com.android.browser"
    )

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

    fun decide(
        windowInfo: WindowInfo,
        state: ProtectionState,
        nowElapsed: Long,
        appLabel: String = "focal",
        ownPackage: String = "com.example.myapplication",
        isFixAllowanceActive: Boolean = false
    ): ProtectionDecision {
        val lowerPkg = windowInfo.packageName.lowercase()
        val ownPkgLower = ownPackage.lowercase()
        val appLabelLower = appLabel.lowercase()
        val fullTextLower = windowInfo.fullText.lowercase()

        // Rule a: Always ALLOW
        // FOCAL own app (including debug/demo variants)
        if (lowerPkg == ownPkgLower || lowerPkg.contains("com.focal.app") ||
            lowerPkg.contains("com.rohan.focalapplication") || lowerPkg.contains("myapplication")) {
            return ProtectionDecision.Allow('a', "FOCAL own package allowed")
        }

        // Fix-Button Allowance: If active (user clicked Fix/Allow button in FOCAL within 3 min), allow system settings/permissions page
        if (isFixAllowanceActive) {
            val isSettingsOrPermissionPage = SETTINGS_PACKAGES.contains(lowerPkg) ||
                    INSTALLER_AND_LAUNCHER_PACKAGES.contains(lowerPkg)
            if (isSettingsOrPermissionPage) {
                val mentionsFocalUninstall = (fullTextLower.contains(appLabelLower) || fullTextLower.contains("focal") || fullTextLower.contains("rohan")) &&
                        (fullTextLower.contains("uninstall") || fullTextLower.contains("clear data"))
                if (!mentionsFocalUninstall) {
                    return ProtectionDecision.Allow('a', "FIX_ALLOWANCE_ACTIVE: Fix-button system settings page allowed")
                }
            }
        }

        // Dialers & Call UI
        if (DIALER_AND_CALL_PACKAGES.contains(lowerPkg) || lowerPkg.contains("incallui")) {
            return ProtectionDecision.Allow('a', "Dialer & Call UI allowed")
        }

        // Emergency SOS
        if (EMERGENCY_PACKAGES.contains(lowerPkg) || lowerPkg.contains("emergency") ||
            fullTextLower.contains("emergency") || fullTextLower.contains("sos call") ||
            fullTextLower.contains("112") || fullTextLower.contains("911")
        ) {
            return ProtectionDecision.Allow('a', "Emergency SOS allowed")
        }

        // Keyboards / IMEs
        if (windowInfo.isIme || KEYBOARD_PACKAGES.contains(lowerPkg) || lowerPkg.contains("inputmethod")) {
            return ProtectionDecision.Allow('a', "Keyboard/IME allowed")
        }

        // System UI (except Active Apps stop dialog for FOCAL)
        if (lowerPkg == "com.android.systemui") {
            val isActiveAppsStopFocal = fullTextLower.contains("active apps") &&
                    (fullTextLower.contains(appLabelLower) || fullTextLower.contains("focal"))
            if (!isActiveAppsStopFocal) {
                return ProtectionDecision.Allow('a', "System UI allowed")
            }
        }

        // Device Admin Activation Prompt (Allowed when activating device admin app)
        val isDeviceAdminScreen = fullTextLower.contains("device admin") || fullTextLower.contains("device administrator")
        val hasActualActivateKeyword = fullTextLower.replace("deactivate", "").contains("activate") ||
                fullTextLower.contains("enable") || fullTextLower.contains("turn on") || fullTextLower.contains("add device admin")
        val isActivationPrompt = isDeviceAdminScreen && hasActualActivateKeyword
        if (isActivationPrompt) {
            val isExplicitDeactivationOnly = fullTextLower.contains("deactivate this") || fullTextLower.contains("remove device admin")
            if (!isExplicitDeactivationOnly) {
                return ProtectionDecision.Allow('a', "Device Admin activation prompt allowed")
            }
        }

        // Launcher Home Screen (Allowed unless an explicit FOCAL uninstall prompt dialog is active)
        if (LAUNCHER_PACKAGES.contains(lowerPkg)) {
            val mentionsFocal = fullTextLower.contains(appLabelLower) || fullTextLower.contains("focal") ||
                    fullTextLower.contains("rohan") || (ownPkgLower.isNotBlank() && fullTextLower.contains(ownPkgLower))
            val isUninstallDialog = mentionsFocal && (
                    fullTextLower.contains("uninstall") || fullTextLower.contains("verification code") ||
                    fullTextLower.contains("enter code to uninstall")
            )
            if (!isUninstallDialog) {
                return ProtectionDecision.Allow('a', "Launcher home screen allowed")
            }
        }

        // Rule b: If !setupComplete -> ALLOW everything
        if (!state.setupComplete) {
            return ProtectionDecision.Allow('b', "Setup incomplete allowed")
        }

        // Rule c: If now < parentUnlockUntil -> ALLOW everything
        if (nowElapsed < state.parentUnlockUntilElapsed) {
            return ProtectionDecision.Allow('c', "Parent unlock active allowed")
        }

        // Rule d: UNINSTALL PROTECTION (focus or not)
        val isUninstallInstallerOrLauncher = (INSTALLER_AND_LAUNCHER_PACKAGES.contains(lowerPkg) ||
                SETTINGS_PACKAGES.contains(lowerPkg)) && lowerPkg != "com.android.systemui"
        if (isUninstallInstallerOrLauncher) {
            val mentionsFocal = fullTextLower.contains(appLabelLower) || fullTextLower.contains("focal") ||
                    fullTextLower.contains("rohan") || (ownPkgLower.isNotBlank() && fullTextLower.contains(ownPkgLower))
            val isUninstallFlow = fullTextLower.contains("uninstall") || fullTextLower.contains("clear data") ||
                    fullTextLower.contains("clear storage") || fullTextLower.contains("force stop") ||
                    fullTextLower.contains("verification code") || fullTextLower.contains("enter code to uninstall")
            if (mentionsFocal && isUninstallFlow) {
                return ProtectionDecision.BlockUninstall('d', "Uninstall/Disable FOCAL screen blocked")
            }
        }
        if (fullTextLower.contains("device admin") && (fullTextLower.contains("focal") || fullTextLower.contains(appLabelLower))) {
            val isExplicitDeactivate = fullTextLower.contains("deactivate") || fullTextLower.contains("remove")
            val isActivateButtonPresent = fullTextLower.replace("deactivate", "").contains("activate") || fullTextLower.contains("enable")
            if (isExplicitDeactivate && !isActivateButtonPresent) {
                return ProtectionDecision.BlockUninstall('d', "Device Admin deactivation screen blocked")
            }
        }

        // Lock Settings = ALWAYS (All the time mode)
        if (SETTINGS_PACKAGES.contains(lowerPkg) && state.lockSettingsMode == LockSettingsMode.ALWAYS) {
            return ProtectionDecision.LockSettings('f', "Settings package locked all the time")
        }

        // Rule e: If !focusActive -> ALLOW
        if (!state.focusActive) {
            return ProtectionDecision.Allow('e', "Focus not active allowed")
        }

        // Rule f: During focus — SETTINGS
        if (SETTINGS_PACKAGES.contains(lowerPkg)) {
            return ProtectionDecision.LockSettings('f', "Settings package locked during focus")
        }
        if (lowerPkg == "com.android.systemui" && fullTextLower.contains("active apps") && (fullTextLower.contains(appLabelLower) || fullTextLower.contains("focal"))) {
            return ProtectionDecision.LockSettings('f', "Active Apps stop dialog locked during focus")
        }

        // Rule g: During focus — APPS
        val blockedPkgs = state.policy.blockedPackages.map { it.lowercase() }.toSet()

        // Match package directly or by mapped app name
        var matchedApp: String? = null
        for (appName in state.policy.blockedApps) {
            val pkg = APP_NAME_TO_PACKAGE[appName]?.lowercase() ?: appName.lowercase()
            if (isPackageMatch(lowerPkg, pkg)) {
                matchedApp = appName
                break
            }
        }
        if (matchedApp == null) {
            for (pkg in blockedPkgs) {
                if (isPackageMatch(lowerPkg, pkg)) {
                    matchedApp = pkg
                    break
                }
            }
        }

        if (matchedApp != null) {
            return ProtectionDecision.BlockApp(lowerPkg, matchedApp, 'g', "App '$matchedApp' blocked during focus")
        }

        // Rule h: During focus — WEBSITES (ONLY if package in KNOWN_BROWSERS)
        if (KNOWN_BROWSERS.contains(lowerPkg)) {
            val urlText = windowInfo.urlText?.lowercase() ?: ""
            if (urlText.isNotBlank()) {
                val blockedDomains = state.policy.blockedWebsites
                for (domain in blockedDomains) {
                    val cleanDomain = domain.lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.")
                    if (cleanDomain.isNotBlank() && (urlText.contains(cleanDomain) || isSubdomainMatch(urlText, cleanDomain))) {
                        return ProtectionDecision.BlockWebsite(cleanDomain, 'h', "Domain '$cleanDomain' blocked during focus")
                    }
                }
            }
        }

        // Rule i: Otherwise -> ALLOW
        return ProtectionDecision.Allow('i', "App allowed")
    }

    private fun isPackageMatch(currentPkg: String, targetPkgOrName: String): Boolean {
        val target = targetPkgOrName.lowercase().trim()
        if (target.isBlank() || target.length < 3) return false
        if (currentPkg == target) return true
        if (target.contains(".")) {
            return currentPkg == target || currentPkg.startsWith("$target.")
        }
        if (target.length >= 4 && currentPkg.contains(target)) {
            return true
        }
        return false
    }

    private fun isSubdomainMatch(urlText: String, cleanDomain: String): Boolean {
        if (urlText.contains(".$cleanDomain")) return true
        if (urlText.contains("/$cleanDomain")) return true
        return false
    }
}
