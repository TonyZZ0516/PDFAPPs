package com.swiftpdf.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.domain.document.toReadableDateTime
import com.swiftpdf.app.domain.document.toReadableFileSize

@Composable
fun HomeScreen(
    recentDocuments: List<DocumentItem>,
    onImportPdfClick: () -> Unit,
    onScanClick: () -> Unit,
    onExportClick: () -> Unit,
    onDocumentClick: (DocumentItem) -> Unit,
    onDocumentToolsClick: (DocumentItem) -> Unit,
    onDocumentShareClick: (DocumentItem) -> Unit,
    onRenameDocumentClick: (DocumentItem, String) -> Unit,
    onRemoveDocumentClick: (DocumentItem) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var actionDocument by remember { mutableStateOf<DocumentItem?>(null) }
    var renameDocument by remember { mutableStateOf<DocumentItem?>(null) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    val sortedDocuments = remember(recentDocuments, newestFirst) {
        if (newestFirst) {
            recentDocuments.sortedByDescending { it.importedAtMillis }
        } else {
            recentDocuments.sortedBy { it.importedAtMillis }
        }
    }
    val filteredDocuments = remember(sortedDocuments, query) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            sortedDocuments
        } else {
            sortedDocuments.filter { document ->
                document.displayName.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }
    val homeTools = remember(onScanClick, onExportClick) {
        listOf(
            HomeTool("Image to PDF", "3 left today", Icons.Outlined.AddPhotoAlternate, onScanClick),
            HomeTool("Sign PDF", "Save copy", Icons.Outlined.Draw, onExportClick),
            HomeTool("Compress", "Basic free", Icons.Outlined.Compress, onExportClick),
            HomeTool("Merge", "Multi-file", Icons.AutoMirrored.Outlined.MergeType, onExportClick),
            HomeTool("Split", "Export pages", Icons.Outlined.ContentCut, onExportClick),
            HomeTool("PDF to Image", "JPG/PNG", Icons.Outlined.Image, onExportClick),
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 8.dp,
            end = 20.dp,
            bottom = 108.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                placeholder = {
                    Text(
                        text = "Search PDFs, tools, or folders",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                shape = MaterialTheme.shapes.medium,
            )
        }
        item {
            ToolGrid(tools = homeTools)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (query.isBlank()) "Recent files" else "Search results",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { showSortDialog = true }) {
                    Text("Sort")
                }
            }
        }
        when {
            recentDocuments.isEmpty() -> {
                item {
                    EmptyHomeState(
                        onImportPdfClick = onImportPdfClick,
                        onImageToPdfClick = onScanClick,
                    )
                }
            }
            filteredDocuments.isEmpty() -> {
                item {
                    EmptySearchRow(onClick = onImportPdfClick)
                }
            }
            else -> {
                items(
                    items = filteredDocuments,
                    key = { it.uri.toString() },
                ) { document ->
                    RecentDocumentRow(
                        document = document,
                        onClick = { onDocumentClick(document) },
                        onMoreClick = { actionDocument = document },
                    )
                }
            }
        }
    }

    actionDocument?.let { document ->
        RecentDocumentActionsDialog(
            document = document,
            onDismiss = { actionDocument = null },
            onOpenClick = {
                actionDocument = null
                onDocumentClick(document)
            },
            onToolsClick = {
                actionDocument = null
                onDocumentToolsClick(document)
            },
            onRenameClick = {
                actionDocument = null
                renameDocument = document
            },
            onShareClick = {
                actionDocument = null
                onDocumentShareClick(document)
            },
            onRemoveClick = {
                actionDocument = null
                onRemoveDocumentClick(document)
            },
        )
    }
    renameDocument?.let { document ->
        RenameDocumentDialog(
            document = document,
            onDismiss = { renameDocument = null },
            onRenameClick = { nextDisplayName ->
                renameDocument = null
                onRenameDocumentClick(document, nextDisplayName)
            },
        )
    }
    if (showSortDialog) {
        SortDialog(
            newestFirst = newestFirst,
            onNewestFirstClick = {
                newestFirst = true
                showSortDialog = false
            },
            onOldestFirstClick = {
                newestFirst = false
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false },
        )
    }
}

private data class HomeTool(
    val title: String,
    val caption: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun ToolGrid(tools: List<HomeTool>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tools.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { tool ->
                    ToolTile(
                        tool = tool,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolTile(
    tool: HomeTool,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = tool.onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tool.caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyHomeState(
    onImportPdfClick: () -> Unit,
    onImageToPdfClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp),
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
                modifier = Modifier.size(78.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Text(
                text = "No local PDFs yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = "Import a PDF or turn your photos into a clean PDF.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onImportPdfClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Import PDF")
                }
                OutlinedButton(
                    onClick = onImageToPdfClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Image to PDF")
                }
            }
        }
    }
}

@Composable
private fun EmptySearchRow(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PdfBadge()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "No matching files",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Try another name or import a PDF.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RecentDocumentRow(
    document: DocumentItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PdfBadge()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${document.sizeBytes.toReadableFileSize()} - ${document.importedAtMillis.toReadableDateTime()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Document actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun RecentDocumentActionsDialog(
    document: DocumentItem,
    onDismiss: () -> Unit,
    onOpenClick: () -> Unit,
    onToolsClick: () -> Unit,
    onRenameClick: () -> Unit,
    onShareClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File actions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Open, rename, share, send to tools, or remove it from local history.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenClick) {
                Text("Open")
            }
        },
        dismissButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onToolsClick) {
                        Text("Tools")
                    }
                    TextButton(onClick = onRenameClick) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Rename")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Share")
                    }
                    TextButton(onClick = onRemoveClick) {
                        Text("Remove")
                    }
                }
            }
        },
    )
}

@Composable
private fun RenameDocumentDialog(
    document: DocumentItem,
    onDismiss: () -> Unit,
    onRenameClick: (String) -> Unit,
) {
    var displayName by rememberSaveable(document.uri.toString()) {
        mutableStateOf(document.displayName)
    }
    val trimmedDisplayName = displayName.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename file") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This changes the display name in SwiftPDF history only. The original file is not modified.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    singleLine = true,
                    label = { Text("Display name") },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = trimmedDisplayName.isNotEmpty() && trimmedDisplayName != document.displayName,
                onClick = { onRenameClick(trimmedDisplayName) },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SortDialog(
    newestFirst: Boolean,
    onNewestFirstClick: () -> Unit,
    onOldestFirstClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort recent files") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (newestFirst) {
                        "Current order: newest first."
                    } else {
                        "Current order: oldest first."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onNewestFirstClick) {
                Text("Newest first")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOldestFirstClick) {
                    Text("Oldest first")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}
