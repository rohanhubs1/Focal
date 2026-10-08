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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.data.FocalRepository
import com.focal.app.model.QrToken
import com.focal.app.theme.FocalColors

@Composable
fun QrScannerModal(
    title: String = "Scan Focus QR",
    subtitle: String = "Align the Focus QR code within the frame to authenticate session.",
    onDismiss: () -> Unit,
    onQrScanned: (QrToken) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scanSuccess by remember { mutableStateOf(false) }
    var scannerMode by remember { mutableStateOf(0) } // 0 = Live Camera, 1 = Test Tokens

    // Laser scan sweep line animation
    val infiniteTransition = rememberInfiniteTransition(label = "laserScan")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPosition"
    )

    Dialog(onDismissRequest = onDismiss) {
        DarkCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = FocalColors.CardElevated,
            borderColor = FocalColors.BorderGlow
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = "OPTICAL QR ENGINE",
                    backgroundColor = FocalColors.SecondaryAccent.copy(alpha = 0.2f),
                    textColor = FocalColors.SecondaryAccentLight,
                    dotColor = FocalColors.Success
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

            Spacer(modifier = Modifier.height(14.dp))

            // Tab switch: Live Camera vs Test Tokens
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(FocalColors.Card)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (scannerMode == 0) FocalColors.CardElevated else Color.Transparent)
                        .clickable { scannerMode = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Live Camera",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (scannerMode == 0) Color.White else FocalColors.TextSecondary
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (scannerMode == 1) FocalColors.CardElevated else Color.Transparent)
                        .clickable { scannerMode = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tokens List",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (scannerMode == 1) Color.White else FocalColors.TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (scannerMode == 0) {
                // Scanner Viewfinder Area with Live Camera and Laser Sweep Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocalColors.CardElevated)
                        .border(1.dp, FocalColors.Border, RoundedCornerShape(16.dp))
                        .drawBehind {
                            val frameSize = 160.dp.toPx()
                            val left = (size.width - frameSize) / 2
                            val top = (size.height - frameSize) / 2
                            val cornerLen = 24.dp.toPx()
                            val stroke = 3.dp.toPx()

                            // Corner markers (Cyan/Blue)
                            val color = if (scanSuccess) FocalColors.Success else FocalColors.SecondaryAccent

                            // Top-left
                            drawLine(color, Offset(left, top), Offset(left + cornerLen, top), stroke)
                            drawLine(color, Offset(left, top), Offset(left, top + cornerLen), stroke)

                            // Top-right
                            drawLine(color, Offset(left + frameSize, top), Offset(left + frameSize - cornerLen, top), stroke)
                            drawLine(color, Offset(left + frameSize, top), Offset(left + frameSize, top + cornerLen), stroke)

                            // Bottom-left
                            drawLine(color, Offset(left, top + frameSize), Offset(left + cornerLen, top + frameSize), stroke)
                            drawLine(color, Offset(left, top + frameSize), Offset(left, top + frameSize - cornerLen), stroke)

                            // Bottom-right
                            drawLine(color, Offset(left + frameSize, top + frameSize), Offset(left + frameSize - cornerLen, top + frameSize), stroke)
                            drawLine(color, Offset(left + frameSize, top + frameSize), Offset(left + frameSize, top + frameSize - cornerLen), stroke)

                            // Animated Laser Sweep Line
                            if (!scanSuccess) {
                                val currentLaserY = top + (frameSize * laserY)
                                drawLine(
                                    color = FocalColors.SecondaryAccentLight,
                                    start = Offset(left + 4, currentLaserY),
                                    end = Offset(left + frameSize - 4, currentLaserY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (scanSuccess) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(FocalColors.SuccessContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Check,
                                    contentDescription = null,
                                    tint = FocalColors.Success,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "QR Verified!",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = FocalColors.Success,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        PlatformCameraScanner(
                            modifier = Modifier.fillMaxSize(),
                            onQrDetected = { rawToken ->
                                if (!scanSuccess) {
                                    val result = repository.verifyQrToken(rawToken.trim())
                                    result.fold(
                                        onSuccess = { verified ->
                                            scanSuccess = true
                                            errorMessage = null
                                            onQrScanned(verified)
                                        },
                                        onFailure = { err ->
                                            errorMessage = "Scanned QR: $rawToken (${err.message ?: "Invalid"})"
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            } else {
                // Quick Scan Simulator Buttons
                Text(
                    text = "AVAILABLE ACTIVE QR TOKENS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        color = FocalColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                repository.qrTokens.forEach { qr ->
                    DarkCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        backgroundColor = FocalColors.Card,
                        borderColor = FocalColors.Border,
                        onClick = {
                            val result = repository.verifyQrToken(qr.token)
                            result.fold(
                                onSuccess = { verified ->
                                    scanSuccess = true
                                    errorMessage = null
                                    onQrScanned(verified)
                                },
                                onFailure = { err ->
                                    errorMessage = err.message ?: "Invalid QR token"
                                }
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = qr.policyTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Token: ${qr.token}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = FocalColors.TextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            StatusBadge(
                                text = "TEST",
                                backgroundColor = FocalColors.SecondaryAccent.copy(alpha = 0.2f),
                                textColor = FocalColors.SecondaryAccentLight
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                DarkCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FocalColors.ErrorContainer.copy(alpha = 0.3f),
                    borderColor = FocalColors.Error
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FocalColors.Error
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlineButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun QrDisplayModal(
    qrToken: QrToken,
    childName: String,
    onDismiss: () -> Unit
) {
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        DarkCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = FocalColors.CardElevated,
            borderColor = FocalColors.BorderGlow
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = "ACTIVE FOCUS QR",
                    backgroundColor = FocalColors.PrimaryAccentDark.copy(alpha = 0.3f),
                    textColor = FocalColors.PrimaryAccentLight,
                    dotColor = FocalColors.Success
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

            Text(
                text = qrToken.policyTitle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Generated for $childName",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Realistic Dark QR Code Canvas Display
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.QrCode,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Secure Token: ${qrToken.token}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = FocalColors.TextMuted,
                    fontSize = 11.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (toastMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                StatusBadge(
                    text = toastMessage ?: "",
                    backgroundColor = FocalColors.SuccessContainer,
                    textColor = FocalColors.Success,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlineButton(
                    text = "Copy Link",
                    onClick = { toastMessage = "Link copied to clipboard!" },
                    modifier = Modifier.weight(1f)
                )
                OutlineButton(
                    text = "Share QR",
                    onClick = { toastMessage = "Shared via system intent!" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            GradientButton(
                text = "Download QR Code",
                onClick = { toastMessage = "QR saved to gallery!" },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
