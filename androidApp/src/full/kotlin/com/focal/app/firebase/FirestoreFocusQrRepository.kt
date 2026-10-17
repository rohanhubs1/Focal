package com.focal.app.firebase

import com.focal.app.data.FamilyFocusQr
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.FocusQrRepository
import com.focal.app.model.currentTimeMillis
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreFocusQrRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FocusQrRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeKeyDocId: String? = null
    private var keyListenerReg: ListenerRegistration? = null

    private val _familyQr = MutableStateFlow(
        FamilyFocusQr(
            familyId = "",
            secretToken = "",
            qrPayload = "",
            createdAtMillis = 0L
        )
    )
    override val familyQr: StateFlow<FamilyFocusQr> = _familyQr.asStateFlow()

    init {
        val famId = resolveFamilyId()
        if (famId.isNotBlank()) {
            attachFamily(famId)
        }
    }

    private fun resolveFamilyId(): String {
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            return user.uid
        }
        return ""
    }

    fun attachFamily(familyId: String) {
        if (familyId.isBlank()) return
        val user = auth.currentUser
        // Only parents query/listen to families/{familyId}/qrKeys collection
        if (user != null && !user.isAnonymous && user.uid == familyId) {
            keyListenerReg?.remove()
            keyListenerReg = firestore.collection("families").document(familyId)
                .collection("qrKeys")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    if (snapshot.isEmpty) {
                        // No key exists yet, create one
                        scope.launch {
                            createFirstQrKey(familyId)
                        }
                    } else {
                        val doc = snapshot.documents.first()
                        val key = doc.id
                        val createdAt = doc.getLong("createdAt") ?: currentTimeMillis()
                        activeKeyDocId = key
                        val payload = "focal://focus?f=$familyId&k=$key"
                        _familyQr.value = FamilyFocusQr(
                            familyId = familyId,
                            secretToken = key,
                            qrPayload = payload,
                            createdAtMillis = createdAt
                        )
                    }
                }
        }
    }

    private suspend fun createFirstQrKey(familyId: String) {
        try {
            val key = generateRandomKey()
            val now = currentTimeMillis()
            firestore.collection("families").document(familyId)
                .collection("qrKeys").document(key)
                .set(mapOf("createdAt" to now))
                .await()
            activeKeyDocId = key
            val payload = "focal://focus?f=$familyId&k=$key"
            _familyQr.value = FamilyFocusQr(
                familyId = familyId,
                secretToken = key,
                qrPayload = payload,
                createdAtMillis = now
            )
        } catch (e: Exception) {
            PlatformPermissionsHelper.showToast("Failed to initialize Focus QR: ${e.localizedMessage}")
        }
    }

    override fun getFamilyQr(familyId: String): FamilyFocusQr {
        val effectiveFamilyId = familyId.ifBlank { resolveFamilyId() }
        if (_familyQr.value.familyId != effectiveFamilyId || _familyQr.value.qrPayload.isBlank()) {
            attachFamily(effectiveFamilyId)
        }
        return _familyQr.value
    }

    override fun resetFamilyQr(familyId: String): FamilyFocusQr {
        val effectiveFamilyId = familyId.ifBlank { resolveFamilyId() }
        val oldKey = activeKeyDocId ?: _familyQr.value.secretToken
        val newKey = generateRandomKey()
        val now = currentTimeMillis()
        val payload = "focal://focus?f=$effectiveFamilyId&k=$newKey"

        scope.launch {
            try {
                val batch = firestore.batch()
                if (!oldKey.isNullOrBlank()) {
                    val oldRef = firestore.collection("families").document(effectiveFamilyId)
                        .collection("qrKeys").document(oldKey)
                    batch.delete(oldRef)
                }
                val newRef = firestore.collection("families").document(effectiveFamilyId)
                    .collection("qrKeys").document(newKey)
                batch.set(newRef, mapOf("createdAt" to now))
                batch.commit().await()

                activeKeyDocId = newKey
                PlatformPermissionsHelper.showToast("Family QR has been reset")
            } catch (e: Exception) {
                PlatformPermissionsHelper.showToast("Failed to reset QR: ${e.localizedMessage}")
            }
        }

        val updated = FamilyFocusQr(
            familyId = effectiveFamilyId,
            secretToken = newKey,
            qrPayload = payload,
            createdAtMillis = now
        )
        _familyQr.value = updated
        return updated
    }

    override fun validateFamilyQr(payload: String, familyId: String): Boolean {
        val trimmed = payload.trim()
        if (trimmed.startsWith("focal://pair", ignoreCase = true)) {
            return false
        }
        val targetFamily = familyId.ifBlank { resolveFamilyId() }
        if (!trimmed.startsWith("focal://focus", ignoreCase = true)) {
            return false
        }
        val fParam = trimmed.substringAfter("f=", "").substringBefore("&").trim()
        return fParam == targetFamily && fParam.isNotBlank()
    }

    private fun generateRandomKey(): String {
        val uuidPart = UUID.randomUUID().toString().replace("-", "")
        val timePart = System.currentTimeMillis().toString(36)
        return (uuidPart + timePart).take(32)
    }
}
