package com.focal.app.data

import com.focal.app.model.*

// -------------------------------------------------------------
// 1. AUTH REPOSITORY INTERFACE
// -------------------------------------------------------------
interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(name: String, email: String, password: String): Result<User>
    suspend fun signInWithGoogle(): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signOut()
    fun getCurrentUser(): User?

    companion object {
        val instance: AuthRepository by lazy { LocalAuthRepository() }
    }
}

// -------------------------------------------------------------
// 2. CHILD REPOSITORY INTERFACE
// -------------------------------------------------------------
interface ChildRepository {
    suspend fun getChildren(parentId: String): List<ChildProfile>
    suspend fun getChildById(childId: String): ChildProfile?
    suspend fun addChild(parentId: String, name: String, age: Int): Result<ChildProfile>
    suspend fun updateChild(child: ChildProfile): Result<Unit>
    suspend fun removeChild(childId: String): Result<Unit>
    suspend fun createPairingCode(parentId: String, childName: String, age: Int): Result<PairingCodeResult>
    suspend fun redeemPairingCode(pairingCode: String, deviceHardwareKey: String): Result<ChildAuthTokenResult>
}

data class PairingCodeResult(
    val codeId: String,
    val code: String,
    val qrPayload: String,
    val expiresAt: String
)

data class ChildAuthTokenResult(
    val customToken: String,
    val childId: String,
    val deviceId: String,
    val parentId: String
)

// -------------------------------------------------------------
// 3. DEVICE REPOSITORY INTERFACE
// -------------------------------------------------------------
interface DeviceRepository {
    suspend fun getDevices(parentId: String): List<AdminDeviceItem>
    suspend fun updateDeviceTelemetry(deviceId: String, telemetry: Map<String, Any>): Result<Unit>
    suspend fun resetDevicePairing(childId: String): Result<Unit>
}

// -------------------------------------------------------------
// 4. CARD REPOSITORY INTERFACE
// -------------------------------------------------------------
interface CardRepository {
    suspend fun getCards(parentId: String?): List<FocusCard>
    suspend fun claimCard(parentId: String, token: String, childId: String?): Result<FocusCard>
    suspend fun assignCard(cardId: String, childId: String): Result<Unit>
    suspend fun unpairCard(cardId: String): Result<Unit>
    suspend fun revokeCard(cardId: String): Result<Unit>
    suspend fun programBatchCards(count: Int): Result<List<FocusCard>>
}

// -------------------------------------------------------------
// 5. POLICY REPOSITORY INTERFACE
// -------------------------------------------------------------
interface PolicyRepository {
    suspend fun getPolicies(childId: String): List<FocusPolicy>
    suspend fun getPolicyById(policyId: String): FocusPolicy?
    suspend fun savePolicy(policy: FocusPolicy): Result<Unit>
    suspend fun deletePolicy(policyId: String): Result<Unit>
    fun normalizeDomain(urlOrDomain: String): String
    fun isDomainBlocked(targetUrlOrDomain: String, blockedDomains: List<String>): Boolean
}

// -------------------------------------------------------------
// 6. SCHEDULE REPOSITORY INTERFACE
// -------------------------------------------------------------
interface ScheduleRepository {
    suspend fun getSchedules(childId: String): List<ScheduleItem>
    suspend fun saveSchedule(schedule: ScheduleItem): Result<Unit>
    suspend fun deleteSchedule(scheduleId: String): Result<Unit>
}

// -------------------------------------------------------------
// 7. SESSION REPOSITORY INTERFACE
// -------------------------------------------------------------
interface SessionRepository {
    suspend fun getActiveSession(childId: String): FocusSession?
    suspend fun startSession(childId: String, policyId: String, durationMinutes: Int, trigger: String): Result<FocusSession>
    suspend fun stopSession(sessionId: String, childId: String): Result<Unit>
    suspend fun createFocusQr(childId: String, policyId: String, durationMinutes: Int): Result<String>
    suspend fun validateFocusQr(qrToken: String): Result<FocusPolicy>
    suspend fun startRemoteSession(childId: String, policyId: String, durationMinutes: Int): Result<FocusSession>
    suspend fun endRemoteSession(sessionId: String, childId: String): Result<Unit>
}

// -------------------------------------------------------------
// 8. EVENT REPOSITORY INTERFACE
// -------------------------------------------------------------
interface EventRepository {
    suspend fun logEvent(childId: String, deviceId: String, eventType: String, details: Map<String, String>): Result<Unit>
    suspend fun getEvents(childId: String, limit: Int = 50): List<Map<String, Any>>
}

// -------------------------------------------------------------
// 9. USAGE REPOSITORY INTERFACE
// -------------------------------------------------------------
interface UsageRepository {
    suspend fun getDailyUsage(childId: String, date: String): Map<String, Any>
    suspend fun reportUsage(childId: String, date: String, screenTimeMinutes: Int, focusMinutes: Int, appBreakdown: Map<String, Int>): Result<Unit>
}

// -------------------------------------------------------------
// 10. NOTIFICATION REPOSITORY INTERFACE
// -------------------------------------------------------------
interface NotificationRepository {
    suspend fun getNotifications(userId: String): List<NotificationItem>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun handleEmergencyRequest(action: String, childId: String, sessionId: String, reason: String?): Result<Unit>
}

// -------------------------------------------------------------
// 11. ADMIN REPOSITORY INTERFACE
// -------------------------------------------------------------
interface AdminRepository {
    suspend fun getAdminStats(): Result<Map<String, Any>>
    suspend fun getParentAccounts(): List<ParentAccount>
    suspend fun extendTrial(userId: String, days: Int): Result<Unit>
    suspend fun suspendAccount(userId: String): Result<Unit>
    suspend fun getAuditLogs(): List<AdminAuditLog>
}
