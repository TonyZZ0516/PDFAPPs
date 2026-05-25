package com.swiftpdf.app.services.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.swiftpdf.app.domain.document.DocumentItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class AndroidPdfToolService(
    private val appContext: Context,
) : PdfToolService {
    override fun exportPagesAsImages(
        document: DocumentItem,
        options: ImageExportOptions,
    ): ExportedImageSet {
        val exportId = "SwiftPDF_images_${FileNameFormat.format(Date())}"
        val outputDir = File(appContext.cacheDir, "exported-images/$exportId").apply { mkdirs() }
        val imageUris = mutableListOf<Uri>()

        renderInput(document.uri) { renderer ->
            require(renderer.pageCount > 0) { "This PDF has no pages." }
            val pageIndexes = resolvePageIndexes(options.pageRangeText, renderer.pageCount)
            for (pageIndex in pageIndexes) {
                renderer.openPage(pageIndex).use { page ->
                    val bitmap = renderPageBitmap(page, ImageExportWidthPx)
                    val outputFile = File(
                        outputDir,
                        "${exportId}_page_${(pageIndex + 1).toString().padStart(2, '0')}.${options.outputFormat.extension}",
                    )
                    FileOutputStream(outputFile).use { output ->
                        val compressFormat = when (options.outputFormat) {
                            ImageOutputFormat.Png -> Bitmap.CompressFormat.PNG
                            ImageOutputFormat.Jpg -> Bitmap.CompressFormat.JPEG
                        }
                        bitmap.compress(compressFormat, 95, output)
                    }
                    bitmap.recycle()
                    imageUris += Uri.fromFile(outputFile)
                }
            }
        }

        return ExportedImageSet(
            uris = imageUris,
            displayName = exportId,
        )
    }

    private fun resolvePageIndexes(pageRangeText: String, pageCount: Int): List<Int> {
        val trimmedRange = pageRangeText.trim()
        if (trimmedRange.isBlank()) return (0 until pageCount).toList()

        val indexes = trimmedRange
            .split(",")
            .flatMap { token ->
                val trimmedToken = token.trim()
                when {
                    "-" in trimmedToken -> {
                        val parts = trimmedToken.split("-", limit = 2)
                        val start = parts.getOrNull(0)?.trim()?.toIntOrNull()
                        val end = parts.getOrNull(1)?.trim()?.toIntOrNull()
                        if (start == null || end == null) emptyList() else start..end
                    }
                    else -> {
                        val pageNumber = trimmedToken.toIntOrNull()
                        if (pageNumber == null) emptyList() else listOf(pageNumber)
                    }
                }
            }
            .map { pageNumber -> pageNumber - 1 }
            .filter { pageIndex -> pageIndex in 0 until pageCount }
            .distinct()

        require(indexes.isNotEmpty()) { "Enter a valid page range, for example 1 or 1-3." }
        return indexes
    }

    override fun compressPdf(document: DocumentItem): ExportedPdf {
        return writeRasterPdf(
            displayName = "SwiftPDF_compressed_${FileNameFormat.format(Date())}.pdf",
            documents = listOf(document),
            maxWidthPx = CompressedPdfWidthPx,
            firstPageOnly = false,
        )
    }

    override fun signPdf(document: DocumentItem, signatureMark: SignatureMark): ExportedPdf {
        return writeRasterPdf(
            displayName = "SwiftPDF_signed_${FileNameFormat.format(Date())}.pdf",
            documents = listOf(document),
            maxWidthPx = StandardPdfWidthPx,
            firstPageOnly = false,
            signatureMark = signatureMark,
        )
    }

    override fun extractFirstPage(document: DocumentItem): ExportedPdf {
        return writeRasterPdf(
            displayName = "SwiftPDF_page_1_${FileNameFormat.format(Date())}.pdf",
            documents = listOf(document),
            maxWidthPx = StandardPdfWidthPx,
            firstPageOnly = true,
        )
    }

    override fun mergePdfs(documents: List<DocumentItem>): ExportedPdf {
        require(documents.size >= 2) { "Select at least two PDFs to merge." }

        return writeRasterPdf(
            displayName = "SwiftPDF_merged_${FileNameFormat.format(Date())}.pdf",
            documents = documents,
            maxWidthPx = StandardPdfWidthPx,
            firstPageOnly = false,
        )
    }

    private fun writeRasterPdf(
        displayName: String,
        documents: List<DocumentItem>,
        maxWidthPx: Int,
        firstPageOnly: Boolean,
        signatureMark: SignatureMark? = null,
    ): ExportedPdf {
        val outputDir = File(appContext.filesDir, "exported-pdfs").apply { mkdirs() }
        val outputFile = File(outputDir, displayName)
        val outputDocument = PdfDocument()
        var outputPageNumber = 1

        try {
            documents.forEach { document ->
                renderInput(document.uri) { renderer ->
                    require(renderer.pageCount > 0) { "${document.displayName} has no pages." }

                    val pageRange = if (firstPageOnly) 0..0 else 0 until renderer.pageCount
                    for (pageIndex in pageRange) {
                        renderer.openPage(pageIndex).use { inputPage ->
                            val bitmap = renderPageBitmap(inputPage, maxWidthPx)
                            val pageInfo = PdfDocument.PageInfo.Builder(
                                inputPage.width,
                                inputPage.height,
                                outputPageNumber,
                            ).create()
                            val outputPage = outputDocument.startPage(pageInfo)
                            drawBitmapToPage(outputPage.canvas, bitmap, inputPage.width, inputPage.height)
                            if (outputPageNumber == 1 && signatureMark != null) {
                                drawSignatureStamp(
                                    canvas = outputPage.canvas,
                                    signatureMark = signatureMark,
                                    pageWidth = inputPage.width,
                                    pageHeight = inputPage.height,
                                )
                            }
                            outputDocument.finishPage(outputPage)
                            outputPageNumber += 1
                            bitmap.recycle()
                        }
                    }
                }
            }

            FileOutputStream(outputFile).use { output ->
                outputDocument.writeTo(output)
            }
        } finally {
            outputDocument.close()
        }

        return ExportedPdf(
            uri = Uri.fromFile(outputFile),
            displayName = displayName,
            sizeBytes = outputFile.length(),
        )
    }

    private fun renderInput(uri: Uri, block: (PdfRenderer) -> Unit) {
        openPdfFileDescriptor(uri).use { descriptor ->
            PdfRenderer(descriptor).use(block)
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

    private fun renderPageBitmap(page: PdfRenderer.Page, maxWidthPx: Int): Bitmap {
        val scale = (maxWidthPx.toFloat() / page.width.toFloat()).coerceAtMost(3.0f)
        val bitmapWidth = (page.width * scale).roundToInt().coerceAtLeast(1)
        val bitmapHeight = (page.height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    private fun drawBitmapToPage(
        canvas: android.graphics.Canvas,
        bitmap: Bitmap,
        pageWidth: Int,
        pageHeight: Int,
    ) {
        canvas.drawColor(Color.WHITE)
        val targetRect = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
        canvas.drawBitmap(
            bitmap,
            null,
            targetRect,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
    }

    private fun drawSignatureStamp(
        canvas: android.graphics.Canvas,
        signatureMark: SignatureMark,
        pageWidth: Int,
        pageHeight: Int,
    ) {
        val margin = pageWidth * 0.055f
        val scale = signatureMark.normalizedScale
        val boxWidth = pageWidth * 0.42f * scale
        val boxHeight = pageHeight * 0.095f * scale
        val (left, top) = when (signatureMark.placement) {
            SignaturePlacement.BottomRight -> pageWidth - margin - boxWidth to pageHeight - margin - boxHeight
            SignaturePlacement.BottomLeft -> margin to pageHeight - margin - boxHeight
            SignaturePlacement.Center -> (pageWidth - boxWidth) / 2f to (pageHeight - boxHeight) / 2f
        }
        val box = RectF(left, top, left + boxWidth, top + boxHeight)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (pageWidth * 0.0032f).coerceAtLeast(2f)
            color = Color.rgb(8, 127, 131)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(101, 113, 137)
            textSize = (pageWidth * 0.022f).coerceAtLeast(16f)
        }
        val signaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(23, 32, 51)
            textSize = (pageWidth * 0.04f).coerceAtLeast(26f)
            isFakeBoldText = true
        }
        val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(8, 127, 131)
            strokeWidth = (pageWidth * 0.0055f).coerceAtLeast(3f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(8, 127, 131)
            strokeWidth = (pageWidth * 0.0024f).coerceAtLeast(1.6f)
        }

        canvas.drawRoundRect(box, 10f, 10f, borderPaint)
        canvas.drawText("Signed by", box.left + boxWidth * 0.08f, box.top + boxHeight * 0.28f, labelPaint)
        val inkBox = RectF(
            box.left + boxWidth * 0.08f,
            box.top + boxHeight * 0.33f,
            box.right - boxWidth * 0.08f,
            box.top + boxHeight * 0.72f,
        )
        val signatureImage = signatureMark.imageUri
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
            ?.let(::decodeSignatureImage)
        if (signatureImage != null) {
            drawSignatureImage(canvas, signatureImage, inkBox)
            signatureImage.recycle()
        } else if (signatureMark.hasInk) {
            signatureMark.strokes.forEach { stroke ->
                stroke.points.zipWithNext { start, end ->
                    canvas.drawLine(
                        inkBox.left + start.x.coerceIn(0f, 1f) * inkBox.width(),
                        inkBox.top + start.y.coerceIn(0f, 1f) * inkBox.height(),
                        inkBox.left + end.x.coerceIn(0f, 1f) * inkBox.width(),
                        inkBox.top + end.y.coerceIn(0f, 1f) * inkBox.height(),
                        inkPaint,
                    )
                }
            }
        } else {
            canvas.drawText(
                signatureMark.signerName.orEmpty().take(MaxSignatureCharacters),
                box.left + boxWidth * 0.08f,
                box.top + boxHeight * 0.63f,
                signaturePaint,
            )
        }
        signatureMark.signerName?.trim()?.takeIf { it.isNotEmpty() }?.let { signerName ->
            canvas.drawText(
                signerName.take(MaxSignatureCharacters),
                box.left + boxWidth * 0.08f,
                box.top + boxHeight * 0.91f,
                labelPaint,
            )
        }
        canvas.drawLine(
            box.left + boxWidth * 0.08f,
            box.top + boxHeight * 0.76f,
            box.right - boxWidth * 0.08f,
            box.top + boxHeight * 0.76f,
            linePaint,
        )
    }

    private fun decodeSignatureImage(uri: Uri): Bitmap? {
        return runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
    }

    private fun drawSignatureImage(
        canvas: android.graphics.Canvas,
        bitmap: Bitmap,
        targetBox: RectF,
    ) {
        val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetAspect = targetBox.width() / targetBox.height()
        val fittedRect = if (imageAspect > targetAspect) {
            val height = targetBox.width() / imageAspect
            val top = targetBox.centerY() - height / 2f
            RectF(targetBox.left, top, targetBox.right, top + height)
        } else {
            val width = targetBox.height() * imageAspect
            val left = targetBox.centerX() - width / 2f
            RectF(left, targetBox.top, left + width, targetBox.bottom)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, fittedRect, paint)
    }

    private companion object {
        const val ImageExportWidthPx = 1440
        const val StandardPdfWidthPx = 1440
        const val CompressedPdfWidthPx = 900
        const val MaxSignatureCharacters = 32
        val FileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }
}
