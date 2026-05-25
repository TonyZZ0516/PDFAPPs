package com.swiftpdf.app.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.services.pdf.PdfRendererService
import com.swiftpdf.app.services.pdf.PdfPreview
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel(
    private val pdfRendererService: PdfRendererService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    private val renderJobs = mutableMapOf<Int, Job>()
    private var documentRenderToken = 0

    fun openDocument(document: DocumentItem?) {
        if (document == null) {
            cancelRendering()
            _uiState.value = ReaderUiState()
            return
        }

        val state = _uiState.value
        if (state.document?.uri == document.uri && (state.pagePreviews.isNotEmpty() || state.isLoading)) {
            return
        }

        documentRenderToken += 1
        cancelRendering()
        val previousState = _uiState.value
        _uiState.value = ReaderUiState(
            document = document,
            isLoading = true,
            currentPageIndex = 0,
            zoomLevel = 1.0f,
            isNightMode = previousState.isNightMode,
        )
        requestPageRender(document, pageIndex = 0, renderToken = documentRenderToken)
    }

    fun goToPreviousPage() {
        val state = _uiState.value
        if (!state.canGoPrevious) return

        showPage(state.currentPageIndex - 1)
    }

    fun goToNextPage() {
        val state = _uiState.value
        if (!state.canGoNext) return

        showPage(state.currentPageIndex + 1)
    }

    fun goToPage(pageNumber: Int) {
        val state = _uiState.value
        if (state.document == null) return
        if (state.pageCount <= 0) return

        val pageIndex = (pageNumber - 1).coerceIn(0, state.pageCount - 1)
        showPage(pageIndex)
    }

    fun showPage(pageIndex: Int) {
        val state = _uiState.value
        if (state.document == null) return
        val safePageIndex = if (state.pageCount > 0) {
            pageIndex.coerceIn(0, state.pageCount - 1)
        } else {
            pageIndex.coerceAtLeast(0)
        }

        if (state.currentPageIndex != safePageIndex) {
            _uiState.value = state.copy(currentPageIndex = safePageIndex)
        }
        renderPageIfNeeded(safePageIndex)
    }

    fun renderPageIfNeeded(pageIndex: Int) {
        val state = _uiState.value
        val document = state.document ?: return
        val safePageIndex = if (state.pageCount > 0) {
            pageIndex.coerceIn(0, state.pageCount - 1)
        } else {
            pageIndex.coerceAtLeast(0)
        }

        requestPageRender(document, safePageIndex, documentRenderToken)
    }

    fun zoomIn() {
        updateZoom(_uiState.value.zoomLevel + ReaderUiState.ZoomStep)
    }

    fun zoomOut() {
        updateZoom(_uiState.value.zoomLevel - ReaderUiState.ZoomStep)
    }

    fun resetZoom() {
        updateZoom(1.0f)
    }

    fun toggleBookmarkForCurrentPage() {
        val state = _uiState.value
        if (state.document == null || state.pageCount <= 0) return

        val pageNumber = state.currentPageNumber
        val nextBookmarks = if (state.bookmarkedPageNumbers.contains(pageNumber)) {
            state.bookmarkedPageNumbers - pageNumber
        } else {
            state.bookmarkedPageNumbers + pageNumber
        }
        _uiState.value = state.copy(bookmarkedPageNumbers = nextBookmarks)
    }

    fun toggleNightMode() {
        _uiState.value = _uiState.value.copy(
            isNightMode = !_uiState.value.isNightMode,
        )
    }

    private fun requestPageRender(
        document: DocumentItem,
        pageIndex: Int,
        renderToken: Int,
    ) {
        val state = _uiState.value
        if (state.document?.uri != document.uri) return
        if (state.pageCount > 0 && pageIndex !in 0 until state.pageCount) return
        if (state.pagePreviews.containsKey(pageIndex)) return
        if (renderJobs[pageIndex]?.isActive == true) return

        _uiState.value = state.copy(
            isLoading = state.pagePreviews.isEmpty() && pageIndex == 0,
            renderingPageIndexes = state.renderingPageIndexes + pageIndex,
            errorMessage = null,
        )
        renderJobs[pageIndex] = viewModelScope.launch {
            val result = runCatching {
                withContext(ioDispatcher) {
                    pdfRendererService.renderPage(document.uri, pageIndex)
                }
            }

            renderJobs.remove(pageIndex)
            if (renderToken != documentRenderToken || _uiState.value.document?.uri != document.uri) {
                return@launch
            }

            result.fold(
                onSuccess = { preview ->
                    applyRenderedPage(preview)
                },
                onFailure = { throwable ->
                    val latestState = _uiState.value
                    _uiState.value = latestState.copy(
                        isLoading = false,
                        renderingPageIndexes = latestState.renderingPageIndexes - pageIndex,
                        errorMessage = throwable.message ?: "Unable to render this PDF.",
                    )
                },
            )
        }
    }

    private fun applyRenderedPage(preview: PdfPreview) {
        val state = _uiState.value
        _uiState.value = state.copy(
            isLoading = false,
            pagePreviews = state.pagePreviews + (preview.pageIndex to preview),
            renderingPageIndexes = state.renderingPageIndexes - preview.pageIndex,
            knownPageCount = preview.pageCount,
            errorMessage = null,
        )
    }

    private fun updateZoom(nextZoomLevel: Float) {
        val state = _uiState.value
        val clampedZoomLevel = nextZoomLevel.coerceIn(
            ReaderUiState.MinZoomLevel,
            ReaderUiState.MaxZoomLevel,
        )
        _uiState.value = state.copy(zoomLevel = clampedZoomLevel)
    }

    private fun cancelRendering() {
        renderJobs.values.forEach { it.cancel() }
        renderJobs.clear()
    }
}
