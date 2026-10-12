package com.focal.app.protection

import com.focal.app.data.FocalLocalStorage
import com.focal.app.model.FocusPolicyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProtectionStateRepository private constructor() {

    companion object {
        val instance: ProtectionStateRepository by lazy { ProtectionStateRepository() }
        private const val MAX_LOG_ENTRIES = 100
    }

    private val _state = MutableStateFlow(ProtectionState())
    val state: StateFlow<ProtectionState> = _state.asStateFlow()

    init {
        loadInitialState()
    }

    fun loadInitialState() {
        val setupDone = FocalLocalStorage.getBoolean("KEY_SETUP_DONE", false) ||
                FocalLocalStorage.getBoolean("KEY_AUTH_DONE", false) ||
                FocalLocalStorage.getBoolean("KEY_PERSONALIZATION_DONE", true)
        val focusActive = FocalLocalStorage.getBoolean("KEY_FOCUS_ACTIVE", false)
        val endsAt = FocalLocalStorage.getLong("KEY_FOCUS_ENDS_AT_ELAPSED", 0L)
        
        _state.value = _state.value.copy(
            setupComplete = setupDone,
            focusActive = focusActive,
            focusEndsAtElapsed = endsAt
        )
    }

    fun updateSetupComplete(complete: Boolean) {
        FocalLocalStorage.saveBoolean("KEY_SETUP_DONE", complete)
        _state.value = _state.value.copy(setupComplete = complete)
    }

    fun updateFocusActive(active: Boolean, endsAtElapsed: Long, policy: FocusPolicyData) {
        FocalLocalStorage.saveBoolean("KEY_FOCUS_ACTIVE", active)
        FocalLocalStorage.saveLong("KEY_FOCUS_ENDS_AT_ELAPSED", endsAtElapsed)
        _state.value = _state.value.copy(
            focusActive = active,
            focusEndsAtElapsed = endsAtElapsed,
            policy = policy
        )
    }

    fun updateLockSettingsMode(mode: LockSettingsMode) {
        FocalLocalStorage.saveString("KEY_LOCK_SETTINGS_MODE", mode.name)
        _state.value = _state.value.copy(lockSettingsMode = mode)
    }

    fun verifyAndApplyParentPin(inputPin: String, currentElapsed: Long, nowRealtimeMs: Long = com.focal.app.model.currentTimeMillis()): Result<Boolean> {
        val currentState = _state.value

        if (nowRealtimeMs < currentState.pinCooldownUntilMs) {
            val minsLeft = ((currentState.pinCooldownUntilMs - nowRealtimeMs) / 60000L) + 1
            return Result.failure(Exception("Too many wrong PIN attempts. Cooldown active for $minsLeft min."))
        }

        if (inputPin == currentState.parentPinHash) {
            _state.value = _state.value.copy(
                pinFailedAttempts = 0,
                pinCooldownUntilMs = 0L,
                parentUnlockUntilElapsed = currentElapsed + 600000L
            )
            return Result.success(true)
        } else {
            val newFailed = currentState.pinFailedAttempts + 1
            val cooldownUntil = if (newFailed >= 5) nowRealtimeMs + 300000L else 0L
            val resetFailed = if (newFailed >= 5) 0 else newFailed

            _state.value = _state.value.copy(
                pinFailedAttempts = resetFailed,
                pinCooldownUntilMs = cooldownUntil
            )

            val attemptsLeft = 5 - resetFailed
            val errorMsg = if (cooldownUntil > 0L) {
                "5 wrong PIN attempts! Lockout active for 5 minutes."
            } else {
                "Incorrect PIN. $attemptsLeft attempts remaining."
            }
            return Result.failure(Exception(errorMsg))
        }
    }

    fun setParentPin(newPin: String) {
        FocalLocalStorage.saveString("KEY_PARENT_PIN", newPin)
        _state.value = _state.value.copy(parentPinHash = newPin)
    }

    fun unlockParentForDuration(durationMs: Long = 600000L, currentElapsed: Long) {
        val unlockUntil = currentElapsed + durationMs
        _state.value = _state.value.copy(parentUnlockUntilElapsed = unlockUntil)
    }

    fun clearParentUnlock() {
        _state.value = _state.value.copy(parentUnlockUntilElapsed = 0L)
    }

    fun recordDecision(entry: DecisionLogEntry) {
        val currentLogs = _state.value.lastDecisions.toMutableList()
        currentLogs.add(0, entry)
        if (currentLogs.size > MAX_LOG_ENTRIES) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _state.value = _state.value.copy(lastDecisions = currentLogs)
    }
}
