package com.swiftpdf.app.feature.reader

import android.graphics.Bitmap
import android.net.Uri
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.services.pdf.PdfPreview
import com.swiftpdf.app.services.pdf.PdfRendererService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    @get:Rule
    val mainDispatcherRule = ReaderMainDispatcherRule()

    private val uri: Uri = mock(Uri::class.java)
    private val otherUri: Uri = mock(Uri::class.java)
    private val document = DocumentItem(uri, "reader.pdf", 2048L, 1L)
    private val otherDocument = DocumentItem(otherUri, "other.pdf", 4096L, 2L)

    @Test
    fun openDocumentRendersFirstPage() = runTest(mainDispatcherRule.dispatcher) {
        val renderer = FakePdfRendererService(pageCount = 3)
        val viewModel = ReaderViewModel(renderer, mainDispatcherRule.dispatcher)

        viewModel.openDocument(document)
        assertTrue(viewModel.uiState.value.isLoading)
        assertSame(document, viewModel.uiState.value.document)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(0, state.currentPageIndex)
        assertEquals(1, state.currentPageNumber)
        assertEquals(3, state.pageCount)
        assertTrue(state.pagePreviews.containsKey(0))
        assertEquals(listOf(0), renderer.renderedPages)
    }

    @Test
    fun openSameDocumentWithPreviewDoesNotRenderAgain() = runTest(mainDispatcherRule.dispatcher) {
        val renderer = FakePdfRendererService(pageCount = 2)
        val viewModel = ReaderViewModel(renderer, mainDispatcherRule.dispatcher)

        viewModel.openDocument(document)
        advanceUntilIdle()
        viewModel.openDocument(document)
        advanceUntilIdle()

        assertEquals(listOf(0), renderer.renderedPages)
    }

    @Test
    fun nextAndPreviousPageStayInsideBounds() = runTest(mainDispatcherRule.dispatcher) {
        val renderer = FakePdfRendererService(pageCount = 2)
        val viewModel = ReaderViewModel(renderer, mainDispatcherRule.dispatcher)

        viewModel.openDocument(document)
        advanceUntilIdle()
        viewModel.goToPreviousPage()
        advanceUntilIdle()
        viewModel.goToNextPage()
        advanceUntilIdle()
        viewModel.goToNextPage()
        advanceUntilIdle()
        viewModel.goToPreviousPage()
        advanceUntilIdle()

        assertEquals(listOf(0, 1), renderer.renderedPages)
        assertEquals(0, viewModel.uiState.value.currentPageIndex)
    }

    @Test
    fun pageJumpClampsToAvailablePages() = runTest(mainDispatcherRule.dispatcher) {
        val renderer = FakePdfRendererService(pageCount = 5)
        val viewModel = ReaderViewModel(renderer, mainDispatcherRule.dispatcher)

        viewModel.openDocument(document)
        advanceUntilIdle()
        viewModel.goToPage(99)
        advanceUntilIdle()
        viewModel.goToPage(0)
        advanceUntilIdle()

        assertEquals(listOf(0, 4), renderer.renderedPages)
        assertEquals(0, viewModel.uiState.value.currentPageIndex)
    }

    @Test
    fun visiblePagesRenderOnceAndStayCached() = runTest(mainDispatcherRule.dispatcher) {
        val renderer = FakePdfRendererService(pageCount = 4)
        val viewModel = ReaderViewModel(renderer, mainDispatcherRule.dispatcher)

        viewModel.openDocument(document)
        advanceUntilIdle()
        viewModel.renderPageIfNeeded(1)
        advanceUntilIdle()
        viewModel.renderPageIfNeeded(1)
        advanceUntilIdle()
        viewModel.showPage(2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(0, 1, 2), renderer.renderedPages)
        assertEquals(setOf(0, 1, 2), state.pagePreviews.keys)
        assertEquals(2, state.currentPageIndex)
        assertEquals(4, state.pageCount)
    }

    @Test
    fun zoomControlsClampAndReset() {
        val viewModel = ReaderViewModel(FakePdfRendererService(), mainDispatcherRule.dispatcher)

        repeat(20) { viewModel.zoomIn() }
        assertEquals(ReaderUiState.MaxZoomLevel, viewModel.uiState.value.zoomLevel, 0.001f)

        repeat(20) { viewModel.zoomOut() }
        assertEquals(ReaderUiState.MinZoomLevel, viewModel.uiState.value.zoomLevel, 0.001f)

        viewModel.resetZoom()
        assertEquals(1.0f, viewModel.uiState.value.zoomLevel, 0.001f)
    }

    @Test
    fun sameDocumentKeepsZoomButNewDocumentResetsZoom() = runTest(mainDispatcherRule.dispatcher) {
        val renderer = FakePdfRendererService(pageCount = 2)
        val viewModel = ReaderViewModel(renderer, mainDispatcherRule.dispatcher)

        viewModel.openDocument(document)
        advanceUntilIdle()
        viewModel.zoomIn()
        viewModel.goToNextPage()
        advanceUntilIdle()

        assertEquals(1.25f, viewModel.uiState.value.zoomLevel, 0.001f)

        viewModel.openDocument(otherDocument)
        advanceUntilIdle()

        assertEquals(1.0f, viewModel.uiState.value.zoomLevel, 0.001f)
    }

    @Test
    fun bookmarkAndNightModeToggleReaderState() = runTest(mainDispatcherRule.dispatcher) {
        val renderer = FakePdfRendererService(pageCount = 2)
        val viewModel = ReaderViewModel(renderer, mainDispatcherRule.dispatcher)

        viewModel.openDocument(document)
        advanceUntilIdle()

        viewModel.toggleBookmarkForCurrentPage()
        assertTrue(viewModel.uiState.value.isCurrentPageBookmarked)
        assertEquals(setOf(1), viewModel.uiState.value.bookmarkedPageNumbers)

        viewModel.goToNextPage()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCurrentPageBookmarked)

        viewModel.toggleNightMode()
        assertTrue(viewModel.uiState.value.isNightMode)

        viewModel.openDocument(otherDocument)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isNightMode)
        assertEquals(emptySet<Int>(), viewModel.uiState.value.bookmarkedPageNumbers)
    }

    @Test
    fun nullDocumentResetsReaderState() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ReaderViewModel(FakePdfRendererService(), mainDispatcherRule.dispatcher)

        viewModel.openDocument(document)
        advanceUntilIdle()
        viewModel.openDocument(null)

        val state = viewModel.uiState.value
        assertNull(state.document)
        assertNull(state.preview)
        assertFalse(state.isLoading)
        assertEquals(1.0f, state.zoomLevel, 0.001f)
    }

    @Test
    fun renderFailureReportsPreviewError() = runTest(mainDispatcherRule.dispatcher) {
        val renderer = FakePdfRendererService(error = IllegalStateException("Renderer failed"))
        val viewModel = ReaderViewModel(renderer, mainDispatcherRule.dispatcher)

        viewModel.openDocument(document)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertSame(document, state.document)
        assertEquals("Renderer failed", state.errorMessage)
        assertFalse(state.isLoading)
        assertNull(state.preview)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderMainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakePdfRendererService(
    private val pageCount: Int = 1,
    private val error: RuntimeException? = null,
) : PdfRendererService {
    private val bitmap: Bitmap = mock(Bitmap::class.java)
    val renderedPages = mutableListOf<Int>()

    override fun renderPage(uri: Uri, pageIndex: Int, maxWidthPx: Int): PdfPreview {
        renderedPages += pageIndex
        error?.let { throw it }
        return PdfPreview(
            pageBitmap = bitmap,
            pageIndex = pageIndex,
            pageCount = pageCount,
        )
    }
}
