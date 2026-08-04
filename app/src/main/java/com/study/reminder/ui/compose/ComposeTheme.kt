package com.study.reminder.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Color Palette ─────────────────────────────────────────────

private val Accent = Color(0xFF39C5BB)
private val AccentDark = Color(0xFF2AA89F)
private val AccentPink = Color(0xFFFF77B7)
private val AccentBlue = Color(0xFF5FD7FF)

// ── Design Tokens ─────────────────────────────────────────────

@Immutable
data class AppColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val cardSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentDark: Color,
    val accentPink: Color,
    val accentBlue: Color,
    val divider: Color,
    val glassStroke: Color,
    val glassFill: Color,
    val navBar: Color,
    val infoBg: Color,
    val infoText: Color,
    val white: Color,
    val inputBg: Color,
    val success: Color,
    val error: Color
)

val LightColors = AppColorScheme(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF1A1A1A),
    card = Color(0xFF1E1E1E),
    cardSoft = Color(0xFF181818),
    textPrimary = Color(0xFFF0F0F0),
    textSecondary = Color(0xFFA0A0A0),
    textTertiary = Color(0xFF6B6B6B),
    accent = Accent,
    accentDark = AccentDark,
    accentPink = AccentPink,
    accentBlue = AccentBlue,
    divider = Color(0x12FFFFFF),
    glassStroke = Color(0x18FFFFFF),
    glassFill = Color(0xEE1A1A1A),
    navBar = Color(0xF0141414),
    infoBg = Color(0xFF1A2925),
    infoText = Color(0xFF7ECEB8),
    white = Color.White,
    inputBg = Color(0xFF1A1A1A),
    success = Color(0xFF30D158),
    error = Color(0xFFFF6961)
)

val DarkColors = AppColorScheme(
    background = Color(0xFF0A1A1F),
    surface = Color(0xFF13252B),
    surfaceVariant = Color(0xFF1A2E35),
    card = Color(0xFF1E343C),
    cardSoft = Color(0xFF162A31),
    textPrimary = Color(0xFFE8F4F2),
    textSecondary = Color(0xFF9AB8B4),
    textTertiary = Color(0xFF6A8E8A),
    accent = Accent,
    accentDark = AccentDark,
    accentPink = AccentPink,
    accentBlue = AccentBlue,
    divider = Color(0x15FFFFFF),
    glassStroke = Color(0x20FFFFFF),
    glassFill = Color(0xCC1A2E35),
    navBar = Color(0xE613252B),
    infoBg = Color(0xFF1A3029),
    infoText = Color(0xFF7ECEB8),
    white = Color.White,
    inputBg = Color(0xFF1A2E35),
    success = Color(0xFF30D158),
    error = Color(0xFFFF6961)
)

// ── CompositionLocal for AppColors ───────────────────────────

val LocalAppColors = staticCompositionLocalOf<Any> { DarkAppColorScheme }

// Dark theme color scheme
object DarkAppColorScheme : DynamicColors {
    override val Background = Color(0xFF0A0A0A)
    override val Surface = Color(0xFF141414)
    override val CardBg = Color(0xFF1E1E1E)
    override val CardBgSoft = Color(0xFF181818)
    override val CardSoft = Color(0xFF181818)
    override val TextPrimary = Color(0xFFF0F0F0)
    override val TextSecondary = Color(0xFFA0A0A0)
    override val TextTertiary = Color(0xFF6B6B6B)
    override val Accent = Color(0xFF39C5BB)
    override val AccentDark = Color(0xFF2AA89F)
    override val AccentPink = Color(0xFFFF77B7)
    override val AccentBlue = Color(0xFF5FD7FF)
    override val GlassStroke = Color(0x18FFFFFF)
    override val InfoBg = Color(0xFF1A2925)
    override val InfoText = Color(0xFF7ECEB8)
    override val White = Color.White
    override val LightDivider = Color(0x12FFFFFF)
    override val WeekSelectedBg = Color(0x12FFFFFF)
    override val InputBg = Color(0xFF1A1A1A)
    override val Success = Color(0xFF30D158)
    override val Error = Color(0xFFFF6961)
    override val NavBar = Color(0xF0141414)
}

// Light theme color scheme
object LightAppColorScheme : DynamicColors {
    override val Background = Color(0xFFF4FAF9)
    override val Surface = Color.White
    override val CardBg = Color(0xFFF8FFFE)
    override val CardBgSoft = Color(0xFFEDF8F6)
    override val CardSoft = Color(0xFFEDF8F6)
    override val TextPrimary = Color(0xFF0D2F33)
    override val TextSecondary = Color(0xFF4A7A80)
    override val TextTertiary = Color(0xFF8AABAE)
    override val Accent = Color(0xFF39C5BB)
    override val AccentDark = Color(0xFF2AA89F)
    override val AccentPink = Color(0xFFFF77B7)
    override val AccentBlue = Color(0xFF5FD7FF)
    override val GlassStroke = Color(0x30FFFFFF)
    override val InfoBg = Color(0xFFE8FFF8)
    override val InfoText = Color(0xFF1A6B60)
    override val White = Color.White
    override val LightDivider = Color(0x0C000000)
    override val WeekSelectedBg = Color(0x0C000000)
    override val InputBg = Color(0xFFF0F7F6)
    override val Success = Color(0xFF34C759)
    override val Error = Color(0xFFFF3B30)
    override val NavBar = Color(0xF2FFFFFF)
}

// Backward-compatible AppColors object (uses CompositionLocal)
object AppColors {
    @Composable
    private fun colors(): DynamicColors {
        return when (val c = LocalAppColors.current) {
            is DarkAppColorScheme -> c
            is LightAppColorScheme -> c
            else -> DarkAppColorScheme
        }
    }

    val Background @Composable get() = colors().Background
    val Surface @Composable get() = colors().Surface
    val CardBg @Composable get() = colors().CardBg
    val CardBgSoft @Composable get() = colors().CardBgSoft
    val CardSoft @Composable get() = colors().CardSoft
    val TextPrimary @Composable get() = colors().TextPrimary
    val TextSecondary @Composable get() = colors().TextSecondary
    val TextTertiary @Composable get() = colors().TextTertiary
    val Accent @Composable get() = colors().Accent
    val AccentDark @Composable get() = colors().AccentDark
    val AccentPink @Composable get() = colors().AccentPink
    val AccentBlue @Composable get() = colors().AccentBlue
    val GlassStroke @Composable get() = colors().GlassStroke
    val InfoBg @Composable get() = colors().InfoBg
    val InfoText @Composable get() = colors().InfoText
    val White @Composable get() = colors().White
    val LightDivider @Composable get() = colors().LightDivider
    val WeekSelectedBg @Composable get() = colors().WeekSelectedBg
    val InputBg @Composable get() = colors().InputBg
    val Success @Composable get() = colors().Success
    val Error @Composable get() = colors().Error
    val NavBar @Composable get() = colors().NavBar
}

// Interface for color schemes
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

// ── Typography ────────────────────────────────────────────────

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

// ── Spacing ───────────────────────────────────────────────────

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

// ── Corner Radius ─────────────────────────────────────────────

object CornerRadius {
    val sm = 6
    val md = 10
    val lg = 12
    val xl = 16
    val pill = 50
}

// ── Material 3 Theme Wrapper ──────────────────────────────────

private val StudyLightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = AccentPink,
    tertiary = AccentBlue,
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF141414),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF0F0F0),
    onBackground = Color(0xFFF0F0F0),
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF222222)
)

private val StudyDarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF062227),
    secondary = AccentPink,
    tertiary = AccentBlue,
    background = Color(0xFF0A1A1F),
    surface = Color(0xFF13252B),
    surfaceVariant = Color(0xFF1A2E35),
    onSurface = Color(0xFFE8F4F2),
    onBackground = Color(0xFFE8F4F2),
    outline = Color(0xFF3A5560),
    outlineVariant = Color(0xFF2A4048)
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
            content = content
        )
    }
}

// Composable function to get current theme colors
@Composable
fun currentAppColors(): AppColorScheme {
    val isDark = isSystemInDarkTheme()
    return if (isDark) DarkColors else LightColors
}
