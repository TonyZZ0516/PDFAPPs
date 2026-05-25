package com.swiftpdf.app.feature.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftpdf.app.services.pdf.ExportedPdf
import com.swiftpdf.app.services.pdf.ImagePdfExportService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(
    private val imagePdfExportService: ImagePdfExportService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun updateCameraPermission(isGranted: Boolean) {
        _uiState.value = _uiState.value.copy(
            cameraPermissionStatus = if (isGranted) {
                CameraPermissionStatus.Granted
            } else {
                CameraPermissionStatus.Denied
            },
        )
    }

    fun markPermissionUnknown() {
        _uiState.value = _uiState.value.copy(
            cameraPermissionStatus = CameraPermissionStatus.Unknown,
        )
    }

    fun recordCapturedPage(uri: Uri, displayName: String) {
        val page = CapturedScanPage(
            uri = uri,
            displayName = displayName,
            capturedAtMillis = System.currentTimeMillis(),
        )
        _uiState.value = _uiState.value.copy(
            capturedPages = _uiState.value.capturedPages + page,
            captureErrorMessage = null,
        )
    }

    fun recordImportedImages(images: List<Pair<Uri, String>>) {
        if (images.isEmpty()) return

        val now = System.currentTimeMillis()
        val importedPages = images.mapIndexed { index, image ->
            CapturedScanPage(
                uri = image.first,
                displayName = image.second.ifBlank { "image_${now}_$index" },
                capturedAtMillis = now + index,
            )
        }
        _uiState.value = _uiState.value.copy(
            capturedPages = _uiState.value.capturedPages + importedPages,
            captureErrorMessage = null,
            exportErrorMessage = null,
        )
    }

    fun recordCaptureError(message: String) {
        _uiState.value = _uiState.value.copy(captureErrorMessage = message)
    }

    fun requestCapture() {
        _uiState.value = _uiState.value.copy(
            captureRequestCount = _uiState.value.captureRequestCount + 1,
            captureErrorMessage = null,
        )
    }

    fun removeCapturedPage(page: CapturedScanPage) {
        _uiState.value = _uiState.value.copy(
            capturedPages = _uiState.value.capturedPages.filterNot { it.uri == page.uri },
            exportErrorMessage = null,
        )
    }

    fun retakeCapturedPage(page: CapturedScanPage) {
        removeCapturedPage(page)
        requestCapture()
    }

    fun rotateCapturedPage(page: CapturedScanPage) {
        updateCapturedPage(page) { existing ->
            existing.copy(rotationDegrees = (existing.rotationDegrees + 90) % 360)
        }
    }

    fun toggleAutoCropForAllPages() {
        val pages = _uiState.value.capturedPages
        if (pages.isEmpty()) return

        val shouldEnableAutoCrop = pages.any { !it.isAutoCropped }
        _uiState.value = _uiState.value.copy(
            capturedPages = pages.map { it.copy(isAutoCropped = shouldEnableAutoCrop) },
            exportErrorMessage = null,
        )
    }

    fun moveCapturedPageUp(page: CapturedScanPage) {
        moveCapturedPage(page, direction = -1)
    }

    fun moveCapturedPageDown(page: CapturedScanPage) {
        moveCapturedPage(page, direction = 1)
    }

    fun exportScanToPdf(onExported: (ExportedPdf) -> Unit) {
        val pages = _uiState.value.capturedPages
        if (pages.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                exportErrorMessage = "Capture at least one page before exporting.",
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isExporting = true,
            exportErrorMessage = null,
        )

        viewModelScope.launch {
            val result = runCatching {
                withContext(ioDispatcher) {
                    imagePdfExportService.exportScanPages(pages)
                }
            }

            result.onSuccess { exportedPdf ->
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    capturedPages = emptyList(),
                    exportErrorMessage = null,
                )
                onExported(exportedPdf)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportErrorMessage = throwable.message ?: "Unable to export scan pages.",
                )
            }
        }
    }

    private fun updateCapturedPage(
        page: CapturedScanPage,
        transform: (CapturedScanPage) -> CapturedScanPage,
    ) {
        _uiState.value = _uiState.value.copy(
            capturedPages = _uiState.value.capturedPages.map { existing ->
                if (existing.uri == page.uri) transform(existing) else existing
            },
            exportErrorMessage = null,
        )
    }

    private fun moveCapturedPage(page: CapturedScanPage, direction: Int) {
        val pages = _uiState.value.capturedPages.toMutableList()
        val index = pages.indexOfFirst { it.uri == page.uri }
        if (index < 0) return

        val targetIndex = (index + direction).coerceIn(0, pages.lastIndex)
        if (targetIndex == index) return

        val movedPage = pages.removeAt(index)
        pages.add(targetIndex, movedPage)
        _uiState.value = _uiState.value.copy(
            capturedPages = pages,
            exportErrorMessage = null,
        )
    }
}
