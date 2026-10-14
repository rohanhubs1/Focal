package com.focal.app.data

import com.focal.app.engine.FocusEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface EmergencyRequestState {
    object Idle : EmergencyRequestState
    object Pending : EmergencyRequestState
    object Approved : EmergencyRequestState
    data class Rejected(val reason: String = "") : EmergencyRequestState
}

interface EmergencyExitRepository {
    val requestState: StateFlow<EmergencyRequestState>
    fun request(reason: String): Flow<EmergencyRequestState>
    fun reset()

    companion object {
        val instance: EmergencyExitRepository by lazy { DefaultEmergencyExitRepository() }
    }
}

class DefaultEmergencyExitRepository : EmergencyExitRepository {
    private val _requestState = MutableStateFlow<EmergencyRequestState>(EmergencyRequestState.Idle)
    override val requestState: StateFlow<EmergencyRequestState> = _requestState.asStateFlow()

    override fun request(reason: String): Flow<EmergencyRequestState> {
        _requestState.value = EmergencyRequestState.Pending
        try {
            val session = FocusEngine.instance.currentSession
            if (session != null) {
                FocalRepository.instance.submitEmergencyExitRequest(session.id, reason)
                FocalSyncEngine.instance.dispatchEmergencyExitRequest(session.id, reason)
            }
        } catch (_: Exception) {}
        return _requestState.asStateFlow()
    }

    override fun reset() {
        _requestState.value = EmergencyRequestState.Idle
    }
}
