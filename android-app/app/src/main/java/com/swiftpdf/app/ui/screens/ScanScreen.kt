package com.swiftpdf.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swiftpdf.app.feature.scan.CameraPermissionStatus
import com.swiftpdf.app.feature.scan.CapturedScanPage
import com.swiftpdf.app.feature.scan.ScanUiState
import com.swiftpdf.app.ui.components.CameraXPreview

@Composable
fun ScanScreen(
    state: ScanUiState,
    onRequestCameraPermissionClick: () -> Unit,
    onImportImagesClick: () -> Unit,
    onCapturePageClick: () -> Unit,
    onCaptureSaved: (Uri, String) -> Unit,
    onCaptureError: (String) -> Unit,
    onRemoveCapturedPageClick: (CapturedScanPage) -> Unit,
    onRetakeCapturedPageClick: (CapturedScanPage) -> Unit,
    onRotateCapturedPageClick: (CapturedScanPage) -> Unit,
    onAutoCropAllPagesClick: () -> Unit,
    onMoveCapturedPageUpClick: (CapturedScanPage) -> Unit,
    onMoveCapturedPageDownClick: (CapturedScanPage) -> Unit,
    onExportPdfClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 8.dp,
            end = 20.dp,
            bottom = 108.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScanStepCard(
                pageCount = state.captureCount,
                onImportImagesClick = onImportImagesClick,
                onRequestCameraPermissionClick = onRequestCameraPermissionClick,
                cameraPermissionStatus = state.cameraPermissionStatus,
            )
        }

        when (state.cameraPermissionStatus) {
            CameraPermissionStatus.Granted -> {
                item {
                    CameraPreviewCard(
                        state = state,
                        onCapturePageClick = onCapturePageClick,
                        onImportImagesClick = onImportImagesClick,
                        onCaptureSaved = onCaptureSaved,
                        onCaptureError = onCaptureError,
                    )
                }
            }
            CameraPermissionStatus.Denied -> {
                item {
                    PermissionCard(
                        title = "Camera permission needed",
                        body = "SwiftPDF can still import images. Enable camera access when you want to scan paper pages.",
                        onRequestCameraPermissionClick = onRequestCameraPermissionClick,
                    )
                }
            }
            CameraPermissionStatus.Unknown -> {
                item {
                    PermissionCard(
                        title = "Allow camera when needed",
                        body = "SwiftPDF reads only the PDFs and images you select. Camera access is used only while scanning.",
                        onRequestCameraPermissionClick = onRequestCameraPermissionClick,
                    )
                }
            }
        }

        state.captureErrorMessage?.let { message ->
            item {
                NoticeCard(
                    title = "Capture failed",
                    body = message,
                    tone = NoticeTone.Error,
                )
            }
        }
        state.exportErrorMessage?.let { message ->
            item {
                NoticeCard(
                    title = "Export failed",
                    body = message,
                    tone = NoticeTone.Error,
                )
            }
        }

        if (state.capturedPages.isEmpty()) {
            item {
                EmptyImageQueue(onImportImagesClick = onImportImagesClick)
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Selected images",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onImportImagesClick) {
                        Text("Add")
                    }
                }
            }
            item {
                CapturedPageGrid(
                    pages = state.capturedPages,
                    onRemoveCapturedPageClick = onRemoveCapturedPageClick,
                    onRetakeCapturedPageClick = onRetakeCapturedPageClick,
                    onRotateCapturedPageClick = onRotateCapturedPageClick,
                    onMoveCapturedPageUpClick = onMoveCapturedPageUpClick,
                    onMoveCapturedPageDownClick = onMoveCapturedPageDownClick,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onAutoCropAllPagesClick,
                        enabled = state.capturedPages.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Crop,
                            contentDescription = null,
                        )
                        Text(if (state.capturedPages.all { it.isAutoCropped }) "Crop on" else "Auto crop")
                    }
                    Button(
                        onClick = onExportPdfClick,
                        enabled = state.canExport,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.isExporting) "Exporting..." else "Preview")
                    }
                }
            }
            item {
                NoticeCard(
                    title = "Save as copy",
                    body = "Reorder, rotate, retake, delete, and export pages as a new PDF.",
                    tone = NoticeTone.Info,
                )
            }
        }
    }
}

@Composable
private fun ScanStepCard(
    pageCount: Int,
    onImportImagesClick: () -> Unit,
    onRequestCameraPermissionClick: () -> Unit,
    cameraPermissionStatus: CameraPermissionStatus,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Image to PDF",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (pageCount == 0) {
                            "Step 1 of 4 - add pages"
                        } else {
                            "Step 2 of 4 - reorder pages"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            ProgressSteps(activeCount = if (pageCount == 0) 1 else 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onImportImagesClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Add images")
                }
                FilledTonalButton(
                    onClick = onRequestCameraPermissionClick,
                    enabled = cameraPermissionStatus != CameraPermissionStatus.Granted,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (cameraPermissionStatus == CameraPermissionStatus.Granted) "Camera ready" else "Camera")
                }
            }
        }
    }
}

@Composable
private fun ProgressSteps(activeCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (index < activeCount) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun CameraPreviewCard(
    state: ScanUiState,
    onCapturePageClick: () -> Unit,
    onImportImagesClick: () -> Unit,
    onCaptureSaved: (Uri, String) -> Unit,
    onCaptureError: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Live camera",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            CameraXPreview(
                captureRequestCount = state.captureRequestCount,
                onCaptureSaved = onCaptureSaved,
                onCaptureError = onCaptureError,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onCapturePageClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Capture")
                }
                FilledTonalButton(
                    onClick = onImportImagesClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                    )
                    Text("Images")
                }
            }
        }
    }
}

@Composable
private fun EmptyImageQueue(onImportImagesClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable(onClick = onImportImagesClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Text(
                text = "No images selected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = "Import photos or scan pages to create a clean PDF copy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun CapturedPageGrid(
    pages: List<CapturedScanPage>,
    onRemoveCapturedPageClick: (CapturedScanPage) -> Unit,
    onRetakeCapturedPageClick: (CapturedScanPage) -> Unit,
    onRotateCapturedPageClick: (CapturedScanPage) -> Unit,
    onMoveCapturedPageUpClick: (CapturedScanPage) -> Unit,
    onMoveCapturedPageDownClick: (CapturedScanPage) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pages.chunked(2).forEachIndexed { rowIndex, rowPages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowPages.forEachIndexed { columnIndex, page ->
                    val index = rowIndex * 2 + columnIndex
                    CapturedPageTile(
                        page = page,
                        pageNumber = index + 1,
                        canMoveUp = index > 0,
                        canMoveDown = index < pages.lastIndex,
                        onRemoveClick = { onRemoveCapturedPageClick(page) },
                        onRetakeClick = { onRetakeCapturedPageClick(page) },
                        onRotateClick = { onRotateCapturedPageClick(page) },
                        onMoveUpClick = { onMoveCapturedPageUpClick(page) },
                        onMoveDownClick = { onMoveCapturedPageDownClick(page) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowPages.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CapturedPageTile(
    page: CapturedScanPage,
    pageNumber: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemoveClick: () -> Unit,
    onRetakeClick: () -> Unit,
    onRotateClick: () -> Unit,
    onMoveUpClick: () -> Unit,
    onMoveDownClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / 1.25f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Surface(
                    modifier = Modifier
                        .padding(9.dp)
                        .size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = pageNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Text(
                text = if (page.isAutoCropped) "${page.displayName} - cropped" else page.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onMoveUpClick,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Move page up",
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onMoveDownClick,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Move page down",
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onRotateClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                        contentDescription = "Rotate page",
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onRetakeClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Retake page",
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove captured page",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    onRequestCameraPermissionClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onRequestCameraPermissionClick) {
                Text("Allow")
            }
        }
    }
}

private enum class NoticeTone {
    Info,
    Error,
}

@Composable
private fun NoticeCard(
    title: String,
    body: String,
    tone: NoticeTone,
) {
    val container = if (tone == NoticeTone.Error) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val content = if (tone == NoticeTone.Error) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.labelMedium,
                color = content,
            )
        }
    }
}
