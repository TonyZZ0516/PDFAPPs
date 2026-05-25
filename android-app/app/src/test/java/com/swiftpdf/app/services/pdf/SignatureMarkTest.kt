package com.swiftpdf.app.services.pdf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureMarkTest {
    @Test
    fun emptySignatureHasNoContent() {
        val signature = SignatureMark(strokes = emptyList(), signerName = null)

        assertFalse(signature.hasInk)
        assertFalse(signature.hasContent)
    }

    @Test
    fun signerNameCountsAsSignatureContent() {
        val signature = SignatureMark(strokes = emptyList(), signerName = "MOBU")

        assertFalse(signature.hasInk)
        assertTrue(signature.hasContent)
    }

    @Test
    fun strokeWithTwoPointsCountsAsInk() {
        val signature = SignatureMark(
            strokes = listOf(
                SignatureStroke(
                    points = listOf(
                        SignaturePoint(0.1f, 0.2f),
                        SignaturePoint(0.8f, 0.6f),
                    ),
                ),
            ),
            signerName = null,
        )

        assertTrue(signature.hasInk)
        assertTrue(signature.hasContent)
    }

    @Test
    fun imageUriCountsAsSignatureContent() {
        val signature = SignatureMark(
            strokes = emptyList(),
            signerName = null,
            imageUri = "content://signature/image.png",
        )

        assertTrue(signature.hasImage)
        assertTrue(signature.hasContent)
    }

    @Test
    fun signatureScaleIsClampedForPdfPlacement() {
        val smallSignature = SignatureMark(strokes = emptyList(), signerName = "MOBU", scale = 0.1f)
        val largeSignature = SignatureMark(strokes = emptyList(), signerName = "MOBU", scale = 5.0f)

        assertTrue(smallSignature.normalizedScale >= SignatureMark.MinScale)
        assertTrue(largeSignature.normalizedScale <= SignatureMark.MaxScale)
    }
}
