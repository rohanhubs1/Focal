package com.focal.app.data

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.focal.app.model.*

data class PolicyUsage(
    val children: List<ChildProfile>,
    val schedules: List<ScheduleItem>
) {
    val isUsed: Boolean get() = children.isNotEmpty() || schedules.isNotEmpty()
}

interface FocalRepository {
    var currentUser: User
    val adminUser: User
    var controllerUser: User
    val childRahul: User
    val childAnanya: User
    var parentPin: String
    val childInstalledApps: SnapshotStateList<InstalledApp>
    val policies: SnapshotStateList<FocusPolicy>
    val cards: SnapshotStateList<FocusCard>
    val nfcCards: SnapshotStateList<FocusCard>
    val qrTokens: SnapshotStateList<QrToken>
    val emergencyRequests: SnapshotStateList<EmergencyExitRequest>
    val schedules: SnapshotStateList<ScheduleItem>
    val friends: SnapshotStateList<FriendUser>
    val notifications: SnapshotStateList<NotificationItem>
    val childrenList: SnapshotStateList<ChildProfile>
    val children: List<ChildProfile>
    val parentAccounts: SnapshotStateList<ParentAccount>
    val auditLogs: SnapshotStateList<AdminAuditLog>
    val adminDevices: SnapshotStateList<AdminDeviceItem>
    var parentUnlockedUntilMs: Long
    var showParentUnlockScreen: Boolean
    var requestNavigateToChildHomeCounter: Int
    var isProtectionDisabledOnChild: Boolean
    var protectionDisabledReason: String
    var lastHeartbeatTimeMs: Long
    var lastHeartbeatBatteryLevel: Int
    var lastOfflineAlertSentTimeMs: Long
    var isDeviceOfflineAlertActive: Boolean
    var childGuardMode: String
    var isSoloStrictModeEnabled: Boolean
    val topDistractions: List<DistractionAppStat>
    val dailyFocusHistory: List<DailyFocusBar>
    var totalUsersCount: Int
    var nfcCustomersCount: Int
    var qrCustomersCount: Int
    var activeTrialsCount: Int
    var activeSubscriptionsCount: Int
    var expiredSubscriptionsCount: Int
    var monthlyRevenueInr: Int
    var nfcCardSalesInr: Int
    var activeFocusSessionsCount: Int
    var totalFocusHoursCount: Int
    var onUnlinkedFromParent: (() -> Unit)?

    fun updateParentName(newName: String)
    fun updateParentEmail(newEmail: String)
    fun updateParentPin(newPin: String)
    fun renameCard(cardId: String, newLabel: String)
    fun removeCard(cardId: String)
    fun deleteParentAccount()
    fun syncChildInstalledApps(apps: List<InstalledApp>)
    fun normalizeDomain(urlOrDomain: String): String
    fun isValidDomainFormat(domain: String): Boolean
    fun isAppBlockable(app: InstalledApp): Boolean
    fun isDomainBlocked(targetUrlOrDomain: String, blockedDomains: List<String>): Boolean
    fun switchUserRole(role: UserRole)
    fun switchChild(childUser: User)
    fun canStartFocus(user: User): Pair<Boolean, String>
    fun addPolicy(policy: FocusPolicy)
    fun updatePolicy(updated: FocusPolicy)
    fun deletePolicy(policyId: String)
    fun getPolicyUsage(policyId: String): PolicyUsage
    fun deletePolicyWithReplacement(policyId: String, replacementPolicyId: String)
    fun generateFocusQr(childId: String, policyId: String, policyTitle: String, durationMinutes: Int): QrToken
    fun submitEmergencyExitRequest(sessionId: String, reason: String, note: String = "", childId: String = currentUser.id, childName: String = currentUser.name): EmergencyExitRequest
    fun resolveEmergencyRequest(requestId: String, approve: Boolean)
    fun cancelEmergencyExitRequest(childId: String = currentUser.id, requestId: String? = null)
    fun expirePendingEmergencyRequests(childId: String? = null)
    fun checkEmergencyRequestExpirations()
    fun assignCardToChild(cardId: String, childId: String): FocusCard?
    fun unpairCardFromChild(cardId: String)
    fun revokeCard(cardId: String)
    fun validateNfcCardForChildStart(cardIdOrToken: String, childUser: User): Result<FocusCard>
    fun validateNfcCardForChildEnd(cardIdOrToken: String, childUser: User, currentSession: FocusSession?): Result<FocusCard>
    fun disableCard(cardId: String)
    fun enableCard(cardId: String)
    fun validateQrTokenForChildStart(qrPayload: String, childUser: User): Result<Pair<FocusPolicy, Int>>
    fun toggleSchedule(scheduleId: String)
    fun saveSchedule(schedule: ScheduleItem)
    fun deleteSchedule(scheduleId: String)
    fun updateChild(updated: ChildProfile)
    fun removeChild(childId: String)
    fun unlinkChildDevice(childId: String)
    fun addChild(name: String, age: Int, emailOrPhone: String): Pair<ChildProfile, String>
    fun isDeviceProtectionActive(): Boolean
    fun isChildProtectionOff(childId: String): Boolean
    fun navigateToChildHome()
    fun isParentGuardUnlocked(): Boolean
    fun unlockParentGuardFor10Minutes(): String
    fun clearParentUnlock()
    fun reportProtectionDisabled(reason: String, childId: String = "child_1")
    fun reportProtectionRestored(childId: String = "child_1")
    fun processChildHeartbeat(childId: String = "child_1", permissionsMap: Map<String, Boolean> = emptyMap(), battery: Int = 85)
    fun reportGuardTriggered(screenName: String)
    fun reportUninstallAttempt()
    fun reportForceStopAttempt()
    fun reportDeviceAdminDisableAttempt()
    fun reportDeviceRebootedDuringFocus()
    fun reportUsbDebuggingOn()
    fun reportFocusEndedWithoutCard()
    fun reportClockChanged()
    fun reportExtraProfileDetected()
    fun toggleSoloStrictMode(cardVerified: Boolean): Pair<Boolean, String>
    fun checkServerHeartbeatTimeout(childId: String = "child_1")
    fun logAdminAction(action: String, target: String)
    fun addInventoryCard(serial: String, activationCode: String): FocusCard
    fun adminExtendTrial(userId: String, days: Int = 14)
    fun adminResetChildPairing(childId: String)
    fun adminRevokeCard(cardId: String)
    fun adminSuspendAccount(userId: String)
    fun adminProgramBatchCards(count: Int)
    fun programCard(cardId: String, nfcUid: String, token: String): FocusCard
    fun programAndAssignNfcCard(cardId: String, targetChildId: String?): FocusCard
    fun registerNfcCard(cardId: String, nfcUid: String, token: String): Result<FocusCard>
    fun verifyQrToken(token: String): Result<QrToken>
    fun verifyNfcCard(cardId: String, nfcUid: String, token: String): Result<FocusCard>
    fun processSubscriptionPayment(planType: PlanType, amountInr: Int)
    fun renewSubscriptionMonthly()
    fun writeAndRegisterBlankCard(parentId: String, parentName: String): FocusCard
    fun inspectCardForRegistration(rawPayloadOrToken: String, parentId: String): CardRegistrationInspection
    fun registerCardToParent(tokenOrCardId: String, parentId: String, parentName: String): FocusCard

    fun recordSessionStart(session: FocusSession) {}
    fun recordSessionHeartbeat(sessionId: String, childId: String, blockedAttempts: Int) {}
    fun recordSessionCompleted(sessionId: String, childId: String, endedAt: Long, actualDurationMillis: Long) {}
    fun recordSessionEndedEarly(sessionId: String, childId: String, endedAt: Long) {}

    fun startRemoteSession(childId: String, policyId: String, policyName: String, durationMinutes: Int) {}
    fun stopRemoteSession(childId: String) {}
    fun startChildSessionListener(context: Any?) {}
    fun stopChildSessionListener() {}
    fun scanFamilyFocusQr(payload: String, onComplete: (Result<String>) -> Unit) {}

    companion object {
        private var _instance: FocalRepository? = null

        fun install(repository: FocalRepository) {
            _instance = repository
        }

        val instance: FocalRepository
            get() = _instance ?: LocalFocalRepository.instance.also { _instance = it }
    }
}
