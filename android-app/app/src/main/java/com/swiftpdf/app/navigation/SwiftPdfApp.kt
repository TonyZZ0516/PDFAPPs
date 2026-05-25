package com.swiftpdf.app.navigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swiftpdf.app.data.document.AndroidDocumentRepository
import com.swiftpdf.app.data.document.DocumentHistoryStore
import com.swiftpdf.app.feature.export.ExportViewModel
import com.swiftpdf.app.feature.library.DocumentLibraryViewModel
import com.swiftpdf.app.feature.reader.ReaderViewModel
import com.swiftpdf.app.feature.scan.ScanViewModel
import com.swiftpdf.app.services.pdf.AndroidImagePdfExportService
import com.swiftpdf.app.services.pdf.AndroidPdfRendererService
import com.swiftpdf.app.services.pdf.AndroidPdfToolService
import com.swiftpdf.app.services.pdf.ExportedPdf
import com.swiftpdf.app.services.share.AndroidShareService
import com.swiftpdf.app.ui.screens.ExportScreen
import com.swiftpdf.app.ui.screens.HomeScreen
import com.swiftpdf.app.ui.screens.ReaderScreen
import com.swiftpdf.app.ui.screens.ScanScreen
import com.swiftpdf.app.ui.screens.SettingsScreen
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwiftPdfApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = bottomNavRoutes.firstOrNull { it.matches(currentDestination) } ?: AppRoute.Home
    var selectedSignatureImageUriText by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSignatureImageUri = selectedSignatureImageUriText?.let(Uri::parse)
    val documentRepository = remember(appContext) {
        AndroidDocumentRepository(appContext)
    }
    val documentHistoryStore = remember(appContext) {
        DocumentHistoryStore(appContext)
    }
    val pdfRendererService = remember(appContext) {
        AndroidPdfRendererService(appContext)
    }
    val imagePdfExportService = remember(appContext) {
        AndroidImagePdfExportService(appContext)
    }
    val pdfToolService = remember(appContext) {
        AndroidPdfToolService(appContext)
    }
    val shareService = remember(context) {
        AndroidShareService(context)
    }
    val documentLibraryViewModel: DocumentLibraryViewModel = viewModel(
        key = "documentLibraryViewModel",
        factory = remember(documentRepository, documentHistoryStore) {
            SwiftPdfViewModelFactory {
                DocumentLibraryViewModel(documentRepository, documentHistoryStore)
            }
        },
    )
    val readerViewModel: ReaderViewModel = viewModel(
        key = "readerViewModel",
        factory = remember(pdfRendererService) {
            SwiftPdfViewModelFactory {
                ReaderViewModel(pdfRendererService)
            }
        },
    )
    val scanViewModel: ScanViewModel = viewModel(
        key = "scanViewModel",
        factory = remember(imagePdfExportService) {
            SwiftPdfViewModelFactory {
                ScanViewModel(imagePdfExportService)
            }
        },
    )
    val exportViewModel: ExportViewModel = viewModel(
        key = "exportViewModel",
        factory = remember(pdfToolService) {
            SwiftPdfViewModelFactory {
                ExportViewModel(pdfToolService)
            }
        },
    )
    val documentLibraryState by documentLibraryViewModel.uiState.collectAsState()
    val readerState by readerViewModel.uiState.collectAsState()
    val scanState by scanViewModel.uiState.collectAsState()
    val exportState by exportViewModel.uiState.collectAsState()
    val topTitle = when (currentRoute) {
        AppRoute.Home -> "SwiftPDF"
        AppRoute.Reader -> documentLibraryState.selectedDocument?.displayName ?: "PDF Reader"
        AppRoute.Scan -> "Image to PDF"
        AppRoute.Export -> "PDF Toolbox"
        AppRoute.Settings -> "Settings"
    }
    val topSubtitle = when (currentRoute) {
        AppRoute.Home -> "Recent documents"
        AppRoute.Reader -> if (readerState.pageCount > 0) {
            "Page ${readerState.currentPageNumber} of ${readerState.pageCount}"
        } else {
            "Open local PDF"
        }
        AppRoute.Scan -> if (scanState.captureCount == 0) {
            "Step 1 of 4 - add pages"
        } else {
            "Step 2 of 4 - reorder pages"
        }
        AppRoute.Export -> "Common tasks, no clutter"
        AppRoute.Settings -> "Preferences"
    }
    val openGeneratedPdf: (ExportedPdf) -> Unit = { exportedPdf ->
        documentLibraryViewModel.registerGeneratedPdf(
            uri = exportedPdf.uri,
            displayName = exportedPdf.displayName,
            sizeBytes = exportedPdf.sizeBytes,
        )
        navController.navigate(AppRoute.Reader.route) {
            launchSingleTop = true
        }
    }
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        documentLibraryViewModel.importPdf(uri)
        navController.navigate(AppRoute.Reader.route) {
            launchSingleTop = true
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val images = uris.map { uri ->
            takeReadPermission(context, uri)
            uri to readDisplayName(context, uri)
        }
        scanViewModel.recordImportedImages(images)
        navController.navigate(AppRoute.Scan.route) {
            launchSingleTop = true
        }
    }
    val signatureImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        selectedSignatureImageUriText = uri?.toString()
    }
    val mergePdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val documents = uris.map { uri ->
            documentRepository.importPdf(uri)
        }
        exportViewModel.mergePdfs(documents, openGeneratedPdf)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        scanViewModel.updateCameraPermission(isGranted)
    }
    val requestCameraPermission = {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            scanViewModel.updateCameraPermission(true)
        } else {
            scanViewModel.markPermissionUnknown()
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    val navigateToBottomRoute: (AppRoute) -> Unit = navigate@ { route ->
        if (route.matches(currentDestination)) return@navigate
        if (route == AppRoute.Home) {
            val popped = navController.popBackStack(AppRoute.Home.route, inclusive = false)
            if (!popped) {
                navController.navigate(AppRoute.Home.route) {
                    launchSingleTop = true
                }
            }
        } else {
            navController.navigate(route.route) {
                popUpTo(AppRoute.Home.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "PDF",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text(
                                text = topTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                text = topSubtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                actions = {
                    when (currentRoute) {
                        AppRoute.Export -> {
                            Surface(
                                modifier = Modifier.padding(end = 16.dp),
                                color = Color(0xFFFFF7ED),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    text = "PRO",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFEA580C),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        else -> {
                            val actionIcon = when (currentRoute) {
                                AppRoute.Home -> Icons.Outlined.AutoAwesome
                                AppRoute.Reader -> Icons.Outlined.MoreVert
                                AppRoute.Scan -> Icons.Outlined.CameraAlt
                                AppRoute.Settings -> Icons.Outlined.FileOpen
                                AppRoute.Export -> Icons.Outlined.AutoAwesome
                            }
                            val actionDescription = when (currentRoute) {
                                AppRoute.Home -> "Pro"
                                AppRoute.Reader -> "Reader tools"
                                AppRoute.Scan -> "Camera"
                                AppRoute.Settings -> "Open PDF"
                                AppRoute.Export -> "Pro"
                            }
                            Surface(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(40.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                IconButton(
                                    onClick = {
                                        when (currentRoute) {
                                            AppRoute.Home -> navigateToBottomRoute(AppRoute.Export)
                                            AppRoute.Reader -> navigateToBottomRoute(AppRoute.Export)
                                            AppRoute.Scan -> requestCameraPermission()
                                            AppRoute.Settings -> pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                            AppRoute.Export -> Unit
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = actionIcon,
                                        contentDescription = actionDescription,
                                        tint = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            SwiftPdfBottomBar(
                currentDestination = currentDestination,
                onRouteClick = navigateToBottomRoute,
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppRoute.Home.route) {
                HomeScreen(
                    recentDocuments = documentLibraryState.recentDocuments,
                    onImportPdfClick = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onScanClick = {
                        navigateToBottomRoute(AppRoute.Scan)
                    },
                    onExportClick = {
                        navigateToBottomRoute(AppRoute.Export)
                    },
                    onDocumentClick = { document ->
                        documentLibraryViewModel.selectDocument(document)
                        navController.navigate(AppRoute.Reader.route) {
                            launchSingleTop = true
                        }
                    },
                    onDocumentToolsClick = { document ->
                        documentLibraryViewModel.selectDocument(document)
                        navigateToBottomRoute(AppRoute.Export)
                    },
                    onDocumentShareClick = { document ->
                        shareService.sharePdf(document)
                    },
                    onRenameDocumentClick = documentLibraryViewModel::renameRecentDocument,
                    onRemoveDocumentClick = documentLibraryViewModel::removeRecentDocument,
                )
            }
            composable(AppRoute.Reader.route) {
                val selectedDocument = documentLibraryState.selectedDocument
                LaunchedEffect(selectedDocument?.uri) {
                    readerViewModel.openDocument(selectedDocument)
                }
                ReaderScreen(
                    state = readerState,
                    onImportPdfClick = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onSharePdfClick = {
                        readerState.document?.let { document ->
                            shareService.sharePdf(document)
                        }
                    },
                    onBookmarkClick = readerViewModel::toggleBookmarkForCurrentPage,
                    onPageVisible = readerViewModel::showPage,
                    onPageRenderRequested = readerViewModel::renderPageIfNeeded,
                    onNightModeClick = readerViewModel::toggleNightMode,
                    onSignClick = {
                        navigateToBottomRoute(AppRoute.Export)
                    },
                )
            }
            composable(AppRoute.Scan.route) {
                ScanScreen(
                    state = scanState,
                    onRequestCameraPermissionClick = requestCameraPermission,
                    onImportImagesClick = {
                        imagePickerLauncher.launch(arrayOf("image/*"))
                    },
                    onCapturePageClick = scanViewModel::requestCapture,
                    onCaptureSaved = scanViewModel::recordCapturedPage,
                    onCaptureError = scanViewModel::recordCaptureError,
                    onRemoveCapturedPageClick = scanViewModel::removeCapturedPage,
                    onRetakeCapturedPageClick = scanViewModel::retakeCapturedPage,
                    onRotateCapturedPageClick = scanViewModel::rotateCapturedPage,
                    onAutoCropAllPagesClick = scanViewModel::toggleAutoCropForAllPages,
                    onMoveCapturedPageUpClick = scanViewModel::moveCapturedPageUp,
                    onMoveCapturedPageDownClick = scanViewModel::moveCapturedPageDown,
                    onExportPdfClick = {
                        scanViewModel.exportScanToPdf { exportedPdf ->
                            openGeneratedPdf(exportedPdf)
                        }
                    },
                )
            }
            composable(AppRoute.Export.route) {
                ExportScreen(
                    document = documentLibraryState.selectedDocument,
                    state = exportState,
                    onImageToPdfClick = {
                        navigateToBottomRoute(AppRoute.Scan)
                    },
                    onSharePdfClick = {
                        documentLibraryState.selectedDocument?.let { document ->
                            shareService.sharePdf(document)
                        }
                    },
                    onExportImagesClick = { options ->
                        exportViewModel.exportImages(documentLibraryState.selectedDocument, options)
                    },
                    onShareImagesClick = {
                        exportState.exportedImageSet?.let { imageSet ->
                            shareService.shareImages(imageSet)
                        }
                    },
                    onSignPdfClick = { signatureMark ->
                        exportViewModel.signPdf(
                            documentLibraryState.selectedDocument,
                            signatureMark,
                            openGeneratedPdf,
                        )
                    },
                    selectedSignatureImageUri = selectedSignatureImageUri,
                    onImportSignatureImageClick = {
                        signatureImagePickerLauncher.launch("image/*")
                    },
                    onClearSignatureImageClick = {
                        selectedSignatureImageUriText = null
                    },
                    onCompressPdfClick = {
                        exportViewModel.compressPdf(
                            documentLibraryState.selectedDocument,
                            openGeneratedPdf,
                        )
                    },
                    onExtractFirstPageClick = {
                        exportViewModel.extractFirstPage(
                            documentLibraryState.selectedDocument,
                            openGeneratedPdf,
                        )
                    },
                    onMergePdfsClick = {
                        mergePdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onOpenResultClick = {
                        navigateToBottomRoute(AppRoute.Reader)
                    },
                    onClearResultClick = exportViewModel::clearResultMessage,
                    onSaveDraftClick = exportViewModel::saveErrorDraft,
                )
            }
            composable(AppRoute.Settings.route) {
                SettingsScreen(
                    recentDocumentCount = documentLibraryState.recentDocuments.size,
                    onClearRecentDocumentsClick = documentLibraryViewModel::clearRecentDocuments,
                )
            }
        }
    }
}

@Composable
private fun SwiftPdfBottomBar(
    currentDestination: NavDestination?,
    onRouteClick: (AppRoute) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomNavRoutes.forEach { route ->
                val selected = route.matches(currentDestination)
                val color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable(
                            role = Role.Tab,
                            onClick = { onRouteClick(route) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            imageVector = route.icon,
                            contentDescription = route.label,
                            tint = color,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                    Text(
                        text = route.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                    )
                }
            }
        }
    }
}

private fun AppRoute.matches(destination: NavDestination?): Boolean {
    return destination?.hierarchy?.any { it.route == route } == true
}

private class SwiftPdfViewModelFactory<VM : ViewModel>(
    private val createViewModel: () -> VM,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return createViewModel() as T
    }
}

private fun takeReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}

private fun readDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                return cursor.getString(nameIndex)
            }
        }
    }
    return uri.lastPathSegment ?: "Imported image"
}
