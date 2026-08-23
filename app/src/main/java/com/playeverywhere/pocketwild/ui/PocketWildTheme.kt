package com.playeverywhere.pocketwild.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PocketColors = lightColorScheme(
    primary = Color(0xFF2F6B5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EEE5),
    onPrimaryContainer = Color(0xFF143D35),
    secondary = Color(0xFFFF8B5E),
    onSecondary = Color(0xFF4A1908),
    tertiary = Color(0xFF9B8AE6),
    background = Color(0xFFF4F1E8),
    surface = Color(0xFFFFFBF4),
    onSurface = Color(0xFF253239),
    surfaceVariant = Color(0xFFE5E9E2),
    outline = Color(0xFF75807B)
)

@Composable
fun PocketWildTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PocketColors, content = content)
}
