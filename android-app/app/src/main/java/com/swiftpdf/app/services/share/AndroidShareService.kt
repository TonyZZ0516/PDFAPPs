package com.swiftpdf.app.services.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.services.pdf.ExportedImageSet
import java.io.File

class AndroidShareService(
    private val context: Context,
) : ShareService {
    override fun sharePdf(document: DocumentItem) {
        val shareUri = document.uri.toShareableUri()
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, document.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, "Share PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    override fun shareImages(imageSet: ExportedImageSet) {
        if (imageSet.uris.isEmpty()) return

        val shareUris = ArrayList(imageSet.uris.map { it.toShareableUri() })
        val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, shareUris)
            putExtra(Intent.EXTRA_SUBJECT, imageSet.displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, "Share Images").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun Uri.toShareableUri(): Uri {
        if (scheme != "file") return this

        val path = path ?: error("Unable to share this PDF.")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(path),
        )
    }
}
