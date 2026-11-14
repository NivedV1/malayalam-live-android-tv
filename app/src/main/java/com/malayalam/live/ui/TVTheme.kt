package com.malayalam.live.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val MalayalamColors = darkColorScheme(
    primary = Color(0xFFD8EF83),
    onPrimary = Color(0xFF132018),
    secondary = Color(0xFFFF9B75),
    onSecondary = Color(0xFF25140F),
    background = Color(0xFF091712),
    onBackground = Color(0xFFF4F5EF),
    surface = Color(0xFF12251E),
    onSurface = Color(0xFFF4F5EF),
)

@Composable
fun MalayalamLiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MalayalamColors, content = content)
}
