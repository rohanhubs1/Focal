package com.example.myapplication.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.SessionState
import com.focal.app.model.TriggerType
import java.util.Calendar

class FocalScheduleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_START_SCHEDULE = "com.focal.app.ACTION_START_SCHEDULE"
        const val ACTION_END_SCHEDULE = "com.focal.app.ACTION_END_SCHEDULE"
        const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        const val EXTRA_POLICY_ID = "EXTRA_POLICY_ID"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return
        val policyId = intent.getStringExtra(EXTRA_POLICY_ID) ?: ""

        val repo = FocalRepository.instance
        val engine = FocusEngine.instance

        when (action) {
            ACTION_START_SCHEDULE -> {
                if (engine.sessionState != SessionState.ACTIVE) {
                    val policy = repo.policies.find { it.id == policyId } ?: repo.policies.firstOrNull()
                    if (policy != null) {
                        engine.startFocus(TriggerType.SCHEDULED, "schedule_$scheduleId", policy)
                    }
                }
            }
            ACTION_END_SCHEDULE -> {
                if (engine.sessionState == SessionState.ACTIVE) {
                    engine.endFocus(TriggerType.SCHEDULED, "schedule_$scheduleId")
                }
            }
        }
    }
}

object FocalScheduleManager {

    fun scheduleAlarm(context: Context, scheduleId: String, policyId: String, startHour: Int, startMinute: Int, durationMinutes: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val now = Calendar.getInstance()

            // Start Alarm
            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val startIntent = Intent(context, FocalScheduleReceiver::class.java).apply {
                action = FocalScheduleReceiver.ACTION_START_SCHEDULE
                putExtra(FocalScheduleReceiver.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(FocalScheduleReceiver.EXTRA_POLICY_ID, policyId)
            }

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val startPendingIntent = PendingIntent.getBroadcast(
                context,
                (scheduleId + "_start").hashCode(),
                startIntent,
                pendingFlags
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    startCal.timeInMillis,
                    startPendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    startCal.timeInMillis,
                    startPendingIntent
                )
            }

            // End Alarm
            val endCal = (startCal.clone() as Calendar).apply {
                add(Calendar.MINUTE, durationMinutes)
            }

            val endIntent = Intent(context, FocalScheduleReceiver::class.java).apply {
                action = FocalScheduleReceiver.ACTION_END_SCHEDULE
                putExtra(FocalScheduleReceiver.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(FocalScheduleReceiver.EXTRA_POLICY_ID, policyId)
            }

            val endPendingIntent = PendingIntent.getBroadcast(
                context,
                (scheduleId + "_end").hashCode(),
                endIntent,
                pendingFlags
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endCal.timeInMillis,
                    endPendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    endCal.timeInMillis,
                    endPendingIntent
                )
            }
        } catch (_: Exception) {}
    }
}
