package com.example.easyprice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class ScannerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (allPermissionsGranted()) {
            setContent {
                ScannerScreen(onCancel = { finish() })
            }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setContent {
                    ScannerScreen(onCancel = { finish() })
                }
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun ScannerScreen(onCancel: () -> Unit) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var flashOn by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 📷 Camera
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({

                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val scanner = BarcodeScanning.getClient()

                    analyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { result ->
                                        val intent = Intent()
                                        intent.putExtra("barcode_result", result)
                                        (context as ComponentActivity).setResult(
                                            Activity.RESULT_OK,
                                            intent
                                        )
                                        (context as ComponentActivity).finish()
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        }
                    }

                    val selector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            analyzer
                        )
                    } catch (e: Exception) {
                        Log.e("ScannerScreen", "Use case binding failed", e)
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // 🔵 Overlay oscuro
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA1F2A36))
        )

        // 🟡 Marco escaneo
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
                .border(3.dp, Color.Yellow, RoundedCornerShape(12.dp))
        )

        // 🔰 Logo arriba
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo",
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )

        // 🔘 Botones inferiores
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4C430)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(150.dp)
                    .height(60.dp)
            ) {
                Text("Cancelar", color = Color.Black)
            }

            Button(
                onClick = {
                    flashOn = !flashOn
                    camera?.cameraControl?.enableTorch(flashOn)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4C430)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(150.dp)
                    .height(60.dp)
            ) {
                Text("Flash", color = Color.Black)
            }
        }
    }
}
