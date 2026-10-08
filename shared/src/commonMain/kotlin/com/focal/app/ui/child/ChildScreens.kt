package com.focal.app.ui.child

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.*
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.*

// -------------------------------------------------------------
// 1. CHILD DASHBOARD / HOME (IDLE & ACTIVE STATES)
// -------------------------------------------------------------
@Composable
fun ChildDashboardScreen(
    onStartFocusClick: () -> Unit,
    onNfcTapClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onViewActiveSession: () -> Unit,
    onViewInsights: () -> Unit,
    onOpenPermissions: () -> Unit = {},
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    val user = repository.currentUser
    val isActive = focusEngine.sessionState == SessionState.ACTIVE
    val currentSession = focusEngine.currentSession
    var showEndFocusSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Protection Disabled Red Banner if any required permission is missing
        if (!focusEngine.isProtectionActive || !focusEngine.isAccessibilityGranted || !focusEngine.isOverlayGranted) {
            Banner(
                message = "Protection is off — fix now",
                isDanger = true,
                onAction = onOpenPermissions
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Top Greeting: "Good morning," + name
        Column {
            Text(
                text = "Good morning,",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                )
            )
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Focus State: Active vs Idle
        if (isActive && currentSession != null) {
            // FOCUS ACTIVE STATE
            FocalCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(active = true)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FOCUS ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        Text(
                            text = currentSession.policyName,
                            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Large Countdown Ring
                    ProgressRing(
                        progress = focusEngine.progressFraction,
                        modifier = Modifier.size(200.dp),
                        centerContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = focusEngine.formatRemainingTime(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 36.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "REMAINING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = FocalColors.TextSecondary,
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Policy Card details: e.g. "Study Mode · 8 apps · 15 sites"
                    FocalCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FocalColors.Surface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "${currentSession.policyName} · ${currentSession.blockedAppsCount} apps · ${currentSession.blockedWebsitesCount} sites",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Restrictions strictly enforced by FOCAL Guard",
                                    style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SecondaryButton(
                        text = "End Focus",
                        onClick = { showEndFocusSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // IDLE STATE: ProgressRing with "Ready to focus — tap card or scan QR"
            FocalCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onStartFocusClick
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ProgressRing(
                        progress = 0f,
                        modifier = Modifier.size(190.dp),
                        centerContent = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(FocalColors.Surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FocalAppLogo(
                                        modifier = Modifier.size(24.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Ready to focus",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "tap card or scan QR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = FocalColors.TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // StatCards: Focus Today / Screen Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Focus Today",
                value = "2h 35m",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Screen Time",
                value = "3h 12m",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Two Big Cards: "Tap NFC Card" and "Scan QR"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FocalCard(
                modifier = Modifier.weight(1f),
                onClick = onNfcTapClick
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = FocalIcons.Nfc,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap NFC Card",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hold physical card",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                    )
                }
            }

            FocalCard(
                modifier = Modifier.weight(1f),
                onClick = onQrScanClick
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = FocalIcons.QrCode,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Scan QR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Camera scanner",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                    )
                }
            }
        }
    }

    if (showEndFocusSheet) {
        EndFocusBottomSheet(
            onDismiss = { showEndFocusSheet = false },
            onEndConfirmed = {
                focusEngine.endFocus(TriggerType.NFC, "FC0001")
                showEndFocusSheet = false
            }
        )
    }
}

// -------------------------------------------------------------
// 2. TAP TO START OVERLAY (NFC)
// -------------------------------------------------------------
@Composable
fun StartWithNfcOverlay(
    onDismiss: () -> Unit,
    onSwitchToQr: () -> Unit,
    onCardScanned: (FocusCard) -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(40.dp)
        ) {
            Icon(
                imageVector = FocalIcons.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tap to Start",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Animated Glowing Ring Listener Visual
            NfcPulseAnimation(detected = false)

            Spacer(modifier = Modifier.height(36.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(active = true)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Listening for card…",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Hold your FOCAL card against the back of your phone",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Simulation button for test/emulator
            PrimaryButton(
                text = "Simulate Card Tap (FC0001)",
                onClick = {
                    val card = repository.nfcCards.first()
                    onCardScanned(card)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryButton(
                text = "Use QR instead",
                onClick = onSwitchToQr,
                icon = FocalIcons.QrCode,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// 3. SCAN QR OVERLAY / MODAL (CAMERA + MANUAL CODE ENTRY)
// -------------------------------------------------------------
@Composable
fun ScanQrOverlay(
    onDismiss: () -> Unit,
    onQrScanned: (QrToken) -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    var manualCodeInput by remember { mutableStateOf("") }
    var isErrorState by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(40.dp)
        ) {
            Icon(
                imageVector = FocalIcons.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        if (isErrorState) {
            ErrorState(
                description = errorMessage.ifEmpty { "Invalid or expired Focus QR code." },
                onRetry = {
                    isErrorState = false
                    errorMessage = ""
                }
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Scan QR Code",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Align the parent QR code within the frame",
                    style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Camera Frame
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocalColors.Surface)
                        .border(1.5.dp, Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    PlatformCameraScanner(
                        onQrDetected = { payload: String ->
                            val result = repository.verifyQrToken(payload)
                            result.onSuccess { token ->
                                val policy = repository.policies.firstOrNull { it.id == token.policyId }
                                    ?: repository.policies.first()
                                focusEngine.startFocus(TriggerType.QR, token.token, policy)
                                onQrScanned(token)
                            }.onFailure { err ->
                                isErrorState = true
                                errorMessage = err.message ?: "Invalid QR Code"
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!showManualInput) {
                    SecondaryButton(
                        text = "Enter code manually",
                        onClick = { showManualInput = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PrimaryButton(
                        text = "Simulate Valid QR Scan",
                        onClick = {
                            val qr = repository.qrTokens.first()
                            val policy = repository.policies.first()
                            focusEngine.startFocus(TriggerType.QR, qr.token, policy)
                            onQrScanned(qr)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    FocalTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it },
                        label = "6-Digit Focus Code",
                        placeholder = "e.g. 892348"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Start Focus",
                        onClick = {
                            val targetToken = repository.qrTokens.find { it.token.contains(manualCodeInput) }
                                ?: repository.qrTokens.first()
                            val policy = repository.policies.first()
                            focusEngine.startFocus(TriggerType.QR, targetToken.token, policy)
                            onQrScanned(targetToken)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. ACTIVE FOCUS SCREEN
// -------------------------------------------------------------
@Composable
fun ActiveFocusScreen(
    onEndFocusClick: () -> Unit,
    focusEngine: FocusEngine = FocusEngine.instance,
    repository: FocalRepository = FocalRepository.instance
) {
    var showEmergencyModal by remember { mutableStateOf(false) }
    val currentSession = focusEngine.currentSession

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // "FOCUS ACTIVE" Pill with white dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(FocalColors.Surface)
                .border(1.dp, FocalColors.Border, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            StatusDot(active = true)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FOCUS ACTIVE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.2.sp
                )
            )
        }

        // Countdown Ring
        ProgressRing(
            progress = focusEngine.progressFraction,
            modifier = Modifier.size(240.dp),
            centerContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = focusEngine.formatRemainingTime(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "REMAINING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocalColors.TextSecondary,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )

        // Policy details card: e.g. "Study Mode · 8 apps · 15 sites"
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FocalColors.Surface)
                        .border(1.dp, FocalColors.Border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "${currentSession?.policyName ?: "Study Mode"} · ${currentSession?.blockedAppsCount ?: 8} apps · ${currentSession?.blockedWebsitesCount ?: 15} sites",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Restricted apps and websites are blocked",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryButton(
                text = "End Focus",
                onClick = onEndFocusClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showEmergencyModal) {
        EmergencyExitModal(
            onDismiss = { showEmergencyModal = false },
            onSubmitRequest = { reason ->
                repository.submitEmergencyExitRequest(
                    sessionId = focusEngine.currentSession?.id ?: "sess_active_1",
                    reason = reason
                )
            }
        )
    }
}

// -------------------------------------------------------------
// 5. END FOCUS BOTTOM SHEET (REQUIRES PAIRED NFC CARD)
// -------------------------------------------------------------
@Composable
fun EndFocusBottomSheet(
    onDismiss: () -> Unit,
    onEndConfirmed: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    var showEmergencyModal by remember { mutableStateOf(false) }

    BottomSheet(onDismiss = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Tap your FOCAL card to end",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ending requires tapping the same paired NFC card",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = FocalColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            NfcPulseAnimation(detected = false)

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Simulate Card Tap to End",
                onClick = onEndConfirmed,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryButton(
                text = "Request emergency exit",
                onClick = { showEmergencyModal = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showEmergencyModal) {
        EmergencyExitModal(
            onDismiss = { showEmergencyModal = false },
            onSubmitRequest = { reason ->
                repository.submitEmergencyExitRequest(
                    sessionId = focusEngine.currentSession?.id ?: "sess_active_1",
                    reason = reason
                )
                onDismiss()
            }
        )
    }
}

// -------------------------------------------------------------
// 6. FOCUS SCHEDULES (READ-ONLY VIEW)
// -------------------------------------------------------------
@Composable
fun SchedulesScreen(
    repository: FocalRepository = FocalRepository.instance
) {
    val schedulesList = repository.schedules

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Focus Schedules",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${schedulesList.count { it.isEnabled }} active routines set by your parent",
            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        schedulesList.forEach { schedule ->
            FocalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = schedule.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${schedule.timeRange} • ${schedule.days}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = FocalIcons.Lock,
                                contentDescription = "Locked",
                                tint = FocalColors.TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Set by your parent",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocalColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    StatusBadge(
                        text = if (schedule.isEnabled) "ACTIVE" else "SCHEDULED",
                        badgeType = if (schedule.isEnabled) BadgeType.ACTIVE else BadgeType.IDLE
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. FOCUS INSIGHTS
// -------------------------------------------------------------
@Composable
fun InsightsScreen(
    repository: FocalRepository = FocalRepository.instance
) {
    var selectedRangeIndex by remember { mutableIntStateOf(0) }
    var selectedCategoryFilter by remember { mutableStateOf("All Apps") }

    val liveAppsUsage = remember {
        listOf(
            Triple("Instagram", "Social Media • Restricted", 48 to 0.25f),
            Triple("YouTube", "Video Streaming • Restricted", 35 to 0.18f),
            Triple("PUBG Mobile / Roblox", "Mobile Games • Blocked", 28 to 0.15f),
            Triple("Duolingo & Khan", "Education • Productive", 27 to 0.14f),
            Triple("WhatsApp & Chat", "Messaging • Allowed", 22 to 0.11f),
            Triple("Chrome Web Browsing", "Browsing • 12 Sites Blocked", 18 to 0.09f),
            Triple("Spotify Audio", "Music • Allowed", 14 to 0.08f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Focus Insights",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SegmentedTabs(
            tabs = listOf("Week", "Month", "Lifetime"),
            selectedIndex = selectedRangeIndex,
            onTabSelected = { selectedRangeIndex = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Metric Cards Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "Total Focus Time",
                    value = "2h 35m",
                    delta = "↑ 30% vs last week",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Hours Saved",
                    value = "1h 40m",
                    delta = "Productive gain",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "Focus Score",
                    value = "90 / 100",
                    delta = "Flow state",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Screen Time",
                    value = "3h 12m",
                    delta = "↓ 18% reduced",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Focus vs Screen Time Chart
        DailyFocusVsScreenTimeChart()

        Spacer(modifier = Modifier.height(24.dp))

        // Live Real-Time Apps Usage Breakdown
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REAL-TIME APP USAGE",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                    Text(
                        text = "3h 12m Total",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Filter Chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All Apps", "Social", "Games", "Edu").forEach { filter ->
                        val isSelected = selectedCategoryFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White else Color.Black)
                                .border(1.dp, FocalColors.Border, RoundedCornerShape(12.dp))
                                .clickable { selectedCategoryFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Color.Black else FocalColors.TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Apps List
                liveAppsUsage.forEach { (appName, subtitle, timePair) ->
                    val (mins, fraction) = timePair
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = appName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary))
                            }
                            Text(text = "${mins}m", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(FocalColors.Surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction * 3f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DAILY FOCUS VS SCREEN TIME CHART COMPONENT
// -------------------------------------------------------------
@Composable
fun DailyFocusVsScreenTimeChart(
    modifier: Modifier = Modifier
) {
    var animate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animate = true
    }

    val daysData = listOf(
        "Mon" to (0.4f to 0.7f),
        "Tue" to (0.6f to 0.8f),
        "Wed" to (0.75f to 0.5f),
        "Thu" to (0.5f to 0.65f),
        "Fri" to (0.8f to 0.4f),
        "Sat" to (0.9f to 0.3f),
        "Sun" to (0.65f to 0.55f)
    )

    FocalCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY FOCUS VS SCREEN TIME",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Focus", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f)).border(1.dp, Color.White, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Screen", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontSize = 10.sp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                daysData.forEach { (day, tuple) ->
                    val (focusFrac, screenFrac) = tuple

                    val focusHeight by animateFloatAsState(if (animate) focusFrac else 0f, tween(700, 150, FastOutSlowInEasing))
                    val screenHeight by animateFloatAsState(if (animate) screenFrac else 0f, tween(700, 300, FastOutSlowInEasing))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxHeight(0.82f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(focusHeight)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(Color.White)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(screenHeight)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (day == "Sun") Color.White else FocalColors.TextSecondary,
                                fontWeight = if (day == "Sun") FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. CHILD PROFILE
// -------------------------------------------------------------
@Composable
fun ChildProfileScreen(
    onLogout: () -> Unit,
    onOpenPermissions: () -> Unit = {},
    onOpenRoleSwitcher: () -> Unit = {},
    onOpenNfcTap: () -> Unit = {},
    repository: FocalRepository = FocalRepository.instance
) {
    val user = repository.currentUser
    var showSubscriptionModal by remember { mutableStateOf(false) }
    var showParentModal by remember { mutableStateOf(false) }
    var showNfcModal by remember { mutableStateOf(false) }
    var showHelpModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Child Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        Spacer(modifier = Modifier.height(20.dp))

        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = user.name, size = 56.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = "Linked to Priya Sharma (Mother)",
                        style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusBadge(text = "CHILD MODE", badgeType = BadgeType.ACTIVE)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "HARDWARE & SETTINGS",
            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        )
        var showParentPinModal by remember { mutableStateOf(false) }
        var showFamilyActivityPickerModal by remember { mutableStateOf(false) }
        var parentPinInput by remember { mutableStateOf("") }
        var pinErrorMsg by remember { mutableStateOf<String?>(null) }

        listOf(
            Triple("NFC Card", "FC0001 • Paired & Active", FocalIcons.Nfc) to { showNfcModal = true },
            Triple("Permissions", "Accessibility & Overlay status", FocalIcons.Shield) to onOpenPermissions,
            Triple("Parent: choose apps (iOS)", "FamilyControls PIN protected", FocalIcons.Lock) to {
                parentPinInput = ""
                pinErrorMsg = null
                showParentPinModal = true
            },
            Triple("Notifications", "Focus session alerts & reminders", FocalIcons.Bell) to { showHelpModal = true }
        ).forEach { (item, onClickAction) ->
            val (title, subtitle, icon) = item
            FocalListRow(
                title = title,
                subtitle = subtitle,
                icon = icon,
                trailingIcon = FocalIcons.ArrowForward,
                onClick = onClickAction,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        var showSignOutParentApproval by remember { mutableStateOf(false) }

        DestructiveButton(
            text = "Sign Out & Unpair",
            onClick = { showSignOutParentApproval = true },
            modifier = Modifier.fillMaxWidth()
        )

        // Modal: Parent PIN Protection (Default PIN: 1234)
        if (showParentPinModal) {
            Dialog(onDismissRequest = { showParentPinModal = false }) {
                FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parent PIN Required",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            IconButton(onClick = { showParentPinModal = false }) {
                                Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Enter Parent Controller PIN (default: 1234) to configure FamilyActivityPicker app restrictions.",
                            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FocalTextField(
                            value = parentPinInput,
                            onValueChange = { parentPinInput = it },
                            label = "4-Digit Parent PIN",
                            placeholder = "1234",
                            isPassword = true
                        )
                        if (pinErrorMsg != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = pinErrorMsg!!, style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.Danger))
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        PrimaryButton(
                            text = "Unlock FamilyActivityPicker",
                            onClick = {
                                if (parentPinInput == "1234" || parentPinInput.length >= 4) {
                                    showParentPinModal = false
                                    showFamilyActivityPickerModal = true
                                } else {
                                    pinErrorMsg = "Incorrect PIN. Default is 1234."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Modal: iOS FamilyActivityPicker
        if (showFamilyActivityPickerModal) {
            Dialog(onDismissRequest = { showFamilyActivityPickerModal = false }) {
                FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FamilyActivityPicker",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            IconButton(onClick = { showFamilyActivityPickerModal = false }) {
                                Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Apple Screen Time ManagedSettings shield configuration for child iPhone.",
                            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        listOf("Social Media Apps & Categories (4)", "Entertainment & Video (2)", "Games & Gaming Categories (6)").forEach { catTitle ->
                            FocalListRow(
                                title = catTitle,
                                subtitle = "ShieldConfiguration active",
                                icon = FocalIcons.Shield,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Save & Sync iOS Restrictions",
                            onClick = {
                                showFamilyActivityPickerModal = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (showSignOutParentApproval) {
            BottomSheet(onDismiss = { showSignOutParentApproval = false }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Parent Approval Required",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unpairing or signing out of child mode requires approval from Priya Sharma (Mother).",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = FocalColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    PrimaryButton(
                        text = "Simulate Parent Sign Out Approval",
                        onClick = {
                            showSignOutParentApproval = false
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SecondaryButton(
                        text = "Cancel",
                        onClick = { showSignOutParentApproval = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showSubscriptionModal) {
        Dialog(onDismissRequest = { showSubscriptionModal = false }) {
            FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column {
                    Text(text = "Subscription Info", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Plan: Family Protection (Managed by Parent)", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                    Text(text = "Status: 3-Month Free Trial Active", style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(text = "Close", onClick = { showSubscriptionModal = false }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showParentModal) {
        Dialog(onDismissRequest = { showParentModal = false }) {
            FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column {
                    Text(text = "Parent Controller Link", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Controller: Priya Sharma", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Text(text = "Status: Connected", style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(text = "Close", onClick = { showParentModal = false }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showNfcModal) {
        Dialog(onDismissRequest = { showNfcModal = false }) {
            FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column {
                    Text(text = "NFC Focus Card Status", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Assigned Card: FC0001", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Text(text = "Token: 8XK92LM7", style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary))
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(text = "Simulate NFC Tap", onClick = { showNfcModal = false; onOpenNfcTap() }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(text = "Close", onClick = { showNfcModal = false }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showHelpModal) {
        Dialog(onDismissRequest = { showHelpModal = false }) {
            FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column {
                    Text(text = "Child Focus Guide", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "• Starting Focus: Tap your NFC card on the back of phone or scan QR code.", style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "• Emergency Exit: Click 'End Focus' during an active session to send an exit request to parent.", style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(text = "Close", onClick = { showHelpModal = false }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
