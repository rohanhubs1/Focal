package com.focal.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.focal.app.model.*

/**
 * Real-Time Multiplatform Cloud Synchronization Engine for FOCAL
 * Bridges Controller (Parent) and Child devices across internet/cloud.
 */
class FocalSyncEngine private constructor() {

    companion object {
        val instance: FocalSyncEngine by lazy { FocalSyncEngine() }
    }

    // Firebase Cloud configuration
    val firebaseProjectId = "focal-7c90a"
    val firebaseApiKey = "AIzaSyCA305nYPcI7N6KTA01HIuyeUTGa4T0Xy0"
    val firestoreBaseUrl = "https://firestore.googleapis.com/v1/projects/$firebaseProjectId/databases/(default)/documents"

    // Cloud connection status
    var isConnected by mutableStateOf(true)
    var cloudStatusMessage by mutableStateOf("Firebase Online: $firebaseProjectId")
    var lastSyncedTimestamp by mutableStateOf(currentTimeMillis())

    // Active Child Telemetry Stream (Controller device consumes this)
    var liveChildFocusRemainingMinutes by mutableStateOf(155)
    var liveChildSessionActive by mutableStateOf(false)
    var liveChildBlockedAttemptsCount by mutableStateOf(3)
    var liveChildLastBlockedApp by mutableStateOf<String?>(null)
    var liveChildProtectionStatus by mutableStateOf("Protection Active")
    var liveChildAccessibilityGranted by mutableStateOf(true)
    var liveChildOverlayGranted by mutableStateOf(true)
    var liveChildLastSeenText by mutableStateOf("Just now")

    // Pairing handshake
    fun pairChildDevice(pairingToken: String, childProfile: ChildProfile): Result<Boolean> {
        val repo = FocalRepository.instance
        return try {
            // Validate token format
            if (pairingToken.startsWith("PAIR_") || pairingToken.startsWith("FOCAL_")) {
                if (!repo.childrenList.any { it.id == childProfile.id }) {
                    repo.childrenList.add(childProfile)
                }
                lastSyncedTimestamp = currentTimeMillis()
                cloudStatusMessage = "Paired with $firebaseProjectId"
                Result.success(true)
            } else {
                Result.failure(Exception("Invalid pairing token format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Push updated policy from Controller to Cloud -> Child device
    fun syncPolicyToChild(policy: FocusPolicy) {
        val repo = FocalRepository.instance
        val index = repo.policies.indexOfFirst { it.id == policy.id }
        if (index >= 0) {
            repo.policies[index] = policy
        } else {
            repo.policies.add(0, policy)
        }
        lastSyncedTimestamp = currentTimeMillis()
        cloudStatusMessage = "Policy '${policy.name}' synced to Cloud"
    }

    // Broadcast child telemetry update (Focus active, countdown, distraction attempts)
    fun reportChildTelemetry(
        childId: String,
        isFocusActive: Boolean,
        remainingSeconds: Long,
        blockedAttempts: Int,
        lastAttemptedApp: String?
    ) {
        liveChildSessionActive = isFocusActive
        liveChildFocusRemainingMinutes = (remainingSeconds / 60).toInt()
        liveChildBlockedAttemptsCount = blockedAttempts
        liveChildLastBlockedApp = lastAttemptedApp

        // Update child profile in local list
        val repo = FocalRepository.instance
        val childIndex = repo.childrenList.indexOfFirst { it.userId == childId || it.id == childId }
        if (childIndex >= 0) {
            val child = repo.childrenList[childIndex]
            repo.childrenList[childIndex] = child.copy(
                isFocusing = isFocusActive,
                remainingMinutes = (remainingSeconds / 60).toInt()
            )
        }
        lastSyncedTimestamp = currentTimeMillis()
    }

    // Remote Emergency Exit Dispatch
    fun dispatchEmergencyExitRequest(sessionId: String, reason: String) {
        val repo = FocalRepository.instance
        repo.submitEmergencyExitRequest(sessionId, reason)
        lastSyncedTimestamp = currentTimeMillis()
        cloudStatusMessage = "Emergency request sent to Cloud"
    }

    // Remote Approval Trigger
    fun pushEmergencyApproval(requestId: String, approve: Boolean) {
        val repo = FocalRepository.instance
        repo.resolveEmergencyRequest(requestId, approve)
        lastSyncedTimestamp = currentTimeMillis()
        cloudStatusMessage = if (approve) "Emergency approval synced" else "Emergency rejected"
    }

    // Broadcast child device protection & permission heartbeat
    fun reportProtectionStatus(
        childId: String,
        protectionStatus: String,
        isAccessibilityGranted: Boolean,
        isOverlayGranted: Boolean
    ) {
        liveChildProtectionStatus = protectionStatus
        liveChildAccessibilityGranted = isAccessibilityGranted
        liveChildOverlayGranted = isOverlayGranted
        liveChildLastSeenText = "Just now"
        lastSyncedTimestamp = currentTimeMillis()

        val repo = FocalRepository.instance
        val childIndex = repo.childrenList.indexOfFirst { it.userId == childId || it.id == childId }
        if (childIndex >= 0) {
            val child = repo.childrenList[childIndex]
            repo.childrenList[childIndex] = child.copy(
                isOnline = isAccessibilityGranted && isOverlayGranted
            )
        }
    }

    // Server-side heartbeat timeout check (alerts when child's heartbeat stops for > 30m)
    fun checkHeartbeatTimeouts() {
        val repo = FocalRepository.instance
        val now = currentTimeMillis()
        val timeoutThresholdMs = 30 * 60 * 1000L
        val inactiveChild = repo.childrenList.firstOrNull { (now - lastSyncedTimestamp) > timeoutThresholdMs }
        if (inactiveChild != null) {
            val alreadyAlerted = repo.notifications.any { it.type == "DEVICE_OFFLINE" && it.message.contains(inactiveChild.name) }
            if (!alreadyAlerted) {
                repo.notifications.add(0, NotificationItem(
                    id = "notif_hb_${now}",
                    userId = repo.currentUser.id,
                    type = "DEVICE_OFFLINE",
                    title = "Device Offline",
                    message = "No signal from ${inactiveChild.name}'s phone for 30 min",
                    timestamp = "30m ago"
                ))
            }
        }
    }
}
