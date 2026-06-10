package com.example.gympulse.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gympulse.ui.theme.BebasNeue
import com.example.gympulse.ui.theme.DMSans
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream
import java.util.Locale

@Composable
fun OwnerDashboardScreen(onLogoutSuccess: () -> Unit) {
    val brandOrange = Color(0xFFFF5722)
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Studio Profile Information States
    var ownerName by remember { mutableStateOf("ADMIN") }
    var gymName by remember { mutableStateOf("POWERHOUSE GYM") }
    var qrJoinToken by remember { mutableStateOf("") }
    var gymId by remember { mutableStateOf("") }

    // Matrix Bitmap Graphic State
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingQr by remember { mutableStateOf(true) }

    // Dialog state management for enlarging the QR code
    var isQrDialogOpen by remember { mutableStateOf(false) }

    // Dynamic Live Athlete Stream State
    val activeMembersList = remember { mutableStateListOf<Pair<String, String>>() }

    // Helper Engine Function to Convert String Token into a Visual Bitmap Grid Matrix
    fun generateQrCodeBitmap(text: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Composites Gym Context Details onto a single printable flyer image template
    fun createPrintableQrFlyer(qrBitmap: Bitmap, gymTitle: String, tokenCode: String): Bitmap {
        val flyerWidth = 800
        val flyerHeight = 1100
        val compositeBitmap = Bitmap.createBitmap(flyerWidth, flyerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(compositeBitmap)

        // 1. Draw solid pristine background
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        // 2. Render branding app subhead header
        paint.textSize = 28f
        paint.color = android.graphics.Color.parseColor("#FF5722") // Brand Orange Accent
        canvas.drawText("POWERED BY GYMPULSE", flyerWidth / 2f, 90f, paint)

        // 3. Render Custom Gym Name Header
        paint.textSize = 48f
        paint.color = android.graphics.Color.BLACK
        paint.isFakeBoldText = true
        val titleBounds = Rect()
        paint.getTextBounds(gymTitle, 0, gymTitle.length, titleBounds)
        canvas.drawText(gymTitle, flyerWidth / 2f, 170f, paint)

        // 4. Draw Divider Separation Rules
        paint.isFakeBoldText = false
        paint.strokeWidth = 3f
        paint.color = android.graphics.Color.parseColor("#E0E0E0")
        canvas.drawLine(100f, 220f, flyerWidth - 100f, 220f, paint)

        // 5. Draw Core instructions callouts
        paint.color = android.graphics.Color.DKGRAY
        paint.textSize = 24f
        canvas.drawText("Scan this QR Code via the GymPulse App to link your workout profile", flyerWidth / 2f, 280f, paint)

        // 6. Draw the scaled centralized QR code matrix graphic
        val qrSize = 460
        val left = (flyerWidth - qrSize) / 2
        val top = 340
        val srcRect = Rect(0, 0, qrBitmap.width, qrBitmap.height)
        val dstRect = Rect(left, top, left + qrSize, top + qrSize)
        canvas.drawBitmap(qrBitmap, srcRect, dstRect, null)

        // 7. Draw Divider under QR
        paint.color = android.graphics.Color.parseColor("#E0E0E0")
        canvas.drawLine(100f, 850f, flyerWidth - 100f, 850f, paint)

        // 8. Add Fail-safe Token ID manual fallback notice messaging fields
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 26f
        canvas.drawText("QR Code not working? Enter this token ID manually:", flyerWidth / 2f, 910f, paint)

        paint.textSize = 36f
        paint.color = android.graphics.Color.parseColor("#FF5722") // Brand Orange Focus Accent
        paint.isFakeBoldText = true
        canvas.drawText(tokenCode, flyerWidth / 2f, 980f, paint)

        return compositeBitmap
    }

    // Direct platform MediaStore shared directory gallery storage exporter hook pipeline
    fun saveQrToGallery(context: Context, qrBitmap: Bitmap, gymTitle: String, tokenCode: String, filename: String) {
        val resolver = context.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GymPulse")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri: Uri? = resolver.insert(imageCollection, contentValues)

        if (imageUri != null) {
            try {
                // Generate composite graphic dynamic template flyer directly prior to storage execution write handshakes
                val compositeFlyer = createPrintableQrFlyer(qrBitmap, gymTitle, tokenCode)

                val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
                if (outputStream != null) {
                    compositeFlyer.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.close()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                Toast.makeText(context, "Flyer saved with Gym details & Token!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to build printable file asset.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Failed to initialize storage destination pointer.", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch Owner & Gym Metadata Profiles
    LaunchedEffect(Unit) {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { userDoc ->
                    userDoc.getString("name")?.let {
                        ownerName = it.uppercase(Locale.ROOT)
                    }
                }

            db.collection("gyms").document(currentUid).get()
                .addOnSuccessListener { gymDoc ->
                    if (gymDoc.exists()) {
                        val name = gymDoc.getString("gymName") ?: "POWERHOUSE GYM"
                        val token = gymDoc.getString("qrJoinToken") ?: ""
                        gymId = gymDoc.getString("id") ?: currentUid

                        gymName = name.uppercase(Locale.ROOT)
                        qrJoinToken = token

                        if (token.isNotBlank()) {
                            qrBitmap = generateQrCodeBitmap(token)
                        }
                    }
                    isLoadingQr = false
                }
                .addOnFailureListener { e ->
                    isLoadingQr = false
                    Toast.makeText(context, "Failed to sync dashboard: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } else {
            isLoadingQr = false
        }
    }

    // Live Snapshot Real-time Listener for Active Members under this Gym ID
    LaunchedEffect(gymId) {
        if (gymId.isNotBlank()) {
            db.collection("users")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    if (snapshot != null) {
                        activeMembersList.clear()
                        for (doc in snapshot.documents) {
                            val name = doc.getString("name") ?: "Anonymous Athlete"
                            val currentRoutinePlaceholder = "Push Day v1"
                            activeMembersList.add(name to currentRoutinePlaceholder)
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111215))
    ) {
        // Top Toolbar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16171D))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(gymName, fontFamily = BebasNeue, fontSize = 24.sp, color = Color.White)
                Text("MANAGEMENT PORTAL • MGR: $ownerName", fontFamily = DMSans, fontSize = 10.sp, color = Color.Gray, letterSpacing = 0.5.sp)
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFF24252C), RoundedCornerShape(6.dp))
                    .clickable {
                        auth.signOut()
                        onLogoutSuccess()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("LOGOUT", fontFamily = BebasNeue, fontSize = 11.sp, color = Color.LightGray)
            }
        }

        // Main Stream Body content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val totalMembersString = "${activeMembersList.size}\nActive Members"
                val metrics = listOf(totalMembersString, "₹84.5k\nRevenue", "8\nRoutines")

                metrics.forEach { data ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                        border = BorderStroke(1.dp, Color(0xFF23242A))
                    ) {
                        Text(
                            text = data, fontFamily = BebasNeue, fontSize = 15.sp,
                            color = Color.White, modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // QR code display overview card module block container
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                border = BorderStroke(1.dp, Color(0xFF23242A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("STUDIO ONBOARDING QR CODE", fontFamily = BebasNeue, fontSize = 18.sp, color = Color.White)
                        Text(
                            text = "Display this code at the front desk. Tap the preview image to expand and download a printable crisp layout template containing the manual fallback token.",
                            fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)
                        )
                        if (qrJoinToken.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Token ID: $qrJoinToken",
                                fontFamily = BebasNeue, fontSize = 14.sp, color = brandOrange, letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .clickable(enabled = qrBitmap != null) { isQrDialogOpen = true }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingQr) {
                            CircularProgressIndicator(color = brandOrange, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Gym Onboarding QR Code Preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("⚠️", fontSize = 20.sp)
                        }
                    }
                }
            }

            Text("LIVE GYM FLOOR ACTIVITY", fontFamily = BebasNeue, fontSize = 14.sp, color = Color.Gray, letterSpacing = 1.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                border = BorderStroke(1.dp, Color(0xFF23242A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (activeMembersList.isEmpty()) {
                        Text(
                            text = "No athletes checked in yet. Waiting for front desk QR scans...",
                            fontFamily = DMSans, fontSize = 12.sp, color = Color.DarkGray,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                        )
                    } else {
                        activeMembersList.forEach { (name, routine) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(name, fontFamily = DMSans, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(routine, fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(brandOrange.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("ACTIVE", fontFamily = BebasNeue, fontSize = 11.sp, color = brandOrange, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal workspace dialog window engine overlay layout
    if (isQrDialogOpen && qrBitmap != null) {
        Dialog(onDismissRequest = { isQrDialogOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16171D)),
                border = BorderStroke(1.dp, Color(0xFF23242A)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = gymName, fontFamily = BebasNeue, fontSize = 24.sp, color = Color.White, textAlign = TextAlign.Center)
                    Text(text = "FRONT DESK REGISTRATION HUB", fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray, letterSpacing = 0.5.sp)

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Enlarged Studio Registration Key Matrix",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live UI fallback hint message label displayed inside preview window bounds
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1C24)),
                        border = BorderStroke(0.5.dp, Color.DarkGray)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("MANUAL ENTRY BACKUP KEY", fontFamily = BebasNeue, fontSize = 11.sp, color = Color.Gray)
                            Text(text = qrJoinToken, fontFamily = BebasNeue, fontSize = 18.sp, color = brandOrange, modifier = Modifier.padding(top = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            saveQrToGallery(context, qrBitmap!!, gymName, qrJoinToken, "GymPulse_${qrJoinToken}")
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DOWNLOAD ONBOARDING FLYER", fontFamily = BebasNeue, fontSize = 16.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { isQrDialogOpen = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLOSE PREVIEW", fontFamily = BebasNeue, fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

data class GymProfileData(val gymName: String, val token: String)