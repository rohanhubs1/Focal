package com.focal.app.protection

data class SettingsBlockRule(
    val id: String,
    val description: String,
    val matches: (windowInfo: WindowInfo, ownPackage: String, appLabel: String) -> Boolean
)

/**
 * Rules for blocking sensitive Settings pages during active focus while allowing general Settings.
 */
object SettingsBlockRules {

    val TARGET_PACKAGES_TO_PROTECT = listOf(
        "com.rohan.focalapplication",
        "com.rohan.focalapplication.demo",
        "com.example.myapplication",
        "com.focal.app"
    )

    val RULES = listOf(
        // 1. Accessibility (any page)
        SettingsBlockRule(
            id = "ACCESSIBILITY",
            description = "Accessibility settings page"
        ) { info, _, _ ->
            val text = info.fullText.lowercase()
            val cls = info.windowClass.lowercase()
            val title = info.windowTitle.lowercase()
            title.contains("accessibility") ||
            cls.contains("accessibility") ||
            text.contains("accessibility") ||
            text.contains("installed services") ||
            text.contains("downloaded apps") ||
            text.contains("talkback")
        },

        // 2. App info / App details / Uninstall for FOCAL
        SettingsBlockRule(
            id = "FOCAL_APP_INFO",
            description = "FOCAL app info and uninstall page"
        ) { info, ownPkg, appLabel ->
            val text = info.fullText.lowercase()
            val cls = info.windowClass.lowercase()
            val title = info.windowTitle.lowercase()
            val mentionsFocal = text.contains("focal") ||
                    text.contains(appLabel.lowercase()) ||
                    TARGET_PACKAGES_TO_PROTECT.any { text.contains(it) } ||
                    (ownPkg.isNotBlank() && text.contains(ownPkg.lowercase())) ||
                    title.contains("focal")

            val isAppInfoOrUninstall = cls.contains("appdetail") ||
                    cls.contains("installedapp") ||
                    cls.contains("appinfo") ||
                    cls.contains("uninstall") ||
                    title.contains("app info") ||
                    title.contains("app details") ||
                    text.contains("app info") ||
                    text.contains("app details") ||
                    text.contains("uninstall") ||
                    text.contains("force stop") ||
                    text.contains("clear storage") ||
                    text.contains("clear data")

            mentionsFocal && isAppInfoOrUninstall
        },

        // 3. Device admin apps / Device administrators
        SettingsBlockRule(
            id = "DEVICE_ADMIN",
            description = "Device admin settings page"
        ) { info, _, _ ->
            val text = info.fullText.lowercase()
            val cls = info.windowClass.lowercase()
            val title = info.windowTitle.lowercase()
            title.contains("device admin") ||
            cls.contains("deviceadmin") ||
            text.contains("device admin") ||
            text.contains("device administrator") ||
            text.contains("device admin apps")
        },

        // 4. Date & time
        SettingsBlockRule(
            id = "DATE_TIME",
            description = "Date & time settings page"
        ) { info, _, _ ->
            val text = info.fullText.lowercase()
            val cls = info.windowClass.lowercase()
            val title = info.windowTitle.lowercase()
            title.contains("date & time") ||
            title.contains("date and time") ||
            cls.contains("datetime") ||
            cls.contains("dateandtime") ||
            cls.contains("zonepicker") ||
            text.contains("date & time") ||
            text.contains("date and time") ||
            text.contains("automatic date") ||
            text.contains("set time automatically") ||
            text.contains("use network-provided time") ||
            text.contains("time zone")
        },

        // 5. Developer options
        SettingsBlockRule(
            id = "DEVELOPER_OPTIONS",
            description = "Developer options page"
        ) { info, _, _ ->
            val text = info.fullText.lowercase()
            val cls = info.windowClass.lowercase()
            val title = info.windowTitle.lowercase()
            title.contains("developer options") ||
            cls.contains("development") ||
            cls.contains("developeroptions") ||
            text.contains("developer options") ||
            text.contains("development settings") ||
            text.contains("usb debugging")
        },

        // 6. Reset / Factory reset / Backup & reset
        SettingsBlockRule(
            id = "RESET",
            description = "Factory reset and reset options page"
        ) { info, _, _ ->
            val text = info.fullText.lowercase()
            val cls = info.windowClass.lowercase()
            val title = info.windowTitle.lowercase()
            title.contains("reset") ||
            cls.contains("masterclear") ||
            cls.contains("resetdashboard") ||
            cls.contains("factoryreset") ||
            text.contains("factory reset") ||
            text.contains("erase all data") ||
            text.contains("backup & reset") ||
            text.contains("backup and reset") ||
            text.contains("reset phone") ||
            text.contains("reset options")
        },

        // 7. "Special app access" pages for Display over other apps / Usage access when FOCAL is shown
        SettingsBlockRule(
            id = "SPECIAL_APP_ACCESS_FOCAL",
            description = "Special app access page mentioning FOCAL"
        ) { info, ownPkg, appLabel ->
            val text = info.fullText.lowercase()
            val cls = info.windowClass.lowercase()
            val title = info.windowTitle.lowercase()
            val mentionsFocal = text.contains("focal") ||
                    text.contains(appLabel.lowercase()) ||
                    TARGET_PACKAGES_TO_PROTECT.any { text.contains(it) } ||
                    (ownPkg.isNotBlank() && text.contains(ownPkg.lowercase()))

            val isSpecialAccess = title.contains("special app access") ||
                    title.contains("display over other apps") ||
                    title.contains("appear on top") ||
                    title.contains("usage access") ||
                    cls.contains("drawoverlay") ||
                    cls.contains("manageexternalstorage") ||
                    cls.contains("usageaccess") ||
                    text.contains("special app access") ||
                    text.contains("display over other apps") ||
                    text.contains("appear on top") ||
                    text.contains("usage access") ||
                    text.contains("usage data access")

            mentionsFocal && isSpecialAccess
        },

        // 8. Battery optimisation page for FOCAL
        SettingsBlockRule(
            id = "BATTERY_OPTIMISATION_FOCAL",
            description = "Battery optimization page mentioning FOCAL"
        ) { info, ownPkg, appLabel ->
            val text = info.fullText.lowercase()
            val cls = info.windowClass.lowercase()
            val title = info.windowTitle.lowercase()
            val mentionsFocal = text.contains("focal") ||
                    text.contains(appLabel.lowercase()) ||
                    TARGET_PACKAGES_TO_PROTECT.any { text.contains(it) } ||
                    (ownPkg.isNotBlank() && text.contains(ownPkg.lowercase()))

            val isBatteryOptimization = title.contains("battery") ||
                    cls.contains("battery") ||
                    cls.contains("power") ||
                    text.contains("battery optimisation") ||
                    text.contains("battery optimization") ||
                    text.contains("app battery usage") ||
                    text.contains("background usage limits") ||
                    text.contains("unrestricted") ||
                    text.contains("optimized") ||
                    text.contains("optimised") ||
                    text.contains("sleep apps")

            mentionsFocal && isBatteryOptimization
        }
    )

    fun findMatchingBlockedRule(
        windowInfo: WindowInfo,
        ownPackage: String,
        appLabel: String
    ): SettingsBlockRule? {
        return RULES.firstOrNull { it.matches(windowInfo, ownPackage, appLabel) }
    }
}
