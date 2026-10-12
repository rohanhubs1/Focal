package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.focal.app.engine.FocusEngine
import com.focal.app.protection.*
import com.focal.app.ui.components.PlatformPermissionsHelper

object ActionExecutor {

    private const val TAG = "ActionExecutor"
    private const val DEBOUNCE_THRESHOLD_MS = 500L
    private const val HOME_RATE_WINDOW_MS = 2000L
    private const val MAX_HOME_PER_WINDOW = 3

    private var lastPackage: String? = null
    private var lastDecisionType: DecisionType? = null
    private var lastActionTimeElapsed: Long = 0L

    private val homeTimestamps = mutableListOf<Long>()

    fun execute(
        context: Context,
        accessibilityService: AccessibilityService?,
        windowInfo: WindowInfo,
        decision: ProtectionDecision,
        currentElapsed: Long = SystemClock.elapsedRealtime()
    ) {
        val now = currentElapsed

        // Debounce identical decisions for the same package within 500ms
        if (windowInfo.packageName == lastPackage && decision.type == lastDecisionType && (now - lastActionTimeElapsed) < DEBOUNCE_THRESHOLD_MS) {
            return
        }

        lastPackage = windowInfo.packageName
        lastDecisionType = decision.type
        lastActionTimeElapsed = now

        val actionTakenText: String

        if (decision is ProtectionDecision.Allow) {
            actionTakenText = "Allowed (${decision.reason})"
            // Dismiss overlay if previously showing and user navigated to an allowed app
            if (BlockOverlayManager.isOverlayShowing()) {
                BlockOverlayManager.dismissOverlay(context)
            }
        } else {
            // Non-ALLOW decision: MUST ALWAYS SHOW FOCAL BLOCK SCREEN OVERLAY
            // Safety check: Never block launcher home screen unless explicit FOCAL uninstall prompt is active
            if (ProtectionEngine.LAUNCHER_PACKAGES.contains(windowInfo.packageName.lowercase()) && decision !is ProtectionDecision.BlockUninstall) {
                Log.w(TAG, "Preventing overlay/HOME execution on launcher package '${windowInfo.packageName}' for decision ${decision.label}")
                if (BlockOverlayManager.isOverlayShowing()) {
                    BlockOverlayManager.dismissOverlay(context)
                }
                return
            }

            val engine = FocusEngine.instance
            val policyName = engine.currentSession?.policyName ?: "Active Focus Policy"
            val remainingText = engine.formatRemainingTime()

            val titleText = when (decision) {
                is ProtectionDecision.BlockApp -> "${decision.appName} is blocked"
                is ProtectionDecision.BlockWebsite -> "${decision.domain} is blocked"
                is ProtectionDecision.LockSettings -> "Settings are locked"
                is ProtectionDecision.BlockUninstall -> "Uninstalling FOCAL is locked"
                else -> "Access Restricted"
            }

            val subtitleText = when (decision) {
                is ProtectionDecision.BlockUninstall -> "Ask your parent to unlock"
                is ProtectionDecision.LockSettings -> "Settings are locked until focus ends"
                else -> if (remainingText.isNotBlank()) "$policyName · $remainingText left" else policyName
            }

            val isOverlayEnabled = PlatformPermissionsHelper.isOverlayEnabled()

            if (isOverlayEnabled) {
                actionTakenText = "Overlay Shown + HOME (${decision.label})"
                BlockOverlayManager.showOverlay(
                    context = context,
                    title = titleText,
                    subtitle = subtitleText,
                    decision = decision,
                    onBackToFocusClick = {
                        // Staying on home screen behind overlay
                    }
                )
            } else {
                actionTakenText = "BlockActivity Launched + HOME (${decision.label})"
                val intent = Intent(context, BlockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_TITLE", titleText)
                    putExtra("EXTRA_SUBTITLE", subtitleText)
                }
                context.startActivity(intent)
            }

            // Perform HOME action behind the full-screen overlay (rate-limited)
            performHomeAction(context, accessibilityService, now)

            // Trigger telemetry in FocusEngine
            val targetName = when (decision) {
                is ProtectionDecision.BlockApp -> decision.appName
                is ProtectionDecision.BlockWebsite -> decision.domain
                else -> decision.label
            }
            engine.triggerBlockedAppAttempt(targetName)
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

    private fun performHomeAction(context: Context, accessibilityService: AccessibilityService?, nowElapsed: Long) {
        // Enforce max 3 HOME actions per 2000ms
        homeTimestamps.removeAll { (nowElapsed - it) > HOME_RATE_WINDOW_MS }
        if (homeTimestamps.size >= MAX_HOME_PER_WINDOW) {
            Log.w(TAG, "Home rate limit reached (${homeTimestamps.size} in last 2s). Skipping extra HOME action.")
            return
        }
        homeTimestamps.add(nowElapsed)

        if (accessibilityService != null) {
            accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
        }
    }
}
