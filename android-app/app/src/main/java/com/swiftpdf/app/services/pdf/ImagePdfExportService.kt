package com.swiftpdf.app.services.pdf

import com.swiftpdf.app.feature.scan.CapturedScanPage

interface ImagePdfExportService {
    fun exportScanPages(pages: List<CapturedScanPage>): ExportedPdf
}
