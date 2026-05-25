package com.swiftpdf.app.services.pdf

import android.net.Uri

data class ExportedPdf(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long?,
)
