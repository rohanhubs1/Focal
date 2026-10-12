package com.focal.app.protection

import com.focal.app.model.FocusPolicyData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtectionEngineTest {

    private val baseState = ProtectionState(
        setupComplete = true,
        focusActive = true,
        focusEndsAtElapsed = 100000L,
        policy = FocusPolicyData(
            blockedApps = listOf("YouTube", "Instagram"),
            blockedPackages = setOf("com.google.android.youtube", "com.instagram.android"),
            blockedWebsites = listOf("youtube.com", "instagram.com")
        ),
        parentUnlockUntilElapsed = 0L,
        lockSettingsMode = LockSettingsMode.DURING_FOCUS
    )

    @Test
    fun test1_ClaudeInFocusAllowed() {
        val claude = WindowInfo(packageName = "com.anthropic.claude", fullText = "Claude AI Chat")
        val res = ProtectionEngine.decide(claude, baseState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('i', res.ruleLetter)
    }

    @Test
    fun test2_WhatsAppInFocusAllowed() {
        val whatsapp = WindowInfo(packageName = "com.whatsapp", fullText = "Chats")
        val res = ProtectionEngine.decide(whatsapp, baseState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('i', res.ruleLetter)
    }

    @Test
    fun test3_YouTubeInFocusBlocked() {
        val yt = WindowInfo(packageName = "com.google.android.youtube", fullText = "Home")
        val res = ProtectionEngine.decide(yt, baseState, 1000L)
        assertEquals(DecisionType.BLOCK_APP, res.type)
        assertEquals('g', res.ruleLetter)
    }

    @Test
    fun test4_YouTubeOutsideFocusAllowed() {
        val idleState = baseState.copy(focusActive = false)
        val yt = WindowInfo(packageName = "com.google.android.youtube", fullText = "Home")
        val res = ProtectionEngine.decide(yt, idleState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('e', res.ruleLetter)
    }

    @Test
    fun test5_ChromeYouTubeInFocusBlocked() {
        val chromeYt = WindowInfo(packageName = "com.android.chrome", urlText = "https://m.youtube.com/watch?v=123")
        val res = ProtectionEngine.decide(chromeYt, baseState, 1000L)
        assertEquals(DecisionType.BLOCK_WEBSITE, res.type)
        assertEquals('h', res.ruleLetter)
        assertTrue(res is ProtectionDecision.BlockWebsite)
        assertEquals("youtube.com", (res as ProtectionDecision.BlockWebsite).domain)
    }

    @Test
    fun test6_ChromeGoogleInFocusAllowed() {
        val chromeGoogle = WindowInfo(packageName = "com.android.chrome", urlText = "https://google.com/search?q=kotlin")
        val res = ProtectionEngine.decide(chromeGoogle, baseState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('i', res.ruleLetter)
    }

    @Test
    fun test7_SettingsInFocusLocked() {
        val settings = WindowInfo(packageName = "com.android.settings")
        val res = ProtectionEngine.decide(settings, baseState, 1000L)
        assertEquals(DecisionType.LOCK_SETTINGS, res.type)
        assertEquals('f', res.ruleLetter)
    }

    @Test
    fun test8_SettingsOutsideFocusDuringFocusModeAllowed() {
        val idleState = baseState.copy(focusActive = false, lockSettingsMode = LockSettingsMode.DURING_FOCUS)
        val settings = WindowInfo(packageName = "com.android.settings")
        val res = ProtectionEngine.decide(settings, idleState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('e', res.ruleLetter)
    }

    @Test
    fun test9_SettingsOutsideFocusAlwaysModeLocked() {
        val idleStateAlways = baseState.copy(focusActive = false, lockSettingsMode = LockSettingsMode.ALWAYS)
        val settings = WindowInfo(packageName = "com.android.settings")
        val res = ProtectionEngine.decide(settings, idleStateAlways, 1000L)
        assertEquals(DecisionType.LOCK_SETTINGS, res.type)
        assertEquals('f', res.ruleLetter)
    }

    @Test
    fun test10_FocalUninstallDialogOutsideFocusBlocked() {
        val idleState = baseState.copy(focusActive = false)
        val uninstallDialog = WindowInfo(
            packageName = "com.google.android.packageinstaller",
            fullText = "Do you want to uninstall FOCAL?"
        )
        val res = ProtectionEngine.decide(uninstallDialog, idleState, 1000L)
        assertEquals(DecisionType.BLOCK_UNINSTALL, res.type)
        assertEquals('d', res.ruleLetter)
    }

    @Test
    fun test11_FocalAppInfoOutsideFocusBlocked() {
        val idleState = baseState.copy(focusActive = false)
        val focalAppInfo = WindowInfo(
            packageName = "com.android.settings",
            fullText = "FOCAL App info Force stop Clear data"
        )
        val res = ProtectionEngine.decide(focalAppInfo, idleState, 1000L)
        assertEquals(DecisionType.BLOCK_UNINSTALL, res.type)
        assertEquals('d', res.ruleLetter)
    }

    @Test
    fun test12_OtherAppInfoOutsideFocusAllowed() {
        val idleState = baseState.copy(focusActive = false)
        val otherAppInfo = WindowInfo(
            packageName = "com.android.settings",
            fullText = "Calculator App info Storage Force stop"
        )
        val res = ProtectionEngine.decide(otherAppInfo, idleState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('e', res.ruleLetter)
    }

    @Test
    fun test13_DialerInFocusAllowed() {
        val dialer = WindowInfo(packageName = "com.google.android.dialer")
        val res = ProtectionEngine.decide(dialer, baseState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('a', res.ruleLetter)
    }

    @Test
    fun test14_ParentUnlockActiveAllowsAll() {
        val parentUnlockedState = baseState.copy(parentUnlockUntilElapsed = 5000L)
        val yt = WindowInfo(packageName = "com.google.android.youtube")
        val resYt = ProtectionEngine.decide(yt, parentUnlockedState, 1000L)
        assertEquals(DecisionType.ALLOW, resYt.type)
        assertEquals('c', resYt.ruleLetter)

        val settings = WindowInfo(packageName = "com.android.settings")
        val resSettings = ProtectionEngine.decide(settings, parentUnlockedState, 1000L)
        assertEquals(DecisionType.ALLOW, resSettings.type)
        assertEquals('c', resSettings.ruleLetter)
    }

    @Test
    fun test15_SetupIncompleteAllowsAll() {
        val incompleteState = baseState.copy(setupComplete = false)
        val settings = WindowInfo(packageName = "com.android.settings")
        val res = ProtectionEngine.decide(settings, incompleteState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('b', res.ruleLetter)
    }

    @Test
    fun test16_LauncherHomeScreenAllowed() {
        val oppoLauncher = WindowInfo(packageName = "com.oppo.launcher", fullText = "FOCAL Clock WhatsApp Weather")
        val res = ProtectionEngine.decide(oppoLauncher, baseState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('a', res.ruleLetter)
    }

    @Test
    fun test17_DeviceAdminActivationAllowed() {
        val idleState = baseState.copy(focusActive = false)
        val activationPrompt = WindowInfo(
            packageName = "com.android.settings",
            fullText = "Activate device admin app? FOCAL Cancel Activate"
        )
        val res = ProtectionEngine.decide(activationPrompt, idleState, 1000L)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('a', res.ruleLetter)
    }

    @Test
    fun test18_DeviceAdminDeactivationBlocked() {
        val idleState = baseState.copy(focusActive = false)
        val deactivationPrompt = WindowInfo(
            packageName = "com.android.settings",
            fullText = "FOCAL Device admin Deactivate this device admin app"
        )
        val res = ProtectionEngine.decide(deactivationPrompt, idleState, 1000L)
        assertEquals(DecisionType.BLOCK_UNINSTALL, res.type)
        assertEquals('d', res.ruleLetter)
    }

    @Test
    fun test19_FixButtonAllowanceActiveAllowsBatterySettings() {
        val batteryPage = WindowInfo(
            packageName = "com.android.settings",
            fullText = "FOCAL Battery usage Unrestricted"
        )
        val res = ProtectionEngine.decide(batteryPage, baseState, 1000L, isFixAllowanceActive = true)
        assertEquals(DecisionType.ALLOW, res.type)
        assertEquals('a', res.ruleLetter)
    }

    @Test
    fun test20_FixButtonAllowanceDoesNotAllowFocalUninstall() {
        val focalUninstallPage = WindowInfo(
            packageName = "com.android.settings",
            fullText = "FOCAL App info Force stop Clear data Uninstall"
        )
        val res = ProtectionEngine.decide(focalUninstallPage, baseState, 1000L, isFixAllowanceActive = true)
        assertEquals(DecisionType.BLOCK_UNINSTALL, res.type)
        assertEquals('d', res.ruleLetter)
    }
}
