package com.focal.app.ui.components

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
actual fun PlatformCameraScanner(
    modifier: Modifier,
    torchEnabled: Boolean,
    isScanning: Boolean,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }
    val zxingReader = remember {
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE)
                )
            )
        }
    }

    LaunchedEffect(torchEnabled, boundCamera) {
        try {
            if (boundCamera?.cameraInfo?.hasFlashUnit() == true) {
                boundCamera?.cameraControl?.enableTorch(torchEnabled)
            }
        } catch (_: Exception) {}
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                boundCamera?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    if (!hasCameraPermission) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.TextButton(
                onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }
            ) {
                androidx.compose.material3.Text(
                    text = "Grant camera permission to scan",
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (!isScanning) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                processImage(barcodeScanner, zxingReader, imageProxy) { scannedText ->
                                    if (isScanning) {
                                        PlatformPermissionsHelper.vibrate(50)
                                        onQrDetected(scannedText)
                                    }
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    val cam = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                    boundCamera = cam
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImage(
    scanner: BarcodeScanner,
    zxingReader: MultiFormatReader,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                var found = false
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        found = true
                        onSuccess(rawValue)
                        break
                    }
                }
                if (!found) {
                    tryDecodeZxing(zxingReader, imageProxy, onSuccess)
                }
            }
            .addOnFailureListener {
                tryDecodeZxing(zxingReader, imageProxy, onSuccess)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        tryDecodeZxing(zxingReader, imageProxy, onSuccess)
        imageProxy.close()
    }
}

private fun tryDecodeZxing(
    reader: MultiFormatReader,
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    try {
        val plane = imageProxy.planes.getOrNull(0) ?: return
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val width = imageProxy.width
        val height = imageProxy.height
        val source = PlanarYUVLuminanceSource(
            bytes, width, height, 0, 0, width, height, false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val result = reader.decodeWithState(bitmap)
        if (!result.text.isNullOrBlank()) {
            onSuccess(result.text)
        }
    } catch (_: Exception) {
    } finally {
        reader.reset()
    }
}
