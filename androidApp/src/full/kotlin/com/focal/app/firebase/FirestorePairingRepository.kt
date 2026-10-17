package com.focal.app.firebase

import com.focal.app.data.ActivityEventType
import com.focal.app.data.ActivityRepository
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.FocalRepository
import com.focal.app.model.UserRole
import com.focal.app.model.currentTimeMillis
import com.focal.app.pairing.Base64Url
import com.focal.app.pairing.PairRequest
import com.focal.app.pairing.PairState
import com.focal.app.pairing.ParentPairState
import com.focal.app.pairing.ParentPairingSession
import com.focal.app.pairing.PairingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestorePairingRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PairingRepository {

    private var activeRequest: PairRequest? = null
    private var failedAttempts: Int = 0
    private var lockoutUntilMs: Long = 0L

    private val parentSessions = mutableMapOf<String, ParentPairingSession>()
    private val _parentSessionFlow = MutableStateFlow<ParentPairingSession?>(null)
    override val activeParentSessionFlow: StateFlow<ParentPairingSession?> = _parentSessionFlow.asStateFlow()

    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    companion object {
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
        return regenerateParentCode(childId)
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
        val parentId = auth.currentUser?.uid ?: parent.id.ifBlank { "parent_01" }
        val parentName = parent.name.ifBlank { "Parent" }
        val child = repo.childrenList.find { it.id == childId }
        val grade = child?.displayGrade ?: "Class 10"
        val policyId = child?.defaultPolicyId ?: "pol_study"
        val policy = repo.policies.find { it.id == policyId } ?: repo.policies.firstOrNull()
        val defaultFocusMins = child?.defaultFocusMinutes ?: 90
        val expiresAt = now + 10 * 60 * 1000L

        val appsJson = policy?.blockedApps?.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" } ?: ""
        val websitesJson = policy?.blockedWebsites?.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" } ?: ""
        val familyId = parentId

        val pairingJson = """{"familyId":"$familyId","parentName":"$parentName","childId":"$childId","childName":"$childName","class":"$grade","defaultPolicy":{"name":"${policy?.name ?: "Study"}","apps":[$appsJson],"websites":[$websitesJson]},"defaultFocusMinutes":$defaultFocusMins,"schedules":[],"pairToken":"pt_$code","expiresAt":$expiresAt}"""
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

        // Write to Firestore /pairingCodes/{code}
        val pairingDoc = hashMapOf(
            "code" to code,
            "familyId" to parentId,
            "parentId" to parentId,
            "parentName" to parentName,
            "childId" to childId,
            "childName" to childName,
            "expiresAtMs" to expiresAt,
            "createdAtMs" to now,
            "state" to "WAITING",
            "used" to false
        )
        // Ensure children/{childId} document exists in Firestore (with familyId = parentId, NO childUid)
        val childDocData = hashMapOf(
            "id" to childId,
            "familyId" to parentId,
            "name" to childName,
            "grade" to grade,
            "defaultPolicyId" to policyId,
            "defaultFocusMinutes" to defaultFocusMins,
            "deviceName" to "",
            "isOnline" to false,
            "pairedCardId" to null,
            "createdAt" to now
        )
        try {
            firestore.collection("children").document(childId).set(childDocData, com.google.firebase.firestore.SetOptions.merge())
            firestore.collection("pairingCodes").document(code).set(pairingDoc)
        } catch (e: Exception) {
            com.focal.app.ui.components.PlatformPermissionsHelper.showToast("Failed to create code: ${e.localizedMessage}")
        }

        // Listen for child redeeming this code on parent side (single doc listener, no queries)
        val reg = firestore.collection("pairingCodes").document(code)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val stateStr = snapshot.getString("state") ?: "WAITING"
                val devName = snapshot.getString("childDeviceName") ?: "Realme · Android 14"
                val devParts = devName.split("·")
                val devMan = devParts.getOrNull(0)?.trim() ?: "Realme"
                val andVer = devParts.getOrNull(1)?.replace("Android", "")?.trim() ?: "14"
                val reqTime = snapshot.getLong("requestedAt") ?: 0L
                val childUid = snapshot.getString("childUid")

                val currentState = when (stateStr) {
                    "PENDING_APPROVAL" -> ParentPairState.PENDING_APPROVAL
                    "APPROVED" -> ParentPairState.APPROVED
                    "DECLINED" -> ParentPairState.DECLINED
                    "EXPIRED" -> ParentPairState.EXPIRED
                    else -> ParentPairState.WAITING
                }

                val currentSession = parentSessions[childId]
                if (currentSession != null && currentSession.code == code) {
                    val updated = currentSession.copy(
                        state = currentState,
                        deviceManufacturer = devMan,
                        androidVersion = andVer,
                        requestedAtMs = reqTime,
                        childUid = childUid
                    )
                    parentSessions[childId] = updated
                    _parentSessionFlow.value = updated
                }
            }
        listenerRegistrations.add(reg)

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
        try {
            val session = parentSessions[childId] ?: _parentSessionFlow.value
            if (session == null || session.code.isBlank()) {
                com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                    "Cannot approve: pairing session not found"
                )
                return false
            }
            val repo = FocalRepository.instance
            val parent = repo.controllerUser
            val parentName = parent.name.ifBlank { "Parent" }
            val parentFamilyId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: parent.id

            val cachedUid = session.childUid
            if (!cachedUid.isNullOrBlank()) {
                executeApprovalBatch(childId, cachedUid, session, parentName, parentFamilyId)
            } else {
                try {
                    firestore.collection("pairingCodes").document(session.code).get()
                        .addOnSuccessListener { doc ->
                            try {
                                val childUid = doc.getString("childUid")
                                if (childUid.isNullOrBlank()) {
                                    com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                                        "Child phone has not requested pairing yet"
                                    )
                                    return@addOnSuccessListener
                                }
                                executeApprovalBatch(childId, childUid, session, parentName, parentFamilyId)
                            } catch (e: Exception) {
                                com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                                    "Approval failed: ${e.localizedMessage ?: "Unexpected error"}"
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                                "Approval failed: ${e.localizedMessage ?: "Could not verify pairing code"}"
                            )
                        }
                } catch (e: Exception) {
                    com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                        "Approval failed: ${e.localizedMessage}"
                    )
                }
            }
            return true
        } catch (e: Exception) {
            com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                "Approval error: ${e.localizedMessage}"
            )
            return false
        }
    }

    private fun executeApprovalBatch(
        childId: String,
        childUid: String,
        session: ParentPairingSession,
        parentName: String,
        parentFamilyId: String
    ) {
        try {
            val batch = firestore.batch()

            // 1. (a) Update children/{childId}: childUid = code.childUid, parentName
            val childRef = firestore.collection("children").document(childId)
            batch.update(childRef, mapOf(
                "childUid" to childUid,
                "parentName" to parentName,
                "deviceName" to "${session.deviceManufacturer} · Android ${session.androidVersion}",
                "isOnline" to true
            ))

            // 2. Set childLinks/{childUid} = { childId, familyId }
            val linkRef = firestore.collection("childLinks").document(childUid)
            batch.set(linkRef, hashMapOf(
                "childId" to childId,
                "familyId" to parentFamilyId
            ))

            // 3. Update pairingCodes/{code}: state = APPROVED
            val codeRef = firestore.collection("pairingCodes").document(session.code)
            batch.update(codeRef, mapOf(
                "state" to "APPROVED",
                "used" to true
            ))

            batch.commit()
                .addOnSuccessListener {
                    val approved = session.copy(state = ParentPairState.APPROVED, childUid = childUid)
                    parentSessions[childId] = approved
                    _parentSessionFlow.value = approved

                    val repo = FocalRepository.instance
                    val childIdx = repo.childrenList.indexOfFirst { it.id == childId }
                    if (childIdx >= 0) {
                        val existing = repo.childrenList[childIdx]
                        repo.childrenList[childIdx] = existing.copy(
                            childUid = childUid,
                            deviceName = "${session.deviceManufacturer} · Android ${session.androidVersion}",
                            isOnline = true,
                            pairedCardId = "FC0001"
                        )
                    }

                    com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                        "${session.childName}'s phone is connected"
                    )

                    try {
                        ActivityRepository.instance.recordEvent(
                            type = ActivityEventType.PHONE_PAIRED,
                            childId = childId,
                            details = "Paired ${session.childName}'s phone to $parentName",
                            metadata = mapOf("child" to session.childName, "device" to "${session.deviceManufacturer} · Android ${session.androidVersion}")
                        )
                    } catch (_: Exception) {}
                }
                .addOnFailureListener { e ->
                    com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                        "Approval failed: ${e.localizedMessage ?: "Failed to write pairing records"}"
                    )
                }
        } catch (e: Exception) {
            com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                "Batch write error: ${e.localizedMessage}"
            )
        }
    }

    override fun declineChildPairing(childId: String): ParentPairingSession {
        val session = parentSessions[childId]
        if (session != null) {
            firestore.collection("pairingCodes").document(session.code).update(
                mapOf(
                    "state" to "DECLINED",
                    "used" to true
                )
            )
        }

        val childName = session?.childName ?: "Child"
        val regenerated = regenerateParentCodeInternal(childId, childName).copy(isDeclined = true)
        parentSessions[childId] = regenerated
        _parentSessionFlow.value = regenerated
        return regenerated
    }

    override suspend fun redeemCode(code: String): Result<PairRequest> {
        val now = currentTimeMillis()

        // 1. Check local lockout (5 wrong attempts -> 5 min lockout)
        // TODO: Move failed attempts lockout to Cloud Function in FB2
        if (now < lockoutUntilMs) {
            val remainingMins = ((lockoutUntilMs - now) / 60000L).coerceAtLeast(1)
            return Result.failure(Exception("Too many tries. Try again in $remainingMins min."))
        }

        val cleaned = code.trim().filter { it.isDigit() }
        if (cleaned.length != 6) {
            return Result.failure(Exception("That code didn't work. Check and try again."))
        }

        // 2. Ensure child device is authenticated anonymously
        var childUid = auth.currentUser?.uid
        if (childUid == null) {
            try {
                val authRes = auth.signInAnonymously().await()
                childUid = authRes.user?.uid
            } catch (e: Exception) {
                return Result.failure(Exception("Network error. Check connection and try again."))
            }
        }

        return try {
            val doc = firestore.collection("pairingCodes").document(cleaned).get().await()
            if (!doc.exists()) {
                failedAttempts++
                if (failedAttempts >= 5) {
                    lockoutUntilMs = now + 5 * 60 * 1000L
                    return Result.failure(Exception("Too many tries. Try again in 5 min."))
                }
                return Result.failure(Exception("That code didn't work. Check and try again."))
            }

            val isUsed = doc.getBoolean("used") ?: false
            val expiresAtMs = doc.getLong("expiresAtMs") ?: 0L

            if (isUsed || now >= expiresAtMs) {
                return Result.failure(Exception("This code has expired. Ask your parent for a new one."))
            }

            failedAttempts = 0

            val parentId = doc.getString("parentId") ?: ""
            val parentName = doc.getString("parentName") ?: "Parent"
            val childId = doc.getString("childId") ?: ""
            val childName = doc.getString("childName") ?: "Child"
            val familyId = doc.getString("familyId") ?: parentId

            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim().ifBlank { "Realme · Android 14" }

            // Child sets pairingCodes/{code}.state = PENDING_APPROVAL + childUid + childDeviceName + requestedAt only.
            firestore.collection("pairingCodes").document(cleaned).update(
                mapOf(
                    "state" to "PENDING_APPROVAL",
                    "childUid" to childUid,
                    "childDeviceName" to deviceName,
                    "requestedAt" to now
                )
            ).await()

            val parentInitials = parentName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "PS" }
            val childInitials = childName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "AR" }

            val req = PairRequest(
                id = cleaned,
                code = cleaned,
                parentId = parentId,
                parentName = parentName,
                parentInitials = parentInitials,
                childId = childId,
                childName = childName,
                childInitials = childInitials,
                childGrade = "Class 10 · Student",
                pairedCardId = "FC0001",
                expiresAtMs = expiresAtMs,
                state = PairState.PENDING
            )
            activeRequest = req
            savePendingRequest(req)
            Result.success(req)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Connection error. Try again."))
        }
    }

    override suspend fun redeemQr(payload: String): Result<PairRequest> {
        val trimmed = payload.trim()

        if (trimmed.startsWith("focal://focus", ignoreCase = true) || trimmed.startsWith("FOCAL_FAMILY_QR", ignoreCase = true)) {
            return Result.failure(Exception("This isn't a FOCAL pairing code"))
        }

        if (!trimmed.startsWith("focal://pair", ignoreCase = true) && !trimmed.startsWith("FOCAL", ignoreCase = true)) {
            return Result.failure(Exception("This isn't a FOCAL pairing code"))
        }

        if (trimmed.contains("code=")) {
            val code = trimmed.substringAfter("code=", "").substringBefore("&").trim()
            if (code.isNotBlank()) {
                return redeemCode(code)
            }
        }

        if (trimmed.contains("d=")) {
            val dParam = trimmed.substringAfter("d=").substringBefore("&").trim()
            if (dParam.isNotBlank()) {
                try {
                    val jsonStr = Base64Url.decodeToString(dParam)
                    val codeMatch = "\"pairToken\"\\s*:\\s*\"pt_([0-9]{6})\"".toRegex().find(jsonStr)
                    val code = codeMatch?.groupValues?.get(1)
                    if (!code.isNullOrBlank()) {
                        return redeemCode(code)
                    }
                } catch (ignored: Exception) {}
            }
        }

        return Result.failure(Exception("This isn't a FOCAL pairing code"))
    }

    override fun observeRequest(id: String): Flow<PairState> = callbackFlow {
        trySend(PairState.PENDING)

        val reg = firestore.collection("pairingCodes").document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                    ) {
                        com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                            "Permission denied: ${error.localizedMessage}"
                        )
                    }
                    trySend(PairState.EXPIRED)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(PairState.EXPIRED)
                    return@addSnapshotListener
                }

                val stateStr = snapshot.getString("state") ?: "WAITING"
                val expiresAtMs = snapshot.getLong("expiresAtMs") ?: 0L
                val now = currentTimeMillis()

                if (now > expiresAtMs && stateStr != "APPROVED") {
                    trySend(PairState.EXPIRED)
                    return@addSnapshotListener
                }

                when (stateStr) {
                    "APPROVED" -> {
                        val childIdFromDoc = snapshot.getString("childId") ?: activeRequest?.childId ?: "child_01"
                        try {
                            firestore.collection("children").document(childIdFromDoc).get()
                                .addOnSuccessListener { childDoc ->
                                    try {
                                        val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                        val docChildUid = childDoc.getString("childUid")

                                        // Child goes to "Connected" ONLY after it can read children/{childId} (childUid == its uid) — not before.
                                        if (!childDoc.exists() || docChildUid.isNullOrBlank() || docChildUid != myUid) {
                                            // Parent batch is in flight or childUid not set yet; wait for doc update
                                            return@addOnSuccessListener
                                        }

                                        val childName = childDoc.getString("name") ?: snapshot.getString("childName") ?: activeRequest?.childName ?: "Child"
                                        val parentName = childDoc.getString("parentName") ?: snapshot.getString("parentName") ?: activeRequest?.parentName ?: "Parent"
                                        val cardId = childDoc.getString("pairedCardId") ?: "FC0001"
                                        val req = (activeRequest ?: getPendingRequest())?.copy(
                                            childId = childIdFromDoc,
                                            childName = childName,
                                            parentName = parentName,
                                            pairedCardId = cardId,
                                            state = PairState.APPROVED
                                        ) ?: PairRequest(
                                            id = id,
                                            code = id,
                                            parentId = snapshot.getString("parentId") ?: "",
                                            parentName = parentName,
                                            parentInitials = parentName.take(2).uppercase().ifEmpty { "PS" },
                                            childId = childIdFromDoc,
                                            childName = childName,
                                            childInitials = childName.take(2).uppercase().ifEmpty { "AR" },
                                            childGrade = childDoc.getString("grade") ?: "Class 10 · Student",
                                            pairedCardId = cardId,
                                            expiresAtMs = expiresAtMs,
                                            state = PairState.APPROVED
                                        )
                                        markPaired(req)
                                        trySend(PairState.APPROVED)
                                    } catch (e: Exception) {
                                        com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                                            "Pairing error: ${e.localizedMessage}"
                                        )
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // Do NOT mark paired or transition to approved on failure!
                                    com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                                        "Cannot read child data: ${e.localizedMessage ?: "Permission denied"}"
                                    )
                                }
                        } catch (e: Exception) {
                            com.focal.app.ui.components.PlatformPermissionsHelper.showToast(
                                "Error reading child profile: ${e.localizedMessage}"
                            )
                        }
                    }
                    "DECLINED" -> trySend(PairState.DECLINED)
                    "EXPIRED" -> trySend(PairState.EXPIRED)
                    else -> trySend(PairState.PENDING)
                }
            }

        awaitClose {
            reg.remove()
        }
    }

    override suspend fun cancelRequest(id: String) {
        activeRequest = null
        clearPendingRequest()
        // Note: Security rules for pairingCodes do not permit child to transition code state to CANCELLED.
        // Cancellation is handled locally on the child device.
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
            val parentName = FocalLocalStorage.getString(KEY_PENDING_PARENT_NAME, "Parent")
            val childName = FocalLocalStorage.getString(KEY_PENDING_CHILD_NAME, "Child")
            val cardId = FocalLocalStorage.getString(KEY_PENDING_CARD_ID, "FC0001")

            val restored = PairRequest(
                id = reqId,
                code = code,
                parentId = "parent_01",
                parentName = parentName,
                parentInitials = parentName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "PS" },
                childId = "child_01",
                childName = childName,
                childInitials = childName.take(2).uppercase().ifEmpty { "AR" },
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
        FocalLocalStorage.saveString("KEY_PAIRED_CHILD_ID", request.childId)
        FocalLocalStorage.saveString("KEY_PAIRED_FAMILY_ID", request.parentId)
        clearPendingRequest()
        activeRequest = null

        val repo = FocalRepository.instance
        repo.currentUser = repo.currentUser.copy(
            id = request.childId,
            name = request.childName,
            role = UserRole.CHILD,
            familyId = request.parentId
        )
        repo.controllerUser = repo.controllerUser.copy(
            id = request.parentId,
            name = request.parentName,
            familyId = request.parentId
        )

        // Attach family listeners on child phone
        (repo as? FirestoreFocalRepository)?.attachFamily(request.parentId)

        // Sync apps immediately after pairing
        val context = com.focal.app.ui.components.PlatformPermissionsHelper.appContext
        if (context != null) {
            ChildAppScanner.syncApps(context, request.childId)
        }
    }

    override fun unlink() {
        FocalLocalStorage.saveBoolean(KEY_PAIRED_IS_PAIRED, false)
        FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", false)
        FocalLocalStorage.saveBoolean("KEY_SETUP_DONE", false)
        FocalLocalStorage.saveString("KEY_PAIRED_CHILD_ID", "")
        FocalLocalStorage.saveString("KEY_PAIRED_FAMILY_ID", "")
        clearPendingRequest()
        activeRequest = null
        parentSessions.clear()
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
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
