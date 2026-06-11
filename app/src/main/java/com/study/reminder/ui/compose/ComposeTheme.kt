package com.study.reminder.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudyLightColors = lightColorScheme(
    primary = Color(0xFF63D2D6),
    onPrimary = Color.White,
    secondary = Color(0xFFFF82B2),
    tertiary = Color(0xFF7AD7FF),
    background = Color(0xFFEAFBFC),
    surface = Color(0xF5F7FFFF),
    onSurface = Color(0xFF12393E),
    onBackground = Color(0xFF12393E),
    outline = Color(0x8063D2D6)
)

private val StudyDarkColors = darkColorScheme(
    primary = Color(0xFF63D2D6),
    onPrimary = Color(0xFF062227),
    secondary = Color(0xFFFF82B2),
    tertiary = Color(0xFF7AD7FF),
    background = Color(0xFF08161B),
    surface = Color(0xE61A2D33),
    onSurface = Color(0xFFE8FFFE),
    onBackground = Color(0xFFE8FFFE),
    outline = Color(0x8063D2D6)
)

object AppColors {
    val Background = Color(0xFFEAFBF8)
    val CardBg = Color(0xCCF7FFFF)
    val CardBgSoft = Color(0xDDF9F4)
    val TextPrimary = Color(0xFF10393F)
    val TextSecondary = Color(0xFF4E7F84)
    val Accent = Color(0xFF39C5BB)
    val AccentPink = Color(0xFFFF77B7)
    val GlassStroke = Color(0x90FFFFFF)
    val InfoBg = Color(0xE8FFFA)
    val InfoText = Color(0xFF17646B)
    val White = Color.White
    val LightDivider = Color(0x15000000)
    val WeekSelectedBg = Color(0x15000000)
}

@Composable
fun StudyReminderComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) StudyDarkColors else StudyLightColors,
        content = content
    )
}
