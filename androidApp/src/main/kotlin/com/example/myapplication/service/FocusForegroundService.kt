package com.example.myapplication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.focal.app.engine.FocusEngine
import com.focal.app.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import com.focal.app.data.FocalRepository
import com.focal.app.ui.components.PlatformPermissionsHelper

class FocusForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "focal_focus_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_FOCUS_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_FOCUS_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startNotificationUpdates()
            }
        }
        return START_STICKY
    }

    private fun startNotificationUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            while (isActive) {
                val engine = FocusEngine.instance
                if (engine.sessionState != SessionState.ACTIVE) {
                    stopSelf()
                    break
                }
                
                // Backup 1-second UsageStatsManager detector
                checkForegroundAppBackup(engine)

                notificationManager.notify(NOTIFICATION_ID, buildNotification())
                delay(1000)
            }
        }
    }

    private fun checkForegroundAppBackup(engine: FocusEngine) {
        val state = com.focal.app.protection.ProtectionStateRepository.instance.state.value
        if (!state.focusActive) return
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val nowMs = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(nowMs - 3000L, nowMs) ?: return

        var fgPkg: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                fgPkg = event.packageName
            }
        }

        val topPkg = fgPkg ?: return
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val windowInfo = com.focal.app.protection.WindowInfo(
            packageName = topPkg,
            windowClass = "",
            fullText = "",
            urlText = null
        )

        val decision = com.focal.app.protection.ProtectionEngine.decide(
            windowInfo = windowInfo,
            state = state,
            nowElapsed = nowElapsed,
            ownPackage = packageName
        )

        if (decision !is com.focal.app.protection.ProtectionDecision.Allow) {
            ActionExecutor.execute(
                context = applicationContext,
                accessibilityService = FocalAccessibilityService.instance,
                windowInfo = windowInfo,
                decision = decision,
                currentElapsed = nowElapsed
            )
        }
    }

    private fun buildNotification(): Notification {
        val engine = FocusEngine.instance
        val remaining = engine.formatRemainingTime()
        val currentSession = engine.currentSession
        val policyTitle = currentSession?.policyName ?: "Focus Session"
        val blockedCount = currentSession?.blockedAppsCount ?: 6

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ FOCAL — $policyTitle Active")
            .setContentText("⏳ $remaining remaining • $blockedCount apps & websites restricted")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FOCAL Active Focus Sessions",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live remaining time and active app restriction barriers"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
