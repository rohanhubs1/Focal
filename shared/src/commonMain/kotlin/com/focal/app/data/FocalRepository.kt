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

    var parentPin by mutableStateOf("1234")

    fun updateParentName(newName: String) {
        controllerUser = controllerUser.copy(name = newName)
        if (currentUser.id == controllerUser.id) {
            currentUser = currentUser.copy(name = newName)
        }
    }

    fun updateParentEmail(newEmail: String) {
        controllerUser = controllerUser.copy(email = newEmail)
        if (currentUser.id == controllerUser.id) {
            currentUser = currentUser.copy(email = newEmail)
        }
    }

    fun updateParentPin(newPin: String) {
        parentPin = newPin
        com.focal.app.protection.ProtectionStateRepository.instance.setParentPin(newPin)
    }

    fun renameCard(cardId: String, newLabel: String) {
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            nfcCards[idx] = nfcCards[idx].copy(label = newLabel)
        }
    }

    fun removeCard(cardId: String) {
        nfcCards.removeAll { it.cardId.equals(cardId, ignoreCase = true) }
    }

    fun deleteParentAccount() {
        childrenList.clear()
        nfcCards.clear()
        policies.clear()
        schedules.clear()
        notifications.clear()
        parentPin = ""
        com.focal.app.pairing.PairingRepository.instance.unlink()
        com.focal.app.protection.ProtectionStateRepository.instance.reset()
    }

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
            userId = "user_rahul",
            controllerId = "user_priya",
            name = "Aarav",
            familyId = "family_1",
            defaultPolicyId = "pol_study",
            defaultFocusMinutes = 90,
            age = 15,
            grade = "Class 10",
            isOnline = true,
            isFocusing = true,
            todayFocusMinutes = 135,
            screenTimeMinutes = 192,
            focusScore = 88,
            timeSavedMinutes = 75,
            streakDays = 3,
            currentSessionId = "sess_active_1",
            remainingMinutes = 42,
            pairedCardId = "FC0001",
            deviceName = "Realme · Android 14"
        ),
        ChildProfile(
            id = "child_2",
            userId = "user_kiara",
            controllerId = "user_priya",
            name = "Kiara",
            familyId = "family_1",
            defaultPolicyId = "pol_study",
            defaultFocusMinutes = 90,
            age = 11,
            grade = "Class 8",
            isOnline = true,
            isFocusing = false,
            todayFocusMinutes = 110, // 1h 50m
            screenTimeMinutes = 120,
            focusScore = 92,
            timeSavedMinutes = 45,
            streakDays = 5,
            currentSessionId = null,
            remainingMinutes = 0,
            pairedCardId = "FC0001",
            deviceName = "Kiara's Galaxy Tab"
        )
    )

    // Installed Apps uploaded from Child Device
    val childInstalledApps = mutableStateListOf(
        InstalledApp("com.instagram.android", "Instagram", "Social", "hash1"),
        InstalledApp("com.google.android.youtube", "YouTube", "Video", "hash4"),
        InstalledApp("com.zhiliaoapp.musically", "TikTok", "Video", "hash2"),
        InstalledApp("com.amazon.mShop.android.shopping", "Amazon", "Shopping", "hash8"),
        InstalledApp("com.pubg.imobile", "BGMI", "Games", "hash_bgmi"),
        InstalledApp("com.android.chrome", "Chrome", "Browsers", "hash_chrome"),
        InstalledApp("com.whatsapp", "WhatsApp", "Social", "hash11"),
        InstalledApp("com.snapchat.android", "Snapchat", "Social", "hash3"),
        InstalledApp("com.netflix.mediaclient", "Netflix", "Video", "hash5"),
        InstalledApp("com.roblox.client", "Roblox", "Games", "hash7"),
        InstalledApp("com.supercell.clashofclans", "Clash of Clans", "Games", "hash6"),
        InstalledApp("com.flipkart.android", "Flipkart", "Shopping", "hash9"),
        InstalledApp("org.telegram.messenger", "Telegram", "Social", "hash10")
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
        val queryIdx = clean.indexOf('?')
        if (queryIdx != -1) clean = clean.substring(0, queryIdx)
        val hashIdx = clean.indexOf('#')
        if (hashIdx != -1) clean = clean.substring(0, hashIdx)
        return clean
    }

    fun isValidDomainFormat(domain: String): Boolean {
        val clean = normalizeDomain(domain)
        val domainRegex = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$")
        return domainRegex.matches(clean)
    }

    fun isAppBlockable(app: InstalledApp): Boolean {
        val pkg = app.packageName.lowercase()
        val name = app.appName.lowercase()

        // 1. Phone / Dialer
        if (pkg.contains("dialer") || pkg.contains("telecom") || name == "phone" || name == "telephone") return false

        // 2. Settings & System Settings
        if (pkg.contains("settings") || name == "settings") return false

        // 3. FOCAL itself
        if (pkg.contains("com.focal.app") || name == "focal") return false

        // 4. Emergency & System UI
        if (pkg.contains("emergency") || pkg.contains("systemui") || pkg.contains("com.android.systemui")) return false

        // 5. Default Launchers / Homescreen
        if (pkg.contains("launcher") || name.contains("launcher") || pkg.contains("trebuchet") || pkg.contains("pixel launcher")) return false

        return true
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
            blockedApps = listOf("Instagram", "YouTube", "TikTok", "BGMI", "Snapchat", "Netflix", "Roblox", "Clash of Clans"),
            blockedWebsites = listOf("instagram.com", "youtube.com", "tiktok.com", "snapchat.com", "netflix.com", "facebook.com", "reddit.com", "twitch.tv", "amazon.com", "flipkart.com"),
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

    // Registered NFC Cards (ONE family card for the entire family by default)
    val nfcCards = mutableStateListOf(
        FocusCard(
            id = "card_family_1",
            cardId = "FC0001",
            token = "FC0001_TOKEN",
            familyId = "family_1",
            ownerParentId = "user_priya",
            ownerId = "user_priya",
            ownerName = "Priya Sharma",
            assignedChildId = null,
            status = CardStatus.ACTIVE,
            isEnabled = true,
            registeredAt = "12 Sep 2026",
            ndefPayload = "FOCUSCARD:FC0001",
            lastUsed = "Today, 10:02 AM"
        )
    )

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
            childName = "Aarav",
            reason = "Need to call someone",
            requestedAt = "2 min ago",
            status = "PENDING"
        )
    )

    // Schedules
    val schedules = mutableStateListOf(
        ScheduleItem(
            id = "sch_2",
            title = "Deep Work",
            timeRange = "10:00 AM – 12:00 PM",
            days = "Mon – Fri",
            mode = FocusMode.DEEP_WORK,
            isEnabled = true,
            appsCount = 12,
            websitesCount = 20,
            policyName = "Study Mode",
            allowCardExit = false
        ),
        ScheduleItem(
            id = "sch_3",
            title = "Homework",
            timeRange = "4:00 – 6:00 PM",
            days = "Every day",
            mode = FocusMode.WORK,
            isEnabled = true,
            appsCount = 6,
            websitesCount = 10,
            policyName = "Study Mode",
            allowCardExit = false
        ),
        ScheduleItem(
            id = "sch_5",
            title = "Night Guard",
            timeRange = "10:00 PM – 6:00 AM",
            days = "Every night",
            mode = FocusMode.SLEEP,
            isEnabled = false,
            appsCount = 25,
            websitesCount = 50,
            policyName = "Wind Down",
            allowCardExit = false
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
        val planPrice = com.focal.app.config.FocalPricingConfig.PLAN_PRICE_PER_MONTH
        return when (user.subscriptionStatus) {
            SubscriptionStatus.TRIAL -> {
                if (user.trialDaysRemaining > 0) {
                    true to "Trial valid (${user.trialDaysRemaining} days remaining)"
                } else {
                    false to "Your free trial has expired. Subscribe for $planPrice to continue."
                }
            }
            SubscriptionStatus.ACTIVE -> true to "Subscription active"
            SubscriptionStatus.EXPIRED -> false to "Subscription expired. Please renew for $planPrice."
            SubscriptionStatus.PAST_DUE -> false to "Payment past due. Update billing to continue."
            SubscriptionStatus.CANCELLED -> false to "Subscription cancelled. Reactivate for $planPrice."
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

    // Validate NFC card for starting focus (Child tap - family-wide card)
    fun validateNfcCardForChildStart(cardIdOrToken: String, childUser: User): Result<FocusCard> {
        val childProfile = childrenList.find { it.userId == childUser.id || it.name.equals(childUser.name, ignoreCase = true) }
        val card = nfcCards.find {
            it.cardId.equals(cardIdOrToken, ignoreCase = true) ||
            it.token.equals(cardIdOrToken, ignoreCase = true) ||
            it.ndefPayload.contains(cardIdOrToken, ignoreCase = true)
        } ?: return Result.failure(Exception("This isn't your family's FOCAL card."))

        val cardFamily = card.familyId.ifBlank { "family_1" }
        val userFamily = childUser.familyId.ifBlank { childProfile?.familyId ?: "family_1" }
        if (cardFamily != userFamily) {
            return Result.failure(Exception("This isn't your family's FOCAL card."))
        }

        if (!card.isEnabled || card.status == CardStatus.DISABLED || card.status == CardStatus.REVOKED) {
            return Result.failure(Exception("This card has been disabled by your parent."))
        }

        val (canFocus, message) = canStartFocus(childUser)
        if (!canFocus) {
            return Result.failure(Exception(message))
        }

        return Result.success(card)
    }

    // Validate NFC card for ending focus early (family-wide card)
    fun validateNfcCardForChildEnd(cardIdOrToken: String, childUser: User, currentSession: FocusSession?): Result<FocusCard> {
        val card = nfcCards.find {
            it.cardId.equals(cardIdOrToken, ignoreCase = true) ||
            it.token.equals(cardIdOrToken, ignoreCase = true) ||
            it.ndefPayload.contains(cardIdOrToken, ignoreCase = true)
        } ?: return Result.failure(Exception("This isn't your family's FOCAL card."))

        val childProfile = childrenList.find { it.userId == childUser.id || it.name.equals(childUser.name, ignoreCase = true) }
        val cardFamily = card.familyId.ifBlank { "family_1" }
        val userFamily = childUser.familyId.ifBlank { childProfile?.familyId ?: "family_1" }
        if (cardFamily != userFamily) {
            return Result.failure(Exception("This isn't your family's FOCAL card."))
        }

        if (!card.isEnabled || card.status == CardStatus.DISABLED || card.status == CardStatus.REVOKED) {
            return Result.failure(Exception("This card has been disabled by your parent."))
        }

        return Result.success(card)
    }

    // Disable a family card immediately across all child devices
    fun disableCard(cardId: String) {
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            nfcCards[idx] = nfcCards[idx].copy(isEnabled = false, status = CardStatus.DISABLED)
        }
    }

    // Enable a family card across all child devices
    fun enableCard(cardId: String) {
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            nfcCards[idx] = nfcCards[idx].copy(isEnabled = true, status = CardStatus.ACTIVE)
        }
    }

    // Validate QR token for starting focus (Family QR or single-use token)
    fun validateQrTokenForChildStart(qrPayload: String, childUser: User): Result<Pair<FocusPolicy, Int>> {
        val trimmed = qrPayload.trim()
        if (trimmed.startsWith("focal://pair", ignoreCase = true)) {
            return Result.failure(Exception("That's not a FOCAL focus QR"))
        }

        val childProfile = childrenList.find { it.userId == childUser.id || it.name.equals(childUser.name, ignoreCase = true) }
        val userFamily = childUser.familyId.ifBlank { childProfile?.familyId ?: "family_1" }

        // 1. Family Focus QR check
        if (FocusQrRepository.instance.validateFamilyQr(trimmed, userFamily)) {
            val (canFocus, message) = canStartFocus(childUser)
            if (!canFocus) {
                return Result.failure(Exception(message))
            }
            val policyId = childProfile?.defaultPolicyId ?: "pol_study"
            val policy = policies.find { it.id == policyId } ?: policies.first()
            val durationMinutes = childProfile?.defaultFocusMinutes ?: 90
            return Result.success(policy to durationMinutes)
        }

        // 2. Individual QR Tokens
        val qrToken = qrTokens.find { it.token == trimmed }
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
        }

        return Result.failure(Exception("That's not a FOCAL focus QR"))
    }

    // Verify NFC card
    fun verifyNfcCard(cardId: String, nfcUid: String, token: String): Result<FocusCard> {
        val card = nfcCards.find { it.cardId.equals(cardId, ignoreCase = true) || it.token.equals(token, ignoreCase = true) }
            ?: return Result.failure(Exception("This isn't your family's FOCAL card."))

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

    data class PolicyUsage(
        val children: List<ChildProfile>,
        val schedules: List<ScheduleItem>
    ) {
        val isUsed: Boolean get() = children.isNotEmpty() || schedules.isNotEmpty()
    }

    fun getPolicyUsage(policyId: String): PolicyUsage {
        val policy = policies.find { it.id == policyId }
        val pName = policy?.name ?: ""
        val children = childrenList.filter { it.defaultPolicyId == policyId || it.defaultPolicyId.equals(pName, ignoreCase = true) }
        val scheds = schedules.filter { it.policyName.equals(pName, ignoreCase = true) || it.policyName.equals(policyId, ignoreCase = true) }
        return PolicyUsage(children, scheds)
    }

    fun deletePolicyWithReplacement(policyId: String, replacementPolicyId: String) {
        val oldPolicy = policies.find { it.id == policyId }
        val newPolicy = policies.find { it.id == replacementPolicyId }
        if (oldPolicy != null && newPolicy != null) {
            childrenList.forEachIndexed { index, child ->
                if (child.defaultPolicyId == policyId || child.defaultPolicyId.equals(oldPolicy.name, ignoreCase = true)) {
                    childrenList[index] = child.copy(defaultPolicyId = newPolicy.id)
                }
            }
            schedules.forEachIndexed { index, sch ->
                if (sch.policyName.equals(oldPolicy.name, ignoreCase = true) || sch.policyName.equals(policyId, ignoreCase = true)) {
                    schedules[index] = sch.copy(policyName = newPolicy.name)
                }
            }
        }
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
        ActivityRepository.instance.recordEvent(
            type = ActivityEventType.EXIT_REQUESTED,
            childId = currentUser.id,
            details = "${currentUser.name} requested early exit: '${reason.ifEmpty { "No reason provided" }}'",
            metadata = mapOf("sessionId" to sessionId, "reason" to reason)
        )
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

            val eventType = if (approve) ActivityEventType.EXIT_APPROVED else ActivityEventType.EXIT_DECLINED
            val eventDetails = if (approve) "Parent Priya approved emergency exit for ${req.childName}" else "Parent Priya declined emergency exit for ${req.childName}"
            ActivityRepository.instance.recordEvent(
                type = eventType,
                childId = req.childId,
                details = eventDetails,
                metadata = mapOf("requestId" to requestId)
            )

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

    // Save schedule (create or update)
    fun saveSchedule(schedule: ScheduleItem) {
        val index = schedules.indexOfFirst { it.id == schedule.id }
        if (index >= 0) {
            schedules[index] = schedule
        } else {
            schedules.add(0, schedule)
        }
    }

    // Delete schedule
    fun deleteSchedule(scheduleId: String) {
        schedules.removeAll { it.id == scheduleId }
    }

    // Update child profile
    fun updateChild(updated: ChildProfile) {
        val index = childrenList.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            childrenList[index] = updated
        }
    }

    // Remove child profile
    fun removeChild(childId: String) {
        childrenList.removeAll { it.id == childId }
    }

    // Unlink child device
    fun unlinkChildDevice(childId: String) {
        val index = childrenList.indexOfFirst { it.id == childId }
        if (index >= 0) {
            childrenList[index] = childrenList[index].copy(
                deviceName = "",
                pairedCardId = null,
                isOnline = false,
                isFocusing = false,
                remainingMinutes = 0
            )
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

    // Protection Telemetry, Settings Guard & Server Heartbeat Logic
    var isProtectionDisabledOnChild by mutableStateOf(false)
    var protectionDisabledReason by mutableStateOf("")
    var lastHeartbeatTimeMs by mutableStateOf(currentTimeMillis())
    var lastHeartbeatBatteryLevel by mutableStateOf(85)
    var lastOfflineAlertSentTimeMs by mutableStateOf(0L)
    var isDeviceOfflineAlertActive by mutableStateOf(false)

    // Protection Guard Mode: "ALWAYS" (default for child) or "DURING_FOCUS_ONLY"
    var childGuardMode by mutableStateOf("ALWAYS")

    // Parent Unlock Timer (Pauses settings guard for 10 minutes)
    var parentUnlockedUntilMs by mutableStateOf(0L)
    var showParentUnlockScreen by mutableStateOf(false)

    fun isParentGuardUnlocked(): Boolean {
        return currentTimeMillis() < parentUnlockedUntilMs
    }

    fun unlockParentGuardFor10Minutes(): String {
        val tenMinsMs = 10 * 60 * 1000L
        parentUnlockedUntilMs = currentTimeMillis() + tenMinsMs
        com.focal.app.protection.ProtectionStateRepository.instance.unlockParentForDuration(
            durationMs = tenMinsMs,
            currentElapsed = com.focal.app.model.currentTimeMillis()
        )
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "PARENT_UNLOCK",
            title = "Parent Unlock Activated",
            message = "Protection guard paused on child device for 10 minutes.",
            timestamp = "Just now"
        ))
        return "Parent unlock active for 10 minutes. Settings access permitted."
    }

    fun reportProtectionDisabled(reason: String, childId: String = "child_1") {
        isProtectionDisabledOnChild = true
        protectionDisabledReason = reason
        val idx = childrenList.indexOfFirst { it.id == childId }
        if (idx >= 0) {
            childrenList[idx] = childrenList[idx].copy(isProtectionOff = true)
        }

        val existingNotif = notifications.find { it.type == "PROTECTION_OFF" }
        if (existingNotif == null) {
            notifications.add(0, NotificationItem(
                id = "notif_${currentTimeMillis()}",
                userId = controllerUser.id,
                type = "PROTECTION_OFF",
                title = "Focus Protection Disabled",
                message = "Phone disabled protection: $reason",
                timestamp = "Just now"
            ))
            com.focal.app.data.ActivityRepository.instance.recordEvent(
                type = com.focal.app.data.ActivityEventType.PROTECTION_OFF,
                childId = childId,
                details = reason,
                metadata = mapOf("permission" to "Accessibility")
            )
        }
    }

    fun reportProtectionRestored(childId: String = "child_1") {
        isProtectionDisabledOnChild = false
        protectionDisabledReason = ""
        val idx = childrenList.indexOfFirst { it.id == childId }
        if (idx >= 0) {
            childrenList[idx] = childrenList[idx].copy(isProtectionOff = false)
        }

        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "PROTECTION_RESTORED",
            title = "Focus Protection Restored",
            message = "Phone restored all required permissions.",
            timestamp = "Just now"
        ))
        com.focal.app.data.ActivityRepository.instance.recordEvent(
            type = com.focal.app.data.ActivityEventType.PROTECTION_ON,
            childId = childId,
            details = "All permissions on"
        )
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
                message = "Heartbeats resumed from phone.",
                timestamp = "Just now"
            ))
        }

        val isAnyOff = permissionsMap.values.any { !it }
        if (isAnyOff) {
            reportProtectionDisabled("Accessibility was turned off", childId)
        } else if (isProtectionDisabledOnChild && permissionsMap.isNotEmpty() && !isAnyOff) {
            reportProtectionRestored(childId)
        }
    }

    fun reportGuardTriggered(screenName: String) {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "GUARD_TRIGGERED",
            title = "Settings Guard Intercepted",
            message = "Blocked attempt to access: $screenName",
            timestamp = "Just now"
        ))
    }

    fun reportUninstallAttempt() {
        reportProtectionDisabled("Child attempted to uninstall FOCAL")
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "UNINSTALL_ATTEMPT",
            title = "UNINSTALL ATTEMPT DETECTED",
            message = "Child attempted to uninstall the FOCAL app package.",
            timestamp = "Just now"
        ))
    }

    fun reportForceStopAttempt() {
        reportProtectionDisabled("Child attempted to force-stop FOCAL")
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "FORCE_STOP_ATTEMPT",
            title = "FORCE STOP ATTEMPT DETECTED",
            message = "Child attempted to force-stop FOCAL background service.",
            timestamp = "Just now"
        ))
    }

    fun reportDeviceAdminDisableAttempt() {
        reportProtectionDisabled("Child requested to deactivate Device Admin")
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "DEVICE_ADMIN_DISABLE_ATTEMPT",
            title = "DEVICE ADMIN DEACTIVATION ATTEMPT",
            message = "Child opened Device Admin settings to remove FOCAL protection.",
            timestamp = "Just now"
        ))
    }

    fun reportDeviceRebootedDuringFocus() {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "DEVICE_REBOOTED_DURING_FOCUS",
            title = "Device Rebooted During Focus",
            message = "Child's phone restarted while a focus session was active. Protection auto-restored.",
            timestamp = "Just now"
        ))
    }

    fun reportUsbDebuggingOn() {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "USB_DEBUGGING_ON",
            title = "USB Debugging / ADB Enabled",
            message = "USB Debugging or Developer Options turned on on child device.",
            timestamp = "Just now"
        ))
    }

    fun reportFocusEndedWithoutCard() {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "FOCUS_ENDED_WITHOUT_CARD",
            title = "Focus Ended Without Card",
            message = "Session ended without card tap or parent approval.",
            timestamp = "Just now"
        ))
    }

    fun reportClockChanged() {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "CLOCK_CHANGED",
            title = "System Time Tampering Detected",
            message = "System clock was changed on child device. Session duration calculated using monotonic hardware clock.",
            timestamp = "Just now"
        ))
    }

    fun reportExtraProfileDetected() {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "EXTRA_PROFILE_DETECTED",
            title = "Second Space / Guest Profile Detected",
            message = "A second user space or guest profile was opened or configured.",
            timestamp = "Just now"
        ))
    }

    // Solo Mode Strict Configuration
    var isSoloStrictModeEnabled by mutableStateOf(false)

    fun toggleSoloStrictMode(cardVerified: Boolean): Pair<Boolean, String> {
        return if (!isSoloStrictModeEnabled) {
            isSoloStrictModeEnabled = true
            true to "Solo Strict Mode enabled. Card tap required to turn off."
        } else {
            if (cardVerified) {
                isSoloStrictModeEnabled = false
                true to "Solo Strict Mode disabled."
            } else {
                false to "Tap your paired FOCAL card to turn off Solo Strict Mode."
            }
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
