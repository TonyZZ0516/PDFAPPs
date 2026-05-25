package com.swiftpdf.app.services.share

import com.swiftpdf.app.domain.document.DocumentItem
import com.swiftpdf.app.services.pdf.ExportedImageSet

interface ShareService {
    fun sharePdf(document: DocumentItem)

    fun shareImages(imageSet: ExportedImageSet)
}
