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
    DEACTIVATED
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
    val age: Int = 14,
    val avatar: String = "",
    val isOnline: Boolean = true,
    val isFocusing: Boolean = false,
    val todayFocusMinutes: Int = 155, // 02h 35m
    val screenTimeMinutes: Int = 192, // 03h 12m
    val focusScore: Int = 82,
    val timeSavedMinutes: Int = 100,  // 01h 40m
    val streakDays: Int = 12,
    val currentSessionId: String? = null,
    val remainingMinutes: Int = 0,
    val pairedCardId: String? = "FC0001",
    val deviceName: String = "Rahul's Galaxy Tab S9",
    val cardCustodyMode: String = "Card stays with child" // "Card stays with child" (default) or "I keep the card"
)

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val category: String, // "Social", "Video", "Games", "Shopping", "Other"
    val iconHash: String = "",
    val isSystemApp: Boolean = false
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
    val blockedWebsitesCount: Int = 12
)

data class FocusCard(
    val id: String,
    val cardId: String,           // e.g. FC0001
    val token: String,            // e.g. 8XK92LM7
    val ownerParentId: String? = null,
    val assignedChildId: String? = null,
    val status: CardStatus = CardStatus.IN_STOCK,
    val registeredAt: String = "2026-10-08",
    val nfcUid: String = "04:A3:91:7B:22:6C",
    val ownerId: String? = ownerParentId,
    val ownerName: String? = null,
    val createdAt: String = registeredAt,
    val assignedAt: String? = null,
    val ndefPayload: String = "FOCUSCARD:$token",
    val lastUsed: String = "Today at 09:30 AM"
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

data class EmergencyExitRequest(
    val id: String,
    val sessionId: String,
    val childId: String,
    val childName: String,
    val reason: String,
    val requestedAt: String,
    val requestedAtMillis: Long = 0L,
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED, EXPIRED
)

data class ScheduleItem(
    val id: String,
    val title: String,
    val timeRange: String,
    val days: String,
    val mode: FocusMode,
    val isEnabled: Boolean,
    val appsCount: Int,
    val websitesCount: Int
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
    val appVersion: String = "1.2.0-monochrome",
    val isOnline: Boolean,
    val isProtectionActive: Boolean,
    val lastHeartbeat: String,
    val accessibilityGranted: Boolean = true,
    val overlayGranted: Boolean = true,
    val usageGranted: Boolean = true,
    val notificationsGranted: Boolean = true
)

