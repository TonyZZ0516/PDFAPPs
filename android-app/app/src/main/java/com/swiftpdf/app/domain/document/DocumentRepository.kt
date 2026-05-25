package com.swiftpdf.app.domain.document

import android.net.Uri

interface DocumentRepository {
    fun importPdf(uri: Uri): DocumentItem
    fun registerGeneratedPdf(uri: Uri, displayName: String, sizeBytes: Long? = null): DocumentItem
}
