package com.swiftpdf.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swiftpdf.app.ui.components.FeatureCard

@Composable
fun SettingsScreen(
    recentDocumentCount: Int,
    onClearRecentDocumentsClick: () -> Unit,
) {
    var showClearHistoryDialog by remember { mutableStateOf(false) }

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
            FeatureCard(
                title = "Recent Files",
                body = "$recentDocumentCount files stored in local history. Clearing history does not delete original PDFs.",
                actionLabel = if (recentDocumentCount > 0) "Clear History" else null,
                onActionClick = if (recentDocumentCount > 0) {
                    { showClearHistoryDialog = true }
                } else {
                    null
                },
            )
        }
        item {
            FeatureCard(
                title = "Storage",
                body = "Imported PDFs stay in their original location. Scanned PDF exports are stored in the app files directory.",
            )
        }
        item {
            FeatureCard(
                title = "Default Export Quality",
                body = "Standard quality is active. High quality export is marked as a Pro option.",
            )
        }
        item {
            FeatureCard(
                title = "About SwiftPDF",
                body = "Version 0.1.0 - MVP development build.",
            )
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear recent history?") },
            text = {
                Text("This only removes SwiftPDF history. Original PDFs stay on the device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearRecentDocumentsClick()
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
