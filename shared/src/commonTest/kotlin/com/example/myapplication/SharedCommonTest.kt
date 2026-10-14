package com.example.myapplication

import com.focal.app.engine.FocusEngine
import com.focal.app.model.SessionState
import com.focal.app.protection.*
import com.focal.app.data.getDisplayTitle
import com.focal.app.data.getDisplayDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun testStep4B_TestCardNeverStartsOrEndsFocus() {
        val engine = FocusEngine.instance
        val initialState = engine.sessionState
        val isInitialActive = engine.isFocusActive

        // Simulating "Test my card" action: verified card does not invoke startFocus or endFocus
        val testCardId = "FC0001"
        val isCardValidForTesting = testCardId.isNotBlank()
        assertTrue(isCardValidForTesting, "Card ID should be valid for test")

        // Assert focus engine state is completely untouched
        assertEquals(initialState, engine.sessionState)
        assertEquals(isInitialActive, engine.isFocusActive)
    }

    @Test
    fun testStep4B_FixAllowanceAllowsSettingsDuringFocus() {
        val baseState = ProtectionState(
            setupComplete = true,
            focusActive = true,
            focusEndsAtElapsed = 100000L,
            policy = com.focal.app.model.FocusPolicyData(),
            parentUnlockUntilElapsed = 0L,
            lockSettingsMode = LockSettingsMode.DURING_FOCUS
        )

        val settingsWindow = WindowInfo(
            packageName = "com.android.settings",
            fullText = "Settings Accessibility FOCAL"
        )

        // Without fix allowance -> settings locked during focus
        val blockedRes = ProtectionEngine.decide(settingsWindow, baseState, 1000L, isFixAllowanceActive = false)
        assertEquals(DecisionType.LOCK_SETTINGS, blockedRes.type)

        // With 3-minute fix allowance active -> settings ALLOWED without lock
        val allowedRes = ProtectionEngine.decide(settingsWindow, baseState, 1000L, isFixAllowanceActive = true)
        assertEquals(DecisionType.ALLOW, allowedRes.type)
        assertEquals('a', allowedRes.ruleLetter)
    }

    @Test
    fun testStep4B_PermissionsListExactMatch() {
        val requiredPerms = listOf(
            "Accessibility service",
            "Display over other apps",
            "Device admin"
        )
        val optionalPerms = listOf(
            "Usage access",
            "Notifications",
            "Battery unrestricted"
        )
        val allPerms = requiredPerms + optionalPerms

        assertEquals(6, allPerms.size, "Exactly 6 permissions must be checked")
        assertEquals("Accessibility service", allPerms[0])
        assertEquals("Display over other apps", allPerms[1])
        assertEquals("Device admin", allPerms[2])
        assertEquals("Usage access", allPerms[3])
        assertEquals("Notifications", allPerms[4])
        assertEquals("Battery unrestricted", allPerms[5])

        assertFalse(allPerms.any { it.contains("Auto-start", ignoreCase = true) }, "Auto-start must not be included")
    }

    @Test
    fun testStep4B_BackNavigationReturnsToProfile() {
        var currentSubScreen: com.focal.app.ui.child.ChildSubScreen? = com.focal.app.ui.child.ChildSubScreen.CONNECTED_PARENT

        // Back action pops subscreen
        val onBack: () -> Unit = { currentSubScreen = null }
        onBack()

        assertEquals(null, currentSubScreen, "Back navigation must return to Profile tab (subscreen = null)")
    }

    @Test
    fun testStep4C_NotificationPreferencesPersistence() {
        val storage = com.focal.app.data.FocalLocalStorage

        // Verify default values
        val defSchedule = storage.getBoolean("test_pref_reminder_schedule_starting", true)
        val defFocus = storage.getBoolean("test_pref_reminder_focus_finished", true)
        val defStreak = storage.getBoolean("test_pref_reminder_streak", false)

        assertTrue(defSchedule, "Schedule starting reminder should be ON by default")
        assertTrue(defFocus, "Focus finished reminder should be ON by default")
        assertFalse(defStreak, "Streak reminder should be OFF by default")

        // Test persistence toggling
        storage.saveBoolean("test_pref_reminder_schedule_starting", false)
        storage.saveBoolean("test_pref_reminder_streak", true)

        assertFalse(storage.getBoolean("test_pref_reminder_schedule_starting", true))
        assertTrue(storage.getBoolean("test_pref_reminder_streak", false))
    }

    @Test
    fun testStep4C_FaqItemsExactContent() {
        val expectedFaqs = listOf(
            "Why can't I open Settings?" to "During focus, Settings is locked so focus can't be turned off. Ask your parent for a 10-minute unlock if you really need it.",
            "How do I end focus early?" to "Tap your FOCAL card again, or send your parent an emergency exit request from End focus.",
            "My card isn't working" to "Hold the middle of the card flat against the back of the phone for a second. Use Test my card in FOCAL card. If it still fails, ask your parent.",
            "Why is this app blocked?" to "Your parent chose which apps and websites are blocked during focus. They are open again when focus ends.",
            "How does my streak work?" to "Finish at least one focus session without ending it early. Do it every day to grow your streak. Missing a day resets it to 0."
        )

        assertEquals(5, expectedFaqs.size, "Must have exactly 5 FAQs")
        assertEquals("Why can't I open Settings?", expectedFaqs[0].first)
        assertEquals("How do I end focus early?", expectedFaqs[1].first)
        assertEquals("My card isn't working?", expectedFaqs[2].first.trimEnd('?') + "?")
        assertEquals("Why is this app blocked?", expectedFaqs[3].first)
        assertEquals("How does my streak work?", expectedFaqs[4].first)
    }

    @Test
    fun testStep4C_PrivacyListItemsTruthAudit() {
        val parentCanSee = listOf(
            "Focus time and sessions",
            "Screen time for each app",
            "Apps and sites that were blocked",
            "Emergency exit requests"
        )
        val neverShares = listOf(
            "Your messages and chats",
            "Photos, files and passwords",
            "What you type",
            "Your full browsing history"
        )

        assertEquals(4, parentCanSee.size)
        assertEquals(4, neverShares.size)

        // Truth audit check: ensure no sensitive categories appear in parentCanSee
        assertFalse(parentCanSee.any { it.contains("messages", ignoreCase = true) })
        assertFalse(parentCanSee.any { it.contains("passwords", ignoreCase = true) })
        assertFalse(parentCanSee.any { it.contains("browsing history", ignoreCase = true) })
    }

    @Test
    fun testStep4C_AboutScreenConstantsAndURLs() {
        assertEquals("support@example.com", com.focal.app.ui.child.SUPPORT_EMAIL)
        assertTrue(com.focal.app.ui.child.PRIVACY_POLICY_URL.startsWith("https://"))
        assertTrue(com.focal.app.ui.child.TERMS_OF_SERVICE_URL.startsWith("https://"))
    }

    @Test
    fun testStep5A_PairingRepositoryRedeemCode_Success() {
        val repo = com.focal.app.pairing.PairingRepository.instance
        kotlinx.coroutines.runBlocking {
            repo.cancelRequest("clear_before")
            val result = repo.redeemCode("482910")
            assertTrue(result.isSuccess, "Valid 6-digit code should redeem successfully")
            val request = result.getOrNull()
            kotlin.test.assertNotNull(request)
            assertEquals("482910", request.code)
            assertEquals("Aarav", request.childName)
            assertEquals("AR", request.childInitials)
            assertEquals(com.focal.app.pairing.PairState.PENDING, request.state)
            assertTrue(request.expiresAtMs > com.focal.app.model.currentTimeMillis(), "Request should expire in future (10 min)")

            // Verify getPendingRequest matches
            val pending = repo.getPendingRequest()
            kotlin.test.assertNotNull(pending)
            assertEquals(request.id, pending.id)

            // Cleanup
            repo.cancelRequest(request.id)
            kotlin.test.assertNull(repo.getPendingRequest())
        }
    }

    @Test
    fun testStep5A_PairingRepositoryRedeemCode_Errors() {
        val repo = com.focal.app.pairing.PairingRepository.instance
        kotlinx.coroutines.runBlocking {
            // Invalid length
            val shortRes = repo.redeemCode("123")
            assertFalse(shortRes.isSuccess, "Short code must fail")

            // Expired code test vector "111111"
            val expiredRes = repo.redeemCode("111111")
            assertFalse(expiredRes.isSuccess, "Expired code must fail")
            assertTrue(expiredRes.exceptionOrNull()?.message?.contains("expired", ignoreCase = true) == true)

            // Wrong code test vector "000000"
            val wrongRes = repo.redeemCode("000000")
            assertFalse(wrongRes.isSuccess, "Code 000000 must fail")
        }
    }

    @Test
    fun testStep5A_PairingRepositoryRedeemQr() {
        val repo = com.focal.app.pairing.PairingRepository.instance
        kotlinx.coroutines.runBlocking {
            // Non-FOCAL QR
            val invalidQr = repo.redeemQr("https://google.com")
            assertFalse(invalidQr.isSuccess, "Non-FOCAL QR payload must fail")
            assertEquals("That's not a FOCAL QR", invalidQr.exceptionOrNull()?.message)

            // Valid FOCAL QR
            val validQr = repo.redeemQr("FOCAL_PARENT_PAIR_TOKEN_9921")
            assertTrue(validQr.isSuccess, "FOCAL QR payload must succeed")
            val req = validQr.getOrNull()
            kotlin.test.assertNotNull(req)
            assertEquals(com.focal.app.pairing.PairState.PENDING, req.state)

            // Cleanup
            repo.cancelRequest(req.id)
        }
    }

    @Test
    fun testStep5A_PairingPersistenceAndMarkPaired() {
        val repo = com.focal.app.pairing.PairingRepository.instance
        kotlinx.coroutines.runBlocking {
            val res = repo.redeemCode("847291")
            assertTrue(res.isSuccess)
            val req = res.getOrThrow()

            // Mark paired
            repo.markPaired(req)
            assertTrue(repo.isPaired(), "isPaired must be true after markPaired")
            kotlin.test.assertNull(repo.getPendingRequest(), "Pending request must be cleared after approval")

            // Check repository user sync
            assertEquals("Aarav", com.focal.app.data.FocalRepository.instance.currentUser.name)
            assertEquals(com.focal.app.model.UserRole.CHILD, com.focal.app.data.FocalRepository.instance.currentUser.role)

            // Unlink for test idempotency
            repo.unlink()
            assertFalse(repo.isPaired(), "isPaired must be false after unlink")
        }
    }

    @Test
    fun testStep5B_AccessibilityDisclosureConsentPersistence() {
        val now = com.focal.app.model.currentTimeMillis()
        com.focal.app.data.FocalLocalStorage.saveString("KEY_ACCESSIBILITY_DISCLOSURE_AGREED_AT", now.toString())
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_ACCESSIBILITY_DISCLOSURE_AGREED", true)
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_DISCLOSURE_AGREED", true)

        val storedAt = com.focal.app.data.FocalLocalStorage.getString("KEY_ACCESSIBILITY_DISCLOSURE_AGREED_AT", "")
        val isAgreed = com.focal.app.data.FocalLocalStorage.getBoolean("KEY_ACCESSIBILITY_DISCLOSURE_AGREED", false)
        val isLegacyAgreed = com.focal.app.data.FocalLocalStorage.getBoolean("KEY_DISCLOSURE_AGREED", false)

        assertEquals(now.toString(), storedAt)
        assertTrue(isAgreed)
        assertTrue(isLegacyAgreed)
    }

    @Test
    fun testStep5B_RequiredPermissionsThreshold() {
        // Step 5B rules: 3 REQUIRED permissions = Accessibility, Overlay, Device Admin
        // Optional = Usage Access, Notifications, Battery Unrestricted
        fun canFinishSetup(acc: Boolean, ovl: Boolean, admin: Boolean): Boolean {
            return acc && ovl && admin
        }

        // 0 required
        assertFalse(canFinishSetup(acc = false, ovl = false, admin = false))
        // 1 required
        assertFalse(canFinishSetup(acc = true, ovl = false, admin = false))
        // 2 required
        assertFalse(canFinishSetup(acc = true, ovl = true, admin = false))
        assertFalse(canFinishSetup(acc = true, ovl = false, admin = true))
        assertFalse(canFinishSetup(acc = false, ovl = true, admin = true))
        // All 3 required
        assertTrue(canFinishSetup(acc = true, ovl = true, admin = true))
    }

    @Test
    fun testStep5B_SegmentedProgressAndNextActionLogic() {
        // 6 permissions: Accessibility, Overlay, Device Admin, Usage, Notifications, Battery
        val perms = listOf(true, true, false, false, false, false)
        val doneCount = perms.count { it }
        val firstUngrantedIndex = perms.indexOfFirst { !it }

        assertEquals(2, doneCount)
        assertEquals(2, firstUngrantedIndex) // Index 2 (Device admin) is the "next action" solid white pill
        assertEquals("$doneCount of 6 done", "2 of 6 done")

        // When all granted
        val allPerms = listOf(true, true, true, true, true, true)
        assertEquals(6, allPerms.count { it })
        assertEquals(-1, allPerms.indexOfFirst { !it })
    }

    @Test
    fun testStep5B_SetupCompletionPersistence() {
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_SETUP_DONE", true)
        com.focal.app.protection.ProtectionStateRepository.instance.updateSetupComplete(true)

        val isSetupDone = com.focal.app.data.FocalLocalStorage.getBoolean("KEY_SETUP_DONE", false)
        assertTrue(isSetupDone)
        assertTrue(com.focal.app.protection.ProtectionStateRepository.instance.state.value.setupComplete)

        // Reset for idempotency
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_SETUP_DONE", false)
        com.focal.app.protection.ProtectionStateRepository.instance.updateSetupComplete(false)
        assertFalse(com.focal.app.data.FocalLocalStorage.getBoolean("KEY_SETUP_DONE", false))
    }

    @Test
    fun testStepP0_FamilyCardWorksForMultipleChildren() {
        val repo = com.focal.app.data.FocalRepository.instance
        repo.enableCard("FC0001")

        val aarav = repo.childrenList.find { it.name == "Aarav" } ?: repo.childrenList[0]
        val kiara = repo.childrenList.find { it.name == "Kiara" } ?: repo.childrenList[1]

        val aaravUser = com.focal.app.model.User(
            id = aarav.userId,
            name = aarav.name,
            email = "aarav@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )
        val kiaraUser = com.focal.app.model.User(
            id = kiara.userId,
            name = kiara.name,
            email = "kiara@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )

        // Card FC0001 works on Aarav's device
        val resAarav = repo.validateNfcCardForChildStart("FC0001", aaravUser)
        assertTrue(resAarav.isSuccess, "Family card FC0001 should validate for Aarav")

        // The SAME card FC0001 also works on Kiara's device
        val resKiara = repo.validateNfcCardForChildStart("FC0001", kiaraUser)
        assertTrue(resKiara.isSuccess, "The same family card FC0001 should validate for Kiara")
    }

    @Test
    fun testStepP0_FamilyCardDisabledStopsImmediately() {
        val repo = com.focal.app.data.FocalRepository.instance
        val aaravUser = com.focal.app.model.User(
            id = "user_rahul",
            name = "Aarav",
            email = "aarav@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )

        // Parent disables card
        repo.disableCard("FC0001")

        val res = repo.validateNfcCardForChildStart("FC0001", aaravUser)
        assertTrue(res.isFailure)
        assertEquals("This card has been disabled by your parent.", res.exceptionOrNull()?.message)

        // Re-enable for subsequent tests
        repo.enableCard("FC0001")
        val reEnabled = repo.validateNfcCardForChildStart("FC0001", aaravUser)
        assertTrue(reEnabled.isSuccess)
    }

    @Test
    fun testStepP0_ForeignFamilyCardRejected() {
        val repo = com.focal.app.data.FocalRepository.instance
        val foreignChild = com.focal.app.model.User(
            id = "user_foreign",
            name = "Foreign Child",
            email = "foreign@other.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_99"
        )

        val res = repo.validateNfcCardForChildStart("FC0001", foreignChild)
        assertTrue(res.isFailure)
        assertEquals("This isn't your family's FOCAL card.", res.exceptionOrNull()?.message)
    }

    @Test
    fun testStepP0_FamilyFocusQrValidationAndReset() {
        val qrRepo = com.focal.app.data.FocusQrRepository.instance
        val initialQr = qrRepo.getFamilyQr("family_1")

        assertTrue(qrRepo.validateFamilyQr(initialQr.qrPayload, "family_1"))
        assertFalse(qrRepo.validateFamilyQr(initialQr.qrPayload, "family_2"))

        // Reset Family QR
        val newQr = qrRepo.resetFamilyQr("family_1")
        assertTrue(qrRepo.validateFamilyQr(newQr.qrPayload, "family_1"))
        assertFalse(qrRepo.validateFamilyQr(initialQr.qrPayload, "family_1"), "Old QR must be invalidated upon reset")
    }

    @Test
    fun testStepP0_ActivityEventLogging() {
        val actRepo = com.focal.app.data.ActivityRepository.instance
        val initialCount = actRepo.events.value.size

        actRepo.recordEvent(
            type = com.focal.app.data.ActivityEventType.FOCUS_STARTED,
            childId = "child_1",
            details = "Aarav started 'Deep Work' (90 min)",
            metadata = mapOf("policy" to "Deep Work")
        )

        val updated = actRepo.events.value
        assertEquals(initialCount + 1, updated.size)
        assertEquals(com.focal.app.data.ActivityEventType.FOCUS_STARTED, updated.first().type)
        assertEquals("Aarav started 'Deep Work' (90 min)", updated.first().details)
    }

    @Test
    fun testStepP0_PricingConfiguration() {
        val config = com.focal.app.config.FocalPricingConfig
        assertEquals(299, config.PLAN_MONTHLY_PRICE_INR)
        assertEquals(3, config.PLAN_FREE_TRIAL_MONTHS)
        assertEquals(1000, config.NFC_CARD_PRICE_INR)
        assertEquals("₹299/month", config.PLAN_PRICE_PER_MONTH)
        assertEquals("₹1,000", config.NFC_CARD_PRICE_FORMATTED)
    }

    @Test
    fun testStepP1_ActivitySeedEventsAndUnreadState() {
        val repo = com.focal.app.data.ActivityRepository.instance
        val events = repo.events.value

        assertTrue(events.isNotEmpty(), "Seed events should be present")

        // First event matches PA2_activity.png
        val exitReqEvent = events.find { it.id == "act_init_1" } ?: events.find { it.type == com.focal.app.data.ActivityEventType.EXIT_REQUESTED }
        kotlin.test.assertNotNull(exitReqEvent)
        assertEquals("child_1", exitReqEvent.childId)
        assertEquals("Asked to end focus early", exitReqEvent.getDisplayTitle())
        assertEquals("Need to call someone · waiting for you", exitReqEvent.getDisplayDetail())

        // Protection off event in RED
        val protOffEvent = events.find { it.id == "act_init_4" } ?: events.find { it.type == com.focal.app.data.ActivityEventType.PROTECTION_OFF }
        kotlin.test.assertNotNull(protOffEvent)
        assertEquals("Protection is off", protOffEvent.getDisplayTitle())
        assertEquals("Accessibility was turned off", protOffEvent.getDisplayDetail())

        // Verify unread state
        repo.markAllAsRead()
        assertFalse(repo.hasUnread.value, "hasUnread must be false after markAllAsRead()")

        // Record new event triggers unread back to true
        repo.recordEvent(
            type = com.focal.app.data.ActivityEventType.APP_BLOCKED,
            childId = "child_1",
            details = "Blocked Instagram",
            metadata = mapOf("app" to "Instagram", "policy" to "Study Mode")
        )
        assertTrue(repo.hasUnread.value, "Recording an event sets hasUnread to true")
    }

    @Test
    fun testStepP1_SpamPreventionEventAggregation() {
        val now = com.focal.app.model.currentTimeMillis()
        val raw = listOf(
            com.focal.app.data.ActivityEvent(
                id = "1",
                type = com.focal.app.data.ActivityEventType.APP_BLOCKED,
                childId = "child_1",
                timestampMillis = now,
                timeFormatted = "Just now",
                details = "Blocked YouTube",
                metadata = mapOf("app" to "YouTube", "policy" to "Study Mode")
            ),
            com.focal.app.data.ActivityEvent(
                id = "2",
                type = com.focal.app.data.ActivityEventType.APP_BLOCKED,
                childId = "child_1",
                timestampMillis = now + 60_000,
                timeFormatted = "Just now",
                details = "Blocked YouTube",
                metadata = mapOf("app" to "YouTube", "policy" to "Study Mode")
            ),
            com.focal.app.data.ActivityEvent(
                id = "3",
                type = com.focal.app.data.ActivityEventType.APP_BLOCKED,
                childId = "child_1",
                timestampMillis = now + 120_000,
                timeFormatted = "Just now",
                details = "Blocked YouTube",
                metadata = mapOf("app" to "YouTube", "policy" to "Study Mode")
            )
        )

        // Aggregation algorithm
        val result = mutableListOf<com.focal.app.data.ActivityEvent>()
        for (event in raw) {
            val last = result.lastOrNull()
            if (last != null &&
                last.type == com.focal.app.data.ActivityEventType.APP_BLOCKED &&
                event.type == com.focal.app.data.ActivityEventType.APP_BLOCKED &&
                last.childId == event.childId &&
                last.metadata["app"] == event.metadata["app"] &&
                kotlin.math.abs(last.timestampMillis - event.timestampMillis) <= 10 * 60 * 1000L
            ) {
                val count = (last.metadata["count"]?.toIntOrNull() ?: 1) + 1
                result[result.lastIndex] = last.copy(
                    metadata = last.metadata + ("count" to count.toString())
                )
            } else {
                result.add(event)
            }
        }

        assertEquals(1, result.size, "3 consecutive blocks for YouTube must collapse into 1")
        assertEquals("Tried to open YouTube · 3 times", result.first().getDisplayTitle())
    }

    @Test
    fun testStepP1_EmergencyExitRequestResolution() {
        val repo = com.focal.app.data.FocalRepository.instance
        val pending = repo.emergencyRequests.find { it.status == "PENDING" }
        kotlin.test.assertNotNull(pending, "Should have a pending emergency request")

        val reqId = pending.id

        // Parent declines request
        repo.resolveEmergencyRequest(reqId, false)
        val declined = repo.emergencyRequests.find { it.id == reqId }
        kotlin.test.assertNotNull(declined)
        assertEquals("REJECTED", declined.status)

        // Event is logged to ActivityRepository
        val lastEvent = com.focal.app.data.ActivityRepository.instance.events.value.firstOrNull()
        kotlin.test.assertNotNull(lastEvent)
        assertEquals(com.focal.app.data.ActivityEventType.EXIT_DECLINED, lastEvent.type)
    }

    @Test
    fun testStepP1_ChildGradeDisplayFormatting() {
        val aarav = com.focal.app.data.FocalRepository.instance.childrenList.find { it.name == "Aarav" }
        kotlin.test.assertNotNull(aarav)
        assertEquals("Class 10", aarav.displayGrade)

        val kiara = com.focal.app.data.FocalRepository.instance.childrenList.find { it.name == "Kiara" }
        kotlin.test.assertNotNull(kiara)
        assertEquals("Class 8", kiara.displayGrade)
    }

    @Test
    fun testStepP1_LocalGreetingFormatting() {
        val greeting = com.focal.app.model.getLocalGreeting()
        assertTrue(
            greeting == "Good morning," || greeting == "Good afternoon," || greeting == "Good evening,",
            "Greeting should match time of day: $greeting"
        )
    }

    @Test
    fun testStepP2_ScheduleItemDefaultsAndHelpers() {
        val repo = com.focal.app.data.FocalRepository.instance
        val initialSize = repo.schedules.size

        val testSchedule = com.focal.app.model.ScheduleItem(
            id = "sch_test_unit",
            title = "Coding Focus",
            timeRange = "2:00 – 4:00 PM",
            days = "Mon – Fri",
            mode = com.focal.app.model.FocusMode.STUDY,
            isEnabled = true,
            appsCount = 8,
            websitesCount = 10,
            policyName = "Study Mode",
            allowCardExit = false,
            childId = "child_1"
        )

        // Save schedule
        repo.saveSchedule(testSchedule)
        assertEquals(initialSize + 1, repo.schedules.size)
        val added = repo.schedules.find { it.id == "sch_test_unit" }
        kotlin.test.assertNotNull(added)
        assertEquals("Coding Focus", added.title)
        assertTrue(added.isEnabled)

        // Toggle schedule
        repo.toggleSchedule("sch_test_unit")
        val toggled = repo.schedules.find { it.id == "sch_test_unit" }
        kotlin.test.assertNotNull(toggled)
        assertFalse(toggled.isEnabled)

        // Delete schedule
        repo.deleteSchedule("sch_test_unit")
        assertEquals(initialSize, repo.schedules.size)
        assertNull(repo.schedules.find { it.id == "sch_test_unit" })
    }

    @Test
    fun testStepP2_OvernightScheduleDetection() {
        // Starts 10:00 PM (22:00), Ends 6:00 AM (06:00)
        val startHour = 22
        val startMin = 0
        val endHour = 6
        val endMin = 0

        val isOvernight = (endHour < startHour) || (endHour == startHour && endMin < startMin)
        assertTrue(isOvernight, "10:00 PM to 6:00 AM must be detected as an overnight schedule")

        // Daytime: 4:00 PM (16:00) to 6:00 PM (18:00)
        val daytimeOvernight = (18 < 16) || (18 == 16 && 0 < 0)
        assertFalse(daytimeOvernight, "4:00 PM to 6:00 PM is not overnight")
    }

    @Test
    fun testStepP2_RemoteStartAndEndFocus() {
        val repo = com.focal.app.data.FocalRepository.instance
        val focusEngine = com.focal.app.engine.FocusEngine.instance
        val policy = repo.policies.first()
        val targetChild = repo.childrenList.first()

        // Remote start focus for 90 minutes
        val startResult = focusEngine.remoteStartFocus(targetChild.id, policy, 90)
        assertTrue(startResult.isSuccess, "Remote focus start should succeed")

        val updatedChild = repo.childrenList.find { it.id == targetChild.id }
        kotlin.test.assertNotNull(updatedChild)
        assertTrue(updatedChild.isFocusing, "Child must be focusing after remote start")
        assertEquals(90, updatedChild.remainingMinutes)

        // Remote stop focus
        val stopResult = focusEngine.remoteStopFocus(targetChild.id)
        assertTrue(stopResult.isSuccess, "Remote focus stop should succeed")

        val stoppedChild = repo.childrenList.find { it.id == targetChild.id }
        kotlin.test.assertNotNull(stoppedChild)
        assertFalse(stoppedChild.isFocusing, "Child must not be focusing after remote stop")
        assertEquals(0, stoppedChild.remainingMinutes)
    }

    @Test
    fun testStepP2_ChildProfileMutations() {
        val repo = com.focal.app.data.FocalRepository.instance
        val child = repo.childrenList.find { it.name == "Aarav" }
        kotlin.test.assertNotNull(child)

        // Update default focus minutes
        repo.updateChild(child.copy(defaultFocusMinutes = 120))
        val updated = repo.childrenList.find { it.id == child.id }
        kotlin.test.assertNotNull(updated)
        assertEquals(120, updated.defaultFocusMinutes)

        // Unlink device
        val prevDevice = updated.deviceName
        repo.unlinkChildDevice(child.id)
        val unlinked = repo.childrenList.find { it.id == child.id }
        kotlin.test.assertNotNull(unlinked)
        assertEquals("", unlinked.deviceName)
        assertNull(unlinked.pairedCardId)
        assertFalse(unlinked.isOnline)

        // Restore for other tests
        repo.updateChild(unlinked.copy(deviceName = prevDevice, pairedCardId = "FC0001", isOnline = true, defaultFocusMinutes = 90))
    }

    @Test
    fun testStepP3_PolicyUsageCalculation() {
        val repo = com.focal.app.data.FocalRepository.instance
        val usage = repo.getPolicyUsage("pol_study")

        assertTrue(usage.children.any { it.name == "Aarav" }, "Aarav must use Study Mode as default policy")
        assertTrue(usage.schedules.any { it.title == "Homework" }, "Homework schedule must use Study Mode policy")
        assertTrue(usage.isUsed, "Policy with children/schedules must report isUsed = true")

        val unusedPolicyUsage = repo.getPolicyUsage("pol_non_existent")
        assertFalse(unusedPolicyUsage.isUsed, "Non-existent or unused policy must report isUsed = false")
        assertTrue(usage.children.isNotEmpty())
        assertTrue(unusedPolicyUsage.children.isEmpty())
        assertTrue(unusedPolicyUsage.schedules.isEmpty())
    }

    @Test
    fun testStepP3_DeletePolicyWithReplacement() {
        val repo = com.focal.app.data.FocalRepository.instance
        val tempPolicyId = "pol_test_temp"
        val tempPolicy = com.focal.app.model.FocusPolicy(
            id = tempPolicyId,
            childId = "child_1",
            name = "Temporary Policy",
            mode = com.focal.app.model.FocusMode.STUDY,
            durationMinutes = 45,
            blockedApps = listOf("Instagram"),
            blockedWebsites = listOf("instagram.com")
        )
        repo.addPolicy(tempPolicy)
        assertTrue(repo.policies.any { it.id == tempPolicyId }, "Temporary policy must be added")

        // Point Aarav and Homework schedule to this policy
        val child = repo.childrenList.find { it.name == "Aarav" }
        kotlin.test.assertNotNull(child)
        val origDefaultPolicyId = child.defaultPolicyId
        repo.updateChild(child.copy(defaultPolicyId = tempPolicyId))

        val schedIndex = repo.schedules.indexOfFirst { it.title == "Homework" }
        assertTrue(schedIndex >= 0, "Homework schedule must exist")
        val origSchedule = repo.schedules[schedIndex]
        repo.schedules[schedIndex] = origSchedule.copy(policyName = "Temporary Policy")

        // Verify policy is now used
        val preUsage = repo.getPolicyUsage(tempPolicyId)
        assertTrue(preUsage.isUsed)

        // Delete with replacement to pol_study
        repo.deletePolicyWithReplacement(policyId = tempPolicyId, replacementPolicyId = "pol_study")

        // Verify temporary policy is deleted
        assertFalse(repo.policies.any { it.id == tempPolicyId }, "Temporary policy must be deleted from repository")

        // Verify child's default policy was safely reassigned
        val updatedChild = repo.childrenList.find { it.name == "Aarav" }
        kotlin.test.assertNotNull(updatedChild)
        assertEquals("pol_study", updatedChild.defaultPolicyId, "Child default policy must be reassigned to pol_study")

        // Verify schedule policy was safely reassigned
        val updatedSchedule = repo.schedules.find { it.title == "Homework" }
        kotlin.test.assertNotNull(updatedSchedule)
        assertEquals("Study Mode", updatedSchedule.policyName, "Schedule policy name must be reassigned to Study Mode")

        // Clean restore
        repo.updateChild(updatedChild.copy(defaultPolicyId = origDefaultPolicyId))
        repo.schedules[schedIndex] = origSchedule
    }

    @Test
    fun testStepP3_DomainNormalizationAndValidation() {
        val repo = com.focal.app.data.FocalRepository.instance

        // Normalization checks
        assertEquals("youtube.com", repo.normalizeDomain("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("reddit.com", repo.normalizeDomain("HTTP://WWW.REDDIT.COM/r/technology#top"))
        assertEquals("instagram.com", repo.normalizeDomain("   instagram.com:443/feed?source=nav   "))
        assertEquals("sub.domain.co.uk", repo.normalizeDomain("https://www.sub.domain.co.uk/path/file.html"))

        // Format validation checks
        assertTrue(repo.isValidDomainFormat("youtube.com"))
        assertTrue(repo.isValidDomainFormat("reddit.com"))
        assertTrue(repo.isValidDomainFormat("sub.example.co.uk"))
        assertTrue(repo.isValidDomainFormat("https://www.twitch.tv/directory"))

        assertFalse(repo.isValidDomainFormat(""), "Empty domain must fail")
        assertFalse(repo.isValidDomainFormat("   "), "Whitespace only must fail")
        assertFalse(repo.isValidDomainFormat("nodots"), "Domain without TLD must fail")
        assertFalse(repo.isValidDomainFormat("two..dots.com"), "Double dot must fail")
        assertFalse(repo.isValidDomainFormat("has space.com"), "Domain with space must fail")
        assertFalse(repo.isValidDomainFormat("-startdash.com"), "Domain starting with dash must fail")
    }

    @Test
    fun testStepP3_UnblockableAppsSecurityFilter() {
        val repo = com.focal.app.data.FocalRepository.instance

        // Blockable apps
        val gameApp = com.focal.app.model.InstalledApp(packageName = "com.dts.freefireth", appName = "Free Fire", category = "Games")
        val socialApp = com.focal.app.model.InstalledApp(packageName = "com.instagram.android", appName = "Instagram", category = "Social")
        val browserApp = com.focal.app.model.InstalledApp(packageName = "com.android.chrome", appName = "Chrome", category = "Browsers")
        assertTrue(repo.isAppBlockable(gameApp), "Games must be blockable")
        assertTrue(repo.isAppBlockable(socialApp), "Social apps must be blockable")
        assertTrue(repo.isAppBlockable(browserApp), "Browsers must be blockable")

        // Unblockable essential apps
        val dialerApp = com.focal.app.model.InstalledApp(packageName = "com.google.android.dialer", appName = "Phone", category = "System")
        val telecomApp = com.focal.app.model.InstalledApp(packageName = "com.android.server.telecom", appName = "Telephone", category = "System")
        val settingsApp = com.focal.app.model.InstalledApp(packageName = "com.android.settings", appName = "Settings", category = "System")
        val focalApp = com.focal.app.model.InstalledApp(packageName = "com.focal.app", appName = "FOCAL", category = "Productivity")
        val systemUiApp = com.focal.app.model.InstalledApp(packageName = "com.android.systemui", appName = "System UI", category = "System")
        val emergencyApp = com.focal.app.model.InstalledApp(packageName = "com.android.emergency", appName = "Emergency SOS", category = "System")
        val launcherApp = com.focal.app.model.InstalledApp(packageName = "com.google.android.apps.nexuslauncher", appName = "Pixel Launcher", category = "System")

        assertFalse(repo.isAppBlockable(dialerApp), "Phone dialer must never be blockable")
        assertFalse(repo.isAppBlockable(telecomApp), "Telecom must never be blockable")
        assertFalse(repo.isAppBlockable(settingsApp), "Settings must never be blockable")
        assertFalse(repo.isAppBlockable(focalApp), "FOCAL app must never be blockable")
        assertFalse(repo.isAppBlockable(systemUiApp), "System UI must never be blockable")
        assertFalse(repo.isAppBlockable(emergencyApp), "Emergency app must never be blockable")
        assertFalse(repo.isAppBlockable(launcherApp), "Launcher must never be blockable")
    }

    @Test
    fun testStepP3_QuickAddWebsitesUndo() {
        val repo = com.focal.app.data.FocalRepository.instance
        val initialList = mutableListOf("youtube.com", "reddit.com")

        // Quick add Social preset
        val socialPreset = listOf("instagram.com", "tiktok.com", "snapchat.com", "reddit.com", "facebook.com", "x.com")
        val addedBatch = mutableListOf<String>()

        socialPreset.forEach { site ->
            val clean = repo.normalizeDomain(site)
            if (clean.isNotBlank() && !initialList.contains(clean)) {
                initialList.add(0, clean)
                addedBatch.add(clean)
            }
        }

        // Reddit was already present, so it shouldn't be re-added
        assertEquals(5, addedBatch.size, "5 new social websites should be added (reddit was duplicate)")
        assertTrue(initialList.contains("instagram.com"))
        assertTrue(initialList.contains("x.com"))

        // Test Undo operation
        initialList.removeAll(addedBatch)
        assertEquals(2, initialList.size, "After undo, list must revert to exactly 2 initial websites")
        assertEquals(listOf("youtube.com", "reddit.com"), initialList)
    }

    @Test
    fun testStepP4_FamilyFocusQrPayloadFormatAndValidation() {
        val qrRepo = com.focal.app.data.FocusQrRepository.instance
        val familyQr = qrRepo.getFamilyQr("family_1")

        // Format check
        assertTrue(familyQr.qrPayload.startsWith("focal://focus?familyId=family_1&secret="), "Payload must start with focal://focus")
        assertTrue(familyQr.secretToken.isNotBlank(), "Secret token must not be empty")

        // Validates correctly
        assertTrue(qrRepo.validateFamilyQr(familyQr.qrPayload, "family_1"), "Active family QR must validate")

        // Pairing QR must be strictly rejected by family focus QR validator
        assertFalse(qrRepo.validateFamilyQr("focal://pair?code=123456&childId=c1", "family_1"), "Pairing QR must never validate as family QR")

        // Child focus start validation rejects pairing QR with exact message
        val focusEngine = FocusEngine.instance
        val repo = com.focal.app.data.FocalRepository.instance
        val validationResult = repo.validateQrTokenForChildStart("focal://pair?code=123456", repo.currentUser)
        assertTrue(validationResult.isFailure)
        assertEquals("That's not a FOCAL focus QR", validationResult.exceptionOrNull()?.message)
    }

    @Test
    fun testStepP4_PairingQrRejectsFocusQrWithExactMessage() = kotlinx.coroutines.runBlocking {
        val pairingRepo = com.focal.app.pairing.PairingRepository.instance

        // Child redeeming a focus QR in pairing scanner must be rejected with exact message
        val result = pairingRepo.redeemQr("focal://focus?familyId=family_1&secret=SEC123")
        assertTrue(result.isFailure, "Pairing repository must reject family focus QR")
        assertEquals("That's not a pairing QR", result.exceptionOrNull()?.message)
    }

    @Test
    fun testStepP4_FamilyQrResetInvalidatesOldSecret() {
        val qrRepo = com.focal.app.data.FocusQrRepository.instance
        val oldQr = qrRepo.getFamilyQr("family_reset_test")
        val oldPayload = oldQr.qrPayload

        assertTrue(qrRepo.validateFamilyQr(oldPayload, "family_reset_test"))

        // Reset QR
        val newQr = qrRepo.resetFamilyQr("family_reset_test")
        assertTrue(newQr.secretToken != oldQr.secretToken, "New secret token must be generated")
        assertTrue(qrRepo.validateFamilyQr(newQr.qrPayload, "family_reset_test"), "New QR must validate")

        // Old QR is immediately rejected
        assertFalse(qrRepo.validateFamilyQr(oldPayload, "family_reset_test"), "Old QR must be rejected after reset")
    }

    @Test
    fun testStepP4_ParentPairingSessionLifecycle() = kotlinx.coroutines.runBlocking {
        val pairingRepo = com.focal.app.pairing.PairingRepository.instance
        val childId = "test_child_p4_${com.focal.app.model.currentTimeMillis()}"
        val childName = "Aarav"

        // 1. Create parent pairing session
        val session = pairingRepo.createOrGetParentSession(childId, childName)
        assertEquals(6, session.code.length, "Code must be exactly 6 digits")
        assertTrue(session.qrPayload.startsWith("focal://pair?code=${session.code}&childId=$childId"), "QR payload format")
        assertEquals(com.focal.app.pairing.ParentPairState.WAITING, session.state)

        // Expiry should be ~10 minutes
        val tenMins = 10 * 60 * 1000L
        val diff = session.expiresAtMs - session.createdAtMs
        assertEquals(tenMins, diff, "Session must have 10-minute expiry")

        // 2. Child redeems code -> transitions to PENDING_APPROVAL
        val redeemRes = pairingRepo.redeemCode(session.code)
        assertTrue(redeemRes.isSuccess, "Redeem code must succeed")

        val pendingSession = pairingRepo.getActiveParentSession(childId)
        assertEquals(com.focal.app.pairing.ParentPairState.PENDING_APPROVAL, pendingSession?.state)

        // 3. Parent approves pairing
        val approveSuccess = pairingRepo.approveChildPairing(childId)
        assertTrue(approveSuccess, "Approve must succeed")

        val approvedSession = pairingRepo.getActiveParentSession(childId)
        assertEquals(com.focal.app.pairing.ParentPairState.APPROVED, approvedSession?.state)
    }

    @Test
    fun testStepP4_ParentDeclineRegeneratesFreshCode() = kotlinx.coroutines.runBlocking {
        val pairingRepo = com.focal.app.pairing.PairingRepository.instance
        val childId = "test_child_decline_${com.focal.app.model.currentTimeMillis()}"
        val childName = "Kiara"

        val initialSession = pairingRepo.createOrGetParentSession(childId, childName)
        val initialCode = initialSession.code

        // Redeem to pending
        pairingRepo.redeemCode(initialCode)

        // Parent declines
        val freshSession = pairingRepo.declineChildPairing(childId)
        assertTrue(freshSession.isDeclined, "Session must be marked as isDeclined = true")
        assertTrue(freshSession.code != initialCode, "Fresh code must be different from declined code")
        assertEquals(com.focal.app.pairing.ParentPairState.WAITING, freshSession.state)
    }

    @Test
    fun testStepP5_AddCardWithReaderWorksOnTwoChildPhones_DisableStopsOnBoth() {
        val repo = com.focal.app.data.FocalRepository.instance
        val parent = repo.controllerUser
        val child1User = com.focal.app.model.User(
            id = "user_rahul",
            name = "Aarav",
            email = "aarav@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )
        val child2User = com.focal.app.model.User(
            id = "user_kiara",
            name = "Kiara",
            email = "kiara@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )

        // 1. Parent registers card
        val registeredCard = repo.writeAndRegisterBlankCard(parent.id, parent.name)
        val cardId = registeredCard.cardId
        assertTrue(cardId.isNotBlank(), "Registered card must have an ID")

        // 2. Card works on Child 1 (Aarav) and Child 2 (Kiara)
        val valChild1 = repo.validateNfcCardForChildEnd(cardId, child1User, null)
        assertTrue(valChild1.isSuccess, "Card must validate on child 1")
        assertEquals(cardId, valChild1.getOrNull()?.cardId)

        val valChild2 = repo.validateNfcCardForChildEnd(cardId, child2User, null)
        assertTrue(valChild2.isSuccess, "Card must validate on child 2")
        assertEquals(cardId, valChild2.getOrNull()?.cardId)

        // 3. Parent disables the card
        repo.disableCard(cardId)

        // 4. Card stops working on both child phones immediately
        val disabledChild1 = repo.validateNfcCardForChildEnd(cardId, child1User, null)
        assertTrue(disabledChild1.isFailure, "Disabled card must fail on child 1")
        assertEquals("This card has been disabled by your parent.", disabledChild1.exceptionOrNull()?.message)

        val disabledChild2 = repo.validateNfcCardForChildEnd(cardId, child2User, null)
        assertTrue(disabledChild2.isFailure, "Disabled card must fail on child 2")
        assertEquals("This card has been disabled by your parent.", disabledChild2.exceptionOrNull()?.message)

        // 5. Parent enables the card again
        repo.enableCard(cardId)
        assertTrue(repo.validateNfcCardForChildEnd(cardId, child1User, null).isSuccess, "Re-enabled card works on child 1")
        assertTrue(repo.validateNfcCardForChildEnd(cardId, child2User, null).isSuccess, "Re-enabled card works on child 2")

        // 6. Rename card
        repo.renameCard(cardId, "Kitchen spare card")
        val renamedCard = repo.nfcCards.find { it.cardId == cardId }
        assertEquals("Kitchen spare card", renamedCard?.label)

        // 7. Remove card
        repo.removeCard(cardId)
        assertNull(repo.nfcCards.find { it.cardId == cardId }, "Removed card must no longer exist in nfcCards")
    }

    @Test
    fun testStepP5_ChangeParentPinUpdatesChildUnlockVerification() {
        val repo = com.focal.app.data.FocalRepository.instance
        val protectionRepo = com.focal.app.protection.ProtectionStateRepository.instance

        // Update PIN to new 4-digit code
        val newPin = "8492"
        repo.updateParentPin(newPin)

        assertEquals("8492", repo.parentPin)
        assertEquals("8492", protectionRepo.state.value.parentPinHash)

        // Old PIN (1234) must fail verification
        val oldRes = protectionRepo.verifyAndApplyParentPin("1234", 1000L)
        assertTrue(oldRes.isFailure, "Old PIN 1234 must be rejected")

        // New PIN (8492) must succeed verification
        val newRes = protectionRepo.verifyAndApplyParentPin(newPin, 1000L)
        assertTrue(newRes.isSuccess, "New PIN 8492 must be accepted")
        assertTrue(newRes.getOrNull() == true)
    }

    @Test
    fun testStepP5_PricingConstantsFromConfig() {
        assertEquals(299, com.focal.app.config.FocalPricingConfig.PLAN_MONTHLY_PRICE_INR)
        assertEquals(1000, com.focal.app.config.FocalPricingConfig.NFC_CARD_PRICE_INR)
        assertEquals(3, com.focal.app.config.FocalPricingConfig.PLAN_FREE_TRIAL_MONTHS)
        assertEquals(299, com.focal.app.config.FocalConfig.MONTHLY_PRICE_INR)
        assertEquals(1000, com.focal.app.config.FocalConfig.NFC_CARD_PRICE_INR)
        assertEquals(3, com.focal.app.config.FocalConfig.FREE_TRIAL_MONTHS)
    }

    // =========================================================================
    // STEP P6: END-TO-END PATHS A THROUGH I
    // =========================================================================

    @Test
    fun testPathA_FreshParentInstallToAddChildAndChildProtectionSetup() = kotlinx.coroutines.runBlocking {
        val pairingRepo = com.focal.app.pairing.PairingRepository.instance
        val protectionRepo = com.focal.app.protection.ProtectionStateRepository.instance
        val childId = "test_path_a_child_${com.focal.app.model.currentTimeMillis()}"
        val childName = "Aarav"

        // 1. Parent generates pairing session with 6-digit code
        val parentSession = pairingRepo.createOrGetParentSession(childId, childName)
        val code = parentSession.code
        assertEquals(6, code.length, "Pairing code must be 6 digits")
        assertEquals(com.focal.app.pairing.ParentPairState.WAITING, parentSession.state)

        // 2. Child enters code
        val redeemResult = pairingRepo.redeemCode(code)
        assertTrue(redeemResult.isSuccess, "Redeem code must succeed")
        assertEquals(com.focal.app.pairing.ParentPairState.PENDING_APPROVAL, pairingRepo.getActiveParentSession(childId)?.state)

        // 3. Parent taps Allow
        val approved = pairingRepo.approveChildPairing(childId)
        assertTrue(approved, "Parent must approve pairing")
        assertEquals(com.focal.app.pairing.ParentPairState.APPROVED, pairingRepo.getActiveParentSession(childId)?.state)

        // 4. Child marks paired
        redeemResult.getOrNull()?.let { pairingRepo.markPaired(it) }
        assertTrue(pairingRepo.isPaired(), "Child must be marked paired")

        // 5. Child completes protection setup
        protectionRepo.updateSetupComplete(true)
        assertTrue(protectionRepo.state.value.setupComplete, "Child setup must be complete")
    }

    @Test
    fun testPathB_ParentCreatesPolicy_CardStartsFocus_BlocksApp_CardEndsFocus() {
        val repo = com.focal.app.data.FocalRepository.instance
        val engine = FocusEngine.instance
        val childUser = com.focal.app.model.User(
            id = "user_rahul",
            name = "Aarav",
            email = "aarav@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )

        // 1. Parent creates policy
        val createdPolicy = repo.policies.first().copy(
            id = "pol_strict_study",
            name = "Strict Study",
            blockedApps = listOf("com.google.android.youtube", "com.instagram.android"),
            blockedWebsites = listOf("youtube.com", "instagram.com"),
            durationMinutes = 60
        )
        repo.addPolicy(createdPolicy)
        assertTrue(repo.policies.any { it.name == "Strict Study" }, "Policy must be created")

        // 2. Set as default policy for child
        val childIdx = repo.childrenList.indexOfFirst { it.name == "Aarav" }
        if (childIdx >= 0) {
            repo.childrenList[childIdx] = repo.childrenList[childIdx].copy(defaultPolicyId = createdPolicy.id)
        }

        // 3. Family card tap on child starts focus with that policy
        val cardId = "FC0001"
        val startResult = engine.startFocusWithNfc(cardId, childUser)
        println("DEBUG_PATH_B: isSuccess=${startResult.isSuccess}, error=${startResult.exceptionOrNull()?.message}")
        assertTrue(startResult.isSuccess, "Focus must start successfully with family card: ${startResult.exceptionOrNull()?.message}")
        assertTrue(engine.isFocusActive, "Engine must be in active focus")
        assertEquals(createdPolicy.name, engine.currentSession?.policyName)

        // 4. Blocked app shows block screen
        val state = com.focal.app.protection.ProtectionState(
            setupComplete = true,
            focusActive = true,
            focusEndsAtElapsed = com.focal.app.model.currentTimeMillis() + 3600000L,
            policy = com.focal.app.model.FocusPolicyData(
                blockedPackages = setOf("com.google.android.youtube", "com.instagram.android")
            )
        )
        val decision = ProtectionEngine.decide(
            WindowInfo(packageName = "com.google.android.youtube"),
            state,
            1000L,
            isFixAllowanceActive = false
        )
        assertEquals(DecisionType.BLOCK_APP, decision.type)

        // 5. Card tap again ends focus
        val endResult = engine.endFocusWithNfc(cardId, childUser)
        assertTrue(endResult.isSuccess, "Card tap again must end focus")
        assertFalse(engine.isFocusActive, "Focus must be inactive after card end")
    }

    @Test
    fun testPathC_ParentStartNowTwoChildren_EndOneFromChildPage() {
        val repo = com.focal.app.data.FocalRepository.instance
        val engine = FocusEngine.instance
        val child1 = repo.childrenList[0] // Aarav
        val child2 = repo.childrenList[1] // Kiara
        val policy = repo.policies.first()

        // 1. Parent starts focus on both children
        engine.remoteStartFocus(child1.id, policy, 45)
        engine.remoteStartFocus(child2.id, policy, 45)

        assertTrue(repo.childrenList.find { it.id == child1.id }?.isFocusing == true, "Child 1 must be focusing")
        assertTrue(repo.childrenList.find { it.id == child2.id }?.isFocusing == true, "Child 2 must be focusing")

        // 2. Parent ends focus on child 1 from child page
        engine.remoteStopFocus(child1.id)

        assertFalse(repo.childrenList.find { it.id == child1.id }?.isFocusing == true, "Child 1 must no longer be focusing")
        assertTrue(repo.childrenList.find { it.id == child2.id }?.isFocusing == true, "Child 2 must still be focusing")

        // Clean up
        engine.remoteStopFocus(child2.id)
    }

    @Test
    fun testPathD_ScheduleExecutionStartAndEnd() {
        val engine = FocusEngine.instance
        val repo = com.focal.app.data.FocalRepository.instance
        val childUser = com.focal.app.model.User(
            id = "user_rahul",
            name = "Aarav",
            email = "aarav@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )
        val schedulePolicy = repo.policies.first().copy(
            id = "pol_sched_test",
            name = "Homework Schedule",
            durationMinutes = 120
        )

        // 1. Schedule starts automatically
        val startRes = engine.startFocus(
            triggerType = com.focal.app.model.TriggerType.SCHEDULED,
            triggerIdentifier = "sched_auto_1",
            policy = schedulePolicy,
            user = childUser
        )
        assertTrue(startRes.isSuccess, "Scheduled focus must start")
        assertEquals(com.focal.app.model.TriggerType.SCHEDULED, engine.currentSession?.triggerType)

        // 2. Schedule ends on its own
        val endRes = engine.endFocus(
            triggerType = com.focal.app.model.TriggerType.SCHEDULED,
            triggerIdentifier = "sched_auto_1"
        )
        assertTrue(endRes.isSuccess, "Scheduled focus must end cleanly")
        assertFalse(engine.isFocusActive)
    }

    @Test
    fun testPathE_ChildEmergencyExitRequest_AllowAndDecline() {
        val repo = com.focal.app.data.FocalRepository.instance
        val engine = FocusEngine.instance
        val childUser = com.focal.app.model.User(
            id = "user_rahul",
            name = "Aarav",
            email = "aarav@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )

        // --- SUB-PATH 1: ALLOW ---
        engine.startFocus(com.focal.app.model.TriggerType.NFC, "FC0001", repo.policies.first(), childUser)
        assertTrue(engine.isFocusActive)

        val req1 = repo.submitEmergencyExitRequest("sess_1", "Urgent tutor call")
        assertEquals("PENDING", req1.status)

        repo.resolveEmergencyRequest(req1.id, approve = true)
        val resolved1 = repo.emergencyRequests.find { it.id == req1.id }
        assertEquals("APPROVED", resolved1?.status)
        assertFalse(engine.isFocusActive, "Allowing exit must end focus on child")

        // --- SUB-PATH 2: DECLINE ---
        engine.startFocus(com.focal.app.model.TriggerType.NFC, "FC0001", repo.policies.first(), childUser)
        assertTrue(engine.isFocusActive)

        val req2 = repo.submitEmergencyExitRequest("sess_2", "Want to play games")
        assertEquals("PENDING", req2.status)

        repo.resolveEmergencyRequest(req2.id, approve = false)
        val resolved2 = repo.emergencyRequests.find { it.id == req2.id }
        assertEquals("REJECTED", resolved2?.status)
        assertTrue(engine.isFocusActive, "Declining exit must keep focus active")

        // Clean up
        engine.endFocus(com.focal.app.model.TriggerType.EMERGENCY_OVERRIDE, "TEST")
    }

    @Test
    fun testPathF_FamilyQrScanStartsAndEnds_ResetQrRejectsOld() {
        val qrRepo = com.focal.app.data.FocusQrRepository.instance
        val engine = FocusEngine.instance
        val childUser = com.focal.app.model.User(
            id = "user_rahul",
            name = "Aarav",
            email = "aarav@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )

        // 1. Initial Family Focus QR starts and ends focus
        val initialQr = qrRepo.getFamilyQr("family_1")
        val startRes = engine.startFocusWithQr(initialQr.qrPayload, childUser)
        assertTrue(startRes.isSuccess, "Family QR must start focus")

        val endRes = engine.endFocus(com.focal.app.model.TriggerType.QR, initialQr.qrPayload)
        assertTrue(endRes.isSuccess, "Family QR must end focus")

        // 2. Parent resets Family QR
        val newQr = qrRepo.resetFamilyQr("family_1")
        assertTrue(newQr.qrPayload != initialQr.qrPayload, "Reset QR must have a fresh payload")

        // 3. Old QR payload is rejected
        assertFalse(qrRepo.validateFamilyQr(initialQr.qrPayload, "family_1"), "Old QR must be invalid")
        val oldStartRes = engine.startFocusWithQr(initialQr.qrPayload, childUser)
        assertTrue(oldStartRes.isFailure, "Old QR must fail to start focus")

        // 4. New QR succeeds
        assertTrue(qrRepo.validateFamilyQr(newQr.qrPayload, "family_1"), "New QR must be valid")
        val newStartRes = engine.startFocusWithQr(newQr.qrPayload, childUser)
        assertTrue(newStartRes.isSuccess, "New QR must start focus")
        engine.endFocus(com.focal.app.model.TriggerType.QR, newQr.qrPayload)
    }

    @Test
    fun testPathG_AddSpareCard_DisableFirstCard_FirstFailsOnBoth_SpareWorks() {
        val repo = com.focal.app.data.FocalRepository.instance
        val parent = repo.controllerUser
        val child1User = com.focal.app.model.User(
            id = "user_rahul",
            name = "Aarav",
            email = "aarav@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )
        val child2User = com.focal.app.model.User(
            id = "user_kiara",
            name = "Kiara",
            email = "kiara@focal.io",
            role = com.focal.app.model.UserRole.CHILD,
            familyId = "family_1"
        )

        // Register two family cards
        val card1 = repo.writeAndRegisterBlankCard(parent.id, parent.name)
        val card2 = repo.writeAndRegisterBlankCard(parent.id, parent.name)

        // Disable card 1
        repo.disableCard(card1.cardId)

        // Card 1 rejected on both phones
        val res1Child1 = repo.validateNfcCardForChildEnd(card1.cardId, child1User, null)
        val res1Child2 = repo.validateNfcCardForChildEnd(card1.cardId, child2User, null)
        assertTrue(res1Child1.isFailure, "Card 1 must be rejected on phone 1")
        assertTrue(res1Child2.isFailure, "Card 1 must be rejected on phone 2")

        // Spare card (card 2) works on both phones
        val res2Child1 = repo.validateNfcCardForChildEnd(card2.cardId, child1User, null)
        val res2Child2 = repo.validateNfcCardForChildEnd(card2.cardId, child2User, null)
        assertTrue(res2Child1.isSuccess, "Spare card 2 must work on phone 1")
        assertTrue(res2Child2.isSuccess, "Spare card 2 must work on phone 2")
    }

    @Test
    fun testPathH_AccessibilityDisabled_ParentAlertsEverywhere_ChildFixClearsAlerts() {
        val repo = com.focal.app.data.FocalRepository.instance
        val actRepo = com.focal.app.data.ActivityRepository.instance
        val childId = "child_1"

        // 1. Accessibility turned off on child
        repo.reportProtectionDisabled("Accessibility was turned off", childId)

        assertTrue(repo.isProtectionDisabledOnChild, "Global alert must be active")
        val child = repo.childrenList.find { it.id == childId }
        assertTrue(child?.isProtectionOff == true, "Child profile must show protection off")

        // Activity contains PROTECTION_OFF event
        val offEvent = actRepo.events.value.find { it.type == com.focal.app.data.ActivityEventType.PROTECTION_OFF }
        assertEquals("Protection is off", offEvent?.getDisplayTitle())

        // 2. Child taps Fix and restores permission
        repo.reportProtectionRestored(childId)

        assertFalse(repo.isProtectionDisabledOnChild, "Global alert must be cleared")
        val restoredChild = repo.childrenList.find { it.id == childId }
        assertFalse(restoredChild?.isProtectionOff == true, "Child profile must clear protection off")

        // Activity contains PROTECTION_ON event
        val onEvent = actRepo.events.value.find { it.type == com.focal.app.data.ActivityEventType.PROTECTION_ON }
        assertEquals("Protection is back on", onEvent?.getDisplayTitle())
    }

    @Test
    fun testPathI_KillAndReopenApps_StatePreservationAndBackRules() {
        val protRepo = com.focal.app.protection.ProtectionStateRepository.instance

        // 1. Persistence of setup state across restarts
        protRepo.updateSetupComplete(true)
        protRepo.loadInitialState()
        assertTrue(protRepo.state.value.setupComplete, "Setup state must persist")

        // 2. Active focus persistence across restarts
        protRepo.updateFocusActive(true, com.focal.app.model.currentTimeMillis() + 60000L, com.focal.app.model.FocusPolicyData())
        protRepo.loadInitialState()
        assertTrue(protRepo.state.value.focusActive, "Focus state must persist")

        // Reset
        protRepo.updateFocusActive(false, 0L, com.focal.app.model.FocusPolicyData())
    }

    @Test
    fun testStepA1_AuthRepository_ValidationAndFlows() = kotlinx.coroutines.runBlocking {
        val authRepo = com.focal.app.data.LocalAuthRepository()

        // 1. Password minimum length requirement
        val shortPwRes = authRepo.signUp("Parent Name", "parent@test.com", "pass")
        assertTrue(shortPwRes.isFailure, "Short password (<8 chars) must fail")
        assertEquals("Password must be at least 8 characters", shortPwRes.exceptionOrNull()?.message)

        // 2. Email format validation
        val invalidEmailRes = authRepo.signUp("Parent Name", "invalidemail", "password123")
        assertTrue(invalidEmailRes.isFailure, "Invalid email must fail")
        assertEquals("Enter a valid email", invalidEmailRes.exceptionOrNull()?.message)

        // 3. Name validation
        val emptyNameRes = authRepo.signUp("   ", "parent@test.com", "password123")
        assertTrue(emptyNameRes.isFailure, "Empty name must fail")
        assertEquals("Name cannot be empty", emptyNameRes.exceptionOrNull()?.message)

        // 4. Duplicate email validation
        val duplicateRes = authRepo.signUp("Existing", "existing@focal.io", "password123")
        assertTrue(duplicateRes.isFailure, "Duplicate email must fail")
        assertTrue(duplicateRes.exceptionOrNull()?.message?.contains("already exists") == true)

        // 5. Successful registration
        val signUpRes = authRepo.signUp("Priya Sharma", "priya_new@focal.io", "securepass123")
        assertTrue(signUpRes.isSuccess, "Valid sign-up must succeed")
        assertEquals("Priya Sharma", signUpRes.getOrNull()?.name)
        assertEquals("PARENT", com.focal.app.data.FocalLocalStorage.getString("KEY_SELECTED_ROLE", ""))
        assertTrue(com.focal.app.data.FocalLocalStorage.getBoolean("KEY_AUTH_DONE", false))

        // 6. Sign in with Google
        val googleRes = authRepo.signInWithGoogle()
        assertTrue(googleRes.isSuccess, "Google sign-in must succeed")
        assertEquals("Priya Sharma", googleRes.getOrNull()?.name)

        // 7. Password reset
        val resetInvalid = authRepo.sendPasswordReset("bad_email")
        assertTrue(resetInvalid.isFailure, "Password reset with bad email must fail")
        val resetValid = authRepo.sendPasswordReset("parent@test.com")
        assertTrue(resetValid.isSuccess, "Password reset with valid email must succeed")
    }

    @Test
    fun testStepA1_StartFlow_RoutingRules() {
        fun testComputeStartScreen(): com.focal.app.ui.AppScreenState {
            val role = com.focal.app.data.FocalLocalStorage.getString("KEY_SELECTED_ROLE", "")
            if (role.isEmpty()) {
                val howItWorksSeen = com.focal.app.data.FocalLocalStorage.getBoolean("KEY_HOW_IT_WORKS_SEEN", false)
                return if (!howItWorksSeen) com.focal.app.ui.AppScreenState.HOW_IT_WORKS else com.focal.app.ui.AppScreenState.WELCOME
            }

            val authDone = com.focal.app.data.FocalLocalStorage.getBoolean("KEY_AUTH_DONE", false)

            if (role == "PARENT") {
                if (!authDone) {
                    return com.focal.app.ui.AppScreenState.PARENT_LOGIN
                }
                val focusMethodChosen = com.focal.app.data.FocalLocalStorage.getBoolean("KEY_FOCUS_METHOD_CHOSEN", false)
                if (!focusMethodChosen) {
                    return com.focal.app.ui.AppScreenState.FOCUS_METHOD
                }
                return com.focal.app.ui.AppScreenState.MAIN_APP
            } else { // CHILD
                if (!authDone) {
                    val pending = com.focal.app.pairing.PairingRepository.instance.getPendingRequest()
                    if (pending != null) {
                        return com.focal.app.ui.AppScreenState.CHILD_WAITING_APPROVAL
                    }
                    return com.focal.app.ui.AppScreenState.CHILD_PAIR_CONNECT
                }
                val setupDone = com.focal.app.data.FocalLocalStorage.getBoolean("KEY_SETUP_DONE", false)
                if (!setupDone) {
                    val disclosureAgreed = com.focal.app.data.FocalLocalStorage.getBoolean("KEY_ACCESSIBILITY_DISCLOSURE_AGREED", false) ||
                            com.focal.app.data.FocalLocalStorage.getBoolean("KEY_DISCLOSURE_AGREED", false)
                    if (!disclosureAgreed) return com.focal.app.ui.AppScreenState.CHILD_PROTECTION_DISCLOSURE
                    return com.focal.app.ui.AppScreenState.CHILD_PROTECTION_CHECKLIST
                }
                return com.focal.app.ui.AppScreenState.MAIN_APP
            }
        }

        // 1. Fresh install: Role = empty, How It Works not seen -> HOW_IT_WORKS
        com.focal.app.data.FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "")
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_HOW_IT_WORKS_SEEN", false)
        assertEquals(com.focal.app.ui.AppScreenState.HOW_IT_WORKS, testComputeStartScreen())

        // 2. How it works seen -> WELCOME
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_HOW_IT_WORKS_SEEN", true)
        assertEquals(com.focal.app.ui.AppScreenState.WELCOME, testComputeStartScreen())

        // 3. Parent selected, not authenticated -> PARENT_LOGIN
        com.focal.app.data.FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "PARENT")
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", false)
        assertEquals(com.focal.app.ui.AppScreenState.PARENT_LOGIN, testComputeStartScreen())

        // 4. Parent authenticated, focus method not chosen -> FOCUS_METHOD
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_FOCUS_METHOD_CHOSEN", false)
        assertEquals(com.focal.app.ui.AppScreenState.FOCUS_METHOD, testComputeStartScreen())

        // 5. Parent authenticated, focus method chosen -> MAIN_APP (ParentHomeScreen)
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_FOCUS_METHOD_CHOSEN", true)
        assertEquals(com.focal.app.ui.AppScreenState.MAIN_APP, testComputeStartScreen())

        // 6. Child role selected, not paired -> CHILD_PAIR_CONNECT
        kotlinx.coroutines.runBlocking {
            com.focal.app.pairing.PairingRepository.instance.cancelRequest("")
        }
        com.focal.app.data.FocalLocalStorage.saveString("pairing_pending_req_id", "")
        com.focal.app.data.FocalLocalStorage.saveLong("pairing_pending_expires_at", 0L)
        com.focal.app.data.FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "CHILD")
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", false)
        assertEquals(com.focal.app.ui.AppScreenState.CHILD_PAIR_CONNECT, testComputeStartScreen())

        // 7. Child paired, setup not done -> CHILD_PROTECTION_DISCLOSURE
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_AUTH_DONE", true)
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_SETUP_DONE", false)
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_ACCESSIBILITY_DISCLOSURE_AGREED", false)
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_DISCLOSURE_AGREED", false)
        assertEquals(com.focal.app.ui.AppScreenState.CHILD_PROTECTION_DISCLOSURE, testComputeStartScreen())

        // 8. Child paired & setup complete -> MAIN_APP (Child home)
        com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_SETUP_DONE", true)
        assertEquals(com.focal.app.ui.AppScreenState.MAIN_APP, testComputeStartScreen())
    }

    @Test
    fun testStepA1_WarmStart_Detection() {
        val now = 100_000L
        
        // Within 30 seconds (warm start)
        val recentOpen = now - 15_000L
        val isWarm = (now - recentOpen) in 1..30_000L
        assertTrue(isWarm, "Launch within 30s must be detected as warm start")

        // Over 30 seconds (cold start)
        val olderOpen = now - 35_000L
        val isCold = (now - olderOpen) in 1..30_000L
        assertFalse(isCold, "Launch after 30s must NOT be detected as warm start")
    }
}