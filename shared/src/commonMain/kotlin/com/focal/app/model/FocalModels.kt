package com.focal.app.model

enum class UserRole {
    ADMIN,
    CONTROLLER, // Parent / Controller
    CHILD       // Child / User
}

enum class PlanType {
    NFC,
    QR
}

enum class SubscriptionStatus {
    TRIAL,
    ACTIVE,
    PAST_DUE,
    CANCELLED,
    EXPIRED
}

enum class FocusMode(val title: String, val iconDescription: String) {
    STUDY("Study", "Intense study session"),
    DEEP_WORK("Deep Work", "High concentration work"),
    WORK("Work", "Standard productive work"),
    SLEEP("Sleep", "Rest and sleep protection"),
    DIGITAL_DETOX("Digital Detox", "Complete device disconnect"),
    CUSTOM("Custom", "Customized restrictions")
}

enum class SessionState {
    IDLE,
    STARTING,
    ACTIVE,
    ENDING,
    COMPLETED,
    CANCELLED,
    EXPIRED
}

sealed class CardRegistrationInspection {
    object BlankTag : CardRegistrationInspection()
    data class UnownedInStock(val card: FocusCard) : CardRegistrationInspection()
    data class AlreadyOwnedByMe(val card: FocusCard) : CardRegistrationInspection()
    data class OwnedByOther(val card: FocusCard) : CardRegistrationInspection()
    data class Revoked(val card: FocusCard) : CardRegistrationInspection()
}

enum class CardStatus {
    IN_STOCK,
    REGISTERED,
    ASSIGNED,
    REVOKED,
    UNPROGRAMMED,
    PROGRAMMED,
    ACTIVE,
    BLOCKED,
    DEACTIVATED,
    DISABLED
}

enum class TriggerType {
    NFC,
    QR,
    SCHEDULED,
    REMOTE_PARENT,
    EMERGENCY_OVERRIDE,
    TIMER_EXPIRED
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val role: UserRole,
    val familyId: String = "family_1",
    val planType: PlanType = PlanType.NFC,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.TRIAL,
    val trialDaysRemaining: Int = 84, // 3 months free trial
    val nextBillingDate: String = "2026-12-18",
    val monthlyPriceInr: Int = 299,
    val nfcCardPaid: Boolean = true,
    val avatarUrl: String = "",
    val createdAt: String = "2026-09-01"
)

data class ChildProfile(
    val id: String,
    val userId: String,
    val controllerId: String,
    val name: String,
    val familyId: String = "family_1",
    val defaultPolicyId: String = "policy_study",
    val defaultFocusMinutes: Int = 90,
    val age: Int = 14,
    val grade: String = "",
    val avatar: String = "",
    val isOnline: Boolean = true,
    val isFocusing: Boolean = false,
    val isProtectionOff: Boolean = false,
    val todayFocusMinutes: Int = 155, // 02h 35m
    val screenTimeMinutes: Int = 192, // 03h 12m
    val focusScore: Int = 82,
    val timeSavedMinutes: Int = 100,  // 01h 40m
    val streakDays: Int = 12,
    val currentSessionId: String? = null,
    val remainingMinutes: Int = 0,
    val focusEndsAtMillis: Long = 0L,
    val focusTotalMinutes: Int = 90,
    val pairedCardId: String? = "FC0001",
    val deviceName: String = "Rahul's Galaxy Tab S9",
    val cardCustodyMode: String = "Card stays with child" // "Card stays with child" (default) or "I keep the card"
) {
    val displayGrade: String
        get() = when {
            grade.isNotBlank() -> grade
            name.equals("Aarav", ignoreCase = true) -> "Class 10"
            name.equals("Kiara", ignoreCase = true) -> "Class 8"
            age >= 6 -> "Class ${age - 5}"
            else -> "Student"
        }
}

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val category: String, // "Social", "Video", "Games", "Shopping", "Browsers", "Other"
    val iconHash: String = "",
    val isSystemApp: Boolean = false,
    val childOwnerName: String = ""
)

data class FocusPolicy(
    val id: String,
    val childId: String,
    val name: String,
    val mode: FocusMode,
    val durationMinutes: Int,
    val blockedApps: List<String>,
    val blockedWebsites: List<String>,
    val blockedCategories: List<String> = emptyList(), // "Social", "Video", "Games", "Shopping"
    val alwaysAllowedApps: List<String> = listOf(
        "com.focal.app",
        "com.example.myapplication",
        "com.android.dialer",
        "com.android.phone",
        "com.google.android.dialer",
        "com.android.settings",
        "com.android.systemui",
        "com.apple.mobilephone"
    ),
    val scheduleDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
    val scheduleStartTime: String = "09:00",
    val scheduleEndTime: String = "13:00",
    val emergencyExitAllowed: Boolean = true,
    val parentApprovalRequired: Boolean = true
)

data class FocusPolicyData(
    val blockedApps: List<String> = emptyList(),
    val blockedPackages: Set<String> = emptySet(),
    val blockedCategories: Set<String> = emptySet(),
    val blockedWebsites: List<String> = emptyList()
)

data class FocusSession(
    val id: String,
    val userId: String,
    val childName: String,
    val policyId: String,
    val policyName: String,
    val mode: FocusMode,
    val triggerType: TriggerType,
    val startTimeMillis: Long,
    val targetDurationMinutes: Int,
    val remainingSeconds: Int,
    val status: SessionState,
    val blockedAttempts: Int = 0,
    val distractionsPrevented: Int = 0,
    val emergencyExits: Int = 0,
    val focusScore: Int = 85,
    val blockedAppsCount: Int = 7,
    val blockedWebsitesCount: Int = 12,
    val endTimeMillis: Long = startTimeMillis + (targetDurationMinutes * 60 * 1000L),
    val startElapsedRealtime: Long = 0L,
    val endedAtMillis: Long? = null
) {
    fun getRemainingMillis(
        nowElapsed: Long = com.focal.app.streak.getDeviceElapsedRealtime(),
        nowWall: Long = com.focal.app.streak.getDeviceCurrentTimeMillis()
    ): Long {
        val durationMs = targetDurationMinutes * 60 * 1000L
        // 1. Monotonic elapsed realtime check (elapsedRealtime monotonically advances)
        if (startElapsedRealtime > 0L && nowElapsed >= startElapsedRealtime) {
            val elapsedSinceStart = nowElapsed - startElapsedRealtime
            return (durationMs - elapsedSinceStart).coerceAtLeast(0L)
        }
        // 2. Post-reboot fallback (nowElapsed < startElapsedRealtime because boot counter reset)
        val wallRemaining = endTimeMillis - nowWall
        // If the wall clock jumped forward more than the remaining time, keep focus active instead of ending
        if (wallRemaining <= 0L) {
            return 60_000L // Keep focus active
        }
        return wallRemaining.coerceAtLeast(0L)
    }

    val actualDurationMillis: Long
        get() = ((endedAtMillis ?: currentTimeMillis()) - startTimeMillis).coerceAtLeast(0L)

    val actualDurationMinutes: Int
        get() = (actualDurationMillis / 60000L).toInt()

    fun formatActualFocusedTime(): String {
        val durationMs = actualDurationMillis
        if (durationMs < 60_000L) return "<1m"
        val totalMins = (durationMs / 60000L).toInt()
        val hours = totalMins / 60
        val mins = totalMins % 60
        return when {
            hours > 0 && mins == 0 -> "${hours}h 00m"
            hours > 0 -> "${hours}h ${if (mins < 10) "0$mins" else "$mins"}m"
            else -> "${mins}m"
        }
    }
}

data class FocusCard(
    val id: String,
    val cardId: String,           // e.g. FC0001
    val token: String,            // e.g. 8XK92LM7
    val familyId: String = "family_1",
    val isEnabled: Boolean = true,
    val label: String = "Family card",
    val ownerParentId: String? = null,
    val assignedChildId: String? = null, // Deprecated: cards are family-wide
    val status: CardStatus = CardStatus.ACTIVE,
    val registeredAt: String = "2026-10-08",
    val nfcUid: String = "04:A3:91:7B:22:6C",
    val ownerId: String? = ownerParentId,
    val ownerName: String? = null,
    val createdAt: String = registeredAt,
    val assignedAt: String? = null,
    val ndefPayload: String = "FOCUSCARD:$token",
    val lastUsed: String = "Today at 09:30 AM",
    val lastUsedChildName: String? = "Aarav",
    val tapCount: Int = 0,
    val lastTapTime: String? = null
)

data class QrToken(
    val id: String,
    val token: String,
    val ownerId: String,
    val childId: String,
    val policyId: String,
    val policyTitle: String,
    val durationMinutes: Int,
    val status: String = "ACTIVE",
    val createdAt: String,
    val expiresAt: String,
    val scanCount: Int = 0
)

enum class EmergencyRequestStatus {
    PENDING,
    APPROVED,
    DECLINED,
    CANCELLED,
    EXPIRED
}

data class EmergencyExitRequest(
    val id: String,
    val sessionId: String,
    val childId: String,
    val childName: String,
    val reason: String,
    val note: String = "",
    val requestedAt: String,
    val requestedAtMillis: Long = 0L,
    val status: String = "PENDING" // PENDING | APPROVED | DECLINED | CANCELLED | EXPIRED
) {
    val isPending: Boolean get() = status == "PENDING"
    val isApproved: Boolean get() = status == "APPROVED"
    val isDeclined: Boolean get() = status == "DECLINED" || status == "REJECTED"
    val isCancelled: Boolean get() = status == "CANCELLED"
    val isExpired: Boolean get() = status == "EXPIRED"
}

data class ScheduleItem(
    val id: String,
    val title: String,
    val timeRange: String,
    val days: String,
    val mode: FocusMode,
    val isEnabled: Boolean,
    val appsCount: Int,
    val websitesCount: Int,
    val policyName: String = "Study Mode",
    val allowCardExit: Boolean = false,
    val childId: String = ""
)

data class FriendUser(
    val id: String,
    val name: String,
    val avatarInitials: String,
    val streakDays: Int,
    val weeklyFocusHours: Double,
    val focusScore: Int,
    val isFocusingNow: Boolean
)

data class NotificationItem(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false
)

data class DistractionAppStat(
    val appName: String,
    val blockedCount: Int,
    val percentage: Float
)

data class DailyFocusBar(
    val day: String,
    val focusHours: Float,
    val screenHours: Float
)

enum class ParentSubscriptionStatus {
    TRIAL_3_MONTHS,
    ACTIVE_MONTHLY,
    SUSPENDED,
    EXPIRED
}

data class ParentAccount(
    val id: String,
    val name: String,
    val email: String,
    val childrenIds: List<String>,
    val subscriptionStatus: ParentSubscriptionStatus = ParentSubscriptionStatus.TRIAL_3_MONTHS,
    val trialDaysLeft: Int = 84,
    val registeredDate: String = "2026-08-01"
)

data class AdminAuditLog(
    val id: String,
    val adminId: String,
    val action: String,
    val target: String,
    val timestamp: String
)

data class AdminDeviceItem(
    val id: String,
    val childName: String,
    val parentName: String,
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String = "1.2.0",
    val isOnline: Boolean,
    val isProtectionActive: Boolean,
    val lastHeartbeat: String,
    val isFocusing: Boolean = false,
    val batteryPercent: Int = 85,
    val accessibilityGranted: Boolean = true,
    val overlayGranted: Boolean = true,
    val deviceAdminGranted: Boolean = true,
    val usageGranted: Boolean = true,
    val notificationsGranted: Boolean = true,
    val batteryUnrestricted: Boolean = true
)

