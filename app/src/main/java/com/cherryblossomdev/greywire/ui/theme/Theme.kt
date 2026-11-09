package com.cherryblossomdev.greywire.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Greywire is dark-only, like the website, and doesn't use Android 12 dynamic
// color: the brand palette is fixed (unlike the Prosaurus app's BreakroomTheme).
private val GreywireColorScheme = darkColorScheme(
    primary = GreywireCopper,
    onPrimary = GreywireOnCopper,
    primaryContainer = GreywireSurface2,
    onPrimaryContainer = GreywireText,
    secondary = GreywireCopperSoft,
    onSecondary = GreywireOnCopper,
    tertiary = GreywireSignal,
    background = GreywireBg,
    onBackground = GreywireText,
    surface = GreywireBg,
    onSurface = GreywireText,
    surfaceVariant = GreywireSurface,
    onSurfaceVariant = GreywireMuted,
    surfaceContainer = GreywireSurface,
    surfaceContainerHigh = GreywireSurface2,
    outline = GreywireLine,
    outlineVariant = GreywireLine,
    error = GreywireError,
    onError = GreywireBg
)

@Composable
fun GreywireTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GreywireColorScheme,
        typography = Typography,
        content = content
    )
}
