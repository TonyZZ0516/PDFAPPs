package com.swiftpdf.app.data.document

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.domain.document.DocumentRepository
import java.io.File

class AndroidDocumentRepository(
    private val appContext: Context,
) : DocumentRepository {
    override fun importPdf(uri: Uri): DocumentItem {
        appContext.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )

        val metadata = readMetadata(uri)
        return DocumentItem(
            uri = uri,
            displayName = metadata.displayName ?: uri.lastPathSegment ?: "Untitled PDF",
            sizeBytes = metadata.sizeBytes,
            importedAtMillis = System.currentTimeMillis(),
        )
    }

    override fun registerGeneratedPdf(
        uri: Uri,
        displayName: String,
        sizeBytes: Long?,
    ): DocumentItem {
        return DocumentItem(
            uri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes ?: readLocalFileSize(uri),
            importedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun readMetadata(uri: Uri): DocumentMetadata {
        var displayName: String? = null
        var sizeBytes: Long? = null

        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    displayName = cursor.getString(nameIndex)
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        return DocumentMetadata(displayName, sizeBytes)
    }
}

private fun readLocalFileSize(uri: Uri): Long? {
    if (uri.scheme != "file") return null
    return uri.path?.let { path ->
        File(path).takeIf { it.exists() }?.length()
    }
}

private data class DocumentMetadata(
    val displayName: String?,
    val sizeBytes: Long?,
)
