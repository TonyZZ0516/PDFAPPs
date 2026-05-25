package com.swiftpdf.app.services.pdf

import com.swiftpdf.app.domain.document.DocumentItem

interface PdfToolService {
    fun exportPagesAsImages(
        document: DocumentItem,
        options: ImageExportOptions = ImageExportOptions(),
    ): ExportedImageSet

    fun compressPdf(document: DocumentItem): ExportedPdf

    fun signPdf(document: DocumentItem, signatureMark: SignatureMark): ExportedPdf

    fun extractFirstPage(document: DocumentItem): ExportedPdf

    fun mergePdfs(documents: List<DocumentItem>): ExportedPdf
}
