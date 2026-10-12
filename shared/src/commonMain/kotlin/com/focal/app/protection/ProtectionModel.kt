package com.focal.app.protection

import com.focal.app.model.FocusPolicyData

data class WindowInfo(
    val packageName: String,
    val windowClass: String = "",
    val windowTitle: String = "",
    val fullText: String = "",
    val urlText: String? = null,
    val isApplicationWindow: Boolean = true,
    val isIme: Boolean = false
)

enum class LockSettingsMode {
    DURING_FOCUS,
    ALWAYS
}

enum class DecisionType {
    ALLOW,
    BLOCK_APP,
    BLOCK_WEBSITE,
    LOCK_SETTINGS,
    BLOCK_UNINSTALL
}

sealed class ProtectionDecision(
    val type: DecisionType,
    val ruleLetter: Char,
    val reason: String
) {
    class Allow(ruleLetter: Char, reason: String) : ProtectionDecision(DecisionType.ALLOW, ruleLetter, reason)
    class BlockApp(val packageName: String, val appName: String, ruleLetter: Char, reason: String) : ProtectionDecision(DecisionType.BLOCK_APP, ruleLetter, reason)
    class BlockWebsite(val domain: String, ruleLetter: Char, reason: String) : ProtectionDecision(DecisionType.BLOCK_WEBSITE, ruleLetter, reason)
    class LockSettings(ruleLetter: Char, reason: String) : ProtectionDecision(DecisionType.LOCK_SETTINGS, ruleLetter, reason)
    class BlockUninstall(ruleLetter: Char, reason: String) : ProtectionDecision(DecisionType.BLOCK_UNINSTALL, ruleLetter, reason)

    val label: String
        get() = when (this) {
            is Allow -> "ALLOW"
            is BlockApp -> "BLOCK_APP($packageName)"
            is BlockWebsite -> "BLOCK_WEBSITE($domain)"
            is LockSettings -> "LOCK_SETTINGS"
            is BlockUninstall -> "BLOCK_UNINSTALL"
        }
}

data class DecisionLogEntry(
    val timestampMs: Long,
    val packageName: String,
    val windowClass: String,
    val decisionType: DecisionType,
    val ruleLetter: Char,
    val reason: String,
    val actionTaken: String
)

data class ProtectionState(
    val setupComplete: Boolean = false,
    val focusActive: Boolean = false,
    val focusEndsAtElapsed: Long = 0L,
    val policy: FocusPolicyData = FocusPolicyData(),
    val parentUnlockUntilElapsed: Long = 0L,
    val lockSettingsMode: LockSettingsMode = LockSettingsMode.DURING_FOCUS,
    val pinFailedAttempts: Int = 0,
    val pinCooldownUntilMs: Long = 0L,
    val parentPinHash: String = "1234",
    val lastDecisions: List<DecisionLogEntry> = emptyList()
)
