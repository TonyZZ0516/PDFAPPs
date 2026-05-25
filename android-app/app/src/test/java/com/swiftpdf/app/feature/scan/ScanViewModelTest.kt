package com.swiftpdf.app.feature.scan

import android.net.Uri
import com.swiftpdf.app.services.pdf.ExportedPdf
import com.swiftpdf.app.services.pdf.ImagePdfExportService
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
class ScanViewModelTest {
    @get:Rule
    val mainDispatcherRule = ScanMainDispatcherRule()

    private val uriOne: Uri = mock(Uri::class.java)
    private val uriTwo: Uri = mock(Uri::class.java)
    private val uriThree: Uri = mock(Uri::class.java)

    @Test
    fun cameraPermissionStateUpdates() {
        val viewModel = ScanViewModel(FakeImagePdfExportService(), mainDispatcherRule.dispatcher)

        viewModel.updateCameraPermission(isGranted = true)
        assertEquals(CameraPermissionStatus.Granted, viewModel.uiState.value.cameraPermissionStatus)
        assertTrue(viewModel.uiState.value.hasCameraPermission)

        viewModel.updateCameraPermission(isGranted = false)
        assertEquals(CameraPermissionStatus.Denied, viewModel.uiState.value.cameraPermissionStatus)
        assertFalse(viewModel.uiState.value.hasCameraPermission)

        viewModel.markPermissionUnknown()
        assertEquals(CameraPermissionStatus.Unknown, viewModel.uiState.value.cameraPermissionStatus)
    }

    @Test
    fun capturedAndImportedPagesAppendToQueue() {
        val viewModel = ScanViewModel(FakeImagePdfExportService(), mainDispatcherRule.dispatcher)

        viewModel.recordCapturedPage(uriOne, "camera.jpg")
        viewModel.recordImportedImages(listOf(uriTwo to "", uriThree to "imported.jpg"))

        val pages = viewModel.uiState.value.capturedPages
        assertEquals(3, pages.size)
        assertEquals("camera.jpg", pages[0].displayName)
        assertTrue(pages[1].displayName.startsWith("image_"))
        assertEquals("imported.jpg", pages[2].displayName)
        assertEquals(3, viewModel.uiState.value.captureCount)
        assertSame(pages.last(), viewModel.uiState.value.latestCapturedPage)
        assertTrue(viewModel.uiState.value.canExport)
    }

    @Test
    fun captureRequestAndErrorStateAreTracked() {
        val viewModel = ScanViewModel(FakeImagePdfExportService(), mainDispatcherRule.dispatcher)

        viewModel.recordCaptureError("Camera failed")
        assertEquals("Camera failed", viewModel.uiState.value.captureErrorMessage)

        viewModel.requestCapture()
        assertEquals(1, viewModel.uiState.value.captureRequestCount)
        assertNull(viewModel.uiState.value.captureErrorMessage)
    }

    @Test
    fun pageQueueCanRemoveRetakeRotateAndMove() {
        val viewModel = ScanViewModel(FakeImagePdfExportService(), mainDispatcherRule.dispatcher)
        viewModel.recordImportedImages(
            listOf(
                uriOne to "one.jpg",
                uriTwo to "two.jpg",
                uriThree to "three.jpg",
            ),
        )

        val one = viewModel.uiState.value.capturedPages[0]
        val two = viewModel.uiState.value.capturedPages[1]
        val three = viewModel.uiState.value.capturedPages[2]

        viewModel.rotateCapturedPage(two)
        assertEquals(90, viewModel.uiState.value.capturedPages[1].rotationDegrees)

        repeat(4) { viewModel.rotateCapturedPage(two) }
        assertEquals(90, viewModel.uiState.value.capturedPages[1].rotationDegrees)

        viewModel.toggleAutoCropForAllPages()
        assertTrue(viewModel.uiState.value.capturedPages.all { it.isAutoCropped })

        viewModel.toggleAutoCropForAllPages()
        assertTrue(viewModel.uiState.value.capturedPages.none { it.isAutoCropped })

        viewModel.moveCapturedPageDown(one)
        assertEquals(listOf("two.jpg", "one.jpg", "three.jpg"), viewModel.uiState.value.capturedPages.map { it.displayName })

        viewModel.moveCapturedPageUp(three)
        assertEquals(listOf("two.jpg", "three.jpg", "one.jpg"), viewModel.uiState.value.capturedPages.map { it.displayName })

        viewModel.removeCapturedPage(three)
        assertEquals(listOf("two.jpg", "one.jpg"), viewModel.uiState.value.capturedPages.map { it.displayName })

        viewModel.retakeCapturedPage(two)
        assertEquals(listOf("one.jpg"), viewModel.uiState.value.capturedPages.map { it.displayName })
        assertEquals(1, viewModel.uiState.value.captureRequestCount)
    }

    @Test
    fun exportWithoutPagesReportsError() {
        val service = FakeImagePdfExportService()
        val viewModel = ScanViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.exportScanToPdf {}

        assertEquals("Capture at least one page before exporting.", viewModel.uiState.value.exportErrorMessage)
        assertFalse(viewModel.uiState.value.isExporting)
        assertEquals(0, service.exportCalls)
    }

    @Test
    fun successfulExportClearsQueueAndCallsBack() = runTest(mainDispatcherRule.dispatcher) {
        val service = FakeImagePdfExportService()
        val viewModel = ScanViewModel(service, mainDispatcherRule.dispatcher)
        var exported: ExportedPdf? = null

        viewModel.recordImportedImages(listOf(uriOne to "one.jpg", uriTwo to "two.jpg"))
        viewModel.exportScanToPdf { exported = it }
        assertTrue(viewModel.uiState.value.isExporting)
        assertFalse(viewModel.uiState.value.canExport)

        advanceUntilIdle()

        assertSame(service.exportedPdf, exported)
        assertEquals(2, service.exportedPages.single().size)
        assertTrue(viewModel.uiState.value.capturedPages.isEmpty())
        assertFalse(viewModel.uiState.value.isExporting)
        assertNull(viewModel.uiState.value.exportErrorMessage)
    }

    @Test
    fun failedExportKeepsQueueAndReportsError() = runTest(mainDispatcherRule.dispatcher) {
        val service = FakeImagePdfExportService(error = IllegalStateException("PDF export failed"))
        val viewModel = ScanViewModel(service, mainDispatcherRule.dispatcher)

        viewModel.recordImportedImages(listOf(uriOne to "one.jpg"))
        viewModel.exportScanToPdf {}
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("PDF export failed", state.exportErrorMessage)
        assertEquals(1, state.capturedPages.size)
        assertFalse(state.isExporting)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScanMainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeImagePdfExportService(
    private val error: RuntimeException? = null,
) : ImagePdfExportService {
    private val uri: Uri = mock(Uri::class.java)
    val exportedPdf = ExportedPdf(uri, "SwiftPDF_scan.pdf", 4096L)
    val exportedPages = mutableListOf<List<CapturedScanPage>>()
    val exportCalls: Int
        get() = exportedPages.size

    override fun exportScanPages(pages: List<CapturedScanPage>): ExportedPdf {
        exportedPages += pages
        error?.let { throw it }
        return exportedPdf
    }
}
