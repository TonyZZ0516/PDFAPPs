package com.swiftpdf.app.feature.library

import com.swiftpdf.app.domain.document.DocumentItem

data class DocumentLibraryUiState(
    val recentDocuments: List<DocumentItem> = emptyList(),
    val selectedDocument: DocumentItem? = null,
)
