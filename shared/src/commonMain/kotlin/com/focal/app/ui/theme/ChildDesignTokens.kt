package com.focal.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import focal.shared.generated.resources.Res
import focal.shared.generated.resources.geist_bold
import focal.shared.generated.resources.geist_medium
import focal.shared.generated.resources.geist_regular
import focal.shared.generated.resources.geist_semibold
import org.jetbrains.compose.resources.Font

/**
 * CHILD APP DESIGN TOKENS
 * Strict monochrome system. No yellow or other colours permitted.
 */
object ChildColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF0B0B0B)
    val CardGradientStart = Color(0xFF131313)
    val CardGradientEnd = Color(0xFF0A0A0A)
    val Border = Color(0xFF262626)
    val BorderStrong = Color(0xFF3A3A3A)
    val BorderHighlight = Color(0x0FFFFFFF) // 1dp top inner highlight: white 6%
    val Text = Color(0xFFFFFFFF)
    val Secondary = Color(0xFFA3A3A3)
    val Tertiary = Color(0xFF6B6B6B)
    val Danger = Color(0xFFFF453A) // only errors and "Protection is off"

    // Component-specific neutral tokens
    val IconCircleBg = Color(0xFF171717)
    val ListDivider = Color(0xFF1F1F1F)
    val NavBg = Color(0xFF0B0B0B)
    val NavSelected = Color(0xFF1E1E1E)
    val BottomSheetBgStart = Color(0xFF141414)
    val BottomSheetBgEnd = Color(0xFF0A0A0A)
    val BannerBg = Color(0xFF0E0E0E)
    val ButtonGradientEnd = Color(0xFFDEDEDE)
    val ChipTextUnselected = Color(0xFFD4D4D4)
}

object ChildRadius {
    val Card = 24.dp
    val SmallCard = 20.dp
    val Input = 16.dp
    val Sheet = 32.dp
    val Pill = 999.dp

    val CardShape = RoundedCornerShape(24.dp)
    val SmallCardShape = RoundedCornerShape(20.dp)
    val InputShape = RoundedCornerShape(16.dp)
    val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val PillShape = CircleShape
}

data class ChildTypographyTokens(
    val display: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle
)

@Composable
fun getGeistFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.geist_regular, FontWeight.Normal),
        Font(Res.font.geist_medium, FontWeight.Medium),
        Font(Res.font.geist_semibold, FontWeight.SemiBold),
        Font(Res.font.geist_bold, FontWeight.Bold)
    )
}

@Composable
fun getChildTypography(fontFamily: FontFamily = getGeistFontFamily()): ChildTypographyTokens {
    return ChildTypographyTokens(
        display = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            color = ChildColors.Text
        ),
        title = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            color = ChildColors.Text
        ),
        headline = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            color = ChildColors.Text
        ),
        body = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = ChildColors.Text
        ),
        caption = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = ChildColors.Secondary
        ),
        overline = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.5.sp,
            color = ChildColors.Secondary
        )
    )
}

val LocalChildTypography = staticCompositionLocalOf<ChildTypographyTokens> {
    ChildTypographyTokens(
        display = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, color = ChildColors.Text),
        title = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = ChildColors.Text),
        headline = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = ChildColors.Text),
        body = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, color = ChildColors.Text),
        caption = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, color = ChildColors.Secondary),
        overline = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.5.sp, color = ChildColors.Secondary)
    )
}

@Composable
fun ChildTheme(
    content: @Composable () -> Unit
) {
    val typography = getChildTypography()
    CompositionLocalProvider(
        LocalChildTypography provides typography,
        content = content
    )
}
