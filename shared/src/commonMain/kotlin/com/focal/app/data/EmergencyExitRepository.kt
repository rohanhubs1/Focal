package com.focal.app.data

import com.focal.app.engine.FocusEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface EmergencyRequestState {
    object Idle : EmergencyRequestState
    data class Pending(val requestId: String = "") : EmergencyRequestState
    data class Approved(val requestId: String = "") : EmergencyRequestState
    data class Declined(val requestId: String = "", val parentName: String = "Parent") : EmergencyRequestState
    data class Rejected(val reason: String = "") : EmergencyRequestState
    data class Failed(val message: String = "") : EmergencyRequestState
}

interface EmergencyExitRepository {
    val requestState: StateFlow<EmergencyRequestState>
    fun request(reason: String, note: String = ""): Flow<EmergencyRequestState>
    fun notifyPending(request: com.focal.app.model.EmergencyExitRequest)
    fun notifyApproved(requestId: String)
    fun notifyDeclined(requestId: String, parentName: String = "Parent")
    fun notifyFailed(message: String = "")
    fun reset()

    companion object {
        val instance: EmergencyExitRepository by lazy { DefaultEmergencyExitRepository() }
    }
}

class DefaultEmergencyExitRepository : EmergencyExitRepository {
    private val _requestState = MutableStateFlow<EmergencyRequestState>(EmergencyRequestState.Idle)
    override val requestState: StateFlow<EmergencyRequestState> = _requestState.asStateFlow()

    override fun request(reason: String, note: String): Flow<EmergencyRequestState> {
        val session = FocusEngine.instance.currentSession
        val req = if (session != null) {
            FocalRepository.instance.submitEmergencyExitRequest(session.id, reason, note)
        } else null

        _requestState.value = if (req != null) EmergencyRequestState.Pending(req.id) else EmergencyRequestState.Pending()
        try {
            if (session != null) {
                FocalSyncEngine.instance.dispatchEmergencyExitRequest(session.id, reason)
            }
        } catch (_: Exception) {}
        return _requestState.asStateFlow()
    }

    override fun notifyPending(request: com.focal.app.model.EmergencyExitRequest) {
        _requestState.value = EmergencyRequestState.Pending(request.id)
    }

    override fun notifyApproved(requestId: String) {
        _requestState.value = EmergencyRequestState.Approved(requestId)
    }

    override fun notifyDeclined(requestId: String, parentName: String) {
        _requestState.value = EmergencyRequestState.Declined(requestId, parentName)
    }

    override fun notifyFailed(message: String) {
        _requestState.value = EmergencyRequestState.Failed(message)
    }

    override fun reset() {
        _requestState.value = EmergencyRequestState.Idle
    }
}
