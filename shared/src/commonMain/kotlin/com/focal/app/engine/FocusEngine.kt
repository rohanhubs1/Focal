package com.focal.app.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.focal.app.data.FocalRepository
import com.focal.app.model.*
import kotlinx.coroutines.*

class FocusEngine private constructor() {

    companion object {
        val instance: FocusEngine by lazy { FocusEngine() }
    }

    private val repository = FocalRepository.instance
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    // Session status & state
    var sessionState by mutableStateOf(SessionState.IDLE)
        private set

    val isFocusActive: Boolean
        get() = sessionState == SessionState.ACTIVE

    var currentSession by mutableStateOf<FocusSession?>(null)
        private set

    var remainingSeconds by mutableIntStateOf(0)
        private set

    var blockedAttemptsCount by mutableIntStateOf(0)
        private set

    var distractionsPrevented by mutableIntStateOf(0)
        private set

    var emergencyExitsCount by mutableIntStateOf(0)
        private set

    // When a blocked app is attempted, this triggers the Motivational Blocked App Screen
    var blockedAppOverlayName by mutableStateOf<String?>(null)

    // Completed summary state
    var lastCompletedSummary by mutableStateOf<FocusSession?>(null)

    // Protection & Permission State (Source of Truth)
    var isAccessibilityGranted by mutableStateOf(true)
    var isOverlayGranted by mutableStateOf(true)
    var isProtectionActive by mutableStateOf(true)
    var protectionStatusText by mutableStateOf("Protection Active")
    var missingPermissionsList by mutableStateOf<List<String>>(emptyList())
    var todayFocusMinutes by mutableStateOf(0)
    var lastHeartbeatTimeMillis by mutableStateOf(currentTimeMillis())

    init {
        // Default session state is IDLE when logging in as child
        sessionState = SessionState.IDLE
        currentSession = null
        remainingSeconds = 0
        checkAndUpdatePermissions()
    }

    fun checkAndUpdatePermissions(): Boolean {
        val hasAccess = com.focal.app.ui.components.PlatformPermissionsHelper.isAccessibilityEnabled()
        val hasOverlay = com.focal.app.ui.components.PlatformPermissionsHelper.isOverlayEnabled()
        val hasAdmin = com.focal.app.ui.components.PlatformPermissionsHelper.isDeviceAdminActive()
        val hasUsage = com.focal.app.ui.components.PlatformPermissionsHelper.isUsageAccessEnabled()
        val hasNotif = com.focal.app.ui.components.PlatformPermissionsHelper.isNotificationPermissionGranted()
        val hasBat = com.focal.app.ui.components.PlatformPermissionsHelper.isBatteryOptimizationDisabled()
        val hasAlarm = com.focal.app.ui.components.PlatformPermissionsHelper.isExactAlarmPermissionGranted()

        val missingList = mutableListOf<String>()
        if (!hasAccess) missingList.add("Accessibility")
        if (!hasOverlay) missingList.add("Display over other apps")
        if (!hasAdmin) missingList.add("Uninstall protection")
        if (!hasUsage) missingList.add("Usage access")
        if (!hasNotif) missingList.add("Notifications")

        val warningList = mutableListOf<String>()
        if (!hasBat) warningList.add("Battery: Unrestricted")
        if (!hasAlarm) warningList.add("Alarms & reminders")

        val previousState = isProtectionActive
        isAccessibilityGranted = hasAccess
        isOverlayGranted = hasOverlay
        isProtectionActive = missingList.isEmpty()
        missingPermissionsList = missingList
        lastHeartbeatTimeMillis = currentTimeMillis()

        if (!isProtectionActive) {
            protectionStatusText = "Protection Disabled"
            if (previousState) {
                val childName = currentSession?.childName ?: repository.currentUser.name
                val missingStr = missingList.joinToString(", ")

                com.focal.app.data.ActivityRepository.instance.recordEvent(
                    type = com.focal.app.data.ActivityEventType.PROTECTION_OFF,
                    childId = repository.currentUser.id,
                    details = "Protection disabled: missing $missingStr",
                    metadata = mapOf("missing" to missingStr)
                )

                val notifMessage = "Focus Protection Disabled — ${childName}'s device has disabled $missingStr. FOCAL restrictions may not be fully enforced."

                repository.notifications.add(0, NotificationItem(
                    id = "notif_perm_${currentTimeMillis()}",
                    userId = "user_priya",
                    type = "PROTECTION_DISABLED",
                    title = "Focus Protection Disabled",
                    message = notifMessage,
                    timestamp = "Just now"
                ))

                com.focal.app.data.FocalSyncEngine.instance.reportProtectionStatus(
                    childId = repository.currentUser.id,
                    protectionStatus = "Protection Disabled",
                    isAccessibilityGranted = hasAccess,
                    isOverlayGranted = hasOverlay
                )
            }
        } else {
            protectionStatusText = "Protection Active"
            if (!previousState) {
                com.focal.app.data.ActivityRepository.instance.recordEvent(
                    type = com.focal.app.data.ActivityEventType.PROTECTION_ON,
                    childId = repository.currentUser.id,
                    details = "All protection permissions active",
                    metadata = emptyMap()
                )

                com.focal.app.data.FocalSyncEngine.instance.reportProtectionStatus(
                    childId = repository.currentUser.id,
                    protectionStatus = "Protection Active",
                    isAccessibilityGranted = true,
                    isOverlayGranted = hasOverlay
                )
            }
        }
        return isProtectionActive
    }

    private fun initializeDefaultActiveSession() {
        val targetMinutes = 160 // 2h 40m
        val remainingSecs = 155 * 60 + 32 // 02:35:32
        remainingSeconds = remainingSecs
        blockedAttemptsCount = 7
        distractionsPrevented = 23
        sessionState = SessionState.ACTIVE

        currentSession = FocusSession(
            id = "sess_active_1",
            userId = "user_rahul",
            childName = "Rahul",
            policyId = "pol_study",
            policyName = "Study Mode",
            mode = FocusMode.STUDY,
            triggerType = TriggerType.NFC,
            startTimeMillis = currentTimeMillis() - ((targetMinutes * 60 - remainingSecs) * 1000L),
            targetDurationMinutes = targetMinutes,
            remainingSeconds = remainingSecs,
            status = SessionState.ACTIVE,
            blockedAttempts = blockedAttemptsCount,
            distractionsPrevented = distractionsPrevented,
            emergencyExits = 0,
            focusScore = 84,
            blockedAppsCount = 7,
            blockedWebsitesCount = 12
        )

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && remainingSeconds > 0 && sessionState == SessionState.ACTIVE) {
                delay(1000)
                remainingSeconds--
                currentSession = currentSession?.copy(
                    remainingSeconds = remainingSeconds
                )
                if (remainingSeconds <= 0) {
                    onTimerExpired()
                }
            }
        }
    }

    // Unified start focus method used by BOTH NFC and QR
    fun startFocus(
        triggerType: TriggerType,
        triggerIdentifier: String,
        policy: FocusPolicy,
        user: User = repository.currentUser
    ): Result<FocusSession> {
        // Permission check (local child protection check; bypassed for parent remote commands)
        if (triggerType != TriggerType.REMOTE_PARENT && !checkAndUpdatePermissions()) {
            return Result.failure(Exception("Focus Protection Disabled: Required Accessibility or Overlay permission is missing."))
        }

        // Server-side check
        val (canStart, reason) = repository.canStartFocus(user)
        if (!canStart) {
            return Result.failure(Exception(reason))
        }

        sessionState = SessionState.STARTING

        val totalSeconds = policy.durationMinutes * 60
        remainingSeconds = totalSeconds
        blockedAttemptsCount = 0
        distractionsPrevented = 0
        emergencyExitsCount = 0

        val newSession = FocusSession(
            id = "sess_${currentTimeMillis()}",
            userId = user.id,
            childName = user.name,
            policyId = policy.id,
            policyName = policy.name,
            mode = policy.mode,
            triggerType = triggerType,
            startTimeMillis = currentTimeMillis(),
            targetDurationMinutes = policy.durationMinutes,
            remainingSeconds = totalSeconds,
            status = SessionState.ACTIVE,
            blockedAttempts = 0,
            distractionsPrevented = 0,
            emergencyExits = 0,
            focusScore = 85,
            blockedAppsCount = policy.blockedApps.size,
            blockedWebsitesCount = policy.blockedWebsites.size
        )

        currentSession = newSession
        sessionState = SessionState.ACTIVE

        val resolvedPkgs = policy.blockedApps.map { com.focal.app.protection.ProtectionEngine.APP_NAME_TO_PACKAGE[it] ?: it }.toSet()
        com.focal.app.protection.ProtectionStateRepository.instance.updateFocusActive(
            active = true,
            endsAtElapsed = currentTimeMillis() + totalSeconds * 1000L,
            policy = com.focal.app.model.FocusPolicyData(
                blockedApps = policy.blockedApps,
                blockedPackages = resolvedPkgs,
                blockedWebsites = policy.blockedWebsites
            )
        )

        startTimer()

        // Push real-time telemetry to Cloud Sync Engine for Controller Device
        com.focal.app.data.FocalSyncEngine.instance.reportChildTelemetry(
            childId = user.id,
            isFocusActive = true,
            remainingSeconds = totalSeconds.toLong(),
            blockedAttempts = 0,
            lastAttemptedApp = null
        )

        // Notify controller
        repository.notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = "user_priya",
            type = "FOCUS_STARTED",
            title = "Focus Session Started",
            message = "${user.name} started '${policy.name}' (${policy.durationMinutes} min) via ${triggerType.name}.",
            timestamp = "Just now"
        ))

        // Log Activity Event
        com.focal.app.data.ActivityRepository.instance.recordEvent(
            type = com.focal.app.data.ActivityEventType.FOCUS_STARTED,
            childId = user.id,
            details = "${user.name} started '${policy.name}' (${policy.durationMinutes} min)",
            metadata = mapOf("policy" to policy.name, "trigger" to triggerType.name)
        )

        return Result.success(newSession)
    }

    // Start focus via NFC card tap (Family-wide card applies this child's defaults)
    fun startFocusWithNfc(cardIdentifier: String, user: User = repository.currentUser): Result<FocusSession> {
        val cardValidation = repository.validateNfcCardForChildStart(cardIdentifier, user)
        if (cardValidation.isFailure) {
            return Result.failure(cardValidation.exceptionOrNull() ?: Exception("This isn't your family's FOCAL card."))
        }
        val childProfile = repository.childrenList.find { it.userId == user.id || it.name.equals(user.name, ignoreCase = true) }
        val policyId = childProfile?.defaultPolicyId ?: "pol_study"
        val policy = repository.policies.find { it.id == policyId } ?: repository.policies.first()
        val durationMins = childProfile?.defaultFocusMinutes ?: 90
        val resolvedPolicy = policy.copy(durationMinutes = durationMins)
        return startFocus(
            triggerType = TriggerType.NFC,
            triggerIdentifier = cardIdentifier,
            policy = resolvedPolicy,
            user = user
        )
    }

    // Start focus via QR scan (Verifies single-use, 10-min expiry, child binding)
    fun startFocusWithQr(qrPayload: String, user: User = repository.currentUser): Result<FocusSession> {
        val qrValidation = repository.validateQrTokenForChildStart(qrPayload, user)
        if (qrValidation.isFailure) {
            return Result.failure(qrValidation.exceptionOrNull() ?: Exception("Invalid or expired Focus QR code."))
        }
        val (policy, durationMins) = qrValidation.getOrThrow()
        val customPolicy = policy.copy(durationMinutes = durationMins)
        return startFocus(
            triggerType = TriggerType.QR,
            triggerIdentifier = qrPayload,
            policy = customPolicy,
            user = user
        )
    }

    // End focus via NFC card tap (Family-wide card)
    fun endFocusWithNfc(cardIdentifier: String, user: User = repository.currentUser): Result<FocusSession> {
        val cardValidation = repository.validateNfcCardForChildEnd(cardIdentifier, user, currentSession)
        if (cardValidation.isFailure) {
            return Result.failure(cardValidation.exceptionOrNull() ?: Exception("This isn't your family's FOCAL card."))
        }
        return endFocus(
            triggerType = TriggerType.NFC,
            triggerIdentifier = cardIdentifier
        )
    }

    // Remote Start Focus (Parent Controller Action)
    fun remoteStartFocus(childId: String, policy: FocusPolicy, durationMinutes: Int): Result<FocusSession> {
        val child = repository.childrenList.find { it.id == childId }
        val targetUser = User(
            id = child?.userId ?: childId,
            name = child?.name ?: "Child",
            email = "",
            role = UserRole.CHILD
        )
        val customPolicy = policy.copy(durationMinutes = durationMinutes)
        val childIdx = repository.childrenList.indexOfFirst { it.id == childId }
        if (childIdx >= 0) {
            repository.childrenList[childIdx] = repository.childrenList[childIdx].copy(
                isFocusing = true,
                remainingMinutes = durationMinutes
            )
        }
        return startFocus(
            triggerType = TriggerType.REMOTE_PARENT,
            triggerIdentifier = "REMOTE_START_$childId",
            policy = customPolicy,
            user = targetUser
        )
    }

    // Remote Stop Focus (Parent Controller Action)
    fun remoteStopFocus(childId: String): Result<FocusSession> {
        val child = repository.childrenList.find { it.id == childId || it.userId == childId }
        val childIdx = repository.childrenList.indexOfFirst { it.id == childId || it.userId == childId }
        if (childIdx >= 0) {
            repository.childrenList[childIdx] = repository.childrenList[childIdx].copy(
                isFocusing = false,
                remainingMinutes = 0
            )
        }

        com.focal.app.data.FocalSyncEngine.instance.reportChildTelemetry(
            childId = child?.userId ?: childId,
            isFocusActive = false,
            remainingSeconds = 0L,
            blockedAttempts = 0,
            lastAttemptedApp = null
        )

        // Only terminate local session if it actually belongs to this child
        if (currentSession != null && (currentSession?.userId == childId || currentSession?.userId == child?.userId)) {
            return endFocus(
                triggerType = TriggerType.REMOTE_PARENT,
                triggerIdentifier = "REMOTE_STOP_$childId"
            )
        }

        val completed = FocusSession(
            id = "sess_remote_$childId",
            userId = child?.userId ?: childId,
            childName = child?.name ?: "Child",
            policyId = "pol_remote",
            policyName = "Focus",
            mode = FocusMode.STUDY,
            triggerType = TriggerType.REMOTE_PARENT,
            startTimeMillis = currentTimeMillis(),
            targetDurationMinutes = 0,
            remainingSeconds = 0,
            status = SessionState.COMPLETED
        )
        return Result.success(completed)
    }

    // End focus via parent-approved emergency exit
    fun endFocusWithEmergency(): Result<FocusSession> {
        emergencyExitsCount++
        val res = endFocus(TriggerType.EMERGENCY_OVERRIDE, "Parent Approved Emergency Exit")
        val childIdx = repository.childrenList.indexOfFirst { it.id == "child_1" || it.userId == repository.currentUser.id }
        if (childIdx >= 0) {
            repository.childrenList[childIdx] = repository.childrenList[childIdx].copy(
                isFocusing = false,
                remainingMinutes = 0
            )
        }
        return res
    }

    // End focus requires NFC tap or QR scan verification
    fun endFocus(
        triggerType: TriggerType,
        triggerIdentifier: String
    ): Result<FocusSession> {
        val session = currentSession ?: return Result.failure(Exception("No active focus session found."))

        if (triggerType == TriggerType.QR) {
            val trimmed = triggerIdentifier.trim()
            if (trimmed.startsWith("focal://pair", ignoreCase = true)) {
                return Result.failure(Exception("That's not a FOCAL focus QR"))
            }
            val childProfile = repository.childrenList.find { it.userId == session.userId || it.name.equals(session.userId, ignoreCase = true) }
            val userFamily = childProfile?.familyId ?: "family_1"
            val isFamilyValid = com.focal.app.data.FocusQrRepository.instance.validateFamilyQr(trimmed, userFamily)
            val isSingleUse = repository.qrTokens.any { it.token == trimmed }
            if (!isFamilyValid && !isSingleUse && !trimmed.startsWith("FOCAL_")) {
                return Result.failure(Exception("That's not a FOCAL focus QR"))
            }
        }

        sessionState = SessionState.ENDING
        timerJob?.cancel()

        // Push completion telemetry to Cloud Sync Engine
        com.focal.app.data.FocalSyncEngine.instance.reportChildTelemetry(
            childId = session.userId,
            isFocusActive = false,
            remainingSeconds = 0L,
            blockedAttempts = blockedAttemptsCount,
            lastAttemptedApp = null
        )

        val elapsedMinutes = (session.targetDurationMinutes - (remainingSeconds / 60)).coerceAtLeast(1)
        val score = (80 + (distractionsPrevented * 2) - (emergencyExitsCount * 10)).coerceIn(50, 99)

        val completedSession = session.copy(
            status = SessionState.COMPLETED,
            remainingSeconds = 0,
            blockedAttempts = blockedAttemptsCount,
            distractionsPrevented = distractionsPrevented,
            emergencyExits = emergencyExitsCount,
            focusScore = score
        )

        lastCompletedSummary = completedSession
        currentSession = null
        remainingSeconds = 0
        sessionState = SessionState.COMPLETED

        com.focal.app.protection.ProtectionStateRepository.instance.updateFocusActive(
            active = false,
            endsAtElapsed = 0L,
            policy = com.focal.app.model.FocusPolicyData()
        )

        // Notify controller
        repository.notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = "user_priya",
            type = "FOCUS_ENDED",
            title = "Focus Session Completed",
            message = "${session.childName} finished '${session.policyName}'. Focus Score: $score.",
            timestamp = "Just now"
        ))

        // Log Activity Event
        com.focal.app.data.ActivityRepository.instance.recordEvent(
            type = com.focal.app.data.ActivityEventType.FOCUS_ENDED,
            childId = session.userId,
            details = "${session.childName} finished '${session.policyName}'. Focus Score: $score.",
            metadata = mapOf("policy" to session.policyName, "score" to score.toString())
        )

        return Result.success(completedSession)
    }

    // Handle timer expiration
    private fun onTimerExpired() {
        val session = currentSession ?: return
        sessionState = SessionState.EXPIRED
        lastCompletedSummary = session.copy(
            status = SessionState.COMPLETED,
            remainingSeconds = 0,
            focusScore = 95
        )
        currentSession = null
        sessionState = SessionState.COMPLETED

        // Log Activity Event
        com.focal.app.data.ActivityRepository.instance.recordEvent(
            type = com.focal.app.data.ActivityEventType.FOCUS_COMPLETED,
            childId = session.userId,
            details = "${session.childName} completed '${session.policyName}' (${session.targetDurationMinutes} min)",
            metadata = mapOf("policy" to session.policyName, "duration" to session.targetDurationMinutes.toString())
        )

        com.focal.app.protection.ProtectionStateRepository.instance.updateFocusActive(
            active = false,
            endsAtElapsed = 0L,
            policy = com.focal.app.model.FocusPolicyData()
        )
    }

    // Trigger opening a blocked app / website in real native service or simulation
    fun triggerBlockedAppAttempt(appName: String) {
        if (sessionState == SessionState.ACTIVE) {
            blockedAttemptsCount++
            distractionsPrevented++
            blockedAppOverlayName = appName

            com.focal.app.data.ActivityRepository.instance.recordEvent(
                type = com.focal.app.data.ActivityEventType.APP_BLOCKED,
                childId = currentSession?.userId ?: "child_1",
                details = "Blocked $appName attempt during Focus",
                metadata = mapOf("app" to appName)
            )

            // Stream distraction event to Controller dashboard in real-time
            com.focal.app.data.FocalSyncEngine.instance.reportChildTelemetry(
                childId = currentSession?.userId ?: "child_1",
                isFocusActive = true,
                remainingSeconds = remainingSeconds.toLong(),
                blockedAttempts = blockedAttemptsCount,
                lastAttemptedApp = appName
            )
        }
    }

    // Simulate opening a blocked app (e.g. Instagram or YouTube)
    fun simulateBlockedAppLaunch(appName: String) {
        triggerBlockedAppAttempt(appName)
    }

    fun dismissBlockedOverlay() {
        blockedAppOverlayName = null
    }

    fun dismissSummary() {
        lastCompletedSummary = null
        sessionState = SessionState.IDLE
    }

    fun formatRemainingTime(): String {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val seconds = remainingSeconds % 60
        fun pad2(v: Int): String = if (v < 10) "0$v" else "$v"
        return "${pad2(hours)}:${pad2(minutes)}:${pad2(seconds)}"
    }

    val formattedTotalToday: String
        get() {
            val totalMins = 272 // 4h 32m
            return "${totalMins / 60}h ${totalMins % 60}m"
        }

    val progressFraction: Float
        get() {
            val target = (currentSession?.targetDurationMinutes ?: 160) * 60
            if (target <= 0) return 0.68f
            val elapsed = (target - remainingSeconds).coerceAtLeast(0)
            return (elapsed.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        }
}
