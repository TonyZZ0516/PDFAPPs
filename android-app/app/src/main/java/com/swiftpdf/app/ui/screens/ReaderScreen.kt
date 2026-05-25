package com.swiftpdf.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swiftpdf.app.domain.document.toReadableFileSize
import com.swiftpdf.app.feature.reader.ReaderUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onImportPdfClick: () -> Unit,
    onSharePdfClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onPageVisible: (Int) -> Unit,
    onPageRenderRequested: (Int) -> Unit,
    onNightModeClick: () -> Unit,
    onSignClick: () -> Unit,
) {
    val document = state.document
    var showSearchDialog by remember { mutableStateOf(false) }
    var showAnnotateDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(document?.uri) {
        if (document != null) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(document?.uri, state.pageCount) {
        if (document != null && state.pageCount > 0) {
            snapshotFlow { listState.firstVisibleItemIndex - ReaderPageListStartIndex }
                .filter { pageIndex -> pageIndex in 0 until state.pageCount }
                .distinctUntilChanged()
                .collect { pageIndex -> onPageVisible(pageIndex) }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 8.dp,
            end = 20.dp,
            bottom = 108.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (document == null) {
            item {
                EmptyReaderState(onImportPdfClick = onImportPdfClick)
            }
        } else {
            when {
                state.isLoading -> {
                    item {
                        ReaderMessageCard(
                            title = "Rendering preview",
                            body = "Opening the first page of this PDF.",
                            showProgress = true,
                        )
                    }
                }
                state.errorMessage != null -> {
                    item {
                        ReaderMessageCard(
                            title = "Preview unavailable",
                            body = state.errorMessage,
                            showProgress = false,
                        )
                    }
                }
                state.pageCount > 0 -> {
                    item {
                        ReaderControlsPanel(
                            state = state,
                            onSearchClick = { showSearchDialog = true },
                            onBookmarkClick = onBookmarkClick,
                            onSignClick = onSignClick,
                            onNightModeClick = onNightModeClick,
                            onAnnotateClick = { showAnnotateDialog = true },
                            onSharePdfClick = onSharePdfClick,
                        )
                    }
                    items(
                        count = state.pageCount,
                        key = { pageIndex -> "reader-page-$pageIndex" },
                    ) { pageIndex ->
                        LaunchedEffect(document.uri, pageIndex) {
                            onPageRenderRequested(pageIndex)
                        }
                        ContinuousPagePreview(
                            state = state,
                            pageIndex = pageIndex,
                        )
                    }
                }
            }
        }
    }

    if (showSearchDialog && document != null) {
        ReaderSearchDialog(
            documentTitle = document.displayName,
            currentPageNumber = state.currentPageNumber,
            bookmarkedPageNumbers = state.bookmarkedPageNumbers,
            onDismiss = { showSearchDialog = false },
        )
    }
    if (showAnnotateDialog) {
        AnnotationDialog(onDismiss = { showAnnotateDialog = false })
    }
}

private const val ReaderPageListStartIndex = 1

@Composable
private fun EmptyReaderState(onImportPdfClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No local PDF selected",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Import a PDF to read, zoom, share, and send it to the toolbox.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onImportPdfClick,
                modifier = Modifier.width(220.dp),
            ) {
                Text("Import PDF")
            }
        }
    }
}

@Composable
private fun DocumentSummaryCard(
    title: String,
    subtitle: String,
    onOpenClick: () -> Unit,
    onShareClick: () -> Unit,
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
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PdfBadge()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onOpenClick) {
                Icon(
                    imageVector = Icons.Outlined.FileOpen,
                    contentDescription = "Open PDF",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share PDF",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PdfBadge() {
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
}

@Composable
private fun ReaderMessageCard(
    title: String,
    body: String,
    showProgress: Boolean,
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showProgress) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            } else {
                PdfBadge()
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReaderControlsPanel(
    state: ReaderUiState,
    onSearchClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onSignClick: () -> Unit,
    onNightModeClick: () -> Unit,
    onAnnotateClick: () -> Unit,
    onSharePdfClick: () -> Unit,
) {
    ReaderToolDock(
        state = state,
        onSearchClick = onSearchClick,
        onBookmarkClick = onBookmarkClick,
        onSignClick = onSignClick,
        onNightModeClick = onNightModeClick,
        onAnnotateClick = onAnnotateClick,
        onSharePdfClick = onSharePdfClick,
    )
}

@Composable
private fun ContinuousPagePreview(
    state: ReaderUiState,
    pageIndex: Int,
) {
    val preview = state.pagePreviews[pageIndex]
    if (preview == null) {
        PageLoadingPlaceholder(
            pageNumber = pageIndex + 1,
            isRendering = state.renderingPageIndexes.contains(pageIndex),
        )
        return
    }
    val bitmap = preview.pageBitmap
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val horizontalScrollState = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                Text(
                    text = "Page ${pageIndex + 1} of ${state.pageCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${(state.zoomLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState),
            ) {
                val pageWidth = maxWidth * state.zoomLevel
                val pageHeight = pageWidth / aspectRatio

                Box(
                    modifier = Modifier
                        .width(pageWidth)
                        .height(pageHeight)
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (state.isNightMode) Color(0xFF111827) else MaterialTheme.colorScheme.surface),
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF page preview",
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (state.isNightMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.18f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageLoadingPlaceholder(
    pageNumber: Int,
    isRendering: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isRendering) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = "Page $pageNumber",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (isRendering) "Rendering page preview" else "Waiting to render",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReaderProgress(
    state: ReaderUiState,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
) {
    val progress = if (state.pageCount > 0) {
        state.currentPageNumber.toFloat() / state.pageCount.toFloat()
    } else {
        0f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPreviousPageClick,
            enabled = state.canGoPrevious,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Previous page",
            )
        }
        Text(
            text = state.currentPageNumber.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            text = state.pageCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = onNextPageClick,
            enabled = state.canGoNext,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Next page",
            )
        }
    }
}

@Composable
private fun ReaderToolDock(
    state: ReaderUiState,
    onSearchClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onSignClick: () -> Unit,
    onNightModeClick: () -> Unit,
    onAnnotateClick: () -> Unit,
    onSharePdfClick: () -> Unit,
) {
    val tools = listOf(
        ReaderTool(Icons.Outlined.Search, "Search", onSearchClick, false),
        ReaderTool(
            icon = if (state.isCurrentPageBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
            label = "Bookmark",
            onClick = onBookmarkClick,
            selected = state.isCurrentPageBookmarked,
        ),
        ReaderTool(Icons.Outlined.Draw, "Sign", onSignClick, false),
        ReaderTool(Icons.Outlined.DarkMode, "Night", onNightModeClick, state.isNightMode),
        ReaderTool(Icons.Outlined.EditNote, "Note", onAnnotateClick, false),
        ReaderTool(Icons.Outlined.Share, "Share", onSharePdfClick, false),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tools.forEach { tool ->
            FilledTonalButton(
                onClick = tool.onClick,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.label,
                    modifier = Modifier.size(if (tool.selected) 22.dp else 20.dp),
                    tint = if (tool.selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                )
            }
        }
    }
}

private data class ReaderTool(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val selected: Boolean,
)

@Composable
private fun ReaderSearchDialog(
    documentTitle: String,
    currentPageNumber: Int,
    bookmarkedPageNumbers: Set<Int>,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val resultText = when {
        trimmedQuery.isBlank() -> "Search the current file name and saved page bookmarks."
        documentTitle.contains(trimmedQuery, ignoreCase = true) ->
            "Found a match in the file name. You are on page $currentPageNumber."
        bookmarkedPageNumbers.any { it.toString() == trimmedQuery } ->
            "Found bookmarked page $trimmedQuery."
        else ->
            "No local metadata match. Full PDF text extraction is planned for the OCR/text layer."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Keyword or page bookmark") },
                )
                Text(
                    text = resultText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun AnnotationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Basic signature copies are available now.")
                Text(
                    text = "Highlight, pen, and text annotations are preserved as a scoped next module so PDF edits stay copy-safe.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun ZoomControls(
    state: ReaderUiState,
    onZoomOutClick: () -> Unit,
    onResetZoomClick: () -> Unit,
    onZoomInClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onZoomOutClick,
            enabled = state.canZoomOut,
            modifier = Modifier.weight(1f),
        ) {
            Text("Zoom -")
        }
        Button(
            onClick = onResetZoomClick,
            enabled = state.zoomLevel != 1.0f,
            modifier = Modifier.weight(1f),
        ) {
            Text("Reset")
        }
        OutlinedButton(
            onClick = onZoomInClick,
            enabled = state.canZoomIn,
            modifier = Modifier.weight(1f),
        ) {
            Text("Zoom +")
        }
    }
}

@Composable
private fun PageJumpControl(
    state: ReaderUiState,
    onPageJumpClick: (Int) -> Unit,
) {
    var pageInput by remember(state.document?.uri, state.pageCount) {
        mutableStateOf(state.currentPageNumber.toString())
    }

    LaunchedEffect(state.currentPageNumber, state.pageCount, state.isLoading) {
        if (!state.isLoading && state.pageCount > 0) {
            pageInput = state.currentPageNumber.toString()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = pageInput,
            onValueChange = { value ->
                pageInput = value.filter { it.isDigit() }.take(4)
            },
            label = { Text("Page") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = {
                pageInput.toIntOrNull()?.let(onPageJumpClick)
            },
            enabled = pageInput.toIntOrNull() != null && state.pageCount > 0,
            modifier = Modifier.weight(1f),
        ) {
            Text("Go")
        }
    }
}
