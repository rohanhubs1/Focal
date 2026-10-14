package com.focal.app.ui.child.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import com.focal.app.data.EmergencyExitRepository
import com.focal.app.data.EmergencyRequestState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.ChildRadius
import com.focal.app.ui.theme.LocalChildTypography
import focal.shared.generated.resources.Res
import focal.shared.generated.resources.focal_logo
import org.jetbrains.compose.resources.painterResource
import com.focal.app.ui.components.FocalIcons


// =========================================================================
// 1. GlassCard
// Gradient #131313 -> #0A0A0A, 1dp #262626 border, 1dp top inner highlight (white 6%),
// radius 24dp, padding 16–20dp.
// =========================================================================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    radius: Dp = ChildRadius.Card,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ChildColors.CardGradientStart, ChildColors.CardGradientEnd)
                )
            )
            .border(
                width = 1.dp,
                color = ChildColors.Border,
                shape = shape
            )
            .drawWithContent {
                drawContent()
                // 1dp top inner highlight (white 6%)
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(24.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 24.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(padding)
    ) {
        Column(content = content)
    }
}

// =========================================================================
// 2. PrimaryButton
// 52–56dp pill, white -> #DEDEDE gradient, black text, soft white glow,
// optional leading/trailing icon. Scales 0.98 when pressed.
// =========================================================================
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val typography = LocalChildTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "btnScale"
    )
    val shape = CircleShape

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(54.dp)
            .drawBehind {
                // Soft white glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                        center = center,
                        radius = size.width / 1.8f
                    )
                )
            }
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color.White, ChildColors.ButtonGradientEnd)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.Black.copy(alpha = 0.2f)),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = typography.headline.copy(
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

// =========================================================================
// 3. SecondaryButton
// 52dp pill, transparent, 1dp #3A3A3A border, white text. Scales 0.98 pressed.
// =========================================================================
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val typography = LocalChildTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "secBtnScale"
    )
    val shape = CircleShape

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(52.dp)
            .clip(shape)
            .background(Color.Transparent)
            .border(1.dp, ChildColors.BorderStrong, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.1f)),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = typography.headline.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

// =========================================================================
// 4. IconCircle
// 44–52dp circle #171717 with white line icon (1.75dp stroke).
// =========================================================================
@Composable
fun IconCircle(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ChildColors.IconCircleBg),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// =========================================================================
// 5. Pill / Badge
// 30–32dp. Streak pill: 1dp white border, lightning icon + "N day streak".
// Status pills: Active (white fill + black text) · Next/Upcoming (1dp #3A3A3A + white text)
// · Off/Done (1dp #262626 + #6B6B6B text).
// =========================================================================
enum class StatusPillType {
    ACTIVE,
    UPCOMING,
    OFF
}

@Composable
fun StreakPill(
    streakCount: Int,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    Row(
        modifier = modifier
            .height(32.dp)
            .wrapContentWidth()
            .clip(CircleShape)
            .border(1.dp, Color.White, CircleShape)
            .background(Color.Transparent)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = FocalIcons.Lightning,
            contentDescription = "Streak",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = if (streakCount > 0) "$streakCount day streak" else "Start a streak",
            style = typography.caption.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    type: StatusPillType,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val bg = when (type) {
        StatusPillType.ACTIVE -> Color.White
        StatusPillType.UPCOMING, StatusPillType.OFF -> Color.Transparent
    }
    val borderModifier = when (type) {
        StatusPillType.ACTIVE -> Modifier
        StatusPillType.UPCOMING -> Modifier.border(1.dp, ChildColors.BorderStrong, CircleShape)
        StatusPillType.OFF -> Modifier.border(1.dp, ChildColors.Border, CircleShape)
    }
    val textColor = when (type) {
        StatusPillType.ACTIVE -> Color.Black
        StatusPillType.UPCOMING -> Color.White
        StatusPillType.OFF -> ChildColors.Tertiary
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .wrapContentWidth()
            .clip(CircleShape)
            .then(borderModifier)
            .background(bg)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = typography.caption.copy(
                color = textColor,
                fontWeight = if (type == StatusPillType.ACTIVE) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =========================================================================
// 6. Avatar
// Initials on #171717 circle with 1dp #3A3A3A border (no photos by default).
// =========================================================================
@Composable
fun ChildAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val typography = LocalChildTypography.current
    val initials = name.trim().split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifEmpty { "F" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ChildColors.IconCircleBg)
            .border(1.dp, ChildColors.BorderStrong, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = typography.headline.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp
            )
        )
    }
}

// =========================================================================
// 7. ListRow
// IconCircle + title + subtitle + chevron; rows separated by 1dp #1F1F1F inset divider.
// =========================================================================
@Composable
fun ChildListRow(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val typography = LocalChildTypography.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircle(size = 46.dp) {
                icon()
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.headline.copy(color = Color.White)
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = typography.caption.copy(color = ChildColors.Secondary)
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = FocalIcons.ArrowForward,
                    contentDescription = "Open",
                    tint = ChildColors.Tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 60.dp),
                thickness = 1.dp,
                color = ChildColors.ListDivider
            )
        }
    }
}

// =========================================================================
// 8. BottomNav
// Floating pill, 16dp side/bottom margin, height 70, radius 35, #0B0B0B + 1dp border.
// 4 tabs only: Home, Schedules, Insights, Profile.
// Selected = #1E1E1E highlight + white icon/text; unselected = #6B6B6B.
// =========================================================================
enum class ChildTab(val label: String) {
    HOME("Home"),
    SCHEDULES("Schedules"),
    INSIGHTS("Insights"),
    PROFILE("Profile")
}

@Composable
fun ChildBottomNav(
    selectedTab: ChildTab,
    onTabSelected: (ChildTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val shape = RoundedCornerShape(35.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .height(70.dp)
            .clip(shape)
            .background(ChildColors.NavBg)
            .border(1.dp, ChildColors.Border, shape)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChildTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val tabBg = if (isSelected) ChildColors.NavSelected else Color.Transparent
                val contentColor = if (isSelected) Color.White else ChildColors.Tertiary

                val iconVector = when (tab) {
                    ChildTab.HOME -> FocalIcons.Home
                    ChildTab.SCHEDULES -> FocalIcons.Calendar
                    ChildTab.INSIGHTS -> FocalIcons.Chart
                    ChildTab.PROFILE -> FocalIcons.User
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(tabBg)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = tab.label,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
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
        }
    }
}

// =========================================================================
// 9. GlowArcs
// Two thin glowing circle outlines in the top-right corner, same size/position on all 4 tab screens.
// =========================================================================
@Composable
fun GlowArcs(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val centerOffset = Offset(size.width + 20.dp.toPx(), -20.dp.toPx())
        val outerRadius = 260.dp.toPx()
        val innerRadius = 180.dp.toPx()

        listOf(outerRadius, innerRadius).forEach { r ->
            // Soft outer glow (white ~15-18% combined, blur ~40dp)
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = r,
                center = centerOffset,
                style = Stroke(width = 40.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = r,
                center = centerOffset,
                style = Stroke(width = 20.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = r,
                center = centerOffset,
                style = Stroke(width = 8.dp.toPx())
            )
            // Crisp border white at 32% (30–35% opacity)
            drawCircle(
                color = Color.White.copy(alpha = 0.32f),
                radius = r,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

// =========================================================================
// 10. FocalCardHero
// Drawn in code (not a photo): rounded black card (gradient #1D1D1D→#030303),
// 1.5dp white border with soft white glow, focal_logo centered, rotated tiltAngle;
// 2–3 thin ring ellipses under it and a radial glow behind.
// =========================================================================
@Composable
fun FocalCardHero(
    modifier: Modifier = Modifier,
    cardWidth: Dp = 230.dp,
    cardHeight: Dp = 144.dp,
    logoSize: Dp = 80.dp,
    tiltAngle: Float = -14f,
    pulsingRings: Boolean = false,
    ringCount: Int = 2
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
    val pulseScale by if (pulsingRings) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val pulseAlpha by if (pulsingRings) {
        infiniteTransition.animateFloat(
            initialValue = 0.05f,
            targetValue = 0.12f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableStateOf(0.08f) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Radial glow and thin ellipses under the card
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)

            // Radial soft glow behind
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.09f), Color.Transparent),
                    center = c,
                    radius = 140.dp.toPx()
                ),
                radius = 140.dp.toPx(),
                center = c
            )

            if (ringCount >= 3) {
                // Outer ring
                val r1W = 280.dp.toPx() * pulseScale
                val r1H = 90.dp.toPx() * pulseScale
                drawOval(
                    color = Color.White.copy(alpha = (pulseAlpha * 0.6f).coerceIn(0.03f, 0.15f)),
                    topLeft = Offset(c.x - r1W / 2f, c.y - r1H / 2f),
                    size = Size(r1W, r1H),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Middle ring
                val r2W = 210.dp.toPx() * pulseScale
                val r2H = 68.dp.toPx() * pulseScale
                drawOval(
                    color = Color.White.copy(alpha = pulseAlpha.coerceIn(0.05f, 0.18f)),
                    topLeft = Offset(c.x - r2W / 2f, c.y - r2H / 2f),
                    size = Size(r2W, r2H),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Inner ring
                val r3W = 150.dp.toPx() * pulseScale
                val r3H = 48.dp.toPx() * pulseScale
                drawOval(
                    color = Color.White.copy(alpha = (pulseAlpha * 1.3f).coerceIn(0.06f, 0.22f)),
                    topLeft = Offset(c.x - r3W / 2f, c.y - r3H / 2f),
                    size = Size(r3W, r3H),
                    style = Stroke(width = 1.dp.toPx())
                )
            } else if (ringCount in 1..2) {
                // Outer ellipse ring
                drawOval(
                    color = Color.White.copy(alpha = 0.05f),
                    topLeft = Offset(c.x - 140.dp.toPx(), c.y - 45.dp.toPx()),
                    size = Size(280.dp.toPx(), 90.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Inner ellipse ring
                drawOval(
                    color = Color.White.copy(alpha = 0.08f),
                    topLeft = Offset(c.x - 110.dp.toPx(), c.y - 35.dp.toPx()),
                    size = Size(220.dp.toPx(), 70.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // 2. The Physical Card drawn in code
        val cardShape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = tiltAngle
                    cameraDistance = 12f * density
                }
                .width(cardWidth)
                .height(cardHeight)
                .drawBehind {
                    // Soft white glow around border
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                            center = center,
                            radius = size.width / 1.5f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                    )
                }
                .clip(cardShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(ChildColors.CardGradientStart, ChildColors.CardGradientEnd),
                        start = Offset(0f, 0f),
                        end = Offset(cardWidth.value, cardHeight.value)
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = Color.White,
                    shape = cardShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Centered official white FOCAL logo ONLY
            Image(
                painter = painterResource(Res.drawable.focal_logo),
                contentDescription = "FOCAL Logo",
                modifier = Modifier.size(logoSize)
            )
        }
    }
}

// =========================================================================
// 11. BottomSheet
// Top radius 32, background #141414→#0A0A0A, 1dp top border, handle bar, dark scrim behind.
// =========================================================================
@Composable
fun ChildBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // consume clicks inside sheet
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(ChildColors.BottomSheetBgStart, ChildColors.BottomSheetBgEnd)
                    )
                )
                .border(1.dp, ChildColors.BorderStrong, shape)
                .padding(horizontal = 20.dp, vertical = 14.dp)
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
                        .clip(CircleShape)
                        .background(ChildColors.BorderStrong)
                )
                Spacer(Modifier.height(18.dp))
                content()
            }
        }
    }
}

// =========================================================================
// 12. CountdownRing
// White arc on #262626 track, rounded ends.
// =========================================================================
@Composable
fun CountdownRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    centerContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier.size(200.dp).then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val radius = (size.minDimension - strokePx) / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Background Track #262626
            drawCircle(
                color = ChildColors.Border,
                radius = radius,
                center = c,
                style = Stroke(width = strokePx)
            )

            // Foreground white progress arc
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = clampedProgress * 360f,
                useCenter = false,
                topLeft = Offset(c.x - radius, c.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        if (centerContent != null) {
            centerContent()
        }
    }
}

// =========================================================================
// 13. Banner (danger)
// #0E0E0E background, 1dp #FF453A border, red dot, title + subtitle + "Fix" button.
// =========================================================================
@Composable
fun ChildDangerBanner(
    title: String,
    subtitle: String,
    onFixClick: () -> Unit,
    modifier: Modifier = Modifier,
    fixButtonText: String = "Fix"
) {
    val typography = LocalChildTypography.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ChildColors.BannerBg)
            .border(1.dp, ChildColors.Danger, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ChildColors.Danger)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.headline.copy(
                    color = ChildColors.Danger,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.caption.copy(
                    color = ChildColors.Secondary
                )
            )
        }
        Spacer(Modifier.width(8.dp))
        // Small outlined pill (1dp white border, white text)
        Box(
            modifier = Modifier
                .height(32.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White, CircleShape)
                .clickable(onClick = onFixClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fixButtonText,
                style = typography.caption.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// =========================================================================
// 14. FocusActivePill
// Rounded capsule with 1dp border, white dot, uppercase "FOCUS ACTIVE", letter-spacing 1.5sp
// =========================================================================
@Composable
fun FocusActivePill(
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    Row(
        modifier = modifier
            .height(32.dp)
            .wrapContentWidth()
            .clip(CircleShape)
            .border(1.dp, ChildColors.BorderStrong, CircleShape)
            .background(ChildColors.BannerBg)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Text(
            text = "FOCUS ACTIVE",
            style = typography.caption.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =========================================================================
// 15. TapCardBottomSheet
// ModalBottomSheet: background #141414→#0A0A0A, top radius 32, 1dp #2A2A2A top border,
// drag handle. Scrim dims home behind.
// Overline "START FOCUS" / "END FOCUS" -> title -> subtitle
// FocalCardHero tilted ~-12° with 3 pulsing concentric ellipse rings (1.6s)
// Reader-state StatusPill: listening, no reader, success, wrong card
// SecondaryButton "Cancel"
// =========================================================================
enum class ReaderState {
    LISTENING_USB,
    LISTENING_NFC,
    NO_READER,
    SUCCESS,
    WRONG_CARD
}

@Composable
fun TapCardBottomSheet(
    isEndingFocus: Boolean = false,
    overlineText: String? = null,
    readerState: ReaderState = ReaderState.LISTENING_USB,
    onDismiss: () -> Unit,
    onCardScanned: () -> Unit = {},
    onTestWrongCard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current

    ChildBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        Text(
            text = overlineText ?: if (isEndingFocus) "END FOCUS" else "START FOCUS",
            style = typography.overline.copy(
                color = ChildColors.Tertiary,
                letterSpacing = 1.5.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Hold your card near the phone",
            style = typography.title.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Keep it still for a second.",
            style = typography.body.copy(color = ChildColors.Secondary),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))

        // Illustration: FocalCardHero tilted -12° with 3 pulsing concentric ellipse rings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCardScanned
                )
        ) {
            FocalCardHero(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                tiltAngle = -12f,
                pulsingRings = true,
                ringCount = 3
            )
        }

        Spacer(Modifier.height(18.dp))

        // Reader-state StatusPill
        when (readerState) {
            ReaderState.LISTENING_USB -> {
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .border(1.dp, ChildColors.BorderStrong, CircleShape)
                        .background(ChildColors.BannerBg)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "Listening · FOCAL reader connected",
                        style = typography.caption.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ReaderState.LISTENING_NFC -> {
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .border(1.dp, ChildColors.BorderStrong, CircleShape)
                        .background(ChildColors.BannerBg)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "Listening for card",
                        style = typography.caption.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ReaderState.NO_READER -> {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .border(1.dp, ChildColors.Border, CircleShape)
                        .background(Color.Transparent)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Connect the FOCAL reader",
                        style = typography.caption.copy(
                            color = ChildColors.Tertiary,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ReaderState.SUCCESS -> {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (overlineText == "TEST CARD") "✓ Card works" else "✓ Card accepted",
                        style = typography.caption.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ReaderState.WRONG_CARD -> {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .border(1.dp, ChildColors.BorderStrong, CircleShape)
                        .background(ChildColors.BannerBg)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "This card isn't linked to you",
                        style = typography.caption.copy(
                            color = ChildColors.Secondary,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        // SecondaryButton "Cancel"
        SecondaryButton(
            text = "Cancel",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
    }
}

// =========================================================================
// 16. EndFocusBottomSheet
// Opens from "End focus" on Home (focus active).
// ModalBottomSheet: gradient #141414→#0A0A0A, top radius 32, #2A2A2A top border, drag handle.
// =========================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EndFocusBottomSheet(
    policyName: String,
    remainingMinutes: Int,
    onDismiss: () -> Unit,
    onCardTapped: () -> Unit,
    cardListeningSubtitle: String = "Listening… the same card ends focus",
    onRequestEmergencyExit: (String) -> Unit = {},
    isRequestPending: Boolean = false,
    parentName: String = "Mom",
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    var selectedReason by remember { mutableStateOf<String?>(null) }

    val formattedTimeLeft = when {
        remainingMinutes >= 60 && remainingMinutes % 60 == 0 -> "${remainingMinutes / 60}h"
        remainingMinutes >= 60 -> "${remainingMinutes / 60}h ${remainingMinutes % 60}m"
        else -> "${remainingMinutes}m"
    }

    val reasons = listOf(
        "Need to call someone",
        "Homework app",
        "Emergency",
        "Other"
    )

    ChildBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        // Title
        Text(
            text = "End focus early?",
            style = typography.title.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        // Subtitle
        Text(
            text = "$formattedTimeLeft left in $policyName. Only your card or your parent can end it.",
            style = typography.body.copy(
                color = ChildColors.Secondary,
                fontSize = 14.5.sp,
                lineHeight = 20.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // Card listening row (GlassCard, radius 20)
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardTapped() },
            radius = 20.dp,
            padding = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 64dp icon with 2 concentric rings + NFC waves glyph
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        // Solid dark background
                        drawCircle(color = ChildColors.IconCircleBg, radius = 30.dp.toPx(), center = c)
                        // Outer ring (white 15%)
                        drawCircle(color = Color.White.copy(alpha = 0.15f), radius = 31.dp.toPx(), center = c, style = Stroke(1.dp.toPx()))
                        // Inner ring (white 35%)
                        drawCircle(color = Color.White.copy(alpha = 0.35f), radius = 23.dp.toPx(), center = c, style = Stroke(1.dp.toPx()))
                    }
                    Icon(
                        imageVector = FocalIcons.Nfc,
                        contentDescription = "NFC card",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tap your FOCAL card",
                        style = typography.headline.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = cardListeningSubtitle,
                        style = typography.caption.copy(
                            color = ChildColors.Secondary,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Divider with centred text "or ask your parent"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = ChildColors.Border)
            Text(
                text = "or ask your parent",
                style = typography.caption.copy(
                    color = ChildColors.Tertiary,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = ChildColors.Border)
        }

        // "Why do you need to stop?" + reason chips
        Text(
            text = "Why do you need to stop?",
            style = typography.headline.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            reasons.forEach { reason ->
                val isSelected = selectedReason == reason
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) ChildColors.IconCircleBg else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.White else ChildColors.BorderStrong,
                            shape = CircleShape
                        )
                        .clickable(enabled = !isRequestPending) {
                            selectedReason = if (isSelected) null else reason
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reason,
                        style = typography.body.copy(
                            color = if (isSelected) Color.White else ChildColors.ChipTextUnselected,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Pending GlassCard (if request sent)
        if (isRequestPending) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 16.dp,
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pendingSpinner")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "spinnerAngle"
                    )
                    Canvas(modifier = Modifier.size(24.dp)) {
                        rotate(angle) {
                            drawArc(
                                color = Color.White,
                                startAngle = 0f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Request sent to $parentName",
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Waiting for approval · focus continues",
                            style = typography.caption.copy(color = ChildColors.Secondary),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // SecondaryButton "Request emergency exit"
        SecondaryButton(
            text = "Request emergency exit",
            onClick = {
                val reason = selectedReason ?: return@SecondaryButton
                onRequestEmergencyExit(reason)
            },
            enabled = selectedReason != null && !isRequestPending,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Phone icon + "Calls and emergency SOS always work" (tertiary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = FocalIcons.Phone,
                contentDescription = null,
                tint = ChildColors.Tertiary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Calls and emergency SOS always work",
                style = typography.caption.copy(
                    color = ChildColors.Tertiary,
                    fontSize = 13.sp
                )
            )
        }

        Spacer(Modifier.height(10.dp))
    }
}

// =========================================================================
// 15. FocalSwitch
// 48×28dp pill: ON = white track + black thumb, OFF = #262626 track + #6B6B6B thumb, 150ms animation.
// =========================================================================
@Composable
fun FocalSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color.White else ChildColors.Border,
        animationSpec = tween(150),
        label = "switchTrackColor"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.Black else ChildColors.Tertiary,
        animationSpec = tween(150),
        label = "switchThumbColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 23.dp else 3.dp,
        animationSpec = tween(150),
        label = "switchThumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .then(
                if (enabled && onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onCheckedChange(!checked) }
                    )
                } else {
                    Modifier
                }
            )
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

