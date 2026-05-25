package com.swiftpdf.app.services.pdf

import android.net.Uri

interface PdfRendererService {
    fun renderPage(uri: Uri, pageIndex: Int, maxWidthPx: Int = 1400): PdfPreview
}
