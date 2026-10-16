package com.focal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.EmergencyExitRequest
import com.focal.app.streak.getDeviceCurrentTimeMillis
import com.focal.app.theme.FocalColors
import com.focal.app.ui.child.ParentUnlockScreen
import com.focal.app.ui.child.components.GlassCard
import com.focal.app.ui.child.components.PrimaryButton as ChildPrimaryButton
import com.focal.app.ui.child.components.SecondaryButton as ChildSecondaryButton
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.ChildRadius
import com.focal.app.ui.theme.LocalChildTypography

/**
 * Common Block Screen Content matching Image 2 (P2_2_block_screen.png)
 */
@Composable
fun ChildBlockScreenContent(
    title: String,
    subtitle: String,
    icon: ImageVector = FocalIcons.Globe,
    isFocusActive: Boolean = true,
    blockedAttemptsCount: Int = 0,
    onBackToFocus: () -> Unit,
    onSecondaryAction: (() -> Unit)? = null,
    secondaryActionText: String = "Parent unlock"
) {
    val typography = LocalChildTypography.current
    val quotes = listOf(
        "Small focus now, big wins later.",
        "Stay in the zone. You've got this.",
        "One task at a time.",
        "Distractions are temporary. Focus is forever.",
        "Great things take uninterrupted time."
    )
    val quote = remember { quotes.first() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(40.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 180dp illustration frame: radial glow + 180dp thin ring + 104dp tile + 34dp lock badge
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val r = (size.width - 2.dp.toPx()) / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                                center = c,
                                radius = r
                            ),
                            center = c,
                            radius = r
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            center = c,
                            radius = r,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
                        )
                    }

                    // 104dp rounded-square tile (radius 30, #0E0E0E, 1dp #2A2A2A border)
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(ChildColors.BannerBg)
                            .border(1.dp, ChildColors.BorderStrong, RoundedCornerShape(30.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )

                        // 34dp white lock badge bottom-right (3dp black border, black lock glyph)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.dp, Color.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Lock,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Title (30sp bold, one line, ellipsize if long)
                Text(
                    text = title,
                    style = typography.display.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Spacer(Modifier.height(8.dp))

                // Subtitle: "{policy} · {min} min left" or "Protected by FOCAL · ask a parent"
                Text(
                    text = subtitle,
                    style = typography.body.copy(
                        fontSize = 15.sp,
                        color = ChildColors.Secondary
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Spacer(Modifier.height(36.dp))

                // GlassCard: motivational line + "Blocked {n} times this session"
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = ChildRadius.SmallCard,
                    padding = 18.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = quote,
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center
                        )
                        if (isFocusActive) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Blocked $blockedAttemptsCount times this session",
                                style = typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = ChildColors.Secondary
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                if (onSecondaryAction != null) {
                    ChildSecondaryButton(
                        text = secondaryActionText,
                        onClick = onSecondaryAction,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                }

                ChildPrimaryButton(
                    text = if (isFocusActive) "Back to focus" else "Go back",
                    onClick = onBackToFocus,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Restricted App Overlay displayed when a blocked application is launched during an active focus session.
 */
@Composable
fun BlockedAppOverlay(
    appName: String,
    onDismiss: () -> Unit,
    focusEngine: FocusEngine = FocusEngine.instance,
    repository: FocalRepository = FocalRepository.instance
) {
    var showEmergencyModal by remember { mutableStateOf(false) }
    var showParentUnlockModal by remember { mutableStateOf(false) }

    val currentSession = focusEngine.currentSession
    val policyTitle = currentSession?.policyName ?: "Study Mode"
    val isFocusActive = focusEngine.isFocusActive
    val endTimeMillis = currentSession?.endTimeMillis ?: 0L
    val nowClock by produceState(getDeviceCurrentTimeMillis(), endTimeMillis) {
        while (true) {
            value = getDeviceCurrentTimeMillis()
            delay(1000 - value % 1000)
        }
    }
    val remainingMinutes = if (endTimeMillis > 0L) {
        ((endTimeMillis - nowClock + 59999L) / 60000L).coerceAtLeast(1L).toInt()
    } else {
        (focusEngine.remainingSeconds / 60).coerceAtLeast(1)
    }
    val subtitleText = if (isFocusActive) "$policyTitle · $remainingMinutes min left" else "Protected by FOCAL · ask a parent"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ChildBlockScreenContent(
            title = "$appName is blocked",
            subtitle = subtitleText,
            icon = FocalIcons.Smartphone,
            isFocusActive = isFocusActive,
            blockedAttemptsCount = focusEngine.blockedAttemptsCount,
            onBackToFocus = onDismiss,
            onSecondaryAction = { showParentUnlockModal = true },
            secondaryActionText = "Parent unlock"
        )

        if (showParentUnlockModal) {
            ParentUnlockScreen(
                onClose = { showParentUnlockModal = false },
                onUnlockSuccess = {
                    showParentUnlockModal = false
                    onDismiss()
                },
                repository = repository
            )
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
}

/**
 * Restricted Website Overlay displayed when a blocked web page is accessed during active focus.
 */
@Composable
fun BlockedWebsiteOverlay(
    domainName: String,
    onDismiss: () -> Unit,
    onOpenNewTab: () -> Unit = {
        PlatformPermissionsHelper.openNewBrowserTab()
        onDismiss()
    },
    focusEngine: FocusEngine = FocusEngine.instance,
    repository: FocalRepository = FocalRepository.instance
) {
    var showEmergencyModal by remember { mutableStateOf(false) }
    var showParentUnlockModal by remember { mutableStateOf(false) }

    val currentSession = focusEngine.currentSession
    val policyTitle = currentSession?.policyName ?: "Study Mode"
    val isFocusActive = focusEngine.isFocusActive
    val endTimeMillis = currentSession?.endTimeMillis ?: 0L
    val nowClock by produceState(getDeviceCurrentTimeMillis(), endTimeMillis) {
        while (true) {
            value = getDeviceCurrentTimeMillis()
            delay(1000 - value % 1000)
        }
    }
    val remainingMinutes = if (endTimeMillis > 0L) {
        ((endTimeMillis - nowClock + 59999L) / 60000L).coerceAtLeast(1L).toInt()
    } else {
        (focusEngine.remainingSeconds / 60).coerceAtLeast(1)
    }
    val subtitleText = if (isFocusActive) "$policyTitle · $remainingMinutes min left" else "Protected by FOCAL · ask a parent"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ChildBlockScreenContent(
            title = "${domainName.ifEmpty { "Website" }} is blocked",
            subtitle = subtitleText,
            icon = FocalIcons.Globe,
            isFocusActive = isFocusActive,
            blockedAttemptsCount = focusEngine.blockedAttemptsCount,
            onBackToFocus = onDismiss,
            onSecondaryAction = { showParentUnlockModal = true },
            secondaryActionText = "Parent unlock"
        )

        if (showParentUnlockModal) {
            ParentUnlockScreen(
                onClose = { showParentUnlockModal = false },
                onUnlockSuccess = {
                    showParentUnlockModal = false
                    onDismiss()
                },
                repository = repository
            )
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
}

enum class EmergencyState {
    INPUT,
    WAITING,
    APPROVED,
    DECLINED
}

/**
 * Emergency Exit Modal with input, Send button, and live status flow (Waiting / Approved / Declined).
 */
@Composable
fun EmergencyExitModal(
    onDismiss: () -> Unit,
    onSubmitRequest: (String) -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    var reasonText by remember { mutableStateOf("") }
    var emergencyState by remember { mutableStateOf(EmergencyState.INPUT) }
    var activeRequest by remember { mutableStateOf<EmergencyExitRequest?>(null) }

    // Periodically inspect request status changes (simulate/sync parent response)
    LaunchedEffect(emergencyState) {
        if (emergencyState == EmergencyState.WAITING && activeRequest != null) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val updated = repository.emergencyRequests.find { it.id == activeRequest?.id }
                if (updated != null) {
                    if (updated.status == "APPROVED") {
                        emergencyState = EmergencyState.APPROVED
                        focusEngine.endFocus(
                            com.focal.app.model.TriggerType.EMERGENCY_OVERRIDE,
                            "Emergency exit approved by parent"
                        )
                        break
                    } else if (updated.status == "REJECTED" || updated.status == "DECLINED") {
                        emergencyState = EmergencyState.DECLINED
                        break
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Emergency Exit",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = FocalIcons.Close,
                            contentDescription = "Close",
                            tint = FocalColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (emergencyState) {
                    EmergencyState.INPUT -> {
                        Text(
                            text = "Parent approval is required to end this focus session early. Please state your reason below:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = FocalColors.TextSecondary,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        FocalTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = "Reason (optional)",
                            placeholder = "e.g. Need to call home / School assignment..."
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        PrimaryButton(
                            text = "Send Request",
                            onClick = {
                                val req = repository.submitEmergencyExitRequest(
                                    sessionId = focusEngine.currentSession?.id ?: "sess_active_1",
                                    reason = reasonText.ifBlank { "Emergency exit requested" }
                                )
                                activeRequest = req
                                emergencyState = EmergencyState.WAITING
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    EmergencyState.WAITING -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Waiting for parent...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Parent has been notified of your request. Session remains active until approved.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = FocalColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Test shortcut for simulation
                            SecondaryButton(
                                text = "Simulate Parent Approval",
                                onClick = {
                                    activeRequest?.let { req ->
                                        repository.resolveEmergencyRequest(req.id, approve = true)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    EmergencyState.APPROVED -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(FocalColors.Surface)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Emergency Exit Approved",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your parent has approved the exit. Session restrictions have been removed.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = FocalColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PrimaryButton(
                                text = "Done",
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    EmergencyState.DECLINED -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ChildColors.IconCircleBg)
                                    .border(1.dp, ChildColors.BorderStrong, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Close,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Request Declined",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Parent declined the emergency exit request. Please return to your focus session.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = FocalColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PrimaryButton(
                                text = "Back to focus",
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
