package com.swiftpdf.app.feature.library

import android.net.Uri
import com.swiftpdf.app.data.document.DocumentHistoryDataSource
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.domain.document.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentLibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = LibraryMainDispatcherRule()

    private val uriOne: Uri = mock(Uri::class.java)
    private val uriTwo: Uri = mock(Uri::class.java)
    private val uriThree: Uri = mock(Uri::class.java)
    private val firstDocument = DocumentItem(uriOne, "first.pdf", 100L, 1L)
    private val secondDocument = DocumentItem(uriTwo, "second.pdf", 200L, 2L)
    private val generatedDocument = DocumentItem(uriThree, "generated.pdf", 300L, 3L)

    @Test
    fun historyLoadSelectsFirstDocumentWhenNothingSelected() = runTest(mainDispatcherRule.dispatcher) {
        val history = FakeDocumentHistoryDataSource(initialDocuments = listOf(firstDocument, secondDocument))
        val viewModel = DocumentLibraryViewModel(FakeDocumentRepository(), history)
        advanceUntilIdle()

        assertEquals(listOf(firstDocument, secondDocument), viewModel.uiState.value.recentDocuments)
        assertSame(firstDocument, viewModel.uiState.value.selectedDocument)
    }

    @Test
    fun importPdfPrependsSelectsAndPersistsDocument() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeDocumentRepository(importedDocuments = mapOf(uriThree to generatedDocument))
        val history = FakeDocumentHistoryDataSource(initialDocuments = listOf(firstDocument, secondDocument))
        val viewModel = DocumentLibraryViewModel(repository, history)
        advanceUntilIdle()

        viewModel.importPdf(uriThree)
        advanceUntilIdle()

        val expected = listOf(generatedDocument, firstDocument, secondDocument)
        assertEquals(expected, viewModel.uiState.value.recentDocuments)
        assertSame(generatedDocument, viewModel.uiState.value.selectedDocument)
        assertEquals(listOf(expected), history.savedDocuments)
    }

    @Test
    fun importExistingDocumentMovesItToTopWithoutDuplicate() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeDocumentRepository(importedDocuments = mapOf(uriTwo to secondDocument))
        val history = FakeDocumentHistoryDataSource(initialDocuments = listOf(firstDocument, secondDocument))
        val viewModel = DocumentLibraryViewModel(repository, history)
        advanceUntilIdle()

        viewModel.importPdf(uriTwo)
        advanceUntilIdle()

        val expected = listOf(secondDocument, firstDocument)
        assertEquals(expected, viewModel.uiState.value.recentDocuments)
        assertSame(secondDocument, viewModel.uiState.value.selectedDocument)
        assertEquals(listOf(expected), history.savedDocuments)
    }

    @Test
    fun generatedPdfIsRegisteredSelectedPersistedAndReturned() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeDocumentRepository(generatedDocument = generatedDocument)
        val history = FakeDocumentHistoryDataSource(initialDocuments = listOf(firstDocument))
        val viewModel = DocumentLibraryViewModel(repository, history)
        advanceUntilIdle()

        val returnedDocument = viewModel.registerGeneratedPdf(uriThree, "generated.pdf", 300L)
        advanceUntilIdle()

        assertSame(generatedDocument, returnedDocument)
        assertEquals(listOf(generatedDocument, firstDocument), viewModel.uiState.value.recentDocuments)
        assertSame(generatedDocument, viewModel.uiState.value.selectedDocument)
        assertEquals(uriThree, repository.lastGeneratedUri)
        assertEquals("generated.pdf", repository.lastGeneratedName)
        assertEquals(300L, repository.lastGeneratedSize)
    }

    @Test
    fun selectAndRemoveDocumentsUpdateCurrentSelection() = runTest(mainDispatcherRule.dispatcher) {
        val history = FakeDocumentHistoryDataSource(initialDocuments = listOf(firstDocument, secondDocument))
        val viewModel = DocumentLibraryViewModel(FakeDocumentRepository(), history)
        advanceUntilIdle()

        viewModel.selectDocument(secondDocument)
        assertSame(secondDocument, viewModel.uiState.value.selectedDocument)

        viewModel.removeRecentDocument(firstDocument)
        advanceUntilIdle()
        assertEquals(listOf(secondDocument), viewModel.uiState.value.recentDocuments)
        assertSame(secondDocument, viewModel.uiState.value.selectedDocument)

        viewModel.removeRecentDocument(secondDocument)
        advanceUntilIdle()
        assertEquals(emptyList<DocumentItem>(), viewModel.uiState.value.recentDocuments)
        assertNull(viewModel.uiState.value.selectedDocument)
        assertEquals(listOf(listOf(secondDocument), emptyList<DocumentItem>()), history.savedDocuments)
    }

    @Test
    fun renameRecentDocumentUpdatesHistoryAndCurrentSelection() = runTest(mainDispatcherRule.dispatcher) {
        val history = FakeDocumentHistoryDataSource(initialDocuments = listOf(firstDocument, secondDocument))
        val viewModel = DocumentLibraryViewModel(FakeDocumentRepository(), history)
        advanceUntilIdle()

        viewModel.selectDocument(secondDocument)
        viewModel.renameRecentDocument(secondDocument, "  renamed.pdf  ")
        advanceUntilIdle()

        val renamedDocument = secondDocument.copy(displayName = "renamed.pdf")
        assertEquals(listOf(firstDocument, renamedDocument), viewModel.uiState.value.recentDocuments)
        assertEquals(renamedDocument, viewModel.uiState.value.selectedDocument)
        assertEquals(listOf(listOf(firstDocument, renamedDocument)), history.savedDocuments)
    }

    @Test
    fun clearRecentDocumentsClearsSelectionAndPersistsEmptyList() = runTest(mainDispatcherRule.dispatcher) {
        val history = FakeDocumentHistoryDataSource(initialDocuments = listOf(firstDocument, secondDocument))
        val viewModel = DocumentLibraryViewModel(FakeDocumentRepository(), history)
        advanceUntilIdle()

        viewModel.clearRecentDocuments()
        advanceUntilIdle()

        assertEquals(emptyList<DocumentItem>(), viewModel.uiState.value.recentDocuments)
        assertNull(viewModel.uiState.value.selectedDocument)
        assertEquals(listOf(emptyList<DocumentItem>()), history.savedDocuments)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryMainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeDocumentRepository(
    private val importedDocuments: Map<Uri, DocumentItem> = emptyMap(),
    private val generatedDocument: DocumentItem? = null,
) : DocumentRepository {
    var lastGeneratedUri: Uri? = null
        private set
    var lastGeneratedName: String? = null
        private set
    var lastGeneratedSize: Long? = null
        private set

    override fun importPdf(uri: Uri): DocumentItem {
        return importedDocuments[uri] ?: error("No fake import document for $uri")
    }

    override fun registerGeneratedPdf(uri: Uri, displayName: String, sizeBytes: Long?): DocumentItem {
        lastGeneratedUri = uri
        lastGeneratedName = displayName
        lastGeneratedSize = sizeBytes
        return generatedDocument ?: DocumentItem(uri, displayName, sizeBytes, 1L)
    }
}

private class FakeDocumentHistoryDataSource(
    initialDocuments: List<DocumentItem> = emptyList(),
) : DocumentHistoryDataSource {
    private val documents = MutableStateFlow(initialDocuments)
    val savedDocuments = mutableListOf<List<DocumentItem>>()

    override val recentDocuments: StateFlow<List<DocumentItem>> = documents

    override suspend fun saveRecentDocuments(documents: List<DocumentItem>) {
        savedDocuments += documents
        this.documents.value = documents
    }
}
