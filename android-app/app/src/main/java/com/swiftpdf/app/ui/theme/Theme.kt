package com.swiftpdf.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SwiftPdfColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Panel,
    primaryContainer = PanelSoft,
    onPrimaryContainer = PrimaryTealDark,
    secondary = AccentBlue,
    onSecondary = Panel,
    secondaryContainer = AccentBlueSoft,
    onSecondaryContainer = Ink,
    tertiary = SuccessGreen,
    onTertiary = Panel,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Panel,
    surfaceContainerLowest = Panel,
    surfaceContainerLow = Panel,
    surfaceContainer = Panel,
    surfaceContainerHigh = Panel,
    surfaceContainerHighest = Panel,
    onSurface = Ink,
    surfaceVariant = PanelSoft,
    onSurfaceVariant = MutedInk,
    outline = Rule,
    outlineVariant = Rule,
)

@Composable
fun SwiftPdfTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SwiftPdfColorScheme,
        typography = SwiftPdfTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(8.dp),
        ),
        content = content,
    )
}
