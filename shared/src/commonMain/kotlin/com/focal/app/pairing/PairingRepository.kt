package com.focal.app.pairing

import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.FocalRepository
import com.focal.app.model.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

enum class PairState {
    PENDING,
    APPROVED,
    DECLINED,
    EXPIRED
}

enum class ParentPairState {
    WAITING,
    PENDING_APPROVAL,
    APPROVED,
    DECLINED,
    EXPIRED
}

data class ParentPairingSession(
    val childId: String,
    val childName: String,
    val code: String, // 6 digits, e.g. "472916"
    val qrPayload: String, // "focal://pair?code=472916&childId=child_1&parentId=parent_01"
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val state: ParentPairState = ParentPairState.WAITING,
    val deviceManufacturer: String = "Realme",
    val androidVersion: String = "14",
    val requestedAtMs: Long = 0L,
    val isDeclined: Boolean = false
)

data class PairRequest(
    val id: String,
    val code: String,
    val parentId: String,
    val parentName: String,
    val parentInitials: String = "PS",
    val childId: String,
    val childName: String,
    val childInitials: String = "AR",
    val childGrade: String = "Class 10 · Student",
    val pairedCardId: String? = "FC0001",
    val expiresAtMs: Long,
    val state: PairState = PairState.PENDING
)

interface PairingRepository {
    suspend fun redeemCode(code: String): Result<PairRequest>
    suspend fun redeemQr(payload: String): Result<PairRequest>
    fun observeRequest(id: String): Flow<PairState>
    suspend fun cancelRequest(id: String)

    fun getPendingRequest(): PairRequest?
    fun isPaired(): Boolean
    fun getActivePairRequest(): PairRequest?
    fun markPaired(request: PairRequest)
    fun unlink()

    // Parent pairing session management (Step P4)
    fun createOrGetParentSession(childId: String, childName: String): ParentPairingSession
    fun getActiveParentSession(childId: String): ParentPairingSession?
    fun regenerateParentCode(childId: String): ParentPairingSession
    fun approveChildPairing(childId: String): Boolean
    fun declineChildPairing(childId: String): ParentPairingSession
    val activeParentSessionFlow: StateFlow<ParentPairingSession?>

    companion object {
        val instance: PairingRepository get() = DefaultPairingRepository.instance
    }
}

class DefaultPairingRepository : PairingRepository {

    private var activeRequest: PairRequest? = null
    private var failedAttempts: Int = 0
    private var lockoutUntilMs: Long = 0L

    private val parentSessions = mutableMapOf<String, ParentPairingSession>()
    private val _parentSessionFlow = MutableStateFlow<ParentPairingSession?>(null)
    override val activeParentSessionFlow: StateFlow<ParentPairingSession?> = _parentSessionFlow.asStateFlow()
    private val pairStateFlows = mutableMapOf<String, MutableStateFlow<PairState>>()

    companion object {
        val instance = DefaultPairingRepository()

        private const val KEY_PENDING_REQ_ID = "pairing_pending_req_id"
        private const val KEY_PENDING_CODE = "pairing_pending_code"
        private const val KEY_PENDING_PARENT_NAME = "pairing_pending_parent_name"
        private const val KEY_PENDING_CHILD_NAME = "pairing_pending_child_name"
        private const val KEY_PENDING_EXPIRES_AT = "pairing_pending_expires_at"
        private const val KEY_PENDING_CARD_ID = "pairing_pending_card_id"

        private const val KEY_PAIRED_IS_PAIRED = "KEY_PAIRED_AS_CHILD"
        private const val KEY_PAIRED_CHILD_NAME = "KEY_PAIRED_CHILD_NAME"
        private const val KEY_PAIRED_PARENT_NAME = "KEY_PAIRED_PARENT_NAME"
        private const val KEY_PAIRED_CARD_ID = "KEY_PAIRED_CARD_ID"
    }

    override fun createOrGetParentSession(childId: String, childName: String): ParentPairingSession {
        val now = currentTimeMillis()
        val existing = parentSessions[childId]
        if (existing != null && now < existing.expiresAtMs && existing.state != ParentPairState.EXPIRED) {
            _parentSessionFlow.value = existing
            return existing
        }
        return regenerateParentCodeInternal(childId, childName)
    }

    override fun regenerateParentCode(childId: String): ParentPairingSession {
        val child = FocalRepository.instance.childrenList.find { it.id == childId }
        val name = child?.name ?: parentSessions[childId]?.childName ?: "Child"
        return regenerateParentCodeInternal(childId, name)
    }

    private fun regenerateParentCodeInternal(childId: String, childName: String): ParentPairingSession {
        val now = currentTimeMillis()
        var code = (100000..999999).random().toString()
        while (code in setOf("000000", "999999", "111111", "222222", "333333")) {
            code = (100000..999999).random().toString()
        }
        val repo = FocalRepository.instance
        val parent = repo.controllerUser
        val parentId = parent.id.ifBlank { "parent_01" }
        val parentName = parent.name.ifBlank { "Priya Sharma" }
        val child = repo.childrenList.find { it.id == childId }
        val grade = child?.displayGrade ?: "Class 10"
        val policyId = child?.defaultPolicyId ?: "pol_study"
        val policy = repo.policies.find { it.id == policyId } ?: repo.policies.first()
        val defaultFocusMins = child?.defaultFocusMinutes ?: 90
        val expiresAt = now + 10 * 60 * 1000L

        val appsJson = policy.blockedApps.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
        val websitesJson = policy.blockedWebsites.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }
        val pairingJson = """{"familyId":"${parent.familyId.ifBlank { "family_1" }}","parentName":"$parentName","childId":"$childId","childName":"$childName","class":"$grade","defaultPolicy":{"name":"${policy.name}","apps":[$appsJson],"websites":[$websitesJson]},"defaultFocusMinutes":$defaultFocusMins,"schedules":[],"pairToken":"pt_$code","expiresAt":$expiresAt}"""

        val base64Payload = Base64Url.encode(pairingJson)
        val qrPayload = "focal://pair?code=$code&childId=$childId&v=1&d=$base64Payload"

        val session = ParentPairingSession(
            childId = childId,
            childName = childName,
            code = code,
            qrPayload = qrPayload,
            createdAtMs = now,
            expiresAtMs = expiresAt,
            state = ParentPairState.WAITING,
            deviceManufacturer = "Realme",
            androidVersion = "14",
            requestedAtMs = 0L,
            isDeclined = false
        )
        parentSessions[childId] = session
        _parentSessionFlow.value = session
        return session
    }

    override fun getActiveParentSession(childId: String): ParentPairingSession? {
        val session = parentSessions[childId] ?: return null
        if (currentTimeMillis() >= session.expiresAtMs && session.state != ParentPairState.APPROVED) {
            val expired = session.copy(state = ParentPairState.EXPIRED)
            parentSessions[childId] = expired
            if (_parentSessionFlow.value?.childId == childId) {
                _parentSessionFlow.value = expired
            }
            return expired
        }
        return session
    }

    override fun approveChildPairing(childId: String): Boolean {
        val session = parentSessions[childId] ?: return false
        val approved = session.copy(state = ParentPairState.APPROVED)
        parentSessions[childId] = approved
        _parentSessionFlow.value = approved

        val repo = FocalRepository.instance
        val childIdx = repo.childrenList.indexOfFirst { it.id == childId }
        if (childIdx >= 0) {
            val existing = repo.childrenList[childIdx]
            repo.childrenList[childIdx] = existing.copy(
                deviceName = "${session.deviceManufacturer} · Android ${session.androidVersion}",
                isOnline = true,
                pairedCardId = "FC0001"
            )
        }

        pairStateFlows[session.code]?.value = PairState.APPROVED
        pairStateFlows[session.childId]?.value = PairState.APPROVED
        pairStateFlows["DEFAULT"]?.value = PairState.APPROVED

        FocalLocalStorage.saveBoolean(KEY_PAIRED_IS_PAIRED, true)
        FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)
        FocalLocalStorage.saveString(KEY_PAIRED_CHILD_NAME, session.childName)
        FocalLocalStorage.saveString(KEY_PAIRED_CARD_ID, "FC0001")

        com.focal.app.data.ActivityRepository.instance.recordEvent(
            type = com.focal.app.data.ActivityEventType.PHONE_PAIRED,
            childId = childId,
            details = "Paired ${session.childName}'s phone to ${repo.controllerUser.name.ifBlank { "Priya Sharma" }}",
            metadata = mapOf("child" to session.childName, "device" to "${session.deviceManufacturer} · Android ${session.androidVersion}")
        )
        return true
    }

    override fun declineChildPairing(childId: String): ParentPairingSession {
        val session = parentSessions[childId]
        val code = session?.code ?: ""
        pairStateFlows[code]?.value = PairState.DECLINED
        pairStateFlows[childId]?.value = PairState.DECLINED
        pairStateFlows["DEFAULT"]?.value = PairState.DECLINED

        val childName = session?.childName ?: "Child"
        val regenerated = regenerateParentCodeInternal(childId, childName).copy(isDeclined = true)
        parentSessions[childId] = regenerated
        _parentSessionFlow.value = regenerated
        return regenerated
    }

    override suspend fun redeemCode(code: String): Result<PairRequest> {
        val now = currentTimeMillis()

        // Check lockout
        if (now < lockoutUntilMs) {
            val remainingMins = ((lockoutUntilMs - now) / 60000L).coerceAtLeast(1)
            return Result.failure(Exception("Too many tries. Try again in $remainingMins min."))
        }

        val cleaned = code.trim().filter { it.isDigit() }
        if (cleaned.length != 6) {
            return Result.failure(Exception("That code didn't work. Check and try again."))
        }

        // Simulating specific error test vectors:
        if (cleaned == "000000" || cleaned == "999999") {
            failedAttempts++
            if (failedAttempts >= 5) {
                lockoutUntilMs = now + 5 * 60 * 1000L
                return Result.failure(Exception("Too many tries. Try again in 5 min."))
            }
            return Result.failure(Exception("That code didn't work. Check and try again."))
        }

        if (cleaned == "111111") {
            return Result.failure(Exception("This code has expired. Ask your parent for a new one."))
        }

        // Check active parent session matching this code
        val matchingSession = parentSessions.values.find { it.code == cleaned }
        if (matchingSession != null) {
            if (now >= matchingSession.expiresAtMs) {
                return Result.failure(Exception("This code has expired. Ask your parent for a new one."))
            }
            failedAttempts = 0
            val updated = matchingSession.copy(
                state = ParentPairState.PENDING_APPROVAL,
                requestedAtMs = now
            )
            parentSessions[matchingSession.childId] = updated
            _parentSessionFlow.value = updated

            val parent = FocalRepository.instance.controllerUser
            val parentName = if (parent.name.isNotBlank()) parent.name else "Priya Sharma"
            val parentInitials = parentName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "PS" }

            val req = PairRequest(
                id = "REQ_${now}_${cleaned}",
                code = cleaned,
                parentId = parent.id.ifBlank { "parent_01" },
                parentName = parentName,
                parentInitials = parentInitials,
                childId = matchingSession.childId,
                childName = matchingSession.childName,
                childInitials = matchingSession.childName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "AR" },
                childGrade = "Class 10 · Student",
                pairedCardId = "FC0001",
                expiresAtMs = matchingSession.expiresAtMs,
                state = PairState.PENDING
            )
            activeRequest = req
            savePendingRequest(req)
            return Result.success(req)
        }

        // Allow test mock codes ("482910", "847291")
        if (cleaned == "482910" || cleaned == "847291") {
            failedAttempts = 0
            val parent = FocalRepository.instance.controllerUser
            val parentName = if (parent.name.isNotBlank()) parent.name else "Priya Sharma"
            val parentInitials = parentName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "PS" }

            val childName = "Aarav"
            val childInitials = "AR"

            val req = PairRequest(
                id = "REQ_${now}_${cleaned}",
                code = cleaned,
                parentId = parent.id.ifBlank { "parent_01" },
                parentName = parentName,
                parentInitials = parentInitials,
                childId = "child_01",
                childName = childName,
                childInitials = childInitials,
                childGrade = "Class 10 · Student",
                pairedCardId = "FC0001",
                expiresAtMs = now + 10 * 60 * 1000L,
                state = PairState.PENDING
            )

            activeRequest = req
            savePendingRequest(req)
            return Result.success(req)
        }

        // 6-digit code in demo: works only when parent and child are on the SAME phone;
        // on a different phone show "Use the QR in demo mode"
        return Result.failure(Exception("Use the QR in demo mode"))
    }

    override suspend fun redeemQr(payload: String): Result<PairRequest> {
        val trimmed = payload.trim()

        // 1. Family focus QR scanned in pairing scanner -> reject with strict message
        if (trimmed.startsWith("focal://focus", ignoreCase = true) || trimmed.startsWith("FOCAL_FAMILY_QR", ignoreCase = true)) {
            return Result.failure(Exception("That's not a pairing QR"))
        }

        // 2. Reject non focal://pair and non FOCAL QR
        if (!trimmed.startsWith("focal://pair", ignoreCase = true) && !trimmed.startsWith("FOCAL", ignoreCase = true)) {
            return Result.failure(Exception("That's not a FOCAL QR"))
        }

        val now = currentTimeMillis()

        // 3. Pairing QR with payload: focal://pair?v=1&d=...
        if (trimmed.startsWith("focal://pair", ignoreCase = true) && trimmed.contains("d=")) {
            val dParam = trimmed.substringAfter("d=").substringBefore("&").trim()
            if (dParam.isNotBlank()) {
                try {
                    val jsonStr = Base64Url.decodeToString(dParam)
                    val expiresAt = "\"expiresAt\"\\s*:\\s*([0-9]+)".toRegex().find(jsonStr)?.groupValues?.get(1)?.toLongOrNull() ?: (now + 10 * 60 * 1000L)
                    if (now > expiresAt) {
                        return Result.failure(Exception("This pairing QR has expired. Ask your parent for a new one."))
                    }

                    fun extractString(key: String): String =
                        "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex().find(jsonStr)?.groupValues?.get(1) ?: ""

                    val familyId = extractString("familyId").ifBlank { "family_1" }
                    val parentName = extractString("parentName").ifBlank { "Priya Sharma" }
                    val childId = extractString("childId").ifBlank { "child_01" }
                    val childName = extractString("childName").ifBlank { "Aarav" }
                    val grade = extractString("class").ifBlank { "Class 10 · Student" }
                    val pairToken = extractString("pairToken").ifBlank { "pt_qr" }

                    val parentInitials = parentName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "PS" }
                    val childInitials = childName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "AR" }

                    // Save locally in demo mode - fully offline!
                    val repo = FocalRepository.instance
                    repo.controllerUser = repo.controllerUser.copy(name = parentName, familyId = familyId)
                    repo.currentUser = repo.currentUser.copy(
                        id = childId,
                        name = childName,
                        role = com.focal.app.model.UserRole.CHILD,
                        familyId = familyId
                    )
                    FocalLocalStorage.saveBoolean(KEY_PAIRED_IS_PAIRED, true)
                    FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)
                    FocalLocalStorage.saveString(KEY_PAIRED_CHILD_NAME, childName)
                    FocalLocalStorage.saveString(KEY_PAIRED_PARENT_NAME, parentName)
                    FocalLocalStorage.saveString(KEY_PAIRED_CARD_ID, "FC0001")
                    FocalLocalStorage.saveString("KEY_ROLE", "CHILD")

                    val req = PairRequest(
                        id = "REQ_${now}_$pairToken",
                        code = "QR_DEMO",
                        parentId = familyId,
                        parentName = parentName,
                        parentInitials = parentInitials,
                        childId = childId,
                        childName = childName,
                        childInitials = childInitials,
                        childGrade = grade,
                        pairedCardId = "FC0001",
                        expiresAtMs = expiresAt,
                        state = PairState.APPROVED
                    )
                    activeRequest = req
                    markPaired(req)
                    return Result.success(req)
                } catch (e: Exception) {
                    return Result.failure(Exception("That's not a pairing QR"))
                }
            }
        }

        // 4. Pairing QR with focal://pair?code=...
        if (trimmed.startsWith("focal://pair", ignoreCase = true)) {
            val code = trimmed.substringAfter("code=", "").substringBefore("&").trim()
            if (code.isNotBlank()) {
                return redeemCode(code)
            }
            return Result.failure(Exception("That's not a pairing QR"))
        }

        // 5. Legacy test token support: FOCAL_PARENT_PAIR_TOKEN_...
        if (trimmed.startsWith("FOCAL_PARENT_PAIR_TOKEN", ignoreCase = true)) {
            val parent = FocalRepository.instance.controllerUser
            val parentName = if (parent.name.isNotBlank()) parent.name else "Priya Sharma"
            val parentInitials = parentName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "PS" }

            val childName = if (trimmed.contains("Ananya", ignoreCase = true)) "Ananya" else "Aarav"
            val childInitials = if (childName == "Ananya") "AN" else "AR"

            val req = PairRequest(
                id = "REQ_${now}_QR",
                code = "QR_PAIR",
                parentId = parent.id.ifBlank { "parent_01" },
                parentName = parentName,
                parentInitials = parentInitials,
                childId = "child_01",
                childName = childName,
                childInitials = childInitials,
                childGrade = "Class 10 · Student",
                pairedCardId = "FC0001",
                expiresAtMs = now + 10 * 60 * 1000L,
                state = PairState.PENDING
            )

            activeRequest = req
            savePendingRequest(req)
            return Result.success(req)
        }

        return Result.failure(Exception("That's not a pairing QR"))
    }

    override fun observeRequest(id: String): Flow<PairState> = flow {
        emit(PairState.PENDING)

        val req = activeRequest ?: getPendingRequest()
        if (req == null) {
            emit(PairState.EXPIRED)
            return@flow
        }

        // Special simulation vectors for declined / expired
        if (req.code == "222222") {
            delay(2500)
            emit(PairState.DECLINED)
            return@flow
        }
        if (req.code == "333333") {
            delay(2500)
            emit(PairState.EXPIRED)
            return@flow
        }

        // If an active parent pairing session matches this code or childId
        val session = parentSessions.values.find { it.code == req.code || it.childId == req.childId }
        if (session != null) {
            val stateFlow = pairStateFlows.getOrPut(req.code) { MutableStateFlow(PairState.PENDING) }
            stateFlow.collect { state ->
                emit(state)
            }
            return@flow
        }

        // Standard test simulation: parent approves after 3.5s
        delay(3500)
        emit(PairState.APPROVED)
    }

    override suspend fun cancelRequest(id: String) {
        activeRequest = null
        clearPendingRequest()
    }

    override fun getPendingRequest(): PairRequest? {
        if (activeRequest != null) {
            val req = activeRequest!!
            if (currentTimeMillis() < req.expiresAtMs) return req
            activeRequest = null
            clearPendingRequest()
            return null
        }

        val reqId = FocalLocalStorage.getString(KEY_PENDING_REQ_ID, "")
        val expiresAt = FocalLocalStorage.getLong(KEY_PENDING_EXPIRES_AT, 0L)
        if (reqId.isNotBlank() && currentTimeMillis() < expiresAt) {
            val code = FocalLocalStorage.getString(KEY_PENDING_CODE, "")
            val parentName = FocalLocalStorage.getString(KEY_PENDING_PARENT_NAME, "Priya Sharma")
            val childName = FocalLocalStorage.getString(KEY_PENDING_CHILD_NAME, "Aarav")
            val cardId = FocalLocalStorage.getString(KEY_PENDING_CARD_ID, "FC0001")

            val restored = PairRequest(
                id = reqId,
                code = code,
                parentId = "parent_01",
                parentName = parentName,
                parentInitials = parentName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "PS" },
                childId = "child_01",
                childName = childName,
                childInitials = if (childName == "Aarav") "AR" else childName.take(2).uppercase(),
                childGrade = "Class 10 · Student",
                pairedCardId = cardId,
                expiresAtMs = expiresAt,
                state = PairState.PENDING
            )
            activeRequest = restored
            return restored
        }

        clearPendingRequest()
        return null
    }

    override fun isPaired(): Boolean {
        return FocalLocalStorage.getBoolean(KEY_PAIRED_IS_PAIRED, false)
    }

    override fun getActivePairRequest(): PairRequest? {
        return activeRequest ?: getPendingRequest()
    }

    override fun markPaired(request: PairRequest) {
        FocalLocalStorage.saveBoolean(KEY_PAIRED_IS_PAIRED, true)
        FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)
        FocalLocalStorage.saveString(KEY_PAIRED_CHILD_NAME, request.childName)
        FocalLocalStorage.saveString(KEY_PAIRED_PARENT_NAME, request.parentName)
        FocalLocalStorage.saveString(KEY_PAIRED_CARD_ID, request.pairedCardId ?: "FC0001")
        clearPendingRequest()
        activeRequest = null

        // Sync with FocalRepository
        val repo = FocalRepository.instance
        val updatedUser = repo.currentUser.copy(
            name = request.childName,
            role = com.focal.app.model.UserRole.CHILD
        )
        repo.currentUser = updatedUser
        repo.controllerUser = repo.controllerUser.copy(name = request.parentName)

        val existingChildren = repo.childrenList.toMutableList()
        val cIdx = existingChildren.indexOfFirst { it.name.contains("Aarav", ignoreCase = true) || it.name.contains("Rahul", ignoreCase = true) }
        if (cIdx >= 0) {
            existingChildren[cIdx] = existingChildren[cIdx].copy(
                name = request.childName,
                pairedCardId = request.pairedCardId ?: "FC0001"
            )
        }

        com.focal.app.data.ActivityRepository.instance.recordEvent(
            type = com.focal.app.data.ActivityEventType.PHONE_PAIRED,
            childId = request.childId,
            details = "Paired ${request.childName}'s phone to ${request.parentName}",
            metadata = mapOf("parent" to request.parentName, "child" to request.childName)
        )
    }

    override fun unlink() {
        FocalLocalStorage.saveBoolean(KEY_PAIRED_IS_PAIRED, false)
        FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", false)
        FocalLocalStorage.saveBoolean("KEY_SETUP_DONE", false)
        clearPendingRequest()
        activeRequest = null
        parentSessions.clear()
        pairStateFlows.clear()
        failedAttempts = 0
        lockoutUntilMs = 0L
    }

    private fun savePendingRequest(req: PairRequest) {
        FocalLocalStorage.saveString(KEY_PENDING_REQ_ID, req.id)
        FocalLocalStorage.saveString(KEY_PENDING_CODE, req.code)
        FocalLocalStorage.saveString(KEY_PENDING_PARENT_NAME, req.parentName)
        FocalLocalStorage.saveString(KEY_PENDING_CHILD_NAME, req.childName)
        FocalLocalStorage.saveLong(KEY_PENDING_EXPIRES_AT, req.expiresAtMs)
        FocalLocalStorage.saveString(KEY_PENDING_CARD_ID, req.pairedCardId ?: "FC0001")
    }

    private fun clearPendingRequest() {
        FocalLocalStorage.saveString(KEY_PENDING_REQ_ID, "")
        FocalLocalStorage.saveString(KEY_PENDING_CODE, "")
        FocalLocalStorage.saveString(KEY_PENDING_PARENT_NAME, "")
        FocalLocalStorage.saveString(KEY_PENDING_CHILD_NAME, "")
        FocalLocalStorage.saveLong(KEY_PENDING_EXPIRES_AT, 0L)
        FocalLocalStorage.saveString(KEY_PENDING_CARD_ID, "")
    }
}
