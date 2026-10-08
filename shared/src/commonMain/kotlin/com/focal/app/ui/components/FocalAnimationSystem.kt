package com.focal.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.theme.FocalColors
import focal.shared.generated.resources.Res
import focal.shared.generated.resources.focal_logo_hero
import focal.shared.generated.resources.focal_nfc_card_hero
import org.jetbrains.compose.resources.painterResource

// -------------------------------------------------------------
// FOCAL ANIMATION SYSTEM
// Reusable high-performance animations & micro-interactions
// -------------------------------------------------------------

/**
 * 1. FOCAL LOGO HERO COMPONENT (Using supplied image asset)
 * Includes scale 0.94 -> 1.0, soft opacity fade, and subtle bottom light/shadow glow.
 */
@Composable
fun FocalLogoHeroComponent(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    var animState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animState = true
    }

    val scale by animateFloatAsState(
        targetValue = if (animState) 1f else 0.94f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing)
    )

    val alpha by animateFloatAsState(
        targetValue = if (animState) 1f else 0f,
        animationSpec = tween(900, easing = LinearOutSlowInEasing)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Subtle light reflection glow below logo
        Box(
            modifier = Modifier
                .size(size * 1.2f, size * 0.35f)
                .offset(y = size * 0.45f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .graphicsLayer { this.alpha = alpha * 0.8f }
        )

        // Current FOCAL Logo Image Asset (Black & White)
        FocalAppLogo(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        )
    }
}

/**
 * 2. NFC CARD HERO COMPONENT (Using current FOCAL logo in black & white)
 * Entrance animation: scale 0.92 -> 1.0, 3D Y-axis tilt (-12° -> 0°),
 * followed by subtle idle floating & periodic 3D rotation (0° -> 4° -> -4° -> 0°).
 */
@Composable
fun NFCCardHeroComponent(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    interactive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfcHero")

    // Floating vertical Y-offset animation
    val translateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translateY"
    )

    // Periodic 3D Y-axis tilt rotation (0° -> 4° -> -4° -> 0°)
    val rotationY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotationY"
    )

    // Light sheen sweep position
    val sheenOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheenOffset"
    )

    var isEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isEntered = true
    }

    val enterScale by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0.92f,
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    )

    val enterAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(600, easing = LinearOutSlowInEasing)
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && interactive) 0.96f else 1f,
        animationSpec = tween(150)
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = translateY
                this.rotationY = rotationY
                scaleX = enterScale * pressScale
                scaleY = enterScale * pressScale
                alpha = enterAlpha
                cameraDistance = 16f * density
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = interactive
            ) {},
        contentAlignment = Alignment.Center
    ) {
        // Sleek Premium Black & White Physical NFC Card Container
        Box(
            modifier = Modifier
                .width(size)
                .height(size * 0.62f)
                .clip(RoundedCornerShape(18.dp))
                .background(FocalColors.Surface)
                .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            FocalAppLogo(
                modifier = Modifier.size(size * 0.42f)
            )
        }

        // Subtle specular light sheen overlay effect
        Box(
            modifier = Modifier
                .width(size)
                .height(size * 0.62f)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        start = androidx.compose.ui.geometry.Offset(sheenOffset * 300f, 0f),
                        end = androidx.compose.ui.geometry.Offset((sheenOffset + 0.4f) * 300f, 300f)
                    )
                )
        )
    }
}

/**
 * 3. ANIMATED NUMBER COUNTER (Smooth count-up to target integer)
 */
@Composable
fun AnimatedNumberCounter(
    targetValue: Int,
    prefix: String = "",
    suffix: String = "",
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    var count by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetValue) {
        val steps = 30
        val delayMs = 30L
        val increment = (targetValue.toFloat() / steps).coerceAtLeast(1f)
        var current = 0f
        for (i in 1..steps) {
            current += increment
            count = current.toInt().coerceAtMost(targetValue)
            kotlinx.coroutines.delay(delayMs)
        }
        count = targetValue
    }

    Text(
        text = "$prefix$count$suffix",
        style = textStyle
    )
}

/**
 * 4. ANIMATED BAR CHART (Rises from bottom to top for Analytics onboarding)
 */
@Composable
fun AnimatedBarChart(
    modifier: Modifier = Modifier
) {
    var animate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animate = true
    }

    val bar1Height by animateFloatAsState(if (animate) 0.65f else 0f, tween(700, 100, FastOutSlowInEasing))
    val bar2Height by animateFloatAsState(if (animate) 0.90f else 0f, tween(700, 250, FastOutSlowInEasing))
    val bar3Height by animateFloatAsState(if (animate) 0.45f else 0f, tween(700, 400, FastOutSlowInEasing))
    val bar4Height by animateFloatAsState(if (animate) 0.78f else 0f, tween(700, 550, FastOutSlowInEasing))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(FocalColors.Card, RoundedCornerShape(16.dp))
            .border(1.dp, FocalColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        val bars = listOf(
            Pair("Mon", bar1Height),
            Pair("Wed", bar2Height),
            Pair("Fri", bar3Height),
            Pair("Today", bar4Height)
        )

        bars.forEach { (label, heightFraction) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(heightFraction)
                        .width(18.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (label == "Today") FocalColors.PrimaryAccent
                            else FocalColors.CardElevated
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (label == "Today") Color.White else FocalColors.TextSecondary,
                        fontWeight = if (label == "Today") FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

/**
 * 5. NFC SCANNING PULSE ANIMATION (For hardware NFC test wizard step)
 */
@Composable
fun NFCScanningPulseAnimation(
    detected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!detected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(FocalColors.PrimaryAccent.copy(alpha = 0.25f), CircleShape)
                    .border(2.dp, FocalColors.PrimaryAccent, CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(if (detected) FocalColors.SuccessContainer else FocalColors.CardElevated)
                .border(
                    2.dp,
                    if (detected) FocalColors.Success else FocalColors.PrimaryAccent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (detected) "✓" else "NFC",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (detected) FocalColors.Success else Color.White
                )
            )
        }
    }
}
