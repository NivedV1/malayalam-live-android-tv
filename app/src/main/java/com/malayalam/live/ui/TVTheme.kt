package com.malayalam.live.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val MalayalamColors = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF10233F),
    secondary = Color(0xFFC2E7FF),
    onSecondary = Color(0xFF10212B),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFE3EAF4),
    surface = Color(0xFF141B23),
    onSurface = Color(0xFFE3EAF4),
)

@Composable
fun MalayalamLiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MalayalamColors, content = content)
}
