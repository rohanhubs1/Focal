package com.focal.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.focal.app.model.*

open class LocalFocalRepository : FocalRepository {

    companion object {
        val instance: LocalFocalRepository by lazy { LocalFocalRepository() }
    }

    // Current logged-in user
    override var currentUser by mutableStateOf(
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
    override val adminUser = User(
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

    override var controllerUser by mutableStateOf(
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

    override var parentPin by mutableStateOf("1234")
    override var onUnlinkedFromParent: (() -> Unit)? = null

    override fun updateParentName(newName: String) {
        controllerUser = controllerUser.copy(name = newName)
        if (currentUser.id == controllerUser.id) {
            currentUser = currentUser.copy(name = newName)
        }
    }

    override fun updateParentEmail(newEmail: String) {
        controllerUser = controllerUser.copy(email = newEmail)
        if (currentUser.id == controllerUser.id) {
            currentUser = currentUser.copy(email = newEmail)
        }
    }

    override fun updateParentPin(newPin: String) {
        parentPin = newPin
        com.focal.app.protection.ProtectionStateRepository.instance.setParentPin(newPin)
    }

    override fun renameCard(cardId: String, newLabel: String) {
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            nfcCards[idx] = nfcCards[idx].copy(label = newLabel)
        }
    }

    override fun removeCard(cardId: String) {
        nfcCards.removeAll { it.cardId.equals(cardId, ignoreCase = true) }
    }

    override fun deleteParentAccount() {
        childrenList.clear()
        nfcCards.clear()
        policies.clear()
        schedules.clear()
        notifications.clear()
        parentPin = ""
        com.focal.app.pairing.PairingRepository.instance.unlink()
        com.focal.app.protection.ProtectionStateRepository.instance.reset()
    }

    override val childRahul = User(
        id = "user_rahul",
        name = "Aarav",
        email = "aarav@focal.io",
        phone = "+91 98765 43210",
        role = UserRole.CHILD,
        planType = PlanType.NFC,
        subscriptionStatus = SubscriptionStatus.TRIAL,
        trialDaysRemaining = 84,
        nextBillingDate = "2026-12-18",
        nfcCardPaid = true
    )

    override val childAnanya = User(
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
    override val childrenList = mutableStateListOf(
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
            focusEndsAtMillis = currentTimeMillis() + 42 * 60 * 1000L,
            focusTotalMinutes = 90,
            pairedCardId = null,
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
            pairedCardId = null,
            deviceName = "Kiara's Galaxy Tab"
        )
    )

    // Installed Apps uploaded from Child Device
    override val childInstalledApps = mutableStateListOf(
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
    override fun syncChildInstalledApps(apps: List<InstalledApp>) {
        childInstalledApps.clear()
        childInstalledApps.addAll(apps)
    }

    // Domain normalization & subdomain matching logic
    override fun normalizeDomain(urlOrDomain: String): String {
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

    override fun isValidDomainFormat(domain: String): Boolean {
        val clean = normalizeDomain(domain)
        val domainRegex = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$")
        return domainRegex.matches(clean)
    }

    override fun isAppBlockable(app: InstalledApp): Boolean {
        val pkg = app.packageName.lowercase()
        val name = app.appName.lowercase()

        // 1. Phone / Dialer
        if (pkg.contains("dialer") || pkg.contains("telecom") || name == "phone" || name == "telephone") return false

        // 2. Settings & System Settings
        if (pkg.contains("settings") || name == "settings") return false

        // 3. FOCAL itself
        if (pkg.contains("com.focal.app") || pkg.contains("com.rohan.focalapplication") || pkg.contains("myapplication") || name == "focal") return false

        // 4. Emergency & System UI
        if (pkg.contains("emergency") || pkg.contains("systemui") || pkg.contains("com.android.systemui")) return false

        // 5. Default Launchers / Homescreen
        if (pkg.contains("launcher") || name.contains("launcher") || pkg.contains("trebuchet") || pkg.contains("pixel launcher")) return false

        return true
    }

    override fun isDomainBlocked(targetUrlOrDomain: String, blockedDomains: List<String>): Boolean {
        val cleanTarget = normalizeDomain(targetUrlOrDomain)
        if (cleanTarget.isEmpty()) return false
        return blockedDomains.any { rawBlocked ->
            val cleanBlocked = normalizeDomain(rawBlocked)
            cleanBlocked.isNotEmpty() && (cleanTarget == cleanBlocked || cleanTarget.endsWith(".$cleanBlocked"))
        }
    }

    // Focus Policies (2 Seeded Policies for Demo: Study Mode & Wind Down)
    override val policies = mutableStateListOf(
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

    // Registered NFC Cards & Stock Inventory
    override val nfcCards = mutableStateListOf(
        FocusCard(
            id = "card_100001",
            cardId = "Card 100001",
            token = "TK_100001",
            ownerParentId = "user_priya",
            ownerId = "user_priya",
            ownerName = "Priya Sharma",
            assignedChildId = "child_1",
            status = CardStatus.ASSIGNED,
            registeredAt = "12 Sep 2026",
            createdAt = "12 Sep 2026",
            nfcUid = "04:A1:B2:C3:D4:E5:F6",
            ndefPayload = "FOCUSCARD:TK_100001",
            tapCount = 23,
            lastTapTime = "10:02"
        ),
        FocusCard(
            id = "card_100002",
            cardId = "Card 100002",
            token = "TK_100002",
            ownerParentId = null,
            ownerId = null,
            ownerName = null,
            assignedChildId = null,
            status = CardStatus.IN_STOCK,
            registeredAt = "16 Oct 2026",
            createdAt = "16 Oct 2026",
            nfcUid = "04:B2:C3:D4:E5:F6:A1",
            ndefPayload = "FOCUSCARD:TK_100002",
            tapCount = 0,
            lastTapTime = null
        )
    )

    // QR Tokens
    override val qrTokens = mutableStateListOf(
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
    override val emergencyRequests = mutableStateListOf(
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
    override val schedules = mutableStateListOf(
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
    override val friends = mutableStateListOf(
        FriendUser("fr_1", "Aryan Mehta", "AM", 18, 24.5, 94, true),
        FriendUser("fr_2", "Sneha Roy", "SR", 15, 21.0, 89, false),
        FriendUser("fr_3", "Kabir Sen", "KS", 9, 14.0, 78, true),
        FriendUser("fr_4", "Diya Patel", "DP", 22, 31.5, 96, false),
        FriendUser("fr_5", "Rohan Das", "RD", 6, 9.5, 71, false)
    )

    // Notifications
    override val notifications = mutableStateListOf(
        NotificationItem(
            id = "notif_1",
            userId = "user_priya",
            type = "EMERGENCY_REQUEST",
            title = "Emergency Exit Request",
            message = "Aarav requested an early exit: 'Need to submit school science assignment'",
            timestamp = "10m ago"
        ),
        NotificationItem(
            id = "notif_2",
            userId = "user_priya",
            type = "PROTECTION_OFF",
            title = "Focus Protection Disabled",
            message = "Aarav's phone disabled Accessibility Service permission.",
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
            message = "Aarav started Study Mode via NFC card FC0001",
            timestamp = "2h ago"
        ),
        NotificationItem(
            id = "notif_6",
            userId = "user_rahul",
            type = "FOCUS_ENDED",
            title = "Session Completed",
            message = "Aarav finished Study Mode. Focus Score: 92",
            timestamp = "3h ago"
        )
    )

    // Analytics Data
    override val topDistractions = listOf(
        DistractionAppStat("Instagram", 14, 0.42f),
        DistractionAppStat("YouTube Shorts", 9, 0.28f),
        DistractionAppStat("Snapchat", 5, 0.15f),
        DistractionAppStat("Netflix", 3, 0.09f),
        DistractionAppStat("Discord", 2, 0.06f)
    )

    override val dailyFocusHistory = listOf(
        DailyFocusBar("Mon", 4.2f, 2.8f),
        DailyFocusBar("Tue", 5.5f, 2.1f),
        DailyFocusBar("Wed", 3.8f, 3.4f),
        DailyFocusBar("Thu", 6.1f, 1.9f),
        DailyFocusBar("Fri", 5.0f, 2.5f),
        DailyFocusBar("Sat", 4.8f, 3.2f),
        DailyFocusBar("Sun", 5.8f, 2.0f)
    )

    // Admin platform metrics
    override var totalUsersCount by mutableStateOf(1482)
    override var nfcCustomersCount by mutableStateOf(624)
    override var qrCustomersCount by mutableStateOf(858)
    override var activeTrialsCount by mutableStateOf(412)
    override var activeSubscriptionsCount by mutableStateOf(890)
    override var expiredSubscriptionsCount by mutableStateOf(180)
    override var monthlyRevenueInr by mutableStateOf(348000)
    override var nfcCardSalesInr by mutableStateOf(623376)
    override var activeFocusSessionsCount by mutableStateOf(342)
    override var totalFocusHoursCount by mutableStateOf(48290)

    // Switch Role seamlessly
    override fun switchUserRole(role: UserRole) {
        currentUser = when (role) {
            UserRole.ADMIN -> adminUser
            UserRole.CONTROLLER -> controllerUser
            UserRole.CHILD -> childRahul
        }
    }

    override fun switchChild(childUser: User) {
        currentUser = childUser
    }

    // Server-side Subscription Verification
    override fun canStartFocus(user: User): Pair<Boolean, String> {
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
    override fun inspectCardForRegistration(rawPayloadOrToken: String, parentId: String): CardRegistrationInspection {
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
    override fun registerCardToParent(tokenOrCardId: String, parentId: String, parentName: String): FocusCard {
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
    override fun writeAndRegisterBlankCard(parentId: String, parentName: String): FocusCard {
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
        if (childrenList.isNotEmpty() && childrenList[0].pairedCardId == null) {
            childrenList[0] = childrenList[0].copy(pairedCardId = cardId)
        }
        return newCard
    }

    // Assign card to child
    override fun assignCardToChild(cardId: String, childId: String): FocusCard? {
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
    override fun unpairCardFromChild(cardId: String) {
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
    override fun revokeCard(cardId: String) {
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
    override fun validateNfcCardForChildStart(cardIdOrToken: String, childUser: User): Result<FocusCard> {
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

        val time = com.focal.app.streak.getDeviceCurrentTime()
        val h = if (time.hour24 % 12 == 0) 12 else time.hour24 % 12
        val ampm = if (time.hour24 < 12) "AM" else "PM"
        val timeStr = "Today, $h:${time.minute.toString().padStart(2, '0')} $ampm"
        val cardIdx = nfcCards.indexOf(card)
        val updatedCard = card.copy(lastUsed = timeStr, lastUsedChildName = childUser.name)
        if (cardIdx >= 0) {
            nfcCards[cardIdx] = updatedCard
        }

        return Result.success(updatedCard)
    }

    // Validate NFC card for ending focus early (family-wide card)
    override fun validateNfcCardForChildEnd(cardIdOrToken: String, childUser: User, currentSession: FocusSession?): Result<FocusCard> {
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

        val time = com.focal.app.streak.getDeviceCurrentTime()
        val h = if (time.hour24 % 12 == 0) 12 else time.hour24 % 12
        val ampm = if (time.hour24 < 12) "AM" else "PM"
        val timeStr = "Today, $h:${time.minute.toString().padStart(2, '0')} $ampm"
        val cardIdx = nfcCards.indexOf(card)
        val updatedCard = card.copy(lastUsed = timeStr, lastUsedChildName = childUser.name)
        if (cardIdx >= 0) {
            nfcCards[cardIdx] = updatedCard
        }

        return Result.success(updatedCard)
    }

    // Disable a family card immediately across all child devices
    override fun disableCard(cardId: String) {
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            nfcCards[idx] = nfcCards[idx].copy(isEnabled = false, status = CardStatus.DISABLED)
        }
    }

    // Enable a family card across all child devices
    override fun enableCard(cardId: String) {
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            nfcCards[idx] = nfcCards[idx].copy(isEnabled = true, status = CardStatus.ACTIVE)
        }
    }

    // Validate QR token for starting focus (Family QR or single-use token)
    override fun validateQrTokenForChildStart(qrPayload: String, childUser: User): Result<Pair<FocusPolicy, Int>> {
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
    override fun verifyNfcCard(cardId: String, nfcUid: String, token: String): Result<FocusCard> {
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
    override fun verifyQrToken(token: String): Result<QrToken> {
        val trimmed = token.trim()
        if (trimmed.startsWith("focal://pair", ignoreCase = true)) {
            return Result.failure(Exception("That's not a FOCAL focus QR"))
        }
        if (trimmed.startsWith("focal://focus", ignoreCase = true)) {
            val qr = QrToken(
                id = "qr_family_focus",
                token = trimmed,
                ownerId = "user_priya",
                childId = currentUser.id,
                policyId = "policy_study",
                policyTitle = "Study Mode",
                durationMinutes = 25,
                status = "ACTIVE",
                createdAt = currentTimeMillis().toString(),
                expiresAt = "0"
            )
            return Result.success(qr)
        }
        val qr = qrTokens.find { it.token.equals(trimmed, ignoreCase = true) }
            ?: return Result.failure(Exception("That's not a FOCAL focus QR"))

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
    override fun registerNfcCard(cardId: String, nfcUid: String, token: String): Result<FocusCard> {
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
    override fun programAndAssignNfcCard(cardId: String, targetChildId: String?): FocusCard {
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
    override fun programCard(cardId: String, nfcUid: String, token: String): FocusCard {
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

    override fun startRemoteSession(childId: String, policyId: String, policyName: String, durationMinutes: Int) {
        val policy = policies.find { it.id == policyId } ?: FocusPolicy(policyId, childId, policyName, FocusMode.STUDY, durationMinutes, emptyList(), emptyList())
        com.focal.app.engine.FocusEngine.instance.remoteStartFocus(childId, policy, durationMinutes)
    }

    override fun stopRemoteSession(childId: String) {
        com.focal.app.engine.FocusEngine.instance.remoteStopFocus(childId)
    }

    // Create Focus Policy (Controller)
    override fun addPolicy(policy: FocusPolicy) {
        policies.add(0, policy)
    }

    // Update Focus Policy (Controller)
    override fun updatePolicy(updated: FocusPolicy) {
        val index = policies.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            policies[index] = updated
        }
    }

    // Delete Focus Policy (Controller)
    override fun deletePolicy(policyId: String) {
        policies.removeAll { it.id == policyId }
    }


    override fun getPolicyUsage(policyId: String): PolicyUsage {
        val policy = policies.find { it.id == policyId }
        val pName = policy?.name ?: ""
        val children = childrenList.filter { it.defaultPolicyId == policyId || it.defaultPolicyId.equals(pName, ignoreCase = true) }
        val scheds = schedules.filter { it.policyName.equals(pName, ignoreCase = true) || it.policyName.equals(policyId, ignoreCase = true) }
        return PolicyUsage(children, scheds)
    }

    override fun deletePolicyWithReplacement(policyId: String, replacementPolicyId: String) {
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
    override fun generateFocusQr(childId: String, policyId: String, policyTitle: String, durationMinutes: Int): QrToken {
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
    override fun submitEmergencyExitRequest(
        sessionId: String,
        reason: String,
        note: String,
        childId: String,
        childName: String
    ): EmergencyExitRequest {
        val targetChildId = childId.ifBlank { "child_1" }
        val targetChildName = childName.ifBlank { "Aarav" }

        // 1. Any older PENDING request for that child -> CANCELLED
        emergencyRequests.forEachIndexed { index, existingReq ->
            if (existingReq.status == "PENDING" && (existingReq.childId == targetChildId || targetChildId == "child_1" || existingReq.childId == "child_1")) {
                emergencyRequests[index] = existingReq.copy(status = "CANCELLED")
            }
        }

        // 2. New request -> new UUID id
        val newId = com.focal.app.model.generateUuid()
        val now = currentTimeMillis()
        val req = EmergencyExitRequest(
            id = newId,
            sessionId = sessionId,
            childId = targetChildId,
            childName = targetChildName,
            reason = reason,
            note = note,
            requestedAt = "Just now",
            requestedAtMillis = now,
            status = "PENDING"
        )
        emergencyRequests.add(0, req)
        EmergencyExitRepository.instance.notifyPending(req)

        val fullDetail = if (note.isNotBlank()) "$reason: $note" else reason
        ActivityRepository.instance.recordEvent(
            type = ActivityEventType.EXIT_REQUESTED,
            childId = targetChildId,
            details = "$targetChildName requested early exit: '${fullDetail.ifEmpty { "No reason provided" }}'",
            metadata = mapOf("requestId" to newId, "sessionId" to sessionId, "reason" to reason, "note" to note)
        )
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "EMERGENCY_REQUEST",
            title = "Emergency Exit Request",
            message = "$targetChildName requested early exit: '${fullDetail.ifEmpty { "No reason provided" }}'",
            timestamp = "Just now"
        ))
        return req
    }

    // Approve / Decline Emergency Exit (Controller)
    override fun resolveEmergencyRequest(requestId: String, approve: Boolean) {
        val index = emergencyRequests.indexOfFirst { it.id == requestId }
        if (index >= 0) {
            val req = emergencyRequests[index]
            val newStatus = if (approve) "APPROVED" else "DECLINED"
            emergencyRequests[index] = req.copy(status = newStatus)

            val parentName = controllerUser.name.ifBlank { "Parent" }

            if (approve) {
                EmergencyExitRepository.instance.notifyApproved(requestId)
                com.focal.app.engine.FocusEngine.instance.endFocusWithEmergency()
            } else {
                EmergencyExitRepository.instance.notifyDeclined(requestId, parentName)
            }

            val eventType = if (approve) ActivityEventType.EXIT_APPROVED else ActivityEventType.EXIT_DECLINED
            val eventDetails = if (approve) "$parentName approved emergency exit for ${req.childName}" else "$parentName declined emergency exit for ${req.childName}"
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
                message = if (approve) "$parentName approved your emergency exit." else "$parentName said keep going!",
                timestamp = "Just now"
            ))
        }
    }

    override fun cancelEmergencyExitRequest(childId: String, requestId: String?) {
        val targetChildId = childId.ifBlank { "child_1" }
        emergencyRequests.forEachIndexed { index, req ->
            if (req.status == "PENDING" && (requestId == null || req.id == requestId) && (req.childId == targetChildId || targetChildId == "child_1" || req.childId == "child_1")) {
                emergencyRequests[index] = req.copy(status = "CANCELLED")
            }
        }
        EmergencyExitRepository.instance.reset()
    }

    override fun expirePendingEmergencyRequests(childId: String?) {
        emergencyRequests.forEachIndexed { index, req ->
            if (req.status == "PENDING" && (childId == null || req.childId == childId || childId == "child_1" || req.childId == "child_1")) {
                emergencyRequests[index] = req.copy(status = "EXPIRED")
            }
        }
        EmergencyExitRepository.instance.reset()
    }

    override fun checkEmergencyRequestExpirations() {
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
    override fun processSubscriptionPayment(planType: PlanType, amountInr: Int) {
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

    override fun renewSubscriptionMonthly() {
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
    override fun toggleSchedule(scheduleId: String) {
        val index = schedules.indexOfFirst { it.id == scheduleId }
        if (index >= 0) {
            val item = schedules[index]
            schedules[index] = item.copy(isEnabled = !item.isEnabled)
        }
    }

    // Save schedule (create or update)
    override fun saveSchedule(schedule: ScheduleItem) {
        val index = schedules.indexOfFirst { it.id == schedule.id }
        if (index >= 0) {
            schedules[index] = schedule
        } else {
            schedules.add(0, schedule)
        }
    }

    // Delete schedule
    override fun deleteSchedule(scheduleId: String) {
        schedules.removeAll { it.id == scheduleId }
    }

    // Update child profile
    override fun updateChild(updated: ChildProfile) {
        val index = childrenList.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            childrenList[index] = updated
        }
    }

    // Remove child profile
    override fun removeChild(childId: String) {
        childrenList.removeAll { it.id == childId }
    }

    // Unlink child device
    override fun unlinkChildDevice(childId: String) {
        val index = childrenList.indexOfFirst { it.id == childId }
        if (index >= 0) {
            childrenList[index] = childrenList[index].copy(
                childUid = null,
                deviceName = "",
                pairedCardId = null,
                isOnline = false,
                isFocusing = false,
                remainingMinutes = 0
            )
        }
    }

    // Add Child
    override fun addChild(name: String, age: Int, emailOrPhone: String): Pair<ChildProfile, String> {
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

    // Real Protection State (Accessibility + Overlay + Device Admin required)
    override fun isDeviceProtectionActive(): Boolean {
        return com.focal.app.ui.components.PlatformPermissionsHelper.isAccessibilityEnabled() &&
               com.focal.app.ui.components.PlatformPermissionsHelper.isOverlayEnabled() &&
               com.focal.app.ui.components.PlatformPermissionsHelper.isDeviceAdminActive()
    }

    override fun isChildProtectionOff(childId: String): Boolean {
        val child = childrenList.find { it.id == childId }
        val isThisDevice = childId == "child_1" || (currentUser.role == UserRole.CHILD && currentUser.id == child?.userId)
        return if (isThisDevice) {
            !isDeviceProtectionActive()
        } else {
            child?.isProtectionOff ?: false
        }
    }

    // Protection Telemetry, Settings Guard & Server Heartbeat Logic
    override var isProtectionDisabledOnChild by mutableStateOf(false)
    override var protectionDisabledReason by mutableStateOf("")
    override var lastHeartbeatTimeMs by mutableStateOf(currentTimeMillis())
    override var lastHeartbeatBatteryLevel by mutableStateOf(85)
    override var lastOfflineAlertSentTimeMs by mutableStateOf(0L)
    override var isDeviceOfflineAlertActive by mutableStateOf(false)

    // Protection Guard Mode: "ALWAYS" (default for child) or "DURING_FOCUS_ONLY"
    override var childGuardMode by mutableStateOf("ALWAYS")

    // Parent Unlock Timer (Pauses settings guard for 10 minutes)
    override var parentUnlockedUntilMs by mutableStateOf(0L)
    override var showParentUnlockScreen by mutableStateOf(false)

    // Trigger to navigate to Child Home (tab 0)
    override var requestNavigateToChildHomeCounter by mutableStateOf(0)

    override fun navigateToChildHome() {
        requestNavigateToChildHomeCounter++
    }

    override fun isParentGuardUnlocked(): Boolean {
        return currentTimeMillis() < parentUnlockedUntilMs
    }

    override fun unlockParentGuardFor10Minutes(): String {
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

    override fun clearParentUnlock() {
        parentUnlockedUntilMs = 0L
        com.focal.app.protection.ProtectionStateRepository.instance.clearParentUnlock()
    }

    override fun reportProtectionDisabled(reason: String, childId: String) {
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

    override fun reportProtectionRestored(childId: String) {
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

    override fun processChildHeartbeat(childId: String, permissionsMap: Map<String, Boolean>, battery: Int) {
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

    override fun reportGuardTriggered(screenName: String) {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "GUARD_TRIGGERED",
            title = "Settings Guard Intercepted",
            message = "Blocked attempt to access: $screenName",
            timestamp = "Just now"
        ))
    }

    override fun reportUninstallAttempt() {
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

    override fun reportForceStopAttempt() {
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

    override fun reportDeviceAdminDisableAttempt() {
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

    override fun reportDeviceRebootedDuringFocus() {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "DEVICE_REBOOTED_DURING_FOCUS",
            title = "Device Rebooted During Focus",
            message = "Child's phone restarted while a focus session was active. Protection auto-restored.",
            timestamp = "Just now"
        ))
    }

    override fun reportUsbDebuggingOn() {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "USB_DEBUGGING_ON",
            title = "USB Debugging / ADB Enabled",
            message = "USB Debugging or Developer Options turned on on child device.",
            timestamp = "Just now"
        ))
    }

    override fun reportFocusEndedWithoutCard() {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "FOCUS_ENDED_WITHOUT_CARD",
            title = "Focus Ended Without Card",
            message = "Session ended without card tap or parent approval.",
            timestamp = "Just now"
        ))
    }

    override fun reportClockChanged() {
        notifications.add(0, NotificationItem(
            id = "notif_${currentTimeMillis()}",
            userId = controllerUser.id,
            type = "CLOCK_CHANGED",
            title = "System Time Tampering Detected",
            message = "System clock was changed on child device. Session duration calculated using monotonic hardware clock.",
            timestamp = "Just now"
        ))
    }

    override fun reportExtraProfileDetected() {
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
    override var isSoloStrictModeEnabled by mutableStateOf(false)

    override fun toggleSoloStrictMode(cardVerified: Boolean): Pair<Boolean, String> {
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

    override fun checkServerHeartbeatTimeout(childId: String) {
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
    override val parentAccounts = mutableStateListOf(
        ParentAccount(
            id = "user_priya",
            name = "Priya Sharma",
            email = "priya@gmail.com",
            childrenIds = listOf("child_1", "child_2"),
            subscriptionStatus = ParentSubscriptionStatus.TRIAL_3_MONTHS,
            trialDaysLeft = 62,
            registeredDate = "Aug 2026"
        ),
        ParentAccount(
            id = "user_rohit",
            name = "Rohit Mehta",
            email = "rohit.m@gmail.com",
            childrenIds = listOf("child_3"),
            subscriptionStatus = ParentSubscriptionStatus.ACTIVE_MONTHLY,
            trialDaysLeft = 0,
            registeredDate = "Sep 2026"
        )
    )

    override val children get() = childrenList
    override val cards get() = nfcCards

    override val auditLogs = mutableStateListOf(
        AdminAuditLog("log_1", "admin@focal.app", "Added card 100001 to stock", "Inventory", "2h ago"),
        AdminAuditLog("log_2", "admin@focal.app", "Opened Priya Sharma", "Parent account", "Yesterday")
    )

    override val adminDevices = mutableStateListOf(
        AdminDeviceItem(
            id = "dev_1",
            childName = "Aarav",
            parentName = "Priya Sharma",
            deviceModel = "Realme",
            osVersion = "Android 14",
            appVersion = "1.2.0",
            isOnline = true,
            isProtectionActive = true,
            lastHeartbeat = "1m",
            isFocusing = true,
            batteryPercent = 82,
            accessibilityGranted = true,
            overlayGranted = true,
            deviceAdminGranted = true,
            usageGranted = true,
            notificationsGranted = true,
            batteryUnrestricted = true
        ),
        AdminDeviceItem(
            id = "dev_2",
            childName = "Kiara",
            parentName = "Priya Sharma",
            deviceModel = "Samsung",
            osVersion = "Android 14",
            appVersion = "1.2.0",
            isOnline = true,
            isProtectionActive = false,
            lastHeartbeat = "40m",
            isFocusing = false,
            batteryPercent = 64,
            accessibilityGranted = false,
            overlayGranted = true,
            deviceAdminGranted = true,
            usageGranted = true,
            notificationsGranted = true,
            batteryUnrestricted = true
        )
    )

    override fun logAdminAction(action: String, target: String) {
        auditLogs.add(0, AdminAuditLog(
            id = "log_${currentTimeMillis()}",
            adminId = "admin@focal.app",
            action = action,
            target = target,
            timestamp = "Just now"
        ))
    }

    override fun addInventoryCard(serial: String, activationCode: String): FocusCard {
        val trimmedSerial = serial.trim()
        val formattedSerial = if (trimmedSerial.startsWith("Card", ignoreCase = true)) trimmedSerial else "Card $trimmedSerial"
        val trimmedCode = activationCode.trim()
        val formattedCode = if (trimmedCode.startsWith("TK_")) trimmedCode else "TK_$trimmedCode"
        val newCard = FocusCard(
            id = "card_${currentTimeMillis()}",
            cardId = formattedSerial,
            token = formattedCode,
            ownerParentId = null,
            ownerId = null,
            ownerName = null,
            assignedChildId = null,
            status = CardStatus.IN_STOCK,
            registeredAt = "Today",
            createdAt = "Today",
            nfcUid = "04:C3:D4:E5:F6:A1:B2",
            ndefPayload = "FOCUSCARD:$formattedCode",
            tapCount = 0,
            lastTapTime = null
        )
        nfcCards.add(0, newCard)
        logAdminAction("Added $formattedSerial to stock", "Inventory")
        return newCard
    }

    override fun adminExtendTrial(userId: String, days: Int) {
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

    override fun adminResetChildPairing(childId: String) {
        val child = childrenList.find { it.id == childId }
        if (child != null) {
            val idx = childrenList.indexOf(child)
            childrenList[idx] = child.copy(isOnline = false, isFocusing = false, pairedCardId = null)
            logAdminAction("Reset child device pairing", "${child.name}'s device")
        }
    }

    override fun adminRevokeCard(cardId: String) {
        val idx = nfcCards.indexOfFirst { it.cardId.equals(cardId, ignoreCase = true) }
        if (idx >= 0) {
            val card = nfcCards[idx]
            nfcCards[idx] = card.copy(status = CardStatus.REVOKED)
            logAdminAction("Revoked card", card.cardId)
        }
    }

    override fun adminSuspendAccount(userId: String) {
        val parentIdx = parentAccounts.indexOfFirst { it.id == userId }
        if (parentIdx != -1) {
            val parent = parentAccounts[parentIdx]
            parentAccounts[parentIdx] = parent.copy(subscriptionStatus = ParentSubscriptionStatus.SUSPENDED)
            logAdminAction("Suspended account", "${parent.name} (${parent.email})")
        } else {
            logAdminAction("Suspended account", "User ID: $userId")
        }
    }

    override fun adminProgramBatchCards(count: Int) {
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
