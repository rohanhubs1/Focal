package com.focal.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun PermissionsSetupScreen(
    onPermissionsComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var hasAccessibility by remember { mutableStateOf(PlatformPermissionsHelper.isAccessibilityEnabled()) }
    var hasOverlay by remember { mutableStateOf(PlatformPermissionsHelper.isOverlayEnabled()) }
    var hasUsageStats by remember { mutableStateOf(PlatformPermissionsHelper.isUsageAccessEnabled()) }
    var hasNotifications by remember { mutableStateOf(PlatformPermissionsHelper.isNotificationPermissionGranted()) }

    // Check if on iOS vs Android (Simulated for platform compatibility)
    var hasIosScreenTime by remember { mutableStateOf(false) }

    // Real system permission polling loop
    LaunchedEffect(Unit) {
        while (true) {
            hasAccessibility = PlatformPermissionsHelper.isAccessibilityEnabled()
            hasOverlay = PlatformPermissionsHelper.isOverlayEnabled()
            hasUsageStats = PlatformPermissionsHelper.isUsageAccessEnabled()
            hasNotifications = PlatformPermissionsHelper.isNotificationPermissionGranted()
            delay(1000)
        }
    }

    val allGranted = (hasAccessibility && hasOverlay && hasUsageStats && hasNotifications) || hasIosScreenTime

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            StatusBadge(
                text = "SYSTEM PERMISSIONS",
                badgeType = BadgeType.ACTIVE
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Enable Permissions",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Grant required permissions so FOCAL can enforce focus boundaries.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FocalColors.TextSecondary,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 1. Accessibility Service
            PermissionRowItem(
                title = "Accessibility",
                description = "Detects restricted apps and website launches.",
                isGranted = hasAccessibility,
                onTap = { PlatformPermissionsHelper.openAccessibilitySettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Display Over Other Apps (Overlay)
            PermissionRowItem(
                title = "Display over other apps",
                description = "Displays the full-screen blocker shield overlay.",
                isGranted = hasOverlay,
                onTap = { PlatformPermissionsHelper.openOverlaySettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Usage Access
            PermissionRowItem(
                title = "Usage access",
                description = "Calculates screen time and analytics.",
                isGranted = hasUsageStats,
                onTap = { PlatformPermissionsHelper.openUsageAccessSettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Notifications
            PermissionRowItem(
                title = "Notifications",
                description = "Sends session alerts and emergency exit requests.",
                isGranted = hasNotifications,
                onTap = { PlatformPermissionsHelper.openNotificationSettings() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Debug Helper to simulate all permissions granted easily
            SecondaryButton(
                text = if (allGranted) "✓ All Permissions Granted" else "Simulate Grant All (Debug)",
                onClick = {
                    hasAccessibility = true
                    hasOverlay = true
                    hasUsageStats = true
                    hasNotifications = true
                    hasIosScreenTime = true
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Bottom Action Bar: Continue enabled ONLY when all 4 permissions are granted
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            if (!allGranted) {
                Text(
                    text = "⚠️ Setup Gate: Grant all 4 permissions to complete setup",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocalColors.Danger,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
            }

            PrimaryButton(
                text = if (allGranted) "Complete Setup" else "Grant All 4 Permissions",
                enabled = allGranted,
                onClick = onPermissionsComplete,
                modifier = Modifier.fillMaxWidth()
            )
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
    FocalListRow(
        title = title,
        subtitle = description,
        icon = if (isGranted) FocalIcons.Check else FocalIcons.Lock,
        trailingText = if (isGranted) "✓ Granted" else "Required",
        onClick = onTap,
        modifier = Modifier.fillMaxWidth()
    )
}
