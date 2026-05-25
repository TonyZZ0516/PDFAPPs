package com.swiftpdf.app.feature.scan

import android.net.Uri

data class CapturedScanPage(
    val uri: Uri,
    val displayName: String,
    val capturedAtMillis: Long,
    val rotationDegrees: Int = 0,
    val isAutoCropped: Boolean = false,
)
