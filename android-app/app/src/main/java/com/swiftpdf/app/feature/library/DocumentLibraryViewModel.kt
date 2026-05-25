package com.swiftpdf.app.feature.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftpdf.app.data.document.DocumentHistoryDataSource
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.domain.document.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DocumentLibraryViewModel(
    private val documentRepository: DocumentRepository,
    private val documentHistoryStore: DocumentHistoryDataSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentLibraryUiState())
    val uiState: StateFlow<DocumentLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            documentHistoryStore.recentDocuments.collect { documents ->
                val selectedDocument = _uiState.value.selectedDocument ?: documents.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    recentDocuments = documents,
                    selectedDocument = selectedDocument,
                )
            }
        }
    }

    fun importPdf(uri: Uri) {
        val document = documentRepository.importPdf(uri)
        addDocument(document)
    }

    fun registerGeneratedPdf(uri: Uri, displayName: String, sizeBytes: Long? = null): DocumentItem {
        val document = documentRepository.registerGeneratedPdf(uri, displayName, sizeBytes)
        addDocument(document)
        return document
    }

    private fun addDocument(document: DocumentItem) {
        val nextRecentDocuments = buildList {
            add(document)
            addAll(_uiState.value.recentDocuments.filterNot { it.uri == document.uri })
        }

        _uiState.value = _uiState.value.copy(
            recentDocuments = nextRecentDocuments,
            selectedDocument = document,
        )
        viewModelScope.launch {
            documentHistoryStore.saveRecentDocuments(nextRecentDocuments)
        }
    }

    fun selectDocument(document: DocumentItem) {
        _uiState.value = _uiState.value.copy(selectedDocument = document)
    }

    fun renameRecentDocument(document: DocumentItem, displayName: String) {
        val trimmedDisplayName = displayName.trim()
        if (trimmedDisplayName.isBlank()) return

        val renamedDocument = document.copy(displayName = trimmedDisplayName)
        val nextRecentDocuments = _uiState.value.recentDocuments.map { recentDocument ->
            if (recentDocument.uri == document.uri) renamedDocument else recentDocument
        }
        val nextSelectedDocument = _uiState.value.selectedDocument?.let { selectedDocument ->
            if (selectedDocument.uri == document.uri) renamedDocument else selectedDocument
        }

        _uiState.value = _uiState.value.copy(
            recentDocuments = nextRecentDocuments,
            selectedDocument = nextSelectedDocument,
        )
        viewModelScope.launch {
            documentHistoryStore.saveRecentDocuments(nextRecentDocuments)
        }
    }

    fun removeRecentDocument(document: DocumentItem) {
        val nextRecentDocuments = _uiState.value.recentDocuments.filterNot {
            it.uri == document.uri
        }
        val nextSelectedDocument = _uiState.value.selectedDocument?.takeUnless {
            it.uri == document.uri
        }

        _uiState.value = _uiState.value.copy(
            recentDocuments = nextRecentDocuments,
            selectedDocument = nextSelectedDocument,
        )
        viewModelScope.launch {
            documentHistoryStore.saveRecentDocuments(nextRecentDocuments)
        }
    }

    fun clearRecentDocuments() {
        _uiState.value = _uiState.value.copy(
            recentDocuments = emptyList(),
            selectedDocument = null,
        )
        viewModelScope.launch {
            documentHistoryStore.saveRecentDocuments(emptyList())
        }
    }
}
