package com.example.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.service.FocusForegroundService
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.SessionState

class FocalBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                FocalRepository.instance.reportClockChanged()
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                com.focal.app.protection.ProtectionStateRepository.instance.loadInitialState()
                com.example.myapplication.schedule.FocalScheduleManager.restoreCachedSchedules(context)
                FocalRepository.instance.startChildSessionListener(context)
                val state = com.focal.app.protection.ProtectionStateRepository.instance.state.value
                val focusEngine = FocusEngine.instance
                if (state.focusActive || focusEngine.sessionState == SessionState.ACTIVE) {
                    FocalRepository.instance.reportDeviceRebootedDuringFocus()
                    FocusForegroundService.start(context)
                }
            }
        }
    }
}
