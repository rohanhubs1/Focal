package com.focal.app.data.firebase

import com.focal.app.data.*
import com.focal.app.model.*

/**
 * Firebase Repository Stubs — Compile-ready production placeholders mapping directly
 * to Firestore schemas and Cloud Functions defined in docs/03_DATA_MODEL.md & docs/04_BACKEND.md.
 * Real Firebase SDK bindings load dynamically when google-services.json is provisioned.
 */

// 1. AuthRepository Implementation Stub
class FirebaseAuthRepository : AuthRepository {
    override suspend fun signIn(email: String, password: String): Result<User> {
        // TODO (docs/04_BACKEND.md Section 1): Call Firebase Auth signInWithEmailAndPassword(email, password)
        return Result.failure(NotImplementedError("Firebase auth not configured. Provide google-services.json to activate."))
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<User> {
        // TODO (docs/04_BACKEND.md Section 1): Call Firebase Auth createUserWithEmailAndPassword, then write /parents/{uid}
        return Result.failure(NotImplementedError("Firebase auth not configured. Provide google-services.json to activate."))
    }

    override suspend fun signOut() {
        // TODO (docs/04_BACKEND.md Section 1): Call Firebase Auth signOut()
    }

    override fun getCurrentUser(): User? {
        // TODO (docs/04_BACKEND.md Section 1): Return User mapped from FirebaseAuth.currentUser
        return null
    }
}

// 2. ChildRepository Implementation Stub
class FirebaseChildRepository : ChildRepository {
    override suspend fun getChildren(parentId: String): List<ChildProfile> {
        // TODO (docs/03_DATA_MODEL.md): Query Firestore collection 'children' where parentId == parentId
        return emptyList()
    }

    override suspend fun getChildById(childId: String): ChildProfile? {
        // TODO (docs/03_DATA_MODEL.md): Read document /children/{childId}
        return null
    }

    override suspend fun addChild(parentId: String, name: String, age: Int): Result<ChildProfile> {
        // TODO (docs/03_DATA_MODEL.md): Write new document to /children/{childId}
        return Result.failure(NotImplementedError("Firebase backend pending."))
    }

    override suspend fun updateChild(child: ChildProfile): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Update document /children/{child.id}
        return Result.success(Unit)
    }

    override suspend fun removeChild(childId: String): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Delete document /children/{childId}
        return Result.success(Unit)
    }

    override suspend fun createPairingCode(parentId: String, childName: String, age: Int): Result<PairingCodeResult> {
        // TODO (docs/04_BACKEND.md Section 2.1): Call Cloud Function 'createPairingCode'
        return Result.failure(NotImplementedError("Cloud Function 'createPairingCode' pending credentials."))
    }

    override suspend fun redeemPairingCode(pairingCode: String, deviceHardwareKey: String): Result<ChildAuthTokenResult> {
        // TODO (docs/04_BACKEND.md Section 2.2): Call Cloud Function 'redeemPairingCode'
        return Result.failure(NotImplementedError("Cloud Function 'redeemPairingCode' pending credentials."))
    }
}

// 3. DeviceRepository Implementation Stub
class FirebaseDeviceRepository : DeviceRepository {
    override suspend fun getDevices(parentId: String): List<AdminDeviceItem> {
        // TODO (docs/03_DATA_MODEL.md): Query collection 'devices' where parentId == parentId
        return emptyList()
    }

    override suspend fun updateDeviceTelemetry(deviceId: String, telemetry: Map<String, Any>): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md & docs/05_COSTS.md): Batch update document /devices/{deviceId}
        return Result.success(Unit)
    }

    override suspend fun resetDevicePairing(childId: String): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Update child and device document pairing status
        return Result.success(Unit)
    }
}

// 4. CardRepository Implementation Stub
class FirebaseCardRepository : CardRepository {
    override suspend fun getCards(parentId: String?): List<FocusCard> {
        // TODO (docs/03_DATA_MODEL.md): Query collection 'cards' where ownerParentId == parentId
        return emptyList()
    }

    override suspend fun claimCard(parentId: String, token: String, childId: String?): Result<FocusCard> {
        // TODO (docs/04_BACKEND.md Section 2.3): Call Cloud Function 'claimCard'
        return Result.failure(NotImplementedError("Cloud Function 'claimCard' pending credentials."))
    }

    override suspend fun assignCard(cardId: String, childId: String): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Call Cloud Function or update card assignedChildId
        return Result.success(Unit)
    }

    override suspend fun unpairCard(cardId: String): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Reset assignedChildId on card document
        return Result.success(Unit)
    }

    override suspend fun revokeCard(cardId: String): Result<Unit> {
        // TODO (docs/04_BACKEND.md Section 2.4): Call Cloud Function 'revokeCard'
        return Result.failure(NotImplementedError("Cloud Function 'revokeCard' pending credentials."))
    }

    override suspend fun programBatchCards(count: Int): Result<List<FocusCard>> {
        // TODO (docs/04_BACKEND.md Section 2.5): Call Cloud Function 'programCards'
        return Result.failure(NotImplementedError("Cloud Function 'programCards' pending credentials."))
    }
}

// 5. PolicyRepository Implementation Stub
class FirebasePolicyRepository : PolicyRepository {
    override suspend fun getPolicies(childId: String): List<FocusPolicy> {
        // TODO (docs/03_DATA_MODEL.md): Query collection 'policies' where childId == childId
        return emptyList()
    }

    override suspend fun getPolicyById(policyId: String): FocusPolicy? {
        // TODO (docs/03_DATA_MODEL.md): Read document /policies/{policyId}
        return null
    }

    override suspend fun savePolicy(policy: FocusPolicy): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Write document to /policies/{policy.id}
        return Result.success(Unit)
    }

    override suspend fun deletePolicy(policyId: String): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Delete document /policies/{policyId}
        return Result.success(Unit)
    }

    override fun normalizeDomain(urlOrDomain: String): String {
        var clean = urlOrDomain.trim().lowercase()
        if (clean.startsWith("https://")) clean = clean.substring(8)
        if (clean.startsWith("http://")) clean = clean.substring(7)
        if (clean.startsWith("www.")) clean = clean.substring(4)
        val slashIdx = clean.indexOf('/')
        if (slashIdx != -1) clean = clean.substring(0, slashIdx)
        val colonIdx = clean.indexOf(':')
        if (colonIdx != -1) clean = clean.substring(0, colonIdx)
        return clean
    }

    override fun isDomainBlocked(targetUrlOrDomain: String, blockedDomains: List<String>): Boolean {
        val cleanTarget = normalizeDomain(targetUrlOrDomain)
        if (cleanTarget.isEmpty()) return false
        return blockedDomains.any { raw ->
            val cleanBlocked = normalizeDomain(raw)
            cleanBlocked.isNotEmpty() && (cleanTarget == cleanBlocked || cleanTarget.endsWith(".$cleanBlocked"))
        }
    }
}

// 6. ScheduleRepository Implementation Stub
class FirebaseScheduleRepository : ScheduleRepository {
    override suspend fun getSchedules(childId: String): List<ScheduleItem> {
        // TODO (docs/03_DATA_MODEL.md): Query collection 'schedules' where childId == childId
        return emptyList()
    }

    override suspend fun saveSchedule(schedule: ScheduleItem): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Write document to /schedules/{schedule.id}
        return Result.success(Unit)
    }

    override suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Delete document /schedules/{scheduleId}
        return Result.success(Unit)
    }
}

// 7. SessionRepository Implementation Stub
class FirebaseSessionRepository : SessionRepository {
    override suspend fun getActiveSession(childId: String): FocusSession? {
        // TODO (docs/03_DATA_MODEL.md): Query collection 'sessions' where childId == childId and status == 'ACTIVE'
        return null
    }

    override suspend fun startSession(childId: String, policyId: String, durationMinutes: Int, trigger: String): Result<FocusSession> {
        // TODO (docs/03_DATA_MODEL.md): Write new document to /sessions/{sessionId}
        return Result.failure(NotImplementedError("Firebase backend pending."))
    }

    override suspend fun stopSession(sessionId: String, childId: String): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Update document /sessions/{sessionId} status to 'COMPLETED'
        return Result.success(Unit)
    }

    override suspend fun createFocusQr(childId: String, policyId: String, durationMinutes: Int): Result<String> {
        // TODO (docs/04_BACKEND.md Section 2.6): Call Cloud Function 'createFocusQr'
        return Result.failure(NotImplementedError("Cloud Function 'createFocusQr' pending credentials."))
    }

    override suspend fun validateFocusQr(qrToken: String): Result<FocusPolicy> {
        // TODO (docs/04_BACKEND.md Section 2.7): Call Cloud Function 'validateFocusQr'
        return Result.failure(NotImplementedError("Cloud Function 'validateFocusQr' pending credentials."))
    }

    override suspend fun startRemoteSession(childId: String, policyId: String, durationMinutes: Int): Result<FocusSession> {
        // TODO (docs/04_BACKEND.md Section 2.8): Call Cloud Function 'startRemoteSession'
        return Result.failure(NotImplementedError("Cloud Function 'startRemoteSession' pending credentials."))
    }

    override suspend fun endRemoteSession(sessionId: String, childId: String): Result<Unit> {
        // TODO (docs/04_BACKEND.md Section 2.9): Call Cloud Function 'endRemoteSession'
        return Result.success(Unit)
    }
}

// 8. EventRepository Implementation Stub
class FirebaseEventRepository : EventRepository {
    override suspend fun logEvent(childId: String, deviceId: String, eventType: String, details: Map<String, String>): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Write document to /events/{eventId}
        return Result.success(Unit)
    }

    override suspend fun getEvents(childId: String, limit: Int): List<Map<String, Any>> {
        // TODO (docs/03_DATA_MODEL.md): Query collection 'events' where childId == childId order by timestamp desc
        return emptyList()
    }
}

// 9. UsageRepository Implementation Stub
class FirebaseUsageRepository : UsageRepository {
    override suspend fun getDailyUsage(childId: String, date: String): Map<String, Any> {
        // TODO (docs/03_DATA_MODEL.md): Read document /usageDaily/{childId}_{date}
        return emptyMap()
    }

    override suspend fun reportUsage(childId: String, date: String, screenTimeMinutes: Int, focusMinutes: Int, appBreakdown: Map<String, Int>): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md & docs/05_COSTS.md): Set document /usageDaily/{childId}_{date}
        return Result.success(Unit)
    }
}

// 10. NotificationRepository Implementation Stub
class FirebaseNotificationRepository : NotificationRepository {
    override suspend fun getNotifications(userId: String): List<NotificationItem> {
        // TODO (docs/03_DATA_MODEL.md): Query collection 'notifications' where recipientUserId == userId
        return emptyList()
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Update document /notifications/{notificationId} isRead = true
        return Result.success(Unit)
    }

    override suspend fun handleEmergencyRequest(action: String, childId: String, sessionId: String, reason: String?): Result<Unit> {
        // TODO (docs/04_BACKEND.md Section 2.10): Call Cloud Function 'handleEmergencyRequest'
        return Result.success(Unit)
    }
}

// 11. AdminRepository Implementation Stub
class FirebaseAdminRepository : AdminRepository {
    override suspend fun getAdminStats(): Result<Map<String, Any>> {
        // TODO (docs/04_BACKEND.md Section 2.12): Call Cloud Function 'adminStats'
        return Result.failure(NotImplementedError("Cloud Function 'adminStats' pending credentials."))
    }

    override suspend fun getParentAccounts(): List<ParentAccount> {
        // TODO (docs/03_DATA_MODEL.md): Query collection 'parents'
        return emptyList()
    }

    override suspend fun extendTrial(userId: String, days: Int): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Update document /parents/{userId} trialEndsAt
        return Result.success(Unit)
    }

    override suspend fun suspendAccount(userId: String): Result<Unit> {
        // TODO (docs/03_DATA_MODEL.md): Update document /parents/{userId} subscriptionStatus = 'suspended'
        return Result.success(Unit)
    }

    override suspend fun getAuditLogs(): List<AdminAuditLog> {
        // TODO (docs/03_DATA_MODEL.md): Query collection 'adminAudit' order by timestamp desc
        return emptyList()
    }
}
