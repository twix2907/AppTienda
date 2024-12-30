import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import android.view.MotionEvent
import androidx.camera.core.AspectRatio
import androidx.camera.core.FocusMeteringAction
import java.util.concurrent.TimeUnit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProvider = remember { ProcessCameraProvider.getInstance(context) }
    var isTorchOn by remember { mutableStateOf(true) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        scaleType = PreviewView.ScaleType.FIT_CENTER  // Este es el cambio más importante
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { previewView ->
                cameraProvider.addListener({
                    val provider = cameraProvider.get()

                    val preview = Preview.Builder()
                        .setTargetResolution(Size(1080, 1080)) // Cambiamos a 4:3 que es el típico de fotos
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1080, 1080))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(
                                ContextCompat.getMainExecutor(context)
                            ) { imageProxy ->
                                imageProxy.image?.let { image ->
                                    val inputImage = InputImage.fromMediaImage(
                                        image,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    BarcodeScanning.getClient().process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull()?.rawValue?.let { value ->
                                                onBarcodeDetected(value)
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                }
                            }
                        }

                    try {
                        provider.unbindAll()

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )

                        // Configurar enfoque automático inicial
                        val initialFocusPoint = previewView.meteringPointFactory.createPoint(
                            previewView.width / 2f,
                            previewView.height / 2f
                        )

                        val initialFocusAction = FocusMeteringAction.Builder(initialFocusPoint)
                            .setAutoCancelDuration(2, TimeUnit.SECONDS)
                            .build()

                        camera?.cameraControl?.startFocusAndMetering(initialFocusAction)

                        // Configurar tap para enfocar
                        previewView.setOnTouchListener { _, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    val point = previewView.meteringPointFactory.createPoint(
                                        event.x,
                                        event.y
                                    )
                                    val action = FocusMeteringAction.Builder(point)
                                        .setAutoCancelDuration(2, TimeUnit.SECONDS)
                                        .build()
                                    camera?.cameraControl?.startFocusAndMetering(action)
                                    true
                                }
                                else -> false
                            }
                        }
                        camera?.cameraControl?.setZoomRatio(0.1f)
                        camera?.cameraControl?.setLinearZoom(0.0f)  // 0.0f es el zoom más alejado
                        camera?.cameraControl?.enableTorch(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }


        }

        // Botones en la parte superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Botón de linterna
            IconButton(
                onClick = {
                    isTorchOn = !isTorchOn
                    camera?.cameraControl?.enableTorch(isTorchOn)
                }
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.AddCircle else Icons.Default.Build,
                    contentDescription = if (isTorchOn) "Apagar linterna" else "Encender linterna",
                    tint = Color.White
                )
            }

            // Botón de cerrar
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }
    }
}