package com.focal.app.ui.child.pairing

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.model.currentTimeMillis
import com.focal.app.pairing.DefaultPairingRepository
import com.focal.app.pairing.PairRequest
import com.focal.app.pairing.PairState
import com.focal.app.pairing.PairingRepository
import com.focal.app.ui.child.InfoCard
import com.focal.app.ui.child.InfoCardRow
import com.focal.app.ui.child.components.*
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.components.PlatformCameraScanner
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// =============================================================================
// SCREEN 1 — CONNECT TO YOUR PARENT (PairScreen)
// Reference: p5a_1_pair.png
// =============================================================================

@Composable
fun PairScreen(
    onBack: () -> Unit,
    onScanQrClick: () -> Unit,
    onPairSuccess: (PairRequest) -> Unit,
    initialError: String? = null,
    repository: PairingRepository = PairingRepository.instance
) {
    val typography = LocalChildTypography.current
    val coroutineScope = rememberCoroutineScope()

    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf(initialError) }
    var isLoading by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        coroutineScope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-12f) at 60
                    12f at 120
                    (-8f) at 180
                    8f at 240
                    (-4f) at 300
                    0f at 400
                }
            )
        }
    }

    fun submitCode() {
        if (codeInput.length < 6 || isLoading) return
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = repository.redeemCode(codeInput)
            isLoading = false
            result.onSuccess { request ->
                onPairSuccess(request)
            }.onFailure { ex ->
                errorMessage = ex.message ?: "Code expired or not found. Check parent's screen."
                triggerShake()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildColors.Background)
    ) {
        // Top-right background decorative phone illustration (Compose Canvas)
        PairPhonesIllustration(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
                .size(160.dp, 130.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 44dp round back button with 48dp touch area
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ChildColors.Surface)
                        .border(1.dp, ChildColors.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.ChevronLeft,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title & Subtitle
            Text(
                text = "Connect to your parent",
                style = typography.display.copy(
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Ask your parent to open FOCAL on their phone, tap Add Child, and show you their QR or code.",
                style = typography.body.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Button: "Scan parent's QR"
            PrimaryButton(
                text = "Scan parent's QR",
                onClick = onScanQrClick,
                leadingIcon = {
                    Icon(
                        imageVector = FocalIcons.QrCode,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Inset divider: "────── or enter code ──────"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(ChildColors.ListDivider)
                )
                Text(
                    text = "  or enter code  ",
                    style = typography.caption.copy(
                        color = ChildColors.Tertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(ChildColors.ListDivider)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Code Entry GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 20.dp,
                radius = 24.dp
            ) {
                // Invisible BasicTextField capturing input / paste
                BasicTextField(
                    value = codeInput,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() }.take(6)
                        codeInput = filtered
                        if (errorMessage != null) {
                            errorMessage = null
                        }
                        if (filtered.length == 6) {
                            submitCode()
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitCode() }
                    ),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .size(1.dp)
                )

                // 6 Digit Boxes Row with shake animation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = shakeOffset.value.dp)
                        .clickable { focusRequester.requestFocus() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val digit = codeInput.getOrNull(i)?.toString().orEmpty()
                        val isFocused = codeInput.length == i || (i == 5 && codeInput.length == 6)
                        val boxBorderColor = if (isFocused) Color.White else ChildColors.Border
                        val boxBorderWidth = if (isFocused) 1.5.dp else 1.dp

                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ChildColors.IconCircleBg)
                                .border(boxBorderWidth, boxBorderColor, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                style = typography.title.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Caption or Error message below boxes
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "Code expired or not found. Check parent's screen.",
                        style = typography.caption.copy(
                            color = ChildColors.Danger, // #FF453A for real errors
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "6-digit code from parent's screen",
                        style = typography.caption.copy(
                            color = ChildColors.Tertiary,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SecondaryButton "Connect"
                val isButtonEnabled = codeInput.length == 6 && !isLoading
                SecondaryButton(
                    text = if (isLoading) "Connecting…" else "Connect",
                    onClick = { submitCode() },
                    enabled = isButtonEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Info row at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = FocalIcons.InfoCircle,
                    contentDescription = null,
                    tint = ChildColors.Tertiary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your parent controls schedules, blocked apps, and unlocks.",
                    style = typography.caption.copy(
                        color = ChildColors.Tertiary,
                        fontSize = 12.sp
                    ),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

/**
 * Top-right decoration: thin glowing circle + two tilted phone cards
 * ("Parent" with Users icon + "QR · CODE" chip, "You" with User icon)
 * and a dotted curved line between them. Pure Compose drawing.
 */
@Composable
private fun PairPhonesIllustration(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle glowing circle in background
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = 52.dp.toPx(),
                center = Offset(w * 0.65f, h * 0.45f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = 52.dp.toPx(),
                center = Offset(w * 0.65f, h * 0.45f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Dotted curved connection line between the two phone centers
            val p1 = Offset(w * 0.38f, h * 0.52f)
            val p2 = Offset(w * 0.82f, h * 0.62f)
            val control = Offset(w * 0.60f, h * 0.32f)

            val path = Path().apply {
                moveTo(p1.x, p1.y)
                quadraticTo(control.x, control.y, p2.x, p2.y)
            }

            drawPath(
                path = path,
                color = ChildColors.Tertiary.copy(alpha = 0.8f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            )
        }

        // Left phone card: "Parent", tilted -9°
        Box(
            modifier = Modifier
                .offset(x = 12.dp, y = 14.dp)
                .size(width = 72.dp, height = 96.dp)
                .graphicsLayer {
                    rotationZ = -9f
                }
                .clip(RoundedCornerShape(12.dp))
                .background(ChildColors.BottomSheetBgStart)
                .border(1.dp, ChildColors.BorderStrong, RoundedCornerShape(12.dp))
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Users,
                        contentDescription = null,
                        tint = ChildColors.Secondary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Parent",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChildColors.Secondary
                    )
                }

                // QR preview mini box
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ChildColors.BannerBg)
                        .border(1.dp, ChildColors.Border, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.QrCode,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Chip: "QR · CODE"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ChildColors.NavSelected)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "QR · CODE",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Right phone card: "You", tilted 14°
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-4).dp, y = (-2).dp)
                .size(width = 62.dp, height = 82.dp)
                .graphicsLayer {
                    rotationZ = 14f
                }
                .clip(RoundedCornerShape(10.dp))
                .background(ChildColors.BottomSheetBgStart)
                .border(1.dp, ChildColors.BorderStrong, RoundedCornerShape(10.dp))
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.User,
                        contentDescription = null,
                        tint = ChildColors.Secondary,
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "You",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChildColors.Secondary
                    )
                }

                // Child phone center emblem
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ChildColors.NavSelected),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "YOU",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(ChildColors.Border)
                )
            }
        }
    }
}


// =============================================================================
// SCREEN 2 — SCAN PARENT'S QR (ScanParentQrScreen)
// Reference: p5a_2_scan_qr.png
// =============================================================================

@Composable
fun ScanParentQrScreen(
    onBack: () -> Unit,
    onEnterCodeClick: () -> Unit,
    onQrRedeemed: (PairRequest) -> Unit,
    repository: PairingRepository = PairingRepository.instance
) {
    val typography = LocalChildTypography.current
    val coroutineScope = rememberCoroutineScope()

    var torchEnabled by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf<ScanStatus>(ScanStatus.Searching) }
    val shakeOffset = remember { Animatable(0f) }

    // Laser sweep animation: moves vertically between 0f and 1f
    val infiniteTransition = rememberInfiniteTransition()
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    fun handleQrDetected(payload: String) {
        if (scanStatus is ScanStatus.Success) return
        coroutineScope.launch {
            val result = repository.redeemQr(payload)
            result.onSuccess { request ->
                scanStatus = ScanStatus.Success
                delay(300)
                onQrRedeemed(request)
            }.onFailure { ex ->
                scanStatus = ScanStatus.Invalid(ex.message ?: "That's not a pairing QR")
                shakeOffset.snapTo(0f)
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 350
                        0f at 0
                        (-10f) at 50
                        10f at 100
                        (-6f) at 150
                        6f at 200
                        0f at 350
                    }
                )
                delay(1800)
                if (scanStatus is ScanStatus.Invalid) {
                    scanStatus = ScanStatus.Searching
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Viewport
        PlatformCameraScanner(
            modifier = Modifier.fillMaxSize(),
            torchEnabled = torchEnabled,
            onQrDetected = { qrPayload ->
                handleQrDetected(qrPayload)
            }
        )

        // Semi-transparent dimming scrim outside the 260dp cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cutoutSizePx = 260.dp.toPx()
            val cornerRadiusPx = 28.dp.toPx()
            val left = (size.width - cutoutSizePx) / 2f
            val top = (size.height - cutoutSizePx) / 2.3f

            // Outside dim layer (55% black)
            drawRect(
                color = Color.Black.copy(alpha = 0.55f)
            )

            // Clear cutout window
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(cutoutSizePx, cutoutSizePx),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                blendMode = BlendMode.Clear
            )

            // 4 Corner Brackets (white 4dp stroke, 24dp arm length, 12dp corner radius)
            val armLen = 24.dp.toPx()
            val bracketStroke = 4.dp.toPx()
            val cr = 14.dp.toPx()
            val bracketColor = Color.White

            // Top-Left bracket
            val pathTL = Path().apply {
                moveTo(left, top + armLen)
                lineTo(left, top + cr)
                quadraticTo(left, top, left + cr, top)
                lineTo(left + armLen, top)
            }
            drawPath(pathTL, color = bracketColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

            // Top-Right bracket
            val right = left + cutoutSizePx
            val pathTR = Path().apply {
                moveTo(right - armLen, top)
                lineTo(right - cr, top)
                quadraticTo(right, top, right, top + cr)
                lineTo(right, top + armLen)
            }
            drawPath(pathTR, color = bracketColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

            // Bottom-Left bracket
            val bottom = top + cutoutSizePx
            val pathBL = Path().apply {
                moveTo(left, bottom - armLen)
                lineTo(left, bottom - cr)
                quadraticTo(left, bottom, left + cr, bottom)
                lineTo(left + armLen, bottom)
            }
            drawPath(pathBL, color = bracketColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

            // Bottom-Right bracket
            val pathBR = Path().apply {
                moveTo(right - armLen, bottom)
                lineTo(right - cr, bottom)
                quadraticTo(right, bottom, right, bottom - cr)
                lineTo(right, bottom - armLen)
            }
            drawPath(pathBR, color = bracketColor, style = Stroke(width = bracketStroke, cap = StrokeCap.Round))

            // Animated Laser line inside cutout
            val laserY = top + (cutoutSizePx * laserProgress)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.85f),
                        Color.White,
                        Color.White.copy(alpha = 0.85f),
                        Color.Transparent
                    )
                ),
                start = Offset(left + 16.dp.toPx(), laserY),
                end = Offset(right - 16.dp.toPx(), laserY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Top Scrim & Header Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Round back button with 48dp touch area
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ChildColors.Surface)
                        .border(1.dp, ChildColors.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Scan parent's QR",
                style = typography.title.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Point your camera at the QR code on your parent's phone",
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Status Pill below the 260dp cutout
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 166.dp)
                .offset(x = shakeOffset.value.dp)
        ) {
            when (scanStatus) {
                is ScanStatus.Searching -> {
                    StatusPill(
                        text = "Looking for a FOCAL QR…",
                        type = StatusPillType.UPCOMING
                    )
                }
                is ScanStatus.Invalid -> {
                    StatusPill(
                        text = (scanStatus as ScanStatus.Invalid).message,
                        type = StatusPillType.OFF
                    )
                }
                is ScanStatus.Success -> {
                    StatusPill(
                        text = "✓ Found",
                        type = StatusPillType.ACTIVE
                    )
                }
            }
        }

        // Bottom Actions: "Torch" and "Enter code instead"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryButton(
                text = if (torchEnabled) "Torch On" else "Torch",
                onClick = { torchEnabled = !torchEnabled },
                leadingIcon = {
                    Icon(
                        imageVector = FocalIcons.Flashlight,
                        contentDescription = null,
                        tint = if (torchEnabled) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )

            SecondaryButton(
                text = "Enter code",
                onClick = onEnterCodeClick,
                leadingIcon = {
                    Icon(
                        imageVector = FocalIcons.Code,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private sealed class ScanStatus {
    object Searching : ScanStatus()
    data class Invalid(val message: String = "That's not a pairing QR") : ScanStatus()
    object Success : ScanStatus()
}


// =============================================================================
// SCREEN 3 — WAITING FOR APPROVAL (WaitingApprovalScreen)
// Reference: p5a_3_waiting.png
// =============================================================================

@Composable
fun WaitingApprovalScreen(
    request: PairRequest,
    onCancel: () -> Unit,
    onApproved: (PairRequest) -> Unit,
    onDeclinedOrExpired: (String) -> Unit,
    repository: PairingRepository = PairingRepository.instance
) {
    val typography = LocalChildTypography.current
    val coroutineScope = rememberCoroutineScope()

    // Keep screen on while waiting for approval
    DisposableEffect(Unit) {
        PlatformPermissionsHelper.setKeepScreenOn(true)
        onDispose {
            PlatformPermissionsHelper.setKeepScreenOn(false)
        }
    }

    // Live countdown timer
    var remainingSeconds by remember {
        mutableStateOf(
            ((request.expiresAtMs - currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        )
    }

    LaunchedEffect(request.expiresAtMs) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds = ((request.expiresAtMs - currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        }
        if (remainingSeconds <= 0) {
            repository.cancelRequest(request.id)
            onDeclinedOrExpired("The request expired. Try connecting again.")
        }
    }

    // Observe approval from parent
    LaunchedEffect(request.id) {
        repository.observeRequest(request.id).collectLatest { state ->
            when (state) {
                PairState.APPROVED -> {
                    repository.markPaired(request)
                    onApproved(request)
                }
                PairState.DECLINED -> {
                    repository.cancelRequest(request.id)
                    onDeclinedOrExpired("Your parent declined the request.")
                }
                PairState.EXPIRED -> {
                    repository.cancelRequest(request.id)
                    onDeclinedOrExpired("The request expired. Try connecting again.")
                }
                PairState.PENDING -> {
                    // Waiting
                }
            }
        }
    }

    fun handleCancel() {
        coroutineScope.launch {
            repository.cancelRequest(request.id)
            onCancel()
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val countdownText = "$minutes:${seconds.toString().padStart(2, '0')}"

    val manufacturer = remember { PlatformPermissionsHelper.getDeviceManufacturer() }
    val androidVer = remember { PlatformPermissionsHelper.getAndroidVersion() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildColors.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 44dp round close button top-left with 48dp touch area
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { handleCancel() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ChildColors.Surface)
                    .border(1.dp, ChildColors.Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.Close,
                    contentDescription = "Cancel",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Rotating Arc & Avatar Hero
        WaitingApprovalHero(
            initials = request.parentInitials,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Overline "WAITING FOR APPROVAL"
        Text(
            text = "WAITING FOR APPROVAL",
            style = typography.overline.copy(
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                color = ChildColors.Tertiary,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Title: "Ask Priya to tap Allow on her phone"
        Text(
            text = "Ask ${request.parentName.split(" ").firstOrNull() ?: "Priya"} to tap Allow on her phone",
            style = typography.title.copy(
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle
        Text(
            text = "A request was sent to ${request.parentName.split(" ").firstOrNull() ?: "Priya"}'s phone. This screen will update automatically.",
            style = typography.caption.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = ChildColors.Secondary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        // InfoCard with 3 rows
        InfoCard(
            rows = listOf(
                InfoCardRow(
                    label = "Parent's device",
                    value = "Parent's phone"
                ),
                InfoCardRow(
                    label = "This phone",
                    value = "$manufacturer · Android $androidVer"
                ),
                InfoCardRow(
                    label = "Request expires in",
                    value = countdownText
                )
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        // SecondaryButton "Cancel request"
        SecondaryButton(
            text = "Cancel request",
            onClick = { handleCancel() },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

/**
 * 84dp parent initials avatar inside two concentric glowing outline rings,
 * with a subtle rotating white arc (2.4s loop, 1.5dp stroke, 90° sweep).
 */
@Composable
private fun WaitingApprovalHero(
    initials: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(132.dp)) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)

            // Outer ring (132dp)
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = 66.dp.toPx(),
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            // Inner ring (108dp)
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = 54.dp.toPx(),
                center = centerOffset,
                style = Stroke(width = 1.dp.toPx())
            )

            // Rotating white arc (90° sweep, 1.5dp stroke)
            rotate(rotationAngle, pivot = centerOffset) {
                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - 54.dp.toPx(), centerOffset.y - 54.dp.toPx()),
                    size = Size(108.dp.toPx(), 108.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Center 84dp parent initials avatar
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(ChildColors.IconCircleBg)
                .border(1.5.dp, ChildColors.BorderStrong, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifBlank { "PS" },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}


// =============================================================================
// SCREEN 4 — CONNECTED! (PairedSuccessScreen)
// Reference: p5a_4_connected.png
// =============================================================================

@Composable
fun PairedSuccessScreen(
    request: PairRequest,
    onSetupProtection: () -> Unit
) {
    val typography = LocalChildTypography.current

    // Animated connecting line and pop-in scale for checkmark
    val checkmarkScale = remember { Animatable(0.8f) }
    val lineProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            checkmarkScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            lineProgress.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            )
        }
    }

    val parentFirstName = request.parentName.split(" ").firstOrNull() ?: "Priya"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChildColors.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(44.dp))

        // Avatar connection illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft ambient radial glow behind connection
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = 70.dp.toPx(),
                    center = centerOffset
                )

                // Animated horizontal line extending from center to sides
                val halfW = 80.dp.toPx() * lineProgress.value
                drawLine(
                    color = ChildColors.BorderStrong,
                    start = Offset(centerOffset.x - halfW, centerOffset.y),
                    end = Offset(centerOffset.x + halfW, centerOffset.y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Child avatar ("AR", 64dp)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ChildColors.IconCircleBg)
                        .border(1.dp, ChildColors.BorderStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.childInitials.ifBlank { "AR" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Center 48dp checkmark circle (pops in 0.8 -> 1.0)
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = checkmarkScale.value
                            scaleY = checkmarkScale.value
                        }
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Check,
                        contentDescription = "Connected",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Parent avatar ("PS", 64dp)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ChildColors.IconCircleBg)
                        .border(1.dp, ChildColors.BorderStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = request.parentInitials.ifBlank { "PS" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Status Overline: "CONNECTED"
        Text(
            text = "CONNECTED",
            style = typography.overline.copy(
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                color = ChildColors.Tertiary,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Title: "You're linked to Priya"
        Text(
            text = "You're linked to $parentFirstName",
            style = typography.display.copy(
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Your parent has set up your phone. Protection is ready to turn on.",
            style = typography.caption.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = ChildColors.Secondary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Capabilities GlassCard
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            radius = 24.dp,
            padding = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Item 1: Schedules synced
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
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
                            imageVector = FocalIcons.Calendar,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Schedules synced",
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "From your parent",
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 1dp Indented Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 78.dp)
                        .height(1.dp)
                        .background(ChildColors.ListDivider)
                )

                // Item 2: FOCAL card ready
                val cardId = request.pairedCardId ?: "FC0001"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
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
                            imageVector = FocalIcons.Card,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FOCAL card ready",
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your family card works on this phone",
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        // PrimaryButton "Set up protection"
        PrimaryButton(
            text = "Set up protection",
            onClick = onSetupProtection,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}
