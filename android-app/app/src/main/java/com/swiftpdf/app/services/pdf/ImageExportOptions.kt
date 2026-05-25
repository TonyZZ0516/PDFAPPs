package com.swiftpdf.app.services.pdf

data class ImageExportOptions(
    val pageRangeText: String = "",
    val outputFormat: ImageOutputFormat = ImageOutputFormat.Png,
)

enum class ImageOutputFormat(
    val extension: String,
) {
    Png("png"),
    Jpg("jpg"),
}
