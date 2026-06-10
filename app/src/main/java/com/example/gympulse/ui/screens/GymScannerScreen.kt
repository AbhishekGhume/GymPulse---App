package com.example.gympulse.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.gympulse.ui.theme.BebasNeue
import com.example.gympulse.ui.theme.DMSans
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun GymScannerScreen(onJoinSuccess: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val brandOrange = Color(0xFFFF5722)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var manualCode by remember { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Dynamic Permission Requester Launcher Hook
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Laser Animation Configurations
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 240f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "laserPosition"
    )

    // Handshake Database Operation to Connect Member with Gym Document Map
    fun verifyAndLinkGym(token: String) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return

        db.collection("gyms")
            .whereEqualTo("qrJoinToken", cleanToken)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val gymDocument = querySnapshot.documents[0]
                    val gymId = gymDocument.getString("id") ?: ""
                    val gymName = gymDocument.getString("gymName") ?: "your gym"
                    val userUid = auth.currentUser?.uid

                    if (userUid != null) {
                        db.collection("users").document(userUid)
                            .update("gymId", gymId)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Linked to $gymName Successfully!", Toast.LENGTH_SHORT).show()
                                onJoinSuccess()
                            }
                    }
                } else {
                    Toast.makeText(context, "Invalid Gym Token code.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Link failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF241713), Color(0xFF111215))))
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Branding Headers
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("LINK YOUR GYM", fontFamily = BebasNeue, fontSize = 32.sp, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(brandOrange))
            Text(
                "Scan the official onboarding QR Code displayed at your gym's front desk to sync your training routines.",
                fontFamily = DMSans, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Camera Feed Portal Core Block Window frame
        Box(
            modifier = Modifier
                .size(260.dp)
                .border(2.dp, brandOrange, RoundedCornerShape(16.dp))
                .background(Color.Black, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val executor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            // Setup Google Vision Scanner properties targeting QR codes format explicitly
                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                            val scanner = BarcodeScanning.getClient(options)

                            var isScanningActive = true

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty() && isScanningActive) {
                                                val qrContent = barcodes[0].rawValue ?: ""
                                                if (qrContent.isNotBlank()) {
                                                    isScanningActive = false // Throttles execution runs loops
                                                    previewView.post {
                                                        verifyAndLinkGym(qrContent)
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission required", fontFamily = DMSans, color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Animated Laser Beam overlay element
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .offset(y = laserYOffset.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, brandOrange, Color.Transparent)))
            )
        }

        // Manual Fallback Card Entry Form Submodule
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("OR ENTER ASSIGNED TOKEN MANUALLY", fontFamily = BebasNeue, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = manualCode, onValueChange = { manualCode = it },
                        placeholder = { Text("e.g. powerhouse-pune-4281", color = Color.DarkGray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                    )
                    Button(
                        onClick = { verifyAndLinkGym(manualCode) },
                        colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("LINK", fontFamily = BebasNeue)
                    }
                }
            }
        }
    }
}