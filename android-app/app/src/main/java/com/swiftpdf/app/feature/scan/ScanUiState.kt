package com.swiftpdf.app.feature.scan

data class ScanUiState(
    val cameraPermissionStatus: CameraPermissionStatus = CameraPermissionStatus.Unknown,
    val capturedPages: List<CapturedScanPage> = emptyList(),
    val captureErrorMessage: String? = null,
    val captureRequestCount: Int = 0,
    val isExporting: Boolean = false,
    val exportErrorMessage: String? = null,
) {
    val hasCameraPermission: Boolean = cameraPermissionStatus == CameraPermissionStatus.Granted
    val captureCount: Int = capturedPages.size
    val latestCapturedPage: CapturedScanPage? = capturedPages.lastOrNull()
    val canExport: Boolean = capturedPages.isNotEmpty() && !isExporting
}
