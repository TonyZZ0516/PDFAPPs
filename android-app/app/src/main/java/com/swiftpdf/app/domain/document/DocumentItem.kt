package com.swiftpdf.app.domain.document

import android.net.Uri
import java.text.DateFormat
import java.util.Date

data class DocumentItem(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long?,
    val importedAtMillis: Long,
)

fun Long?.toReadableFileSize(): String {
    val size = this ?: return "Unknown size"
    if (size < 1024) return "$size B"

    val units = listOf("KB", "MB", "GB")
    var value = size.toDouble() / 1024.0
    var unitIndex = 0

    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }

    return "%.1f %s".format(value, units[unitIndex])
}

fun Long.toReadableDateTime(): String {
    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
    ).format(Date(this))
}
