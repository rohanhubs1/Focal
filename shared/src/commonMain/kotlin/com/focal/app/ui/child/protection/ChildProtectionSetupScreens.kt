package com.focal.app.ui.child.protection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.data.FocalLocalStorage
import com.focal.app.model.currentTimeMillis
import com.focal.app.ui.child.components.GlassCard
import com.focal.app.ui.child.components.GlowArcs
import com.focal.app.ui.child.components.PrimaryButton
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.focal.app.ui.components.rememberDeviceAdminLauncher
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography
import kotlinx.coroutines.delay

// =============================================================================
// SCREEN 1 — ACCESSIBILITY DISCLOSURE (Step 1 of 2)
// Reference: p5b_1_disclosure.png
// =============================================================================

@Composable
fun AccessibilityDisclosureScreen(
    onAgreeAndContinue: () -> Unit,
    onNotNow: () -> Unit
) {
    val typography = LocalChildTypography.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildColors.Background)
    ) {
        // Glowing arcs background decoration top-right
        GlowArcs(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Overline "STEP 1 OF 2"
                Text(
                    text = "STEP 1 OF 2",
                    style = typography.overline.copy(
                        fontSize = 12.sp,
                        letterSpacing = 1.4.sp,
                        color = ChildColors.Tertiary,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 64dp circle badge (1.5dp white border + ambient glow) with Accessibility icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ChildColors.BannerBg)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Accessibility,
                        contentDescription = "Accessibility",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Title: "How FOCAL uses\nAccessibility"
                Text(
                    text = "How FOCAL uses\nAccessibility",
                    style = typography.title.copy(
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle
                Text(
                    text = "Your parent set up FOCAL to help you focus. To do that, FOCAL needs the Accessibility service on this phone.",
                    style = typography.body.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = ChildColors.Secondary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // GlassCard with 3 capability rows
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 0.dp
                ) {
                    DisclosureCapabilityRow(
                        icon = FocalIcons.WindowPlay,
                        title = "Sees which app or website is open",
                        subtitle = "Only to check it against the list your parent chose."
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ChildColors.ListDivider)
                    )

                    DisclosureCapabilityRow(
                        icon = FocalIcons.ShieldCheck,
                        title = "Blocks apps, settings and uninstall",
                        subtitle = "During focus, and keeps FOCAL from being removed without your parent."
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ChildColors.ListDivider)
                    )

                    DisclosureCapabilityRow(
                        icon = FocalIcons.Close,
                        title = "Does not read or share your content",
                        subtitle = "No messages, photos, passwords or what you type."
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Note below card
                Text(
                    text = "You can read more in Privacy. Turning this off later tells your parent.",
                    style = typography.caption.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = ChildColors.Tertiary
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Bottom Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryButton(
                    text = "Agree and continue",
                    onClick = {
                        val timestamp = currentTimeMillis().toString()
                        FocalLocalStorage.saveString("KEY_ACCESSIBILITY_DISCLOSURE_AGREED_AT", timestamp)
                        FocalLocalStorage.saveBoolean("KEY_ACCESSIBILITY_DISCLOSURE_AGREED", true)
                        FocalLocalStorage.saveBoolean("KEY_DISCLOSURE_AGREED", true)
                        onAgreeAndContinue()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNotNow() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not now",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChildColors.Secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DisclosureCapabilityRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val typography = LocalChildTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
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
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.caption.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = ChildColors.Secondary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// =============================================================================
// SCREEN 2 — PROTECTION SETUP CHECKLIST (Step 2 of 2)
// Reference: p5b_2_checklist.png
// =============================================================================

data class ChecklistPermissionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isRequired: Boolean,
    val isGranted: Boolean,
    val onTurnOn: () -> Unit
)

@Composable
fun ProtectionSetupScreen(
    onAllSet: () -> Unit,
    onShowDisclosure: () -> Unit,
    onBack: () -> Unit
) {
    val typography = LocalChildTypography.current

    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Polling loop to continuously reflect settings changes upon returning
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            refreshTrigger++
        }
    }

    val deviceAdminLauncher = rememberDeviceAdminLauncher { isActive ->
        refreshTrigger++
    }

    // Permission states
    val isAccessibilityGranted = remember(refreshTrigger) { PlatformPermissionsHelper.isAccessibilityEnabled() }
    val isOverlayGranted = remember(refreshTrigger) { PlatformPermissionsHelper.isOverlayEnabled() }
    val isDeviceAdminGranted = remember(refreshTrigger) { PlatformPermissionsHelper.isDeviceAdminActive() }
    val isUsageGranted = remember(refreshTrigger) { PlatformPermissionsHelper.isUsageAccessEnabled() }
    val isNotificationsGranted = remember(refreshTrigger) { PlatformPermissionsHelper.isNotificationPermissionGranted() }
    val isBatteryGranted = remember(refreshTrigger) { PlatformPermissionsHelper.isBatteryOptimizationDisabled() }

    val permissionItems = remember(
        isAccessibilityGranted,
        isOverlayGranted,
        isDeviceAdminGranted,
        isUsageGranted,
        isNotificationsGranted,
        isBatteryGranted
    ) {
        listOf(
            ChecklistPermissionItem(
                title = "Accessibility service",
                subtitle = "Blocks apps and websites",
                icon = FocalIcons.Accessibility,
                isRequired = true,
                isGranted = isAccessibilityGranted,
                onTurnOn = {
                    val disclosureAgreed = FocalLocalStorage.getBoolean("KEY_ACCESSIBILITY_DISCLOSURE_AGREED", false) ||
                            FocalLocalStorage.getBoolean("KEY_DISCLOSURE_AGREED", false)
                    if (!disclosureAgreed) {
                        onShowDisclosure()
                    } else {
                        PlatformPermissionsHelper.openAccessibilitySettings()
                        refreshTrigger++
                    }
                }
            ),
            ChecklistPermissionItem(
                title = "Display over other apps",
                subtitle = "Shows the block screen",
                icon = FocalIcons.DisplayOverlay,
                isRequired = true,
                isGranted = isOverlayGranted,
                onTurnOn = {
                    PlatformPermissionsHelper.openOverlaySettings()
                    refreshTrigger++
                }
            ),
            ChecklistPermissionItem(
                title = "Device admin",
                subtitle = "Stops uninstalling FOCAL",
                icon = FocalIcons.ShieldCheck,
                isRequired = true,
                isGranted = isDeviceAdminGranted,
                onTurnOn = {
                    deviceAdminLauncher()
                    refreshTrigger++
                }
            ),
            ChecklistPermissionItem(
                title = "Usage access",
                subtitle = "Screen time in Insights",
                icon = FocalIcons.Chart,
                isRequired = false,
                isGranted = isUsageGranted,
                onTurnOn = {
                    PlatformPermissionsHelper.openUsageAccessSettings()
                    refreshTrigger++
                }
            ),
            ChecklistPermissionItem(
                title = "Notifications",
                subtitle = "Focus and schedule alerts",
                icon = FocalIcons.Bell,
                isRequired = false,
                isGranted = isNotificationsGranted,
                onTurnOn = {
                    PlatformPermissionsHelper.openNotificationSettings()
                    refreshTrigger++
                }
            ),
            ChecklistPermissionItem(
                title = "Battery unrestricted",
                subtitle = "Keeps FOCAL running",
                icon = FocalIcons.Battery,
                isRequired = false,
                isGranted = isBatteryGranted,
                onTurnOn = {
                    PlatformPermissionsHelper.openBatteryOptimizationSettings()
                    refreshTrigger++
                }
            )
        )
    }

    val doneCount = permissionItems.count { it.isGranted }
    val firstUngrantedIndex = permissionItems.indexOfFirst { !it.isGranted }

    // Required 3 must be granted before user can proceed: Accessibility + Overlay + Device Admin
    val requiredAllGranted = isAccessibilityGranted && isOverlayGranted && isDeviceAdminGranted

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Overline "STEP 2 OF 2"
                Text(
                    text = "STEP 2 OF 2",
                    style = typography.overline.copy(
                        fontSize = 12.sp,
                        letterSpacing = 1.4.sp,
                        color = ChildColors.Tertiary,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title: "Turn on protection"
                Text(
                    text = "Turn on protection",
                    style = typography.title.copy(
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "Each one opens a settings page. Turn it on, then come back here.",
                    style = typography.body.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = ChildColors.Secondary
                    )
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Segmented Progress Bar (6 segments)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until 6) {
                        val isSegmentFilled = i < doneCount
                        val segmentColor by animateColorAsState(
                            targetValue = if (isSegmentFilled) Color.White else ChildColors.Border,
                            animationSpec = tween(300)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(segmentColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle below segments: "{n} of 6 done"
                Text(
                    text = "$doneCount of 6 done",
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Secondary,
                        fontWeight = FontWeight.Normal
                    )
                )

                Spacer(modifier = Modifier.height(22.dp))

                // GlassCard with 6 permission rows
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 0.dp
                ) {
                    permissionItems.forEachIndexed { index, item ->
                        ChecklistPermissionRow(
                            item = item,
                            isFirstUngranted = (index == firstUngrantedIndex)
                        )

                        if (index < permissionItems.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(ChildColors.ListDivider)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Bottom action: "Finish setup" PrimaryButton
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (requiredAllGranted) {
                    PrimaryButton(
                        text = "Finish setup",
                        onClick = onAllSet,
                        enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    )
                } else {
                    // Disabled style: dark #1A1A1A container, text #6B6B6B, no glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(ChildColors.IconCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Finish setup",
                            color = ChildColors.Tertiary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row below button when disabled: small lock icon + "Required to start focus"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Lock,
                            contentDescription = null,
                            tint = ChildColors.Tertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Required to start focus",
                            fontSize = 12.sp,
                            color = ChildColors.Tertiary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistPermissionRow(
    item: ChecklistPermissionItem,
    isFirstUngranted: Boolean
) {
    val typography = LocalChildTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 40dp circle badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ChildColors.IconCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = typography.body.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = ChildColors.Secondary
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Trailing status/action
        if (item.isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = FocalIcons.Check,
                    contentDescription = "Done",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Done",
                    color = ChildColors.Secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else if (isFirstUngranted) {
            // Next-action solid white capsule pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
                    .clickable { item.onTurnOn() }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Turn on",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        } else {
            // Subsequent ungranted outlined capsule pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(ChildColors.BottomSheetBgStart)
                    .border(1.dp, ChildColors.Border, RoundedCornerShape(999.dp))
                    .clickable { item.onTurnOn() }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Turn on",
                    color = ChildColors.Tertiary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}


// =============================================================================
// SCREEN 3 — ALL SET (SetupCompleteScreen)
// Reference: p5b_3_all_set.png
// =============================================================================

@Composable
fun SetupCompleteScreen(
    onGoToHome: () -> Unit
) {
    val typography = LocalChildTypography.current

    // Scale animation (0.8 -> 1.0, 400ms)
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationStarted = true
    }

    val iconScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.8f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildColors.Background)
    ) {
        // Soft ambient radial glow behind hero
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.TopCenter)
        ) {
            val center = Offset(size.width / 2f, 150.dp.toPx())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = 160.dp.toPx()
                ),
                radius = 160.dp.toPx(),
                center = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // 116dp circle (2dp white border + ambient glow) with ShieldCheck icon
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(ChildColors.BannerBg)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.ShieldCheck,
                    contentDescription = "Protection on",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Overline "ALL SET"
            Text(
                text = "ALL SET",
                style = typography.overline.copy(
                    fontSize = 12.sp,
                    letterSpacing = 1.4.sp,
                    color = ChildColors.Tertiary,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Title: "Protection is on"
            Text(
                text = "Protection is on",
                style = typography.title.copy(
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle: "Tap your FOCAL card whenever it's time to focus."
            Text(
                text = "Tap your FOCAL card whenever it's time to focus.",
                style = typography.body.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = ChildColors.Secondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // GlassCard: 44dp NFC circle · "Start focus with your card" · "Or scan your parent's QR"
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
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
                            imageVector = FocalIcons.Nfc,
                            contentDescription = "FOCAL Card",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start focus with your card",
                            style = typography.body.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Or scan your parent's QR",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // PrimaryButton: "Go to Home"
            PrimaryButton(
                text = "Go to Home",
                onClick = {
                    FocalLocalStorage.saveBoolean("KEY_SETUP_DONE", true)
                    try {
                        com.focal.app.protection.ProtectionStateRepository.instance.updateSetupComplete(true)
                    } catch (_: Exception) {}
                    onGoToHome()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
