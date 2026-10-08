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

// -------------------------------------------------------------
// CHILD PERMISSIONS SCREEN ("Turn on protection" — 7 Required Settings)
// -------------------------------------------------------------
@Composable
fun ChildPermissionsSetupScreen(
    onPermissionsComplete: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var hasAccessibility by remember { mutableStateOf(PlatformPermissionsHelper.isAccessibilityEnabled()) }
    var hasOverlay by remember { mutableStateOf(PlatformPermissionsHelper.isOverlayEnabled()) }
    var hasUsageStats by remember { mutableStateOf(PlatformPermissionsHelper.isUsageAccessEnabled()) }
    var hasNotifications by remember { mutableStateOf(PlatformPermissionsHelper.isNotificationPermissionGranted()) }
    var hasBatteryOpt by remember { mutableStateOf(PlatformPermissionsHelper.isBatteryOptimizationDisabled()) }
    var hasExactAlarm by remember { mutableStateOf(PlatformPermissionsHelper.isExactAlarmPermissionGranted()) }
    var hasDeviceAdmin by remember { mutableStateOf(PlatformPermissionsHelper.isDeviceAdminActive()) }

    val hasNfcHardware = remember { PlatformPermissionsHelper.hasNfcHardware() }
    var hasNfcEnabled by remember { mutableStateOf(PlatformPermissionsHelper.isNfcEnabled()) }

    // Auto-refresh permission status every 2 seconds and on resume
    LaunchedEffect(Unit) {
        while (true) {
            hasAccessibility = PlatformPermissionsHelper.isAccessibilityEnabled()
            hasOverlay = PlatformPermissionsHelper.isOverlayEnabled()
            hasUsageStats = PlatformPermissionsHelper.isUsageAccessEnabled()
            hasNotifications = PlatformPermissionsHelper.isNotificationPermissionGranted()
            hasBatteryOpt = PlatformPermissionsHelper.isBatteryOptimizationDisabled()
            hasExactAlarm = PlatformPermissionsHelper.isExactAlarmPermissionGranted()
            hasDeviceAdmin = PlatformPermissionsHelper.isDeviceAdminActive()
            hasNfcEnabled = PlatformPermissionsHelper.isNfcEnabled()
            delay(2000)
        }
    }

    val requiredStatusList = listOf(hasAccessibility, hasOverlay, hasUsageStats, hasNotifications, hasBatteryOpt, hasExactAlarm, hasDeviceAdmin)
    val grantedCount = requiredStatusList.count { it }
    val totalRequired = requiredStatusList.size
    val allGranted = grantedCount == totalRequired

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            StatusBadge(
                text = "$grantedCount OF $totalRequired DONE",
                badgeType = if (allGranted) BadgeType.ACTIVE else BadgeType.PENDING
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Turn on protection",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Grant required permissions so FOCAL can keep your device protected.",
                style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Accessibility Service
            PermissionRowItem(
                title = "1. Accessibility",
                description = "Detects when a blocked app opens",
                isGranted = hasAccessibility,
                onTap = { PlatformPermissionsHelper.openAccessibilitySettings() }
            )

            if (!hasAccessibility) {
                Spacer(modifier = Modifier.height(6.dp))
                FocalCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
                    Column {
                        Text(
                            text = "Android restricted settings block? Go to Settings → Apps → FOCAL → ⋮ → Allow restricted settings",
                            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SecondaryButton(
                            text = "Open App Info",
                            onClick = { PlatformPermissionsHelper.openAppDetailsSettings() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Display Over Other Apps (Overlay)
            PermissionRowItem(
                title = "2. Display over other apps",
                description = "Shows the focus screen shield over blocked apps",
                isGranted = hasOverlay,
                onTap = { PlatformPermissionsHelper.openOverlaySettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Usage Access
            PermissionRowItem(
                title = "3. Usage access",
                description = "Counts screen time and daily focus minutes",
                isGranted = hasUsageStats,
                onTap = { PlatformPermissionsHelper.openUsageAccessSettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Notifications
            PermissionRowItem(
                title = "4. Notifications",
                description = "Session reminders and focus status alerts",
                isGranted = hasNotifications,
                onTap = { PlatformPermissionsHelper.openNotificationSettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Battery Optimization
            PermissionRowItem(
                title = "5. Battery: Unrestricted",
                description = "Keeps protection running reliably in background",
                isGranted = hasBatteryOpt,
                onTap = { PlatformPermissionsHelper.openBatteryOptimizationSettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Exact Alarms
            PermissionRowItem(
                title = "6. Alarms & reminders",
                description = "Runs focus schedules on time even offline",
                isGranted = hasExactAlarm,
                onTap = { PlatformPermissionsHelper.openExactAlarmSettings() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Uninstall Protection (Device Admin)
            PermissionRowItem(
                title = "7. Uninstall protection",
                description = "Stops FOCAL from being removed silently",
                isGranted = hasDeviceAdmin,
                onTap = { PlatformPermissionsHelper.openDeviceAdminSettings() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Conditional NFC Row
            if (hasNfcHardware) {
                PermissionRowItem(
                    title = "NFC Hardware Sensor",
                    description = if (hasNfcEnabled) "Ready for card taps" else "Turn on NFC in system settings to use card taps",
                    isGranted = hasNfcEnabled,
                    onTap = { PlatformPermissionsHelper.openNfcSettings() }
                )
            } else {
                FocalCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
                    Text(
                        text = "No NFC sensor detected — use QR codes or FOCAL USB reader.",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Bottom Action Bar: Continue enabled ONLY when ALL 7 required permissions are enabled
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
        ) {
            if (!allGranted) {
                Text(
                    text = "Grant all 7 required permissions to complete setup",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.Danger, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
            }

            PrimaryButton(
                text = if (allGranted) "Continue" else "$grantedCount of $totalRequired Enabled",
                enabled = allGranted,
                onClick = onPermissionsComplete,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// PROTECTION OFF SCREEN (Child, Revoked Permissions Screen)
// -------------------------------------------------------------
@Composable
fun ProtectionOffScreen(
    onGoToReadOnlyHome: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val hasAcc = PlatformPermissionsHelper.isAccessibilityEnabled()
    val hasOvl = PlatformPermissionsHelper.isOverlayEnabled()
    val hasUsg = PlatformPermissionsHelper.isUsageAccessEnabled()
    val hasNot = PlatformPermissionsHelper.isNotificationPermissionGranted()
    val hasBat = PlatformPermissionsHelper.isBatteryOptimizationDisabled()
    val hasAlm = PlatformPermissionsHelper.isExactAlarmPermissionGranted()
    val hasAdm = PlatformPermissionsHelper.isDeviceAdminActive()

    val missingItems = mutableListOf<Pair<String, () -> Unit>>()
    if (!hasAcc) missingItems.add("Accessibility (detects blocked apps)" to { PlatformPermissionsHelper.openAccessibilitySettings() })
    if (!hasOvl) missingItems.add("Display over other apps (shows focus shield)" to { PlatformPermissionsHelper.openOverlaySettings() })
    if (!hasUsg) missingItems.add("Usage access (counts screen time)" to { PlatformPermissionsHelper.openUsageAccessSettings() })
    if (!hasNot) missingItems.add("Notifications (status alerts)" to { PlatformPermissionsHelper.openNotificationSettings() })
    if (!hasBat) missingItems.add("Battery: Unrestricted (background protection)" to { PlatformPermissionsHelper.openBatteryOptimizationSettings() })
    if (!hasAlm) missingItems.add("Alarms & reminders (runs schedules)" to { PlatformPermissionsHelper.openExactAlarmSettings() })
    if (!hasAdm) missingItems.add("Uninstall protection (Device Admin)" to { PlatformPermissionsHelper.openDeviceAdminSettings() })

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.systemBars).padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            StatusBadge(text = "PROTECTION OFF", badgeType = BadgeType.ACTIVE)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Protection is off — turn it back on",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = FocalColors.Danger),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Required permissions were turned off. Fix missing items to restore focus protection.",
                style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            missingItems.forEach { (name, fixAction) ->
                FocalCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        PrimaryButton(
                            text = "Fix",
                            onClick = fixAction,
                            modifier = Modifier.width(70.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
            SecondaryButton(
                text = "Back to Read-Only Home",
                onClick = onGoToReadOnlyHome,
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
        trailingText = if (isGranted) "✓ Granted" else "Allow",
        onClick = onTap,
        modifier = Modifier.fillMaxWidth()
    )
}
