package com.focal.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.focal.app.model.*

class FocalRepository private constructor() {

    companion object {
        val instance: FocalRepository by lazy { FocalRepository() }
    }

    // Current logged-in user
    var currentUser by mutableStateOf(
        User(
            id = "user_rahul",
            name = "Rahul Sharma",
            email = "rahul@focal.io",
            phone = "+91 98765 43210",
            role = UserRole.CHILD,
            planType = PlanType.NFC,
            subscriptionStatus = SubscriptionStatus.TRIAL,
            trialDaysRemaining = 84, // 3 months free
            nextBillingDate = "2026-12-18",
            monthlyPriceInr = 299,
            nfcCardPaid = true
        )
    )

    // Pre-configured users for instant role switching
    val adminUser = User(
        id = "user_admin",
        name = "Aman Verma (Admin)",
        email = "admin@focal.io",
        phone = "+91 99999 00000",
        role = UserRole.ADMIN,
        planType = PlanType.NFC,
        subscriptionStatus = SubscriptionStatus.ACTIVE,
        trialDaysRemaining = 365,
        nextBillingDate = "2027-01-01"
    )

    var controllerUser by mutableStateOf(
        User(
            id = "user_priya",
            name = "Priya Sharma",
            email = "priya@focal.io",
            phone = "+91 98111 22233",
            role = UserRole.CONTROLLER,
            planType = PlanType.NFC,
            subscriptionStatus = SubscriptionStatus.TRIAL,
            trialDaysRemaining = 84,
            nextBillingDate = "2026-12-18"
        )
    )

    val childRahul = User(
        id = "user_rahul",
        name = "Rahul Sharma",
        email = "rahul@focal.io",
        phone = "+91 98765 43210",
        role = UserRole.CHILD,
        planType = PlanType.NFC,
        subscriptionStatus = SubscriptionStatus.TRIAL,
        trialDaysRemaining = 84,
        nextBillingDate = "2026-12-18",
        nfcCardPaid = true
    )

    val childAnanya = User(
        id = "user_ananya",
        name = "Ananya Sharma",
        email = "ananya@focal.io",
        phone = "+91 98765 43211",
        role = UserRole.CHILD,
        planType = PlanType.QR,
        subscriptionStatus = SubscriptionStatus.TRIAL,
        trialDaysRemaining = 79,
        nextBillingDate = "2026-12-13",
        nfcCardPaid = false
    )

    // Children profiles managed by Priya (Controller)
    val childrenList = mutableStateListOf(
        ChildProfile(
            id = "child_1",
            userId = "user_ananya",
            controllerId = "user_priya",
            name = "Ananya",
            age = 11,
            isOnline = true,
            isFocusing = false,
            todayFocusMinutes = 0,
            screenTimeMinutes = 140, // 2h 20m
            focusScore = 95,
            timeSavedMinutes = 45,
            streakDays = 8,
            currentSessionId = null,
            remainingMinutes = 0,
            pairedCardId = null, // Programmed live in demo
            deviceName = "Ananya's Phone"
        )
    )

    // Installed Apps uploaded from Child Device
    val childInstalledApps = mutableStateListOf(
        InstalledApp("com.instagram.android", "Instagram", "Social", "hash1"),
        InstalledApp("com.zhiliaoapp.musically", "TikTok", "Social", "hash2"),
        InstalledApp("com.snapchat.android", "Snapchat", "Social", "hash3"),
        InstalledApp("com.google.android.youtube", "YouTube", "Video", "hash4"),
        InstalledApp("com.netflix.mediaclient", "Netflix", "Video", "hash5"),
        InstalledApp("com.supercell.clashofclans", "Clash of Clans", "Games", "hash6"),
        InstalledApp("com.roblox.client", "Roblox", "Games", "hash7"),
        InstalledApp("com.amazon.mShop.android.shopping", "Amazon", "Shopping", "hash8"),
        InstalledApp("com.flipkart.android", "Flipkart", "Shopping", "hash9"),
        InstalledApp("org.telegram.messenger", "Telegram", "Messaging", "hash10"),
        InstalledApp("com.whatsapp", "WhatsApp", "Messaging", "hash11")
    )

    // Sync installed app list from child device
    fun syncChildInstalledApps(apps: List<InstalledApp>) {
        childInstalledApps.clear()
        childInstalledApps.addAll(apps)
    }

    // Domain normalization & subdomain matching logic
    fun normalizeDomain(urlOrDomain: String): String {
        var clean = urlOrDomain.trim().lowercase()
        if (clean.startsWith("https://")) clean = clean.substring(8)
        if (clean.startsWith("http://")) clean = clean.substring(7)
        if (clean.startsWith("www.")) clean = clean.substring(4)
        val slashIdx = clean.indexOf('/')
        if (slashIdx != -1) clean = clean.substring(0, slashIdx)
        val colonIdx = clean.indexOf(':')
        if (colonIdx != -1) clean = clean.substring(0, colonIdx)
        return clean
    }

    fun isDomainBlocked(targetUrlOrDomain: String, blockedDomains: List<String>): Boolean {
        val cleanTarget = normalizeDomain(targetUrlOrDomain)
        if (cleanTarget.isEmpty()) return false
        return blockedDomains.any { rawBlocked ->
            val cleanBlocked = normalizeDomain(rawBlocked)
            cleanBlocked.isNotEmpty() && (cleanTarget == cleanBlocked || cleanTarget.endsWith(".$cleanBlocked"))
        }
    }

    // Focus Policies (2 Seeded Policies for Demo: Study Mode & Wind Down)
    val policies = mutableStateListOf(
        FocusPolicy(
            id = "pol_study",
            childId = "child_1",
            name = "Study Mode",
            mode = FocusMode.STUDY,
            durationMinutes = 60,
            blockedApps = listOf("Instagram", "TikTok", "Snapchat", "YouTube", "Netflix", "Clash of Clans", "Roblox", "Telegram"),
            blockedWebsites = listOf("instagram.com", "tiktok.com", "snapchat.com", "youtube.com", "netflix.com", "facebook.com", "reddit.com", "twitch.tv", "amazon.com", "flipkart.com"),
            scheduleDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
            scheduleStartTime = "16:00",
            scheduleEndTime = "17:00",
            emergencyExitAllowed = true,
            parentApprovalRequired = true
        ),
        FocusPolicy(
            id = "pol_wind_down",
            childId = "child_1",
            name = "Wind Down",
            mode = FocusMode.SLEEP,
            durationMinutes = 120,
            blockedApps = listOf("Instagram", "TikTok", "YouTube", "Netflix"),
            blockedWebsites = listOf("instagram.com", "tiktok.com", "youtube.com", "netflix.com", "facebook.com"),
            scheduleDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            scheduleStartTime = "21:00",
            scheduleEndTime = "23:00",
            emergencyExitAllowed = true,
            parentApprovalRequired = false
        )
    )

    // Registered NFC Cards (0 initially; programmed live during demo)
    val nfcCards = mutableStateListOf<FocusCard>()

    // QR Tokens
    val qrTokens = mutableStateListOf(
        QrToken(
            id = "qrt_1",
            token = "FOCAL_QR_892348_STUDY",
            ownerId = "user_priya",
            childId = "child_1",
            policyId = "pol_study",
            policyTitle = "Study Mode (6 Hours)",
            durationMinutes = 360,
            status = "ACTIVE",
            createdAt = "Today at 08:00 AM",
            expiresAt = "Today at 08:00 PM",
            scanCount = 3
        ),
        QrToken(
            id = "qrt_2",
            token = "FOCAL_QR_771829_DETOX",
            ownerId = "user_priya",
            childId = "child_2",
            policyId = "pol_deep_work",
            policyTitle = "Digital Detox (2 Hours)",
            durationMinutes = 120,
            status = "ACTIVE",
            createdAt = "Yesterday",
            expiresAt = "Tomorrow",
            scanCount = 1
        )
    )

    // Emergency Exit Requests
    val emergencyRequests = mutableStateListOf(
        EmergencyExitRequest(
            id = "req_1",
            sessionId = "sess_active_1",
            childId = "child_1",
            childName = "Rahul",
            reason = "Need to submit school science assignment on Google Classroom",
            requestedAt = "10 minutes ago",
            status = "PENDING"
        )
    )

    // Schedules
    val schedules = mutableStateListOf(
        ScheduleItem(
            id = "sch_1",
            title = "Morning Zen",
            timeRange = "6 AM – 9 AM",
            days = "Mon, Tue, Wed, Thu, Fri",
            mode = FocusMode.STUDY,
            isEnabled = true,
            appsCount = 8,
            websitesCount = 15
        ),
        ScheduleItem(
            id = "sch_2",
            title = "Deep Work",
            timeRange = "10 AM – 12 PM",
            days = "Mon, Tue, Wed, Thu, Fri",
            mode = FocusMode.DEEP_WORK,
            isEnabled = true,
            appsCount = 12,
            websitesCount = 20
        ),
        ScheduleItem(
            id = "sch_3",
            title = "Focus Zone",
            timeRange = "1 PM – 5 PM",
            days = "All Days",
            mode = FocusMode.WORK,
            isEnabled = false,
            appsCount = 6,
            websitesCount = 10
        ),
        ScheduleItem(
            id = "sch_4",
            title = "Wind Down",
            timeRange = "6 PM – 9 PM",
            days = "Mon, Tue, Wed, Thu, Fri",
            mode = FocusMode.DIGITAL_DETOX,
            isEnabled = true,
            appsCount = 15,
            websitesCount = 25
        ),
        ScheduleItem(
            id = "sch_5",
            title = "Night Guard",
            timeRange = "10 PM – 5 AM",
            days = "Every Night",
            mode = FocusMode.SLEEP,
            isEnabled = true,
            appsCount = 25,
            websitesCount = 50
        ),
        ScheduleItem(
            id = "sch_6",
            title = "Full Focus",
            timeRange = "24 Hours",
            days = "Weekends",
            mode = FocusMode.DIGITAL_DETOX,
            isEnabled = false,
            appsCount = 30,
            websitesCount = 60
        )
    )

    // Friends list
    val friends = mutableStateListOf(
        FriendUser("fr_1", "Aryan Mehta", "AM", 18, 24.5, 94, true),
        FriendUser("fr_2", "Sneha Roy", "SR", 15, 21.0, 89, false),
        FriendUser("fr_3", "Kabir Sen", "KS", 9, 14.0, 78, true),
        FriendUser("fr_4", "Diya Patel", "DP", 22, 31.5, 96, false),
        FriendUser("fr_5", "Rohan Das", "RD", 6, 9.5, 71, false)
    )

    // Notifications
    val notifications = mutableStateListOf(
        NotificationItem(
            id = "notif_1",
            userId = "user_priya",
            type = "EMERGENCY_REQUEST",
            title = "Emergency Exit Request",
            message = "Rahul requested an early exit: 'Need to submit school science assignment'",
            timestamp = "10m ago"
        ),
        NotificationItem(
            id = "notif_2",
            userId = "user_priya",
            type = "PROTECTION_OFF",
            title = "Focus Protection Disabled",
            message = "Rahul's phone disabled Accessibility Service permission.",
            timestamp = "15m ago"
        ),
        NotificationItem(
            id = "notif_3",
            userId = "user_priya",
            type = "DEVICE_OFFLINE",
            title = "Device Offline",
            message = "No signal from Aarav's phone for 30 min",
            timestamp = "30m ago"
        ),
        NotificationItem(
            id = "notif_4",
            userId = "user_priya",
            type = "SCHEDULE_RAN",
            title = "Schedule Activated",
            message = "Morning Zen routine activated on Ananya's device",
            timestamp = "1h ago"
        ),
        NotificationItem(
            id = "notif_5",
            userId = "user_rahul",
            type = "FOCUS_STARTED",
            title = "Session Started",
            message = "Rahul started Study Mode via NFC card FC0001",
            timestamp = "2h ago"
        ),
        NotificationItem(
            id = "notif_6",
            userId = "user_rahul",
            type = "FOCUS_ENDED",
            title = "Session Completed",
            message = "Rahul finished Study Mode. Focus Score: 92",
            timestamp = "3h ago"
        )
    )

    // Analytics Data
    val topDistractions = listOf(
        DistractionAppStat("Instagram", 14, 0.42f),
        DistractionAppStat("YouTube Shorts", 9, 0.28f),
        DistractionAppStat("Snapchat", 5, 0.15f),
        DistractionAppStat("Netflix", 3, 0.09f),
        DistractionAppStat("Discord", 2, 0.06f)
    )

    val dailyFocusHistory = listOf(
        DailyFocusBar("Mon", 4.2f, 2.8f),
        DailyFocusBar("Tue", 5.5f, 2.1f),
        DailyFocusBar("Wed", 3.8f, 3.4f),
        DailyFocusBar("Thu", 6.1f, 1.9f),
        DailyFocusBar("Fri", 5.0f, 2.5f),
        DailyFocusBar("Sat", 4.8f, 3.2f),
        DailyFocusBar("Sun", 5.8f, 2.0f)
    )

    // Admin platform metrics
    var totalUsersCount by mutableStateOf(1482)
    var nfcCustomersCount by mutableStateOf(624)
    var qrCustomersCount by mutableStateOf(858)
    var activeTrialsCount by mutableStateOf(412)
    var activeSubscriptionsCount by mutableStateOf(890)
    var expiredSubscriptionsCount by mutableStateOf(180)
    var monthlyRevenueInr by mutableStateOf(348000)
    var nfcCardSalesInr by mutableStateOf(623376)
    var activeFocusSessionsCount by mutableStateOf(342)
    var totalFocusHoursCount by mutableStateOf(48290)

    // Switch Role seamlessly
    fun switchUserRole(role: UserRole) {
        currentUser = when (role) {
            UserRole.ADMIN -> adminUser
            UserRole.CONTROLLER -> controllerUser
            UserRole.CHILD -> childRahul
        }
    }

    fun switchChild(childUser: User) {
        currentUser = childUser
    }

    // Server-side Subscription Verification
    fun canStartFocus(user: User): Pair<Boolean, String> {
        return when (user.subscriptionStatus) {
            SubscriptionStatus.TRIAL -> {
                if (user.trialDaysRemaining > 0) {
                    true to "Trial valid (${user.trialDaysRemaining} days remaining)"
                } else {
                    false to "Your free trial has expired. Subscribe for ₹299/mo to continue."
                }
            }
            SubscriptionStatus.ACTIVE -> true to "Subscription active"
            SubscriptionStatus.EXPIRED -> false to "Subscription expired. Please renew for ₹299/mo."
            SubscriptionStatus.PAST_DUE -> false to "Payment past due. Update billing to continue."
            SubscriptionStatus.CANCELLED -> false to "Subscription cancelled. Reactivate for ₹299/mo."
        }
    }

    private val usedQrNonces = mutableSetOf<String>()

    // Inspect tag payload for parent card registration flow
    fun inspectCardForRegistration(rawPayloadOrToken: String, parentId: String): CardRegistrationInspection {
        val token = if (rawPayloadOrToken.startsWith("FOCUSCARD:")) {
            rawPayloadOrToken.removePrefix("FOCUSCARD:")
        } else {
            rawPayloadOrToken
        }.trim()

        if (token.isBlank() || token.equals("BLANK", ignoreCase = true)) {
            return CardRegistrationInspection.BlankTag
        }

        val existingCard = nfcCards.find {
            it.token.equals(token, ignoreCase = true) ||
            it.cardId.equals(token, ignoreCase = true) ||
            it.ndefPayload.contains(token, ignoreCase = true)
        } ?: return CardRegistrationInspection.BlankTag

        return when (existingCard.status) {
            CardStatus.REVOKED, CardStatus.DEACTIVATED -> CardRegistrationInspection.Revoked(existingCard)
            CardStatus.REGISTERED, CardStatus.ASSIGNED -> {
                if (existingCard.ownerParentId == parentId || existingCard.ownerId == parentId) {
                    CardRegistrationInspection.AlreadyOwnedByMe(existingCard)
                } else {
                    CardRegistrationInspection.OwnedByOther(existingCard)
                }
            }
            else -> CardRegistrationInspection.UnownedInStock(existingCard)
        }
    }

    // Register an unowned inventory card to parent
    fun registerCardToParent(tokenOrCardId: String, parentId: String, parentName: String): FocusCard {
        val idx = nfcCards.indexOfFirst {
            it.token.equals(tokenOrCardId, ignoreCase = true) || it.cardId.equals(tokenOrCardId, ignoreCase = true)
        }
        val today = "2026-10-08"
        return if (idx >= 0) {
            val updated = nfcCards[idx].copy(
                ownerParentId = parentId,
                ownerId = parentId,
                ownerName = parentName,
                status = CardStatus.REGISTERED,
                registeredAt = today
            )
            nfcCards[idx] = updated
            updated
        } else {
            val newCardId = "FC000" + (nfcCards.size + 1)
            val newCard = FocusCard(
                id = "card_${currentTimeMillis()}",
                cardId = newCardId,
                token = tokenOrCardId,
                ownerParentId = parentId,
                ownerId = parentId,
                ownerName = parentName,
                assignedChildId = null,
                status = CardStatus.REGISTERED,
                registeredAt = today,
                ndefPayload = "FOCUSCARD:$tokenOrCardId"
            )
            nfcCards.add(0, newCard)
            newCard
        }
    }

    // Write & Register a fresh/blank tag
    fun writeAndRegisterBlankCard(parentId: String, parentName: String): FocusCard {
        val nextNum = (nfcCards.count() + 1).toString().padStart(4, '0')
        val cardId = "FC$nextNum"
        val generatedToken = "TK_" + (100000..999999).random()
        val today = "2026-10-08"

        val newCard = FocusCard(
            id = "card_${currentTimeMillis()}",
            cardId = cardId,
            token = generatedToken,
            ownerParentId = parentId,
            ownerId = parentId,
            ownerName = parentName,
            assignedChildId = null,
            status = CardStatus.REGISTERED,
            registeredAt = today,
            ndefPayload = "FOCUSCARD:$generatedToken"
        )
        nfcCards.add(0, newCard)
        return newCard
    }

    // Assign card to child
    fun assignCardToChild(cardId: String, childId: String): FocusCard? {
        val child = childrenList.find { it.id == childId || it.userId == childId } ?: return null
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx < 0) return null

        val updated = nfcCards[idx].copy(
            assignedChildId = child.id,
            status = CardStatus.ASSIGNED,
            assignedAt = "Today"
        )
        nfcCards[idx] = updated

        val cIdx = childrenList.indexOfFirst { it.id == child.id }
        if (cIdx >= 0) {
            childrenList[cIdx] = childrenList[cIdx].copy(pairedCardId = updated.cardId)
        }
        return updated
    }

    // Unpair card from child
    fun unpairCardFromChild(cardId: String) {
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            val card = nfcCards[idx]
            val oldChildId = card.assignedChildId
            val updated = card.copy(
                assignedChildId = null,
                status = CardStatus.REGISTERED,
                assignedAt = null
            )
            nfcCards[idx] = updated

            val cIdx = childrenList.indexOfFirst { it.id == oldChildId || it.pairedCardId.equals(card.cardId, ignoreCase = true) }
            if (cIdx >= 0) {
                childrenList[cIdx] = childrenList[cIdx].copy(pairedCardId = null)
            }
        }
    }

    // Remove & revoke card (permanent revocation)
    fun revokeCard(cardId: String) {
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            val card = nfcCards[idx]
            val oldChildId = card.assignedChildId
            val updated = card.copy(
                assignedChildId = null,
                ownerParentId = null,
                ownerId = null,
                status = CardStatus.REVOKED,
                assignedAt = null
            )
            nfcCards[idx] = updated

            val cIdx = childrenList.indexOfFirst { it.id == oldChildId || it.pairedCardId.equals(card.cardId, ignoreCase = true) }
            if (cIdx >= 0) {
                childrenList[cIdx] = childrenList[cIdx].copy(pairedCardId = null)
            }
        }
    }

    // Validate NFC card for starting focus (Child tap)
    fun validateNfcCardForChildStart(cardIdOrToken: String, childUser: User): Result<FocusCard> {
        val childProfile = childrenList.find { it.userId == childUser.id || it.name.equals(childUser.name, ignoreCase = true) }
        val childId = childProfile?.id ?: "child_1"

        val card = nfcCards.find {
            it.cardId.equals(cardIdOrToken, ignoreCase = true) ||
            it.token.equals(cardIdOrToken, ignoreCase = true) ||
            it.ndefPayload.contains(cardIdOrToken, ignoreCase = true)
        } ?: return Result.failure(Exception("This card isn't paired to you."))

        if (card.status != CardStatus.ASSIGNED) {
            return Result.failure(Exception("This card isn't paired to you."))
        }

        val isAssignedToThisChild = (card.assignedChildId == childId) ||
                (childProfile?.pairedCardId != null && childProfile.pairedCardId.equals(card.cardId, ignoreCase = true))

        if (!isAssignedToThisChild) {
            return Result.failure(Exception("This card isn't paired to you."))
        }

        val (canFocus, message) = canStartFocus(childUser)
        if (!canFocus) {
            return Result.failure(Exception(message))
        }

        return Result.success(card)
    }

    // Validate NFC card for ending focus early (Must be the SAME paired card)
    fun validateNfcCardForChildEnd(cardIdOrToken: String, childUser: User, currentSession: FocusSession?): Result<FocusCard> {
        val card = nfcCards.find {
            it.cardId.equals(cardIdOrToken, ignoreCase = true) ||
            it.token.equals(cardIdOrToken, ignoreCase = true) ||
            it.ndefPayload.contains(cardIdOrToken, ignoreCase = true)
        } ?: return Result.failure(Exception("Only your paired card FC0001 can unlock this session."))

        val childProfile = childrenList.find { it.userId == childUser.id || it.name.equals(childUser.name, ignoreCase = true) }
        val expectedCardId = childProfile?.pairedCardId ?: "FC0001"

        if (!card.cardId.equals(expectedCardId, ignoreCase = true)) {
            return Result.failure(Exception("Only your paired card $expectedCardId can unlock this session."))
        }

        return Result.success(card)
    }

    // Validate QR token for starting focus (Single-use, 10-minute expiry, child-bound)
    fun validateQrTokenForChildStart(qrPayload: String, childUser: User): Result<Pair<FocusPolicy, Int>> {
        val qrToken = qrTokens.find { it.token == qrPayload }

        if (qrToken != null) {
            if (qrToken.childId != childUser.id && qrToken.childId != "child_1" && !currentUser.name.contains(qrToken.childId, ignoreCase = true)) {
                return Result.failure(Exception("This Focus QR token is meant for another child's phone."))
            }
            if (qrToken.status == "USED" || usedQrNonces.contains(qrToken.id)) {
                return Result.failure(Exception("This QR code has already been used."))
            }

            // Mark single-use token as USED
            val idx = qrTokens.indexOf(qrToken)
            if (idx >= 0) {
                qrTokens[idx] = qrToken.copy(status = "USED")
            }
            usedQrNonces.add(qrToken.id)

            val policy = policies.find { it.id == qrToken.policyId } ?: policies.first()
            return Result.success(policy to qrToken.durationMinutes)
        } else {
            if (qrPayload.contains("EXPIRED")) {
                return Result.failure(Exception("This Focus QR token has expired (10 min expiry)."))
            }
            if (usedQrNonces.contains(qrPayload)) {
                return Result.failure(Exception("This QR code has already been used."))
            }
            usedQrNonces.add(qrPayload)
            val policy = policies.first()
            return Result.success(policy to policy.durationMinutes)
        }
    }

    // Verify NFC card
    fun verifyNfcCard(cardId: String, nfcUid: String, token: String): Result<FocusCard> {
        val card = nfcCards.find { it.cardId.equals(cardId, ignoreCase = true) || it.token.equals(token, ignoreCase = true) }
            ?: return Result.failure(Exception("This card isn't paired to you."))

        if (card.status == CardStatus.BLOCKED || card.status == CardStatus.DEACTIVATED) {
            return Result.failure(Exception("This Focus Card has been deactivated by administrator."))
        }

        val (canFocus, message) = canStartFocus(currentUser)
        if (!canFocus) {
            return Result.failure(Exception(message))
        }

        return Result.success(card)
    }

    // Verify QR token
    fun verifyQrToken(token: String): Result<QrToken> {
        val qr = qrTokens.find { it.token == token }
            ?: return Result.failure(Exception("Invalid or expired Focus QR code."))

        if (qr.status != "ACTIVE") {
            return Result.failure(Exception("This Focus QR token is no longer active."))
        }

        val (canFocus, message) = canStartFocus(currentUser)
        if (!canFocus) {
            return Result.failure(Exception(message))
        }

        return Result.success(qr)
    }

    // Register an NFC card to current child
    fun registerNfcCard(cardId: String, nfcUid: String, token: String): Result<FocusCard> {
        val existingIndex = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        val updatedCard = if (existingIndex >= 0) {
            val existing = nfcCards[existingIndex]
            existing.copy(
                ownerId = currentUser.id,
                ownerName = currentUser.name,
                status = CardStatus.ASSIGNED,
                assignedAt = "Today"
            ).also { nfcCards[existingIndex] = it }
        } else {
            FocusCard(
                id = "card_${currentTimeMillis()}",
                cardId = cardId.uppercase(),
                nfcUid = nfcUid,
                token = token,
                ownerId = currentUser.id,
                ownerName = currentUser.name,
                status = CardStatus.ASSIGNED,
                assignedAt = "Today",
                ndefPayload = "FOCUSCARD:$token"
            ).also { nfcCards.add(it) }
        }
        return Result.success(updatedCard)
    }

    // Program & Assign Card (Controller/Admin tool)
    fun programAndAssignNfcCard(cardId: String, targetChildId: String?): FocusCard {
        val child = childrenList.find { it.id == targetChildId }
        val newCard = FocusCard(
            id = "card_${currentTimeMillis()}",
            cardId = cardId.uppercase(),
            nfcUid = "04:B2:77:8A:1C:4F",
            token = "TK_${cardId.uppercase()}",
            ownerId = child?.userId ?: targetChildId,
            ownerName = child?.name ?: "Child",
            status = if (child != null) CardStatus.ASSIGNED else CardStatus.PROGRAMMED,
            createdAt = "Today",
            assignedAt = if (child != null) "Today" else null,
            ndefPayload = "FOCUSCARD:TK_${cardId.uppercase()}"
        )
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            nfcCards[idx] = newCard
        } else {
            nfcCards.add(0, newCard)
        }
        if (child != null) {
            val cIdx = childrenList.indexOfFirst { it.id == child.id }
            if (cIdx >= 0) {
                childrenList[cIdx] = childrenList[cIdx].copy(pairedCardId = cardId.uppercase())
            }
        }
        return newCard
    }

    // Program Card (Admin tool)
    fun programCard(cardId: String, nfcUid: String, token: String): FocusCard {
        val newCard = FocusCard(
            id = "card_${currentTimeMillis()}",
            cardId = cardId.uppercase(),
            nfcUid = nfcUid,
            token = token,
            ownerId = null,
            ownerName = null,
            status = CardStatus.PROGRAMMED,
            createdAt = "Today",
            assignedAt = null,
            ndefPayload = "FOCUSCARD:$token"
        )
        nfcCards.add(0, newCard)
        return newCard
    }

    // Create Focus Policy (Controller)
    fun addPolicy(policy: FocusPolicy) {
        policies.add(0, policy)
    }

    // Update Focus Policy (Controller)
    fun updatePolicy(updated: FocusPolicy) {
        val index = policies.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            policies[index] = updated
        }
    }

    // Delete Focus Policy (Controller)
    fun deletePolicy(policyId: String) {
        policies.removeAll { it.id == policyId }
    }

    // Generate Focus QR (Controller)
    fun generateFocusQr(childId: String, policyId: String, policyTitle: String, durationMinutes: Int): QrToken {
        val secureToken = "FOCAL_QR_${(100000..999999).random()}_${currentTimeMillis().toString().takeLast(4)}"
        val qr = QrToken(
            id = "qrt_${currentTimeMillis()}",
            token = secureToken,
            ownerId = currentUser.id,
            childId = childId,
            policyId = policyId,
            policyTitle = policyTitle,
            durationMinutes = durationMinutes,
            status = "ACTIVE",
            createdAt = "Just now",
            expiresAt = "In 24 hours",
            scanCount = 0
        )
        qrTokens.add(0, qr)
        return qr
    }

    // Emergency Exit Request (Child -> Controller)
    fun submitEmergencyExitRequest(sessionId: String, reason: String): EmergencyExitRequest {
        val req = EmergencyExitRequest(
            id = "req_${currentTimeMillis()}",
            sessionId = sessionId,
            childId = currentUser.id,
            childName = currentUser.name,
            reason = reason,
            requestedAt = "Just now",
            requestedAtMillis = currentTimeMillis(),
            status = "PENDING"
        )
        emergencyRequests.add(0, req)
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "EMERGENCY_REQUEST",
            title = "Emergency Exit Request",
            message = "${currentUser.name} requested early exit: '${reason.ifEmpty { "No reason provided" }}'",
            timestamp = "Just now"
        ))
        return req
    }

    // Approve / Reject Emergency Exit (Controller)
    fun resolveEmergencyRequest(requestId: String, approve: Boolean) {
        val index = emergencyRequests.indexOfFirst { it.id == requestId }
        if (index >= 0) {
            val req = emergencyRequests[index]
            emergencyRequests[index] = req.copy(status = if (approve) "APPROVED" else "REJECTED")

            if (approve) {
                com.focal.app.engine.FocusEngine.instance.endFocusWithEmergency()
            }

            notifications.add(0, NotificationItem(
                id = "notif_${currentTimeMillis()}",
                userId = req.childId,
                type = if (approve) "EMERGENCY_APPROVED" else "EMERGENCY_REJECTED",
                title = if (approve) "Exit Approved" else "Exit Declined",
                message = if (approve) "Parent Priya approved your emergency exit." else "Declined — keep going!",
                timestamp = "Just now"
            ))
        }
    }

    fun checkEmergencyRequestExpirations() {
        val now = currentTimeMillis()
        val FIFTEEN_MINS_MS = 15 * 60 * 1000L
        emergencyRequests.forEachIndexed { idx, req ->
            if (req.status == "PENDING" && req.requestedAtMillis > 0 && (now - req.requestedAtMillis) > FIFTEEN_MINS_MS) {
                emergencyRequests[idx] = req.copy(status = "EXPIRED")
                notifications.add(0, NotificationItem(
                    id = "notif_${now}_exp",
                    userId = req.childId,
                    type = "EMERGENCY_EXPIRED",
                    title = "Request Expired",
                    message = "Emergency request expired after 15 min. You can send a new request if needed.",
                    timestamp = "Just now"
                ))
            }
        }
    }

    // Process Payment & Subscription
    fun processSubscriptionPayment(planType: PlanType, amountInr: Int) {
        currentUser = currentUser.copy(
            planType = planType,
            subscriptionStatus = SubscriptionStatus.TRIAL,
            trialDaysRemaining = 90,
            nfcCardPaid = (planType == PlanType.NFC)
        )
        if (planType == PlanType.NFC) {
            nfcCustomersCount++
            nfcCardSalesInr += 999
        } else {
            qrCustomersCount++
        }
        totalUsersCount++
        activeTrialsCount++

        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = currentUser.id,
            type = "PAYMENT_SUCCESS",
            title = if (planType == PlanType.NFC) "NFC Card Order Confirmed" else "QR Focus Activated",
            message = if (planType == PlanType.NFC) "Paid ₹999. 3 Months Free Trial started." else "Free account activated. 3 Months Free Trial started.",
            timestamp = "Just now"
        ))
    }

    fun renewSubscriptionMonthly() {
        currentUser = currentUser.copy(
            subscriptionStatus = SubscriptionStatus.ACTIVE,
            trialDaysRemaining = 0
        )
        activeSubscriptionsCount++
        if (activeTrialsCount > 0) activeTrialsCount--
        monthlyRevenueInr += 299

        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = currentUser.id,
            type = "SUBSCRIPTION_ACTIVE",
            title = "Monthly Subscription Active",
            message = "Successfully renewed for ₹299/month. Full access unlocked.",
            timestamp = "Just now"
        ))
    }

    // Toggle schedule
    fun toggleSchedule(scheduleId: String) {
        val index = schedules.indexOfFirst { it.id == scheduleId }
        if (index >= 0) {
            val item = schedules[index]
            schedules[index] = item.copy(isEnabled = !item.isEnabled)
        }
    }

    // Add Child
    fun addChild(name: String, age: Int, emailOrPhone: String): Pair<ChildProfile, String> {
        val newChild = ChildProfile(
            id = "child_${currentTimeMillis()}",
            userId = "user_${name.lowercase().replace(" ", "")}",
            controllerId = currentUser.id,
            name = name,
            age = age,
            isOnline = true,
            isFocusing = false,
            todayFocusMinutes = 0,
            screenTimeMinutes = 60,
            focusScore = 80,
            timeSavedMinutes = 0,
            streakDays = 1,
            pairedCardId = null,
            deviceName = "$name's Phone"
        )
        childrenList.add(newChild)
        val pairingToken = "PAIR_${(1000..9999).random()}_$name"
        return newChild to pairingToken
    }

    // Protection Telemetry & Server Heartbeat Logic
    var isProtectionDisabledOnChild by mutableStateOf(false)
    var protectionDisabledReason by mutableStateOf("")
    var lastHeartbeatTimeMs by mutableStateOf(currentTimeMillis())
    var lastHeartbeatBatteryLevel by mutableStateOf(85)
    var lastOfflineAlertSentTimeMs by mutableStateOf(0L)
    var isDeviceOfflineAlertActive by mutableStateOf(false)

    fun reportProtectionDisabled(reason: String) {
        isProtectionDisabledOnChild = true
        protectionDisabledReason = reason

        val existingNotif = notifications.find { it.type == "PROTECTION_OFF" }
        if (existingNotif == null) {
            notifications.add(0, NotificationItem(
                id = "notif_${currentTimeMillis()}",
                userId = controllerUser.id,
                type = "PROTECTION_OFF",
                title = "Focus Protection Disabled",
                message = "Rahul's phone disabled protection: $reason",
                timestamp = "Just now"
            ))
        }
    }

    fun reportProtectionRestored() {
        isProtectionDisabledOnChild = false
        protectionDisabledReason = ""

        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "PROTECTION_RESTORED",
            title = "Focus Protection Restored",
            message = "Rahul's phone restored all required permissions.",
            timestamp = "Just now"
        ))
    }

    fun processChildHeartbeat(childId: String = "child_1", permissionsMap: Map<String, Boolean> = emptyMap(), battery: Int = 85) {
        val now = currentTimeMillis()
        lastHeartbeatTimeMs = now
        lastHeartbeatBatteryLevel = battery

        val childIdx = childrenList.indexOfFirst { it.id == childId }
        if (childIdx >= 0) {
            childrenList[childIdx] = childrenList[childIdx].copy(isOnline = true)
        }

        if (isDeviceOfflineAlertActive) {
            isDeviceOfflineAlertActive = false
            notifications.add(0, NotificationItem(
                id = "notif_$now",
                userId = controllerUser.id,
                type = "DEVICE_ONLINE",
                title = "Device Back Online",
                message = "Heartbeats resumed from Rahul's phone.",
                timestamp = "Just now"
            ))
        }

        val isAnyOff = permissionsMap.values.any { !it }
        if (isAnyOff) {
            reportProtectionDisabled("Permissions missing in heartbeat signal")
        } else if (isProtectionDisabledOnChild && permissionsMap.isNotEmpty() && !isAnyOff) {
            reportProtectionRestored()
        }
    }

    fun checkServerHeartbeatTimeout(childId: String = "child_1") {
        val now = currentTimeMillis()
        val timeSinceHeartbeatMs = now - lastHeartbeatTimeMs
        val THIRTY_MINUTES_MS = 30 * 60 * 1000L
        val ONE_HOUR_MS = 60 * 60 * 1000L

        if (timeSinceHeartbeatMs > THIRTY_MINUTES_MS) {
            val childIdx = childrenList.indexOfFirst { it.id == childId }
            if (childIdx >= 0) {
                childrenList[childIdx] = childrenList[childIdx].copy(isOnline = false)
            }

            if (!isDeviceOfflineAlertActive || (now - lastOfflineAlertSentTimeMs) > ONE_HOUR_MS) {
                isDeviceOfflineAlertActive = true
                lastOfflineAlertSentTimeMs = now
                notifications.add(0, NotificationItem(
                    id = "notif_$now",
                    userId = controllerUser.id,
                    type = "DEVICE_OFFLINE",
                    title = "Device Offline",
                    message = "No signal from Rahul's phone for 30 min",
                    timestamp = "Just now"
                ))
            }
        }
    }

    // Admin Console Data & Audit Logs
    val parentAccounts = mutableStateListOf(
        ParentAccount("user_priya", "Priya Sharma", "priya@focal.io", listOf("child_1"), ParentSubscriptionStatus.TRIAL_3_MONTHS, 84, "2026-08-01")
    )

    val children get() = childrenList
    val cards get() = nfcCards

    val auditLogs = mutableStateListOf(
        AdminAuditLog("log_1", "admin@focal.io", "Initialized Demo", "Priya Sharma & Ananya", "Just now")
    )

    val adminDevices = mutableStateListOf(
        AdminDeviceItem("dev_1", "Ananya", "Priya Sharma", "Android Phone", "Android 14", "1.2.0-demo", true, true, "1m ago", true, true, true, true)
    )

    fun logAdminAction(action: String, target: String) {
        auditLogs.add(0, AdminAuditLog(
            id = "log_${currentTimeMillis()}",
            adminId = adminUser.email,
            action = action,
            target = target,
            timestamp = "Just now"
        ))
    }

    fun adminExtendTrial(userId: String, days: Int = 14) {
        val parentIdx = parentAccounts.indexOfFirst { it.id == userId }
        if (parentIdx != -1) {
            val parent = parentAccounts[parentIdx]
            parentAccounts[parentIdx] = parent.copy(trialDaysLeft = parent.trialDaysLeft + days)
            logAdminAction("Extended trial (+$days days)", "${parent.name} (${parent.email})")
        } else {
            val targetUser = if (userId == controllerUser.id) controllerUser else null
            if (targetUser != null) {
                controllerUser = controllerUser.copy(trialDaysRemaining = controllerUser.trialDaysRemaining + days)
                logAdminAction("Extended trial (+$days days)", "${targetUser.name} (${targetUser.email})")
            } else {
                logAdminAction("Extended trial (+$days days)", "User ID: $userId")
            }
        }
    }

    fun adminResetChildPairing(childId: String) {
        val child = childrenList.find { it.id == childId }
        if (child != null) {
            val idx = childrenList.indexOf(child)
            childrenList[idx] = child.copy(isOnline = false, isFocusing = false, pairedCardId = null)
            logAdminAction("Reset child device pairing", "${child.name}'s device")
        }
    }

    fun adminRevokeCard(cardId: String) {
        val card = nfcCards.find { it.cardId.equals(cardId, ignoreCase = true) }
        if (card != null) {
            val idx = nfcCards.indexOf(card)
            nfcCards[idx] = card.copy(status = CardStatus.DEACTIVATED, ownerId = null, ownerName = null)
            logAdminAction("Revoked card", "Card $cardId")
        }
    }

    fun adminSuspendAccount(userId: String) {
        val parentIdx = parentAccounts.indexOfFirst { it.id == userId }
        if (parentIdx != -1) {
            val parent = parentAccounts[parentIdx]
            parentAccounts[parentIdx] = parent.copy(subscriptionStatus = ParentSubscriptionStatus.SUSPENDED)
            logAdminAction("Suspended account", "${parent.name} (${parent.email})")
        } else {
            logAdminAction("Suspended account", "User ID: $userId")
        }
    }

    fun adminProgramBatchCards(count: Int) {
        val startNum = nfcCards.size + 4
        val batchIds = mutableListOf<String>()
        for (i in 0 until count) {
            val cId = "FC000${startNum + i}"
            batchIds.add(cId)
            nfcCards.add(0, FocusCard(
                id = "card_${currentTimeMillis()}_$i",
                cardId = cId,
                nfcUid = "04:FF:88:${(10..99).random()}:${(10..99).random()}:${(10..99).random()}",
                token = "TK_${(1000..9999).random()}",
                ownerId = null,
                ownerName = null,
                status = CardStatus.PROGRAMMED,
                createdAt = "Today",
                assignedAt = null,
                ndefPayload = "FOCUSCARD:TK_${(1000..9999).random()}"
            ))
        }
        logAdminAction("Programmed batch ($count cards)", "${batchIds.first()}–${batchIds.last()}")
    }
}
