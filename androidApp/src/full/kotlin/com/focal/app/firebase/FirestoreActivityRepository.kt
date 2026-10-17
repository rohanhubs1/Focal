package com.focal.app.firebase

import com.focal.app.data.ActivityEvent
import com.focal.app.data.ActivityEventType
import com.focal.app.data.ActivityRepository
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.FocalRepository
import com.focal.app.model.currentTimeMillis
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreActivityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ActivityRepository {

    private val _events = MutableStateFlow<List<ActivityEvent>>(emptyList())
    override val events: StateFlow<List<ActivityEvent>> = _events.asStateFlow()

    private val _hasUnread = MutableStateFlow(false)
    override val hasUnread: StateFlow<Boolean> = _hasUnread.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    init {
        val familyId = FocalRepository.instance.currentUser.familyId
        if (familyId.isNotBlank()) {
            attachFamily(familyId)
        }
    }

    fun attachFamily(familyId: String) {
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (authUser == null || authUser.isAnonymous) {
            // Child phone only writes activity events; security rules grant read access to parent only
            return
        }
        listenerRegistration?.remove()
        listenerRegistration = firestore.collection("activity")
            .whereEqualTo("familyId", familyId)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                val parsedEvents = snapshots.documents.mapNotNull { doc ->
                    try {
                        val typeStr = doc.getString("type") ?: return@mapNotNull null
                        val eventType = try {
                            ActivityEventType.valueOf(typeStr)
                        } catch (e: Exception) {
                            ActivityEventType.FOCUS_STARTED
                        }
                        val childId = doc.getString("childId") ?: "child_01"
                        val timestamp = doc.getLong("timestampMillis") ?: currentTimeMillis()
                        val details = doc.getString("details") ?: ""
                        @Suppress("UNCHECKED_CAST")
                        val metadata = (doc.get("metadata") as? Map<String, String>) ?: emptyMap()
                        val isRead = doc.getBoolean("isRead") ?: false

                        ActivityEvent(
                            id = doc.id,
                            type = eventType,
                            childId = childId,
                            timestampMillis = timestamp,
                            timeFormatted = formatEventTime(timestamp),
                            timeClock = formatClockTime(timestamp),
                            dayGroup = if (isToday(timestamp)) "TODAY" else "YESTERDAY",
                            details = details,
                            metadata = metadata,
                            isRead = isRead,
                            isPending = (eventType == ActivityEventType.EXIT_REQUESTED && !(doc.getBoolean("resolved") ?: false))
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _events.value = parsedEvents
                _hasUnread.value = parsedEvents.any { !it.isRead }
            }
    }

    override fun recordEvent(
        type: ActivityEventType,
        childId: String,
        details: String,
        metadata: Map<String, String>
    ): ActivityEvent {
        val now = currentTimeMillis()
        val isChildPhone = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
        val realChildId = if (isChildPhone) {
            FocalLocalStorage.getString("KEY_PAIRED_CHILD_ID", "").ifBlank { childId }
        } else {
            childId
        }
        val realFamilyId = if (isChildPhone) {
            FocalLocalStorage.getString("KEY_PAIRED_FAMILY_ID", "").ifBlank { FocalRepository.instance.currentUser.familyId }
        } else {
            FocalRepository.instance.currentUser.familyId.ifBlank {
                FocalRepository.instance.controllerUser.familyId.ifBlank {
                    FocalRepository.instance.controllerUser.id
                }
            }
        }
        val eventId = "act_${now}_${(1000..9999).random()}"

        val event = ActivityEvent(
            id = eventId,
            type = type,
            childId = realChildId,
            timestampMillis = now,
            timeFormatted = "Just now",
            timeClock = formatClockTime(now),
            dayGroup = "TODAY",
            details = details,
            metadata = metadata,
            isRead = false,
            isPending = (type == ActivityEventType.EXIT_REQUESTED)
        )

        // Add to local flow immediately
        val currentList = _events.value.toMutableList()
        currentList.add(0, event)
        _events.value = currentList
        _hasUnread.value = true

        // Persist to Firestore /activity/{eventId}
        if (realFamilyId.isNotBlank() && realChildId.isNotBlank()) {
            val docData = hashMapOf(
                "id" to eventId,
                "familyId" to realFamilyId,
                "childId" to realChildId,
                "type" to type.name,
                "timestampMillis" to now,
                "details" to details,
                "metadata" to metadata,
                "isRead" to false,
                "resolved" to false
            )
            firestore.collection("activity").document(eventId).set(docData)
        }

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

    private fun isToday(millis: Long): Boolean {
        val diff = currentTimeMillis() - millis
        return diff < 24 * 60 * 60 * 1000L
    }
}
