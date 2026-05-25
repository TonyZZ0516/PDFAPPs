package com.swiftpdf.app.feature.reader

import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.services.pdf.PdfPreview

data class ReaderUiState(
    val document: DocumentItem? = null,
    val isLoading: Boolean = false,
    val pagePreviews: Map<Int, PdfPreview> = emptyMap(),
    val renderingPageIndexes: Set<Int> = emptySet(),
    val currentPageIndex: Int = 0,
    val knownPageCount: Int = 0,
    val errorMessage: String? = null,
    val zoomLevel: Float = 1.0f,
    val bookmarkedPageNumbers: Set<Int> = emptySet(),
    val isNightMode: Boolean = false,
) {
    val preview: PdfPreview? = pagePreviews[currentPageIndex]
    val currentPageNumber: Int = currentPageIndex + 1
    val pageCount: Int = knownPageCount.takeIf { it > 0 }
        ?: pagePreviews.values.firstOrNull()?.pageCount
        ?: 0
    val canGoPrevious: Boolean = !isLoading && currentPageIndex > 0
    val canGoNext: Boolean = !isLoading && pageCount > 0 && currentPageIndex < pageCount - 1
    val canZoomOut: Boolean = !isLoading && zoomLevel > MinZoomLevel
    val canZoomIn: Boolean = !isLoading && zoomLevel < MaxZoomLevel
    val isCurrentPageBookmarked: Boolean = bookmarkedPageNumbers.contains(currentPageNumber)

    companion object {
        const val MinZoomLevel = 0.75f
        const val MaxZoomLevel = 3.0f
        const val ZoomStep = 0.25f
    }
}
