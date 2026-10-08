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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.*
import com.focal.app.theme.FocalColors
import com.focal.app.theme.FocalTheme
import com.focal.app.ui.admin.AdminDashboardScreen
import com.focal.app.ui.auth.*
import com.focal.app.ui.child.*
import com.focal.app.ui.components.*
import com.focal.app.ui.controller.*

enum class AppScreenState {
    SPLASH,
    ONBOARDING,
    ROLE_SELECTION,
    PARENT_LOGIN,
    PARENT_SIGN_UP,
    PARENT_FORGOT_PASSWORD,
    PARENT_ADD_CHILD,
    PARENT_PAIR_CHILD,
    CHILD_PAIRING,
    PERMISSIONS_SETUP,
    CARD_SETUP,
    MAIN_APP
}

@Composable
fun FocalRootApp(
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    val screenBackStack = remember { mutableStateListOf(AppScreenState.SPLASH) }
    var screenState by remember { mutableStateOf(AppScreenState.SPLASH) }

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

    // Role-based bottom navigation tab index (4 tabs for Parent, 4 tabs for Child)
    var childTabIndex by remember { mutableIntStateOf(0) }
    val childTabHistory = remember { mutableStateListOf(0) }

    var controllerTabIndex by remember { mutableIntStateOf(0) }
    val controllerTabHistory = remember { mutableStateListOf(0) }

    // Selected child for detail management in Parent role
    var selectedChildForManagement by remember { mutableStateOf<ChildProfile?>(null) }
    var createdChildNameForPairing by remember { mutableStateOf("Rahul") }

    fun selectChildTab(index: Int) {
        if (childTabIndex == index) return
        if (index == 0) {
            childTabHistory.clear()
            childTabHistory.add(0)
        } else {
            childTabHistory.remove(index)
            childTabHistory.add(index)
        }
        childTabIndex = index
    }

    fun selectControllerTab(index: Int) {
        if (controllerTabIndex == index && selectedChildForManagement == null) return
        selectedChildForManagement = null
        if (index == 0) {
            controllerTabHistory.clear()
            controllerTabHistory.add(0)
        } else {
            controllerTabHistory.remove(index)
            controllerTabHistory.add(index)
        }
        controllerTabIndex = index
    }

    // Modals & Overlays state
    var showNfcScanner by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showActiveFocusScreen by remember { mutableStateOf(false) }
    var showAddChildModal by remember { mutableStateOf(false) }
    var showCreatePolicyModal by remember { mutableStateOf(false) }
    var showPermissionsModal by remember { mutableStateOf(false) }
    var generatedFocusQrModal by remember { mutableStateOf<Pair<QrToken, String>?>(null) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showComponentGallery by remember { mutableStateOf(false) }
    var showRoleSwitcherModal by remember { mutableStateOf(false) }

    val currentUser = repository.currentUser

    val canGoBack = remember(
        screenState,
        screenBackStack.size,
        showNotificationsModal,
        generatedFocusQrModal,
        showCreatePolicyModal,
        showAddChildModal,
        showPermissionsModal,
        showQrScanner,
        showNfcScanner,
        showActiveFocusScreen,
        selectedChildForManagement,
        currentUser.role,
        childTabHistory.size,
        controllerTabHistory.size,
        showComponentGallery
    ) {
        if (showComponentGallery || showNotificationsModal || generatedFocusQrModal != null ||
            showCreatePolicyModal || showAddChildModal || showPermissionsModal ||
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
            showComponentGallery -> showComponentGallery = false
            showNotificationsModal -> showNotificationsModal = false
            generatedFocusQrModal != null -> generatedFocusQrModal = null
            showCreatePolicyModal -> showCreatePolicyModal = false
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

    FocalBackHandler(enabled = canGoBack, onBack = handleBack)

    FocalTheme {
        if (currentUser.role == UserRole.CHILD && repository.isProtectionDisabledOnChild) {
            Dialog(onDismissRequest = {}) {
                FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StatusBadge(text = "PROTECTION OFF", badgeType = BadgeType.ACTIVE)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Protection is Off — Turn it Back On",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = FocalColors.Danger),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = repository.protectionDisabledReason.ifEmpty { "Required permissions were revoked on your device." },
                            style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Fix System Settings Now",
                            onClick = {
                                if (!PlatformPermissionsHelper.isAccessibilityEnabled()) {
                                    PlatformPermissionsHelper.openAccessibilitySettings()
                                } else if (!PlatformPermissionsHelper.isOverlayEnabled()) {
                                    PlatformPermissionsHelper.openOverlaySettings()
                                } else if (!PlatformPermissionsHelper.isUsageAccessEnabled()) {
                                    PlatformPermissionsHelper.openUsageAccessSettings()
                                } else {
                                    PlatformPermissionsHelper.openNotificationSettings()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else if (showComponentGallery) {
            ComponentGalleryScreen(onBack = { showComponentGallery = false })
        } else {
            Scaffold(
                containerColor = FocalColors.Background,
                contentWindowInsets = WindowInsets.systemBars,
                topBar = {
                    if (screenState == AppScreenState.MAIN_APP) {
                        FocalTopBar(
                            title = when (currentUser.role) {
                                UserRole.CHILD -> when (childTabIndex) {
                                    0 -> "FOCAL"
                                    1 -> "Focus Schedules"
                                    2 -> "Screen Insights"
                                    else -> "Profile"
                                }
                                UserRole.CONTROLLER -> when (controllerTabIndex) {
                                    0 -> "FOCAL Controller"
                                    1 -> "Children Directory"
                                    2 -> "Focus Policies"
                                    else -> "Parent Profile"
                                }
                                UserRole.ADMIN -> "Admin Dashboard"
                            },
                            onBackClick = if (selectedChildForManagement != null) {
                                { selectedChildForManagement = null }
                            } else null
                        )
                    }
                },
                bottomBar = {
                    if (screenState == AppScreenState.MAIN_APP && !showActiveFocusScreen) {
                        when (currentUser.role) {
                            UserRole.CHILD -> {
                                FocalBottomNav(
                                    items = listOf(
                                        FocalNavItem("Home", FocalIcons.Home),
                                        FocalNavItem("Schedules", FocalIcons.Calendar),
                                        FocalNavItem("Insights", FocalIcons.Chart),
                                        FocalNavItem("Profile", FocalIcons.Smartphone)
                                    ),
                                    selectedIndex = childTabIndex,
                                    onSelectTab = { selectChildTab(it) }
                                )
                            }
                            UserRole.CONTROLLER -> {
                                FocalBottomNav(
                                    items = listOf(
                                        FocalNavItem("Home", FocalIcons.Home),
                                        FocalNavItem("Children", FocalIcons.Users),
                                        FocalNavItem("Policies", FocalIcons.Shield),
                                        FocalNavItem("Profile", FocalIcons.Smartphone)
                                    ),
                                    selectedIndex = controllerTabIndex,
                                    onSelectTab = { selectControllerTab(it) }
                                )
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
                                onSplashFinished = { navigateToScreen(AppScreenState.ONBOARDING, clearHistory = true) }
                            )
                        }

                        AppScreenState.ONBOARDING -> {
                            OnboardingFlowScreen(
                                onOnboardingFinished = { navigateToScreen(AppScreenState.ROLE_SELECTION) },
                                onSkip = { navigateToScreen(AppScreenState.ROLE_SELECTION) }
                            )
                        }

                        AppScreenState.ROLE_SELECTION -> {
                            RoleSelectionScreen(
                                onRoleConfirmed = { selectedRole ->
                                    repository.switchUserRole(selectedRole)
                                    if (selectedRole == UserRole.CONTROLLER) {
                                        navigateToScreen(AppScreenState.PARENT_LOGIN)
                                    } else {
                                        navigateToScreen(AppScreenState.CHILD_PAIRING)
                                    }
                                }
                            )
                        }

                        AppScreenState.PARENT_LOGIN -> {
                            ParentLoginScreen(
                                onLoginSuccess = {
                                    navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                                },
                                onNavigateToSignUp = { navigateToScreen(AppScreenState.PARENT_SIGN_UP) },
                                onNavigateToForgotPassword = { navigateToScreen(AppScreenState.PARENT_FORGOT_PASSWORD) },
                                repository = repository
                            )
                        }

                        AppScreenState.PARENT_SIGN_UP -> {
                            ParentSignUpScreen(
                                onSignUpSuccess = {
                                    navigateToScreen(AppScreenState.PARENT_ADD_CHILD)
                                },
                                onNavigateToLogin = { navigateToScreen(AppScreenState.PARENT_LOGIN) },
                                repository = repository
                            )
                        }

                        AppScreenState.PARENT_FORGOT_PASSWORD -> {
                            ParentForgotPasswordScreen(
                                onBackToLogin = { navigateToScreen(AppScreenState.PARENT_LOGIN) }
                            )
                        }

                        AppScreenState.PARENT_ADD_CHILD -> {
                            ParentAddChildScreen(
                                onChildAdded = { name ->
                                    createdChildNameForPairing = name
                                    navigateToScreen(AppScreenState.PARENT_PAIR_CHILD)
                                }
                            )
                        }

                        AppScreenState.PARENT_PAIR_CHILD -> {
                            ParentPairChildScreen(
                                childName = createdChildNameForPairing,
                                onPairingComplete = {
                                    navigateToScreen(AppScreenState.CARD_SETUP)
                                }
                            )
                        }

                        AppScreenState.CHILD_PAIRING -> {
                            ChildPairingScreen(
                                onPairingSuccess = {
                                    navigateToScreen(AppScreenState.PERMISSIONS_SETUP)
                                }
                            )
                        }

                        AppScreenState.PERMISSIONS_SETUP -> {
                            PermissionsSetupScreen(
                                onPermissionsComplete = { navigateToScreen(AppScreenState.CARD_SETUP) },
                                onSkip = { navigateToScreen(AppScreenState.CARD_SETUP) }
                            )
                        }

                        AppScreenState.CARD_SETUP -> {
                            CardSetupScreen(
                                onSetupFinished = { navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true) },
                                onSkip = { navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true) }
                            )
                        }

                        AppScreenState.MAIN_APP -> {
                            when (currentUser.role) {
                                UserRole.CHILD -> {
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
                                            onViewInsights = { selectChildTab(2) },
                                            onOpenPermissions = { showPermissionsModal = true }
                                        )
                                        1 -> SchedulesScreen()
                                        2 -> InsightsScreen()
                                        3 -> ChildProfileScreen(
                                            onLogout = { navigateToScreen(AppScreenState.ROLE_SELECTION, clearHistory = true) },
                                            onOpenPermissions = { showPermissionsModal = true },
                                            onOpenNfcTap = { showNfcScanner = true }
                                        )
                                    }
                                }

                                UserRole.CONTROLLER -> {
                                    if (selectedChildForManagement != null) {
                                        ChildManagementScreen(
                                            child = selectedChildForManagement!!,
                                            onBack = { selectedChildForManagement = null },
                                            onCreateFocus = { showCreatePolicyModal = true },
                                            onGenerateQr = {
                                                val policy = repository.policies.first()
                                                val qr = repository.generateFocusQr(
                                                    childId = selectedChildForManagement!!.id,
                                                    policyId = policy.id,
                                                    policyTitle = "${policy.name} (${policy.durationMinutes / 60} Hours)",
                                                    durationMinutes = policy.durationMinutes
                                                )
                                                generatedFocusQrModal = qr to selectedChildForManagement!!.name
                                            }
                                        )
                                    } else {
                                        when (controllerTabIndex) {
                                            0 -> ControllerDashboardScreen(
                                                onChildSelected = { selectedChildForManagement = it },
                                                onAddChildClick = { showAddChildModal = true },
                                                onCreatePolicyClick = { showCreatePolicyModal = true },
                                                onOpenPermissions = { showPermissionsModal = true }
                                            )
                                            1 -> ChildrenDirectoryScreen(
                                                onChildSelected = { selectedChildForManagement = it },
                                                onAddChildClick = { showAddChildModal = true }
                                            )
                                            2 -> ControllerFocusPoliciesScreen(
                                                onCreatePolicyClick = { showCreatePolicyModal = true },
                                                onGenerateQr = { policy ->
                                                    val child = repository.childrenList.firstOrNull()
                                                    val qr = repository.generateFocusQr(
                                                        childId = child?.id ?: "child_1",
                                                        policyId = policy.id,
                                                        policyTitle = "${policy.name} (${policy.durationMinutes / 60} Hours)",
                                                        durationMinutes = policy.durationMinutes
                                                    )
                                                    generatedFocusQrModal = qr to (child?.name ?: "Child")
                                                }
                                            )
                                            3 -> ControllerProfileScreen(
                                                onLogout = { navigateToScreen(AppScreenState.ROLE_SELECTION, clearHistory = true) },
                                                onOpenPermissions = { showPermissionsModal = true },
                                                onOpenNfcManager = { showNfcScanner = true },
                                                onOpenGenerateQr = { showCreatePolicyModal = true },
                                                onCreatePolicyClick = { showCreatePolicyModal = true }
                                            )
                                        }
                                    }
                                }

                                UserRole.ADMIN -> {
                                    AdminDashboardScreen()
                                }
                            }
                        }
                    }

                    // Completed Focus Summary Modal
                    focusEngine.lastCompletedSummary?.let { session ->
                        FocusSummaryScreen(
                            session = session,
                            onDismiss = {
                                focusEngine.dismissSummary()
                                showActiveFocusScreen = false
                            }
                        )
                    }

                    // Protection Disabled Modal Warning
                    if (!focusEngine.isProtectionActive && (currentUser.role == UserRole.CHILD || focusEngine.sessionState == SessionState.ACTIVE)) {
                        ProtectionDisabledModal(
                            focusEngine = focusEngine,
                            onDismiss = { focusEngine.checkAndUpdatePermissions() }
                        )
                    }

                    // Blocked App Overlay Experience
                    focusEngine.blockedAppOverlayName?.let { blockedApp ->
                        BlockedAppOverlay(
                            appName = blockedApp,
                            onDismiss = { focusEngine.dismissBlockedOverlay() }
                        )
                    }

                    // Active Focus Overlay Screen
                    if (showActiveFocusScreen && focusEngine.currentSession != null) {
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

                    // Error Alert Dialog for Validation Failures
                    var validationErrorMessage by remember { mutableStateOf<String?>(null) }
                    if (validationErrorMessage != null) {
                        ErrorState(
                            title = "Access Error",
                            description = validationErrorMessage!!,
                            onRetry = { validationErrorMessage = null }
                        )
                    }

                    // NFC Scanner Dialog
                    if (showNfcScanner) {
                        val isSessionActive = focusEngine.sessionState == SessionState.ACTIVE
                        NfcScannerModal(
                            title = if (isSessionActive) "Tap Card to End Focus" else "Tap Focus Card to Start",
                            subtitle = if (isSessionActive) "Hold your registered NFC card to unlock apps." else "Hold your card near NFC area to begin restrictions.",
                            onDismiss = { showNfcScanner = false },
                            onCardScanned = { card ->
                                showNfcScanner = false
                                if (isSessionActive) {
                                    val res = focusEngine.endFocusWithNfc(card.cardId, currentUser)
                                    if (res.isFailure) {
                                        validationErrorMessage = res.exceptionOrNull()?.message ?: "Only your paired card FC0001 can unlock this session."
                                    }
                                } else {
                                    val res = focusEngine.startFocusWithNfc(card.cardId, currentUser)
                                    if (res.isSuccess) {
                                        showActiveFocusScreen = true
                                    } else {
                                        validationErrorMessage = res.exceptionOrNull()?.message ?: "This card isn't paired to you."
                                    }
                                }
                            }
                        )
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
                                if (isSessionActive) {
                                    focusEngine.endFocus(TriggerType.QR, qr.token)
                                } else {
                                    val res = focusEngine.startFocusWithQr(qr.token, currentUser)
                                    if (res.isSuccess) {
                                        showActiveFocusScreen = true
                                    } else {
                                        validationErrorMessage = res.exceptionOrNull()?.message ?: "Invalid or expired Focus QR code."
                                    }
                                }
                            }
                        )
                    }

                    // Add Child Modal
                    if (showAddChildModal) {
                        AddChildModal(onDismiss = { showAddChildModal = false })
                    }

                    // Create Policy Modal
                    if (showCreatePolicyModal) {
                        CreateFocusPolicyScreen(onDismiss = { showCreatePolicyModal = false })
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
                    if (com.focal.app.config.FocalConfig.showDevRoleSwitcher) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(top = 8.dp, end = 12.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1F1F1F))
                                    .border(1.dp, Color(0xFF555555), RoundedCornerShape(12.dp))
                                    .clickable { showRoleSwitcherModal = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "DEV",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Role Switcher Modal
                    if (showRoleSwitcherModal) {
                        RoleSwitcherModal(
                            onDismiss = { showRoleSwitcherModal = false },
                            onRoleSelected = { role ->
                                showRoleSwitcherModal = false
                                navigateToScreen(AppScreenState.MAIN_APP, clearHistory = true)
                            }
                        )
                    }
                }
            }
        }
    }
}
