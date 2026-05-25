package com.swiftpdf.app.feature.export

import com.swiftpdf.app.services.pdf.ExportedImageSet

data class ExportUiState(
    val isWorking: Boolean = false,
    val activeOperationLabel: String? = null,
    val statusMessage: String? = null,
    val successTitle: String? = null,
    val successFileName: String? = null,
    val successDetail: String? = null,
    val errorMessage: String? = null,
    val exportedImageSet: ExportedImageSet? = null,
) {
    val canRunTool: Boolean = !isWorking
}
