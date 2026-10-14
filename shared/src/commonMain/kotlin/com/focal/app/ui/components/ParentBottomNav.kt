package com.focal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography

/**
 * 5 Parent tabs for Controller navigation
 */
enum class ParentTab(val label: String) {
    HOME("Home"),
    POLICIES("Policies"),
    FOCUS("Focus"),
    ACTIVITY("Activity"),
    PROFILE("Profile")
}

/**
 * Redesigned Parent Floating Bottom Navigation Bar.
 * Matches P0 specification with 5 slots and raised center Focus button.
 */
@Composable
fun ParentBottomNav(
    selectedTab: ParentTab,
    onTabSelected: (ParentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val barShape = RoundedCornerShape(35.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Floating navigation bar (70dp height, #0B0B0B, #262626 border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(barShape)
                .background(ChildColors.NavBg)
                .border(1.dp, ChildColors.Border, barShape)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Home
                ParentNavTabItem(
                    label = "Home",
                    icon = FocalIcons.Home,
                    isSelected = selectedTab == ParentTab.HOME,
                    onClick = { onTabSelected(ParentTab.HOME) }
                )

                // Tab 1: Policies
                ParentNavTabItem(
                    label = "Policies",
                    icon = FocalIcons.Shield,
                    isSelected = selectedTab == ParentTab.POLICIES,
                    onClick = { onTabSelected(ParentTab.POLICIES) }
                )

                // Gap for the raised center Focus button
                Spacer(modifier = Modifier.width(64.dp))

                // Tab 3: Activity
                ParentNavTabItem(
                    label = "Activity",
                    icon = FocalIcons.Activity,
                    isSelected = selectedTab == ParentTab.ACTIVITY,
                    onClick = { onTabSelected(ParentTab.ACTIVITY) }
                )

                // Tab 4: Profile
                ParentNavTabItem(
                    label = "Profile",
                    icon = FocalIcons.User,
                    isSelected = selectedTab == ParentTab.PROFILE,
                    onClick = { onTabSelected(ParentTab.PROFILE) }
                )
            }
        }

        // 2. Centre "Focus" = raised 60dp circle (QR icon), sits 26dp above the bar
        // #0B0B0B with #3A3A3A border + soft white glow; when selected it turns white with black icon.
        val isFocusSelected = selectedTab == ParentTab.FOCUS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-8).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .drawBehind {
                        // Soft white glow
                        drawCircle(
                            color = Color.White.copy(alpha = if (isFocusSelected) 0.18f else 0.08f),
                            radius = 38.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = if (isFocusSelected) 0.10f else 0.04f),
                            radius = 52.dp.toPx()
                        )
                    }
                    .clip(CircleShape)
                    .background(if (isFocusSelected) Color.White else ChildColors.NavBg)
                    .border(
                        width = 1.dp,
                        color = if (isFocusSelected) Color.White else ChildColors.BorderStrong,
                        shape = CircleShape
                    )
                    .clickable { onTabSelected(ParentTab.FOCUS) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.QrCode,
                    contentDescription = "Focus",
                    tint = if (isFocusSelected) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Focus",
                style = typography.caption.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isFocusSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isFocusSelected) Color.White else ChildColors.Tertiary
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ParentNavTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typography = LocalChildTypography.current
    val tabBg = if (isSelected) ChildColors.NavSelected else Color.Transparent
    val contentColor = if (isSelected) Color.White else ChildColors.Tertiary

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tabBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = typography.caption.copy(
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}
