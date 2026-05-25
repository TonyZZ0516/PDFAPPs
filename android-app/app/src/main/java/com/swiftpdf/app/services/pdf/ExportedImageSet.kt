package com.swiftpdf.app.services.pdf

import android.net.Uri

data class ExportedImageSet(
    val uris: List<Uri>,
    val displayName: String,
)
