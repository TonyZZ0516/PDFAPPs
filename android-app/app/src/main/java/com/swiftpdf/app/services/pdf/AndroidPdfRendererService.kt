package com.swiftpdf.app.services.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.roundToInt

class AndroidPdfRendererService(
    private val appContext: Context,
) : PdfRendererService {
    override fun renderPage(uri: Uri, pageIndex: Int, maxWidthPx: Int): PdfPreview {
        val fileDescriptor = openPdfFileDescriptor(uri)

        return fileDescriptor.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount <= 0) {
                    error("This PDF has no pages.")
                }
                val safePageIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)

                renderer.openPage(safePageIndex).use { page ->
                    val scale = (maxWidthPx.toFloat() / page.width.toFloat()).coerceAtMost(2.0f)
                    val bitmapWidth = (page.width * scale).roundToInt().coerceAtLeast(1)
                    val bitmapHeight = (page.height * scale).roundToInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(
                        bitmapWidth,
                        bitmapHeight,
                        Bitmap.Config.ARGB_8888,
                    )

                    bitmap.eraseColor(Color.WHITE)
                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                    )

                    PdfPreview(
                        pageBitmap = bitmap,
                        pageIndex = safePageIndex,
                        pageCount = renderer.pageCount,
                    )
                }
            }
        }
    }

    private fun openPdfFileDescriptor(uri: Uri): ParcelFileDescriptor {
        if (uri.scheme == "file") {
            val path = uri.path ?: error("Unable to open generated PDF.")
            return ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        }

        return appContext.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Unable to open selected PDF.")
    }
}
