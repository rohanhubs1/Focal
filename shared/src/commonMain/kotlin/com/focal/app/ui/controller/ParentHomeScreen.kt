package com.focal.app.ui.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import com.focal.app.model.currentTimeMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.data.ActivityRepository
import com.focal.app.data.FocalRepository
import com.focal.app.model.ChildProfile
import com.focal.app.model.getLocalGreeting
import com.focal.app.ui.child.components.GlassCard
import com.focal.app.ui.child.components.GlowArcs
import com.focal.app.ui.child.components.PrimaryButton
import com.focal.app.ui.child.components.SecondaryButton
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography

/**
 * Tab 0: Parent Home Screen (PA1_home.png)
 */
@Composable
fun ParentHomeScreen(
    onChildSelected: (ChildProfile) -> Unit,
    onAddChildClick: () -> Unit,
    onCreatePolicyClick: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenActivityTab: (filter: String?) -> Unit = {},
    onOpenFocusTab: () -> Unit = {},
    onStartNowClick: () -> Unit = {},
    repository: FocalRepository = FocalRepository.instance,
    activityRepository: ActivityRepository = ActivityRepository.instance
) {
    val typography = LocalChildTypography.current
    val user = repository.currentUser
    val pendingEmergencyRequests = repository.emergencyRequests.filter { it.status == "PENDING" }
    val newestPendingRequest = pendingEmergencyRequests.maxByOrNull { it.requestedAtMillis }
    val hasUnreadEvents by activityRepository.hasUnread.collectAsState()
    val activeParentSession: com.focal.app.pairing.ParentPairingSession? by com.focal.app.pairing.PairingRepository.instance.activeParentSessionFlow.collectAsState()
    var showAllowPhoneSheet by remember { mutableStateOf(false) }
    var showStartFocusSheet by remember { mutableStateOf(false) }
    val tickerNow by produceState(com.focal.app.streak.getDeviceCurrentTimeMillis()) {
        while (true) {
            value = com.focal.app.streak.getDeviceCurrentTimeMillis()
            delay(1000 - value % 1000)
        }
    }
    val notificationPromptShown = remember {
        com.focal.app.data.FocalLocalStorage.getBoolean("KEY_PARENT_NOTIFICATION_PROMPT_SHOWN", false)
    }
    var showNotificationSheet by remember { mutableStateOf(!notificationPromptShown) }
    val isPairingWaiting = activeParentSession?.state == com.focal.app.pairing.ParentPairState.PENDING_APPROVAL
    val showUnreadDot = hasUnreadEvents || newestPendingRequest != null || isPairingWaiting

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(bottom = 110.dp)
        ) {
            // Header row: 48dp parent initials avatar · Greeting / First name · 44dp bell button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val parentInitials = remember(user.name) {
                        com.focal.app.ui.child.components.getAvatarInitials(user.name.ifBlank { "Priya Sharma" })
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171717))
                            .border(1.dp, Color(0xFF2A2A2A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = parentInitials,
                            style = typography.headline.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = getLocalGreeting(),
                            style = typography.caption.copy(
                                fontSize = 14.sp,
                                color = ChildColors.Secondary
                            )
                        )
                        val firstName = remember(user.name) {
                            user.name.trim().split(" ").firstOrNull()?.ifEmpty { "Priya" } ?: "Priya"
                        }
                        Text(
                            text = firstName,
                            style = typography.headline.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                // 44dp circular Bell button on right
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B0B0B))
                        .border(1.dp, Color(0xFF262626), CircleShape)
                        .clickable { onOpenActivityTab(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Bell,
                        contentDescription = "Activity",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    if (showUnreadDot) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-10).dp, y = 10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            // REQUEST CARD (only when something is waiting; newest first)
            if (newestPendingRequest != null) {
                Spacer(modifier = Modifier.height(20.dp))
                val req = newestPendingRequest
                val associatedChild = repository.childrenList.find { it.id == req.childId || it.name.equals(req.childName, ignoreCase = true) }
                val childInitials = remember(req.childName) {
                    val parts = req.childName.trim().split(" ").filter { it.isNotBlank() }
                    if (parts.size >= 2) "${parts.first().firstOrNull() ?: ""}${parts.last().firstOrNull() ?: ""}".uppercase()
                    else req.childName.take(2).uppercase().ifEmpty { "AR" }
                }
                val policyName = if (com.focal.app.engine.FocusEngine.instance.isFocusActive && com.focal.app.engine.FocusEngine.instance.currentSession != null) {
                    com.focal.app.engine.FocusEngine.instance.currentSession?.policyName ?: "Study Mode"
                } else {
                    associatedChild?.defaultPolicyId?.let { id ->
                        repository.policies.find { it.id == id }?.name
                    } ?: "Study Mode"
                }
                val realRemainingMinutes = when {
                    com.focal.app.engine.FocusEngine.instance.isFocusActive && com.focal.app.engine.FocusEngine.instance.remainingSeconds > 0 -> {
                        ((com.focal.app.engine.FocusEngine.instance.remainingSeconds + 59) / 60).toInt()
                    }
                    associatedChild != null && associatedChild.focusEndsAtMillis > com.focal.app.streak.getDeviceCurrentTimeMillis() -> {
                        (((associatedChild.focusEndsAtMillis - com.focal.app.streak.getDeviceCurrentTimeMillis()) + 59999L) / 60000L).toInt()
                    }
                    associatedChild != null && associatedChild.remainingMinutes > 0 -> {
                        associatedChild.remainingMinutes
                    }
                    else -> 1
                }
                val remainingLeft = "${realRemainingMinutes} min left"

                val timeLabel = remember(req.requestedAtMillis, req.requestedAt, tickerNow) {
                    if (req.requestedAtMillis > 0L) {
                        val diffMs = (tickerNow - req.requestedAtMillis).coerceAtLeast(0L)
                        val diffMins = diffMs / 60000L
                        when {
                            diffMins < 1L -> "Just now"
                            diffMins == 1L -> "1 min ago"
                            diffMins < 60L -> "$diffMins min ago"
                            else -> "${diffMins / 60L} hr ago"
                        }
                    } else {
                        req.requestedAt.ifBlank { "Just now" }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF131313),
                                    Color(0xFF0A0A0A)
                                )
                            )
                        )
                        .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Overline row: ● EMERGENCY EXIT REQUEST        timeLabel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EMERGENCY EXIT REQUEST",
                                    style = typography.caption.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.2.sp,
                                        color = Color(0xFFA3A3A3)
                                    )
                                )
                            }

                            Text(
                                text = timeLabel,
                                style = typography.caption.copy(
                                    fontSize = 12.sp,
                                    color = Color(0xFFA3A3A3)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Child info row: initials avatar · Name · "Reason" / Policy · min left
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF171717))
                                    .border(1.dp, Color(0xFF2A2A2A), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = childInitials,
                                    style = typography.body.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${req.childName} · \"${req.reason}\"",
                                    style = typography.headline.copy(
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$policyName · $remainingLeft",
                                    style = typography.caption.copy(
                                        fontSize = 13.sp,
                                        color = Color(0xFFA3A3A3)
                                    ),
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Two 44dp buttons: PrimaryButton("Allow exit") | SecondaryButton("Decline")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PrimaryButton(
                                text = "Allow exit",
                                onClick = {
                                    repository.resolveEmergencyRequest(req.id, true)
                                },
                                modifier = Modifier.weight(1f).height(44.dp)
                            )

                            SecondaryButton(
                                text = "Decline",
                                onClick = {
                                    repository.resolveEmergencyRequest(req.id, false)
                                },
                                modifier = Modifier.weight(1f).height(44.dp)
                            )
                        }

                        if (pendingEmergencyRequests.size > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "+${pendingEmergencyRequests.size - 1} more waiting",
                                    style = typography.caption.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFFA3A3A3)
                                    ),
                                    modifier = Modifier.clickable { onOpenActivityTab("Requests") }
                                )
                            }
                        }
                    }
                }
            } else if (isPairingWaiting && activeParentSession != null) {
                val pairSession: com.focal.app.pairing.ParentPairingSession = activeParentSession!!
                val childInitials = remember(pairSession.childName) {
                    val parts = pairSession.childName.trim().split(" ").filter { it.isNotBlank() }
                    if (parts.size >= 2) {
                        "${parts.first().firstOrNull() ?: ""}${parts.last().firstOrNull() ?: ""}".uppercase()
                    } else {
                        val sub = pairSession.childName.take(2).uppercase()
                        if (sub.isBlank()) "AR" else sub
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF131313),
                                    Color(0xFF0A0A0A)
                                )
                            )
                        )
                        .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Overline row: ● NEW PHONE        Just now
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NEW PHONE",
                                    style = typography.caption.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.2.sp,
                                        color = Color(0xFFA3A3A3)
                                    )
                                )
                            }

                            Text(
                                text = "Just now",
                                style = typography.caption.copy(
                                    fontSize = 12.sp,
                                    color = Color(0xFFA3A3A3)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Child info row: initials avatar · "{child}'s phone wants to connect" / "{manufacturer} · Android {ver}"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF171717))
                                    .border(1.dp, Color(0xFF2A2A2A), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = childInitials,
                                    style = typography.body.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${pairSession.childName}'s phone wants to connect",
                                    style = typography.headline.copy(
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${pairSession.deviceManufacturer} · Android ${pairSession.androidVersion}",
                                    style = typography.caption.copy(
                                        fontSize = 13.sp,
                                        color = Color(0xFFA3A3A3)
                                    ),
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Two 44dp buttons: PrimaryButton("Review") | SecondaryButton("Decline")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PrimaryButton(
                                text = "Review",
                                onClick = { showAllowPhoneSheet = true },
                                modifier = Modifier.weight(1f).height(44.dp)
                            )

                            SecondaryButton(
                                text = "Decline",
                                onClick = {
                                    com.focal.app.pairing.PairingRepository.instance.declineChildPairing(pairSession.childId)
                                },
                                modifier = Modifier.weight(1f).height(44.dp)
                            )
                        }
                    }
                }
            }

            if (showAllowPhoneSheet && activeParentSession != null && activeParentSession?.state == com.focal.app.pairing.ParentPairState.PENDING_APPROVAL) {
                AllowPhoneSheet(
                    session = activeParentSession!!,
                    onDismiss = { showAllowPhoneSheet = false },
                    onAllowed = {
                        showAllowPhoneSheet = false
                        val linked = repository.childrenList.find { it.id == activeParentSession?.childId }
                        if (linked != null) {
                            onChildSelected(linked)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section header: "Your children" + right "{n} linked"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your children",
                    style = typography.headline.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )

                Text(
                    text = "${repository.childrenList.size} linked",
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Secondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Children Section Rows
            if (repository.childrenList.isEmpty()) {
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
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .border(1.dp, Color(0xFF262626), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Users,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Add your first child",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect their phone in under a minute.",
                            style = typography.caption.copy(
                                fontSize = 13.5.sp,
                                color = ChildColors.Secondary
                            )
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        PrimaryButton(
                            text = "Add child",
                            onClick = onAddChildClick,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            } else {
                repository.childrenList.forEach { child ->
                    val initials = remember(child.name) {
                        com.focal.app.ui.child.components.getAvatarInitials(child.name)
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChildSelected(child) },
                        radius = 24.dp,
                        padding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 52dp initials avatar
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF171717))
                                    .border(1.dp, Color(0xFF2A2A2A), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = typography.headline.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = child.name,
                                    style = typography.headline.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = child.displayGrade,
                                    style = typography.caption.copy(
                                        fontSize = 12.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Status line with dot
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isProtectionOff = repository.isChildProtectionOff(child.id)
                                    if (isProtectionOff) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF453A))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Protection is off",
                                            style = typography.caption.copy(
                                                fontSize = 13.sp,
                                                color = Color(0xFFFF453A)
                                            ),
                                            maxLines = 2
                                        )
                                    } else if (child.deviceName.isBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6B6B6B))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Phone not connected yet",
                                            style = typography.caption.copy(
                                                fontSize = 13.sp,
                                                color = Color(0xFF6B6B6B)
                                            ),
                                            maxLines = 2
                                        )
                                    } else if (child.isFocusing) {
                                        val pName = repository.policies.find { it.id == child.defaultPolicyId }?.name ?: "Study Mode"
                                        val remSecs = if (child.focusEndsAtMillis > 0L) maxOf(0L, (child.focusEndsAtMillis - tickerNow) / 1000L) else (child.remainingMinutes * 60L).coerceAtLeast(0L)
                                        val remMins = (remSecs + 59) / 60
                                        if (remSecs > 0L) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Focusing · $pName · ${remMins}m left",
                                                style = typography.caption.copy(
                                                    fontSize = 13.sp,
                                                    color = Color(0xFFA3A3A3)
                                                ),
                                                maxLines = 2
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF6B6B6B))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val hours = child.todayFocusMinutes / 60
                                            val mins = child.todayFocusMinutes % 60
                                            val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                                            Text(
                                                text = "Not focusing · $timeStr today",
                                                style = typography.caption.copy(
                                                    fontSize = 13.sp,
                                                    color = Color(0xFFA3A3A3)
                                                ),
                                                maxLines = 2
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6B6B6B))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val hours = child.todayFocusMinutes / 60
                                        val mins = child.todayFocusMinutes % 60
                                        val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                                        Text(
                                            text = "Not focusing · $timeStr today",
                                            style = typography.caption.copy(
                                                fontSize = 13.sp,
                                                color = Color(0xFFA3A3A3)
                                            ),
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            // Chevron ›
                            Icon(
                                imageVector = FocalIcons.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF6B6B6B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Dashed "Add child" card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .drawBehind {
                        drawRoundRect(
                            color = Color(0xFF3A3A3A),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            ),
                            cornerRadius = CornerRadius(24.dp.toPx())
                        )
                    }
                    .clickable { onAddChildClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171717)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Add child",
                        style = typography.headline.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // "Start a focus session" GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Start a focus session",
                        style = typography.headline.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Lock a child's apps right now.",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            color = ChildColors.Secondary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PrimaryButton(
                        text = "Start focus",
                        leadingIcon = {
                            Icon(
                                imageVector = FocalIcons.Bolt,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { showStartFocusSheet = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    )
                }
            }
        }

        // Modal: StartFocusSheet
        if (showStartFocusSheet) {
            StartFocusSheet(
                preselectedChildId = null,
                onDismiss = { showStartFocusSheet = false },
                repository = repository
            )
        }

        // First-open notification permission sheet (once per install)
        if (showNotificationSheet) {
            ParentNotificationPermissionSheet(
                onAllow = {
                    com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_PARENT_NOTIFICATION_PROMPT_SHOWN", true)
                    com.focal.app.ui.components.PlatformPermissionsHelper.openNotificationSettings()
                    showNotificationSheet = false
                },
                onNotNow = {
                    com.focal.app.data.FocalLocalStorage.saveBoolean("KEY_PARENT_NOTIFICATION_PROMPT_SHOWN", true)
                    showNotificationSheet = false
                }
            )
        }
    }
}

@Composable
fun ParentNotificationPermissionSheet(
    onAllow: () -> Unit,
    onNotNow: () -> Unit
) {
    val typography = LocalChildTypography.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onNotNow) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF141414), Color(0xFF0A0A0A))
                    )
                )
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle bar pill
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF333333))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bell icon circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF171717))
                        .border(1.dp, Color(0xFF262626), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Bell,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Get alerts when your child needs you",
                    style = typography.headline.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Emergency exit requests, new phones and protection alerts.",
                    style = typography.body.copy(
                        fontSize = 14.sp,
                        color = ChildColors.Secondary,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                PrimaryButton(
                    text = "Allow",
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNotNow() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not now",
                        style = typography.caption.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ChildColors.Secondary
                        )
                    )
                }
            }
        }
    }
}


@Composable
fun ControllerDashboardScreen(
    onChildSelected: (ChildProfile) -> Unit,
    onAddChildClick: () -> Unit,
    onCreatePolicyClick: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenActivityTab: (filter: String?) -> Unit = {},
    onOpenFocusTab: () -> Unit = {},
    onStartNowClick: () -> Unit = {},
    repository: FocalRepository = FocalRepository.instance,
    activityRepository: ActivityRepository = ActivityRepository.instance
) {
    ParentHomeScreen(
        onChildSelected = onChildSelected,
        onAddChildClick = onAddChildClick,
        onCreatePolicyClick = onCreatePolicyClick,
        onOpenPermissions = onOpenPermissions,
        onOpenActivityTab = onOpenActivityTab,
        onOpenFocusTab = onOpenFocusTab,
        onStartNowClick = onStartNowClick,
        repository = repository,
        activityRepository = activityRepository
    )
}
