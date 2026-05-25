package com.swiftpdf.app.services.pdf

data class SignatureMark(
    val strokes: List<SignatureStroke>,
    val signerName: String?,
    val imageUri: String? = null,
    val placement: SignaturePlacement = SignaturePlacement.BottomRight,
    val scale: Float = 1.0f,
) {
    val hasInk: Boolean = strokes.any { it.points.size >= 2 }
    val hasImage: Boolean = !imageUri.isNullOrBlank()
    val hasContent: Boolean = hasInk || hasImage || !signerName.isNullOrBlank()
    val normalizedScale: Float = scale.coerceIn(MinScale, MaxScale)

    companion object {
        const val MinScale = 0.75f
        const val MaxScale = 1.35f
    }
}

enum class SignaturePlacement {
    BottomRight,
    Center,
    BottomLeft,
}

data class SignatureStroke(
    val points: List<SignaturePoint>,
)

data class SignaturePoint(
    val x: Float,
    val y: Float,
)
