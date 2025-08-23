package com.example.DhoonHub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography

private val DarkColors = darkColorScheme(
    primary = Color(0xFF1DB954), // Spotify green
    secondary = Color(0xFF1ED760),
    tertiary = Color(0xFF40444B),
    background = Color(0xFF0F1115),
    surface = Color(0xFF151923),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE5E5E5),
    onSurface = Color(0xFFE5E5E5)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1DB954),
    secondary = Color(0xFF1ED760),
    tertiary = Color(0xFF40444B)
)

private val AppTypography = Typography()

@Composable
fun MusicAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}