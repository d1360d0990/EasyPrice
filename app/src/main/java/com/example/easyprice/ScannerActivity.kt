package com.example.easyprice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
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

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setContent {
                ScannerScreen(onCancel = { finish() })
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                10
            )
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun ScannerScreen(onCancel: () -> Unit) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera: Camera? by remember { mutableStateOf(null) }
    var flashOn by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) }
    var isCameraReady by remember { mutableStateOf(false) }
    var isLoadingResult by remember { mutableStateOf(false) }

    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.beep) }

    val scannerSize = 260.dp
    val laserHeight = 6.dp // más alto para el glow

    /* 🔴 ANIMACIÓN DEL LÁSER */
    val infiniteTransition = rememberInfiniteTransition(label = "laser")

    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    val laserMaxOffset = scannerSize - laserHeight
    val laserY = laserMaxOffset * laserProgress

    Box(modifier = Modifier.fillMaxSize()) {

        /* 📷 CÁMARA */
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

                    analyzer.setAnalyzer(
                        ContextCompat.getMainExecutor(ctx)
                    ) { imageProxy ->

                        if (!isScanning) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { result ->
                                        if (isScanning) {
                                            isScanning = false
                                            isLoadingResult = true
                                            mediaPlayer.start()

                                            val intent = Intent(context, Result::class.java)
                                            intent.putExtra("barcode", result)
                                            context.startActivity(intent)
                                            (context as? Activity)?.finish()
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analyzer
                        )
                        isCameraReady = true
                    } catch (e: Exception) {
                        Log.e("Scanner", "Camera error", e)
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        if (isCameraReady) {
            /* 🔲 OVERLAY OSCURO */
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000))
            )

            /* 🟡 MARCO DE ESCANEO */
            Box(
                modifier = Modifier
                    .size(scannerSize)
                    .align(Alignment.Center)
                    .border(3.dp, Color.Yellow, RoundedCornerShape(12.dp))
                    .clipToBounds()
            ) {

                /* 🔥 LÁSER CON EFECTO GLOW */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(laserHeight)
                        .offset(y = laserY)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Red,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        /* 🔰 LOGO */
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo",
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )

        /* 🔘 BOTONES */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .width(150.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF4C430)
                )
            ) {
                Text("Cancelar", color = Color.Black)
            }

            Button(
                onClick = {
                    flashOn = !flashOn
                    camera?.cameraControl?.enableTorch(flashOn)
                },
                modifier = Modifier
                    .width(150.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF4C430)
                )
            ) {
                Text("Flash", color = Color.Black)
            }
        }

        // Indicadores de progreso
        if (!isCameraReady) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1E2A35)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.Yellow)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Iniciando cámara...", color = Color.White)
                }
            }
        } else if (isLoadingResult) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xAA000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.Yellow)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Procesando...", color = Color.White)
                }
            }
        }
    }
}