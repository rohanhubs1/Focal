package com.focal.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.theme.FocalColors
import com.focal.app.theme.FocalSpacing
import focal.shared.generated.resources.Res
import focal.shared.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource

// -------------------------------------------------------------
// App Logo Component
// -------------------------------------------------------------
@Composable
fun FocalAppLogo(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    contentDescription: String? = "FOCAL Logo"
) {
    Image(
        painter = painterResource(Res.drawable.app_logo),
        contentDescription = contentDescription,
        modifier = modifier.clip(shape)
    )
}

// -------------------------------------------------------------
// 1. FOCAL BUTTON (Primary / Secondary / Danger, Loading state)
// -------------------------------------------------------------
enum class FocalButtonVariant {
    PRIMARY,
    SECONDARY,
    DANGER
}

@Composable
fun FocalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FocalButtonVariant = FocalButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.98f else 1f,
        animationSpec = tween(durationMillis = 200)
    )

    val (bg, border, textColor) = when (variant) {
        FocalButtonVariant.PRIMARY -> Triple(
            if (enabled) Color.White else FocalColors.Border,
            Color.Transparent,
            if (enabled) Color.Black else FocalColors.TextMuted
        )
        FocalButtonVariant.SECONDARY -> Triple(
            Color.Transparent,
            FocalColors.BorderSecondary,
            if (enabled) FocalColors.TextPrimary else FocalColors.TextMuted
        )
        FocalButtonVariant.DANGER -> Triple(
            Color.Transparent,
            FocalColors.Danger,
            if (enabled) FocalColors.Danger else FocalColors.Danger.copy(alpha = 0.5f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (border != Color.Transparent) 1.dp else 0.dp, border, RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
        }
    }
}

// Helpers / Aliases for backwards compatibility
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    FocalButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = FocalButtonVariant.PRIMARY,
        icon = icon,
        enabled = enabled,
        isLoading = isLoading
    )
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = FocalColors.BorderSecondary,
    textColor: Color = FocalColors.TextPrimary
) {
    FocalButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = FocalButtonVariant.SECONDARY,
        icon = icon
    )
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    FocalButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = FocalButtonVariant.DANGER,
        icon = icon
    )
}

@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    DangerButton(text = text, onClick = onClick, modifier = modifier, icon = icon)
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    PrimaryButton(text = text, onClick = onClick, modifier = modifier, icon = icon, enabled = enabled, isLoading = isLoading)
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = FocalColors.BorderSecondary,
    textColor: Color = FocalColors.TextPrimary
) {
    SecondaryButton(text = text, onClick = onClick, modifier = modifier, icon = icon)
}

// -------------------------------------------------------------
// 2. FOCAL CARD (#0E0E0E, 16dp radius, 1dp #1F1F1F border, 16dp padding)
// -------------------------------------------------------------
@Composable
fun FocalCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    backgroundColor: Color = FocalColors.Surface,
    borderColor: Color = FocalColors.Border,
    borderWidth: Dp = 1.dp,
    padding: Dp = FocalSpacing.CardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    val clickableModifier = if (onClick != null) {
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    } else {
        modifier
    }

    Box(
        modifier = clickableModifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .padding(padding)
    ) {
        Column(content = content)
    }
}

// Alias for backwards compatibility
@Composable
fun DarkCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    backgroundColor: Color = FocalColors.Surface,
    borderColor: Color = FocalColors.Border,
    borderWidth: Dp = 1.dp,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    FocalCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        padding = padding,
        onClick = onClick,
        content = content
    )
}

@Composable
fun HighlightCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    borderColor: Color = FocalColors.Border,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    FocalCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = FocalColors.Surface,
        borderColor = borderColor,
        borderWidth = 1.dp,
        padding = padding,
        onClick = onClick,
        content = content
    )
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) FocalColors.Pressed else FocalColors.Surface,
        animationSpec = tween(250)
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) Color.White else FocalColors.Border,
        animationSpec = tween(250)
    )

    FocalCard(
        modifier = modifier,
        backgroundColor = animatedBg,
        borderColor = animatedBorder,
        borderWidth = 1.dp,
        padding = padding,
        onClick = onClick,
        content = content
    )
}

// -------------------------------------------------------------
// 3. FOCAL LIST ROW (Icon, Title, Subtitle, Trailing Chevron/Value/Toggle)
// -------------------------------------------------------------
@Composable
fun FocalListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector? = null,
    trailingToggle: Boolean? = null,
    trailingToggleChecked: Boolean? = trailingToggle,
    onToggleChange: ((Boolean) -> Unit)? = null,
    onToggleChanged: ((Boolean) -> Unit)? = onToggleChange,
    onClick: (() -> Unit)? = null
) {
    val actualToggleChecked = trailingToggleChecked ?: trailingToggle
    val actualToggleHandler = onToggleChanged ?: onToggleChange

    FocalCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FocalColors.Pressed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                    )
                }
            }

            if (actualToggleChecked != null && actualToggleHandler != null) {
                Spacer(modifier = Modifier.width(8.dp))
                FocalToggle(
                    checked = actualToggleChecked,
                    onCheckedChange = actualToggleHandler
                )
            } else if (trailingText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocalColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            } else if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = FocalColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            } else if (onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = FocalIcons.ArrowForward,
                    contentDescription = "Navigate",
                    tint = FocalColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 4. STAT CARD (Label, Value, Delta Caption e.g. "+42m vs avg")
// -------------------------------------------------------------
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    deltaCaption: String? = delta
) {
    val actualDelta = deltaCaption ?: delta
    FocalCard(
        modifier = modifier,
        padding = 12.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    color = FocalColors.TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                ),
                maxLines = 1
            )
            if (actualDelta != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = actualDelta,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocalColors.TextSecondary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 5. FOCAL TEXT FIELD (Icon, Password Eye Toggle)
// -------------------------------------------------------------
@Composable
fun FocalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    icon: ImageVector? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label, color = FocalColors.TextSecondary) },
        placeholder = if (placeholder != null) { { Text(placeholder, color = FocalColors.TextMuted) } } else null,
        leadingIcon = if (icon != null) {
            { Icon(imageVector = icon, contentDescription = null, tint = FocalColors.TextSecondary) }
        } else null,
        trailingIcon = if (isPassword) {
            {
                Text(
                    text = if (passwordVisible) "HIDE" else "SHOW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .clickable { passwordVisible = !passwordVisible }
                        .padding(8.dp)
                )
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White,
            unfocusedBorderColor = FocalColors.Border,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

// -------------------------------------------------------------
// 6. FOCAL TOGGLE (White thumb on white track when ON; #2A2A2A track when OFF)
// -------------------------------------------------------------
@Composable
fun FocalToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.Black,
            checkedTrackColor = Color.White,
            checkedBorderColor = Color.White,
            uncheckedThumbColor = FocalColors.TextSecondary,
            uncheckedTrackColor = FocalColors.BorderSecondary,
            uncheckedBorderColor = FocalColors.BorderSecondary
        )
    )
}

// -------------------------------------------------------------
// 7. CHIP SELECTOR (Days of Week e.g. Mon, Tue, Wed, Thu, Fri, Sat, Sun)
// -------------------------------------------------------------
@Composable
fun ChipSelector(
    options: List<String>,
    selectedOptions: Set<String>,
    onOptionToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedOptions.contains(option)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color.White else FocalColors.Surface)
                    .border(1.dp, if (isSelected) Color.White else FocalColors.Border, RoundedCornerShape(10.dp))
                    .clickable { onOptionToggled(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else FocalColors.TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 8. STATUS DOT (White = Active/Focusing, #555555 = Idle)
// -------------------------------------------------------------
enum class BadgeType {
    ACTIVE,
    FOCUSING,
    PENDING,
    EXPIRED,
    OFFLINE,
    IDLE
}

@Composable
fun StatusDot(
    isActive: Boolean = true,
    active: Boolean = isActive,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    val isDotActive = active && isActive
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isDotActive) FocalColors.StatusActive else FocalColors.StatusIdle)
    )
}

@Composable
fun FocalStatusDot(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    StatusDot(isActive = isActive, modifier = modifier, size = size)
}

@Composable
fun StatusBadge(
    text: String,
    badgeType: BadgeType? = null,
    backgroundColor: Color? = null,
    textColor: Color? = null,
    dotColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val isActive = badgeType == BadgeType.ACTIVE || badgeType == BadgeType.FOCUSING
    val dot = dotColor ?: if (isActive) FocalColors.StatusActive else FocalColors.StatusIdle
    val bg = backgroundColor ?: FocalColors.Pressed
    val txt = textColor ?: if (isActive) Color.White else FocalColors.TextSecondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, FocalColors.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(isActive = isActive, size = 6.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = txt,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}

// -------------------------------------------------------------
// 9. INITIALS AVATAR (Initials Text on #1A1A1A Circle)
// -------------------------------------------------------------
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val initials = com.focal.app.ui.child.components.getAvatarInitials(name)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(FocalColors.Pressed)
            .border(1.dp, FocalColors.Border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = (size.value * 0.38f).sp
            )
        )
    }
}

@Composable
fun FocalAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    InitialsAvatar(name = name, modifier = modifier, size = size)
}

// -------------------------------------------------------------
// 10. PROGRESS RING (Time Remaining, White Arc on #1F1F1F Track)
// -------------------------------------------------------------
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    centerContent: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = centerContent
) {
    val actualContent = content ?: centerContent
    Box(
        modifier = modifier.drawBehind {
            val sw = strokeWidth.toPx()
            val radius = (size.minDimension - sw) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Track
            drawCircle(
                color = FocalColors.ProgressTrack,
                radius = radius,
                style = Stroke(width = sw)
            )

            // Arc
            val sweepAngle = progress.coerceIn(0f, 1f) * 360f
            drawArc(
                color = FocalColors.ProgressArc,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = sw, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        },
        contentAlignment = Alignment.Center
    ) {
        actualContent?.invoke()
    }
}

@Composable
fun FocalProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    content: @Composable (() -> Unit)? = null
) {
    ProgressRing(progress = progress, modifier = modifier, strokeWidth = strokeWidth, content = content)
}

@Composable
fun FocusTimer(
    timeFormatted: String,
    modeTitle: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    ProgressRing(
        progress = progress,
        modifier = modifier.size(230.dp),
        strokeWidth = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StatusBadge(
                text = modeTitle.uppercase(),
                badgeType = BadgeType.FOCUSING
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "REMAINING",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontSize = 11.sp,
                    color = FocalColors.TextSecondary
                ),
                maxLines = 1
            )
        }
    }
}

// -------------------------------------------------------------
// 11. BAR CHART (Two Series: Focus bars white #FFFFFF, Screen time bars #3A3A3A)
// -------------------------------------------------------------
data class BarChartItem(
    val label: String,
    val focusHours: Float,
    val screenTimeHours: Float
)

@Composable
fun BarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    maxHours: Float = 8f
) {
    FocalCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FOCUS VS SCREEN TIME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        color = FocalColors.TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FocalColors.ChartFocus))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Focus", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FocalColors.ChartScreenTime))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Screen Time", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { item ->
                    val focusHeight = (item.focusHours / maxHours).coerceIn(0.05f, 1f)
                    val screenHeight = (item.screenTimeHours / maxHours).coerceIn(0.05f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f, fill = true)
                        ) {
                            // Focus Bar (White)
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight(focusHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(FocalColors.ChartFocus)
                            )
                            // Screen Time Bar (#3A3A3A)
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight(screenHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(FocalColors.ChartScreenTime)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = FocalColors.TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 12. SEGMENTED TABS (Week / Month Selection)
// -------------------------------------------------------------
@Composable
fun SegmentedTabs(
    options: List<String> = emptyList(),
    tabs: List<String> = options,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val actualOptions = if (tabs.isNotEmpty()) tabs else options
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FocalColors.Surface)
            .border(1.dp, FocalColors.Border, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            actualOptions.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else FocalColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 13. FOCAL TOP BAR (Back, Title, Action)
// -------------------------------------------------------------
@Composable
fun FocalTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 56.dp)
            .background(Color.Black)
            .padding(horizontal = FocalSpacing.ScreenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                FocalIconButton(
                    icon = FocalIcons.ArrowBack,
                    onClick = onBackClick,
                    contentDescription = "Back"
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 2
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

// -------------------------------------------------------------
// 14. FOCAL BOTTOM NAV (Black, 1dp Top Border #1F1F1F, Selected White, Unselected #555555, No Pill)
// -------------------------------------------------------------
data class FocalNavItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun FocalBottomNav(
    items: List<FocalNavItem>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .border(BorderStroke(1.dp, FocalColors.Border), shape = RoundedCornerShape(0.dp))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val tint = if (isSelected) Color.White else FocalColors.TextTertiary

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectTab(index) }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = tint
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 15. BOTTOM SHEET / DIALOG CONTAINERS
// -------------------------------------------------------------
@Composable
fun FocalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(FocalColors.Surface)
                    .border(1.dp, FocalColors.Border, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(24.dp)
            ) {
                Column(content = content)
            }
        }
    }
}

@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    FocalBottomSheet(onDismissRequest = onDismiss, modifier = modifier, content = content)
}

@Composable
fun FocalDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        FocalCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            content = content
        )
    }
}

// -------------------------------------------------------------
// 16. EMPTY STATE, 17. SUCCESS STATE, 18. ERROR STATE
// -------------------------------------------------------------
@Composable
fun EmptyStateComponent(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FocalSpacing.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(FocalColors.Pressed)
                .border(1.dp, FocalColors.Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FocalIcons.Shield,
                contentDescription = null,
                tint = FocalColors.TextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = FocalColors.TextSecondary
            ),
            textAlign = TextAlign.Center
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.widthIn(max = 220.dp)
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = FocalIcons.Shield,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    EmptyStateComponent(
        title = title,
        description = message,
        modifier = modifier,
        actionText = actionText,
        onActionClick = onActionClick
    )
}

@Composable
fun SuccessState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String = "Done",
    onActionClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FocalSpacing.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(FocalColors.Pressed)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FocalIcons.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(text = actionText, onClick = onActionClick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ErrorState(
    title: String = "Something went wrong",
    description: String = "Unable to complete request. Please check your connection and try again.",
    message: String = description,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FocalSpacing.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(FocalColors.ErrorContainer)
                .border(1.dp, FocalColors.Danger, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FocalIcons.Shield,
                contentDescription = null,
                tint = FocalColors.Danger,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        SecondaryButton(
            text = "Try Again",
            onClick = onRetry,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}

@Composable
fun NfcNotSupportedState(
    onUseQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FocalSpacing.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(FocalColors.Pressed)
                .border(1.dp, FocalColors.BorderSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FocalIcons.Nfc,
                contentDescription = null,
                tint = FocalColors.TextSecondary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "NFC Not Supported / Turned Off",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your device doesn't support NFC or NFC is disabled in settings. Use QR scanning instead.",
            style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(
            text = "Use QR Code Instead",
            onClick = onUseQrClick,
            icon = FocalIcons.QrCode,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NoInternetState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorState(
        title = "No Internet Connection",
        description = "You're offline. Please check your network connection and try again.",
        onRetry = onRetry,
        modifier = modifier
    )
}

// -------------------------------------------------------------
// 19. LOADING SKELETON
// -------------------------------------------------------------
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp
) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(FocalColors.Pressed.copy(alpha = alpha))
            .border(1.dp, FocalColors.Border, RoundedCornerShape(16.dp))
    )
}

@Composable
fun LoadingStateComponent(
    modifier: Modifier = Modifier,
    label: String = "Loading..."
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
        )
    }
}

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    label: String = "Loading..."
) {
    LoadingStateComponent(modifier = modifier, label = label)
}

// -------------------------------------------------------------
// 20. BANNER (Warning style, Red ONLY for "Protection Off")
// -------------------------------------------------------------
@Composable
fun Banner(
    message: String,
    title: String = "Protection Alert",
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
    isProtectionOff: Boolean = isDanger,
    onAction: (() -> Unit)? = null,
    onActionClick: (() -> Unit)? = onAction
) {
    val bg = if (isProtectionOff) FocalColors.ErrorContainer else FocalColors.Pressed
    val border = if (isProtectionOff) FocalColors.Danger else FocalColors.Border
    val textTint = if (isProtectionOff) FocalColors.Danger else Color.White

    FocalCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = bg,
        borderColor = border,
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(isActive = !isProtectionOff, size = 8.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textTint,
                            fontSize = 15.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                )
            }
            val clickHandler = onActionClick ?: onAction
            if (clickHandler != null) {
                Spacer(modifier = Modifier.width(12.dp))
                SecondaryButton(
                    text = "Fix",
                    onClick = clickHandler,
                    modifier = Modifier.widthIn(max = 70.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 21. QR CODE VIEW & 22. NFC PULSE ANIMATION
// -------------------------------------------------------------
@Composable
fun QrCodeView(
    qrContent: String,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = FocalIcons.QrCode,
            contentDescription = qrContent,
            tint = Color.Black,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun NfcPulseAnimation(
    detected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = 0.3f
                }
                .clip(CircleShape)
                .background(FocalColors.Pressed)
                .border(1.dp, Color.White, CircleShape)
        )

        // Center icon circle
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(FocalColors.Surface)
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FocalIcons.Nfc,
                contentDescription = "NFC Scanning",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

// Additional Legacy Components for screen back-compat
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    color = FocalColors.TextSecondary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (actionText != null && onActionClick != null) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { onActionClick() }
                )
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
            )
        }
    }
}

@Composable
fun FocalIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = FocalColors.TextPrimary,
    backgroundColor: Color = FocalColors.Pressed,
    borderColor: Color = FocalColors.Border
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

// -------------------------------------------------------------
// PAGE INDICATOR & TELEMETRY ALIASES
// -------------------------------------------------------------
@Composable
fun FocalPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) Color.White else FocalColors.TextSecondary.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
fun DualRingTelemetryTimer(
    timeText: String,
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    ProgressRing(
        progress = progressFraction,
        modifier = modifier.size(160.dp),
        strokeWidth = 6.dp,
        centerContent = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "REMAINING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocalColors.TextSecondary,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    )
}

@Composable
fun TelemetryStatsStrip(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FocalColors.Surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "8", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
            Text(text = "Apps Blocked", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "15", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
            Text(text = "Sites Blocked", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
        }
    }
}

@Composable
fun ComingSoonModal(
    featureName: String = "This feature",
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F))
                        .border(1.dp, Color(0xFF555555), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Coming in the Next Release",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$featureName requires multi-device cloud synchronization or backend server connection.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocalColors.TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    text = "Got it",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


