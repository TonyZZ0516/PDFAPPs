package com.swiftpdf.app.domain.document

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormatTest {
    @Test
    fun formatsUnknownSize() {
        val size: Long? = null

        assertEquals("Unknown size", size.toReadableFileSize())
    }

    @Test
    fun formatsBytes() {
        assertEquals("512 B", 512L.toReadableFileSize())
    }

    @Test
    fun formatsKilobytes() {
        assertEquals("1.5 KB", 1536L.toReadableFileSize())
    }

    @Test
    fun formatsMegabytes() {
        assertEquals("2.0 MB", (2L * 1024L * 1024L).toReadableFileSize())
    }
}
