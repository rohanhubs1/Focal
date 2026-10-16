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
        todayFocusMinutes = repository.childrenList.find { it.id == "child_1" || it.userId == repository.currentUser.id }?.todayFocusMinutes ?: 135
        checkAndUpdatePermissions()
    }

    fun checkAndUpdatePermissions(): Boolean {
        val hasAccess = com.focal.app.ui.components.PlatformPermissionsHelper.isAccessibilityEnabled()
        val hasOverlay = com.focal.app.ui.components.PlatformPermissionsHelper.isOverlayEnabled()
        val hasAdmin = com.focal.app.ui.components.PlatformPermissionsHelper.isDeviceAdminActive()

        val missingList = mutableListOf<String>()
        if (!hasAccess) missingList.add("Accessibility")
        if (!hasOverlay) missingList.add("Display over other apps")
        if (!hasAdmin) missingList.add("Device admin")

        val isNowActive = missingList.isEmpty()
        val previousState = isProtectionActive
        isAccessibilityGranted = hasAccess
        isOverlayGranted = hasOverlay
        isProtectionActive = isNowActive
        missingPermissionsList = missingList
        lastHeartbeatTimeMillis = currentTimeMillis()

        val targetChildId = if (repository.currentUser.role == com.focal.app.model.UserRole.CHILD) repository.currentUser.id else "child_1"
        val childName = currentSession?.childName ?: repository.currentUser.name

        if (!isNowActive) {
            protectionStatusText = "Protection Disabled"
            if (previousState) {
                val permName = missingList.firstOrNull() ?: "Accessibility"

                com.focal.app.data.ActivityRepository.instance.recordEvent(
                    type = com.focal.app.data.ActivityEventType.PROTECTION_OFF,
                    childId = targetChildId,
                    details = "$permName was turned off",
                    metadata = mapOf("permission" to permName)
                )

                val notifMessage = "Focus Protection Disabled — ${childName}'s device has disabled $permName. FOCAL restrictions may not be fully enforced."

                repository.notifications.add(0, NotificationItem(
                    id = "notif_perm_${currentTimeMillis()}",
                    userId = "user_priya",
                    type = "PROTECTION_DISABLED",
                    title = "Focus Protection Disabled",
                    message = notifMessage,
                    timestamp = "Just now"
                ))

                com.focal.app.data.FocalSyncEngine.instance.reportProtectionStatus(
                    childId = targetChildId,
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
                    childId = targetChildId,
                    details = "All permissions on",
                    metadata = emptyMap()
                )

                com.focal.app.data.FocalSyncEngine.instance.reportProtectionStatus(
                    childId = targetChildId,
                    protectionStatus = "Protection Active",
                    isAccessibilityGranted = true,
                    isOverlayGranted = true
                )
            }

            // Immediately resume blocking if focus is currently active!
            if (sessionState == SessionState.ACTIVE) {
                val current = currentSession
                val endsAt = current?.startTimeMillis?.plus((current.targetDurationMinutes * 60 * 1000L))
                    ?: (currentTimeMillis() + remainingSeconds * 1000L)
                val policy = repository.policies.find { it.id == current?.policyId }
                val blockedApps = policy?.blockedApps ?: listOf(
                    "Instagram", "YouTube", "TikTok", "Amazon", "BGMI", "Chrome", "Snapchat", "Netflix", "Roblox", "Clash of Clans"
                )
                val blockedWebsites = policy?.blockedWebsites ?: emptyList()
                val resolvedPkgs = blockedApps.map { com.focal.app.protection.ProtectionEngine.APP_NAME_TO_PACKAGE[it] ?: it }.toSet()

                com.focal.app.protection.ProtectionStateRepository.instance.updateFocusActive(
                    active = true,
                    endsAtElapsed = endsAt,
                    policy = com.focal.app.model.FocusPolicyData(
                        blockedApps = blockedApps,
                        blockedPackages = resolvedPkgs,
                        blockedWebsites = blockedWebsites
                    )
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
            var clockChangeLogged = false
            while (isActive && sessionState == SessionState.ACTIVE) {
                val session = currentSession ?: break
                val nowElapsed = com.focal.app.streak.getDeviceElapsedRealtime()
                val nowWall = currentTimeMillis()

                // Check after reboot: elapsedRealtime resets to near 0
                if (session.startElapsedRealtime > 0L && nowElapsed < session.startElapsedRealtime) {
                    val wallRemaining = session.endTimeMillis - nowWall
                    if (wallRemaining <= 0L) {
                        // Clock jumped forward or past remaining time after reboot: keep focus active and log event
                        if (!clockChangeLogged) {
                            clockChangeLogged = true
                            com.focal.app.data.ActivityRepository.instance.recordEvent(
                                type = com.focal.app.data.ActivityEventType.CLOCK_CHANGED,
                                childId = session.userId,
                                details = "Device clock was changed during active focus session",
                                metadata = mapOf("reason" to "Clock jump detected after reboot")
                            )
                        }
                        // Re-anchor to current elapsed and keep remaining active
                        val keepDurationMs = (session.targetDurationMinutes * 60 * 1000L).coerceAtLeast(300_000L)
                        val newEndsAt = nowWall + keepDurationMs
                        currentSession = session.copy(
                            startElapsedRealtime = nowElapsed,
                            startTimeMillis = nowWall,
                            endTimeMillis = newEndsAt
                        )
                    }
                } else if (session.startElapsedRealtime > 0L && nowElapsed >= session.startElapsedRealtime) {
                    // Check if wall clock jumped forward while running (date/time manually adjusted)
                    val actualElapsed = nowElapsed - session.startElapsedRealtime
                    val wallElapsed = nowWall - session.startTimeMillis
                    if (wallElapsed - actualElapsed > 60_000L && !clockChangeLogged) {
                        clockChangeLogged = true
                        com.focal.app.data.ActivityRepository.instance.recordEvent(
                            type = com.focal.app.data.ActivityEventType.CLOCK_CHANGED,
                            childId = session.userId,
                            details = "Device clock was changed during active focus session",
                            metadata = mapOf("reason" to "Clock adjustment detected")
                        )
                    }
                }

                val remainingMs = session.getRemainingMillis(nowElapsed, nowWall)
                remainingSeconds = (remainingMs / 1000L).toInt()
                currentSession = session.copy(remainingSeconds = remainingSeconds)
                if (remainingMs <= 0L) {
                    onTimerExpired()
                    break
                }
                val delayTime = (1000 - nowElapsed % 1000).coerceIn(100L, 1000L)
                delay(delayTime)
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

        val now = currentTimeMillis()
        val nowElapsed = com.focal.app.streak.getDeviceElapsedRealtime()
        val endsAt = now + totalSeconds * 1000L
        val newSession = FocusSession(
            id = "sess_${now}",
            userId = user.id,
            childName = user.name,
            policyId = policy.id,
            policyName = policy.name,
            mode = policy.mode,
            triggerType = triggerType,
            startTimeMillis = now,
            targetDurationMinutes = policy.durationMinutes,
            remainingSeconds = totalSeconds,
            status = SessionState.ACTIVE,
            blockedAttempts = 0,
            distractionsPrevented = 0,
            emergencyExits = 0,
            focusScore = 85,
            blockedAppsCount = policy.blockedApps.size,
            blockedWebsitesCount = policy.blockedWebsites.size,
            endTimeMillis = endsAt,
            startElapsedRealtime = nowElapsed
        )

        currentSession = newSession
        sessionState = SessionState.ACTIVE

        // Persist session timing locally
        com.focal.app.data.FocalLocalStorage.saveLong("KEY_FOCUS_ENDS_AT_MILLIS", endsAt)
        com.focal.app.data.FocalLocalStorage.saveLong("KEY_FOCUS_START_ELAPSED", nowElapsed)
        com.focal.app.data.FocalLocalStorage.saveLong("KEY_FOCUS_START_MILLIS", now)

        // Update child profile in parent repository
        val childIdx = repository.childrenList.indexOfFirst { it.userId == user.id || it.id == "child_1" || it.name.equals(user.name, ignoreCase = true) }
        if (childIdx >= 0) {
            repository.childrenList[childIdx] = repository.childrenList[childIdx].copy(
                isFocusing = true,
                focusEndsAtMillis = endsAt,
                focusTotalMinutes = policy.durationMinutes,
                remainingMinutes = policy.durationMinutes
            )
        }

        val resolvedPkgs = policy.blockedApps.map { com.focal.app.protection.ProtectionEngine.APP_NAME_TO_PACKAGE[it] ?: it }.toSet()
        com.focal.app.protection.ProtectionStateRepository.instance.updateFocusActive(
            active = true,
            endsAtElapsed = endsAt,
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

        val endedAt = currentTimeMillis()
        val actualDurationMs = (endedAt - session.startTimeMillis).coerceAtLeast(0L)
        val elapsedMinutes = (actualDurationMs / 60000L).toInt()

        // Accumulate actual focused time today
        todayFocusMinutes += elapsedMinutes

        // Push completion telemetry to Cloud Sync Engine
        com.focal.app.data.FocalSyncEngine.instance.reportChildTelemetry(
            childId = session.userId,
            isFocusActive = false,
            remainingSeconds = 0L,
            blockedAttempts = blockedAttemptsCount,
            lastAttemptedApp = null
        )

        val score = (80 + (distractionsPrevented * 2) - (emergencyExitsCount * 10)).coerceIn(50, 99)

        val completedSession = session.copy(
            status = SessionState.COMPLETED,
            remainingSeconds = 0,
            blockedAttempts = blockedAttemptsCount,
            distractionsPrevented = distractionsPrevented,
            emergencyExits = emergencyExitsCount,
            focusScore = score,
            endedAtMillis = endedAt
        )

        lastCompletedSummary = completedSession
        currentSession = null
        remainingSeconds = 0
        sessionState = SessionState.COMPLETED

        com.focal.app.data.FocalLocalStorage.remove("KEY_FOCUS_ENDS_AT_MILLIS")
        com.focal.app.data.FocalLocalStorage.remove("KEY_FOCUS_START_ELAPSED")
        com.focal.app.data.FocalLocalStorage.remove("KEY_FOCUS_START_MILLIS")

        val childIdx = repository.childrenList.indexOfFirst { it.userId == session.userId || it.id == "child_1" || it.name.equals(session.childName, ignoreCase = true) }
        if (childIdx >= 0) {
            repository.childrenList[childIdx] = repository.childrenList[childIdx].copy(
                isFocusing = false,
                focusEndsAtMillis = 0L,
                remainingMinutes = 0,
                todayFocusMinutes = todayFocusMinutes
            )
        }

        if (triggerType != TriggerType.EMERGENCY_OVERRIDE) {
            repository.expirePendingEmergencyRequests(session.userId)
        }

        com.focal.app.protection.ProtectionStateRepository.instance.updateFocusActive(
            active = false,
            endsAtElapsed = 0L,
            policy = com.focal.app.model.FocusPolicyData()
        )

        // Record streak session
        val streakReason = when (triggerType) {
            TriggerType.TIMER_EXPIRED -> com.focal.app.streak.StreakEndReason.TIMER_FINISHED
            TriggerType.SCHEDULED -> com.focal.app.streak.StreakEndReason.SCHEDULE_ENDED
            TriggerType.REMOTE_PARENT -> com.focal.app.streak.StreakEndReason.PARENT_REMOTE_ENDED
            TriggerType.NFC, TriggerType.QR -> com.focal.app.streak.StreakEndReason.CARD_EARLY
            TriggerType.EMERGENCY_OVERRIDE -> com.focal.app.streak.StreakEndReason.EMERGENCY_EXIT
            else -> com.focal.app.streak.StreakEndReason.CARD_EARLY
        }
        com.focal.app.streak.StreakRepository.instance.addSession(
            com.focal.app.streak.StreakSessionRecord(
                id = completedSession.id,
                startTimeMillis = session.startTimeMillis,
                endTimeMillis = endedAt,
                targetDurationMinutes = session.targetDurationMinutes,
                endReason = streakReason,
                remainingSecondsAtEnd = 0
            )
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

    // Handle timer expiration (uses the exact same code path as End Focus)
    private fun onTimerExpired() {
        endFocus(TriggerType.TIMER_EXPIRED, "TIMER_EXPIRED")
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
            if (target <= 0) return 0f
            return (remainingSeconds.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        }
}
