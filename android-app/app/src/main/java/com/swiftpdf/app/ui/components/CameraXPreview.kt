package com.swiftpdf.app.ui.components

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun CameraXPreview(
    captureRequestCount: Int,
    onCaptureSaved: (Uri, String) -> Unit,
    onCaptureError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                )
                imageCapture = capture
                cameraError = null
            }.onFailure { throwable ->
                cameraError = throwable.message ?: "Unable to start camera preview."
            }
        }

        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    LaunchedEffect(captureRequestCount) {
        if (captureRequestCount <= 0) return@LaunchedEffect
        val capture = imageCapture ?: run {
            onCaptureError("Camera is not ready yet.")
            return@LaunchedEffect
        }
        captureScanPage(
            context = context,
            imageCapture = capture,
            onCaptureSaved = onCaptureSaved,
            onCaptureError = onCaptureError,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.matchParentSize(),
        )
        if (cameraError != null) {
            Text(
                text = cameraError.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun captureScanPage(
    context: Context,
    imageCapture: ImageCapture,
    onCaptureSaved: (Uri, String) -> Unit,
    onCaptureError: (String) -> Unit,
) {
    val scansDir = File(context.cacheDir, "scan-pages").apply {
        mkdirs()
    }
    val displayName = "scan_${System.currentTimeMillis()}.jpg"
    val outputFile = File(scansDir, displayName)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onCaptureSaved(Uri.fromFile(outputFile), displayName)
            }

            override fun onError(exception: ImageCaptureException) {
                onCaptureError(exception.message ?: "Unable to capture this page.")
            }
        },
    )
}
