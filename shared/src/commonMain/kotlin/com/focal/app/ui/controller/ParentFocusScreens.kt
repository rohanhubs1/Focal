package com.focal.app.ui.controller

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.data.FamilyFocusQr
import com.focal.app.data.FocusQrRepository
import com.focal.app.data.FocalRepository
import com.focal.app.model.ChildProfile
import com.focal.app.model.currentTimeMillis
import com.focal.app.pairing.PairingRepository
import com.focal.app.pairing.ParentPairState
import com.focal.app.pairing.ParentPairingSession
import com.focal.app.ui.child.SectionOverline
import com.focal.app.ui.child.components.GlassCard
import com.focal.app.ui.child.components.GlowArcs
import com.focal.app.ui.child.components.PrimaryButton
import com.focal.app.ui.child.components.SecondaryButton
import com.focal.app.ui.child.components.StatusPill
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography
import kotlinx.coroutines.delay

// =========================================================================
// FRAMED QR CODE COMPONENT (Shared by Focus Tab & Connect Phone)
// =========================================================================

/**
 * Renders the white rounded QR code tile framed by 4 white corner brackets (3dp stroke, radius 18)
 * and an ambient soft radial glow.
 */
@Composable
fun FocalQrCodeCard(
    payload: String,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    tilePadding: Dp = 14.dp,
    tileCornerRadius: Dp = 18.dp,
    bracketCornerRadius: Dp = 18.dp,
    bracketArmLength: Dp = 24.dp,
    bracketStrokeWidth: Dp = 3.dp,
    contentSize: Dp = 220.dp
) {
    val totalSize = contentSize + 32.dp

    Box(
        modifier = modifier.size(totalSize),
        contentAlignment = Alignment.Center
    ) {
        // Soft ambient white radial glow behind the tile
        Box(
            modifier = Modifier
                .size(contentSize + 48.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDimmed) 0.04f else 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 4 corner brackets canvas framing the tile
        Canvas(modifier = Modifier.size(totalSize)) {
            val strokePx = bracketStrokeWidth.toPx()
            val rPx = bracketCornerRadius.toPx()
            val armPx = bracketArmLength.toPx()
            val bracketColor = if (isDimmed) Color.White.copy(alpha = 0.35f) else Color.White
            val w = size.width
            val h = size.height

            // Top-Left corner bracket
            val tlPath = Path().apply {
                moveTo(armPx, strokePx / 2f)
                arcTo(
                    rect = Rect(strokePx / 2f, strokePx / 2f, strokePx / 2f + 2f * rPx, strokePx / 2f + 2f * rPx),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(strokePx / 2f, armPx)
            }
            drawPath(tlPath, bracketColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))

            // Top-Right corner bracket
            val trPath = Path().apply {
                moveTo(w - armPx, strokePx / 2f)
                arcTo(
                    rect = Rect(w - strokePx / 2f - 2f * rPx, strokePx / 2f, w - strokePx / 2f, strokePx / 2f + 2f * rPx),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(w - strokePx / 2f, armPx)
            }
            drawPath(trPath, bracketColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))

            // Bottom-Left corner bracket
            val blPath = Path().apply {
                moveTo(strokePx / 2f, h - armPx)
                arcTo(
                    rect = Rect(strokePx / 2f, h - strokePx / 2f - 2f * rPx, strokePx / 2f + 2f * rPx, h - strokePx / 2f),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(armPx, h - strokePx / 2f)
            }
            drawPath(blPath, bracketColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))

            // Bottom-Right corner bracket
            val brPath = Path().apply {
                moveTo(w - strokePx / 2f, h - armPx)
                arcTo(
                    rect = Rect(w - strokePx / 2f - 2f * rPx, h - strokePx / 2f - 2f * rPx, w - strokePx / 2f, h - strokePx / 2f),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(w - armPx, h - strokePx / 2f)
            }
            drawPath(brPath, bracketColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
        }

        val qrBitmap = remember(payload) {
            com.focal.app.ui.components.PlatformQrGenerator.generateQrBitmap(payload, 512)
        }

        // Centered white rounded tile containing QR code
        Box(
            modifier = Modifier
                .size(contentSize)
                .clip(RoundedCornerShape(tileCornerRadius))
                .background(if (isDimmed) Color.White.copy(alpha = 0.35f) else Color.White)
                .padding(tilePadding),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = qrBitmap,
                    contentDescription = payload,
                    modifier = Modifier.fillMaxSize(),
                    alpha = if (isDimmed) 0.35f else 1f
                )
            } else {
                Icon(
                    imageVector = FocalIcons.QrCode,
                    contentDescription = payload,
                    tint = if (isDimmed) Color.Black.copy(alpha = 0.35f) else Color.Black,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// =========================================================================
// SCREEN 1 — FOCUS TAB (Matching PD1_focus_tab.png)
// =========================================================================

/**
 * Tab 2: Focus Tab Root Screen (Centre raised QR button on bottom bar)
 * - GlowArcs top-right
 * - Title "Start focus" (34sp bold) + subtitle
 * - "Start now" GlassCard: overlapping initials avatars · "Start now" · PrimaryButton "Start focus now"
 * - "Family focus QR" GlassCard: title · subtitle · framed QR · "Share / print" · "Reset QR"
 * - Keeps screen on + max brightness while visible
 */
@Composable
fun ParentFocusTabScreen(
    onAddChildClick: () -> Unit = {},
    repository: FocalRepository = FocalRepository.instance,
    focusQrRepository: FocusQrRepository = FocusQrRepository.instance
) {
    val typography = LocalChildTypography.current
    var showStartFocusSheet by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    val familyQr by focusQrRepository.familyQr.collectAsState()

    // Max brightness & Keep screen on while tab is visible
    DisposableEffect(Unit) {
        PlatformPermissionsHelper.setMaxBrightness(true)
        PlatformPermissionsHelper.setKeepScreenOn(true)
        onDispose {
            PlatformPermissionsHelper.setMaxBrightness(false)
            PlatformPermissionsHelper.setKeepScreenOn(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GlowArcs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(bottom = 110.dp) // Clears floating ParentBottomNav
        ) {
            // Header: Title + Subtitle
            Text(
                text = "Start focus",
                style = typography.display.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Start it on a child's phone now, or let them scan your family QR.",
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(24.dp))

            if (repository.childrenList.isEmpty()) {
                // Empty state: No children yet
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add a child first",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Add your child to start focus sessions and pair their phone.",
                            style = typography.caption.copy(
                                fontSize = 13.5.sp,
                                color = ChildColors.Secondary
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(
                            text = "Add child",
                            onClick = onAddChildClick,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            } else {
                // GlassCard 1: "Start now"
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 20.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Overlapping initials avatars of linked children (max 3, then "+n")
                            val linkedChildren = repository.childrenList
                            val displayChildren = linkedChildren.take(3)
                            val extraCount = linkedChildren.size - 3

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                displayChildren.forEachIndexed { index, ch ->
                                    val initials = remember(ch.name) {
                                        com.focal.app.ui.child.components.getAvatarInitials(ch.name)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .offset(x = if (index > 0) (-10 * index).dp else 0.dp)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF171717))
                                            .border(1.5.dp, Color(0xFF2A2A2A), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            style = typography.caption.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }

                                if (extraCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = (-10 * displayChildren.size).dp)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E1E1E))
                                            .border(1.5.dp, Color(0xFF2A2A2A), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+$extraCount",
                                            style = typography.caption.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(if (displayChildren.size > 1) 4.dp else 14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start now",
                                    style = typography.headline.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Child, policy and time",
                                    style = typography.caption.copy(
                                        fontSize = 13.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    maxLines = 2
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // PrimaryButton (bolt icon) "Start focus now"
                        PrimaryButton(
                            text = "Start focus now",
                            leadingIcon = {
                                Icon(
                                    imageVector = FocalIcons.Lightning,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = { showStartFocusSheet = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // GlassCard 2: "Family focus QR"
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 22.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Family focus QR",
                            style = typography.headline.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Your child scans this in FOCAL to start or end focus.",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(22.dp))

                        // Framed QR code
                        FocalQrCodeCard(
                            payload = familyQr.qrPayload,
                            contentSize = 220.dp
                        )

                        Spacer(Modifier.height(22.dp))

                        // Two SecondaryButtons side-by-side: "Share" | "Reset"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SecondaryButton(
                                text = "Share",
                                leadingIcon = {
                                    Icon(
                                        imageVector = FocalIcons.Share,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    PlatformPermissionsHelper.shareQrCode(
                                        title = "FOCAL Family Focus QR",
                                        payload = familyQr.qrPayload
                                    )
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            )

                            SecondaryButton(
                                text = "Reset",
                                leadingIcon = {
                                    Icon(
                                        imageVector = FocalIcons.Refresh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = { showResetConfirmDialog = true },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Reset makes old printed or saved copies stop working.",
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Start Focus Sheet (from Step P2)
        if (showStartFocusSheet) {
            StartFocusSheet(
                preselectedChildId = null,
                onDismiss = { showStartFocusSheet = false },
                repository = repository
            )
        }

        // Reset QR Confirm Dialog
        if (showResetConfirmDialog) {
            Dialog(onDismissRequest = { showResetConfirmDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Reset Family QR?",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "If your QR was shared with someone else, reset it. The old QR stops working right away. Any printed or saved copies will no longer start or end focus.",
                            style = typography.body.copy(
                                fontSize = 13.5.sp,
                                color = ChildColors.Secondary
                            )
                        )

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SecondaryButton(
                                text = "Cancel",
                                onClick = { showResetConfirmDialog = false },
                                modifier = Modifier.weight(1f).height(46.dp)
                            )

                            PrimaryButton(
                                text = "Reset",
                                onClick = {
                                    focusQrRepository.resetFamilyQr(repository.currentUser.id.ifBlank { "family_1" })
                                    PlatformPermissionsHelper.showToast("Family QR has been reset.")
                                    showResetConfirmDialog = false
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 46.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 2 — ADD CHILD · STEP 1 OF 2 (DETAILS) (Matching PE1_add_child.png)
// =========================================================================

/**
 * Add Child Step 1: Child profile details (Name, Class/grade)
 * Full-screen page (DetailScaffold style, bottom bar hidden).
 * Centred 88dp round avatar preview with initials that update live.
 */
@Composable
fun AddChildStep1Screen(
    onBack: () -> Unit,
    onContinue: (name: String, grade: String) -> Unit,
    existingChildren: List<ChildProfile> = FocalRepository.instance.childrenList
) {
    val typography = LocalChildTypography.current
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }

    // Live initials computation
    val initials = remember(name) {
        if (name.isBlank()) "?" else com.focal.app.ui.child.components.getAvatarInitials(name)
    }

    // Check duplicate name
    val isDuplicate = remember(name, existingChildren) {
        val trimmed = name.trim()
        trimmed.isNotBlank() && existingChildren.any { it.name.trim().equals(trimmed, ignoreCase = true) }
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
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 24.dp)
        ) {
            // Top row: 44dp circular back button
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
                    imageVector = FocalIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            // Overline: ADD CHILD · 1 OF 2
            Text(
                text = "ADD CHILD · 1 OF 2",
                style = typography.overline.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF6B6B6B)
                )
            )

            Spacer(Modifier.height(4.dp))

            // Title: Who is this for?
            Text(
                text = "Who is this for?",
                style = typography.display.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Your child won't need an email or password.",
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(32.dp))

            // Centred 88dp live initials preview avatar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF171717))
                    .border(1.5.dp, Color(0xFF2A2A2A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = typography.display.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(Modifier.height(32.dp))

            // Section: NAME
            SectionOverline(text = "NAME")

            Spacer(Modifier.height(6.dp))

            // Name Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (name.isEmpty()) {
                    Text(
                        text = "Child's name",
                        style = typography.body.copy(
                            fontSize = 16.sp,
                            color = Color(0xFF6B6B6B)
                        )
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = { input ->
                        if (input.length <= 30) {
                            name = input
                        }
                    },
                    singleLine = true,
                    textStyle = typography.body.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isDuplicate) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "You already have an ${name.trim()}",
                    style = typography.caption.copy(
                        fontSize = 12.5.sp,
                        color = Color(0xFFA3A3A3)
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            // Section: CLASS OR GRADE (OPTIONAL)
            SectionOverline(text = "CLASS OR GRADE (OPTIONAL)")

            Spacer(Modifier.height(6.dp))

            // Class/Grade Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (grade.isEmpty()) {
                    Text(
                        text = "Class or grade (optional), e.g. Class 10",
                        style = typography.body.copy(
                            fontSize = 16.sp,
                            color = Color(0xFF6B6B6B)
                        )
                    )
                }
                BasicTextField(
                    value = grade,
                    onValueChange = { input ->
                        if (input.length <= 20) {
                            grade = input
                        }
                    },
                    singleLine = true,
                    textStyle = typography.body.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "You can change these anytime in their profile.",
                style = typography.caption.copy(
                    fontSize = 12.5.sp,
                    color = Color(0xFF6B6B6B)
                )
            )

            Spacer(Modifier.weight(1f))

            // Bottom-pinned PrimaryButton "Continue"
            PrimaryButton(
                text = "Continue",
                onClick = {
                    if (name.isNotBlank()) {
                        onContinue(name.trim(), grade.trim())
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            )
        }
    }
}

// =========================================================================
// SCREEN 3 — ADD CHILD · STEP 2 OF 2 (CONNECT PHONE) (Matching PE2_connect_phone.png)
// =========================================================================

/**
 * Add Child Step 2: Connect Phone Screen
 * - Full-screen page (DetailScaffold, bottom bar hidden)
 * - Overline "ADD CHILD · 2 OF 2" + title "Connect {name}'s phone"
 * - GlassCard: QR block (focal://pair?...) + "or code" divider + 6 digit boxes
 * - Countdown "Expires in {m:ss} · works once" + "New code"
 * - StatusPill: "● Waiting for {name}'s phone…"
 * - Auto-transitions to AllowPhoneSheet upon child code/QR redemption
 */
@Composable
fun ConnectPhoneScreen(
    childId: String,
    childName: String,
    onBack: () -> Unit,
    onAllowSuccess: () -> Unit,
    pairingRepository: PairingRepository = PairingRepository.instance,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    // Keep screen on while this page is open
    DisposableEffect(Unit) {
        PlatformPermissionsHelper.setKeepScreenOn(true)
        onDispose {
            PlatformPermissionsHelper.setKeepScreenOn(false)
        }
    }

    // Initialize or get parent pairing session
    LaunchedEffect(childId) {
        pairingRepository.createOrGetParentSession(childId, childName)
    }

    val activeSession: ParentPairingSession? by pairingRepository.activeParentSessionFlow.collectAsState()

    // 1-second countdown timer
    var currentTime by remember { mutableStateOf(currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = currentTimeMillis()
        }
    }

    val remainingMs = remember(currentTime, activeSession?.expiresAtMs) {
        maxOf(0L, (activeSession?.expiresAtMs ?: 0L) - currentTime)
    }
    val isExpired = remember(remainingMs, activeSession?.state) {
        remainingMs <= 0L || activeSession?.state == ParentPairState.EXPIRED
    }

    val remainingFormatted = remember(remainingMs) {
        val totalSecs = (remainingMs / 1000).toInt()
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        "$mins:${if (secs < 10) "0$secs" else "$secs"}"
    }

    // Auto-transition: As soon as child redeems code or QR, open AllowPhoneSheet
    val showAllowSheet = activeSession?.state == ParentPairState.PENDING_APPROVAL

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
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 40.dp)
        ) {
            // Top row: Back button (asks confirm dialog)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0B))
                    .border(1.dp, Color(0xFF262626), CircleShape)
                    .clickable { showCancelConfirmDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            // Overline: ADD CHILD · 2 OF 2
            Text(
                text = "ADD CHILD · 2 OF 2",
                style = typography.overline.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF6B6B6B)
                )
            )

            Spacer(Modifier.height(4.dp))

            // Title: Connect {name}'s phone
            Text(
                text = "Connect ${childName}'s phone",
                style = typography.display.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "On ${childName}'s phone, open FOCAL, tap \"I'm a child\", then scan this QR or type the code.",
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            if (activeSession?.isDeclined == true) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Request declined. Share the new code.",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Main Card (Framed QR + or code + 6 digit boxes)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 20.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // QR block
                    val qrPayload = activeSession?.qrPayload ?: "focal://pair?code=000000"
                    FocalQrCodeCard(
                        payload = qrPayload,
                        isDimmed = isExpired,
                        contentSize = 220.dp
                    )

                    Spacer(Modifier.height(18.dp))

                    // Divider: "or code"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color(0xFF262626))
                        )
                        Text(
                            text = "or code",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = Color(0xFFA3A3A3)
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color(0xFF262626))
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // 6 digit boxes side-by-side (tap to copy)
                    val rawCode: String = activeSession?.code ?: "000000"
                    val displayCode: String = rawCode.padEnd(6, ' ')

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PlatformPermissionsHelper.copyToClipboard(rawCode)
                                showCopiedSnackbar = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0 until 6) {
                            val digit = displayCode.getOrNull(i)?.toString() ?: ""
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141414))
                                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    style = typography.display.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpired) Color(0xFF6B6B6B) else Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Row under code: "Expires in {m:ss} · works once" on left, "New code" on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpired) "Code expired" else "Expires in $remainingFormatted · works once",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = if (isExpired) Color(0xFFA3A3A3) else ChildColors.Secondary
                            )
                        )

                        Text(
                            text = "New code",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            modifier = Modifier.clickable {
                                pairingRepository.regenerateParentCode(childId)
                                PlatformPermissionsHelper.showToast("New code generated.")
                            }
                        )
                    }

                    if (isExpired) {
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            text = "Get a new code",
                            onClick = {
                                pairingRepository.regenerateParentCode(childId)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // StatusPill: "● Waiting for {name}'s phone…" (pulsing white dot)
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dotAlpha"
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF0E0E0E))
                        .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = dotAlpha))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Waiting for ${childName}'s phone…",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Demo connection confirmation button
            SecondaryButton(
                text = "${childName}'s phone is connected",
                onClick = {
                    val childIdx = repository.childrenList.indexOfFirst { it.id == childId }
                    if (childIdx >= 0) {
                        val child = repository.childrenList[childIdx]
                        repository.childrenList[childIdx] = child.copy(
                            isOnline = true,
                            pairedCardId = "FC0001",
                            deviceName = "Realme · Android 14"
                        )
                    }
                    pairingRepository.approveChildPairing(childId)
                    onAllowSuccess()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Demo mode · tap when your child's phone shows Connected",
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF6B6B6B)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (showCopiedSnackbar) {
                Spacer(Modifier.height(12.dp))
                LaunchedEffect(showCopiedSnackbar) {
                    delay(2000)
                    showCopiedSnackbar = false
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F))
                        .border(1.dp, Color(0xFF3A3A3A), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Code copied to clipboard",
                        style = typography.caption.copy(
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Cancel Pairing Confirm Dialog
        if (showCancelConfirmDialog) {
            Dialog(onDismissRequest = { showCancelConfirmDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Cancel pairing?",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "You can connect $childName's phone later from their profile.",
                            style = typography.body.copy(
                                fontSize = 13.5.sp,
                                color = ChildColors.Secondary
                            )
                        )

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SecondaryButton(
                                text = "Keep waiting",
                                onClick = { showCancelConfirmDialog = false },
                                modifier = Modifier.weight(1f).height(46.dp)
                            )

                            PrimaryButton(
                                text = "Cancel pairing",
                                onClick = {
                                    showCancelConfirmDialog = false
                                    onBack()
                                },
                                modifier = Modifier.weight(1f).height(46.dp)
                            )
                        }
                    }
                }
            }
        }

        // Screen 4: Allow Phone Sheet (ModalBottomSheet)
        if (showAllowSheet && activeSession != null) {
            AllowPhoneSheet(
                session = activeSession!!,
                onDismiss = {
                    // Decline or dismiss
                },
                onAllowed = {
                    onAllowSuccess()
                },
                pairingRepository = pairingRepository
            )
        }
    }
}

// =========================================================================
// SCREEN 4 — ALLOW PHONE SHEET (Matching PE3_allow_request.png)
// =========================================================================

/**
 * Allow Phone Sheet: ModalBottomSheet presented when a child phone redeems code or QR.
 * - Top radius 32dp, drag handle
 * - Centered 64dp phone icon circle with soft ambient glow
 * - Title "{child}'s phone wants to connect" + subtitle
 * - InfoCard: Phone · Code used · Requested
 * - Two buttons: SecondaryButton "Decline" | PrimaryButton "Allow"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowPhoneSheet(
    session: ParentPairingSession,
    onDismiss: () -> Unit,
    onAllowed: () -> Unit,
    pairingRepository: PairingRepository = PairingRepository.instance
) {
    val typography = LocalChildTypography.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3A3A))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centred 64dp phone icon circle with soft ambient glow
            Box(
                modifier = Modifier
                    .size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF171717))
                        .border(1.5.dp, Color(0xFF2A2A2A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Smartphone,
                        contentDescription = "Phone",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Title: {child}'s phone wants to connect
            Text(
                text = "${session.childName}'s phone wants to connect",
                style = typography.headline.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Only allow it if this is your child's phone.",
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // InfoCard (GlassCard with label/value rows, radius 20dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF131313), Color(0xFF0A0A0A))
                        )
                    )
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Phone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Phone",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            )
                        )
                        Text(
                            text = "${session.deviceManufacturer} · Android ${session.androidVersion}",
                            style = typography.body.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF1F1F1F))
                    )

                    // Row 2: Code used
                    val formattedCode = remember(session.code) {
                        if (session.code.length == 6) {
                            "${session.code.take(3)} ${session.code.drop(3)}"
                        } else {
                            session.code
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Code used",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            )
                        )
                        Text(
                            text = formattedCode,
                            style = typography.body.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF1F1F1F))
                    )

                    // Row 3: Requested
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Requested",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            )
                        )
                        Text(
                            text = "Just now",
                            style = typography.body.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            // Two buttons side-by-side: SecondaryButton "Decline" | PrimaryButton "Allow"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = "Decline",
                    onClick = {
                        pairingRepository.declineChildPairing(session.childId)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                )

                PrimaryButton(
                    text = "Allow",
                    onClick = {
                        pairingRepository.approveChildPairing(session.childId)
                        onAllowed()
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                )
            }
        }
    }
}

// =========================================================================
// ADD CHILD FLOW COORDINATOR
// =========================================================================

enum class AddChildStep {
    STEP_1_DETAILS,
    STEP_2_CONNECT
}

/**
 * Coordinates Step 1 and Step 2 of the Add Child flow, and notifies parent whether
 * sub-screens are active so the floating bottom bar can be hidden.
 */
@Composable
fun AddChildRootFlow(
    onDismiss: () -> Unit,
    onChildConnected: (ChildProfile) -> Unit,
    onEditingStateChanged: (Boolean) -> Unit = {},
    initialChildId: String? = null,
    repository: FocalRepository = FocalRepository.instance,
    pairingRepository: PairingRepository = PairingRepository.instance
) {
    var step by remember {
        mutableStateOf(if (initialChildId != null) AddChildStep.STEP_2_CONNECT else AddChildStep.STEP_1_DETAILS)
    }
    var activeChild by remember {
        mutableStateOf(repository.childrenList.find { it.id == initialChildId })
    }

    LaunchedEffect(Unit) {
        onEditingStateChanged(true)
    }

    DisposableEffect(Unit) {
        onDispose {
            onEditingStateChanged(false)
        }
    }

    when (step) {
        AddChildStep.STEP_1_DETAILS -> {
            AddChildStep1Screen(
                onBack = {
                    onEditingStateChanged(false)
                    onDismiss()
                },
                onContinue = { name, grade ->
                    // Add new child record with deviceName = "" (Phone not connected yet)
                    val newChild = ChildProfile(
                        id = "child_${currentTimeMillis()}",
                        userId = "user_${name.lowercase().replace(" ", "")}",
                        controllerId = repository.currentUser.id.ifBlank { "parent_01" },
                        name = name,
                        grade = grade.ifBlank { "Student" },
                        deviceName = "", // Phone not connected yet
                        isOnline = false,
                        pairedCardId = null,
                        todayFocusMinutes = 0,
                        screenTimeMinutes = 0
                    )
                    repository.childrenList.add(newChild)
                    activeChild = newChild
                    step = AddChildStep.STEP_2_CONNECT
                },
                existingChildren = repository.childrenList
            )
        }

        AddChildStep.STEP_2_CONNECT -> {
            val child = activeChild ?: repository.childrenList.lastOrNull()
            if (child != null) {
                ConnectPhoneScreen(
                    childId = child.id,
                    childName = child.name,
                    onBack = {
                        onEditingStateChanged(false)
                        onDismiss()
                    },
                    onAllowSuccess = {
                        onEditingStateChanged(false)
                        PlatformPermissionsHelper.showToast("${child.name} is connected")
                        val updatedChild = repository.childrenList.find { it.id == child.id } ?: child
                        onChildConnected(updatedChild)
                    },
                    pairingRepository = pairingRepository
                )
            } else {
                LaunchedEffect(Unit) {
                    onEditingStateChanged(false)
                    onDismiss()
                }
            }
        }
    }
}
