package com.focal.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.focal.app.data.FocalRepository
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.*
import kotlinx.coroutines.delay

// -------------------------------------------------------------
// PARENT PERMISSIONS SCREEN ("Stay in the loop" — Notifications Only)
// -------------------------------------------------------------
@Composable
fun ParentPermissionsScreen(
    onPermissionsComplete: () -> Unit,
    onSkipWithWarning: () -> Unit
) {
    var hasNotifications by remember { mutableStateOf(PlatformPermissionsHelper.isNotificationPermissionGranted()) }
    var showNotNowWarning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            hasNotifications = PlatformPermissionsHelper.isNotificationPermissionGranted()
            delay(1500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            StatusBadge(text = "PARENT ALERTS", badgeType = BadgeType.ACTIVE)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Stay in the loop",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enable notifications to receive alerts when your child requests emergency exit, turns off protection, or goes offline.",
                style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            PermissionRowItem(
                title = "Notifications",
                description = "Emergency exit requests, protection alerts & device status.",
                isGranted = hasNotifications,
                onTap = { PlatformPermissionsHelper.openNotificationSettings() }
            )

            if (showNotNowWarning && !hasNotifications) {
                Spacer(modifier = Modifier.height(16.dp))
                Banner(
                    message = "Alerts won't arrive if notifications are disabled. You can enable them anytime from Parent Settings.",
                    isDanger = true
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        ) {
            PrimaryButton(
                text = if (hasNotifications) "Continue to Parent Dashboard" else "Enable Notifications",
                onClick = {
                    if (hasNotifications) {
                        onPermissionsComplete()
                    } else {
                        PlatformPermissionsHelper.openNotificationSettings()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (!hasNotifications) {
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = "Not now",
                    onClick = {
                        if (!showNotNowWarning) {
                            showNotNowWarning = true
                        } else {
                            onSkipWithWarning()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PermissionRowItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onTap: () -> Unit
) {
    FocalCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FocalColors.Surface)
                        .border(1.dp, FocalColors.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGranted) FocalIcons.Check else FocalIcons.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isGranted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✓ Allowed",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            } else {
                SecondaryButton(
                    text = "Allow",
                    onClick = onTap,
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }
}

