package com.malayalam.live.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val MalayalamColors = darkColorScheme(
    primary = Color(0xFFDCE8D9),
    onPrimary = Color(0xFF174B39),
    secondary = Color(0xFFF08B70),
    onSecondary = Color(0xFF2A1713),
    background = Color(0xFF10251D),
    onBackground = Color(0xFFF5F1E7),
    surface = Color(0xFF19342A),
    onSurface = Color(0xFFF5F1E7),
)

@Composable
fun MalayalamLiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MalayalamColors, content = content)
}
