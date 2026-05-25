package com.swiftpdf.app.services.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.swiftpdf.app.feature.scan.CapturedScanPage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class AndroidImagePdfExportService(
    private val appContext: Context,
) : ImagePdfExportService {
    override fun exportScanPages(pages: List<CapturedScanPage>): ExportedPdf {
        require(pages.isNotEmpty()) { "Capture at least one page before exporting." }

        val outputDir = File(appContext.filesDir, "exported-pdfs").apply { mkdirs() }
        val displayName = "SwiftPDF_scan_${FileNameFormat.format(Date())}.pdf"
        val outputFile = File(outputDir, displayName)

        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, page ->
                val bitmap = decodeBitmap(page.uri)
                    .autoCropIfNeeded(page.isAutoCropped)
                    .rotate(page.rotationDegrees)
                val pageInfo = PdfDocument.PageInfo.Builder(PageWidth, PageHeight, index + 1).create()
                val pdfPage = document.startPage(pageInfo)
                drawBitmapPage(pdfPage.canvas, bitmap)
                document.finishPage(pdfPage)
                bitmap.recycle()
            }

            FileOutputStream(outputFile).use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }

        return ExportedPdf(
            uri = Uri.fromFile(outputFile),
            displayName = displayName,
            sizeBytes = outputFile.length(),
        )
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        return appContext.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Unable to read captured scan page.")
    }

    private fun Bitmap.autoCropIfNeeded(isAutoCropped: Boolean): Bitmap {
        if (!isAutoCropped) return this
        if (width < 20 || height < 20) return this

        val cropRect = Rect(
            (width * AutoCropInsetRatio).toInt(),
            (height * AutoCropInsetRatio).toInt(),
            (width * (1f - AutoCropInsetRatio)).toInt(),
            (height * (1f - AutoCropInsetRatio)).toInt(),
        )
        val croppedBitmap = Bitmap.createBitmap(
            this,
            cropRect.left,
            cropRect.top,
            cropRect.width().coerceAtLeast(1),
            cropRect.height().coerceAtLeast(1),
        )
        if (croppedBitmap != this) recycle()
        return croppedBitmap
    }

    private fun Bitmap.rotate(rotationDegrees: Int): Bitmap {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        if (normalizedRotation == 0) return this

        val matrix = Matrix().apply {
            postRotate(normalizedRotation.toFloat())
        }
        val rotatedBitmap = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        if (rotatedBitmap != this) recycle()
        return rotatedBitmap
    }

    private fun drawBitmapPage(canvas: android.graphics.Canvas, bitmap: Bitmap) {
        canvas.drawColor(Color.WHITE)

        val availableWidth = PageWidth - Margin * 2
        val availableHeight = PageHeight - Margin * 2
        val scale = min(
            availableWidth.toFloat() / bitmap.width.toFloat(),
            availableHeight.toFloat() / bitmap.height.toFloat(),
        )
        val targetWidth = bitmap.width * scale
        val targetHeight = bitmap.height * scale
        val left = (PageWidth - targetWidth) / 2f
        val top = (PageHeight - targetHeight) / 2f
        val rect = android.graphics.RectF(left, top, left + targetWidth, top + targetHeight)

        canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private companion object {
        const val PageWidth = 595
        const val PageHeight = 842
        const val Margin = 32
        const val AutoCropInsetRatio = 0.06f
        val FileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }
}
