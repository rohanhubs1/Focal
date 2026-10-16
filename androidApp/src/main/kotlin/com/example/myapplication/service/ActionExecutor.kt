package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.focal.app.engine.FocusEngine
import com.focal.app.protection.*

object ActionExecutor {

    private const val TAG = "ActionExecutor"
    private const val DEBOUNCE_THRESHOLD_MS = 1500L

    var lastBlockedPackage: String? = null
        private set
    var lastBlockedTimestampMs: Long = 0L
        private set

    fun execute(
        context: Context,
        accessibilityService: AccessibilityService?,
        windowInfo: WindowInfo,
        decision: ProtectionDecision,
        currentElapsed: Long = SystemClock.elapsedRealtime()
    ) {
        val now = currentElapsed
        val lowerPkg = windowInfo.packageName.lowercase()

        // Debounce identical decisions for the same package within 1500ms
        if (lowerPkg == lastBlockedPackage && (now - lastBlockedTimestampMs) < DEBOUNCE_THRESHOLD_MS) {
            return
        }

        val actionTakenText: String

        if (decision is ProtectionDecision.Allow) {
            actionTakenText = "Allowed (${decision.reason})"
            // Dismiss overlay if previously showing and user navigated to an allowed app
            if (BlockOverlayManager.isOverlayShowing()) {
                BlockOverlayManager.dismissOverlay(context)
            }
        } else {
            // Non-ALLOW decision: MUST ALWAYS SHOW FOCAL BLOCK SCREEN DIRECTLY
            // Safety check: Never block launcher home screen unless explicit FOCAL uninstall prompt is active
            if (ProtectionEngine.LAUNCHER_PACKAGES.contains(lowerPkg) && decision !is ProtectionDecision.BlockUninstall) {
                Log.w(TAG, "Preventing block screen execution on launcher package '${windowInfo.packageName}' for decision ${decision.label}")
                if (BlockOverlayManager.isOverlayShowing()) {
                    BlockOverlayManager.dismissOverlay(context)
                }
                return
            }

            val engine = FocusEngine.instance
            val currentSession = engine.currentSession
            val policyName = currentSession?.policyName ?: "Active Focus Policy"
            val remainingMs = currentSession?.let { maxOf(0L, it.endTimeMillis - System.currentTimeMillis()) } ?: (engine.remainingSeconds * 1000L)
            val remMins = ((remainingMs + 59999L) / 60000L).coerceAtLeast(1L)
            val remainingText = "${remMins}m"

            val titleText = when (decision) {
                is ProtectionDecision.BlockApp -> "${decision.appName} is blocked"
                is ProtectionDecision.BlockWebsite -> "${decision.domain} is blocked"
                is ProtectionDecision.LockSettings -> "This setting is locked during focus"
                is ProtectionDecision.BlockUninstall -> "Uninstalling FOCAL is locked"
                else -> "Access Restricted"
            }

            val subtitleText = when (decision) {
                is ProtectionDecision.BlockUninstall -> "Ask your parent to unlock"
                is ProtectionDecision.LockSettings -> "Ask your parent to unlock"
                else -> "$policyName · $remainingText left"
            }

            // Check if block screen is already showing
            val isBlockScreenShowing = BlockActivity.isShowing || BlockOverlayManager.isOverlayShowing()

            actionTakenText = "BlockActivity Launched directly (${decision.label})"
            val intent = Intent(context, BlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                putExtra("EXTRA_TITLE", titleText)
                putExtra("EXTRA_SUBTITLE", subtitleText)
                putExtra("EXTRA_PACKAGE_NAME", windowInfo.packageName)
                putExtra("EXTRA_DECISION_TYPE", decision.javaClass.simpleName)
                putExtra("EXTRA_IS_PARENT_UNLOCK_ALLOWED", decision is ProtectionDecision.LockSettings || decision is ProtectionDecision.BlockUninstall)
            }
            context.startActivity(intent)

            // Problem 2 Fix: Count 1 block per attempt: increment ONLY when a NEW block screen is shown
            // (not on debounced or repeated events while the block screen is already up).
            if (!isBlockScreenShowing) {
                val targetName = when (decision) {
                    is ProtectionDecision.BlockApp -> decision.appName
                    is ProtectionDecision.BlockWebsite -> decision.domain
                    else -> decision.label
                }
                engine.triggerBlockedAppAttempt(targetName)
            }

            lastBlockedPackage = lowerPkg
            lastBlockedTimestampMs = now
        }

        // Record decision log
        val logEntry = DecisionLogEntry(
            timestampMs = System.currentTimeMillis(),
            packageName = windowInfo.packageName,
            windowClass = windowInfo.windowClass,
            decisionType = decision.type,
            ruleLetter = decision.ruleLetter,
            reason = decision.reason,
            actionTaken = actionTakenText
        )
        ProtectionStateRepository.instance.recordDecision(logEntry)
        Log.d(TAG, "Decided [${decision.ruleLetter}] ${decision.label} on '${windowInfo.packageName}' -> $actionTakenText")
    }

    fun resetDebounce() {
        lastBlockedPackage = null
        lastBlockedTimestampMs = 0L
    }
}
