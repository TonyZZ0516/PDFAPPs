package com.swiftpdf.app.services.pdf

import android.graphics.Bitmap

data class PdfPreview(
    val pageBitmap: Bitmap,
    val pageIndex: Int,
    val pageCount: Int,
)
