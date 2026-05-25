package com.swiftpdf.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.domain.document.toReadableFileSize
import com.swiftpdf.app.feature.export.ExportUiState
import com.swiftpdf.app.services.pdf.ImageExportOptions
import com.swiftpdf.app.services.pdf.ImageOutputFormat
import com.swiftpdf.app.services.pdf.SignatureMark
import com.swiftpdf.app.services.pdf.SignaturePlacement
import com.swiftpdf.app.services.pdf.SignaturePoint
import com.swiftpdf.app.services.pdf.SignatureStroke
import com.swiftpdf.app.ui.components.FeatureCard

@Composable
fun ExportScreen(
    document: DocumentItem?,
    state: ExportUiState,
    onImageToPdfClick: () -> Unit,
    onSharePdfClick: () -> Unit,
    onExportImagesClick: (ImageExportOptions) -> Unit,
    onShareImagesClick: () -> Unit,
    onSignPdfClick: (SignatureMark) -> Unit,
    selectedSignatureImageUri: Uri?,
    onImportSignatureImageClick: () -> Unit,
    onClearSignatureImageClick: () -> Unit,
    onCompressPdfClick: () -> Unit,
    onExtractFirstPageClick: () -> Unit,
    onMergePdfsClick: () -> Unit,
    onOpenResultClick: () -> Unit,
    onClearResultClick: () -> Unit,
    onSaveDraftClick: () -> Unit,
) {
    var showProSheet by rememberSaveable { mutableStateOf(false) }
    var showSignatureSheet by rememberSaveable { mutableStateOf(false) }
    var showPdfToImageSheet by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (document == null) {
            item {
                FeatureCard(
                    title = "No active document",
                    body = "Open or scan a PDF first to use document tools. Merge and Image to PDF can start from this page.",
                )
            }
        } else {
            item {
                FeatureCard(
                    title = document.displayName,
                    body = "Current PDF - ${document.sizeBytes.toReadableFileSize()}",
                    actionLabel = "Share PDF",
                    onActionClick = onSharePdfClick,
                )
            }
        }

        if (state.isWorking) {
            item {
                WorkingCard(label = state.activeOperationLabel ?: "Working")
            }
        }
        state.successTitle?.let { title ->
            item {
                ExportSuccessState(
                    title = title,
                    fileName = state.successFileName ?: "SwiftPDF output",
                    detail = state.successDetail ?: state.statusMessage ?: "Your file is ready.",
                    onOpenClick = onOpenResultClick,
                    onShareClick = {
                        if (state.exportedImageSet != null) {
                            onShareImagesClick()
                        } else {
                            onSharePdfClick()
                        }
                    },
                    onDismissClick = onClearResultClick,
                )
            }
        }
        state.errorMessage?.let { message ->
            item {
                ExportRecoveryState(
                    message = message,
                    onSaveDraftClick = onSaveDraftClick,
                    onRetryClick = onClearResultClick,
                )
            }
        }
        state.exportedImageSet?.let { imageSet ->
            item {
                FeatureCard(
                    title = "Exported Images",
                    body = "${imageSet.uris.size} image(s) are ready to share.",
                    actionLabel = "Share Images",
                    onActionClick = onShareImagesClick,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToolGridCard(
                    title = "Image to PDF",
                    body = "3 free exports left",
                    icon = Icons.Outlined.PhotoLibrary,
                    enabled = state.canRunTool,
                    onClick = onImageToPdfClick,
                    modifier = Modifier.weight(1f),
                )
                ToolGridCard(
                    title = "Sign PDF",
                    body = "Draw or import",
                    icon = Icons.Outlined.PictureAsPdf,
                    enabled = document != null && state.canRunTool,
                    onClick = {
                        onClearSignatureImageClick()
                        showSignatureSheet = true
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToolGridCard(
                    title = "Compress",
                    body = "Preview copy",
                    icon = Icons.Outlined.DocumentScanner,
                    enabled = document != null && state.canRunTool,
                    proLabel = "HQ Pro",
                    onClick = onCompressPdfClick,
                    modifier = Modifier.weight(1f),
                )
                ToolGridCard(
                    title = "Merge PDF",
                    body = "Preview merge",
                    icon = Icons.Outlined.FileOpen,
                    enabled = state.canRunTool,
                    onClick = onMergePdfsClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToolGridCard(
                    title = "Split PDF",
                    body = "Page 1 copy",
                    icon = Icons.Outlined.DocumentScanner,
                    enabled = document != null && state.canRunTool,
                    onClick = onExtractFirstPageClick,
                    modifier = Modifier.weight(1f),
                )
                ToolGridCard(
                    title = "PDF to Image",
                    body = "PNG export",
                    icon = Icons.Outlined.PhotoLibrary,
                    enabled = document != null && state.canRunTool,
                    proLabel = "HD Pro",
                    onClick = { showPdfToImageSheet = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            FeatureCard(
                title = "File access explained",
                body = "SwiftPDF works with the local documents you choose and saves edits as new copies.",
            )
        }

        item {
            FeatureCard(
                title = "Pro limits",
                body = "No first-launch paywall. Pro appears only for HD output, batch work, unlimited exports, and saved signature libraries.",
                actionLabel = "View Pro",
                onActionClick = { showProSheet = true },
            )
        }
    }

    if (showSignatureSheet) {
        SignatureDialog(
            selectedSignatureImageUri = selectedSignatureImageUri,
            onDismiss = { showSignatureSheet = false },
            onSignClick = { signatureMark ->
                showSignatureSheet = false
                onSignPdfClick(signatureMark)
            },
            onImportImageClick = onImportSignatureImageClick,
            onClearImageClick = onClearSignatureImageClick,
            onProClick = {
                showSignatureSheet = false
                showProSheet = true
            },
        )
    }
    if (showProSheet) {
        ProDialog(onDismiss = { showProSheet = false })
    }
    if (showPdfToImageSheet) {
        PdfToImageDialog(
            onDismiss = { showPdfToImageSheet = false },
            onExportClick = { options ->
                showPdfToImageSheet = false
                onExportImagesClick(options)
            },
            onProClick = {
                showPdfToImageSheet = false
                showProSheet = true
            },
        )
    }
}

@Composable
private fun ExportSuccessState(
    title: String,
    fileName: String,
    detail: String,
    onOpenClick: () -> Unit,
    onShareClick: () -> Unit,
    onDismissClick: () -> Unit,
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
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(78.dp),
                color = Color(0xFFDCFCE7),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A36C),
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            ResultFileRow(fileName = fileName)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Open")
                }
                Button(
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Share")
                }
            }
            TextButton(onClick = onDismissClick) {
                Text("Back to tools")
            }
        }
    }
}

@Composable
private fun ResultFileRow(fileName: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = Color(0xFFFFE1E5),
                shape = MaterialTheme.shapes.medium,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "PDF",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFF4560),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Saved as a copy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExportRecoveryState(
    message: String,
    onSaveDraftClick: () -> Unit,
    onRetryClick: () -> Unit,
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
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(78.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Export failed",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF7ED),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, Color(0xFFFED7AA)),
            ) {
                Text(
                    text = "Current order and crop settings are preserved so you can continue editing.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9A3412),
                    textAlign = TextAlign.Center,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onSaveDraftClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save draft")
                }
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun WorkingCard(label: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "Please wait while SwiftPDF prepares the output.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ToolGridCard(
    title: String,
    body: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    proLabel: String? = null,
) {
    Card(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.48f)
            .height(118.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                proLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SignatureDialog(
    selectedSignatureImageUri: Uri?,
    onDismiss: () -> Unit,
    onSignClick: (SignatureMark) -> Unit,
    onImportImageClick: () -> Unit,
    onClearImageClick: () -> Unit,
    onProClick: () -> Unit,
) {
    var signatureText by rememberSaveable { mutableStateOf("") }
    var signaturePlacement by rememberSaveable { mutableStateOf(SignaturePlacement.BottomRight.name) }
    var signatureScale by rememberSaveable { mutableStateOf(1.0f) }
    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    var currentStroke by remember { mutableStateOf<List<SignaturePoint>>(emptyList()) }
    val trimmedSignature = signatureText.trim()
    val hasInk = strokes.any { it.points.size >= 2 } || currentStroke.size >= 2
    val signatureMark = SignatureMark(
        strokes = strokes.toList(),
        signerName = trimmedSignature.takeIf { it.isNotEmpty() },
        imageUri = selectedSignatureImageUri?.toString(),
        placement = SignaturePlacement.valueOf(signaturePlacement),
        scale = signatureScale,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Draw or import a signature, then save a signed copy.")
                SignatureCanvas(
                    strokes = strokes,
                    currentStroke = currentStroke,
                    onStrokeChanged = { currentStroke = it },
                    onStrokeFinished = { stroke ->
                        if (stroke.points.size >= 2) {
                            strokes.add(stroke)
                        }
                        currentStroke = emptyList()
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onImportImageClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Import image")
                    }
                    OutlinedButton(
                        enabled = selectedSignatureImageUri != null,
                        onClick = onClearImageClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Remove image")
                    }
                }
                if (selectedSignatureImageUri != null) {
                    Text(
                        text = "Signature image selected.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "Placement",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SignaturePlacement.entries.forEach { placement ->
                        val isSelected = signaturePlacement == placement.name
                        OutlinedButton(
                            onClick = { signaturePlacement = placement.name },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = when (placement) {
                                    SignaturePlacement.BottomRight -> "Right"
                                    SignaturePlacement.Center -> "Center"
                                    SignaturePlacement.BottomLeft -> "Left"
                                },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = "Size ${(signatureScale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = signatureScale,
                        onValueChange = { signatureScale = it },
                        valueRange = SignatureMark.MinScale..SignatureMark.MaxScale,
                        steps = 2,
                    )
                }
                SignaturePlacementPreview(
                    placement = SignaturePlacement.valueOf(signaturePlacement),
                    onPlacementChange = { signaturePlacement = it.name },
                    signerName = trimmedSignature.ifEmpty { "Alex Wu" },
                    scale = signatureScale,
                    hasImage = selectedSignatureImageUri != null,
                )
                OutlinedTextField(
                    value = signatureText,
                    onValueChange = { signatureText = it },
                    singleLine = true,
                    label = { Text("Signer name") },
                )
                Text(
                    text = "Use Clear to redraw. Saved signature libraries remain a Pro option.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = signatureMark.hasContent,
                onClick = { onSignClick(signatureMark) },
            ) {
                Text("Create signed copy")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    enabled = strokes.isNotEmpty() || currentStroke.isNotEmpty(),
                    onClick = {
                        strokes.clear()
                        currentStroke = emptyList()
                    },
                ) {
                    Text("Clear")
                }
                TextButton(onClick = onProClick) {
                    Text("Library")
                }
            }
        },
    )
}

@Composable
private fun SignaturePlacementPreview(
    placement: SignaturePlacement,
    onPlacementChange: (SignaturePlacement) -> Unit,
    signerName: String,
    scale: Float,
    hasImage: Boolean,
) {
    val signatureAlignment = when (placement) {
        SignaturePlacement.BottomRight -> Alignment.BottomEnd
        SignaturePlacement.Center -> Alignment.Center
        SignaturePlacement.BottomLeft -> Alignment.BottomStart
    }
    val signatureWidth = (118.dp * scale).coerceIn(76.dp, 160.dp)
    val signatureHeight = (44.dp * scale).coerceIn(34.dp, 68.dp)
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Place signature",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.medium)
                .onSizeChanged { previewSize = it }
                .pointerInput(previewSize) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onPlacementChange(offset.toSignaturePlacement(previewSize))
                        },
                        onDrag = { change, _ ->
                            onPlacementChange(change.position.toSignaturePlacement(previewSize))
                        },
                    )
                }
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.52f)
                        .height(10.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (it == 3) 0.42f else 0.84f)
                            .height(7.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(signatureAlignment)
                    .size(width = signatureWidth, height = signatureHeight),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (hasImage) "Image sign" else signerName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Text(
            text = "Signed copy keeps the original PDF untouched.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Offset.toSignaturePlacement(size: IntSize): SignaturePlacement {
    if (size.width <= 0) {
        return SignaturePlacement.BottomRight
    }
    val horizontalPosition = (x / size.width).coerceIn(0f, 1f)
    return when {
        horizontalPosition < 0.34f -> SignaturePlacement.BottomLeft
        horizontalPosition < 0.67f -> SignaturePlacement.Center
        else -> SignaturePlacement.BottomRight
    }
}

@Composable
private fun SignatureCanvas(
    strokes: List<SignatureStroke>,
    currentStroke: List<SignaturePoint>,
    onStrokeChanged: (List<SignaturePoint>) -> Unit,
    onStrokeFinished: (SignatureStroke) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val shape = MaterialTheme.shapes.medium
    val borderColor = MaterialTheme.colorScheme.outline
    val backgroundColor = MaterialTheme.colorScheme.surface
    val inkColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                var activePoints = emptyList<SignaturePoint>()
                detectDragGestures(
                    onDragStart = { offset ->
                        activePoints = listOf(offset.toSignaturePoint(canvasSize))
                        onStrokeChanged(activePoints)
                    },
                    onDrag = { change, _ ->
                        activePoints = activePoints + change.position.toSignaturePoint(canvasSize)
                        onStrokeChanged(activePoints)
                    },
                    onDragEnd = {
                        onStrokeFinished(SignatureStroke(activePoints))
                        activePoints = emptyList()
                    },
                    onDragCancel = {
                        activePoints = emptyList()
                        onStrokeChanged(emptyList())
                    },
                )
            },
    ) {
        val allStrokes = strokes + SignatureStroke(currentStroke)
        allStrokes.forEach { stroke ->
            stroke.points.zipWithNext { start, end ->
                drawLine(
                    color = inkColor,
                    start = start.toOffset(size.width, size.height),
                    end = end.toOffset(size.width, size.height),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun Offset.toSignaturePoint(canvasSize: IntSize): SignaturePoint {
    val safeWidth = canvasSize.width.coerceAtLeast(1).toFloat()
    val safeHeight = canvasSize.height.coerceAtLeast(1).toFloat()
    return SignaturePoint(
        x = (x / safeWidth).coerceIn(0f, 1f),
        y = (y / safeHeight).coerceIn(0f, 1f),
    )
}

private fun SignaturePoint.toOffset(width: Float, height: Float): Offset {
    return Offset(
        x = x.coerceIn(0f, 1f) * width,
        y = y.coerceIn(0f, 1f) * height,
    )
}

@Composable
private fun PdfToImageDialog(
    onDismiss: () -> Unit,
    onExportClick: (ImageExportOptions) -> Unit,
    onProClick: () -> Unit,
) {
    var pageRangeText by rememberSaveable { mutableStateOf("") }
    var outputFormat by rememberSaveable { mutableStateOf(ImageOutputFormat.Png.name) }
    val format = ImageOutputFormat.valueOf(outputFormat)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PDF to Image") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Export all pages or enter a range like 1, 3-5.")
                OutlinedTextField(
                    value = pageRangeText,
                    onValueChange = { value ->
                        pageRangeText = value.filter { it.isDigit() || it == ',' || it == '-' || it.isWhitespace() }
                    },
                    singleLine = true,
                    label = { Text("Pages") },
                    placeholder = { Text("All pages") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ImageOutputFormat.entries.forEach { option ->
                        OutlinedButton(
                            onClick = { outputFormat = option.name },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = option.extension.uppercase(),
                                color = if (format == option) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
                Text(
                    text = "Standard resolution is free. HD export is a Pro option.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExportClick(
                        ImageExportOptions(
                            pageRangeText = pageRangeText,
                            outputFormat = format,
                        ),
                    )
                },
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onProClick) {
                    Text("HD Pro")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}

@Composable
private fun ProDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SwiftPDF Pro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Upgrade only when you need more. Your files stay available either way.")
                Text("Unlimited Image to PDF exports")
                Text("HD PDF to Image output")
                Text("Batch compression and saved signature libraries")
                Text(
                    text = "$4.99 / month",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Start Pro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue free")
            }
        },
    )
}
