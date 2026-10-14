package com.focal.app.ui.child.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.ChildTheme
import com.focal.app.ui.theme.LocalChildTypography

/**
 * DEBUG-ONLY COMPONENT GALLERY SCREEN
 * Displays every component of the newly redesigned Child Design System.
 */
@Composable
fun ComponentGalleryScreen(
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(ChildTab.HOME) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var countdownProgress by remember { mutableFloatStateOf(0.72f) }

    ChildTheme {
        val typography = LocalChildTypography.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChildColors.Background)
        ) {
            // GlowArcs placed in top-right corner
            GlowArcs(modifier = Modifier.align(Alignment.TopEnd))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Header (Single Title per screen per design rule)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Component Gallery",
                            style = typography.display.copy(fontSize = 28.sp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "FOCAL Child App Design System (Step 0)",
                            style = typography.caption.copy(color = ChildColors.Secondary),
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .wrapContentWidth()
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .border(1.dp, ChildColors.BorderStrong, androidx.compose.foundation.shape.CircleShape)
                            .clickable(onClick = onBack)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Close",
                            style = typography.caption.copy(
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 1. FOCAL CARD HERO (Drawn in code, not a photo)
                SectionHeader("1. FOCAL CARD HERO (DRAWN IN CODE)")
                FocalCardHero()

                // 2. STREAK & STATUS PILLS
                SectionHeader("2. PILLS & BADGES (30–32DP)")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Streak Pill (1dp white border + lightning):", style = typography.caption)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StreakPill(streakCount = 14)
                        StreakPill(streakCount = 1)
                    }

                    Spacer(Modifier.height(6.dp))
                    Text("Status Pills (Active / Upcoming / Off):", style = typography.caption)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill(text = "Active", type = StatusPillType.ACTIVE)
                        StatusPill(text = "Upcoming 09:00", type = StatusPillType.UPCOMING)
                        StatusPill(text = "Completed", type = StatusPillType.OFF)
                    }
                }

                // 3. BUTTONS (PILL SHAPE, PRESSED SCALE 0.98)
                SectionHeader("3. BUTTONS (52–56DP PILLS)")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(
                        text = "Primary Button (White Gradient)",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                imageVector = FocalIcons.ArrowForward,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    SecondaryButton(
                        text = "Secondary Button (1dp #3A3A3A)",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                imageVector = FocalIcons.Nfc,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                // 4. GLASS CARD (GRADIENT + 1DP BORDER + TOP 6% HIGHLIGHT)
                SectionHeader("4. GLASS CARD (RADIUS 24DP)")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mathematics & Science",
                            style = typography.title.copy(fontSize = 20.sp),
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(12.dp))
                        StatusPill(text = "ACTIVE", type = StatusPillType.ACTIVE)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Gradient #131313→#0A0A0A with 1dp #262626 border and top inner 6% highlight line.",
                        style = typography.body.copy(color = ChildColors.Secondary)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3 Apps Blocked", style = typography.caption)
                        StreakPill(streakCount = 7)
                    }
                }

                // 5. ICON CIRCLE & AVATAR
                SectionHeader("5. ICON CIRCLE & AVATAR (#171717)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconCircle(size = 48.dp) {
                        Icon(imageVector = FocalIcons.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconCircle(size = 48.dp) {
                        Icon(imageVector = FocalIcons.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconCircle(size = 48.dp) {
                        Icon(imageVector = FocalIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    ChildAvatar(name = "Rahul Sharma", size = 48.dp)
                    ChildAvatar(name = "Ananya", size = 48.dp)
                }

                // 6. LIST ROWS (ICON CIRCLE + TITLE + SUBTITLE + CHEVRON + 1DP DIVIDER)
                SectionHeader("6. LIST ROWS (INSET DIVIDER)")
                GlassCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
                    ChildListRow(
                        title = "Hardware NFC Lock",
                        subtitle = "Paired card FC0001 (NTAG215)",
                        icon = { Icon(imageVector = FocalIcons.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                        onClick = {}
                    )
                    ChildListRow(
                        title = "Camera & Sensor Shield",
                        subtitle = "Accessibility protection active",
                        icon = { Icon(imageVector = FocalIcons.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                        onClick = {}
                    )
                    ChildListRow(
                        title = "Parent Custody Mode",
                        subtitle = "Card stays with child",
                        icon = { Icon(imageVector = FocalIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                        showDivider = false,
                        onClick = {}
                    )
                }

                // 7. COUNTDOWN RING
                SectionHeader("7. COUNTDOWN RING (#262626 TRACK + WHITE ARC)")
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CountdownRing(
                        progress = countdownProgress,
                        modifier = Modifier.size(200.dp),
                        strokeWidth = 10.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "02:35:14",
                                style = typography.display.copy(fontSize = 32.sp, color = Color.White)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "REMAINING",
                                style = typography.overline.copy(color = ChildColors.Secondary)
                            )
                        }
                    }
                }

                // 8. DANGER BANNER (ONLY FOR ERRORS & PROTECTION IS OFF)
                SectionHeader("8. DANGER BANNER (#FF453A)")
                ChildDangerBanner(
                    title = "Protection is off",
                    subtitle = "Turn Accessibility on to resume focus locks",
                    onFixClick = {}
                )

                // 9. FLOATING BOTTOM NAV (4 TABS ONLY)
                SectionHeader("9. FLOATING BOTTOM NAV (4 TABS ONLY)")
                Text("Interactive tab selector (Home / Schedules / Insights / Profile):", style = typography.caption)
                ChildBottomNav(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // 10. BOTTOM SHEET
                SectionHeader("10. BOTTOM SHEET (RADIUS 32DP, HANDLE BAR, SCRIM)")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(ChildColors.BottomSheetBgStart, ChildColors.BottomSheetBgEnd)
                            )
                        )
                        .border(
                            1.dp,
                            ChildColors.BorderStrong,
                            androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Handle bar
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(ChildColors.BorderStrong)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Card Verification Sheet",
                            style = typography.title.copy(fontSize = 20.sp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Hold physical card to phone to unlock focus session",
                            style = typography.body.copy(color = ChildColors.Secondary),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        PrimaryButton(
                            text = "Tap Card to Verify",
                            onClick = { showBottomSheet = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 10. FOCAL SWITCH (48x28dp)
                SectionHeader("10. FOCAL SWITCH (48x28DP)")
                var demoSwitch1 by remember { mutableStateOf(true) }
                var demoSwitch2 by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.focal.app.ui.child.components.FocalSwitch(
                        checked = demoSwitch1,
                        onCheckedChange = { demoSwitch1 = it }
                    )
                    Text("ON (White track + Black thumb)", style = typography.caption)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.focal.app.ui.child.components.FocalSwitch(
                        checked = demoSwitch2,
                        onCheckedChange = { demoSwitch2 = it }
                    )
                    Text("OFF (#262626 track + #6B6B6B thumb)", style = typography.caption)
                }

                Spacer(Modifier.height(84.dp))
            }

            // BottomSheet overlay
            if (showBottomSheet) {
                ChildBottomSheet(onDismiss = { showBottomSheet = false }) {
                    Text(
                        text = "Card Security Check",
                        style = typography.title.copy(fontSize = 22.sp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Hold your physical FOCAL card against the back of your phone to verify authorization.",
                        style = typography.body.copy(color = ChildColors.Secondary),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    FocalCardHero(modifier = Modifier.height(180.dp))
                    Spacer(Modifier.height(20.dp))
                    PrimaryButton(
                        text = "Done",
                        onClick = { showBottomSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = LocalChildTypography.current.overline.copy(
            color = ChildColors.Secondary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    )
}
