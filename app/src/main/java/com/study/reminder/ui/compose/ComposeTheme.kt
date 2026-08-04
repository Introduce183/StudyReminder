package com.study.reminder.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
    background = Color(0xFFF4FAF9),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F7F6),
    card = Color(0xFFF8FFFE),
    cardSoft = Color(0xFFEDF8F6),
    textPrimary = Color(0xFF0D2F33),
    textSecondary = Color(0xFF4A7A80),
    textTertiary = Color(0xFF8AABAE),
    accent = Accent,
    accentDark = AccentDark,
    accentPink = AccentPink,
    accentBlue = AccentBlue,
    divider = Color(0x0C000000),
    glassStroke = Color(0x30FFFFFF),
    glassFill = Color(0xE6FFFFFF),
    navBar = Color(0xF2FFFFFF),
    infoBg = Color(0xFFE8FFF8),
    infoText = Color(0xFF1A6B60),
    white = Color.White,
    inputBg = Color(0xFFF0F7F6),
    success = Color(0xFF34C759),
    error = Color(0xFFFF3B30)
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

// ── Backward-compatible AppColors object ─────────────────────

object AppColors {
    val Background = Color(0xFFF4FAF9)
    val Surface = Color.White
    val CardBg = Color(0xFFF8FFFE)
    val CardBgSoft = Color(0xFFEDF8F6)
    val CardSoft = Color(0xFFEDF8F6)
    val TextPrimary = Color(0xFF0D2F33)
    val TextSecondary = Color(0xFF4A7A80)
    val TextTertiary = Color(0xFF8AABAE)
    val Accent = Color(0xFF39C5BB)
    val AccentDark = Color(0xFF2AA89F)
    val AccentPink = Color(0xFFFF77B7)
    val AccentBlue = Color(0xFF5FD7FF)
    val GlassStroke = Color(0x30FFFFFF)
    val InfoBg = Color(0xFFE8FFF8)
    val InfoText = Color(0xFF1A6B60)
    val White = Color.White
    val LightDivider = Color(0x0C000000)
    val WeekSelectedBg = Color(0x0C000000)
    val InputBg = Color(0xFFF0F7F6)
    val Success = Color(0xFF34C759)
    val Error = Color(0xFFFF3B30)
    val NavBar = Color(0xF2FFFFFF)
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
    background = Color(0xFFF4FAF9),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F7F6),
    onSurface = Color(0xFF0D2F33),
    onBackground = Color(0xFF0D2F33),
    outline = Color(0xFFD0E8E4),
    outlineVariant = Color(0xFFE8F0EF)
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
    MaterialTheme(
        colorScheme = if (darkTheme) StudyDarkColors else StudyLightColors,
        typography = AppTypography,
        content = content
    )
}
