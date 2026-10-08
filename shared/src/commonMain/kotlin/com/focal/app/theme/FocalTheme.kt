package com.focal.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FOCAL Strict Minimal Monochrome Spacing Tokens
 */
object FocalSpacing {
    val ScreenPadding = 20.dp
    val CardPadding = 16.dp
    val ItemSpacing = 12.dp
    val SmallSpacing = 8.dp
    val TinySpacing = 4.dp
    val LargeSpacing = 24.dp
}

/**
 * FOCAL Strict Minimal Monochrome Design Tokens
 */
object FocalColors {
    // Surfaces
    val Background = Color(0xFF000000)
    val SecondaryBackground = Color(0xFF0E0E0E)
    val Surface = Color(0xFF0E0E0E)
    val Card = Color(0xFF0E0E0E)
    val CardElevated = Color(0xFF0E0E0E)
    val Pressed = Color(0xFF1A1A1A)
    val CardHighlighted = Color(0xFF0E0E0E)
    val CardGlow = Color(0x1AFFFFFF)

    // Borders
    val Border = Color(0xFF1F1F1F)
    val BorderSubtle = Color(0xFF1F1F1F)
    val BorderSecondary = Color(0xFF2A2A2A)
    val BorderGlow = Color(0xFF1F1F1F)

    // Typography & Text
    val TextPrimary = Color(0xFFFFFFFF)
    val PrimaryText = TextPrimary
    val TextSecondary = Color(0xFF8A8A8A)
    val SecondaryText = TextSecondary
    val TextTertiary = Color(0xFF555555)
    val TextMuted = TextTertiary
    val TextDisabled = TextTertiary
    val DisabledText = TextTertiary

    // Status Tokens (Strictly Monochrome + Danger)
    val PrimaryAccent = Color(0xFFFFFFFF)
    val PrimaryAccentLight = Color(0xFFFFFFFF)
    val PrimaryAccentDark = Color(0xFF8A8A8A)
    val SecondaryAccent = Color(0xFF8A8A8A)
    val SecondaryAccentLight = Color(0xFF8A8A8A)
    val PrimaryPurple = Color(0xFFFFFFFF)
    val SecondaryBlue = Color(0xFF8A8A8A)
    val AccentCyan = Color(0xFFFFFFFF)

    val Danger = Color(0xFFFF453A)
    val Error = Danger
    val StatusActive = Color(0xFFFFFFFF)
    val StatusIdle = Color(0xFF555555)
    val Success = StatusActive
    val Warning = TextSecondary
    val Offline = StatusIdle

    // Container Tokens
    val ErrorContainer = Color(0x1FFF453A)
    val SuccessContainer = Color(0x1F1A1A1A)
    val WarningContainer = Color(0x1F1A1A1A)

    // Charts & Displays
    val ChartFocus = Color(0xFFFFFFFF)
    val ChartScreenTime = Color(0xFF3A3A3A)
    val ProgressTrack = Color(0xFF1F1F1F)
    val ProgressArc = Color(0xFFFFFFFF)
}

/**
 * Typography Scale per Spec:
 * Title 28sp bold, Header 17sp semibold, Body 15sp, Caption 13sp #8A8A8A.
 */
val FocalTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = FocalColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = FocalColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        color = FocalColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = FocalColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = FocalColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = FocalColors.TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = FocalColors.TextPrimary
    )
)

val FocalShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

val FocalDarkColorScheme = darkColorScheme(
    primary = FocalColors.PrimaryAccent,
    onPrimary = Color.Black,
    primaryContainer = FocalColors.Surface,
    onPrimaryContainer = Color.White,
    secondary = FocalColors.SecondaryAccent,
    onSecondary = Color.Black,
    secondaryContainer = FocalColors.Surface,
    onSecondaryContainer = Color.White,
    tertiary = FocalColors.TextTertiary,
    onTertiary = Color.White,
    tertiaryContainer = FocalColors.Surface,
    onTertiaryContainer = Color.White,
    background = FocalColors.Background,
    onBackground = FocalColors.TextPrimary,
    surface = FocalColors.Surface,
    onSurface = FocalColors.TextPrimary,
    surfaceVariant = FocalColors.Surface,
    onSurfaceVariant = FocalColors.TextSecondary,
    outline = FocalColors.Border,
    outlineVariant = FocalColors.BorderSecondary,
    error = FocalColors.Danger,
    onError = Color.White,
    errorContainer = FocalColors.ErrorContainer,
    onErrorContainer = FocalColors.Danger
)

@Composable
fun FocalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FocalDarkColorScheme,
        typography = FocalTypography,
        shapes = FocalShapes,
        content = content
    )
}
