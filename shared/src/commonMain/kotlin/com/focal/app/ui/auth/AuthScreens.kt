package com.focal.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.config.FocalConfig
import com.focal.app.data.AuthRepository
import com.focal.app.data.FocalLocalStorage
import com.focal.app.data.FocalRepository
import com.focal.app.model.UserRole
import com.focal.app.model.currentTimeMillis
import com.focal.app.theme.FocalColors
import com.focal.app.ui.controller.AddCardSheet
import com.focal.app.ui.child.components.FocalCardHero
import com.focal.app.ui.child.components.GlassCard
import com.focal.app.ui.child.components.PrimaryButton
import com.focal.app.ui.child.components.SecondaryButton
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography
import focal.shared.generated.resources.Res
import focal.shared.generated.resources.focal_logo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

// =============================================================================
// SCREEN 1 — SPLASH (ANIMATED) [PO0_splash.png]
// =============================================================================
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val lastOpenTime = remember { FocalLocalStorage.getLong("KEY_LAST_APP_OPEN_TIME", 0L) }
    val now = remember { currentTimeMillis() }
    val isWarmStart = remember { (now - lastOpenTime) in 1..30_000L }
    val isAnimationsDisabled = remember { PlatformPermissionsHelper.isAnimationsDisabled() }
    val skipAnimation = isWarmStart || isAnimationsDisabled

    LaunchedEffect(Unit) {
        FocalLocalStorage.saveLong("KEY_LAST_APP_OPEN_TIME", now)
    }

    val cardAlpha = remember { Animatable(if (skipAnimation) 1f else 0f) }
    val cardOffsetY = remember { Animatable(if (skipAnimation) 0f else 24f) }
    val cardRotation = remember { Animatable(if (skipAnimation) -14f else -20f) }
    val lineWidth = remember { Animatable(if (skipAnimation) 300f else 0f) }
    val sweepProgress = remember { Animatable(if (skipAnimation) 1f else 0f) }
    val glowPulseAlpha = remember { Animatable(0f) }
    val screenFadeAlpha = remember { Animatable(1f) }

    LaunchedEffect(skipAnimation) {
        if (skipAnimation) {
            delay(120)
            onSplashFinished()
            return@LaunchedEffect
        }

        // 0–500ms: Card fades in (0→1), rises 24dp, rotation -20°→-14°
        launch {
            cardAlpha.animateTo(1f, tween(500, easing = LinearOutSlowInEasing))
        }
        launch {
            cardOffsetY.animateTo(0f, tween(500, easing = LinearOutSlowInEasing))
        }
        launch {
            cardRotation.animateTo(-14f, tween(500, easing = LinearOutSlowInEasing))
        }

        // 300–800ms: Light line grows from center outwards (0→300dp)
        launch {
            delay(300)
            lineWidth.animateTo(300f, tween(500, easing = FastOutSlowInEasing))
        }

        // 600–1100ms: One bright light sweep travels along the card outline
        launch {
            delay(600)
            sweepProgress.animateTo(1f, tween(500, easing = LinearEasing))
        }

        // 1100–1600ms: Glow pulses once, then fade to next screen
        launch {
            delay(1100)
            glowPulseAlpha.animateTo(0.35f, tween(200, easing = FastOutSlowInEasing))
            glowPulseAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
            delay(50)
            screenFadeAlpha.animateTo(0f, tween(100, easing = LinearEasing))
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer { alpha = screenFadeAlpha.value },
        contentAlignment = Alignment.Center
    ) {
        // Soft ambient radial glow behind the card
        Canvas(modifier = Modifier.size(360.dp)) {
            val totalGlowAlpha = 0.12f + glowPulseAlpha.value
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = totalGlowAlpha),
                        Color.White.copy(alpha = totalGlowAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width / 2f
                ),
                radius = size.width / 2f,
                center = center
            )
        }

        // The Tilted Card Hero (~250dp wide, ~156dp high, -14° tilt)
        val cardShape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = cardOffsetY.value * density
                    rotationZ = cardRotation.value
                    alpha = cardAlpha.value
                }
                .width(250.dp)
                .height(156.dp)
                .drawBehind {
                    // Soft white glow directly around card
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.22f + glowPulseAlpha.value * 0.5f), Color.Transparent),
                            center = center,
                            radius = size.width / 1.4f
                        ),
                        cornerRadius = CornerRadius(18.dp.toPx())
                    )
                }
                .clip(cardShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF141414), Color(0xFF020202)),
                        start = Offset(0f, 0f),
                        end = Offset(250f * 2.5f, 156f * 2.5f)
                    )
                )
                .border(1.5.dp, Color.White, cardShape),
            contentAlignment = Alignment.Center
        ) {
            // Outline light sweep overlay (600–1100ms)
            if (sweepProgress.value > 0f && sweepProgress.value < 1f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(0f, 0f, size.width, size.height),
                                cornerRadius = CornerRadius(18.dp.toPx())
                            )
                        )
                    }
                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(path, false)
                    val totalLen = pathMeasure.length
                    val segmentLen = 140.dp.toPx()
                    val head = sweepProgress.value * (totalLen + segmentLen)
                    val start = (head - segmentLen).coerceAtLeast(0f)
                    val end = head.coerceAtMost(totalLen)
                    if (end > start) {
                        val segmentPath = Path()
                        pathMeasure.getSegment(start, end, segmentPath, true)
                        drawPath(
                            path = segmentPath,
                            color = Color.White,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Official white FOCAL logo emblem in center
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.focal_logo),
                contentDescription = "FOCAL Logo",
                modifier = Modifier.size(68.dp)
            )
        }

        // Thin horizontal light line under the card (growing from center 0→300dp)
        if (lineWidth.value > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 118.dp)
                    .width(lineWidth.value.dp)
                    .height(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft glow layer behind line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Crisp 1.5dp central light beam
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.5f),
                                    Color.White,
                                    Color.White.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

// =============================================================================
// SCREEN 2 — HOW IT WORKS [PO0b_how_it_works.png]
// =============================================================================
@Composable
fun HowItWorksScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    val typography = LocalChildTypography.current

    val step0Alpha = remember { Animatable(0f) }
    val step0OffsetY = remember { Animatable(16f) }
    val step1Alpha = remember { Animatable(0f) }
    val step1OffsetY = remember { Animatable(16f) }
    val step2Alpha = remember { Animatable(0f) }
    val step2OffsetY = remember { Animatable(16f) }
    val step3Alpha = remember { Animatable(0f) }
    val step3OffsetY = remember { Animatable(16f) }
    val lineProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Line draws progressively
        launch {
            lineProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }

        // Step 1 (0ms)
        launch {
            step0Alpha.animateTo(1f, tween(300))
            step0OffsetY.animateTo(0f, tween(300, easing = LinearOutSlowInEasing))
        }

        // Step 2 (120ms)
        launch {
            delay(120)
            step1Alpha.animateTo(1f, tween(300))
            step1OffsetY.animateTo(0f, tween(300, easing = LinearOutSlowInEasing))
        }

        // Step 3 (240ms)
        launch {
            delay(240)
            step2Alpha.animateTo(1f, tween(300))
            step2OffsetY.animateTo(0f, tween(300, easing = LinearOutSlowInEasing))
        }

        // Step 4 (360ms)
        launch {
            delay(360)
            step3Alpha.animateTo(1f, tween(300))
            step3OffsetY.animateTo(0f, tween(300, easing = LinearOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Dotted white curve drawn behind everything
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 110.dp, bottom = 100.dp)
        ) {
            val w = size.width
            val h = size.height

            // Connects centers of the 4 tiles in a gentle S-curve zig-zag
            // Tile 1: right side (~w * 0.82f, ~h * 0.10f)
            // Tile 2: left side  (~w * 0.18f, ~h * 0.35f)
            // Tile 3: right side (~w * 0.82f, ~h * 0.60f)
            // Tile 4: left side  (~w * 0.18f, ~h * 0.85f)
            val p1 = Offset(w * 0.78f, h * 0.12f)
            val p2 = Offset(w * 0.22f, h * 0.38f)
            val p3 = Offset(w * 0.78f, h * 0.62f)
            val p4 = Offset(w * 0.22f, h * 0.88f)

            val fullCurve = Path().apply {
                moveTo(p1.x, p1.y)
                cubicTo(
                    p1.x - w * 0.35f, p1.y + h * 0.10f,
                    p2.x + w * 0.35f, p2.y - h * 0.10f,
                    p2.x, p2.y
                )
                cubicTo(
                    p2.x + w * 0.35f, p2.y + h * 0.10f,
                    p3.x - w * 0.35f, p3.y - h * 0.10f,
                    p3.x, p3.y
                )
                cubicTo(
                    p3.x - w * 0.35f, p3.y + h * 0.10f,
                    p4.x + w * 0.35f, p4.y - h * 0.10f,
                    p4.x, p4.y
                )
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(fullCurve, false)
            val totalLength = pathMeasure.length
            val drawLength = totalLength * lineProgress.value

            if (drawLength > 0f) {
                val partialPath = Path()
                pathMeasure.getSegment(0f, drawLength, partialPath, true)
                drawPath(
                    path = partialPath,
                    color = Color.White.copy(alpha = 0.55f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 16f), 0f)
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Row: Overline + Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOW FOCAL WORKS",
                    style = typography.caption.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFFA3A3A3)
                    )
                )

                Text(
                    text = "Skip",
                    style = typography.body.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA3A3A3)
                    ),
                    modifier = Modifier
                        .clickable { onSkip() }
                        .padding(6.dp)
                )
            }

            // Title: "Focus in 4 steps" (34sp bold)
            Text(
                text = "Focus in 4 steps",
                style = typography.headline.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Zig-Zag Steps in scrollable content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Step 1: Text Left / Tile Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = step0Alpha.value
                            translationY = step0OffsetY.value * density
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        StepNumberBadge(number = "1")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Parent sets up",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create an account and connect your child's phone.",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary,
                                lineHeight = 18.sp
                            )
                        )
                    }

                    StepIconTile(
                        icon = FocalIcons.Users,
                        tiltAngle = 8f
                    )
                }

                // Step 2: Tile Left / Text Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = step1Alpha.value
                            translationY = step1OffsetY.value * density
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepIconTile(
                        icon = FocalIcons.Shield,
                        tiltAngle = -8f
                    )

                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        StepNumberBadge(number = "2")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Choose what to block",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pick apps, websites and focus times.",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                // Step 3: Text Left / Tile Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = step2Alpha.value
                            translationY = step2OffsetY.value * density
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        StepNumberBadge(number = "3")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap card or scan QR",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the FOCAL card or scan the family QR to start.",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary,
                                lineHeight = 18.sp
                            )
                        )
                    }

                    StepIconTile(
                        icon = FocalIcons.Nfc,
                        tiltAngle = 8f
                    )
                }

                // Step 4: Tile Left / Text Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = step3Alpha.value
                            translationY = step3OffsetY.value * density
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepIconTile(
                        icon = FocalIcons.ShieldCheck,
                        tiltAngle = -8f
                    )

                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        StepNumberBadge(number = "4")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Focus is on",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Apps and websites stay blocked until focus ends.",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }

        // Pinned Bottom Action: "Get started"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            PrimaryButton(
                text = "Get started",
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepNumberBadge(number: String) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF0D0D0D))
            .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

@Composable
private fun StepIconTile(
    icon: ImageVector,
    tiltAngle: Float
) {
    val tileShape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .graphicsLayer { rotationZ = tiltAngle }
            .size(92.dp)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                        center = center,
                        radius = size.width / 1.3f
                    ),
                    cornerRadius = CornerRadius(26.dp.toPx())
                )
            }
            .clip(tileShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF161616), Color(0xFF090909)),
                    start = Offset(0f, 0f),
                    end = Offset(92f * 2.5f, 92f * 2.5f)
                )
            )
            .border(1.5.dp, Color.White, tileShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

// =============================================================================
// SCREEN 3 — WELCOME [PO1_welcome.png]
// =============================================================================
@Composable
fun WelcomeScreen(
    onParentSelected: () -> Unit,
    onChildSelected: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val typography = LocalChildTypography.current
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Centered FocalCardHero (-12° tilt, 2 ellipse rings)
            FocalCardHero(
                cardWidth = 240.dp,
                cardHeight = 150.dp,
                tiltAngle = -12f,
                ringCount = 2,
                pulsingRings = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to FOCAL",
                style = typography.headline.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Who is using this phone?",
                style = typography.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Two role cards side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Parent
                RoleChoiceCard(
                    title = "I'm a parent",
                    subtitle = "Set up focus and manage my children",
                    icon = FocalIcons.Users,
                    isSelected = selectedRole == UserRole.CONTROLLER,
                    onClick = { selectedRole = UserRole.CONTROLLER },
                    modifier = Modifier.weight(1f)
                )

                // Card 2: Child
                RoleChoiceCard(
                    title = "I'm a child",
                    subtitle = "Connect this phone to my parent",
                    icon = FocalIcons.User,
                    isSelected = selectedRole == UserRole.CHILD,
                    onClick = { selectedRole = UserRole.CHILD },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Pinned Bottom Actions: Continue Button + Already have account / Log in
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryButton(
                text = "Continue",
                enabled = selectedRole != null,
                onClick = {
                    if (selectedRole == UserRole.CONTROLLER) {
                        FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "PARENT")
                        onParentSelected()
                    } else if (selectedRole == UserRole.CHILD) {
                        FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "CHILD")
                        onChildSelected()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = typography.caption.copy(
                        fontSize = 14.sp,
                        color = ChildColors.Secondary
                    )
                )
                Text(
                    text = "Log in",
                    style = typography.caption.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(2.dp)
                )
            }
        }
    }
}

@Composable
private fun RoleChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .height(200.dp)
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF131313), Color(0xFF090909))
                )
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color.White else Color(0xFF262626),
                shape = cardShape
            )
            .drawBehind {
                if (isSelected) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                            center = center,
                            radius = size.width / 1.1f
                        ),
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        // Radio top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 1.5.dp,
                    color = if (isSelected) Color.White else Color(0xFF3A3A3A),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        // Center Content: 56dp icon circle, title, subtitle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF171717))
                    .border(1.dp, Color(0xFF262626), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = typography.headline.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = ChildColors.Secondary,
                    lineHeight = 16.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============================================================================
// SCREEN 4 — CREATE ACCOUNT (PARENT) [PO2_create_account.png, LEFT]
// =============================================================================
@Composable
fun CreateAccountScreen(
    onAccountCreated: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    authRepository: AuthRepository = AuthRepository.instance
) {
    val typography = LocalChildTypography.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var termsAgreed by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val isEmailValid = email.contains("@") && email.contains(".")
    val isFormValid = name.isNotBlank() && isEmailValid && password.length >= 8 && termsAgreed

    fun submitSignUp() {
        if (!isFormValid || isLoading) return
        errorMessage = null
        isLoading = true
        coroutineScope.launch {
            val result = authRepository.signUp(name, email, password)
            isLoading = false
            result.fold(
                onSuccess = {
                    onAccountCreated()
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Sign up failed"
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Round back button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0B))
                    .border(1.dp, Color(0xFF262626), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create your account",
                style = typography.headline.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "For parents. Your children won't need an account.",
                style = typography.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Continue with Google PrimaryButton
            PrimaryButton(
                text = "Continue with Google",
                leadingIcon = {
                    Icon(
                        imageVector = FocalIcons.Google,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        val res = authRepository.signInWithGoogle()
                        isLoading = false
                        res.fold(
                            onSuccess = { onAccountCreated() },
                            onFailure = { errorMessage = it.message ?: "Google sign-in failed" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Divider: ───── or with email ─────
            OrDivider(text = "or with email")

            Spacer(modifier = Modifier.height(22.dp))

            // Full Name input field
            FocalAuthInputField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                placeholder = "Full name",
                leadingIcon = FocalIcons.User,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Email Address input field
            FocalAuthInputField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                placeholder = "Email address",
                leadingIcon = FocalIcons.Mail,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password input field + Eye Toggle
            FocalAuthInputField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = "Password",
                leadingIcon = FocalIcons.Lock,
                trailingIcon = if (isPasswordVisible) FocalIcons.EyeOff else FocalIcons.Eye,
                onTrailingIconClick = { isPasswordVisible = !isPasswordVisible },
                isPassword = !isPasswordVisible,
                imeAction = ImeAction.Done,
                onDone = { submitSignUp() }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "At least 8 characters",
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF6B6B6B)
                ),
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Checkbox: "I agree to the Terms and Privacy Policy"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { termsAgreed = !termsAgreed },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (termsAgreed) Color.White else Color(0xFF141414))
                        .border(1.dp, if (termsAgreed) Color.White else Color(0xFF333333), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (termsAgreed) {
                        Icon(
                            imageVector = FocalIcons.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "I agree to the ",
                        style = typography.caption.copy(fontSize = 13.sp, color = Color.White)
                    )
                    Text(
                        text = "Terms",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier.clickable {
                            PlatformPermissionsHelper.openUrl(FocalConfig.TERMS_URL)
                        }
                    )
                    Text(
                        text = " and ",
                        style = typography.caption.copy(fontSize = 13.sp, color = Color.White)
                    )
                    Text(
                        text = "Privacy Policy",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier.clickable {
                            PlatformPermissionsHelper.openUrl(FocalConfig.PRIVACY_POLICY_URL)
                        }
                    )
                }
            }

            // Inline Error Text in Red (#FF453A)
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage!!,
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF453A)
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Pinned Bottom Actions: Create account + Already have account / Log in
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryButton(
                text = "Create account",
                enabled = isFormValid && !isLoading,
                onClick = { submitSignUp() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = typography.caption.copy(
                        fontSize = 14.sp,
                        color = ChildColors.Secondary
                    )
                )
                Text(
                    text = "Log in",
                    style = typography.caption.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(2.dp)
                )
            }
        }
    }
}

// =============================================================================
// SCREEN 5 — LOG IN (PARENT) [PO3_login.png, RIGHT]
// =============================================================================
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToChildPair: () -> Unit,
    onBack: () -> Unit,
    authRepository: AuthRepository = AuthRepository.instance
) {
    val typography = LocalChildTypography.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordSheet by remember { mutableStateOf(false) }

    fun submitLogin() {
        if (email.isBlank() || password.isBlank() || isLoading) return
        errorMessage = null
        isLoading = true
        coroutineScope.launch {
            val result = authRepository.signIn(email, password)
            isLoading = false
            result.fold(
                onSuccess = {
                    onLoginSuccess()
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "Email or password is incorrect"
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Round back button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0B))
                    .border(1.dp, Color(0xFF262626), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome back",
                style = typography.headline.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Log in to manage your family.",
                style = typography.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Continue with Google PrimaryButton
            PrimaryButton(
                text = "Continue with Google",
                leadingIcon = {
                    Icon(
                        imageVector = FocalIcons.Google,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        val res = authRepository.signInWithGoogle()
                        isLoading = false
                        res.fold(
                            onSuccess = { onLoginSuccess() },
                            onFailure = { errorMessage = it.message ?: "Google sign-in failed" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Divider: ───── or with email ─────
            OrDivider(text = "or with email")

            Spacer(modifier = Modifier.height(22.dp))

            // Email address input field
            FocalAuthInputField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                placeholder = "Email address",
                leadingIcon = FocalIcons.Mail,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password input field + Eye Toggle
            FocalAuthInputField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = "Password",
                leadingIcon = FocalIcons.Lock,
                trailingIcon = if (isPasswordVisible) FocalIcons.EyeOff else FocalIcons.Eye,
                onTrailingIconClick = { isPasswordVisible = !isPasswordVisible },
                isPassword = !isPasswordVisible,
                imeAction = ImeAction.Done,
                onDone = { submitLogin() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // "Forgot password?" right-aligned
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showForgotPasswordSheet = true }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Forgot password?",
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }

            // Inline Error Text in Red (#FF453A)
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF453A)
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SecondaryButton "Log in"
            SecondaryButton(
                text = if (isLoading) "Logging in…" else "Log in",
                onClick = { submitLogin() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "Are you a child?" role switcher GlassCard
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        FocalLocalStorage.saveString("KEY_SELECTED_ROLE", "CHILD")
                        onNavigateToChildPair()
                    },
                radius = 24.dp,
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171717)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.User,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Are you a child?",
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Connect to your parent instead",
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            )
                        )
                    }

                    Icon(
                        imageVector = FocalIcons.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF6B6B6B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Pinned Footer: New to FOCAL? Create account
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to FOCAL? ",
                    style = typography.caption.copy(
                        fontSize = 14.sp,
                        color = ChildColors.Secondary
                    )
                )
                Text(
                    text = "Create account",
                    style = typography.caption.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .clickable { onNavigateToSignUp() }
                        .padding(2.dp)
                )
            }
        }
    }

    if (showForgotPasswordSheet) {
        ForgotPasswordBottomSheet(
            onDismiss = { showForgotPasswordSheet = false },
            authRepository = authRepository
        )
    }
}

@Composable
private fun ForgotPasswordBottomSheet(
    onDismiss: () -> Unit,
    authRepository: AuthRepository
) {
    val typography = LocalChildTypography.current
    val coroutineScope = rememberCoroutineScope()
    var resetEmail by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF141414), Color(0xFF0A0A0A))
                    )
                )
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle pill
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF333333))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "RESET PASSWORD",
                    style = typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF6B6B6B)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isSubmitted) "Check your email" else "Reset your password",
                    style = typography.headline.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isSubmitted)
                        "We sent password reset instructions to $resetEmail"
                    else
                        "Enter your email address and we'll send you a link to reset your password.",
                    style = typography.body.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Secondary,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (!isSubmitted) {
                    FocalAuthInputField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            resetError = null
                        },
                        placeholder = "Email address",
                        leadingIcon = FocalIcons.Mail,
                        keyboardType = KeyboardType.Email
                    )

                    if (resetError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resetError!!,
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = Color(0xFFFF453A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    PrimaryButton(
                        text = if (isSending) "Sending…" else "Send reset link",
                        enabled = resetEmail.contains("@") && resetEmail.contains(".") && !isSending,
                        onClick = {
                            isSending = true
                            coroutineScope.launch {
                                val res = authRepository.sendPasswordReset(resetEmail)
                                isSending = false
                                res.fold(
                                    onSuccess = { isSubmitted = true },
                                    onFailure = { resetError = it.message ?: "Failed to send reset link" }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    PrimaryButton(
                        text = "Done",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SecondaryButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =============================================================================
// SCREEN 6 — CHOOSE YOUR FOCUS METHOD [PO4_focus_method.png]
// =============================================================================
enum class FocusMethodType {
    NFC,
    QR
}

@Composable
fun FocusMethodScreen(
    onNavigateToParentHome: (preferredMethod: FocusMethodType) -> Unit,
    onOpenFocusTab: () -> Unit,
    onCloseToParentHome: () -> Unit
) {
    val typography = LocalChildTypography.current
    var selectedMethod by remember { mutableStateOf(FocusMethodType.NFC) }
    var showHaveCardSheet by remember { mutableStateOf(false) }
    var showAddCardSheet by remember { mutableStateOf(false) }

    fun saveChoice(method: FocusMethodType) {
        FocalLocalStorage.saveString("KEY_PREFERRED_FOCUS_METHOD", if (method == FocusMethodType.NFC) "NFC" else "QR")
        FocalLocalStorage.saveBoolean("KEY_FOCUS_METHOD_CHOSEN", true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Round back button closes to Parent Home
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0B))
                    .border(1.dp, Color(0xFF262626), CircleShape)
                    .clickable {
                        saveChoice(selectedMethod)
                        onCloseToParentHome()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.ChevronLeft,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Choose your\nfocus method",
                style = typography.headline.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 40.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "How will your children start and end focus? You can change this later.",
                style = typography.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card 1: NFC card (Selected by default)
            FocusMethodCard(
                title = "NFC card",
                overline = "PHYSICAL CARD",
                subtitle = "Tap the card on your child's phone to start focus, tap again to end.",
                bullets = listOf(
                    Pair(FocalIcons.Bolt, "One tap, no camera needed"),
                    Pair(FocalIcons.Users, "One card for all your children"),
                    Pair(FocalIcons.Lock, "Can't be copied")
                ),
                isSelected = selectedMethod == FocusMethodType.NFC,
                onClick = { selectedMethod = FocusMethodType.NFC },
                artContent = {
                    MiniCardArt()
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Card 2: QR focus
            FocusMethodCard(
                title = "QR focus",
                overline = "NO HARDWARE",
                subtitle = "Print your family QR and keep it at the study desk. Scan to start and end.",
                bullets = listOf(
                    Pair(FocalIcons.Bolt, "Ready right now, free"),
                    Pair(FocalIcons.Share, "Print it or show it on your phone"),
                    Pair(FocalIcons.Refresh, "Reset anytime if it's copied")
                ),
                isSelected = selectedMethod == FocusMethodType.QR,
                onClick = { selectedMethod = FocusMethodType.QR },
                artContent = {
                    MiniQrArt()
                }
            )
        }

        // Pinned Bottom Actions: Skip (1 part) + Next → (1.6 parts)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(
                text = "Skip",
                onClick = {
                    saveChoice(selectedMethod)
                    onNavigateToParentHome(selectedMethod)
                },
                modifier = Modifier.weight(1f)
            )

            PrimaryButton(
                text = "Next →",
                onClick = {
                    saveChoice(selectedMethod)
                    if (selectedMethod == FocusMethodType.NFC) {
                        showHaveCardSheet = true
                    } else {
                        onOpenFocusTab()
                    }
                },
                modifier = Modifier.weight(1.6f)
            )
        }
    }

    // Bottom Sheet: "Do you have a FOCAL card?"
    if (showHaveCardSheet) {
        HaveFocalCardSheet(
            onAddNow = {
                showHaveCardSheet = false
                showAddCardSheet = true
            },
            onOrderCard = {
                showHaveCardSheet = false
                PlatformPermissionsHelper.openUrl(FocalConfig.CARD_ORDER_URL)
                onNavigateToParentHome(FocusMethodType.NFC)
            },
            onDismiss = { showHaveCardSheet = false }
        )
    }

    if (showAddCardSheet) {
        AddCardSheet(
            onDismiss = {
                showAddCardSheet = false
                onNavigateToParentHome(FocusMethodType.NFC)
            },
            onCardAdded = { _ ->
                showAddCardSheet = false
                onNavigateToParentHome(FocusMethodType.NFC)
            }
        )
    }
}

@Composable
private fun FocusMethodCard(
    title: String,
    overline: String,
    subtitle: String,
    bullets: List<Pair<ImageVector, String>>,
    isSelected: Boolean,
    onClick: () -> Unit,
    artContent: @Composable () -> Unit
) {
    val typography = LocalChildTypography.current
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF131313), Color(0xFF090909))
                )
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color.White else Color(0xFF262626),
                shape = cardShape
            )
            .drawBehind {
                if (isSelected) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                            center = center,
                            radius = size.width / 1.1f
                        ),
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Content: Radio, Overline, Title, Subtitle, Bullets
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Radio button
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.dp else 1.5.dp,
                                color = if (isSelected) Color.White else Color(0xFF3A3A3A),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = overline,
                            style = typography.caption.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp,
                                color = Color(0xFF6B6B6B)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = subtitle,
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Secondary,
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Bullets
                bullets.forEach { (bulletIcon, text) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = bulletIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = text,
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = Color(0xFFA3A3A3)
                            )
                        )
                    }
                }
            }

            // Right Art
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                artContent()
            }
        }
    }
}

@Composable
private fun MiniCardArt() {
    Box(
        modifier = Modifier
            .width(115.dp)
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floor ellipse rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * 0.72f)
            drawOval(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = Offset(c.x - 48.dp.toPx(), c.y - 14.dp.toPx()),
                size = Size(96.dp.toPx(), 28.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawOval(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(c.x - 36.dp.toPx(), c.y - 10.dp.toPx()),
                size = Size(72.dp.toPx(), 20.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Tilted Mini Card
        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .graphicsLayer { rotationZ = -12f }
                .width(82.dp)
                .height(52.dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF161616), Color(0xFF040404))
                    )
                )
                .border(1.5.dp, Color.White, shape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.focal_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MiniQrArt() {
    Box(
        modifier = Modifier
            .width(115.dp)
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floor ellipse ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * 0.74f)
            drawOval(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = Offset(c.x - 40.dp.toPx(), c.y - 12.dp.toPx()),
                size = Size(80.dp.toPx(), 24.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Rounded QR Tile
        val shape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .size(66.dp)
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                            center = center,
                            radius = size.width / 1.3f
                        ),
                        cornerRadius = CornerRadius(18.dp.toPx())
                    )
                }
                .clip(shape)
                .background(Color.Black)
                .border(1.5.dp, Color.White, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FocalIcons.QrCode,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun HaveFocalCardSheet(
    onAddNow: () -> Unit,
    onOrderCard: () -> Unit,
    onDismiss: () -> Unit
) {
    val typography = LocalChildTypography.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF141414), Color(0xFF0A0A0A))
                    )
                )
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF333333))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "FOCAL CARD",
                    style = typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF6B6B6B)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Do you have a FOCAL card?",
                    style = typography.headline.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Every card works on all your children's phones.",
                    style = typography.body.copy(
                        fontSize = 14.sp,
                        color = ChildColors.Secondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                PrimaryButton(
                    text = "Yes, add it now",
                    onClick = onAddNow,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                SecondaryButton(
                    text = "Not yet, get one",
                    onClick = onOrderCard,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =============================================================================
// REUSABLE COMPONENTS FOR AUTH FORMS
// =============================================================================
@Composable
fun FocalAuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null
) {
    val typography = LocalChildTypography.current
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF141414), Color(0xFF0B0B0B))
                )
            )
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = typography.body.copy(
                            fontSize = 15.sp,
                            color = Color(0xFF6B6B6B)
                        )
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = typography.body.copy(
                        fontSize = 15.sp,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(Color.White),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                        onDone = {
                            focusManager.clearFocus()
                            onDone?.invoke()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTrailingIconClick?.invoke() }
                )
            }
        }
    }
}

@Composable
private fun OrDivider(text: String) {
    val typography = LocalChildTypography.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF222222))
        )
        Text(
            text = text,
            style = typography.caption.copy(
                fontSize = 12.sp,
                color = Color(0xFF6B6B6B)
            ),
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF222222))
        )
    }
}
