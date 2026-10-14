package com.focal.app.ui.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.data.ActivityEventType
import com.focal.app.data.ActivityRepository
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.ChildProfile
import com.focal.app.model.FocusMode
import com.focal.app.model.FocusPolicy
import com.focal.app.model.ScheduleItem
import com.focal.app.model.currentTimeMillis
import com.focal.app.ui.child.DetailScaffold
import com.focal.app.ui.child.InsightsScreen
import com.focal.app.ui.child.SectionOverline
import com.focal.app.ui.child.components.CountdownRing
import com.focal.app.ui.child.components.FocalSwitch
import com.focal.app.ui.child.components.GlassCard
import com.focal.app.ui.child.components.PrimaryButton
import com.focal.app.ui.child.components.SecondaryButton
import com.focal.app.ui.child.components.StatusPill
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography
import kotlinx.coroutines.launch

/**
 * Sub-screens accessible from ChildDetailScreen.
 */
enum class ChildDetailSubScreen {
    SCHEDULES,
    SCHEDULE_EDITOR,
    PHONE_AND_CARD,
    INSIGHTS
}

// =========================================================================
// SCREEN 1 — CHILD PAGE (PB1_child_detail.png)
// =========================================================================

@Composable
fun ChildDetailScreen(
    child: ChildProfile,
    onBack: () -> Unit,
    onConnectPhone: () -> Unit = {},
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current

    // Live child reference from repository
    val currentChild = repository.childrenList.find { it.id == child.id } ?: child

    // Subscreen navigation within child management
    var currentSubScreen by remember { mutableStateOf<ChildDetailSubScreen?>(null) }
    var editingSchedule by remember { mutableStateOf<ScheduleItem?>(null) }

    // Dialog & sheet states
    var showStartFocusSheet by remember { mutableStateOf(false) }
    var showEndFocusConfirmDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showEditNameClassDialog by remember { mutableStateOf(false) }
    var showUnlinkConfirmDialog by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var showPolicyPickerSheet by remember { mutableStateOf(false) }
    var showDurationSheet by remember { mutableStateOf(false) }

    // 1-second ticker for live timer
    var tickerNow by remember { mutableStateOf(currentTimeMillis()) }
    LaunchedEffect(currentChild.isFocusing) {
        if (currentChild.isFocusing) {
            while (true) {
                delay(1000)
                tickerNow = currentTimeMillis()
            }
        }
    }

    val liveRemainingSecs = remember(tickerNow, currentChild.isFocusing, currentChild.focusEndsAtMillis, currentChild.remainingMinutes) {
        if (!currentChild.isFocusing) 0L
        else if (currentChild.focusEndsAtMillis > 0L) {
            maxOf(0L, (currentChild.focusEndsAtMillis - tickerNow) / 1000L)
        } else {
            (currentChild.remainingMinutes * 60L).coerceAtLeast(0L)
        }
    }
    val isActuallyFocusing = currentChild.isFocusing && liveRemainingSecs > 0L
    val totalFocusSecs = (currentChild.focusTotalMinutes * 60L).coerceAtLeast(1L)
    val progressVal = if (isActuallyFocusing) (liveRemainingSecs.toFloat() / totalFocusSecs.toFloat()).coerceIn(0f, 1f) else 0f
    val remMins = liveRemainingSecs / 60
    val remSecs = liveRemainingSecs % 60
    val timeText = "${remMins}:${if (remSecs < 10) "0$remSecs" else "$remSecs"}"

    when (currentSubScreen) {
        ChildDetailSubScreen.SCHEDULES -> {
            ChildSchedulesScreen(
                child = currentChild,
                onBack = { currentSubScreen = null },
                onEditSchedule = { sched ->
                    editingSchedule = sched
                    currentSubScreen = ChildDetailSubScreen.SCHEDULE_EDITOR
                },
                repository = repository
            )
        }

        ChildDetailSubScreen.SCHEDULE_EDITOR -> {
            ScheduleEditorScreen(
                child = currentChild,
                schedule = editingSchedule,
                onBack = { currentSubScreen = ChildDetailSubScreen.SCHEDULES },
                onSaved = { currentSubScreen = ChildDetailSubScreen.SCHEDULES },
                onDeleted = { currentSubScreen = ChildDetailSubScreen.SCHEDULES },
                repository = repository
            )
        }

        ChildDetailSubScreen.PHONE_AND_CARD -> {
            PhoneAndCardScreen(
                child = currentChild,
                onBack = { currentSubScreen = null },
                repository = repository
            )
        }

        ChildDetailSubScreen.INSIGHTS -> {
            InsightsScreen(
                childId = currentChild.id,
                onBack = { currentSubScreen = null }
            )
        }

        null -> {
            // Main ChildDetailScreen UI matching PB1_child_detail.png exactly
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar: Back (left) + "···" (right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 44dp circular back button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B0B0B))
                                .border(1.dp, Color(0xFF262626), CircleShape)
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 44dp circular "···" button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B0B0B))
                                .border(1.dp, Color(0xFF262626), CircleShape)
                                .clickable { showOverflowMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.MoreHorizontal,
                                contentDescription = "More options",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Initials Avatar (76dp)
                    val initials = remember(currentChild.name) {
                        com.focal.app.ui.child.components.getAvatarInitials(currentChild.name)
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171717))
                            .border(1.dp, Color(0xFF2A2A2A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = typography.display.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Name (24sp bold white)
                    Text(
                        text = currentChild.name,
                        style = typography.display.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 2
                    )

                    Spacer(Modifier.height(4.dp))

                    // Subtitle: "{class} · {manufacturer} · Android {ver}"
                    val subtitleText = remember(currentChild) {
                        val parts = mutableListOf<String>()
                        if (currentChild.displayGrade.isNotBlank()) parts.add(currentChild.displayGrade)
                        if (currentChild.deviceName.isNotBlank()) {
                            // Extract manufacturer or device name
                            val firstWord = currentChild.deviceName.split(" ", "·").firstOrNull() ?: currentChild.deviceName
                            parts.add(firstWord)
                        }
                        parts.add("Android 14")
                        parts.joinToString(" · ")
                    }

                    Text(
                        text = subtitleText,
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            color = ChildColors.Secondary
                        ),
                        maxLines = 2
                    )

                    Spacer(Modifier.height(20.dp))

                    // STATUS CARD (GlassCard, radius 24dp)
                    val isProtectionOff = repository.isChildProtectionOff(currentChild.id)
                    val isPhoneNotConnected = currentChild.deviceName.isBlank()

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        radius = 24.dp,
                        padding = 20.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isActuallyFocusing) {
                                // ● FOCUSING state
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF0E0E0E))
                                        .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "FOCUSING",
                                            style = typography.overline.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                // CountdownRing (150dp)
                                CountdownRing(
                                    progress = progressVal,
                                    modifier = Modifier.size(150.dp),
                                    strokeWidth = 8.dp,
                                    centerContent = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = timeText,
                                                style = typography.display.copy(
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontFeatureSettings = "tnum"
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "LEFT",
                                                style = typography.overline.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ChildColors.Secondary,
                                                    letterSpacing = 1.5.sp
                                                )
                                            )
                                        }
                                    }
                                )

                                Spacer(Modifier.height(18.dp))

                                val policyName = repository.policies.find { it.id == currentChild.defaultPolicyId }?.name ?: "Study Mode"
                                Text(
                                    text = "$policyName · started with card · ends 12:00 PM",
                                    style = typography.caption.copy(
                                        fontSize = 13.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    maxLines = 2
                                )

                                Spacer(Modifier.height(16.dp))

                                SecondaryButton(
                                    text = "End focus now",
                                    onClick = { showEndFocusConfirmDialog = true },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                )
                            } else if (isPhoneNotConnected) {
                                // Phone not connected yet state
                                Text(
                                    text = "Phone not connected yet",
                                    style = typography.headline.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "Connect ${currentChild.name}'s phone to enable protection.",
                                    style = typography.caption.copy(
                                        fontSize = 13.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(16.dp))

                                SecondaryButton(
                                    text = "Connect phone",
                                    onClick = onConnectPhone,
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            } else {
                                // NOT FOCUSING state
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF0E0E0E))
                                        .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6B6B6B))
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "NOT FOCUSING",
                                            style = typography.overline.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }

                                val isChildUnlocked = repository.isParentGuardUnlocked()
                                val nowMs = com.focal.app.model.currentTimeMillis()
                                val unlockMinsLeft = ((repository.parentUnlockedUntilMs - nowMs) / 60000L).coerceAtLeast(1)

                                if (isChildUnlocked) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Unlocked on ${currentChild.name}'s phone · $unlockMinsLeft min left",
                                            style = typography.caption.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            ),
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (com.focal.app.config.FocalConfig.isDemoMode) {
                                            Text(
                                                text = "Lock now",
                                                style = typography.caption.copy(
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        repository.clearParentUnlock()
                                                        com.focal.app.protection.ProtectionStateRepository.instance.clearParentUnlock()
                                                        PlatformPermissionsHelper.showToast("Protection is back on")
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                } else if (isProtectionOff) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Protection is off on this phone",
                                        style = typography.caption.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFF453A)
                                        )
                                    )
                                } else {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Last focus: 2 hours ago",
                                        style = typography.caption.copy(
                                            fontSize = 13.sp,
                                            color = ChildColors.Secondary
                                        )
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                PrimaryButton(
                                    text = "Start focus",
                                    onClick = { showStartFocusSheet = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // TWO STAT CARDS (Side-by-Side GlassCards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Focus Today Card
                        val focusHours = currentChild.todayFocusMinutes / 60
                        val focusMins = currentChild.todayFocusMinutes % 60
                        val focusTimeStr = if (focusHours > 0) "${focusHours}h ${focusMins}m" else "${focusMins}m"

                        GlassCard(
                            modifier = Modifier.weight(1f),
                            radius = 20.dp,
                            padding = 16.dp
                        ) {
                            Column {
                                Text(
                                    text = focusTimeStr,
                                    style = typography.headline.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Focus today",
                                    style = typography.caption.copy(
                                        fontSize = 12.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    maxLines = 2
                                )
                            }
                        }

                        // Screen Time Today Card
                        val screenHours = currentChild.screenTimeMinutes / 60
                        val screenMins = currentChild.screenTimeMinutes % 60
                        val screenTimeStr = if (currentChild.screenTimeMinutes > 0) {
                            if (screenHours > 0) "${screenHours}h ${screenMins}m" else "${screenMins}m"
                        } else "—"

                        GlassCard(
                            modifier = Modifier.weight(1f),
                            radius = 20.dp,
                            padding = 16.dp
                        ) {
                            Column {
                                Text(
                                    text = screenTimeStr,
                                    style = typography.headline.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Screen time today",
                                    style = typography.caption.copy(
                                        fontSize = 12.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // CAPABILITIES & SETTINGS CARD (GlassCard with 5 rows)
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        radius = 24.dp,
                        padding = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Row 1: Default policy
                            val currentPolicy = repository.policies.find { it.id == currentChild.defaultPolicyId } ?: repository.policies.first()
                            ChildSettingRow(
                                icon = FocalIcons.Shield,
                                title = "Default policy",
                                subtitle = currentPolicy.name,
                                onClick = { showPolicyPickerSheet = true }
                            )

                            ChildDivider()

                            // Row 2: Default focus time
                            val focusHoursVal = currentChild.defaultFocusMinutes / 60
                            val focusMinsVal = currentChild.defaultFocusMinutes % 60
                            val focusTimeLabel = if (focusHoursVal > 0 && focusMinsVal > 0) "${focusHoursVal}h ${focusMinsVal}m"
                            else if (focusHoursVal > 0) "${focusHoursVal}h"
                            else "${focusMinsVal}m"

                            ChildSettingRow(
                                icon = FocalIcons.Clock,
                                title = "Default focus time",
                                subtitle = focusTimeLabel,
                                onClick = { showDurationSheet = true }
                            )

                            ChildDivider()

                            // Row 3: Schedules
                            val activeSchedulesCount = repository.schedules.size
                            val schedulesSubtitle = if (activeSchedulesCount > 0) "$activeSchedulesCount schedules · next at 4 PM" else "No schedules"
                            ChildSettingRow(
                                icon = FocalIcons.Calendar,
                                title = "Schedules",
                                subtitle = schedulesSubtitle,
                                onClick = { currentSubScreen = ChildDetailSubScreen.SCHEDULES }
                            )

                            ChildDivider()

                            // Row 4: Insights
                            ChildSettingRow(
                                icon = FocalIcons.Chart,
                                title = "Insights",
                                subtitle = "Focus, screen time, blocked apps",
                                onClick = { currentSubScreen = ChildDetailSubScreen.INSIGHTS }
                            )

                            ChildDivider()

                            // Row 5: Phone & card
                            val phoneCardSubtitle = if (isProtectionOff) "Protection is off" else "Protected · family card works here"
                            val isPhoneCardRed = isProtectionOff

                            ChildSettingRow(
                                icon = FocalIcons.Smartphone,
                                title = "Phone & card",
                                subtitle = phoneCardSubtitle,
                                isSubtitleRed = isPhoneCardRed,
                                onClick = { currentSubScreen = ChildDetailSubScreen.PHONE_AND_CARD }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: End Focus Early Confirmation Dialog
    if (showEndFocusConfirmDialog) {
        Dialog(onDismissRequest = { showEndFocusConfirmDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                radius = 24.dp,
                padding = 24.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "End focus early?",
                        style = typography.headline.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "This will unlock ${currentChild.name}'s apps immediately.",
                        style = typography.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryButton(
                            text = "Cancel",
                            onClick = { showEndFocusConfirmDialog = false },
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                        PrimaryButton(
                            text = "End focus",
                            onClick = {
                                showEndFocusConfirmDialog = false
                                FocusEngine.instance.remoteStopFocus(currentChild.id)
                                ActivityRepository.instance.recordEvent(
                                    type = ActivityEventType.FOCUS_ENDED,
                                    childId = currentChild.id,
                                    details = "Ended by parent",
                                    metadata = mapOf("source" to "you")
                                )
                                PlatformPermissionsHelper.showToast("Focus ended on ${currentChild.name}'s phone")
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal: StartFocusSheet
    if (showStartFocusSheet) {
        StartFocusSheet(
            preselectedChildId = currentChild.id,
            onDismiss = { showStartFocusSheet = false },
            repository = repository
        )
    }

    // Overflow Menu Sheet (···)
    if (showOverflowMenu) {
        ChildOverflowMenuSheet(
            child = currentChild,
            onDismiss = { showOverflowMenu = false },
            onEditNameClass = {
                showOverflowMenu = false
                showEditNameClassDialog = true
            },
            onUnlinkPhone = {
                showOverflowMenu = false
                showUnlinkConfirmDialog = true
            },
            onRemoveChild = {
                showOverflowMenu = false
                showRemoveConfirmDialog = true
            }
        )
    }

    // Dialog: Edit Name & Class
    if (showEditNameClassDialog) {
        EditChildNameClassDialog(
            child = currentChild,
            onDismiss = { showEditNameClassDialog = false },
            onSave = { updated ->
                repository.updateChild(updated)
                showEditNameClassDialog = false
                PlatformPermissionsHelper.showToast("Updated ${updated.name}'s info")
            }
        )
    }

    // Dialog: Confirm Unlink Phone
    if (showUnlinkConfirmDialog) {
        Dialog(onDismissRequest = { showUnlinkConfirmDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                radius = 24.dp,
                padding = 24.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unlink ${currentChild.name}'s phone?",
                        style = typography.headline.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${currentChild.name}'s phone will stop being protected.",
                        style = typography.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF453A))
                                .clickable {
                                    showUnlinkConfirmDialog = false
                                    repository.unlinkChildDevice(currentChild.id)
                                    PlatformPermissionsHelper.showToast("Unlinked ${currentChild.name}'s phone")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Unlink phone",
                                style = typography.headline.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        SecondaryButton(
                            text = "Cancel",
                            onClick = { showUnlinkConfirmDialog = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }
        }
    }

    // Dialog: Confirm Remove Child
    if (showRemoveConfirmDialog) {
        Dialog(onDismissRequest = { showRemoveConfirmDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                radius = 24.dp,
                padding = 24.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Remove ${currentChild.name}?",
                        style = typography.headline.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${currentChild.name}'s phone will stop being protected and their profile will be removed.",
                        style = typography.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF453A))
                                .clickable {
                                    showRemoveConfirmDialog = false
                                    repository.removeChild(currentChild.id)
                                    PlatformPermissionsHelper.showToast("Removed ${currentChild.name}")
                                    onBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Remove child",
                                style = typography.headline.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        SecondaryButton(
                            text = "Cancel",
                            onClick = { showRemoveConfirmDialog = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }
        }
    }

    // Sheet: PolicyPickerSheet
    if (showPolicyPickerSheet) {
        PolicyPickerSheet(
            selectedPolicyId = currentChild.defaultPolicyId,
            onDismiss = { showPolicyPickerSheet = false },
            onPolicySelected = { selected ->
                repository.updateChild(currentChild.copy(defaultPolicyId = selected.id))
                showPolicyPickerSheet = false
                PlatformPermissionsHelper.showToast("Default policy set to ${selected.name}")
            },
            repository = repository
        )
    }

    // Sheet: DurationSheet
    if (showDurationSheet) {
        DurationSheet(
            currentDurationMinutes = currentChild.defaultFocusMinutes,
            onDismiss = { showDurationSheet = false },
            onDurationSelected = { mins ->
                repository.updateChild(currentChild.copy(defaultFocusMinutes = mins))
                showDurationSheet = false
                val hours = mins / 60
                val remM = mins % 60
                val lbl = if (hours > 0 && remM > 0) "${hours}h ${remM}m" else if (hours > 0) "${hours}h" else "${remM}m"
                PlatformPermissionsHelper.showToast("Default focus time set to $lbl")
            }
        )
    }
}

// -------------------------------------------------------------------------
// Helper Row and Divider Components
// -------------------------------------------------------------------------

@Composable
private fun ChildSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSubtitleRed: Boolean = false,
    onClick: () -> Unit
) {
    val typography = LocalChildTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 44dp circular badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF171717)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.headline.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 2
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = if (isSubtitleRed) Color(0xFFFF453A) else ChildColors.Secondary
                ),
                maxLines = 2
            )
        }

        Icon(
            imageVector = FocalIcons.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF6B6B6B),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ChildDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 76.dp)
            .height(1.dp)
            .background(Color(0xFF1F1F1F))
    )
}

// =========================================================================
// SCREEN 2 — START FOCUS SHEET (PB2_start_focus_sheet.png)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartFocusSheet(
    preselectedChildId: String? = null,
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Multi-select children state
    val selectedChildrenIds = remember {
        mutableStateListOf<String>().apply {
            if (preselectedChildId != null) {
                add(preselectedChildId)
            }
        }
    }

    // Policy selection: default to first selected child's default policy or first policy
    var selectedPolicy by remember {
        val defaultId = repository.childrenList.find { it.id == preselectedChildId }?.defaultPolicyId
        val initialPolicy = repository.policies.find { it.id == defaultId } ?: repository.policies.first()
        mutableStateOf(initialPolicy)
    }

    // Duration selection: "30m", "1h", "1h 30m", "2h", "Custom"
    val durationOptions = listOf(30, 60, 90, 120)
    var selectedDurationMinutes by remember { mutableIntStateOf(90) }
    var isCustomDurationSelected by remember { mutableStateOf(false) }
    var customDurationMinutes by remember { mutableIntStateOf(165) } // default 2h 45m

    val activeDuration = if (isCustomDurationSelected) customDurationMinutes else selectedDurationMinutes
    val durationFormatted = remember(activeDuration) {
        val h = activeDuration / 60
        val m = activeDuration % 60
        if (h > 0 && m > 0) "${h}h ${m}m"
        else if (h > 0) "${h}h"
        else "${m}m"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3A3A3A))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF141414), Color(0xFF0A0A0A))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Title & Subtitle
            Text(
                text = "Start focus",
                style = typography.display.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Their phone locks right away.",
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(20.dp))

            // OVERLINE: CHILD
            SectionOverline("CHILD")

            Spacer(Modifier.height(10.dp))

            // Horizontal row of child avatars with checkmark badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repository.childrenList.forEach { childItem ->
                    val isSelected = selectedChildrenIds.contains(childItem.id)
                    val isConnected = childItem.deviceName.isNotBlank()
                    val childInitials = remember(childItem.name) {
                        val parts = childItem.name.trim().split(" ").filter { it.isNotBlank() }
                        if (parts.size >= 2) "${parts.first().firstOrNull() ?: ""}${parts.last().firstOrNull() ?: ""}".uppercase()
                        else childItem.name.take(2).uppercase().ifEmpty { "CH" }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = isConnected) {
                                if (isSelected) {
                                    selectedChildrenIds.remove(childItem.id)
                                } else {
                                    selectedChildrenIds.add(childItem.id)
                                }
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717).copy(alpha = if (isConnected) 1f else 0.4f))
                                .border(1.dp, Color(0xFF2A2A2A).copy(alpha = if (isConnected) 1f else 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = childInitials,
                                style = typography.headline.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = if (isConnected) 1f else 0.4f)
                                )
                            )

                            // Checkmark badge bottom-right
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = FocalIcons.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = childItem.name,
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else ChildColors.Secondary.copy(alpha = if (isConnected) 1f else 0.4f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // OVERLINE: POLICY
            SectionOverline("POLICY")

            Spacer(Modifier.height(10.dp))

            // Policy selection card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    repository.policies.forEachIndexed { index, policyItem ->
                        val isSelected = selectedPolicy.id == policyItem.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPolicy = policyItem }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Custom radio button matching PB2_start_focus_sheet.png
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) Color.White else Color(0xFF3A3A3A),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = policyItem.name,
                                    style = typography.headline.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = "${policyItem.blockedApps.size} apps · ${policyItem.blockedWebsites.size} websites",
                                    style = typography.caption.copy(
                                        fontSize = 13.sp,
                                        color = ChildColors.Secondary
                                    )
                                )
                            }
                        }

                        if (index < repository.policies.lastIndex) {
                            ChildDivider()
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // OVERLINE: HOW LONG
            SectionOverline("HOW LONG")

            Spacer(Modifier.height(10.dp))

            // Single line horizontal chips: 30m · 1h · 1h 30m · 2h · Custom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                durationOptions.forEach { mins ->
                    val isSelected = !isCustomDurationSelected && selectedDurationMinutes == mins
                    val label = when (mins) {
                        30 -> "30m"
                        60 -> "1h"
                        90 -> "1h 30m"
                        120 -> "2h"
                        else -> "${mins}m"
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color(0xFF0E0E0E))
                            .border(1.dp, if (isSelected) Color.White else Color(0xFF2A2A2A), CircleShape)
                            .clickable {
                                isCustomDurationSelected = false
                                selectedDurationMinutes = mins
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = typography.headline.copy(
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        )
                    }
                }

                // "Custom" chip
                val isCustomActive = isCustomDurationSelected
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isCustomActive) Color.White else Color(0xFF0E0E0E))
                        .border(1.dp, if (isCustomActive) Color.White else Color(0xFF2A2A2A), CircleShape)
                        .clickable { isCustomDurationSelected = true }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom",
                        style = typography.headline.copy(
                            fontSize = 14.sp,
                            fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCustomActive) Color.Black else Color.White
                        )
                    )
                }
            }

            // If Custom is selected, show 15-minute increment stepper
            if (isCustomDurationSelected) {
                Spacer(Modifier.height(14.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 18.dp,
                    padding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // - 15m
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                                .clickable {
                                    if (customDurationMinutes > 15) customDurationMinutes -= 15
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", style = typography.headline.copy(color = Color.White, fontSize = 20.sp))
                        }

                        Text(
                            text = durationFormatted,
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        // + 15m
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                                .clickable {
                                    if (customDurationMinutes < 480) customDurationMinutes += 15
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", style = typography.headline.copy(color = Color.White, fontSize = 20.sp))
                        }
                    }
                }
            }

            val unprotectedChildren = selectedChildrenIds
                .mapNotNull { id -> repository.childrenList.find { it.id == id } }
                .filter { repository.isChildProtectionOff(it.id) }

            if (unprotectedChildren.isNotEmpty()) {
                val childNames = unprotectedChildren.joinToString(" and ") { it.name }
                val warningText = "$childNames's phone isn't protected. Apps won't be blocked until protection is turned on."
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    radius = 16.dp,
                    padding = 14.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = FocalIcons.InfoCircleAlert,
                            contentDescription = "Alert",
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = warningText,
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary,
                                lineHeight = 18.sp
                            ),
                            maxLines = 2
                        )
                    }
                }
            }

            // Primary Button "Start for {duration}"
            val isEnabled = selectedChildrenIds.isNotEmpty()

            PrimaryButton(
                text = "Start for $durationFormatted",
                onClick = {
                    if (isEnabled) {
                        val namesList = mutableListOf<String>()
                        selectedChildrenIds.forEach { childId ->
                            FocusEngine.instance.remoteStartFocus(childId, selectedPolicy, activeDuration)
                            val c = repository.childrenList.find { it.id == childId }
                            val cName = c?.name ?: "Child"
                            namesList.add(cName)

                            ActivityRepository.instance.recordEvent(
                                type = ActivityEventType.FOCUS_STARTED,
                                childId = childId,
                                details = "${selectedPolicy.name} · $durationFormatted",
                                metadata = mapOf(
                                    "policy" to selectedPolicy.name,
                                    "duration" to durationFormatted,
                                    "source" to "you"
                                )
                            )

                            if (c != null && !c.isOnline) {
                                PlatformPermissionsHelper.showToast("$cName's phone is offline. Focus will start when it reconnects.")
                            }
                        }

                        if (namesList.isNotEmpty()) {
                            val namesStr = namesList.joinToString(" and ")
                            PlatformPermissionsHelper.showToast("Focus started on $namesStr's phone")
                        }
                        onDismiss()
                    }
                },
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Only you or the family card can end it early",
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF6B6B6B)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =========================================================================
// SCREEN 3 — CHILD SCHEDULES SCREEN (PB3_schedules.png)
// =========================================================================

@Composable
fun ChildSchedulesScreen(
    child: ChildProfile,
    onBack: () -> Unit,
    onEditSchedule: (ScheduleItem?) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header: Round Back Button
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0B))
                    .border(1.dp, Color(0xFF262626), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            // Title: "{child}'s schedules"
            Text(
                text = "${child.name}'s schedules",
                style = typography.display.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 2
            )

            Spacer(Modifier.height(4.dp))

            // Subtitle: "Focus starts and ends on its own at these times."
            Text(
                text = "Focus starts and ends on its own at these times.",
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(24.dp))

            // List of schedules
            if (repository.schedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Calendar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "No schedules yet",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Add one so focus starts on its own.",
                            style = typography.caption.copy(
                                fontSize = 13.5.sp,
                                color = ChildColors.Secondary
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(repository.schedules) { schedule ->
                        // Determine schedule icon based on title/mode
                        val scheduleIcon = remember(schedule.title, schedule.mode) {
                            when {
                                schedule.title.contains("Deep Work", ignoreCase = true) -> FocalIcons.Target
                                schedule.title.contains("Homework", ignoreCase = true) -> FocalIcons.Document
                                schedule.title.contains("Night", ignoreCase = true) || schedule.mode == FocusMode.SLEEP -> FocalIcons.Moon
                                schedule.title.contains("Morning", ignoreCase = true) -> FocalIcons.Sun
                                schedule.title.contains("Zen", ignoreCase = true) -> FocalIcons.Coffee
                                else -> FocalIcons.Clock
                            }
                        }

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditSchedule(schedule) },
                            radius = 24.dp,
                            padding = 18.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 44dp circular badge
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF171717)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = scheduleIcon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = schedule.title,
                                        style = typography.headline.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        ),
                                        maxLines = 2
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = "${schedule.days} · ${schedule.timeRange}",
                                        style = typography.caption.copy(
                                            fontSize = 13.sp,
                                            color = ChildColors.Secondary
                                        ),
                                        maxLines = 2
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = schedule.policyName.ifBlank { "Study Mode" },
                                        style = typography.caption.copy(
                                            fontSize = 13.sp,
                                            color = ChildColors.Secondary
                                        ),
                                        maxLines = 2
                                    )
                                }

                                // Interactive FocalSwitch
                                FocalSwitch(
                                    checked = schedule.isEnabled,
                                    onCheckedChange = {
                                        repository.toggleSchedule(schedule.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Pinned Bottom Action: + New schedule
            PrimaryButton(
                text = "+ New schedule",
                onClick = { onEditSchedule(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
                    .height(54.dp)
            )
        }
    }
}

// =========================================================================
// SCREEN 4 — SCHEDULE EDITOR SCREEN (PB4_schedule_editor.png)
// =========================================================================

@Composable
fun ScheduleEditorScreen(
    child: ChildProfile,
    schedule: ScheduleItem?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current

    val isEditing = schedule != null

    var nameInput by remember { mutableStateOf(schedule?.title ?: "") }
    var selectedDays by remember {
        mutableStateOf(
            if (schedule != null) {
                when (schedule.days) {
                    "Mon – Fri" -> setOf(0, 1, 2, 3, 4)
                    "Every day", "Every night" -> setOf(0, 1, 2, 3, 4, 5, 6)
                    else -> setOf(0, 1, 2, 3, 4)
                }
            } else {
                setOf(0, 1, 2, 3, 4) // Mon - Fri default
            }
        )
    }

    // Time states
    var startHour by remember { mutableIntStateOf(16) } // 4:00 PM
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(18) }   // 6:00 PM
    var endMinute by remember { mutableIntStateOf(0) }

    // Parse existing time range if available
    LaunchedEffect(schedule?.timeRange) {
        schedule?.timeRange?.let { tr ->
            val parts = tr.split("–", "-").map { it.trim() }
            if (parts.size == 2) {
                parse12HourTime(parts[0])?.let { (h, m) ->
                    startHour = h
                    startMinute = m
                }
                parse12HourTime(parts[1])?.let { (h, m) ->
                    endHour = h
                    endMinute = m
                }
            }
        }
    }

    val startTimeFormatted = formatTime12H(startHour, startMinute)
    val endTimeFormatted = formatTime12H(endHour, endMinute)
    val isEndsNextDay = (endHour < startHour) || (endHour == startHour && endMinute < startMinute)

    // Policy & Card exit
    var selectedPolicy by remember {
        val initialPolicy = if (schedule != null) {
            repository.policies.find { it.name.equals(schedule.policyName, ignoreCase = true) } ?: repository.policies.first()
        } else {
            repository.policies.first()
        }
        mutableStateOf(initialPolicy)
    }
    var allowCardExit by remember { mutableStateOf(schedule?.allowCardExit ?: false) }

    // Dialog states
    var showTimePickerForStart by remember { mutableStateOf(false) }
    var showTimePickerForEnd by remember { mutableStateOf(false) }
    var showPolicyPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Validation
    val isNameBlank = nameInput.trim().isBlank()
    val isNoDays = selectedDays.isEmpty()
    val isSameTime = startHour == endHour && startMinute == endMinute

    val validationError = when {
        isNameBlank -> "Schedule name is required"
        isNoDays -> "Select at least one day"
        isSameTime -> "Start and end times cannot be the same"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Round Back Button
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0B))
                    .border(1.dp, Color(0xFF262626), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            // Title: "New schedule" or "Edit schedule"
            Text(
                text = if (isEditing) "Edit schedule" else "New schedule",
                style = typography.display.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(20.dp))

            // SECTION: NAME
            SectionOverline("NAME")

            Spacer(Modifier.height(10.dp))

            // Name Input in GlassCard (radius 18dp)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 18.dp,
                padding = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (nameInput.isEmpty()) {
                        Text(
                            text = "e.g. Homework",
                            style = typography.body.copy(
                                fontSize = 16.sp,
                                color = ChildColors.Tertiary
                            )
                        )
                    }
                    BasicTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // SECTION: DAYS
            SectionOverline("DAYS")

            Spacer(Modifier.height(10.dp))

            // 7 Round 40dp toggles: M, T, W, T, F, S, S
            val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLetters.forEachIndexed { index, letter ->
                    val isDaySelected = selectedDays.contains(index)

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDaySelected) Color.White else Color.Transparent)
                            .border(1.dp, if (isDaySelected) Color.White else Color(0xFF2A2A2A), CircleShape)
                            .clickable {
                                selectedDays = if (isDaySelected) {
                                    selectedDays - index
                                } else {
                                    selectedDays + index
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDaySelected) Color.Black else Color.White
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // SECTION: TIME
            SectionOverline("TIME")

            Spacer(Modifier.height(10.dp))

            // Two side-by-side cards: Starts & Ends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Starts Card
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePickerForStart = true },
                    radius = 18.dp,
                    padding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "Starts",
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = startTimeFormatted,
                            style = typography.headline.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                // Ends Card
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePickerForEnd = true },
                    radius = 18.dp,
                    padding = 16.dp
                ) {
                    Column {
                        Text(
                            text = "Ends",
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = endTimeFormatted,
                            style = typography.headline.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        if (isEndsNextDay) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Ends next day",
                                style = typography.caption.copy(
                                    fontSize = 11.sp,
                                    color = ChildColors.Secondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // SECTION: DURING THIS TIME
            SectionOverline("DURING THIS TIME")

            Spacer(Modifier.height(10.dp))

            // GlassCard with Policy row & Card exit row
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Policy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPolicyPicker = true }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Policy",
                                style = typography.headline.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${selectedPolicy.name} · ${selectedPolicy.blockedApps.size} apps · ${selectedPolicy.blockedWebsites.size} websites",
                                style = typography.caption.copy(
                                    fontSize = 12.sp,
                                    color = ChildColors.Secondary
                                ),
                                maxLines = 2
                            )
                        }

                        Icon(
                            imageVector = FocalIcons.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF6B6B6B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    ChildDivider()

                    // Row 2: Card can end it early
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Nfc,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Card can end it early",
                                style = typography.headline.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (allowCardExit) "On · family card or QR can end it" else "Off · only you can end it",
                                style = typography.caption.copy(
                                    fontSize = 12.sp,
                                    color = ChildColors.Secondary
                                )
                            )
                        }

                        FocalSwitch(
                            checked = allowCardExit,
                            onCheckedChange = { allowCardExit = it }
                        )
                    }
                }
            }

            // Inline validation notice (in secondary gray, never red)
            if (validationError != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = validationError,
                    style = typography.caption.copy(
                        fontSize = 12.5.sp,
                        color = ChildColors.Secondary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))

            // PrimaryButton: Save schedule
            PrimaryButton(
                text = "Save schedule",
                onClick = {
                    if (validationError == null) {
                        val formattedDays = formatDaysSet(selectedDays, isEndsNextDay)
                        val newOrUpdated = ScheduleItem(
                            id = schedule?.id ?: "sch_${currentTimeMillis()}",
                            title = nameInput.trim(),
                            timeRange = "$startTimeFormatted – $endTimeFormatted",
                            days = formattedDays,
                            mode = selectedPolicy.mode,
                            isEnabled = schedule?.isEnabled ?: true,
                            appsCount = selectedPolicy.blockedApps.size,
                            websitesCount = selectedPolicy.blockedWebsites.size,
                            policyName = selectedPolicy.name,
                            allowCardExit = allowCardExit,
                            childId = child.id
                        )
                        repository.saveSchedule(newOrUpdated)
                        PlatformPermissionsHelper.showToast("Schedule saved")
                        onSaved()
                    }
                },
                enabled = validationError == null,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            )

            // Delete schedule (in edit mode)
            if (isEditing) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteConfirmDialog = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete schedule",
                        style = typography.headline.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF453A)
                        )
                    )
                }
            }
        }
    }

    // Time Picker Dialog for Start
    if (showTimePickerForStart) {
        DarkTimePickerDialog(
            title = "Start time",
            initialHour = startHour,
            initialMinute = startMinute,
            onDismiss = { showTimePickerForStart = false },
            onTimeSelected = { h, m ->
                startHour = h
                startMinute = m
                showTimePickerForStart = false
            }
        )
    }

    // Time Picker Dialog for End
    if (showTimePickerForEnd) {
        DarkTimePickerDialog(
            title = "End time",
            initialHour = endHour,
            initialMinute = endMinute,
            onDismiss = { showTimePickerForEnd = false },
            onTimeSelected = { h, m ->
                endHour = h
                endMinute = m
                showTimePickerForEnd = false
            }
        )
    }

    // Policy Picker Sheet
    if (showPolicyPicker) {
        PolicyPickerSheet(
            selectedPolicyId = selectedPolicy.id,
            onDismiss = { showPolicyPicker = false },
            onPolicySelected = {
                selectedPolicy = it
                showPolicyPicker = false
            },
            repository = repository
        )
    }

    // Confirm Delete Schedule Dialog
    if (showDeleteConfirmDialog && schedule != null) {
        Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                radius = 24.dp,
                padding = 24.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Delete this schedule?",
                        style = typography.headline.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${child.name} won't have this schedule anymore.",
                        style = typography.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF453A))
                                .clickable {
                                    showDeleteConfirmDialog = false
                                    repository.deleteSchedule(schedule.id)
                                    PlatformPermissionsHelper.showToast("Schedule deleted")
                                    onDeleted()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete",
                                style = typography.headline.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        SecondaryButton(
                            text = "Cancel",
                            onClick = { showDeleteConfirmDialog = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// SUB-SCREEN: PHONE & CARD (PhoneAndCardScreen)
// =========================================================================

@Composable
fun PhoneAndCardScreen(
    child: ChildProfile,
    onBack: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val isThisDevice = child.id == "child_1"
    val isProtectionOff = repository.isChildProtectionOff(child.id)

    DetailScaffold(
        title = "Phone & card",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Hardware Info GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    InfoRowItem(label = "Phone", value = child.deviceName.ifBlank { "Realme 12 Pro" })
                    ChildDivider()
                    InfoRowItem(label = "Android version", value = "Android 14")
                    ChildDivider()
                    InfoRowItem(label = "Last synced", value = if (child.isOnline) "2 min ago" else "Offline")
                    ChildDivider()
                    InfoRowItem(label = "FOCAL version", value = "1.0.0")
                }
            }

            Spacer(Modifier.height(24.dp))

            // OVERLINE: PROTECTION
            SectionOverline("PROTECTION")

            Spacer(Modifier.height(10.dp))

            // Permissions Read-only GlassCard (6 child permissions)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PermissionRowItem(
                        icon = FocalIcons.Accessibility,
                        title = "Accessibility service",
                        subtitle = "Blocks apps and websites",
                        isGranted = if (isThisDevice) PlatformPermissionsHelper.isAccessibilityEnabled() else !isProtectionOff
                    )
                    ChildDivider()
                    PermissionRowItem(
                        icon = FocalIcons.DisplayOverlay,
                        title = "Display over other apps",
                        subtitle = "Shows the block screen",
                        isGranted = if (isThisDevice) PlatformPermissionsHelper.isOverlayEnabled() else true
                    )
                    ChildDivider()
                    PermissionRowItem(
                        icon = FocalIcons.Shield,
                        title = "Device admin",
                        subtitle = "Stops uninstalling FOCAL",
                        isGranted = if (isThisDevice) PlatformPermissionsHelper.isDeviceAdminActive() else true
                    )
                    ChildDivider()
                    PermissionRowItem(
                        icon = FocalIcons.Chart,
                        title = "Usage access",
                        subtitle = "Screen time in Insights",
                        isGranted = if (isThisDevice) PlatformPermissionsHelper.isUsageAccessEnabled() else true
                    )
                    ChildDivider()
                    PermissionRowItem(
                        icon = FocalIcons.Bell,
                        title = "Notifications",
                        subtitle = "Focus and schedule alerts",
                        isGranted = if (isThisDevice) PlatformPermissionsHelper.isNotificationPermissionGranted() else true
                    )
                    ChildDivider()
                    PermissionRowItem(
                        icon = FocalIcons.Battery,
                        title = "Battery unrestricted",
                        subtitle = "Keeps FOCAL running",
                        isGranted = if (isThisDevice) PlatformPermissionsHelper.isBatteryOptimizationDisabled() else true
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Family Card & QR InfoCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    InfoRowItem(label = "Family card", value = "Works on this phone")
                    ChildDivider()
                    InfoRowItem(label = "Family QR", value = "Works")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Footer footnote with lock icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FocalIcons.Lock,
                    contentDescription = null,
                    tint = Color(0xFF6B6B6B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Only your parent can unlink this phone",
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF6B6B6B)
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoRowItem(label: String, value: String) {
    val typography = LocalChildTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = typography.caption.copy(
                fontSize = 13.5.sp,
                color = ChildColors.Secondary
            )
        )
        Text(
            text = value,
            style = typography.headline.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )
    }
}

@Composable
private fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    val typography = LocalChildTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF171717)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.headline.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = ChildColors.Secondary
                )
            )
        }

        if (isGranted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FocalIcons.Check,
                    contentDescription = "Granted",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "On",
                    style = typography.caption.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = ChildColors.Secondary
                    )
                )
            }
        } else {
            Text(
                text = "Off",
                style = typography.caption.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF453A)
                )
            )
        }
    }
}

// =========================================================================
// SUB-SHEET: POLICY PICKER SHEET
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyPickerSheet(
    selectedPolicyId: String,
    onDismiss: () -> Unit,
    onPolicySelected: (FocusPolicy) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3A3A3A))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Default policy",
                style = typography.display.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Choose which apps and websites to restrict.",
                style = typography.caption.copy(
                    fontSize = 13.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(18.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    repository.policies.forEachIndexed { index, pol ->
                        val isSelected = pol.id == selectedPolicyId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPolicySelected(pol) }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) Color.White else Color(0xFF3A3A3A),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pol.name,
                                    style = typography.headline.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${pol.blockedApps.size} apps · ${pol.blockedWebsites.size} websites",
                                    style = typography.caption.copy(
                                        fontSize = 13.sp,
                                        color = ChildColors.Secondary
                                    )
                                )
                            }
                        }

                        if (index < repository.policies.lastIndex) {
                            ChildDivider()
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SUB-SHEET: DURATION SHEET
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationSheet(
    currentDurationMinutes: Int,
    onDismiss: () -> Unit,
    onDurationSelected: (Int) -> Unit
) {
    val typography = LocalChildTypography.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val presets = listOf(30, 45, 60, 90, 120, 180)
    var selectedMinutes by remember { mutableIntStateOf(currentDurationMinutes) }
    var isCustom by remember { mutableStateOf(!presets.contains(currentDurationMinutes)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3A3A3A))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Default focus time",
                style = typography.display.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Sessions start with this duration unless changed.",
                style = typography.caption.copy(
                    fontSize = 13.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(18.dp))

            // Presets Grid / Flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.take(3).forEach { mins ->
                    val isSel = !isCustom && selectedMinutes == mins
                    val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (isSel) Color.White else Color(0xFF0E0E0E))
                            .border(1.dp, if (isSel) Color.White else Color(0xFF2A2A2A), CircleShape)
                            .clickable {
                                isCustom = false
                                selectedMinutes = mins
                                onDurationSelected(mins)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = typography.headline.copy(
                                fontSize = 14.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.Black else Color.White
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.drop(3).forEach { mins ->
                    val isSel = !isCustom && selectedMinutes == mins
                    val label = when (mins) {
                        90 -> "1h 30m"
                        120 -> "2h"
                        180 -> "3h"
                        else -> "${mins}m"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (isSel) Color.White else Color(0xFF0E0E0E))
                            .border(1.dp, if (isSel) Color.White else Color(0xFF2A2A2A), CircleShape)
                            .clickable {
                                isCustom = false
                                selectedMinutes = mins
                                onDurationSelected(mins)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = typography.headline.copy(
                                fontSize = 14.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.Black else Color.White
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Custom Stepper
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 18.dp,
                padding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171717))
                            .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                            .clickable {
                                isCustom = true
                                if (selectedMinutes > 15) {
                                    selectedMinutes -= 15
                                    onDurationSelected(selectedMinutes)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", style = typography.headline.copy(color = Color.White, fontSize = 20.sp))
                    }

                    val h = selectedMinutes / 60
                    val m = selectedMinutes % 60
                    val str = if (h > 0 && m > 0) "${h}h ${m}m" else if (h > 0) "${h}h" else "${m}m"

                    Text(
                        text = "Custom: $str",
                        style = typography.headline.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171717))
                            .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                            .clickable {
                                isCustom = true
                                if (selectedMinutes < 480) {
                                    selectedMinutes += 15
                                    onDurationSelected(selectedMinutes)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", style = typography.headline.copy(color = Color.White, fontSize = 20.sp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// OVERFLOW MENU SHEET (···)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildOverflowMenuSheet(
    child: ChildProfile,
    onDismiss: () -> Unit,
    onEditNameClass: () -> Unit,
    onUnlinkPhone: () -> Unit,
    onRemoveChild: () -> Unit
) {
    val typography = LocalChildTypography.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3A3A3A))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = child.name,
                style = typography.display.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Option 1: Edit name & class
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditNameClass() }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FocalIcons.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Edit name & class",
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                    }

                    ChildDivider()

                    // Option 2: Unlink phone (red)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUnlinkPhone() }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FocalIcons.Smartphone,
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Unlink phone",
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF453A)
                            )
                        )
                    }

                    ChildDivider()

                    // Option 3: Remove child (red)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRemoveChild() }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FocalIcons.Trash,
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Remove child",
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF453A)
                            )
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// DIALOG: EDIT NAME & CLASS
// =========================================================================

@Composable
fun EditChildNameClassDialog(
    child: ChildProfile,
    onDismiss: () -> Unit,
    onSave: (ChildProfile) -> Unit
) {
    val typography = LocalChildTypography.current
    var nameState by remember { mutableStateOf(child.name) }
    var classState by remember { mutableStateOf(child.grade.ifBlank { child.displayGrade }) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            radius = 24.dp,
            padding = 24.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Edit name & class",
                    style = typography.headline.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(Modifier.height(16.dp))

                SectionOverline("NAME")
                Spacer(Modifier.height(6.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 16.dp,
                    padding = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = nameState,
                            onValueChange = { nameState = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                SectionOverline("CLASS / GRADE")
                Spacer(Modifier.height(6.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 16.dp,
                    padding = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = classState,
                            onValueChange = { classState = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                    PrimaryButton(
                        text = "Save",
                        onClick = {
                            if (nameState.isNotBlank()) {
                                onSave(child.copy(name = nameState.trim(), grade = classState.trim()))
                            }
                        },
                        enabled = nameState.isNotBlank(),
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// DARK TIME PICKER DIALOG
// =========================================================================

@Composable
fun DarkTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val typography = LocalChildTypography.current

    var isPm by remember { mutableStateOf(initialHour >= 12) }
    var hour12 by remember {
        val h = if (initialHour % 12 == 0) 12 else initialHour % 12
        mutableIntStateOf(h)
    }
    var minute by remember { mutableIntStateOf(initialMinute) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            radius = 24.dp,
            padding = 24.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = typography.headline.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(Modifier.height(20.dp))

                // Time Display: HH : MM  AM/PM
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour Stepper
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .clickable {
                                    hour12 = if (hour12 >= 12) 1 else hour12 + 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▲", color = Color.White, fontSize = 12.sp)
                        }

                        Text(
                            text = if (hour12 < 10) "0$hour12" else "$hour12",
                            style = typography.display.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .clickable {
                                    hour12 = if (hour12 <= 1) 12 else hour12 - 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▼", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = ":",
                        style = typography.display.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    // Minute Stepper (15-min jumps)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .clickable {
                                    minute = (minute + 15) % 60
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▲", color = Color.White, fontSize = 12.sp)
                        }

                        Text(
                            text = if (minute < 10) "0$minute" else "$minute",
                            style = typography.display.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .clickable {
                                    minute = (minute - 15 + 60) % 60
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▼", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    // AM / PM Toggle Pills
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (!isPm) Color.White else Color(0xFF0E0E0E))
                                .border(1.dp, if (!isPm) Color.White else Color(0xFF2A2A2A), CircleShape)
                                .clickable { isPm = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "AM",
                                style = typography.headline.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isPm) Color.Black else Color.White
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isPm) Color.White else Color(0xFF0E0E0E))
                                .border(1.dp, if (isPm) Color.White else Color(0xFF2A2A2A), CircleShape)
                                .clickable { isPm = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "PM",
                                style = typography.headline.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPm) Color.Black else Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                    PrimaryButton(
                        text = "Set",
                        onClick = {
                            val computedHour = if (isPm) {
                                if (hour12 == 12) 12 else hour12 + 12
                            } else {
                                if (hour12 == 12) 0 else hour12
                            }
                            onTimeSelected(computedHour, minute)
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Time Formatting & Parsing Utilities
// -------------------------------------------------------------------------

private fun formatTime12H(hour: Int, minute: Int): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    val m = if (minute < 10) "0$minute" else "$minute"
    val amPm = if (hour >= 12) "PM" else "AM"
    return "$h:$m $amPm"
}

private fun parse12HourTime(str: String): Pair<Int, Int>? {
    val clean = str.trim()
    val isPm = clean.endsWith("PM", ignoreCase = true)
    val isAm = clean.endsWith("AM", ignoreCase = true)
    if (!isPm && !isAm) return null

    val timePart = clean.substring(0, clean.length - 2).trim()
    val parts = timePart.split(":")
    if (parts.size != 2) return null

    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null

    val hour24 = if (isPm) {
        if (h == 12) 12 else h + 12
    } else {
        if (h == 12) 0 else h
    }
    return hour24 to m
}

private fun formatDaysSet(days: Set<Int>, isOvernight: Boolean): String {
    return when {
        days.size == 7 -> if (isOvernight) "Every night" else "Every day"
        days == setOf(0, 1, 2, 3, 4) -> "Mon – Fri"
        days == setOf(5, 6) -> "Sat, Sun"
        else -> {
            val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            days.sorted().joinToString(", ") { names[it] }
        }
    }
}
