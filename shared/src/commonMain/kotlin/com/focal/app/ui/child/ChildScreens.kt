package com.focal.app.ui.child

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontStyle
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
import androidx.compose.animation.Crossfade
import androidx.compose.ui.text.style.TextOverflow
import com.focal.app.streak.StreakRepository
import com.focal.app.streak.getDeviceTimeZoneOffsetMillis
import com.focal.app.streak.getDeviceCurrentTime
import com.focal.app.streak.CurrentDeviceTime
import com.focal.app.data.ScreenTimeRepository
import com.focal.app.data.InsightsRange
import com.focal.app.data.InsightsData
import com.focal.app.data.PairedBarData
import com.focal.app.data.AppUsageInfo
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import focal.shared.generated.resources.Res
import focal.shared.generated.resources.focal_logo
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.ActivityRepository
import com.focal.app.data.ActivityEventType
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.*
import com.focal.app.ui.child.components.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import com.focal.app.ui.child.components.PrimaryButton as ChildPrimaryButton
import com.focal.app.ui.child.components.SecondaryButton as ChildSecondaryButton
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.ChildRadius
import com.focal.app.ui.theme.LocalChildTypography
import com.focal.app.ui.theme.ChildTheme
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// 1. CHILD DASHBOARD / HOME (IDLE & ACTIVE STATES)
// -------------------------------------------------------------
private fun formatEndTime(startTimeMillis: Long, durationMinutes: Int): String {
    val endMillis = startTimeMillis + (durationMinutes * 60 * 1000L)
    val tzOffset = getDeviceTimeZoneOffsetMillis()
    val localSecs = ((endMillis + tzOffset) / 1000L) % 86400L
    val positiveSecs = if (localSecs < 0) localSecs + 86400L else localSecs
    val hours24 = (positiveSecs / 3600).toInt()
    val minutes = ((positiveSecs % 3600) / 60).toInt()
    val isPm = hours24 >= 12
    val hours12 = when {
        hours24 == 0 -> 12
        hours24 > 12 -> hours24 - 12
        else -> hours24
    }
    val minStr = if (minutes < 10) "0$minutes" else "$minutes"
    val amPm = if (isPm) "PM" else "AM"
    return "$hours12:$minStr $amPm"
}

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
    val typography = LocalChildTypography.current
    val user = repository.currentUser
    val isActive = focusEngine.sessionState == SessionState.ACTIVE
    val currentSession = focusEngine.currentSession

    val missingItems = focusEngine.missingPermissionsList
    val isProtectionOff = !focusEngine.isProtectionActive || missingItems.isNotEmpty()

    val realScreenTimeMin = PlatformPermissionsHelper.getRealDailyScreenTimeMinutes()
    val screenTimeText = if (realScreenTimeMin > 0) "${realScreenTimeMin / 60}h ${realScreenTimeMin % 60}m" else "0m"

    val todayFocusMin = focusEngine.todayFocusMinutes
    val focusTodayText = if (todayFocusMin > 0) "${todayFocusMin / 60}h ${todayFocusMin % 60}m" else "0m"

    val currentStreak by StreakRepository.instance.currentStreak.collectAsState(initial = StreakRepository.instance.getStreak())

    val protectionState by com.focal.app.protection.ProtectionStateRepository.instance.state.collectAsState()
    var nowElapsed by remember { mutableStateOf(com.focal.app.model.currentTimeMillis()) }
    LaunchedEffect(protectionState.parentUnlockUntilElapsed) {
        while (com.focal.app.model.currentTimeMillis() < protectionState.parentUnlockUntilElapsed) {
            nowElapsed = com.focal.app.model.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
        nowElapsed = com.focal.app.model.currentTimeMillis()
    }
    val isParentUnlockActive = nowElapsed < protectionState.parentUnlockUntilElapsed
    val unlockMinsLeft = ((protectionState.parentUnlockUntilElapsed - nowElapsed) / 60000L).coerceAtLeast(1)
    var showLockNowSnackbar by remember { mutableStateOf(false) }

    var showEndFocusSheet by remember { mutableStateOf(false) }
    var showSessionComplete by remember { mutableStateOf(false) }
    var cardListeningSubtitle by remember { mutableStateOf("Listening… the same card ends focus") }
    val emergencyRepo = com.focal.app.data.EmergencyExitRepository.instance
    val emergencyState by emergencyRepo.requestState.collectAsState()
    val isEmergencyPending = emergencyState is com.focal.app.data.EmergencyRequestState.Pending
    var completedSessionSummary by remember { mutableStateOf<FocusSession?>(null) }

    // Observe normal completion (timer expired naturally)
    LaunchedEffect(focusEngine.sessionState) {
        if (focusEngine.sessionState == SessionState.COMPLETED) {
            val lastSummary = focusEngine.lastCompletedSummary
            if (lastSummary != null && !showEndFocusSheet && lastSummary.triggerType != TriggerType.NFC && lastSummary.triggerType != TriggerType.EMERGENCY_OVERRIDE) {
                completedSessionSummary = lastSummary
                showSessionComplete = true
            }
        }
    }

    if (showSessionComplete && completedSessionSummary != null) {
        val summary = completedSessionSummary!!
        SessionCompleteScreen(
            policyName = summary.policyName,
            durationMinutes = summary.targetDurationMinutes,
            blockedAttemptsCount = summary.blockedAttempts,
            streakCount = currentStreak,
            focusTodayMinutes = todayFocusMin,
            onDoneClick = {
                showSessionComplete = false
                completedSessionSummary = null
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
        // Top-right decorative GlowArcs (same position/size across all tab screens)
        GlowArcs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 110.dp)
        ) {
            // Header Row: Avatar + "Hi, {name}" + StreakPill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChildAvatar(name = user.name, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hi,",
                        style = typography.caption.copy(
                            color = ChildColors.Secondary,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = user.name,
                        style = typography.title.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 2
                    )
                }
                StreakPill(streakCount = currentStreak)
            }

            if (!isActive) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Stay focused. Build a better you.",
                    style = typography.body.copy(
                        color = ChildColors.Secondary,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(Modifier.height(18.dp))

            // Parent Unlock Active Banner (PH3)
            if (isParentUnlockActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Soft white glow around banner
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                                    center = center,
                                    radius = size.width / 1.5f
                                ),
                                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                            )
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF141414), Color(0xFF0A0A0A))
                            )
                        )
                        .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 44dp dark circle with open-lock icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .border(1.dp, Color(0xFF2E2E2E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.LockOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unlocked · $unlockMinsLeft min left",
                                style = typography.body.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Protection is paused",
                                style = typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = ChildColors.Secondary
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        ChildPrimaryButton(
                            text = "Lock now",
                            onClick = {
                                com.focal.app.protection.ProtectionStateRepository.instance.clearParentUnlock()
                                PlatformPermissionsHelper.showToast("Protection is back on")
                                showLockNowSnackbar = true
                            },
                            modifier = Modifier.wrapContentWidth().height(42.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Protection Disabled Red Banner: ONLY when protection is off
            if (isProtectionOff && !isParentUnlockActive) {
                ChildDangerBanner(
                    title = "Protection is off",
                    subtitle = if (missingItems.isNotEmpty()) "${missingItems.joinToString(", ")} protection is off" else "Turn Accessibility on to resume focus locks",
                    onFixClick = onOpenPermissions
                )
                Spacer(Modifier.height(12.dp))
            }

            // Crossfade transition between Ready (Screen 1) and Focus Active (Screen 2)
            Crossfade(
                targetState = isActive,
                animationSpec = tween(250),
                label = "homeFocusTransition"
            ) { active ->
                if (active) {
                    // =========================================================
                    // SCREEN 2 — HOME (FOCUS ACTIVE)
                    // =========================================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Hero GlassCard (Focus Active)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = 24.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                FocusActivePill()

                                Spacer(Modifier.height(24.dp))

                                CountdownRing(
                                    progress = focusEngine.progressFraction,
                                    modifier = Modifier.size(236.dp),
                                    strokeWidth = 10.dp,
                                    centerContent = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = focusEngine.formatRemainingTime(),
                                                style = typography.display.copy(
                                                    fontSize = 38.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontFeatureSettings = "tnum"
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "REMAINING",
                                                style = typography.overline.copy(
                                                    color = ChildColors.Tertiary,
                                                    letterSpacing = 1.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                )

                                Spacer(Modifier.height(20.dp))

                                val endTimeStr = formatEndTime(
                                    currentSession?.startTimeMillis ?: com.focal.app.model.currentTimeMillis(),
                                    currentSession?.targetDurationMinutes ?: 160
                                )
                                val startedWithText = when (currentSession?.triggerType) {
                                    TriggerType.NFC -> "card"
                                    TriggerType.QR -> "QR"
                                    TriggerType.SCHEDULED -> "schedule"
                                    TriggerType.REMOTE_PARENT -> "parent"
                                    else -> "card"
                                }

                                Text(
                                    text = "Ends at $endTimeStr · Started with $startedWithText",
                                    style = typography.body.copy(color = ChildColors.Secondary, fontSize = 14.sp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Policy Row GlassCard
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = 18.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ChildColors.IconCircleBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = FocalIcons.DeviceBox,
                                        contentDescription = "Policy",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentSession?.policyName ?: "Study Mode",
                                        style = typography.headline.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 2
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${currentSession?.blockedAppsCount ?: 8} apps · ${currentSession?.blockedWebsitesCount ?: 3} websites blocked",
                                        style = typography.caption.copy(color = ChildColors.Secondary),
                                        maxLines = 2
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${focusEngine.blockedAttemptsCount}",
                                        style = typography.title.copy(
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "blocked",
                                        style = typography.caption.copy(color = ChildColors.Secondary)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // End Focus SecondaryButton
                        ChildSecondaryButton(
                            text = "End focus",
                            onClick = {
                                cardListeningSubtitle = "Listening… the same card ends focus"
                                showEndFocusSheet = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // =========================================================
                    // SCREEN 1 — HOME (READY)
                    // =========================================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Hero GlassCard (Ready to Focus, PH1)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = 18.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Top section: Left text column (max 55% width) + Top-right fixed 150x120dp box
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(0.55f)
                                    ) {
                                        Text(
                                            text = "TAP YOUR CARD",
                                            style = typography.overline.copy(
                                                color = ChildColors.Tertiary,
                                                letterSpacing = 1.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "Ready to\nfocus?",
                                            style = typography.display.copy(
                                                fontSize = 30.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                lineHeight = 34.sp
                                            ),
                                            maxLines = 2
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = "Tap your card or scan your parent's QR.",
                                            style = typography.body.copy(
                                                color = ChildColors.Secondary,
                                                fontSize = 14.sp,
                                                lineHeight = 19.sp
                                            ),
                                            maxLines = 2
                                        )
                                    }

                                    // top-right: FocalCardHero 124dp wide, tilted -14°, soft glow, two thin ellipse rings under it, inside a fixed 150x120dp box aligned to the card's top-right
                                    Box(
                                        modifier = Modifier.size(width = 150.dp, height = 120.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        FocalCardHero(
                                            modifier = Modifier.fillMaxSize(),
                                            cardWidth = 124.dp,
                                            cardHeight = 77.dp,
                                            logoSize = 42.dp,
                                            tiltAngle = -14f,
                                            ringCount = 2
                                        )
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                // bottom: Row with two equal buttons (weight 1f each, 52dp tall, 10dp gap): PrimaryButton (NFC icon) "Tap card" | SecondaryButton (QR icon) "Scan QR". No arrows, no "…".
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ChildPrimaryButton(
                                        text = "Tap card",
                                        onClick = onNfcTapClick,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = FocalIcons.Nfc,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    )

                                    ChildSecondaryButton(
                                        text = "Scan QR",
                                        onClick = onQrScanClick,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = FocalIcons.QrCode,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // "Today's activity" GlassCard
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = 18.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Today's activity",
                                    style = typography.headline.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Row(
                                    modifier = Modifier.clickable { onViewInsights() },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "View details",
                                        style = typography.caption.copy(color = ChildColors.Secondary)
                                    )
                                    Icon(
                                        imageVector = FocalIcons.ArrowForward,
                                        contentDescription = null,
                                        tint = ChildColors.Secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Focus Today
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconCircle(size = 46.dp) {
                                        Icon(
                                            imageVector = FocalIcons.Target,
                                            contentDescription = "Focus",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = focusTodayText,
                                            style = typography.headline.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Focus today",
                                            style = typography.caption.copy(color = ChildColors.Secondary)
                                        )
                                    }
                                }

                                // Inset vertical divider
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .width(1.dp)
                                        .background(ChildColors.Border)
                                )
                                Spacer(Modifier.width(16.dp))

                                // Screen Time
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconCircle(size = 46.dp) {
                                        Icon(
                                            imageVector = FocalIcons.Smartphone,
                                            contentDescription = "Screen time",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = screenTimeText,
                                            style = typography.headline.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Screen time",
                                            style = typography.caption.copy(color = ChildColors.Secondary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showEndFocusSheet) {
                EndFocusBottomSheet(
                    policyName = currentSession?.policyName ?: "Study Mode",
                    remainingMinutes = (focusEngine.remainingSeconds / 60).coerceAtLeast(1),
                    onDismiss = {
                        showEndFocusSheet = false
                    },
                    cardListeningSubtitle = cardListeningSubtitle,
                    onCardTapped = {
                        val userCards = repository.nfcCards.filter { (it.familyId == user.familyId || it.assignedChildId == user.id || it.ownerId == user.id) && it.isEnabled }
                        val pairedCard = userCards.firstOrNull()?.cardId ?: "FC0001"
                        val res = focusEngine.endFocusWithNfc(pairedCard, user)
                        if (res.isSuccess) {
                            showEndFocusSheet = false
                            showSessionComplete = false
                            PlatformPermissionsHelper.showToast("Focus ended with card")
                        } else {
                            cardListeningSubtitle = "This isn't your family's FOCAL card"
                        }
                    },
                    onRequestEmergencyExit = { reason ->
                        emergencyRepo.request(reason)
                    },
                    isRequestPending = isEmergencyPending,
                    parentName = "Mom"
                )
            }
        }

        if (showLockNowSnackbar) {
            LaunchedEffect(showLockNowSnackbar) {
                kotlinx.coroutines.delay(2500)
                showLockNowSnackbar = false
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, Color(0xFF3A3A3A), CircleShape)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Protection is back on",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
}

// -------------------------------------------------------------
// SCREEN 3 — SESSION COMPLETE (Shown when session ends normally)
// -------------------------------------------------------------
@Composable
fun SessionCompleteScreen(
    policyName: String,
    durationMinutes: Int,
    blockedAttemptsCount: Int,
    streakCount: Int,
    focusTodayMinutes: Int,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current

    val animatedScale = remember { Animatable(0.8f) }
    val animatedCheckAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedScale.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        animatedCheckAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
    }

    val hours = durationMinutes / 60
    val mins = durationMinutes % 60
    val totalFocusedTimeText = when {
        hours > 0 && mins == 0 -> "${hours}h 00m"
        hours > 0 -> "${hours}h ${if (mins < 10) "0$mins" else "$mins"}m"
        else -> "${mins}m"
    }

    val todayHours = focusTodayMinutes / 60
    val todayMins = focusTodayMinutes % 60
    val focusTodayText = if (todayHours > 0) "${todayHours}h ${todayMins}m" else "${todayMins}m"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Soft radial glow at top
        Canvas(modifier = Modifier.fillMaxWidth().height(260.dp).align(Alignment.TopCenter)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width / 2f, 80.dp.toPx()),
                    radius = 160.dp.toPx()
                ),
                center = Offset(size.width / 2f, 80.dp.toPx()),
                radius = 160.dp.toPx()
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(32.dp))

                // 116dp circle, 2dp white border with white glow, check icon
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = animatedScale.value
                            scaleY = animatedScale.value
                        }
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                                    center = center,
                                    radius = size.width / 1.3f
                                ),
                                radius = size.width / 1.3f
                            )
                        }
                        .clip(CircleShape)
                        .background(ChildColors.BannerBg)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Check,
                        contentDescription = "Session Complete",
                        tint = Color.White,
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                alpha = animatedCheckAlpha.value
                            }
                    )
                }

                Spacer(Modifier.height(28.dp))

                // "SESSION COMPLETE" overline
                Text(
                    text = "SESSION COMPLETE",
                    style = typography.overline.copy(
                        color = ChildColors.Tertiary,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Total focused time big (48sp bold)
                Text(
                    text = totalFocusedTimeText,
                    style = typography.display.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 52.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // "focused in {policy}"
                Text(
                    text = "focused in $policyName",
                    style = typography.body.copy(
                        color = ChildColors.Secondary,
                        fontSize = 15.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(36.dp))

                // 3 equal GlassCards (radius 20), single-line labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Blocked
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        radius = 20.dp,
                        padding = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$blockedAttemptsCount",
                                style = typography.title.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Blocked",
                                style = typography.caption.copy(color = ChildColors.Secondary),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Card 2: Day streak
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        radius = 20.dp,
                        padding = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$streakCount",
                                style = typography.title.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Day streak",
                                style = typography.caption.copy(color = ChildColors.Secondary),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Card 3: Focus today
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        radius = 20.dp,
                        padding = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = focusTodayText,
                                style = typography.title.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Focus today",
                                style = typography.caption.copy(color = ChildColors.Secondary),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // PrimaryButton "Done" -> Home Ready
            ChildPrimaryButton(
                text = "Done",
                onClick = onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
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
// 6. FOCUS SCHEDULES (READ-ONLY VIEW) — STEP 3 REDESIGN
// -------------------------------------------------------------
enum class ChildScheduleStatusType {
    ACTIVE_NOW,
    NEXT,
    TONIGHT,
    AT_TIME,
    DONE,
    OFF
}

data class ChildScheduleDisplayItem(
    val schedule: ScheduleItem,
    val icon: ImageVector,
    val statusType: ChildScheduleStatusType,
    val statusLabel: String
)

private fun getScheduleVectorIcon(title: String): ImageVector {
    val lower = title.lowercase()
    return when {
        lower.contains("morning") || lower.contains("zen") || lower.contains("coffee") -> FocalIcons.Coffee
        lower.contains("deep") || lower.contains("work") || lower.contains("study") -> FocalIcons.Target
        lower.contains("homework") || lower.contains("book") || lower.contains("school") -> FocalIcons.Book
        lower.contains("wind") || lower.contains("down") || lower.contains("sun") || lower.contains("evening") -> FocalIcons.Sun
        lower.contains("night") || lower.contains("guard") || lower.contains("sleep") || lower.contains("moon") -> FocalIcons.Moon
        else -> FocalIcons.Clock
    }
}

private fun parseMinuteOfDay(raw: String, defaultAmPm: String? = null): Int {
    val clean = raw.trim().uppercase()
    val isPm = clean.contains("PM") || (defaultAmPm?.uppercase() == "PM" && !clean.contains("AM"))
    val isAm = clean.contains("AM") || (defaultAmPm?.uppercase() == "AM" && !clean.contains("PM"))
    val digits = clean.replace("AM", "").replace("PM", "").trim()
    val parts = digits.split(":")
    var hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
    if (isPm && hour < 12) hour += 12
    if (isAm && hour == 12) hour = 0
    return hour * 60 + min
}

private fun parseScheduleTimeRange(timeRange: String): Triple<Int, Int, String> {
    val split = timeRange.split(Regex("[–\\-]|to"))
    if (split.size < 2) return Triple(0, 0, "")
    val rawStart = split[0].trim()
    val rawEnd = split[1].trim()
    val endAmPm = if (rawEnd.uppercase().contains("PM")) "PM" else if (rawEnd.uppercase().contains("AM")) "AM" else null
    val startMinute = parseMinuteOfDay(rawStart, defaultAmPm = endAmPm)
    val endMinute = parseMinuteOfDay(rawEnd)

    val startHour24 = startMinute / 60
    val startMin = startMinute % 60
    val isPm = startHour24 >= 12
    val hour12 = when {
        startHour24 == 0 -> 12
        startHour24 > 12 -> startHour24 - 12
        else -> startHour24
    }
    val minStr = if (startMin < 10) "0$startMin" else "$startMin"
    val amPmStr = if (isPm) "PM" else "AM"
    val displayStart = if (startMin == 0) "$hour12:00 $amPmStr" else "$hour12:$minStr $amPmStr"

    return Triple(startMinute, endMinute, displayStart)
}

@Composable
fun ScheduleStatusPill(
    statusType: ChildScheduleStatusType,
    text: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, textColor) = when (statusType) {
        ChildScheduleStatusType.ACTIVE_NOW -> Triple(Color.White, Color.Transparent, Color.Black)
        ChildScheduleStatusType.NEXT,
        ChildScheduleStatusType.TONIGHT,
        ChildScheduleStatusType.AT_TIME -> Triple(Color.Transparent, ChildColors.BorderStrong, Color.White)
        ChildScheduleStatusType.DONE,
        ChildScheduleStatusType.OFF -> Triple(Color.Transparent, ChildColors.Border, ChildColors.Tertiary)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, CircleShape)
                else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = LocalChildTypography.current.overline.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = textColor
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun SchedulesScreen(
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val schedulesList = repository.schedules
    val isFocusEngineActive = FocusEngine.instance.sessionState == SessionState.ACTIVE

    // Minute ticker for dynamic reactive status updates
    var minuteTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            minuteTick++
        }
    }

    val currentDeviceTime = remember(minuteTick) { getDeviceCurrentTime() }
    val nowMinute = currentDeviceTime.minuteOfDay

    // Compute status for all schedules
    val displayItems = remember(schedulesList.toList(), minuteTick, isFocusEngineActive) {
        var nextScheduleAssigned = false

        data class TempItem(
            val schedule: ScheduleItem,
            val startMin: Int,
            val endMin: Int,
            val displayStart: String,
            val isOvernight: Boolean,
            var resolvedType: ChildScheduleStatusType? = null,
            var resolvedLabel: String? = null
        )

        val temps = schedulesList.map { sch ->
            val (sMin, eMin, dStart) = parseScheduleTimeRange(sch.timeRange)
            val overnight = sMin > eMin
            TempItem(sch, sMin, eMin, dStart, overnight)
        }

        // 1. Resolve OFF, ACTIVE_NOW, and DONE
        temps.forEach { item ->
            if (!item.schedule.isEnabled) {
                item.resolvedType = ChildScheduleStatusType.OFF
                item.resolvedLabel = "OFF"
            } else {
                val isActiveNow = if (item.isOvernight) {
                    nowMinute >= item.startMin || nowMinute < item.endMin
                } else {
                    nowMinute in item.startMin until item.endMin
                }

                if (isActiveNow) {
                    item.resolvedType = ChildScheduleStatusType.ACTIVE_NOW
                    item.resolvedLabel = "ACTIVE NOW"
                } else {
                    val isDone = if (item.isOvernight) {
                        nowMinute in item.endMin until item.startMin && nowMinute >= 12 * 60
                    } else {
                        nowMinute >= item.endMin
                    }
                    if (isDone) {
                        item.resolvedType = ChildScheduleStatusType.DONE
                        item.resolvedLabel = "DONE"
                    }
                }
            }
        }

        // 2. Identify the earliest upcoming schedule today
        val upcomingTemps = temps.filter { it.resolvedType == null }
            .sortedBy { if (it.startMin < nowMinute) it.startMin + 1440 else it.startMin }

        upcomingTemps.forEachIndexed { index, item ->
            if (index == 0 && !nextScheduleAssigned) {
                item.resolvedType = ChildScheduleStatusType.NEXT
                item.resolvedLabel = "NEXT ${item.displayStart}"
                nextScheduleAssigned = true
            } else {
                if (item.startMin >= 18 * 60 || item.isOvernight) {
                    item.resolvedType = ChildScheduleStatusType.TONIGHT
                    item.resolvedLabel = "TONIGHT"
                } else {
                    item.resolvedType = ChildScheduleStatusType.AT_TIME
                    item.resolvedLabel = "AT ${item.displayStart}"
                }
            }
        }

        temps.map { item ->
            ChildScheduleDisplayItem(
                schedule = item.schedule,
                icon = getScheduleVectorIcon(item.schedule.title),
                statusType = item.resolvedType ?: ChildScheduleStatusType.OFF,
                statusLabel = item.resolvedLabel ?: "OFF"
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GlowArcs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Schedules",
                style = typography.display.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FocalIcons.Lock,
                    contentDescription = null,
                    tint = ChildColors.Secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Set by your parent · runs automatically",
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Secondary
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            if (displayItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        padding = 28.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ChildColors.IconCircleBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Clock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No schedules yet",
                                style = typography.headline.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Your parent can add focus schedules.",
                                style = typography.body.copy(
                                    fontSize = 14.sp,
                                    color = ChildColors.Secondary
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    items(displayItems, key = { it.schedule.id }) { item ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            padding = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(ChildColors.IconCircleBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.schedule.title,
                                        style = typography.headline.copy(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        ),
                                        maxLines = 2
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = FocalIcons.Repeat,
                                            contentDescription = null,
                                            tint = ChildColors.Secondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = item.schedule.days,
                                            style = typography.caption.copy(
                                                fontSize = 13.sp,
                                                color = ChildColors.Secondary
                                            ),
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Icon(
                                            imageVector = FocalIcons.Clock,
                                            contentDescription = null,
                                            tint = ChildColors.Secondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = item.schedule.timeRange,
                                            style = typography.caption.copy(
                                                fontSize = 13.sp,
                                                color = ChildColors.Secondary
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Clip
                                        )
                                    }
                                }

                                Spacer(Modifier.width(10.dp))

                                ScheduleStatusPill(
                                    statusType = item.statusType,
                                    text = item.statusLabel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. FOCUS INSIGHTS — STEP 3 REDESIGN
// -------------------------------------------------------------
private val MOTIVATIONAL_QUOTES = listOf(
    "Small focus now, big wins later.",
    "Consistency beats intensity every single time.",
    "Your future is created by what you do today.",
    "Master your habits, master your life.",
    "Focus is a muscle. The more you use it, the stronger it gets.",
    "Distraction is the enemy of greatness.",
    "One session at a time. Keep going."
)

@Composable
fun InsightsScreen(
    childId: String? = null,
    onBack: (() -> Unit)? = null,
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    val typography = LocalChildTypography.current
    val isParentView = childId != null
    val targetChild = if (isParentView) {
        repository.childrenList.find { it.id == childId } ?: repository.childrenList.first()
    } else {
        repository.childrenList.find { it.userId == repository.currentUser.id || it.id == "child_1" }
            ?: repository.childrenList.first()
    }
    val childName = targetChild.name
    val childGrade = targetChild.grade.ifBlank { "Student" }
    val isThisPhone = (targetChild.id == "child_1" || (!isParentView && repository.currentUser.role == com.focal.app.model.UserRole.CHILD))

    val scrollState = rememberSaveable(saver = androidx.compose.foundation.ScrollState.Saver) {
        androidx.compose.foundation.ScrollState(0)
    }

    var selectedRange by remember { mutableStateOf(InsightsRange.WEEK) }

    // Live data binding from ScreenTimeRepository and FocusEngine
    val childTodayFocusMinutes = if (isThisPhone && focusEngine.todayFocusMinutes > 0) {
        focusEngine.todayFocusMinutes
    } else {
        targetChild.todayFocusMinutes
    }

    val childEvents = remember(targetChild.id) {
        ActivityRepository.instance.getEventsForChild(targetChild.id)
    }
    val blockedCount = remember(childEvents, isThisPhone) {
        val blockedFromEvents = childEvents.count {
            it.type == ActivityEventType.APP_BLOCKED || it.type == ActivityEventType.SITE_BLOCKED
        }
        if (blockedFromEvents > 0) blockedFromEvents else if (isThisPhone) 14 else 0
    }

    val insightsData = remember(selectedRange, childTodayFocusMinutes, isThisPhone, blockedCount) {
        if (isThisPhone) {
            ScreenTimeRepository.getInsightsData(
                range = selectedRange,
                childTodayFocusMinutes = childTodayFocusMinutes,
                totalBlockedAttempts = blockedCount
            )
        } else {
            val rangeFocusMinutes = when (selectedRange) {
                InsightsRange.WEEK -> (childTodayFocusMinutes * 5).coerceAtLeast(120)
                InsightsRange.MONTH -> (childTodayFocusMinutes * 20).coerceAtLeast(600)
                InsightsRange.LIFETIME -> (childTodayFocusMinutes * 120).coerceAtLeast(3600)
            }
            val dailyAvg = (rangeFocusMinutes / (if (selectedRange == InsightsRange.WEEK) 7 else if (selectedRange == InsightsRange.MONTH) 30 else 180)).coerceAtLeast(30)

            val pairedBars = mutableListOf<PairedBarData>()
            when (selectedRange) {
                InsightsRange.WEEK -> {
                    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    dayLabels.forEachIndexed { idx, label ->
                        val focusMin = when (idx) {
                            6 -> childTodayFocusMinutes
                            5 -> (childTodayFocusMinutes * 0.9f).toInt()
                            4 -> (childTodayFocusMinutes * 1.2f).toInt()
                            3 -> (childTodayFocusMinutes * 0.8f).toInt()
                            2 -> (childTodayFocusMinutes * 1.1f).toInt()
                            1 -> (childTodayFocusMinutes * 0.7f).toInt()
                            else -> (childTodayFocusMinutes * 1.0f).toInt()
                        }
                        pairedBars.add(PairedBarData(label, focusMin, 0))
                    }
                }
                InsightsRange.MONTH -> {
                    listOf("W1", "W2", "W3", "W4", "W5").forEachIndexed { idx, label ->
                        pairedBars.add(PairedBarData(label, (dailyAvg * (5 + idx % 3)), 0))
                    }
                }
                InsightsRange.LIFETIME -> {
                    listOf("May", "Jun", "Jul", "Aug", "Sep", "Oct").forEachIndexed { idx, label ->
                        pairedBars.add(PairedBarData(label, (dailyAvg * (20 + idx * 2)), 0))
                    }
                }
            }

            InsightsData(
                range = selectedRange,
                totalFocusMinutes = rangeFocusMinutes,
                dailyAverageFocusMinutes = dailyAvg,
                dailyAverageScreenTimeMinutes = null,
                blockedAttemptsCount = blockedCount,
                pairedBars = pairedBars,
                topApps = emptyList(),
                hasUsageAccess = false
            )
        }
    }

    val currentDeviceTime = remember { getDeviceCurrentTime() }
    val quoteIndex = (currentDeviceTime.dayOfYear) % MOTIVATIONAL_QUOTES.size
    val dailyQuote = MOTIVATIONAL_QUOTES[quoteIndex]

    @Composable
    fun InsightsBody(bottomPadding: androidx.compose.ui.unit.Dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .padding(bottom = bottomPadding)
        ) {
            // Header Profile GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChildAvatar(name = childName, size = 56.dp)

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = childName,
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 2
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (childGrade.contains("Student")) childGrade else "$childGrade · Student",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            ),
                            maxLines = 2
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(ChildColors.Border)
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        val totalHrs = insightsData.totalFocusMinutes / 60
                        val totalMins = insightsData.totalFocusMinutes % 60
                        Text(
                            text = "${totalHrs}h ${totalMins}m",
                            style = typography.title.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (selectedRange) {
                                InsightsRange.WEEK -> "Focused this week"
                                InsightsRange.MONTH -> "Focused this month"
                                InsightsRange.LIFETIME -> "Focused all time"
                            },
                            style = typography.caption.copy(
                                fontSize = 11.sp,
                                color = ChildColors.Secondary,
                                letterSpacing = 0.sp
                            ),
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Segmented Control (3 equal segments)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(ChildColors.BannerBg)
                    .border(1.dp, ChildColors.Border, CircleShape)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InsightsRange.entries.forEach { range ->
                    val isSelected = selectedRange == range
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { selectedRange = range },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = range.label,
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else ChildColors.Secondary
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // "Focus time" GlassCard with Paired Bar Chart
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 18.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ChildColors.IconCircleBg)
                                .border(1.dp, ChildColors.Border, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Target,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Focus time",
                                style = typography.headline.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isThisPhone && insightsData.hasUsageAccess) "vs screen time" else "logged focus",
                                style = typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = ChildColors.Secondary
                                )
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val avgHrs = insightsData.dailyAverageFocusMinutes / 60
                        val avgMins = insightsData.dailyAverageFocusMinutes % 60
                        Text(
                            text = "${avgHrs}h ${avgMins}m",
                            style = typography.title.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Daily average",
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Paired Bar Chart (Canvas)
                var chartAnimated by remember { mutableStateOf(false) }
                LaunchedEffect(selectedRange) {
                    chartAnimated = false
                    kotlinx.coroutines.delay(20)
                    chartAnimated = true
                }

                val animProgress by animateFloatAsState(
                    targetValue = if (chartAnimated) 1f else 0f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )

                val showScreenTimeBars = isThisPhone && insightsData.hasUsageAccess
                val maxMinutes = remember(insightsData, showScreenTimeBars) {
                    val maxFocus = insightsData.pairedBars.maxOfOrNull { it.focusMinutes } ?: 120
                    val maxScreen = if (showScreenTimeBars) {
                        insightsData.pairedBars.maxOfOrNull { it.screenTimeMinutes } ?: 0
                    } else 0
                    maxOf(maxFocus, maxScreen, 120)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val count = insightsData.pairedBars.size
                        if (count == 0) return@Canvas

                        val slotWidth = size.width / count
                        val barWidth = 12.dp.toPx()
                        val barGap = 3.dp.toPx()
                        val availableH = size.height - 8.dp.toPx()

                        insightsData.pairedBars.forEachIndexed { i, barData ->
                            val slotCenter = slotWidth * i + (slotWidth / 2f)

                            if (showScreenTimeBars) {
                                val pairTotalWidth = barWidth * 2 + barGap
                                val focusLeft = slotCenter - (pairTotalWidth / 2f)
                                val screenLeft = focusLeft + barWidth + barGap

                                val focusFrac = (barData.focusMinutes.toFloat() / maxMinutes).coerceIn(0.04f, 1f)
                                val focusBarH = availableH * focusFrac * animProgress

                                val screenFrac = (barData.screenTimeMinutes.toFloat() / maxMinutes).coerceIn(0.04f, 1f)
                                val screenBarH = availableH * screenFrac * animProgress

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.White, ChildColors.Secondary),
                                        startY = availableH - focusBarH,
                                        endY = availableH
                                    ),
                                    topLeft = Offset(focusLeft, availableH - focusBarH),
                                    size = Size(barWidth, focusBarH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )

                                drawRoundRect(
                                    color = ChildColors.BorderStrong,
                                    topLeft = Offset(screenLeft, availableH - screenBarH),
                                    size = Size(barWidth, screenBarH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            } else {
                                val focusLeft = slotCenter - (barWidth / 2f)
                                val focusFrac = (barData.focusMinutes.toFloat() / maxMinutes).coerceIn(0.04f, 1f)
                                val focusBarH = availableH * focusFrac * animProgress

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.White, ChildColors.Secondary),
                                        startY = availableH - focusBarH,
                                        endY = availableH
                                    ),
                                    topLeft = Offset(focusLeft, availableH - focusBarH),
                                    size = Size(barWidth, focusBarH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Labels under chart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    insightsData.pairedBars.forEach { bar ->
                        Text(
                            text = bar.label,
                            style = typography.caption.copy(
                                fontSize = 11.sp,
                                color = ChildColors.Secondary
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Focus",
                        style = typography.caption.copy(
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    )

                    if (showScreenTimeBars) {
                        Spacer(Modifier.width(18.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ChildColors.BorderStrong)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Screen time",
                            style = typography.caption.copy(
                                fontSize = 11.sp,
                                color = ChildColors.Secondary
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Screen time / Most used apps:
            if (!isThisPhone) {
                // GlassCard for remote child: "{name}'s screen time will appear once her phone syncs."
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 20.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ChildColors.IconCircleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Smartphone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "$childName's screen time will appear once her phone syncs.",
                            style = typography.body.copy(
                                fontSize = 14.sp,
                                color = ChildColors.Secondary,
                                lineHeight = 20.sp
                            ),
                            maxLines = 2
                        )
                    }
                }
            } else if (insightsData.hasUsageAccess && insightsData.topApps.isNotEmpty()) {
                // "Most used apps" GlassCard
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 18.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Most used apps",
                            style = typography.headline.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = when (selectedRange) {
                                InsightsRange.WEEK -> "This week"
                                InsightsRange.MONTH -> "This month"
                                InsightsRange.LIFETIME -> "All time"
                            },
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    val maxUsage = remember(insightsData.topApps) {
                        insightsData.topApps.maxOfOrNull { it.usageMinutes }?.coerceAtLeast(1) ?: 1
                    }

                    insightsData.topApps.take(4).forEachIndexed { idx, app ->
                        if (idx > 0) Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ChildColors.IconCircleBg)
                                    .border(1.dp, ChildColors.Border, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.iconFallbackLetter,
                                    style = typography.caption.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = app.appName,
                                        style = typography.body.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    val appH = app.usageMinutes / 60
                                    val appM = app.usageMinutes % 60
                                    val timeStr = if (appH > 0) "${appH}h ${appM}m" else "${appM}m"

                                    Text(
                                        text = timeStr,
                                        style = typography.caption.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        ),
                                        maxLines = 1
                                    )
                                }

                                Spacer(Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(ChildColors.NavSelected)
                                ) {
                                    val fraction = (app.usageMinutes.toFloat() / maxUsage).coerceIn(0.04f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Usage Access Prompt Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 20.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ChildColors.IconCircleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Clock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "See your screen time",
                            style = typography.headline.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Allow usage access to see which apps you use most and compare with focus time.",
                            style = typography.body.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        ChildSecondaryButton(
                            text = "Allow usage access",
                            onClick = { PlatformPermissionsHelper.openUsageAccessSettings() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Two small GlassCards side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Card: Avg screen time
                GlassCard(
                    modifier = Modifier.weight(1f),
                    radius = ChildRadius.SmallCard,
                    padding = 16.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChildColors.IconCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Smartphone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    val avgScreenText = if (isThisPhone && insightsData.hasUsageAccess && insightsData.dailyAverageScreenTimeMinutes != null) {
                        val scrH = insightsData.dailyAverageScreenTimeMinutes / 60
                        val scrM = insightsData.dailyAverageScreenTimeMinutes % 60
                        "${scrH}h ${scrM}m"
                    } else {
                        "—"
                    }

                    Text(
                        text = avgScreenText,
                        style = typography.title.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Avg screen time",
                        style = typography.caption.copy(
                            fontSize = 12.sp,
                            color = ChildColors.Secondary
                        ),
                        maxLines = 2
                    )
                }

                // Right Card: Blocked attempts
                GlassCard(
                    modifier = Modifier.weight(1f),
                    radius = ChildRadius.SmallCard,
                    padding = 16.dp
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChildColors.IconCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "${if (isThisPhone) insightsData.blockedAttemptsCount else blockedCount}",
                        style = typography.title.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Blocked attempts",
                        style = typography.caption.copy(
                            fontSize = 12.sp,
                            color = ChildColors.Secondary
                        ),
                        maxLines = 2
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quote GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                                    center = Offset(size.width * 0.9f, size.height * 0.8f),
                                    radius = size.width * 0.5f
                                ),
                                center = Offset(size.width * 0.9f, size.height * 0.8f),
                                radius = size.width * 0.5f
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ChildColors.IconCircleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Sprout,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(ChildColors.Border)
                        )

                        Spacer(Modifier.width(14.dp))

                        Text(
                            text = dailyQuote,
                            style = typography.body.copy(
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (onBack != null) {
        DetailScaffold(
            title = "${childName}'s Insights",
            onBack = onBack
        ) {
            InsightsBody(bottomPadding = 36.dp)
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            GlowArcs()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                InsightsBody(bottomPadding = 110.dp)
            }
        }
    }
}

// -------------------------------------------------------------
// 8. CHILD SUB-SCREENS & DETAIL ROUTES
// -------------------------------------------------------------
enum class ChildSubScreen {
    CONNECTED_PARENT,
    FOCAL_CARD,
    PROTECTION,
    NOTIFICATIONS,
    HELP,
    PRIVACY,
    ABOUT,
    PARENT_UNLOCK
}

@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val typography = LocalChildTypography.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top row: 44dp round back button (#0B0B0B, 1dp #262626 border) with 48dp touch area and optional actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ChildColors.Surface)
                            .border(1.dp, ChildColors.Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.ChevronLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (actions != null) {
                    actions()
                }
            }

            if (title.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    style = typography.title.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 2
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = typography.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        ),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

// =============================================================
// SHARED PIECES (Step 4B)
// =============================================================

@Composable
fun SectionOverline(
    text: String,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    Text(
        text = text.uppercase(),
        style = typography.overline.copy(
            color = ChildColors.Tertiary,
            fontSize = 12.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = modifier.padding(top = 22.dp, bottom = 10.dp)
    )
}

data class InfoCardRow(
    val label: String,
    val value: String
)

@Composable
fun InfoCard(
    rows: List<InfoCardRow>,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        radius = 24.dp,
        padding = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.label,
                        style = typography.caption.copy(
                            color = ChildColors.Secondary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = row.value,
                        style = typography.body.copy(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2
                    )
                }
                if (index < rows.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ChildColors.ListDivider)
                    )
                }
            }
        }
    }
}

data class IconRowItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String = "",
    val trailingContent: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun IconRowCard(
    items: List<IconRowItem>,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        radius = 24.dp,
        padding = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (item.onClick != null) Modifier.clickable(onClick = item.onClick)
                            else Modifier
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(size = 44.dp) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = typography.body.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 2
                        )
                        if (item.subtitle.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                style = typography.caption.copy(
                                    fontSize = 12.sp,
                                    color = ChildColors.Secondary
                                ),
                                maxLines = 2
                            )
                        }
                    }
                    if (item.trailingContent != null) {
                        Spacer(Modifier.width(10.dp))
                        item.trailingContent()
                    }
                }
                if (index < items.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 76.dp)
                            .height(1.dp)
                            .background(ChildColors.ListDivider)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 1 — CONNECTED TO PARENT
// -------------------------------------------------------------
@Composable
fun ConnectedParentScreen(
    onBack: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    onStartPairing: () -> Unit = {}
) {
    val typography = LocalChildTypography.current
    val parent = repository.controllerUser
    val isLinked = parent.name.isNotBlank() || repository.childrenList.isNotEmpty()
    val parentDisplayName = if (parent.name.isBlank()) "Priya Sharma" else parent.name
    val parentInitials = com.focal.app.ui.child.components.getAvatarInitials(parentDisplayName)
    val relation = "Mom"

    val manufacturer = remember { PlatformPermissionsHelper.getDeviceManufacturer() }
    val androidVer = remember { PlatformPermissionsHelper.getAndroidVersion() }

    DetailScaffold(
        title = "Connected to parent",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(ChildColors.IconCircleBg)
                            .border(1.dp, ChildColors.BorderStrong, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = parentInitials,
                            style = typography.title.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = parentDisplayName,
                        style = typography.title.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 2
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "$relation · Parent",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            color = ChildColors.Secondary
                        ),
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(Modifier.height(14.dp))

                    if (isLinked) {
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, ChildColors.BorderStrong, CircleShape)
                                .background(ChildColors.BannerBg)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "Linked",
                                style = typography.caption.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                        }
                    } else {
                        StatusPill(text = "Not connected", type = StatusPillType.OFF)
                        Spacer(Modifier.height(16.dp))
                        ChildPrimaryButton(
                            text = "Pair with parent",
                            onClick = onStartPairing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (isLinked) {
                Spacer(Modifier.height(16.dp))

                InfoCard(
                    rows = listOf(
                        InfoCardRow("Linked since", "12 Sep 2026"),
                        InfoCardRow("This phone", "$manufacturer · Android $androidVer"),
                        InfoCardRow("Last synced", "2 min ago")
                    )
                )

                SectionOverline(
                    text = "WHAT YOUR PARENT CAN DO",
                    modifier = Modifier.fillMaxWidth()
                )

                IconRowCard(
                    items = listOf(
                        IconRowItem(
                            icon = FocalIcons.Nfc,
                            title = "Start and end focus",
                            subtitle = "With their FOCAL card or the app"
                        ),
                        IconRowItem(
                            icon = FocalIcons.Calendar,
                            title = "Set schedules and blocked apps",
                            subtitle = "You see them in Schedules"
                        ),
                        IconRowItem(
                            icon = FocalIcons.Chart,
                            title = "See your focus and screen time",
                            subtitle = "Same numbers as your Insights"
                        ),
                        IconRowItem(
                            icon = FocalIcons.Shield,
                            title = "Approve emergency exits",
                            subtitle = "And unlock with their PIN"
                        )
                    )
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FocalIcons.Lock,
                        contentDescription = null,
                        tint = ChildColors.Tertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Only your parent can unlink this phone",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            color = ChildColors.Tertiary
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2 — FOCAL CARD
// -------------------------------------------------------------
@Composable
fun FocalCardScreen(
    onBack: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val coroutineScope = rememberCoroutineScope()

    val user = repository.currentUser
    val childProfile = repository.childrenList.find { it.userId == user.id || it.id == user.id }
    val pairedCard = repository.nfcCards.firstOrNull { it.cardId == childProfile?.pairedCardId }
        ?: repository.nfcCards.firstOrNull()
    val isCardPaired = pairedCard != null
    val cardId = pairedCard?.cardId ?: ""

    val parent = repository.controllerUser
    val parentDisplayName = if (parent.name.isBlank()) "Priya Sharma" else parent.name
    val relation = "Mom"

    var showTestCardSheet by remember { mutableStateOf(false) }

    DetailScaffold(
        title = "FOCAL card",
        subtitle = "Your card starts and ends focus.",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Centered FocalCardHero (upright 0° tilt, ~260dp wide, soft ambient glow, ringCount = 0)
            FocalCardHero(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                cardWidth = 260.dp,
                cardHeight = 162.dp,
                logoSize = 72.dp,
                tiltAngle = 0f,
                ringCount = 0
            )

            Spacer(Modifier.height(16.dp))

            if (!isCardPaired) {
                // Empty state: "No card yet" + "Add a card"
                Text(
                    text = "No card yet",
                    style = typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Tap a card on this phone to pair it with your account.",
                    style = typography.caption.copy(color = ChildColors.Secondary, textAlign = TextAlign.Center),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(24.dp))

                ChildPrimaryButton(
                    text = "Add a card",
                    leadingIcon = {
                        Icon(
                            imageVector = FocalIcons.Nfc,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { showTestCardSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // StatusPill: "● Paired & active"
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, ChildColors.BorderStrong, CircleShape)
                        .background(ChildColors.BannerBg)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "Paired & active",
                        style = typography.caption.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }

                Spacer(Modifier.height(20.dp))

                val linkedBy: String = if (!pairedCard.ownerName.isNullOrBlank()) pairedCard.ownerName!! else "$parentDisplayName ($relation)"
                val lastUsedText: String = if (pairedCard.lastUsed.isNotBlank()) {
                    if (!pairedCard.lastUsedChildName.isNullOrBlank()) "${pairedCard.lastUsed} · ${pairedCard.lastUsedChildName}" else pairedCard.lastUsed
                } else {
                    "Not used yet"
                }

                InfoCard(
                    rows = listOf(
                        InfoCardRow("Card ID", pairedCard.cardId),
                        InfoCardRow("Linked by", linkedBy),
                        InfoCardRow("Last used", lastUsedText)
                    )
                )

                Spacer(Modifier.height(16.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 18.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconCircle(size = 44.dp) {
                            Icon(
                                imageVector = FocalIcons.InfoCircleAlert,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lost your card?",
                                style = typography.body.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Ask your parent to replace it in their app. The old card stops working right away.",
                                style = typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = ChildColors.Secondary
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                ChildSecondaryButton(
                    text = "Test my card",
                    leadingIcon = {
                        Icon(
                            imageVector = FocalIcons.Nfc,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { showTestCardSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showTestCardSheet) {
            var readerState by remember {
                mutableStateOf(
                    if (PlatformPermissionsHelper.isNfcEnabled())
                        com.focal.app.ui.child.components.ReaderState.LISTENING_USB
                    else
                        com.focal.app.ui.child.components.ReaderState.NO_READER
                )
            }

            com.focal.app.ui.child.components.TapCardBottomSheet(
                isEndingFocus = false,
                overlineText = if (pairedCard == null) "ADD CARD" else "TEST CARD",
                readerState = readerState,
                onDismiss = { showTestCardSheet = false },
                onCardScanned = {
                    if (pairedCard == null || repository.nfcCards.isEmpty()) {
                        val newCard = repository.writeAndRegisterBlankCard(
                            repository.controllerUser.id,
                            repository.controllerUser.name
                        )
                        childProfile?.let { cp ->
                            val idx = repository.childrenList.indexOfFirst { it.id == cp.id }
                            if (idx >= 0) {
                                repository.childrenList[idx] = cp.copy(pairedCardId = newCard.cardId)
                            }
                        }
                    }
                    readerState = com.focal.app.ui.child.components.ReaderState.SUCCESS
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(1000)
                        showTestCardSheet = false
                    }
                },
                onTestWrongCard = {
                    readerState = com.focal.app.ui.child.components.ReaderState.WRONG_CARD
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(1500)
                        if (readerState == com.focal.app.ui.child.components.ReaderState.WRONG_CARD) {
                            readerState = com.focal.app.ui.child.components.ReaderState.LISTENING_USB
                        }
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3 — PROTECTION
// -------------------------------------------------------------
data class PermissionItem(
    val title: String,
    val subtitle: String,
    val isRequired: Boolean,
    val icon: ImageVector,
    val isGranted: () -> Boolean,
    val onFix: () -> Unit
)

@Composable
fun ProtectionScreen(
    onBack: () -> Unit
) {
    val typography = LocalChildTypography.current
    var refreshKey by remember { mutableStateOf(0) }

    // Periodically re-check permission statuses when user returns from Settings
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            refreshKey++
        }
    }

    val deviceAdminLauncher = rememberDeviceAdminLauncher { isGranted ->
        refreshKey++
    }

    val permissions = remember(refreshKey) {
        listOf(
            PermissionItem(
                title = "Accessibility service",
                subtitle = "Blocks apps and websites",
                isRequired = true,
                icon = FocalIcons.Accessibility,
                isGranted = { PlatformPermissionsHelper.isAccessibilityEnabled() },
                onFix = {
                    PlatformPermissionsHelper.openAccessibilitySettings()
                    refreshKey++
                }
            ),
            PermissionItem(
                title = "Display over other apps",
                subtitle = "Shows the block screen",
                isRequired = true,
                icon = FocalIcons.DisplayOverlay,
                isGranted = { PlatformPermissionsHelper.isOverlayEnabled() },
                onFix = {
                    PlatformPermissionsHelper.openOverlaySettings()
                    refreshKey++
                }
            ),
            PermissionItem(
                title = "Device admin",
                subtitle = "Stops uninstalling FOCAL",
                isRequired = true,
                icon = FocalIcons.ShieldCheck,
                isGranted = { PlatformPermissionsHelper.isDeviceAdminActive() },
                onFix = {
                    deviceAdminLauncher()
                    refreshKey++
                }
            ),
            PermissionItem(
                title = "Usage access",
                subtitle = "Screen time in Insights",
                isRequired = false,
                icon = FocalIcons.Chart,
                isGranted = { PlatformPermissionsHelper.isUsageAccessEnabled() },
                onFix = {
                    PlatformPermissionsHelper.openUsageAccessSettings()
                    refreshKey++
                }
            ),
            PermissionItem(
                title = "Notifications",
                subtitle = "Focus and schedule alerts",
                isRequired = false,
                icon = FocalIcons.Bell,
                isGranted = { PlatformPermissionsHelper.isNotificationPermissionGranted() },
                onFix = {
                    PlatformPermissionsHelper.openNotificationSettings()
                    refreshKey++
                }
            ),
            PermissionItem(
                title = "Battery unrestricted",
                subtitle = "Keeps FOCAL running",
                isRequired = false,
                icon = FocalIcons.Battery,
                isGranted = { PlatformPermissionsHelper.isBatteryOptimizationDisabled() },
                onFix = {
                    PlatformPermissionsHelper.openBatteryOptimizationSettings()
                    refreshKey++
                }
            )
        )
    }

    val grantedCount = permissions.count { it.isGranted() }
    val totalCount = permissions.size
    val fixCount = totalCount - grantedCount
    val missingRequiredCount = permissions.count { it.isRequired && !it.isGranted() }
    val allRequiredGranted = missingRequiredCount == 0

    DetailScaffold(
        title = "Protection",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Status GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 20.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Faint radial glow top-right if protection is on
                    if (allRequiredGranted) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                                    center = Offset(size.width, 0f),
                                    radius = 120.dp.toPx()
                                ),
                                radius = 120.dp.toPx(),
                                center = Offset(size.width, 0f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 56dp circle badge
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .then(
                                    if (allRequiredGranted) {
                                        Modifier
                                            .drawBehind {
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                                                        center = center,
                                                        radius = size.width / 1.4f
                                                    ),
                                                    radius = size.width / 1.4f,
                                                    center = center
                                                )
                                            }
                                            .background(ChildColors.IconCircleBg)
                                            .border(1.5.dp, Color.White, CircleShape)
                                    } else {
                                        Modifier
                                            .background(ChildColors.IconCircleBg)
                                            .border(1.5.dp, ChildColors.BorderStrong, CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.ShieldCheck,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (allRequiredGranted) "Protection is on" else "Protection is off",
                                style = typography.headline.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allRequiredGranted) Color.White else ChildColors.Danger
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = if (allRequiredGranted) {
                                    "$grantedCount of $totalCount on" + if (fixCount > 0) " · $fixCount to fix" else ""
                                } else {
                                    "$missingRequiredCount needed to block apps"
                                },
                                style = typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = ChildColors.Secondary
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            SectionOverline(text = "PERMISSIONS")

            // Permissions IconRowCard
            IconRowCard(
                items = permissions.map { perm ->
                    val granted = perm.isGranted()
                    IconRowItem(
                        icon = perm.icon,
                        title = perm.title,
                        subtitle = perm.subtitle,
                        trailingContent = {
                            if (granted) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = FocalIcons.Check,
                                        contentDescription = null,
                                        tint = ChildColors.Secondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "On",
                                        style = typography.caption.copy(
                                            fontSize = 14.sp,
                                            color = ChildColors.Secondary
                                        )
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .wrapContentWidth()
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .clickable(onClick = perm.onFix)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Fix",
                                        style = typography.caption.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    )
                }
            )

            Spacer(Modifier.height(24.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FocalIcons.Lock,
                    contentDescription = null,
                    tint = ChildColors.Tertiary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Turning these off tells your parent",
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Tertiary
                    )
                )
            }
        }
    }
}

// =============================================================
// STEP 4C — PROFILE DETAILS: NOTIFICATIONS + HELP + PRIVACY + ABOUT
// =============================================================

// TODO: Replace placeholder support email and legal URLs with production endpoints
const val SUPPORT_EMAIL = "support@example.com"
const val PRIVACY_POLICY_URL = "https://focal.family/privacy"
const val TERMS_OF_SERVICE_URL = "https://focal.family/terms"

// -------------------------------------------------------------
// SCREEN 1 — NOTIFICATIONS
// -------------------------------------------------------------
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val typography = LocalChildTypography.current
    val isNotificationPermissionGranted by remember {
        mutableStateOf(PlatformPermissionsHelper.isNotificationPermissionGranted())
    }

    var reminderScheduleStarting by remember {
        mutableStateOf(FocalLocalStorage.getBoolean("pref_reminder_schedule_starting", true))
    }
    var reminderFocusFinished by remember {
        mutableStateOf(FocalLocalStorage.getBoolean("pref_reminder_focus_finished", true))
    }
    var reminderStreak by remember {
        mutableStateOf(FocalLocalStorage.getBoolean("pref_reminder_streak", false))
    }

    DetailScaffold(
        title = "Notifications",
        subtitle = "Choose what FOCAL reminds you about.",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            if (!isNotificationPermissionGranted) {
                Spacer(Modifier.height(8.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 18.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Notifications are off",
                            style = typography.body.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Turn on notifications so FOCAL can remind you about schedules and focus sessions.",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary,
                                lineHeight = 18.sp
                            )
                        )
                        Spacer(Modifier.height(14.dp))
                        ChildSecondaryButton(
                            text = "Turn on",
                            onClick = {
                                PlatformPermissionsHelper.markSetupPageOpened()
                                PlatformPermissionsHelper.openNotificationSettings()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            val remindersAlpha = if (isNotificationPermissionGranted) 1f else 0.4f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = remindersAlpha }
            ) {
                SectionOverline(
                    text = "REMINDERS",
                    modifier = Modifier.fillMaxWidth()
                )

                IconRowCard(
                    items = listOf(
                        IconRowItem(
                            icon = FocalIcons.Calendar,
                            title = "Schedule starting",
                            subtitle = "5 minutes before a schedule",
                            trailingContent = {
                                FocalSwitch(
                                    checked = reminderScheduleStarting,
                                    enabled = isNotificationPermissionGranted,
                                    onCheckedChange = {
                                        reminderScheduleStarting = it
                                        FocalLocalStorage.saveBoolean("pref_reminder_schedule_starting", it)
                                    }
                                )
                            }
                        ),
                        IconRowItem(
                            icon = FocalIcons.Check,
                            title = "Focus finished",
                            subtitle = "When a session ends",
                            trailingContent = {
                                FocalSwitch(
                                    checked = reminderFocusFinished,
                                    enabled = isNotificationPermissionGranted,
                                    onCheckedChange = {
                                        reminderFocusFinished = it
                                        FocalLocalStorage.saveBoolean("pref_reminder_focus_finished", it)
                                    }
                                )
                            }
                        ),
                        IconRowItem(
                            icon = FocalIcons.Lightning,
                            title = "Streak reminder",
                            subtitle = "Evening nudge if no focus today",
                            trailingContent = {
                                FocalSwitch(
                                    checked = reminderStreak,
                                    enabled = isNotificationPermissionGranted,
                                    onCheckedChange = {
                                        reminderStreak = it
                                        FocalLocalStorage.saveBoolean("pref_reminder_streak", it)
                                    }
                                )
                            }
                        )
                    )
                )
            }

            SectionOverline(
                text = "FROM FOCAL",
                modifier = Modifier.fillMaxWidth()
            )

            IconRowCard(
                items = listOf(
                    IconRowItem(
                        icon = FocalIcons.Shield,
                        title = "Parent replies",
                        subtitle = "Exit approved or declined",
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Lock,
                                    contentDescription = null,
                                    tint = ChildColors.Tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Always on",
                                    style = typography.caption.copy(
                                        fontSize = 14.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    ),
                    IconRowItem(
                        icon = FocalIcons.InfoCircleAlert,
                        title = "Protection alerts",
                        subtitle = "If a permission is off",
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Lock,
                                    contentDescription = null,
                                    tint = ChildColors.Tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Always on",
                                    style = typography.caption.copy(
                                        fontSize = 14.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    )
                )
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FocalIcons.Lock,
                    contentDescription = null,
                    tint = ChildColors.Tertiary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Important alerts can't be turned off",
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Tertiary
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2 — HELP & SUPPORT
// -------------------------------------------------------------
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val typography = LocalChildTypography.current
    var expandedFaqIndex by remember { mutableStateOf<Int?>(0) }

    val faqs = remember {
        listOf(
            "Why can't I open Settings?" to "During focus, Settings is locked so focus can't be turned off. Ask your parent for a 10-minute unlock if you really need it.",
            "How do I end focus early?" to "Tap your FOCAL card again, or send your parent an emergency exit request from End focus.",
            "My card isn't working" to "Hold the middle of the card flat against the back of the phone for a second. Use Test my card in FOCAL card. If it still fails, ask your parent.",
            "Why is this app blocked?" to "Your parent chose which apps and websites are blocked during focus. They are open again when focus ends.",
            "How does my streak work?" to "Finish at least one focus session without ending it early. Do it every day to grow your streak. Missing a day resets it to 0."
        )
    }

    DetailScaffold(
        title = "Help & support",
        subtitle = "Quick answers to common questions.",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SectionOverline(
                text = "FAQS",
                modifier = Modifier.fillMaxWidth()
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    faqs.forEachIndexed { index, (question, answer) ->
                        val isExpanded = expandedFaqIndex == index

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec = tween<androidx.compose.ui.unit.IntSize>(150))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedFaqIndex = if (isExpanded) null else index
                                    }
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = question,
                                    style = typography.body.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2
                                )
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    imageVector = if (isExpanded) FocalIcons.ChevronUp else FocalIcons.ChevronDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = ChildColors.Tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (isExpanded) {
                                Text(
                                    text = answer,
                                    style = typography.caption.copy(
                                        fontSize = 13.sp,
                                        color = ChildColors.Secondary,
                                        lineHeight = 19.5.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                                )
                            }
                        }

                        if (index < faqs.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(ChildColors.ListDivider)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Contact GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 18.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(size = 44.dp) {
                        Icon(
                            imageVector = FocalIcons.Mail,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Still need help?",
                            style = typography.body.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Ask your parent, or contact FOCAL support",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            ),
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            ChildSecondaryButton(
                text = "Contact support",
                leadingIcon = {
                    Icon(
                        imageVector = FocalIcons.Mail,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    val ver = PlatformPermissionsHelper.getAppVersionName()
                    PlatformPermissionsHelper.openEmailSupport(
                        email = SUPPORT_EMAIL,
                        subject = "FOCAL help — v$ver"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3 — PRIVACY
// -------------------------------------------------------------
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
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

    DetailScaffold(
        title = "Privacy",
        subtitle = "What FOCAL shares with your parent, and what stays private.",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SectionOverline(
                text = "YOUR PARENT CAN SEE",
                modifier = Modifier.fillMaxWidth()
            )

            PrivacyChecklistCard(
                items = parentCanSee,
                isAllowed = true
            )

            SectionOverline(
                text = "FOCAL NEVER SHARES",
                modifier = Modifier.fillMaxWidth()
            )

            PrivacyChecklistCard(
                items = neverShares,
                isAllowed = false
            )

            Spacer(Modifier.height(16.dp))

            IconRowCard(
                items = listOf(
                    IconRowItem(
                        icon = FocalIcons.Shield,
                        title = "Privacy policy",
                        subtitle = "Full details",
                        trailingContent = {
                            Icon(
                                imageVector = FocalIcons.ChevronRight,
                                contentDescription = null,
                                tint = ChildColors.Tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            PlatformPermissionsHelper.openUrl(PRIVACY_POLICY_URL)
                        }
                    )
                )
            )
        }
    }
}

@Composable
fun PrivacyChecklistCard(
    items: List<String>,
    isAllowed: Boolean,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        radius = 24.dp,
        padding = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, itemText ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAllowed) {
                        Icon(
                            imageVector = FocalIcons.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = FocalIcons.Close,
                            contentDescription = null,
                            tint = ChildColors.Tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = itemText,
                        style = typography.body.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        maxLines = 2
                    )
                }
                if (index < items.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp)
                            .height(1.dp)
                            .background(ChildColors.ListDivider)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 4 — ABOUT
// -------------------------------------------------------------
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val typography = LocalChildTypography.current
    val versionName = remember { PlatformPermissionsHelper.getAppVersionName() }
    val versionCode = remember { PlatformPermissionsHelper.getAppVersionCode() }
    var showLicensesDialog by remember { mutableStateOf(false) }

    DetailScaffold(
        title = "",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))

            // Centered FOCAL logo (96dp with soft radial glow)
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                                center = center,
                                radius = size.width / 1.5f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.focal_logo),
                    contentDescription = "FOCAL Logo",
                    modifier = Modifier.size(96.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "FOCAL",
                style = typography.title.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.5.sp,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Version $versionName ($versionCode)",
                style = typography.caption.copy(
                    fontSize = 13.sp,
                    color = ChildColors.Tertiary
                )
            )

            Spacer(Modifier.height(36.dp))

            IconRowCard(
                items = listOf(
                    IconRowItem(
                        icon = FocalIcons.Document,
                        title = "Terms of service",
                        trailingContent = {
                            Icon(
                                imageVector = FocalIcons.ChevronRight,
                                contentDescription = null,
                                tint = ChildColors.Tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            PlatformPermissionsHelper.openUrl(TERMS_OF_SERVICE_URL)
                        }
                    ),
                    IconRowItem(
                        icon = FocalIcons.Shield,
                        title = "Privacy policy",
                        trailingContent = {
                            Icon(
                                imageVector = FocalIcons.ChevronRight,
                                contentDescription = null,
                                tint = ChildColors.Tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            PlatformPermissionsHelper.openUrl(PRIVACY_POLICY_URL)
                        }
                    ),
                    IconRowItem(
                        icon = FocalIcons.Code,
                        title = "Open-source licences",
                        trailingContent = {
                            Icon(
                                imageVector = FocalIcons.ChevronRight,
                                contentDescription = null,
                                tint = ChildColors.Tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            showLicensesDialog = true
                        }
                    )
                )
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            Text(
                text = "© 2026 FOCAL",
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = ChildColors.Tertiary
                )
            )
        }
    }

    if (showLicensesDialog) {
        OpenSourceLicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

@Composable
fun OpenSourceLicensesDialog(
    onDismiss: () -> Unit
) {
    val typography = LocalChildTypography.current
    val licenses = listOf(
        "Android Jetpack Compose" to "Apache License 2.0\nCopyright 2024 The Android Open Source Project",
        "Kotlin Multiplatform & Coroutines" to "Apache License 2.0\nCopyright 2010-2024 JetBrains s.r.o.",
        "Geist Font Family" to "SIL Open Font License 1.1\nCopyright (c) 2023 Vercel, Inc.",
        "Material Icons & Vector Graphics" to "Apache License 2.0\nCopyright Google LLC"
    )

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            radius = 24.dp,
            padding = 20.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(
                    text = "Open-source licences",
                    style = typography.title.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(Modifier.height(16.dp))
                licenses.forEach { (lib, lic) ->
                    Text(
                        text = lib,
                        style = typography.body.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = lic,
                        style = typography.caption.copy(
                            fontSize = 12.sp,
                            color = ChildColors.Secondary
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(8.dp))
                ChildSecondaryButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 9. PARENT UNLOCK SCREEN (SCREEN 2)
// -------------------------------------------------------------
@Composable
fun ParentUnlockScreen(
    onClose: () -> Unit,
    onUnlockSuccess: () -> Unit = onClose,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var showCardSheet by remember { mutableStateOf(false) }

    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            while (lockoutSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                lockoutSeconds -= 1
            }
            errorMessage = null
        }
    }

    fun triggerWrongPin() {
        coroutineScope.launch {
            for (i in 0 until 3) {
                shakeOffset.animateTo(12f, tween(40))
                shakeOffset.animateTo(-12f, tween(40))
            }
            shakeOffset.animateTo(0f, tween(40))
            enteredPin = ""
            failedAttempts += 1
            if (failedAttempts >= 5) {
                lockoutSeconds = 300
                errorMessage = "Too many attempts"
            } else {
                errorMessage = "Wrong PIN, try again"
            }
        }
    }

    fun triggerSuccess() {
        isSuccess = true
        errorMessage = null
        repository.unlockParentGuardFor10Minutes()
        coroutineScope.launch {
            kotlinx.coroutines.delay(800)
            onUnlockSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: 44dp round close button top-left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ChildColors.Surface)
                            .border(1.dp, ChildColors.Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Center Content: Lock Icon Badge + Title + Subtitle + PIN Dots + Error
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 64dp circle (#171717, #2A2A2A border) with open-lock icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ChildColors.IconCircleBg)
                        .border(1.dp, ChildColors.BorderStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccess) FocalIcons.Check else FocalIcons.LockOpen,
                        contentDescription = "Parent Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Parent unlock",
                    style = typography.title.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter the parent PIN or tap the parent's FOCAL card. Protection pauses for 10 minutes.",
                    style = typography.body.copy(
                        color = ChildColors.Secondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 290.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PIN dots (4 dots, 14dp size, 16dp gap) or Success message
                if (isSuccess) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = FocalIcons.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Unlocked for 10 minutes",
                            style = typography.headline.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.offset(x = shakeOffset.value.dp)
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (isFilled) Color.White else Color.Transparent)
                                    .then(
                                        if (!isFilled) Modifier.border(1.5.dp, ChildColors.Tertiary, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Text under dots: Lockout or Error message
                Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                    if (lockoutSeconds > 0) {
                        Text(
                            text = "Try again in ${lockoutSeconds}s",
                            style = typography.caption.copy(color = ChildColors.Secondary)
                        )
                    } else if (errorMessage != null && !isSuccess) {
                        Text(
                            text = errorMessage!!,
                            style = typography.caption.copy(color = ChildColors.Secondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Keypad 3x4: 62dp tall pill keys (#0E0E0E, #222 border, 24sp medium)
                val isKeypadEnabled = lockoutSeconds == 0 && !isSuccess
                val keyRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "back")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in keyRows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            for (key in row) {
                                when (key) {
                                    "" -> {
                                        Spacer(modifier = Modifier.size(width = 72.dp, height = 62.dp))
                                    }
                                    "back" -> {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 62.dp)
                                                .clickable(enabled = isKeypadEnabled && enteredPin.isNotEmpty()) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        errorMessage = null
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = FocalIcons.Backspace,
                                                contentDescription = "Backspace",
                                                tint = if (isKeypadEnabled && enteredPin.isNotEmpty()) Color.White else ChildColors.Tertiary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        val shape = RoundedCornerShape(31.dp)
                                        Box(
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 62.dp)
                                                .clip(shape)
                                                .background(ChildColors.BannerBg)
                                                .border(1.dp, ChildColors.Border, shape)
                                                .clickable(enabled = isKeypadEnabled && enteredPin.length < 4) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (enteredPin.length < 4) {
                                                        val next = enteredPin + key
                                                        enteredPin = next
                                                        errorMessage = null
                                                        if (next.length == 4) {
                                                            val activePin = repository.parentPin.ifBlank { "1234" }
                                                            if (next == activePin) {
                                                                triggerSuccess()
                                                            } else {
                                                                triggerWrongPin()
                                                            }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = key,
                                                style = typography.title.copy(
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isKeypadEnabled) Color.White else ChildColors.Tertiary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom action: SecondaryButton "Tap parent's card instead"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                ChildSecondaryButton(
                    text = "Tap parent's card instead",
                    leadingIcon = {
                        Icon(
                            imageVector = FocalIcons.Nfc,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = { showCardSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Tap Card Bottom Sheet with overline "PARENT UNLOCK"
        if (showCardSheet) {
            var readerState by remember {
                mutableStateOf(
                    if (PlatformPermissionsHelper.isNfcEnabled())
                        com.focal.app.ui.child.components.ReaderState.LISTENING_USB
                    else
                        com.focal.app.ui.child.components.ReaderState.NO_READER
                )
            }

            com.focal.app.ui.child.components.TapCardBottomSheet(
                isEndingFocus = false,
                overlineText = "PARENT UNLOCK",
                readerState = readerState,
                onDismiss = { showCardSheet = false },
                onCardScanned = {
                    readerState = com.focal.app.ui.child.components.ReaderState.SUCCESS
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(600)
                        showCardSheet = false
                        triggerSuccess()
                    }
                },
                onTestWrongCard = {
                    readerState = com.focal.app.ui.child.components.ReaderState.WRONG_CARD
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(1200)
                        showCardSheet = false
                        errorMessage = "This card isn't the parent's card"
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// 10. CHILD PROFILE SCREEN (SCREEN 1)
// -------------------------------------------------------------
@Composable
fun ChildProfileScreen(
    onNavigateSubScreen: (ChildSubScreen) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val scrollState = rememberScrollState()

    val user = repository.currentUser
    val displayName = if (user.name.contains("Rahul", ignoreCase = true)) "Aarav" else user.name
    val displayInitials = com.focal.app.ui.child.components.getAvatarInitials(displayName)

    val parent = repository.controllerUser
    val parentDisplayName = if (parent.name.isBlank()) "Priya Sharma" else parent.name
    val parentInitials = com.focal.app.ui.child.components.getAvatarInitials(parentDisplayName)

    val streakDays = remember { StreakRepository.instance.getStreak().coerceAtLeast(3) }
    val focusTodayMinutes = remember { 135 } // 2h 15m
    val isUsageGranted = PlatformPermissionsHelper.isUsageAccessEnabled()
    val screenTimeMinutes = remember(isUsageGranted) {
        if (isUsageGranted) PlatformPermissionsHelper.getRealDailyScreenTimeMinutes().coerceAtLeast(192) else 0
    }

    val isParentUnlockActive = repository.isParentGuardUnlocked()
    val unlockMinsLeft = ((repository.parentUnlockedUntilMs - currentTimeMillis()) / 60000L).coerceAtLeast(1)

    val allPermsOn = PlatformPermissionsHelper.isAccessibilityEnabled() &&
            PlatformPermissionsHelper.isOverlayEnabled() &&
            PlatformPermissionsHelper.isUsageAccessEnabled() &&
            PlatformPermissionsHelper.isNotificationPermissionGranted() &&
            PlatformPermissionsHelper.isBatteryOptimizationDisabled() &&
            PlatformPermissionsHelper.isExactAlarmPermissionGranted() &&
            PlatformPermissionsHelper.isDeviceAdminActive()

    val childProfile = repository.childrenList.find { it.userId == user.id || it.id == user.id }
    val pairedCard = repository.nfcCards.firstOrNull { it.cardId == childProfile?.pairedCardId }
        ?: repository.nfcCards.firstOrNull()
    val cardIdText = pairedCard?.cardId ?: ""

    val isIos = remember {
        com.example.myapplication.getPlatform().name.contains("iOS", ignoreCase = true)
    }

    ChildTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChildColors.Background)
        ) {
            // GlowArcs top-right behind the header
            GlowArcs(modifier = Modifier.align(Alignment.TopEnd))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Title "Profile" (34sp bold)
                Text(
                    text = "Profile",
                    style = typography.display.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Profile GlassCard
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 18.dp
                ) {
                    // Top row: Avatar initials 68dp · name + subtitle · streak StatusPill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 68dp avatar initials
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(ChildColors.IconCircleBg)
                                .border(1.dp, ChildColors.BorderStrong, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayInitials,
                                style = typography.title.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = typography.title.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Class 10 · Student",
                                style = typography.caption.copy(
                                    color = ChildColors.Secondary,
                                    fontSize = 13.sp
                                ),
                                maxLines = 2
                            )
                        }

                        // Streak StatusPill
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                                .background(Color.Transparent)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Lightning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (streakDays > 0) "$streakDays day streak" else "Start a streak",
                                    style = typography.caption.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // 1dp horizontal divider
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 18.dp),
                        color = ChildColors.Border,
                        thickness = 1.dp
                    )

                    // 2 columns split by a vertical divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Focus today
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${focusTodayMinutes / 60}h ${focusTodayMinutes % 60}m",
                                style = typography.title.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Focus today",
                                style = typography.caption.copy(
                                    color = ChildColors.Secondary,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        // Vertical divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(ChildColors.Border)
                        )

                        // Right: Screen time today
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 18.dp)
                        ) {
                            Text(
                                text = if (!isUsageGranted) "—" else "${screenTimeMinutes / 60}h ${screenTimeMinutes % 60}m",
                                style = typography.title.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Screen time today",
                                style = typography.caption.copy(
                                    color = ChildColors.Secondary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Parent GlassCard
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateSubScreen(ChildSubScreen.CONNECTED_PARENT) },
                    radius = 24.dp,
                    padding = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Parent initials avatar 44dp
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ChildColors.IconCircleBg)
                                .border(1.dp, ChildColors.BorderStrong, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = parentInitials,
                                style = typography.headline.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connected to parent",
                                style = typography.caption.copy(
                                    color = ChildColors.Secondary,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$parentDisplayName (Mom)",
                                style = typography.headline.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                ),
                                maxLines = 2
                            )
                        }

                        // Right: white dot + "Linked"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "Linked",
                                style = typography.body.copy(
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Settings GlassCard
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 0.dp
                ) {
                    // Parent unlock row
                    ProfileSettingsRow(
                        icon = FocalIcons.Lock,
                        title = "Parent unlock",
                        subtitle = if (isParentUnlockActive) "Unlocked · $unlockMinsLeft min left" else "Parent PIN or card · 10 minutes",
                        onClick = { onNavigateSubScreen(ChildSubScreen.PARENT_UNLOCK) }
                    )

                    IndentedDivider()

                    // FOCAL card row
                    ProfileSettingsRow(
                        icon = FocalIcons.Card,
                        title = "FOCAL card",
                        subtitle = if (cardIdText.isNotBlank()) "$cardIdText · Paired & active" else "No card yet",
                        onClick = { onNavigateSubScreen(ChildSubScreen.FOCAL_CARD) }
                    )

                    IndentedDivider()

                    // Protection row: RED if protection is off!
                    ProfileSettingsRow(
                        icon = FocalIcons.ShieldCheck,
                        title = "Protection",
                        subtitle = if (repository.isDeviceProtectionActive()) "All permissions on" else "Protection is off",
                        subtitleColor = if (repository.isDeviceProtectionActive()) ChildColors.Secondary else ChildColors.Danger,
                        onClick = { onNavigateSubScreen(ChildSubScreen.PROTECTION) }
                    )

                    // iOS ONLY: Choose apps on this iPhone
                    if (isIos) {
                        IndentedDivider()
                        ProfileSettingsRow(
                            icon = FocalIcons.Lock,
                            title = "Choose apps on this iPhone",
                            subtitle = "Apps your parent picked",
                            onClick = {}
                        )
                    }

                    IndentedDivider()

                    // Notifications row
                    ProfileSettingsRow(
                        icon = FocalIcons.Bell,
                        title = "Notifications",
                        subtitle = "Session reminders",
                        onClick = { onNavigateSubScreen(ChildSubScreen.NOTIFICATIONS) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Second GlassCard: Help & support, Privacy, About
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 0.dp
                ) {
                    // Help & support row
                    ProfileSettingsRow(
                        icon = FocalIcons.HelpCircle,
                        title = "Help & support",
                        subtitle = "FAQs and contact",
                        onClick = { onNavigateSubScreen(ChildSubScreen.HELP) }
                    )

                    IndentedDivider()

                    // Privacy row
                    ProfileSettingsRow(
                        icon = FocalIcons.Shield,
                        title = "Privacy",
                        subtitle = "What FOCAL shares with your parent",
                        onClick = { onNavigateSubScreen(ChildSubScreen.PRIVACY) }
                    )

                    IndentedDivider()

                    // About row
                    val verName = PlatformPermissionsHelper.getAppVersionName()
                    ProfileSettingsRow(
                        icon = FocalIcons.InfoCircle,
                        title = "About",
                        subtitle = "Version $verName · Terms",
                        onClick = { onNavigateSubScreen(ChildSubScreen.ABOUT) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer: small lock icon + "Unlinking this phone needs your parent"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FocalIcons.Lock,
                        contentDescription = null,
                        tint = ChildColors.Tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Unlinking this phone needs your parent",
                        style = typography.caption.copy(
                            color = ChildColors.Tertiary,
                            fontSize = 13.sp
                        )
                    )
                }

                // 110dp clearance so content clears the floating bottom nav
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

@Composable
private fun ProfileSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    subtitleColor: Color = ChildColors.Secondary,
    onClick: () -> Unit
) {
    val typography = LocalChildTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ChildColors.IconCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.body.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = subtitleColor
                ),
                maxLines = 2
            )
        }

        Icon(
            imageVector = FocalIcons.ChevronRight,
            contentDescription = null,
            tint = ChildColors.Tertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun IndentedDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 76.dp, end = 18.dp)
            .height(1.dp)
            .background(ChildColors.ListDivider)
    )
}

