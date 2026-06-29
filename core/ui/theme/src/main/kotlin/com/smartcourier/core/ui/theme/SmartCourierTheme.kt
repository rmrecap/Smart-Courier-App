package com.smartcourier.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ColorTokens.DeliveryOrange,
    onPrimary = ColorTokens.TextPrimary,
    primaryContainer = ColorTokens.DeliveryOrange.copy(alpha = 0.15f),
    secondary = ColorTokens.Success,
    background = ColorTokens.CyberDark,
    surface = ColorTokens.SurfaceDark,
    surfaceVariant = ColorTokens.CardDark,
    onBackground = ColorTokens.TextPrimary,
    onSurface = ColorTokens.TextPrimary,
    onSurfaceVariant = ColorTokens.TextSecondary,
    outline = ColorTokens.Divider,
    error = ColorTokens.Error
)

@Composable
fun SmartCourierTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SmartCourierTypography,
        shapes = SmartCourierShapes,
        content = content
    )
}
