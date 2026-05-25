package com.swiftpdf.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppRoute(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : AppRoute("home", "Home", Icons.Outlined.Home)
    data object Reader : AppRoute("reader", "Reader", Icons.Outlined.PictureAsPdf)
    data object Scan : AppRoute("scan", "Scan", Icons.Outlined.DocumentScanner)
    data object Export : AppRoute("export", "Export", Icons.Outlined.FileOpen)
    data object Settings : AppRoute("settings", "Settings", Icons.Outlined.Settings)
}

val bottomNavRoutes = listOf(
    AppRoute.Home,
    AppRoute.Reader,
    AppRoute.Scan,
    AppRoute.Export,
    AppRoute.Settings,
)
