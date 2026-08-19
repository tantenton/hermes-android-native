package com.hermes.terminal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = NeonCyan,
    tertiary = NeonPurple,
    background = TerminalBackground,
    surface = TerminalSurface,
    onPrimary = TerminalBackground,
    onSecondary = TerminalBackground,
    onTertiary = TerminalBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = TerminalBorder
)

@Composable
fun HermesTerminalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
