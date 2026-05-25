package com.swiftpdf.app.feature.export

import android.net.Uri
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.services.pdf.ExportedImageSet
import com.swiftpdf.app.services.pdf.ExportedPdf
import com.swiftpdf.app.services.pdf.ImageExportOptions
import com.swiftpdf.app.services.pdf.ImageOutputFormat
import com.swiftpdf.app.services.pdf.PdfToolService
import com.swiftpdf.app.services.pdf.SignatureMark
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
class ExportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val uri: Uri = mock(Uri::class.java)
    private val document = DocumentItem(
        uri = uri,
        displayName = "sample.pdf",
        sizeBytes = 1024L,
        importedAtMillis = 1L,
    )

    @Test
    fun compressWithoutDocumentReportsMissingDocument() {
        val service = FakePdfToolService()
        val viewModel = ExportViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.compressPdf(null) {}

        val state = viewModel.uiState.value
        assertEquals("Open a PDF before using this tool.", state.errorMessage)
        assertNull(state.statusMessage)
        assertFalse(state.isWorking)
        assertEquals(0, service.compressCalls)
    }

    @Test
    fun emptySignatureDoesNotCallService() {
        val service = FakePdfToolService()
        val viewModel = ExportViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.signPdf(
            document = document,
            signatureMark = SignatureMark(strokes = emptyList(), signerName = null),
            onExported = {},
        )

        val state = viewModel.uiState.value
        assertEquals("Draw a signature or enter a signer name first.", state.errorMessage)
        assertFalse(state.isWorking)
        assertEquals(0, service.signCalls)
    }

    @Test
    fun exportImagesReportsImageCount() = runTest(mainDispatcherRule.dispatcher) {
        val service = FakePdfToolService()
        val viewModel = ExportViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.exportImages(document, ImageExportOptions(pageRangeText = "1", outputFormat = ImageOutputFormat.Jpg))
        assertEquals("Exporting images", viewModel.uiState.value.activeOperationLabel)
        assertTrue(viewModel.uiState.value.isWorking)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Exported 2 image(s).", state.statusMessage)
        assertEquals("Images exported", state.successTitle)
        assertEquals("SwiftPDF_images", state.successFileName)
        assertEquals(2, state.exportedImageSet?.uris?.size)
        assertFalse(state.isWorking)
        assertEquals(1, service.exportImageCalls)
        assertEquals(ImageExportOptions(pageRangeText = "1", outputFormat = ImageOutputFormat.Jpg), service.lastImageExportOptions)
    }

    @Test
    fun compressSuccessReportsPreviewCopyAndCallback() = runTest(mainDispatcherRule.dispatcher) {
        val service = FakePdfToolService()
        val viewModel = ExportViewModel(service, mainDispatcherRule.dispatcher)
        var exported: ExportedPdf? = null

        viewModel.compressPdf(document) { exported = it }
        assertEquals("Creating preview copy", viewModel.uiState.value.activeOperationLabel)

        advanceUntilIdle()

        assertSame(service.compressedPdf, exported)
        assertEquals("Created preview copy SwiftPDF_compressed.pdf.", viewModel.uiState.value.statusMessage)
        assertEquals("PDF exported", viewModel.uiState.value.successTitle)
        assertEquals("SwiftPDF_compressed.pdf", viewModel.uiState.value.successFileName)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun extractAndMergeSuccessUsePreviewCopyStatus() = runTest(mainDispatcherRule.dispatcher) {
        val service = FakePdfToolService()
        val viewModel = ExportViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.extractFirstPage(document) {}
        assertEquals("Creating first-page preview copy", viewModel.uiState.value.activeOperationLabel)
        advanceUntilIdle()
        assertEquals("Created preview copy SwiftPDF_first_page.pdf.", viewModel.uiState.value.statusMessage)

        viewModel.mergePdfs(listOf(document, document)) {}
        assertEquals("Creating merged preview copy", viewModel.uiState.value.activeOperationLabel)
        advanceUntilIdle()
        assertEquals("Created preview copy SwiftPDF_merged.pdf.", viewModel.uiState.value.statusMessage)
        assertEquals(1, service.extractCalls)
        assertEquals(1, service.mergeCalls)
    }

    @Test
    fun mergeRequiresAtLeastTwoDocuments() {
        val service = FakePdfToolService()
        val viewModel = ExportViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.mergePdfs(listOf(document)) {}

        val state = viewModel.uiState.value
        assertEquals("Select at least two PDFs to merge.", state.errorMessage)
        assertNull(state.statusMessage)
        assertEquals(0, service.mergeCalls)
    }

    @Test
    fun pdfToolSuccessKeepsExistingImageSet() = runTest(mainDispatcherRule.dispatcher) {
        val service = FakePdfToolService()
        val viewModel = ExportViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.exportImages(document)
        advanceUntilIdle()
        val imageSet = viewModel.uiState.value.exportedImageSet

        viewModel.compressPdf(document) {}
        advanceUntilIdle()

        assertSame(imageSet, viewModel.uiState.value.exportedImageSet)
        assertEquals("Created preview copy SwiftPDF_compressed.pdf.", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun serviceFailureReportsErrorAndStopsWorking() = runTest(mainDispatcherRule.dispatcher) {
        val service = FakePdfToolService(error = IllegalStateException("Export failed"))
        val viewModel = ExportViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.exportImages(document)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Export failed", state.errorMessage)
        assertNull(state.successTitle)
        assertFalse(state.isWorking)
        assertNull(state.activeOperationLabel)
    }

    @Test
    fun saveErrorDraftTurnsErrorIntoRecoverySuccessState() {
        val service = FakePdfToolService()
        val viewModel = ExportViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.mergePdfs(listOf(document)) {}
        viewModel.saveErrorDraft()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("Draft saved.", state.statusMessage)
        assertEquals("Draft saved", state.successTitle)
        assertEquals("SwiftPDF draft", state.successFileName)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakePdfToolService(
    private val error: RuntimeException? = null,
) : PdfToolService {
    private val uri: Uri = mock(Uri::class.java)
    val compressedPdf = ExportedPdf(uri, "SwiftPDF_compressed.pdf", 2048L)
    private val signedPdf = ExportedPdf(uri, "SwiftPDF_signed.pdf", 2048L)
    private val firstPagePdf = ExportedPdf(uri, "SwiftPDF_first_page.pdf", 1024L)
    private val mergedPdf = ExportedPdf(uri, "SwiftPDF_merged.pdf", 4096L)
    private val imageSet = ExportedImageSet(listOf(uri, uri), "SwiftPDF_images")

    var exportImageCalls = 0
        private set
    var compressCalls = 0
        private set
    var signCalls = 0
        private set
    var extractCalls = 0
        private set
    var mergeCalls = 0
        private set

    var lastImageExportOptions: ImageExportOptions? = null
        private set

    override fun exportPagesAsImages(
        document: DocumentItem,
        options: ImageExportOptions,
    ): ExportedImageSet {
        exportImageCalls += 1
        lastImageExportOptions = options
        error?.let { throw it }
        return imageSet
    }

    override fun compressPdf(document: DocumentItem): ExportedPdf {
        compressCalls += 1
        error?.let { throw it }
        return compressedPdf
    }

    override fun signPdf(document: DocumentItem, signatureMark: SignatureMark): ExportedPdf {
        signCalls += 1
        error?.let { throw it }
        return signedPdf
    }

    override fun extractFirstPage(document: DocumentItem): ExportedPdf {
        extractCalls += 1
        error?.let { throw it }
        return firstPagePdf
    }

    override fun mergePdfs(documents: List<DocumentItem>): ExportedPdf {
        mergeCalls += 1
        error?.let { throw it }
        return mergedPdf
    }
}
