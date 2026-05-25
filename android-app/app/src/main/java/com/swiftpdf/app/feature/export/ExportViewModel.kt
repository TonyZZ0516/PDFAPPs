package com.swiftpdf.app.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.services.pdf.ExportedPdf
import com.swiftpdf.app.services.pdf.ImageExportOptions
import com.swiftpdf.app.services.pdf.PdfToolService
import com.swiftpdf.app.services.pdf.SignatureMark
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportViewModel(
    private val pdfToolService: PdfToolService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun exportImages(
        document: DocumentItem?,
        options: ImageExportOptions = ImageExportOptions(),
    ) {
        if (document == null) {
            reportMissingDocument()
            return
        }

        runTool(
            operationLabel = "Exporting images",
            block = { pdfToolService.exportPagesAsImages(document, options) },
            onSuccess = { imageSet ->
                _uiState.value = ExportUiState(
                    statusMessage = "Exported ${imageSet.uris.size} image(s).",
                    successTitle = "Images exported",
                    successFileName = imageSet.displayName,
                    successDetail = "${imageSet.uris.size} image(s) are ready. Open or share them from this device.",
                    exportedImageSet = imageSet,
                )
            },
        )
    }

    fun compressPdf(document: DocumentItem?, onExported: (ExportedPdf) -> Unit) {
        if (document == null) {
            reportMissingDocument()
            return
        }

        runPdfTool(
            operationLabel = "Creating preview copy",
            block = { pdfToolService.compressPdf(document) },
            onExported = onExported,
        )
    }

    fun signPdf(
        document: DocumentItem?,
        signatureMark: SignatureMark,
        onExported: (ExportedPdf) -> Unit,
    ) {
        if (document == null) {
            reportMissingDocument()
            return
        }
        if (!signatureMark.hasContent) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Draw a signature or enter a signer name first.",
                statusMessage = null,
                successTitle = null,
                successFileName = null,
                successDetail = null,
            )
            return
        }

        runPdfTool(
            operationLabel = "Creating signed preview copy",
            block = { pdfToolService.signPdf(document, signatureMark) },
            onExported = onExported,
        )
    }

    fun extractFirstPage(document: DocumentItem?, onExported: (ExportedPdf) -> Unit) {
        if (document == null) {
            reportMissingDocument()
            return
        }

        runPdfTool(
            operationLabel = "Creating first-page preview copy",
            block = { pdfToolService.extractFirstPage(document) },
            onExported = onExported,
        )
    }

    fun mergePdfs(documents: List<DocumentItem>, onExported: (ExportedPdf) -> Unit) {
        if (documents.size < 2) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Select at least two PDFs to merge.",
                statusMessage = null,
                successTitle = null,
                successFileName = null,
                successDetail = null,
            )
            return
        }

        runPdfTool(
            operationLabel = "Creating merged preview copy",
            block = { pdfToolService.mergePdfs(documents) },
            onExported = onExported,
        )
    }

    private fun runPdfTool(
        operationLabel: String,
        block: () -> ExportedPdf,
        onExported: (ExportedPdf) -> Unit,
    ) {
        runTool(
            operationLabel = operationLabel,
            block = block,
            onSuccess = { exportedPdf ->
                _uiState.value = ExportUiState(
                    statusMessage = "Created preview copy ${exportedPdf.displayName}.",
                    successTitle = "PDF exported",
                    successFileName = exportedPdf.displayName,
                    successDetail = "Saved as a new copy. Open it now or share it with another app.",
                    exportedImageSet = _uiState.value.exportedImageSet,
                )
                onExported(exportedPdf)
            },
        )
    }

    fun clearResultMessage() {
        _uiState.value = _uiState.value.copy(
            statusMessage = null,
            successTitle = null,
            successFileName = null,
            successDetail = null,
            errorMessage = null,
        )
    }

    fun saveErrorDraft() {
        val currentMessage = _uiState.value.errorMessage ?: "Current order and settings are preserved."
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            statusMessage = "Draft saved.",
            successTitle = "Draft saved",
            successFileName = "SwiftPDF draft",
            successDetail = currentMessage,
        )
    }

    private fun <T> runTool(
        operationLabel: String,
        block: () -> T,
        onSuccess: (T) -> Unit,
    ) {
        if (_uiState.value.isWorking) return

        _uiState.value = _uiState.value.copy(
            isWorking = true,
            activeOperationLabel = operationLabel,
            errorMessage = null,
            statusMessage = null,
            successTitle = null,
            successFileName = null,
            successDetail = null,
        )

        viewModelScope.launch {
            val result = runCatching {
                withContext(ioDispatcher) {
                    block()
                }
            }

            result.onSuccess(onSuccess).onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    activeOperationLabel = null,
                    errorMessage = throwable.message ?: "Unable to finish this export.",
                    statusMessage = null,
                    successTitle = null,
                    successFileName = null,
                    successDetail = null,
                )
            }
        }
    }

    private fun reportMissingDocument() {
        _uiState.value = _uiState.value.copy(
            errorMessage = "Open a PDF before using this tool.",
            statusMessage = null,
            successTitle = null,
            successFileName = null,
            successDetail = null,
        )
    }
}
