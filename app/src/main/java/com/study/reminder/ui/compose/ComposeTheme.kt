package com.study.reminder.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand Palette（主色调保持不变）────────────────────────────

private val Accent = Color(0xFF39C5BB)
private val AccentDark = Color(0xFF2AA89F)
private val AccentPink = Color(0xFFFF77B7)
private val AccentBlue = Color(0xFF5FD7FF)

/** 主色上的文字/图标颜色：深青比白色对比度更高，更精致 */
val OnAccent = Color(0xFF06272B)

// ── Color Schemes ────────────────────────────────────────────

interface DynamicColors {
    val Background: Color
    val Surface: Color
    val CardBg: Color
    val CardBgSoft: Color
    val CardSoft: Color
    val TextPrimary: Color
    val TextSecondary: Color
    val TextTertiary: Color
    val Accent: Color
    val AccentDark: Color
    val AccentPink: Color
    val AccentBlue: Color
    val GlassStroke: Color
    val InfoBg: Color
    val InfoText: Color
    val White: Color
    val LightDivider: Color
    val WeekSelectedBg: Color
    val InputBg: Color
    val Success: Color
    val Error: Color
    val NavBar: Color
}

/** 深色：中性近黑 + 一丝青调，毛玻璃卡片层次分明 */
object DarkAppColorScheme : DynamicColors {
    override val Background = Color(0xFF0B1013)
    override val Surface = Color(0xFF11181B)
    override val CardBg = Color(0xFF151E21)
    override val CardBgSoft = Color(0xFF111A1D)
    override val CardSoft = Color(0xFF131C1F)
    override val TextPrimary = Color(0xFFEAF3F2)
    override val TextSecondary = Color(0xFF9DB5B2)
    override val TextTertiary = Color(0xFF64807D)
    override val Accent = Color(0xFF39C5BB)
    override val AccentDark = Color(0xFF2AA89F)
    override val AccentPink = Color(0xFFFF77B7)
    override val AccentBlue = Color(0xFF5FD7FF)
    override val GlassStroke = Color(0x1AFFFFFF)
    override val InfoBg = Color(0xFF15221F)
    override val InfoText = Color(0xFF7ECEB8)
    override val White = Color.White
    override val LightDivider = Color(0x10FFFFFF)
    override val WeekSelectedBg = Color(0x10FFFFFF)
    override val InputBg = Color(0xFF161F22)
    override val Success = Color(0xFF30D158)
    override val Error = Color(0xFFFF6961)
    override val NavBar = Color(0xF20E1518)
}

/** 浅色：薄荷白底 + 白色玻璃卡片 */
object LightAppColorScheme : DynamicColors {
    override val Background = Color(0xFFF3F9F8)
    override val Surface = Color.White
    override val CardBg = Color(0xFFFBFFFE)
    override val CardBgSoft = Color(0xFFEFF7F5)
    override val CardSoft = Color(0xFFF2F9F7)
    override val TextPrimary = Color(0xFF0D2F33)
    override val TextSecondary = Color(0xFF4A7A80)
    override val TextTertiary = Color(0xFF8AABAE)
    override val Accent = Color(0xFF39C5BB)
    override val AccentDark = Color(0xFF2AA89F)
    override val AccentPink = Color(0xFFFF77B7)
    override val AccentBlue = Color(0xFF5FD7FF)
    override val GlassStroke = Color(0x59FFFFFF)
    override val InfoBg = Color(0xFFE8FFF8)
    override val InfoText = Color(0xFF1A6B60)
    override val White = Color.White
    override val LightDivider = Color(0x0C0D2F33)
    override val WeekSelectedBg = Color(0x0C0D2F33)
    override val InputBg = Color(0xFFEFF7F5)
    override val Success = Color(0xFF34C759)
    override val Error = Color(0xFFFF3B30)
    override val NavBar = Color(0xF7FFFFFF)
}

// ── CompositionLocal ─────────────────────────────────────────

val LocalAppColors = staticCompositionLocalOf<DynamicColors> { DarkAppColorScheme }

/** Backward-compatible color accessors used across screens. */
object AppColors {
    val Background @Composable get() = LocalAppColors.current.Background
    val Surface @Composable get() = LocalAppColors.current.Surface
    val CardBg @Composable get() = LocalAppColors.current.CardBg
    val CardBgSoft @Composable get() = LocalAppColors.current.CardBgSoft
    val CardSoft @Composable get() = LocalAppColors.current.CardSoft
    val TextPrimary @Composable get() = LocalAppColors.current.TextPrimary
    val TextSecondary @Composable get() = LocalAppColors.current.TextSecondary
    val TextTertiary @Composable get() = LocalAppColors.current.TextTertiary
    val Accent @Composable get() = LocalAppColors.current.Accent
    val AccentDark @Composable get() = LocalAppColors.current.AccentDark
    val AccentPink @Composable get() = LocalAppColors.current.AccentPink
    val AccentBlue @Composable get() = LocalAppColors.current.AccentBlue
    val GlassStroke @Composable get() = LocalAppColors.current.GlassStroke
    val InfoBg @Composable get() = LocalAppColors.current.InfoBg
    val InfoText @Composable get() = LocalAppColors.current.InfoText
    val White @Composable get() = LocalAppColors.current.White
    val LightDivider @Composable get() = LocalAppColors.current.LightDivider
    val WeekSelectedBg @Composable get() = LocalAppColors.current.WeekSelectedBg
    val InputBg @Composable get() = LocalAppColors.current.InputBg
    val Success @Composable get() = LocalAppColors.current.Success
    val Error @Composable get() = LocalAppColors.current.Error
    val NavBar @Composable get() = LocalAppColors.current.NavBar
}

// ── Typography ───────────────────────────────────────────────

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

// ── Spacing ──────────────────────────────────────────────────

object Spacing {
    val xxs = 2
    val xs = 4
    val sm = 8
    val md = 12
    val base = 16
    val lg = 20
    val xl = 24
    val xxl = 32
}

// ── Corner Radius ────────────────────────────────────────────

object CornerRadius {
    val sm = 6
    val md = 10
    val lg = 12
    val xl = 16
    val pill = 50
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(CornerRadius.sm.dp),
    small = RoundedCornerShape(CornerRadius.md.dp),
    medium = RoundedCornerShape(CornerRadius.lg.dp),
    large = RoundedCornerShape(CornerRadius.xl.dp),
    extraLarge = RoundedCornerShape(CornerRadius.xl.dp)
)

// ── Material 3 Schemes ───────────────────────────────────────

private val StudyLightColors = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = AccentPink,
    tertiary = AccentBlue,
    background = Color(0xFFF3F9F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFEFF7F5),
    onSurface = Color(0xFF0D2F33),
    onBackground = Color(0xFF0D2F33),
    onSurfaceVariant = Color(0xFF4A7A80),
    outline = Color(0xFFD3E7E4),
    outlineVariant = Color(0xFFE7F1EF)
)

private val StudyDarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = AccentPink,
    tertiary = AccentBlue,
    background = Color(0xFF0B1013),
    surface = Color(0xFF11181B),
    surfaceVariant = Color(0xFF161F22),
    onSurface = Color(0xFFEAF3F2),
    onBackground = Color(0xFFEAF3F2),
    onSurfaceVariant = Color(0xFF9DB5B2),
    outline = Color(0xFF2C3B3F),
    outlineVariant = Color(0xFF212D31)
)

@Composable
fun StudyReminderComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColorScheme else LightAppColorScheme
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) StudyDarkColors else StudyLightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
