package com.focal.app.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.data.FocalRepository
import com.focal.app.model.CardRegistrationInspection
import com.focal.app.model.CardStatus
import com.focal.app.model.FocusCard
import com.focal.app.theme.FocalColors

@Composable
fun NfcScannerModal(
    title: String = "Tap Focus Card to Start",
    subtitle: String = "Hold your FOCAL card against the back of your phone.",
    onDismiss: () -> Unit,
    onCardScanned: (FocusCard) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var isScanning by remember { mutableStateOf(true) }
    var scanSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detectedCard by remember { mutableStateOf<FocusCard?>(null) }

    // Pulsing wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "nfcPulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
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
                        text = "NFC SENSOR READY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                }

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

            // Animated NFC Pulse Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .drawBehind {
                            val maxRadius = size.minDimension / 2
                            if (isScanning && !scanSuccess) {
                                drawCircle(
                                    color = Color.White.copy(alpha = (1f - pulse1) * 0.3f),
                                    radius = maxRadius * pulse1,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = (1f - pulse2) * 0.3f),
                                    radius = maxRadius * pulse2,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(FocalColors.Surface)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (scanSuccess) FocalIcons.Check else FocalIcons.Nfc,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Text(
                text = if (scanSuccess) "Card Verified" else title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (scanSuccess) {
                    "Focus Card ${detectedCard?.cardId} successfully authenticated."
                } else subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Banner(
                    message = errorMessage ?: "Verification failed",
                    isDanger = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "TAP TEST CARD (SIMULATOR)",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    color = FocalColors.TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            repository.nfcCards.forEach { card ->
                FocalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    borderColor = if (detectedCard?.id == card.id) Color.White else FocalColors.Border,
                    onClick = {
                        val result = repository.verifyNfcCard(card.cardId, card.nfcUid, card.token)
                        result.fold(
                            onSuccess = { verified ->
                                isScanning = false
                                scanSuccess = true
                                detectedCard = verified
                                errorMessage = null
                                onCardScanned(verified)
                            },
                            onFailure = { err ->
                                errorMessage = err.message ?: "Card verification failed"
                            }
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = FocalIcons.Nfc,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Card ${card.cardId} (${card.ownerName ?: "Unassigned"})",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Token: ${card.token}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = FocalColors.TextSecondary
                                    )
                                )
                            }
                        }
                        StatusBadge(
                            text = card.status.name,
                            badgeType = BadgeType.ACTIVE
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * PARENT CARD REGISTRATION FLOW MODAL
 * 1. Place card on reader
 * 2. Tag inspection (Unowned -> register, Already owned -> reassign, Other owner -> error, Revoked -> error, Blank -> write & register)
 * 3. Step 4: "Who is this card for?" (Select child)
 * 4. Success Screen ("Card FC0007 is ready for Ananya")
 */
@Composable
fun AddCardFlowModal(
    preSelectedChildId: String? = null,
    onDismiss: () -> Unit,
    onSuccess: (FocusCard) -> Unit = {},
    repository: FocalRepository = FocalRepository.instance
) {
    val currentUser = repository.currentUser
    val children = repository.childrenList

    var step by remember { mutableIntStateOf(1) } // 1: Reader, 2: Select Child, 3: Success Screen
    var registeredCard by remember { mutableStateOf<FocusCard?>(null) }
    var selectedChildId by remember { mutableStateOf(preSelectedChildId ?: children.firstOrNull()?.id) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val handleTagScan: (String) -> Unit = { rawPayload ->
        errorMessage = null
        infoMessage = null
        val inspection = repository.inspectCardForRegistration(rawPayload, currentUser.id)
        when (inspection) {
            is CardRegistrationInspection.BlankTag -> {
                if (com.focal.app.config.FocalConfig.allowParentBlankCardWriting) {
                    val newCard = repository.writeAndRegisterBlankCard(currentUser.id, currentUser.name)
                    registeredCard = newCard
                    step = 2
                } else {
                    errorMessage = "This card isn't set up. Contact FOCAL support."
                }
            }
            is CardRegistrationInspection.UnownedInStock -> {
                val newCard = repository.registerCardToParent(inspection.card.token, currentUser.id, currentUser.name)
                registeredCard = newCard
                step = 2
            }
            is CardRegistrationInspection.AlreadyOwnedByMe -> {
                infoMessage = "This card is already yours (${inspection.card.cardId}). You can reassign it below."
                registeredCard = inspection.card
                step = 2
            }
            is CardRegistrationInspection.OwnedByOther -> {
                errorMessage = "This card belongs to another account."
            }
            is CardRegistrationInspection.Revoked -> {
                errorMessage = "This card was deactivated."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (step) {
                        1 -> "Place Card on Reader"
                        2 -> "Who is this card for?"
                        else -> "Card Ready"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = FocalIcons.Close,
                        contentDescription = "Close",
                        tint = FocalColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                1 -> {
                    // STEP 1: Reader status + simulator
                    StatusBadge(
                        text = "PLACE CARD NEAR PHONE",
                        badgeType = BadgeType.ACTIVE
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NfcPulseAnimation(detected = false)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Place your FOCAL card on the reader to claim and register it to your account.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Banner(message = errorMessage ?: "", isDanger = true)
                    }

                    if (com.focal.app.config.FocalConfig.isDemoMode) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "SIMULATED TAG READER (DEMO)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                color = FocalColors.TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Simulator button 1: Blank tag
                        SecondaryButton(
                            text = "Tap Blank Sticker / New Tag",
                            onClick = { handleTagScan("BLANK") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )

                        // Simulator button 2: Unowned inventory card
                        SecondaryButton(
                            text = "Tap Unowned Inventory Card (FC0003)",
                            onClick = { handleTagScan("FOCUSCARD:TK_UNOWNED_FC0003") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )

                        // Simulator button 3: Already owned card
                        SecondaryButton(
                            text = "Tap My Card (FC0001)",
                            onClick = { handleTagScan("FOCUSCARD:8XK92LM7") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )

                        // Simulator button 4: Other parent's card
                        SecondaryButton(
                            text = "Tap Other Parent's Card",
                            onClick = {
                                val otherCard = FocusCard(
                                    id = "card_other",
                                    cardId = "FC9999",
                                    token = "TK_OTHER_PARENT",
                                    ownerParentId = "user_other_parent",
                                    status = CardStatus.REGISTERED
                                )
                                repository.nfcCards.add(otherCard)
                                handleTagScan("FOCUSCARD:TK_OTHER_PARENT")
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )

                        // Simulator button 5: Revoked card
                        SecondaryButton(
                            text = "Tap Revoked Card",
                            onClick = {
                                val revokedCard = FocusCard(
                                    id = "card_revoked",
                                    cardId = "FC8888",
                                    token = "TK_REVOKED",
                                    ownerParentId = null,
                                    status = CardStatus.REVOKED
                                )
                                repository.nfcCards.add(revokedCard)
                                handleTagScan("FOCUSCARD:TK_REVOKED")
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                    }
                }

                2 -> {
                    // STEP 2: "Who is this card for?"
                    if (infoMessage != null) {
                        Banner(message = infoMessage ?: "", isDanger = false)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = "Card ${registeredCard?.cardId} is registered to your account (${currentUser.name}). Select a child to assign this card to:",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    children.forEach { child ->
                        val isSelected = selectedChildId == child.id
                        FocalCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            borderColor = if (isSelected) Color.White else FocalColors.Border,
                            onClick = { selectedChildId = child.id }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InitialsAvatar(name = child.name, size = 32.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = child.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                        Text(
                                            text = child.deviceName,
                                            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontSize = 11.sp)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    StatusBadge(text = "SELECTED", badgeType = BadgeType.ACTIVE)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val selectedChild = children.find { it.id == selectedChildId }

                    PrimaryButton(
                        text = if (selectedChild != null) "Assign to ${selectedChild.name}" else "Select a child",
                        enabled = selectedChild != null,
                        onClick = {
                            if (selectedChild != null && registeredCard != null) {
                                val updated = repository.assignCardToChild(registeredCard!!.cardId, selectedChild.id)
                                if (updated != null) registeredCard = updated
                                step = 3
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SecondaryButton(
                        text = "Keep unassigned for now",
                        onClick = { step = 3 },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                3 -> {
                    // STEP 3: Success Screen
                    val assignedChild = children.find { it.id == registeredCard?.assignedChildId }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(FocalColors.Surface)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Check,
                                contentDescription = "Success",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (assignedChild != null) "Card ${registeredCard?.cardId} is ready for ${assignedChild.name}" else "Card ${registeredCard?.cardId} registered",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (assignedChild != null)
                                "This NFC card is assigned to ${assignedChild.name}. Tapping this card on ${assignedChild.name}'s phone will start a focus session."
                            else
                                "This card is registered under your parent account. You can assign it to a child anytime from Profile → NFC Cards.",
                            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        FocalCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("CARD ID", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                                    Text(registeredCard?.cardId ?: "FC0001", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TOKEN", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                                    Text(registeredCard?.token ?: "", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("OWNER PARENT", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                                    Text(currentUser.name, style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ASSIGNED CHILD", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                                    Text(assignedChild?.name ?: "Unassigned", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("STATUS", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                                    Text(registeredCard?.status?.name ?: "REGISTERED", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        PrimaryButton(
                            text = "Done",
                            onClick = {
                                registeredCard?.let { onSuccess(it) }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

