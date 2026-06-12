package com.dentia.patient.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DentiaColorScheme = lightColorScheme(
    primary = DentiaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F0F5),
    onPrimaryContainer = DentiaPrimaryDark,
    secondary = DentiaAccent,
    onSecondary = DentiaText,
    background = DentiaBackground,
    onBackground = DentiaText,
    surface = DentiaSurface,
    onSurface = DentiaText,
    surfaceVariant = Color(0xFFF0F5F8),
    onSurfaceVariant = DentiaMuted,
    outline = DentiaBorder,
    error = Color(0xFFDC2626),
)

@Composable
fun DentiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DentiaColorScheme,
        typography = DentiaTypography,
        content = content,
    )
}

