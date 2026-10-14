package com.focal.app.data

import com.focal.app.model.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ActivityEventType {
    FOCUS_STARTED,
    FOCUS_ENDED,
    FOCUS_COMPLETED,
    APP_BLOCKED,
    SITE_BLOCKED,
    EXIT_REQUESTED,
    EXIT_APPROVED,
    EXIT_DECLINED,
    PROTECTION_OFF,
    PROTECTION_ON,
    PHONE_PAIRED
}

data class ActivityEvent(
    val id: String,
    val type: ActivityEventType,
    val childId: String,
    val timestampMillis: Long,
    val timeFormatted: String,
    val timeClock: String = "10:48",
    val dayGroup: String = "TODAY",
    val details: String,
    val metadata: Map<String, String> = emptyMap(),
    val isRead: Boolean = false,
    val isPending: Boolean = false
)

fun ActivityEvent.getDisplayTitle(): String {
    return when (type) {
        ActivityEventType.FOCUS_STARTED -> {
            val trigger = metadata["trigger"] ?: "card"
            "Started focus with $trigger"
        }
        ActivityEventType.FOCUS_COMPLETED -> "Finished focus"
        ActivityEventType.FOCUS_ENDED -> {
            val trigger = metadata["trigger"] ?: "card"
            "Ended focus early"
        }
        ActivityEventType.APP_BLOCKED -> {
            val app = metadata["app"] ?: "YouTube"
            val count = metadata["count"]?.toIntOrNull() ?: 1
            if (count > 1) "Tried to open $app · $count times" else "Tried to open $app"
        }
        ActivityEventType.SITE_BLOCKED -> {
            val domain = metadata["domain"] ?: "website"
            "Tried to open $domain"
        }
        ActivityEventType.EXIT_REQUESTED -> "Asked to end focus early"
        ActivityEventType.EXIT_APPROVED -> "You allowed an emergency exit"
        ActivityEventType.EXIT_DECLINED -> "You declined an emergency exit"
        ActivityEventType.PROTECTION_OFF -> "Protection is off"
        ActivityEventType.PROTECTION_ON -> "Protection is back on"
        ActivityEventType.PHONE_PAIRED -> "Phone connected"
    }
}

fun ActivityEvent.getDisplayDetail(): String {
    return when (type) {
        ActivityEventType.FOCUS_STARTED -> {
            val policy = metadata["policy"] ?: "Study Mode"
            val duration = metadata["duration"] ?: "1h 30m"
            "$policy · $duration"
        }
        ActivityEventType.FOCUS_COMPLETED -> {
            val policy = metadata["policy"] ?: "Homework"
            val duration = metadata["duration"] ?: "2h 00m"
            "$policy · $duration"
        }
        ActivityEventType.FOCUS_ENDED -> {
            val trigger = metadata["trigger"] ?: "card"
            "With $trigger"
        }
        ActivityEventType.APP_BLOCKED -> {
            val policy = metadata["policy"] ?: "Study Mode"
            "Blocked · $policy"
        }
        ActivityEventType.SITE_BLOCKED -> {
            val policy = metadata["policy"] ?: "Study Mode"
            "Blocked · $policy"
        }
        ActivityEventType.EXIT_REQUESTED -> {
            val reason = metadata["reason"] ?: details
            "$reason · waiting for you"
        }
        ActivityEventType.EXIT_APPROVED -> {
            metadata["reason"] ?: details
        }
        ActivityEventType.EXIT_DECLINED -> {
            metadata["reason"] ?: details
        }
        ActivityEventType.PROTECTION_OFF -> {
            val perm = metadata["permission"] ?: "Accessibility"
            "$perm was turned off"
        }
        ActivityEventType.PROTECTION_ON -> "All permissions on"
        ActivityEventType.PHONE_PAIRED -> {
            metadata["device"] ?: "Realme · Android 14"
        }
    }
}

interface ActivityRepository {
    val events: StateFlow<List<ActivityEvent>>
    val hasUnread: StateFlow<Boolean>
    fun recordEvent(
        type: ActivityEventType,
        childId: String,
        details: String,
        metadata: Map<String, String> = emptyMap()
    ): ActivityEvent
    fun getEventsForChild(childId: String): List<ActivityEvent>
    fun markAllAsRead()
    fun clearEvents()

    companion object {
        val instance: ActivityRepository by lazy { LocalActivityRepository() }
    }
}

class LocalActivityRepository : ActivityRepository {
    private val _events = MutableStateFlow<List<ActivityEvent>>(createInitialEvents())
    override val events: StateFlow<List<ActivityEvent>> = _events.asStateFlow()

    private val _hasUnread = MutableStateFlow(true)
    override val hasUnread: StateFlow<Boolean> = _hasUnread.asStateFlow()

    override fun recordEvent(
        type: ActivityEventType,
        childId: String,
        details: String,
        metadata: Map<String, String>
    ): ActivityEvent {
        val now = currentTimeMillis()
        val timeClockStr = formatClockTime(now)
        val event = ActivityEvent(
            id = "act_${now}_${(1000..9999).random()}",
            type = type,
            childId = childId,
            timestampMillis = now,
            timeFormatted = formatEventTime(now),
            timeClock = timeClockStr,
            dayGroup = "TODAY",
            details = details,
            metadata = metadata,
            isRead = false,
            isPending = (type == ActivityEventType.EXIT_REQUESTED)
        )
        val currentList = _events.value.toMutableList()
        currentList.add(0, event)
        _events.value = currentList
        _hasUnread.value = true
        return event
    }

    override fun getEventsForChild(childId: String): List<ActivityEvent> {
        return _events.value.filter { it.childId == childId }
    }

    override fun markAllAsRead() {
        _hasUnread.value = false
        val currentList = _events.value.map { it.copy(isRead = true) }
        _events.value = currentList
    }

    override fun clearEvents() {
        _events.value = emptyList()
        _hasUnread.value = false
    }

    private fun formatEventTime(millis: Long): String {
        val secondsAgo = ((currentTimeMillis() - millis) / 1000).coerceAtLeast(0)
        return when {
            secondsAgo < 60 -> "Just now"
            secondsAgo < 3600 -> "${secondsAgo / 60}m ago"
            secondsAgo < 86400 -> "${secondsAgo / 3600}h ago"
            else -> "${secondsAgo / 86400}d ago"
        }
    }

    private fun formatClockTime(millis: Long): String {
        val time = com.focal.app.streak.getDeviceCurrentTime()
        val h = time.hour24.toString().padStart(2, '0')
        val m = time.minute.toString().padStart(2, '0')
        return "$h:$m"
    }

    private fun createInitialEvents(): List<ActivityEvent> {
        val now = currentTimeMillis()
        return listOf(
            ActivityEvent(
                id = "act_init_1",
                type = ActivityEventType.EXIT_REQUESTED,
                childId = "child_1",
                timestampMillis = now - 120_000,
                timeFormatted = "2m ago",
                timeClock = "10:48",
                dayGroup = "TODAY",
                details = "Need to call someone",
                metadata = mapOf("reason" to "Need to call someone", "requestId" to "req_1"),
                isRead = false,
                isPending = true
            ),
            ActivityEvent(
                id = "act_init_2",
                type = ActivityEventType.APP_BLOCKED,
                childId = "child_1",
                timestampMillis = now - 28 * 60_000,
                timeFormatted = "28m ago",
                timeClock = "10:20",
                dayGroup = "TODAY",
                details = "Blocked YouTube",
                metadata = mapOf("app" to "YouTube", "policy" to "Study Mode"),
                isRead = false
            ),
            ActivityEvent(
                id = "act_init_3",
                type = ActivityEventType.FOCUS_STARTED,
                childId = "child_1",
                timestampMillis = now - 46 * 60_000,
                timeFormatted = "46m ago",
                timeClock = "10:02",
                dayGroup = "TODAY",
                details = "Started focus with card",
                metadata = mapOf("trigger" to "card", "policy" to "Study Mode", "duration" to "1h 30m"),
                isRead = false
            ),
            ActivityEvent(
                id = "act_init_4",
                type = ActivityEventType.PROTECTION_OFF,
                childId = "child_2",
                timestampMillis = now - 68 * 60_000,
                timeFormatted = "1h ago",
                timeClock = "09:40",
                dayGroup = "TODAY",
                details = "Accessibility was turned off",
                metadata = mapOf("permission" to "Accessibility"),
                isRead = false
            ),
            ActivityEvent(
                id = "act_init_5",
                type = ActivityEventType.FOCUS_COMPLETED,
                childId = "child_2",
                timestampMillis = now - 86400_000 + 3600_000,
                timeFormatted = "Yesterday",
                timeClock = "18:00",
                dayGroup = "YESTERDAY",
                details = "Finished focus",
                metadata = mapOf("policy" to "Homework", "duration" to "2h 00m"),
                isRead = false
            ),
            ActivityEvent(
                id = "act_init_6",
                type = ActivityEventType.EXIT_APPROVED,
                childId = "child_1",
                timestampMillis = now - 86400_000 + 48 * 60_000,
                timeFormatted = "Yesterday",
                timeClock = "17:12",
                dayGroup = "YESTERDAY",
                details = "Homework app",
                metadata = mapOf("reason" to "Homework app"),
                isRead = false
            )
        )
    }
}
