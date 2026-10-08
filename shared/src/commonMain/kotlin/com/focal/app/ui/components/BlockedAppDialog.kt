package com.focal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.DialogProperties
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.EmergencyExitRequest
import com.focal.app.theme.FocalColors

/**
 * Restricted App Overlay displayed when a blocked application is launched during an active focus session.
 * Redesigned to strict minimal monochrome specifications.
 */
@Composable
fun BlockedAppOverlay(
    appName: String,
    onDismiss: () -> Unit,
    focusEngine: FocusEngine = FocusEngine.instance,
    repository: FocalRepository = FocalRepository.instance
) {
    var showEmergencyModal by remember { mutableStateOf(false) }
    val currentSession = focusEngine.currentSession
    val policyTitle = currentSession?.policyName ?: "Study Mode"
    val remainingTimeStr = focusEngine.formatRemainingTime()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(FocalColors.Surface)
                        .border(1.dp, FocalColors.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    FocalAppLogo(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Status Dot Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocalColors.Surface)
                        .border(1.dp, FocalColors.Border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
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

                Spacer(modifier = Modifier.height(20.dp))

                // "[AppName] is blocked"
                Text(
                    text = "$appName is blocked",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // "[PolicyName] · [Time Left] left"
                Text(
                    text = "$policyTitle · $remainingTimeStr left",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = FocalColors.TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Button: "Back to focus"
                PrimaryButton(
                    text = "Back to focus",
                    onClick = onDismiss,
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
    val currentSession = focusEngine.currentSession
    val policyTitle = currentSession?.policyName ?: "Study Mode"
    val remainingTimeStr = focusEngine.formatRemainingTime()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(FocalColors.Surface)
                        .border(1.dp, FocalColors.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    FocalAppLogo(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(FocalColors.Surface)
                        .border(1.dp, FocalColors.Border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    StatusDot(active = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WEBSITE RESTRICTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "${domainName.ifEmpty { "Website" }} is blocked",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$policyTitle · $remainingTimeStr left",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = FocalColors.TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                PrimaryButton(
                    text = "Back to focus",
                    onClick = onDismiss,
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
                                    .background(FocalColors.Surface)
                                    .border(1.dp, FocalColors.Danger, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Close,
                                    contentDescription = null,
                                    tint = FocalColors.Danger,
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

/**
 * Protection Disabled Modal displayed when critical accessibility/overlay permissions are lost.
 */
@Composable
fun ProtectionDisabledModal(
    focusEngine: FocusEngine = FocusEngine.instance,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            focusEngine.checkAndUpdatePermissions()
        }
    }

    val missingAccess = !focusEngine.isAccessibilityGranted
    val missingOverlay = !focusEngine.isOverlayGranted

    val messageText = when {
        missingAccess && missingOverlay ->
            "Accessibility and Overlay permissions are required to enforce focus rules."
        missingAccess ->
            "Accessibility Service is required to detect restricted app launches."
        missingOverlay ->
            "Display Over Other Apps permission is required to show the Focus Shield."
        else ->
            "Required focus protection permissions are disabled."
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            FocalCard(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(FocalColors.Surface)
                            .border(1.dp, FocalColors.Danger, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Warning,
                            contentDescription = null,
                            tint = FocalColors.Danger,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Protection is off — fix now",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = FocalColors.TextSecondary,
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryButton(
                        text = "Fix Permissions",
                        onClick = {
                            if (missingAccess) {
                                PlatformPermissionsHelper.openAccessibilitySettings()
                            } else {
                                PlatformPermissionsHelper.openOverlaySettings()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SecondaryButton(
                        text = "Re-check status",
                        onClick = { focusEngine.checkAndUpdatePermissions() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
