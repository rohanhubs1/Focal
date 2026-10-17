package com.focal.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.*
import com.focal.app.theme.FocalColors
import com.focal.app.theme.FocalTheme
import com.focal.app.ui.admin.AdminDashboardScreen
import com.focal.app.ui.auth.*
import com.focal.app.ui.child.*
import com.focal.app.ui.child.components.ComponentGalleryScreen
import com.focal.app.ui.child.components.FocalSheetTracker
import com.focal.app.ui.child.pairing.*
import com.focal.app.ui.child.protection.*
import com.focal.app.pairing.*
import com.focal.app.ui.components.*
import com.focal.app.ui.controller.*

enum class AppScreenState {
    SPLASH,
    HOW_IT_WORKS,
    WELCOME,
    PARENT_LOGIN,
    PARENT_SIGN_UP,
    FOCUS_METHOD,
    CHILD_PAIR_CONNECT,
    CHILD_SCAN_QR,
    CHILD_WAITING_APPROVAL,
    CHILD_PAIRED_SUCCESS,
    CHILD_PROTECTION_DISCLOSURE,
    CHILD_PROTECTION_CHECKLIST,
    CHILD_SETUP_ALL_SET,
    PROMINENT_DISCLOSURE,
    CHILD_PERMISSIONS,
    PROTECTION_OFF,
    MAIN_APP
}

@Composable
fun FocalRootApp(
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    val screenBackStack = remember { mutableStateListOf(AppScreenState.SPLASH) }
    var screenState by remember { mutableStateOf(AppScreenState.SPLASH) }
    var activePairRequest by remember { mutableStateOf<PairRequest?>(null) }
    var pairErrorNotice by remember { mutableStateOf<String?>(null) }

    fun navigateToScreen(target: AppScreenState, clearHistory: Boolean = false) {
        if (clearHistory) {
            screenBackStack.clear()
            screenBackStack.add(target)
            screenState = target
            return
        }
        if (screenState == target) return
        if (screenBackStack.lastOrNull() == target) {
            screenState = target
            return
        }
        screenBackStack.remove(target)
        screenBackStack.add(target)
        screenState = target
    }

    fun checkAllChildPermissionsOn(): Boolean {
        return PlatformPermissionsHelper.isAccessibilityEnabled() &&
               PlatformPermissionsHelper.isOverlayEnabled() &&
               PlatformPermissionsHelper.isUsageAccessEnabled() &&
               PlatformPermissionsHelper.isNotificationPermissionGranted() &&
               PlatformPermissionsHelper.isBatteryOptimizationDisabled() &&
               PlatformPermissionsHelper.isExactAlarmPermissionGranted() &&
               PlatformPermissionsHelper.isDeviceAdminActive()
    }

    fun computeStartScreen(): AppScreenState {
        val role = FocalLocalStorage.getString("KEY_SELECTED_ROLE", "")
        if (role.isEmpty()) {
            val howItWorksSeen = FocalLocalStorage.getBoolean("KEY_HOW_IT_WORKS_SEEN", false)
            return if (!howItWorksSeen) AppScreenState.HOW_IT_WORKS else AppScreenState.WELCOME
        }

        val authDone = FocalLocalStorage.getBoolean("KEY_AUTH_DONE", false)

        if (role == "PARENT") {
            if (!authDone) {
                return AppScreenState.PARENT_LOGIN
            }
            val focusMethodChosen = FocalLocalStorage.getBoolean("KEY_FOCUS_METHOD_CHOSEN", false) ||
                    FocalLocalStorage.getBoolean("KEY_ONBOARDING_DONE", false)
            if (!focusMethodChosen) {
                return AppScreenState.FOCUS_METHOD
            }
            return AppScreenState.MAIN_APP
        } else { // CHILD
            if (!authDone) {
                val pending = com.focal.app.pairing.PairingRepository.instance.getPendingRequest()
                if (pending != null) {
                    return AppScreenState.CHILD_WAITING_APPROVAL
                }
                return AppScreenState.CHILD_PAIR_CONNECT
            }
            val setupDone = FocalLocalStorage.getBoolean("KEY_SETUP_DONE", false)
            if (!setupDone) {
                val disclosureAgreed = FocalLocalStorage.getBoolean("KEY_ACCESSIBILITY_DISCLOSURE_AGREED", false) ||
                        FocalLocalStorage.getBoolean("KEY_DISCLOSURE_AGREED", false)
                if (!disclosureAgreed) return AppScreenState.CHILD_PROTECTION_DISCLOSURE
                return AppScreenState.CHILD_PROTECTION_CHECKLIST
            }
            return AppScreenState.MAIN_APP
        }
    }

    // Role-based bottom navigation tab index (4 tabs for Parent, 4 tabs for Child)
    var childTabIndex by remember { mutableIntStateOf(0) }
    val childTabHistory = remember { mutableStateListOf(0) }

    var controllerTabIndex by remember { mutableIntStateOf(0) }
    val controllerTabHistory = remember { mutableStateListOf(0) }

    // Selected child for detail management in Parent role
    var selectedChildForManagement by remember { mutableStateOf<ChildProfile?>(null) }
    var createdChildNameForPairing by remember { mutableStateOf("") }

    // Modals & Overlays state
    var showNfcScanner by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showActiveFocusScreen by remember { mutableStateOf(false) }
    var showAddChildModal by remember { mutableStateOf(false) }
    var isAddChildFlowActive by remember { mutableStateOf(false) }
    var connectPhoneChildId by remember { mutableStateOf<String?>(null) }
    var isPolicyEditingActive by remember { mutableStateOf(false) }
    var openNewPolicyCounter by remember { mutableIntStateOf(0) }
    var policyBackTrigger by remember { mutableIntStateOf(0) }
    var isProfileSubScreenActive by remember { mutableStateOf(false) }
    var profileBackTrigger by remember { mutableIntStateOf(0) }
    var showPermissionsModal by remember { mutableStateOf(false) }
    var generatedFocusQrModal by remember { mutableStateOf<Pair<QrToken, String>?>(null) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showComponentGallery by remember { mutableStateOf(false) }
    var showRoleSwitcherModal by remember { mutableStateOf(false) }
    var childSubScreen: com.focal.app.ui.child.ChildSubScreen? by remember { mutableStateOf<com.focal.app.ui.child.ChildSubScreen?>(null) }
    var parentActivityFilter by remember { mutableStateOf<String?>(null) }

    val currentUser = repository.currentUser

    fun selectChildTab(index: Int) {
        childSubScreen = null
        if (index == 0) {
            childTabHistory.clear()
            childTabHistory.add(0)
        } else {
            childTabHistory.clear()
            childTabHistory.add(0)
            childTabHistory.add(index)
        }
        childTabIndex = index
    }

    fun selectControllerTab(index: Int) {
        selectedChildForManagement = null
        isPolicyEditingActive = false
        isAddChildFlowActive = false
        isProfileSubScreenActive = false
        childSubScreen = null
        if (index == 0) {
            controllerTabHistory.clear()
            controllerTabHistory.add(0)
        } else {
            controllerTabHistory.clear()
            controllerTabHistory.add(0)
            controllerTabHistory.add(index)
        }
        controllerTabIndex = index
    }

    fun navigateToTab(index: Int) {
        selectedChildForManagement = null
        isPolicyEditingActive = false
        isAddChildFlowActive = false
        isProfileSubScreenActive = false
        childSubScreen = null
        profileBackTrigger++
        policyBackTrigger++

        if (currentUser.role == UserRole.CHILD) {
            selectChildTab(index)
        } else {
            selectControllerTab(index)
        }
    }

    val canGoBack = remember(
        screenState,
        screenBackStack.size,
        showNotificationsModal,
        generatedFocusQrModal,
        isPolicyEditingActive,
        isAddChildFlowActive,
        isProfileSubScreenActive,
        showAddChildModal,
        showPermissionsModal,
        showQrScanner,
        showNfcScanner,
        showActiveFocusScreen,
        selectedChildForManagement,
        currentUser.role,
        childTabHistory.size,
        controllerTabHistory.size,
        showComponentGallery,
        childSubScreen,
        repository.showParentUnlockScreen
    ) {
        if (childSubScreen != null || repository.showParentUnlockScreen || isPolicyEditingActive || isAddChildFlowActive || isProfileSubScreenActive) {
            true
        } else if (showComponentGallery || showNotificationsModal || generatedFocusQrModal != null ||
            showAddChildModal || showPermissionsModal ||
            showQrScanner || showNfcScanner || showActiveFocusScreen
        ) {
            true
        } else if (screenState == AppScreenState.MAIN_APP) {
            if (selectedChildForManagement != null) {
                true
            } else if (currentUser.role == UserRole.CHILD && childTabHistory.size > 1) {
                true
            } else if (currentUser.role == UserRole.CONTROLLER && controllerTabHistory.size > 1) {
                true
            } else {
                false
            }
        } else {
            screenBackStack.size > 1
        }
    }

    val handleBack: () -> Unit = {
        when {
            childSubScreen != null -> childSubScreen = null
            repository.showParentUnlockScreen -> repository.showParentUnlockScreen = false
            isPolicyEditingActive -> policyBackTrigger++
            isAddChildFlowActive -> {
                isAddChildFlowActive = false
                connectPhoneChildId = null
            }
            isProfileSubScreenActive -> profileBackTrigger++
            showComponentGallery -> showComponentGallery = false
            showNotificationsModal -> showNotificationsModal = false
            generatedFocusQrModal != null -> generatedFocusQrModal = null
            showAddChildModal -> showAddChildModal = false
            showPermissionsModal -> showPermissionsModal = false
            showQrScanner -> showQrScanner = false
            showNfcScanner -> showNfcScanner = false
            showActiveFocusScreen -> showActiveFocusScreen = false
            screenState == AppScreenState.MAIN_APP -> {
                if (selectedChildForManagement != null) {
                    selectedChildForManagement = null
                } else if (currentUser.role == UserRole.CHILD && childTabHistory.size > 1) {
                    childTabHistory.removeAt(childTabHistory.lastIndex)
                    childTabIndex = childTabHistory.last()
                } else if (currentUser.role == UserRole.CONTROLLER && controllerTabHistory.size > 1) {
                    controllerTabHistory.removeAt(controllerTabHistory.lastIndex)
                    controllerTabIndex = controllerTabHistory.last()
                }
            }
            screenBackStack.size > 1 -> {
                screenBackStack.removeAt(screenBackStack.lastIndex)
                screenState = screenBackStack.last()
            }
        }
    }

    // Periodic Protection Monitoring & Heartbeat Telemetry Loop
    LaunchedEffect(Unit) {
        while (true) {
            val isAcc = PlatformPermissionsHelper.isAccessibilityEnabled()
            val isOvl = PlatformPermissionsHelper.isOverlayEnabled()
            val isUsg = PlatformPermissionsHelper.isUsageAccessEnabled()
            val isNot = PlatformPermissionsHelper.isNotificationPermissionGranted()

            val permsMap = mapOf(
                "accessibility" to isAcc,
                "overlay" to isOvl,
                "usage" to isUsg,
                "notifications" to isNot
            )

            // Send child heartbeat to telemetry repository
            repository.processChildHeartbeat("child_1", permsMap, 85)

            // Check server heartbeat timeout (> 30 min) for offline device detection
            repository.checkServerHeartbeatTimeout("child_1")

            kotlinx.coroutines.delay(10000)
        }
    }

    LaunchedEffect(repository.requestNavigateToChildHomeCounter) {
        if (repository.requestNavigateToChildHomeCounter > 0) {
            childSubScreen = null
            repository.showParentUnlockScreen = false
            showNfcScanner = false
            showQrScanner = false
            showActiveFocusScreen = false
            showComponentGallery = false
            showNotificationsModal = false
            generatedFocusQrModal = null
            showAddChildModal = false
            showPermissionsModal = false
            isPolicyEditingActive = false
            isAddChildFlowActive = false
            isProfileSubScreenActive = false
            FocalSheetTracker.reset()
            if (currentUser.role == UserRole.CHILD) {
                if (screenState != AppScreenState.MAIN_APP) {
                    navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                }
                selectChildTab(0)
            }
        }
    }

    LaunchedEffect(Unit) {
        repository.onUnlinkedFromParent = {
            childSubScreen = null
            repository.showParentUnlockScreen = false
            showNfcScanner = false
            showQrScanner = false
            showActiveFocusScreen = false
            navigateToScreen(AppScreenState.WELCOME, clearHistory = true)
        }
    }

    FocalBackHandler(enabled = canGoBack, onBack = handleBack)

    FocalTheme {
        if (showComponentGallery) {
            ComponentGalleryScreen(onBack = { showComponentGallery = false })
        } else {
            Scaffold(
                containerColor = FocalColors.Background,
                contentWindowInsets = if (currentUser.role == UserRole.CHILD || currentUser.role == UserRole.CONTROLLER || currentUser.role == UserRole.ADMIN) WindowInsets(0, 0, 0, 0) else WindowInsets.systemBars,
                topBar = {},
                bottomBar = {
                    if (screenState == AppScreenState.MAIN_APP && (!showActiveFocusScreen || currentUser.role == UserRole.CHILD)) {
                        when (currentUser.role) {
                            UserRole.CHILD -> {
                                if (childSubScreen == null && !repository.showParentUnlockScreen && !showNfcScanner && !showQrScanner && !FocalSheetTracker.isAnySheetOpen && focusEngine.lastCompletedSummary == null) {
                                    com.focal.app.ui.child.components.ChildBottomNav(
                                        selectedTab = com.focal.app.ui.child.components.ChildTab.entries[childTabIndex],
                                        onTabSelected = { navigateToTab(it.ordinal) },
                                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                    )
                                }
                            }
                            UserRole.CONTROLLER -> {
                                if (selectedChildForManagement == null && !isPolicyEditingActive && !isAddChildFlowActive && !isProfileSubScreenActive && !FocalSheetTracker.isAnySheetOpen) {
                                    com.focal.app.ui.components.ParentBottomNav(
                                        selectedTab = com.focal.app.ui.components.ParentTab.entries.getOrElse(controllerTabIndex) { com.focal.app.ui.components.ParentTab.HOME },
                                        onTabSelected = { navigateToTab(it.ordinal) },
                                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                    )
                                }
                            }
                            UserRole.ADMIN -> {}
                        }
                    }
                }
            ) { paddingValues ->
                AnimatedContent(
                    targetState = screenState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }
                ) { targetScreen ->
                    when (targetScreen) {
                        AppScreenState.SPLASH -> {
                            SplashScreen(
                                onSplashFinished = {
                                    val startScreen = computeStartScreen()
                                    navigateToScreen(startScreen, clearHistory = true)
                                }
                            )
                        }

                        AppScreenState.HOW_IT_WORKS -> {
                            HowItWorksScreen(
                                onGetStarted = {
                                    FocalLocalStorage.saveBoolean("KEY_HOW_IT_WORKS_SEEN", true)
                                    navigateToScreen(AppScreenState.WELCOME)
                                },
                                onSkip = {
                                    FocalLocalStorage.saveBoolean("KEY_HOW_IT_WORKS_SEEN", true)
                                    navigateToScreen(AppScreenState.WELCOME)
                                }
                            )
                        }

                        AppScreenState.WELCOME -> {
                            WelcomeScreen(
                                onParentSelected = {
                                    navigateToScreen(AppScreenState.PARENT_SIGN_UP)
                                },
                                onChildSelected = {
                                    repository.switchUserRole(UserRole.CHILD)
                                    navigateToScreen(AppScreenState.CHILD_PAIR_CONNECT)
                                },
                                onNavigateToLogin = {
                                    navigateToScreen(AppScreenState.PARENT_LOGIN)
                                }
                            )
                        }

                        AppScreenState.PARENT_SIGN_UP -> {
                            CreateAccountScreen(
                                onAccountCreated = {
                                    navigateToScreen(AppScreenState.FOCUS_METHOD)
                                },
                                onNavigateToLogin = {
                                    navigateToScreen(AppScreenState.PARENT_LOGIN)
                                },
                                onBack = {
                                    navigateToScreen(AppScreenState.WELCOME)
                                }
                            )
                        }

                        AppScreenState.PARENT_LOGIN -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    val focusMethodChosen = FocalLocalStorage.getBoolean("KEY_FOCUS_METHOD_CHOSEN", false) ||
                                            FocalLocalStorage.getBoolean("KEY_ONBOARDING_DONE", false)
                                    if (!focusMethodChosen) {
                                        navigateToScreen(AppScreenState.FOCUS_METHOD)
                                    } else {
                                        navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                                    }
                                },
                                onNavigateToSignUp = {
                                    navigateToScreen(AppScreenState.PARENT_SIGN_UP)
                                },
                                onNavigateToChildPair = {
                                    repository.switchUserRole(UserRole.CHILD)
                                    navigateToScreen(AppScreenState.CHILD_PAIR_CONNECT)
                                },
                                onBack = {
                                    navigateToScreen(AppScreenState.WELCOME)
                                }
                            )
                        }

                        AppScreenState.FOCUS_METHOD -> {
                            FocusMethodScreen(
                                onNavigateToParentHome = {
                                    navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                                },
                                onOpenFocusTab = {
                                    navigateToTab(2)
                                    navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                                },
                                onCloseToParentHome = {
                                    navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                                }
                            )
                        }

                        AppScreenState.CHILD_PAIR_CONNECT -> {
                            PairScreen(
                                onBack = { navigateToScreen(AppScreenState.WELCOME) },
                                onScanQrClick = { navigateToScreen(AppScreenState.CHILD_SCAN_QR) },
                                onPairSuccess = { req ->
                                    activePairRequest = req
                                    pairErrorNotice = null
                                    if (req.state == com.focal.app.pairing.PairState.APPROVED) {
                                        navigateToScreen(AppScreenState.CHILD_PAIRED_SUCCESS)
                                    } else {
                                        navigateToScreen(AppScreenState.CHILD_WAITING_APPROVAL)
                                    }
                                },
                                initialError = pairErrorNotice
                            )
                        }

                        AppScreenState.CHILD_SCAN_QR -> {
                            ScanParentQrScreen(
                                onBack = { navigateToScreen(AppScreenState.CHILD_PAIR_CONNECT) },
                                onEnterCodeClick = { navigateToScreen(AppScreenState.CHILD_PAIR_CONNECT) },
                                onQrRedeemed = { req ->
                                    activePairRequest = req
                                    pairErrorNotice = null
                                    if (req.state == com.focal.app.pairing.PairState.APPROVED) {
                                        navigateToScreen(AppScreenState.CHILD_PAIRED_SUCCESS)
                                    } else {
                                        navigateToScreen(AppScreenState.CHILD_WAITING_APPROVAL)
                                    }
                                }
                            )
                        }

                        AppScreenState.CHILD_WAITING_APPROVAL -> {
                            val currentReq = activePairRequest ?: com.focal.app.pairing.PairingRepository.instance.getPendingRequest()
                            if (currentReq != null) {
                                WaitingApprovalScreen(
                                    request = currentReq,
                                    onCancel = {
                                        activePairRequest = null
                                        navigateToScreen(AppScreenState.CHILD_PAIR_CONNECT)
                                    },
                                    onApproved = { approvedReq ->
                                        activePairRequest = approvedReq
                                        pairErrorNotice = null
                                        navigateToScreen(AppScreenState.CHILD_PAIRED_SUCCESS)
                                    },
                                    onDeclinedOrExpired = { errorMsg ->
                                        activePairRequest = null
                                        pairErrorNotice = errorMsg
                                        navigateToScreen(AppScreenState.CHILD_PAIR_CONNECT)
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navigateToScreen(AppScreenState.CHILD_PAIR_CONNECT)
                                }
                            }
                        }

                        AppScreenState.CHILD_PAIRED_SUCCESS -> {
                            val currentReq = activePairRequest ?: com.focal.app.pairing.PairRequest(
                                id = "req_done",
                                code = "000000",
                                parentId = "parent_01",
                                parentName = repository.controllerUser.name.ifBlank { "Parent" },
                                parentInitials = if (repository.controllerUser.name.isNotBlank()) com.focal.app.ui.child.components.getAvatarInitials(repository.controllerUser.name) else "P",
                                childId = "child_01",
                                childName = repository.currentUser.name.ifBlank { "Child" },
                                childInitials = if (repository.currentUser.name.isNotBlank()) com.focal.app.ui.child.components.getAvatarInitials(repository.currentUser.name) else "C",
                                childGrade = "",
                                pairedCardId = null,
                                expiresAtMs = 0L,
                                state = com.focal.app.pairing.PairState.APPROVED
                            )
                            PairedSuccessScreen(
                                request = currentReq,
                                onSetupProtection = {
                                    navigateToScreen(AppScreenState.CHILD_PROTECTION_DISCLOSURE)
                                }
                            )
                        }

                        AppScreenState.CHILD_PROTECTION_DISCLOSURE,
                        AppScreenState.PROMINENT_DISCLOSURE -> {
                            AccessibilityDisclosureScreen(
                                onAgreeAndContinue = {
                                    navigateToScreen(AppScreenState.CHILD_PROTECTION_CHECKLIST)
                                    PlatformPermissionsHelper.openAccessibilitySettings()
                                },
                                onNotNow = {
                                    navigateToScreen(AppScreenState.CHILD_PROTECTION_CHECKLIST)
                                }
                            )
                        }

                        AppScreenState.CHILD_PROTECTION_CHECKLIST,
                        AppScreenState.CHILD_PERMISSIONS -> {
                            ProtectionSetupScreen(
                                onAllSet = {
                                    navigateToScreen(AppScreenState.CHILD_SETUP_ALL_SET)
                                },
                                onShowDisclosure = {
                                    navigateToScreen(AppScreenState.CHILD_PROTECTION_DISCLOSURE)
                                },
                                onBack = {
                                    navigateToScreen(AppScreenState.CHILD_PROTECTION_DISCLOSURE)
                                }
                            )
                        }

                        AppScreenState.CHILD_SETUP_ALL_SET -> {
                            SetupCompleteScreen(
                                onGoToHome = {
                                    navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                                }
                            )
                        }

                        AppScreenState.PROTECTION_OFF -> {
                            LaunchedEffect(Unit) {
                                navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                            }
                        }

                        AppScreenState.MAIN_APP -> {
                            when (currentUser.role) {
                                UserRole.CHILD -> {
                                    if (childSubScreen != null) {
                                        when (childSubScreen) {
                                            com.focal.app.ui.child.ChildSubScreen.CONNECTED_PARENT -> com.focal.app.ui.child.ConnectedParentScreen(onBack = { childSubScreen = null })
                                            com.focal.app.ui.child.ChildSubScreen.FOCAL_CARD -> com.focal.app.ui.child.FocalCardScreen(onBack = { childSubScreen = null })
                                            com.focal.app.ui.child.ChildSubScreen.PROTECTION -> com.focal.app.ui.child.ProtectionScreen(onBack = { childSubScreen = null })
                                            com.focal.app.ui.child.ChildSubScreen.NOTIFICATIONS -> com.focal.app.ui.child.NotificationsScreen(onBack = { childSubScreen = null })
                                            com.focal.app.ui.child.ChildSubScreen.HELP -> com.focal.app.ui.child.HelpScreen(onBack = { childSubScreen = null })
                                            com.focal.app.ui.child.ChildSubScreen.PRIVACY -> com.focal.app.ui.child.PrivacyScreen(onBack = { childSubScreen = null })
                                            com.focal.app.ui.child.ChildSubScreen.ABOUT -> com.focal.app.ui.child.AboutScreen(onBack = { childSubScreen = null })
                                            com.focal.app.ui.child.ChildSubScreen.PARENT_UNLOCK -> com.focal.app.ui.child.ParentUnlockScreen(
                                                onClose = { childSubScreen = null },
                                                onUnlockSuccess = { childSubScreen = null },
                                                repository = repository
                                            )
                                            null -> {}
                                        }
                                    } else {
                                        when (childTabIndex) {
                                            0 -> ChildDashboardScreen(
                                                onStartFocusClick = {
                                                    if (currentUser.planType == PlanType.NFC) {
                                                        showNfcScanner = true
                                                    } else {
                                                        showQrScanner = true
                                                    }
                                                },
                                                onNfcTapClick = { showNfcScanner = true },
                                                onQrScanClick = { showQrScanner = true },
                                                onViewActiveSession = { showActiveFocusScreen = true },
                                                onViewInsights = { navigateToTab(2) },
                                                onOpenPermissions = { childSubScreen = com.focal.app.ui.child.ChildSubScreen.PROTECTION }
                                            )
                                            1 -> SchedulesScreen()
                                            2 -> InsightsScreen()
                                            3 -> com.focal.app.ui.child.ChildProfileScreen(
                                                onNavigateSubScreen = { childSubScreen = it },
                                                repository = repository
                                            )
                                        }
                                    }
                                }

                                UserRole.CONTROLLER -> {
                                    if (isAddChildFlowActive) {
                                        com.focal.app.ui.controller.AddChildRootFlow(
                                            initialChildId = connectPhoneChildId,
                                            onDismiss = {
                                                isAddChildFlowActive = false
                                                connectPhoneChildId = null
                                            },
                                            onChildConnected = { child ->
                                                isAddChildFlowActive = false
                                                connectPhoneChildId = null
                                                selectedChildForManagement = child
                                            },
                                            repository = repository,
                                            pairingRepository = com.focal.app.pairing.PairingRepository.instance
                                        )
                                    } else if (selectedChildForManagement != null) {
                                        ChildDetailScreen(
                                            child = selectedChildForManagement!!,
                                            onBack = { selectedChildForManagement = null },
                                            onConnectPhone = {
                                                connectPhoneChildId = selectedChildForManagement!!.id
                                                isAddChildFlowActive = true
                                            },
                                            repository = repository
                                        )
                                    } else {
                                        when (controllerTabIndex) {
                                            0 -> ParentHomeScreen(
                                                onChildSelected = { selectedChildForManagement = it },
                                                onAddChildClick = {
                                                    connectPhoneChildId = null
                                                    isAddChildFlowActive = true
                                                },
                                                onCreatePolicyClick = {
                                                    navigateToTab(1)
                                                    openNewPolicyCounter++
                                                },
                                                onOpenPermissions = { PlatformPermissionsHelper.openNotificationSettings() },
                                                onOpenActivityTab = { filter ->
                                                    parentActivityFilter = filter
                                                    navigateToTab(3)
                                                },
                                                onOpenFocusTab = { navigateToTab(2) },
                                                onStartNowClick = { navigateToTab(2) }
                                            )
                                            1 -> com.focal.app.ui.controller.ParentPoliciesRootFlow(
                                                onEditingStateChanged = { isPolicyEditingActive = it },
                                                triggerNewPolicy = openNewPolicyCounter,
                                                backTrigger = policyBackTrigger,
                                                repository = repository
                                            )
                                            2 -> com.focal.app.ui.controller.ParentFocusTabScreen(
                                                onAddChildClick = {
                                                    connectPhoneChildId = null
                                                    isAddChildFlowActive = true
                                                },
                                                repository = repository
                                            )
                                            3 -> com.focal.app.ui.controller.ParentActivityScreen(
                                                initialFilter = parentActivityFilter,
                                                repository = repository
                                            )
                                            4 -> com.focal.app.ui.controller.ParentProfileRootFlow(
                                                onLogout = { navigateToScreen(AppScreenState.WELCOME, clearHistory = true) },
                                                onSelectChild = { child -> selectedChildForManagement = child },
                                                onAddChild = {
                                                    connectPhoneChildId = null
                                                    isAddChildFlowActive = true
                                                },
                                                onGoToFocusTab = { navigateToTab(2) },
                                                onSubScreenChanged = { isProfileSubScreenActive = it },
                                                backTrigger = profileBackTrigger,
                                                repository = repository
                                            )
                                        }
                                    }
                                }

                                UserRole.ADMIN -> {
                                    if (!com.focal.app.config.FocalConfig.isDemoMode && !com.focal.app.data.AuthRepository.instance.isUserAdmin()) {
                                        LaunchedEffect(Unit) {
                                            val role = com.focal.app.data.FocalLocalStorage.getString("KEY_SELECTED_ROLE", "")
                                            if (role == "PARENT") {
                                                repository.switchUserRole(UserRole.CONTROLLER)
                                            } else if (role == "CHILD") {
                                                repository.switchUserRole(UserRole.CHILD)
                                            } else {
                                                navigateToScreen(AppScreenState.WELCOME, clearHistory = true)
                                            }
                                        }
                                    } else {
                                        AdminDashboardScreen()
                                    }
                                }
                            }
                        }
                    }
                }

                // Navigate Child to Home on completed focus so SessionCompleteScreen is shown cleanly inside ChildDashboardScreen
                LaunchedEffect(focusEngine.lastCompletedSummary) {
                    if (focusEngine.lastCompletedSummary != null && currentUser.role == UserRole.CHILD) {
                        childTabIndex = 0
                        childTabHistory.clear()
                        childTabHistory.add(0)
                        childSubScreen = null
                    }
                }

                // Blocked App Overlay Experience
                focusEngine.blockedAppOverlayName?.let { blockedApp ->
                    BlockedAppOverlay(
                        appName = blockedApp,
                        onDismiss = { focusEngine.dismissBlockedOverlay() }
                    )
                }

                // Active Focus Overlay Screen (Non-child roles or legacy)
                if (showActiveFocusScreen && focusEngine.currentSession != null && currentUser.role != UserRole.CHILD) {
                    ActiveFocusScreen(
                        onEndFocusClick = {
                            if (currentUser.planType == PlanType.NFC) {
                                showNfcScanner = true
                            } else {
                                showQrScanner = true
                            }
                        }
                    )
                }

                // NFC Scanner Dialog / Tap Card Bottom Sheet
                if (showNfcScanner) {
                    val isSessionActive = focusEngine.sessionState == SessionState.ACTIVE
                    if (currentUser.role == UserRole.CHILD) {
                        var tapSheetReaderState by remember {
                            mutableStateOf(
                                if (PlatformPermissionsHelper.isNfcEnabled())
                                    com.focal.app.ui.child.components.ReaderState.LISTENING_USB
                                else
                                    com.focal.app.ui.child.components.ReaderState.NO_READER
                            )
                        }

                        LaunchedEffect(focusEngine.sessionState) {
                            if (isSessionActive && focusEngine.sessionState != SessionState.ACTIVE) {
                                tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.SUCCESS
                                kotlinx.coroutines.delay(600)
                                showNfcScanner = false
                            } else if (!isSessionActive && focusEngine.sessionState == SessionState.ACTIVE) {
                                tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.SUCCESS
                                kotlinx.coroutines.delay(600)
                                showNfcScanner = false
                            }
                        }

                        val coroutineScope = rememberCoroutineScope()

                        com.focal.app.ui.child.components.TapCardBottomSheet(
                            isEndingFocus = isSessionActive,
                            readerState = tapSheetReaderState,
                            onDismiss = { showNfcScanner = false },
                            onCardScanned = {
                                val targetCardId = repository.childrenList.find { it.userId == currentUser.id }?.pairedCardId ?: "FC0001"
                                if (isSessionActive) {
                                    val res = focusEngine.endFocusWithNfc(targetCardId, currentUser)
                                    if (res.isSuccess) {
                                        tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.SUCCESS
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(600)
                                            showNfcScanner = false
                                        }
                                    } else {
                                        tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.WRONG_CARD
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(2000)
                                            if (tapSheetReaderState == com.focal.app.ui.child.components.ReaderState.WRONG_CARD) {
                                                tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.LISTENING_USB
                                            }
                                        }
                                    }
                                } else {
                                    val res = focusEngine.startFocusWithNfc(targetCardId, currentUser)
                                    if (res.isSuccess) {
                                        tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.SUCCESS
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(600)
                                            showNfcScanner = false
                                        }
                                    } else {
                                        tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.WRONG_CARD
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(2000)
                                            if (tapSheetReaderState == com.focal.app.ui.child.components.ReaderState.WRONG_CARD) {
                                                tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.LISTENING_USB
                                            }
                                        }
                                    }
                                }
                            },
                            onTestWrongCard = {
                                tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.WRONG_CARD
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    if (tapSheetReaderState == com.focal.app.ui.child.components.ReaderState.WRONG_CARD) {
                                        tapSheetReaderState = com.focal.app.ui.child.components.ReaderState.LISTENING_USB
                                    }
                                }
                            }
                        )
                    } else {
                        NfcScannerModal(
                            title = if (isSessionActive) "Tap Card to End Focus" else "Tap Focus Card to Start",
                            subtitle = if (isSessionActive) "Hold your registered NFC card to unlock apps." else "Hold your card near NFC area to begin restrictions.",
                            onDismiss = { showNfcScanner = false },
                            onCardScanned = { card ->
                                showNfcScanner = false
                                if (isSessionActive) {
                                    val res = focusEngine.endFocusWithNfc(card.cardId, currentUser)
                                    if (res.isFailure) {
                                        PlatformPermissionsHelper.showToast(res.exceptionOrNull()?.message ?: "This isn't your family's FOCAL card.")
                                    }
                                } else {
                                    val res = focusEngine.startFocusWithNfc(card.cardId, currentUser)
                                    if (res.isSuccess) {
                                        showActiveFocusScreen = true
                                    } else {
                                        PlatformPermissionsHelper.showToast(res.exceptionOrNull()?.message ?: "This isn't your family's FOCAL card.")
                                    }
                                }
                            }
                        )
                    }
                }

                // QR Scanner Dialog
                if (showQrScanner) {
                    val isSessionActive = focusEngine.sessionState == SessionState.ACTIVE
                    QrScannerModal(
                        title = if (isSessionActive) "Scan QR to End Focus" else "Scan Focus QR to Start",
                        subtitle = if (isSessionActive) "Scan QR to verify and remove restrictions." else "Scan Focus QR to start session.",
                        onDismiss = { showQrScanner = false },
                        onQrScanned = { qr ->
                            showQrScanner = false
                            if (qr.token.startsWith("focal://focus", ignoreCase = true) || qr.token.startsWith("FOCAL_FAMILY_QR", ignoreCase = true)) {
                                repository.scanFamilyFocusQr(qr.token) { res ->
                                    res.fold(
                                        onSuccess = { msg ->
                                            PlatformPermissionsHelper.showToast(msg)
                                            if (!isSessionActive) {
                                                showActiveFocusScreen = true
                                            }
                                        },
                                        onFailure = { err ->
                                            PlatformPermissionsHelper.showToast(err.message ?: "QR error")
                                        }
                                    )
                                }
                            } else if (isSessionActive) {
                                focusEngine.endFocus(TriggerType.QR, qr.token)
                            } else {
                                val res = focusEngine.startFocusWithQr(qr.token, currentUser)
                                if (res.isSuccess) {
                                    showActiveFocusScreen = true
                                } else {
                                    PlatformPermissionsHelper.showToast(res.exceptionOrNull()?.message ?: "Invalid or expired Focus QR code.")
                                }
                            }
                        }
                    )
                }


                // Generated Focus QR Modal
                generatedFocusQrModal?.let { (qr, childName) ->
                    QrDisplayModal(
                        qrToken = qr,
                        childName = childName,
                        onDismiss = { generatedFocusQrModal = null }
                    )
                }

                // DEV Floating Pill Button (Debug Mode / Role Switcher)
                val isClientDemo = com.focal.app.data.FocalLocalStorage.getBoolean("KEY_CLIENT_DEMO_MODE", false)
                if (com.focal.app.config.FocalConfig.showDevRoleSwitcher && !isClientDemo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(top = 2.dp, start = 8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Box(
                            modifier = Modifier
                                .alpha(0.6f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1F1F1F))
                                .border(1.dp, Color(0xFF555555), RoundedCornerShape(8.dp))
                                .clickable { showRoleSwitcherModal = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEV",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                } else if (com.focal.app.config.FocalConfig.showDevRoleSwitcher && isClientDemo) {
                    // Discreet invisible touch target at top-left to reopen debug menu if needed
                    Box(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .size(36.dp)
                            .clickable { showRoleSwitcherModal = true }
                    )
                }

                // Role Switcher Modal
                if (showRoleSwitcherModal) {
                    RoleSwitcherModal(
                        onDismiss = { showRoleSwitcherModal = false },
                        onRoleSelected = { role ->
                            showRoleSwitcherModal = false
                            navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                        },
                        onOpenComponentGallery = {
                            showRoleSwitcherModal = false
                            showComponentGallery = true
                        }
                    )
                }

                // Global Parent Unlock Screen Modal
                if (repository.showParentUnlockScreen) {
                    com.focal.app.ui.child.ParentUnlockScreen(
                        onClose = { repository.showParentUnlockScreen = false },
                        onUnlockSuccess = { repository.showParentUnlockScreen = false },
                        repository = repository
                    )
                }
            }
        }
    }
}
