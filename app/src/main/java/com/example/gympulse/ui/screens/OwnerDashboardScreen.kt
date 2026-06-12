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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gympulse.ui.theme.BebasNeue
import com.example.gympulse.ui.theme.DMSans
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream
import java.util.Locale

// Data representation for Custom Routines awaiting Verification
data class CustomPlanRequest(
    val id: String,
    val memberUid: String,
    val memberName: String,
    val planName: String,
    val totalExercises: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(onLogoutSuccess: () -> Unit) {
    // Premium Onyx Dark Color Palette
    val bgPrimary = Color(0xFF090A0C)
    val bgSecondary = Color(0xFF121418)
    val bgCard = Color(0xFF1A1D24)
    val brandOrange = Color(0xFFFF5722)
    val brandOrangeGradient = Brush.horizontalGradient(listOf(Color(0xFFFF7043), Color(0xFFFF5722)))
    val textMuted = Color(0xFF8A8D99)
    val borderStrokeColor = Color(0xFF262A35)

    val scrollState = rememberScrollState()
    val workoutScrollState = rememberScrollState()
    val memberScrollState = rememberScrollState()
    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Primary Bottom Bar Tab State Navigation Indicator (0 = Home, 1 = Workouts, 2 = Members)
    var selectedTab by remember { mutableStateOf(0) }

    // Workout Panel Segment Secondary Tab State (0 = Tier Templates, 1 = Approval Requests)
    var workoutSubTab by remember { mutableStateOf(0) }

    // Studio Profile Data Models States
    var ownerName by remember { mutableStateOf("ADMIN") }
    var gymName by remember { mutableStateOf("POWERHOUSE GYM") }
    var qrJoinToken by remember { mutableStateOf("") }
    var gymId by remember { mutableStateOf("") }

    // Matrix Bitmap Graphic State
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingQr by remember { mutableStateOf(true) }

    // Navigation Sheets & Dialog Toggle Flags
    var isQrDialogOpen by remember { mutableStateOf(false) }
    var isProfileSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Dynamic Live Athlete Snapshot Handshake Streams List
    val activeMembersList = remember { mutableStateListOf<Pair<String, String>>() }
    val allGymMembersList = remember { mutableStateListOf<Triple<String, String, String>>() }

    // Custom Routine Verification Queue Array
    val customVerificationQueue = remember { mutableStateListOf<CustomPlanRequest>() }

    // Core Tier Level Upgrade Request Tracker List Model (UID to (Name, RequestedLevel))
    val tierUpgradeRequestsQueue = remember { mutableStateListOf<Pair<String, Pair<String, String>>>() }

    // Gym Workout Tiers Master Blueprint Templates State List
    val masterTierTemplatesList = remember {
        mutableStateListOf(
            "Beginner Routine Plan" to "3 Days/Week • Focus: Foundational Compound Splits",
            "Intermediate Routine Plan" to "4 Days/Week • Focus: Hypertrophy Upper-Lower Split",
            "Pro (Advanced) Routine Plan" to "5 Days/Week • Focus: Progressive Overload Push-Pull-Legs"
        )
    }

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

        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        paint.textSize = 28f
        paint.color = android.graphics.Color.parseColor("#FF5722")
        canvas.drawText("POWERED BY GYMPULSE", flyerWidth / 2f, 90f, paint)

        paint.textSize = 48f
        paint.color = android.graphics.Color.BLACK
        paint.isFakeBoldText = true
        val titleBounds = Rect()
        paint.getTextBounds(gymTitle, 0, gymTitle.length, titleBounds)
        canvas.drawText(gymTitle, flyerWidth / 2f, 170f, paint)

        paint.isFakeBoldText = false
        paint.strokeWidth = 3f
        paint.color = android.graphics.Color.parseColor("#E0E0E0")
        canvas.drawLine(100f, 220f, flyerWidth - 100f, 220f, paint)

        paint.color = android.graphics.Color.DKGRAY
        paint.textSize = 24f
        canvas.drawText("Scan this QR Code via the GymPulse App to link your workout profile", flyerWidth / 2f, 280f, paint)

        val qrSize = 460
        val left = (flyerWidth - qrSize) / 2
        val top = 340
        val srcRect = Rect(0, 0, qrBitmap.width, qrBitmap.height)
        val dstRect = Rect(left, top, left + qrSize, top + qrSize)
        canvas.drawBitmap(qrBitmap, srcRect, dstRect, null)

        paint.color = android.graphics.Color.parseColor("#E0E0E0")
        canvas.drawLine(100f, 850f, flyerWidth - 100f, 850f, paint)

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 26f
        canvas.drawText("QR Code not working? Enter this token ID manually:", flyerWidth / 2f, 910f, paint)

        paint.textSize = 36f
        paint.color = android.graphics.Color.parseColor("#FF5722")
        paint.isFakeBoldText = true
        canvas.drawText(tokenCode, flyerWidth / 2f, 980f, paint)

        return compositeBitmap
    }

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
                Toast.makeText(context, "Flyer saved to gallery successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to build printable flyer.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Failed to initialize storage.", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch Owner & Gym Profiles
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

    // Live Snapshot Real-time Listeners for Roster, Custom Plans, and Level Upgrades
    LaunchedEffect(gymId) {
        if (gymId.isNotBlank()) {
            // Stream 1: Users registered under this workspace
            db.collection("users")
                .whereEqualTo("gymId", gymId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    if (snapshot != null) {
                        activeMembersList.clear()
                        allGymMembersList.clear()
                        for (doc in snapshot.documents) {
                            val uid = doc.id
                            val name = doc.getString("name") ?: "Anonymous Athlete"
                            val email = doc.getString("email") ?: "No Email Linked"
                            val currentRoutinePlaceholder = "Push Day v1"

                            activeMembersList.add(name to currentRoutinePlaceholder)
                            allGymMembersList.add(Triple(uid, name, email))
                        }
                    }
                }

            // Stream 2: Custom Workout Splits Pending Approval status
            db.collection("custom_routines")
                .whereEqualTo("gymId", gymId)
                .whereEqualTo("customPlanStatus", "PENDING_VERIFICATION")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    if (snapshot != null) {
                        customVerificationQueue.clear()
                        for (doc in snapshot.documents) {
                            customVerificationQueue.add(
                                CustomPlanRequest(
                                    id = doc.id,
                                    memberUid = doc.getString("userId") ?: "",
                                    memberName = (doc.getString("userName") ?: "Gym Athlete").uppercase(Locale.ROOT),
                                    planName = doc.getString("routineName") ?: "Custom Workout Split Builder",
                                    totalExercises = "${doc.get("exerciseCount") ?: "5"} Exercises"
                                )
                            )
                        }
                    }
                }

            // Stream 3: Standard Tier Level Upgrades Pending verification checks
            db.collection("users")
                .whereEqualTo("gymId", gymId)
                .whereNotEqualTo("pendingPlanRequest", null)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    if (snapshot != null) {
                        tierUpgradeRequestsQueue.clear()
                        for (doc in snapshot.documents) {
                            val uid = doc.id
                            val name = doc.getString("name") ?: "Athlete"
                            val requestedLevel = doc.getString("pendingPlanRequest") ?: ""
                            if (requestedLevel.isNotBlank()) {
                                tierUpgradeRequestsQueue.add(uid to (name to requestedLevel))
                            }
                        }
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when(selectedTab) {
                                0 -> gymName
                                1 -> "WORKOUT CONTROL"
                                else -> "MANAGE MEMBERS"
                            },
                            fontFamily = BebasNeue,
                            fontSize = 26.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = when(selectedTab) {
                                0 -> "MANAGEMENT PORTAL"
                                1 -> if (workoutSubTab == 0) "${masterTierTemplatesList.size} BLUEPRINTS CONFIGURED" else "${customVerificationQueue.size + tierUpgradeRequestsQueue.size} REQUESTS PENDING"
                                else -> "${allGymMembersList.size} REGISTERED ATHLETES"
                            },
                            fontFamily = DMSans,
                            fontSize = 11.sp,
                            color = brandOrange,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isProfileSheetOpen = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = bgCard),
                        modifier = Modifier.padding(end = 8.dp).clip(CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Open Profile Section", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgSecondary)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = bgSecondary, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home View") },
                    label = { Text("Home", fontFamily = DMSans, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = brandOrange, selectedTextColor = brandOrange, indicatorColor = bgCard, unselectedIconColor = textMuted, unselectedTextColor = textMuted)
                )
                NavigationBarItem(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Workout Manager Panel") },
                    label = { Text("Workouts", fontFamily = DMSans, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = brandOrange, selectedTextColor = brandOrange, indicatorColor = bgCard, unselectedIconColor = textMuted, unselectedTextColor = textMuted)
                )
                NavigationBarItem(
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.People, contentDescription = "Members Directory Feed") },
                    label = { Text("Members", fontFamily = DMSans, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = brandOrange, selectedTextColor = brandOrange, indicatorColor = bgCard, unselectedIconColor = textMuted, unselectedTextColor = textMuted)
                )
            }
        },
        containerColor = bgPrimary
    ) { paddingValues ->

        when (selectedTab) {
            0 -> {
                // TAB 0: CORE ANALYTICS FEED
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ResponsiveGrid(spacing = 12.dp) {
                        MetricCard(title = "Active Members", value = activeMembersList.size.toString(), icon = Icons.Default.FitnessCenter, iconColor = Color(0xFF2196F3))
                        MetricCard(title = "Est. Revenue", value = "₹84.5k", icon = Icons.Default.TrendingUp, iconColor = Color(0xFF4CAF50))
                        MetricCard(title = "Pending Approvals", value = (customVerificationQueue.size + tierUpgradeRequestsQueue.size).toString(), icon = Icons.Default.QrCodeScanner, iconColor = brandOrange)
                    }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgSecondary), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, borderStrokeColor)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                    Text(text = "STUDIO ONBOARDING", fontFamily = BebasNeue, fontSize = 20.sp, color = Color.White, letterSpacing = 0.5.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Tap the preview QR badge tile component to inspect or download high-fidelity printable flyer template assets formatted directly for reception desk displays.", fontFamily = DMSans, fontSize = 12.sp, color = textMuted, lineHeight = 16.sp)
                                }
                                Box(modifier = Modifier.size(96.dp).background(Color.White, RoundedCornerShape(16.dp)).clickable(enabled = qrBitmap != null) { isQrDialogOpen = true }.padding(8.dp), contentAlignment = Alignment.Center) {
                                    if (isLoadingQr) {
                                        CircularProgressIndicator(color = brandOrange, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                                    } else if (qrBitmap != null) {
                                        Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "Gym Onboarding QR Code Preview", modifier = Modifier.fillMaxSize())
                                    } else {
                                        Text("⚠️", fontSize = 24.sp)
                                    }
                                }
                            }
                            if (qrJoinToken.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = borderStrokeColor, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "MANUAL ACCESS PASSKEY TOKEN", fontFamily = DMSans, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textMuted, letterSpacing = 0.5.sp)
                                    Text(text = qrJoinToken, fontFamily = BebasNeue, fontSize = 16.sp, color = brandOrange, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "LIVE GYM FLOOR ACTIVITY", fontFamily = BebasNeue, fontSize = 18.sp, color = Color.White, letterSpacing = 0.5.sp)
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgSecondary), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, borderStrokeColor)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (activeMembersList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                        Text(text = "No athletes checked in yet.\nWaiting for active registration handshake signals...", fontFamily = DMSans, fontSize = 13.sp, color = textMuted, textAlign = TextAlign.Center, lineHeight = 18.sp)
                                    }
                                } else {
                                    activeMembersList.forEachIndexed { index, athlete ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(text = athlete.first, fontFamily = DMSans, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(text = athlete.second, fontFamily = DMSans, fontSize = 12.sp, color = textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            Box(modifier = Modifier.background(brandOrange.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                                Text(text = "ACTIVE", fontFamily = BebasNeue, fontSize = 11.sp, color = brandOrange, letterSpacing = 0.5.sp)
                                            }
                                        }
                                        if (index < activeMembersList.lastIndex) {
                                            HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f), thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // TAB 1: WORKOUT MANAGER PIPELINES (With Dual Verification Streams)
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(workoutScrollState).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TabRow(
                        selectedTabIndex = workoutSubTab, containerColor = bgSecondary, contentColor = brandOrange,
                        divider = { HorizontalDivider(color = borderStrokeColor) },
                        indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[workoutSubTab]), color = brandOrange) }
                    ) {
                        Tab(selected = workoutSubTab == 0, onClick = { workoutSubTab = 0 }, text = { Text("Global Tiers", fontFamily = BebasNeue, fontSize = 14.sp, letterSpacing = 0.5.sp) })
                        Tab(selected = workoutSubTab == 1, onClick = { workoutSubTab = 1 }, text = { Text("Custom Requests", fontFamily = BebasNeue, fontSize = 14.sp, letterSpacing = 0.5.sp) })
                    }

                    if (workoutSubTab == 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "AUTO-ASSIGNED DIFFICULTY PLANS", fontFamily = BebasNeue, fontSize = 16.sp, color = Color.White, letterSpacing = 0.5.sp)
                            Button(
                                onClick = { Toast.makeText(context, "Blueprint builder coming soon!", Toast.LENGTH_SHORT).show() },
                                colors = ButtonDefaults.buttonColors(containerColor = brandOrange), shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("NEW TEMPLATE", fontFamily = BebasNeue, fontSize = 12.sp)
                            }
                        }

                        masterTierTemplatesList.forEach { (title, desc) ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgSecondary), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, borderStrokeColor)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(text = title, fontFamily = DMSans, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = desc, fontFamily = DMSans, fontSize = 12.sp, color = textMuted)
                                    }
                                    Box(modifier = Modifier.size(36.dp).background(brandOrange.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, tint = brandOrange, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        Text(text = "ATHLETES CUSTOM SCHEDULE VERIFICATION MATRIX", fontFamily = BebasNeue, fontSize = 16.sp, color = Color.White, letterSpacing = 0.5.sp)

                        if (customVerificationQueue.isEmpty() && tierUpgradeRequestsQueue.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text(text = "Clear pipeline. No workout splits or tier updates awaiting verification.", fontFamily = DMSans, fontSize = 13.sp, color = textMuted, textAlign = TextAlign.Center)
                            }
                        } else {
                            // STREAM A: RENDER PRESET DIFFICULTY LEVEL PROGRESSION CARD REQUESTS
                            tierUpgradeRequestsQueue.forEach { (memberUid, data) ->
                                val (name, targetLevel) = data
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = bgSecondary), border = BorderStroke(1.dp, borderStrokeColor)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = name.uppercase(Locale.ROOT), fontFamily = BebasNeue, fontSize = 13.sp, color = brandOrange, letterSpacing = 0.5.sp)
                                        Text(text = "Requesting Level Upgrade", fontFamily = DMSans, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(text = "Target Destination Level: $targetLevel Blueprint Split", fontFamily = DMSans, fontSize = 12.sp, color = textMuted)

                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    db.collection("users").document(memberUid)
                                                        .update("experienceLevel", targetLevel, "pendingPlanRequest", FieldValue.delete())
                                                        .addOnSuccessListener { Toast.makeText(context, "Progression upgrade approved.", Toast.LENGTH_SHORT).show() }
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("APPROVE LEVEL", fontFamily = BebasNeue, fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    db.collection("users").document(memberUid)
                                                        .update("pendingPlanRequest", FieldValue.delete())
                                                        .addOnSuccessListener { Toast.makeText(context, "Upgrade request rejected.", Toast.LENGTH_SHORT).show() }
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373).copy(alpha = 0.12f)), shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("REJECT", fontFamily = BebasNeue, fontSize = 12.sp, color = Color(0xFFE57373))
                                            }
                                        }
                                    }
                                }
                            }

                            // STREAM B: RENDER ADVANCED USER-BUILT CUSTOM ROUTINE SPLITS
                            customVerificationQueue.forEach { request ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = bgSecondary), border = BorderStroke(1.dp, borderStrokeColor)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = request.memberName, fontFamily = BebasNeue, fontSize = 13.sp, color = brandOrange, letterSpacing = 0.5.sp)
                                        Text(text = request.planName, fontFamily = DMSans, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(text = request.totalExercises, fontFamily = DMSans, fontSize = 12.sp, color = textMuted)

                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    db.collection("custom_routines").document(request.id)
                                                        .update("customPlanStatus", "APPROVED")
                                                        .addOnSuccessListener { Toast.makeText(context, "Custom split unlocked.", Toast.LENGTH_SHORT).show() }
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("APPROVE PLAN", fontFamily = BebasNeue, fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    db.collection("custom_routines").document(request.id)
                                                        .update("customPlanStatus", "REJECTED")
                                                        .addOnSuccessListener { Toast.makeText(context, "Custom setup split rejected.", Toast.LENGTH_SHORT).show() }
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373).copy(alpha = 0.12f)), shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("REJECT ROUTINE", fontFamily = BebasNeue, fontSize = 12.sp, color = Color(0xFFE57373))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // TAB 2: GLOBAL ROSTER USER CONTROL DIRECTORY
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(memberScrollState).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "ATHLETE DIRECTORY ROSTER", fontFamily = BebasNeue, fontSize = 18.sp, color = Color.White, letterSpacing = 0.5.sp)

                    if (allGymMembersList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text(text = "No gym members registered under this facility workspace yet.", fontFamily = DMSans, fontSize = 14.sp, color = textMuted, textAlign = TextAlign.Center)
                        }
                    } else {
                        allGymMembersList.forEach { (memberUid, name, email) ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgSecondary), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, borderStrokeColor)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(text = name, fontFamily = DMSans, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = email, fontFamily = DMSans, fontSize = 12.sp, color = textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(
                                        onClick = {
                                            db.collection("users").document(memberUid).update("gymId", FieldValue.delete())
                                                .addOnSuccessListener { Toast.makeText(context, "$name removed successfully.", Toast.LENGTH_SHORT).show() }
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFE57373).copy(alpha = 0.1f))
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Evict Member", tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Profile Sheets Expand Overlay Component Blueprint
    if (isProfileSheetOpen) {
        ModalBottomSheet(onDismissRequest = { isProfileSheetOpen = false }, sheetState = sheetState, containerColor = bgSecondary, scrimColor = Color.Black.copy(alpha = 0.6f), dragHandle = { BottomSheetDefaults.DragHandle(color = borderStrokeColor) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(56.dp).background(brandOrange.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = brandOrange, modifier = Modifier.size(36.dp))
                    }
                    Column {
                        Text(text = ownerName, fontFamily = BebasNeue, fontSize = 22.sp, color = Color.White, letterSpacing = 0.5.sp)
                        Text(text = "AUTHORIZED ADMINISTRATOR", fontFamily = DMSans, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted, letterSpacing = 0.5.sp)
                    }
                }
                HorizontalDivider(color = borderStrokeColor, thickness = 1.dp)
                Card(modifier = Modifier.fillMaxWidth().clickable { isProfileSheetOpen = false; auth.signOut(); onLogoutSuccess() }, colors = CardDefaults.cardColors(containerColor = bgCard), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, borderStrokeColor)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size(40.dp).background(Color(0xFFE57373).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Workspace Sign Out Link", tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(text = "LOGOUT CURRENT SESSION", fontFamily = BebasNeue, fontSize = 16.sp, color = Color(0xFFE57373))
                            Text(text = "Securely terminate open active profile sessions.", fontFamily = DMSans, fontSize = 11.sp, color = textMuted)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // High Resolution printable flyer builder modal
    if (isQrDialogOpen && qrBitmap != null) {
        Dialog(onDismissRequest = { isQrDialogOpen = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), colors = CardDefaults.cardColors(containerColor = bgSecondary), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, borderStrokeColor)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = gymName, fontFamily = BebasNeue, fontSize = 26.sp, color = Color.White, textAlign = TextAlign.Center, letterSpacing = 0.5.sp)
                    Text(text = "FRONT DESK REGISTRATION HUB", fontFamily = DMSans, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = brandOrange, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                        Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "Enlarged Studio Registration Key Matrix", modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgCard), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, borderStrokeColor)) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "MANUAL ENTRY BACKUP PASSKEY", fontFamily = DMSans, fontSize = 10.sp, color = textMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = qrJoinToken, fontFamily = BebasNeue, fontSize = 22.sp, color = brandOrange, letterSpacing = 1.5.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { saveQrToGallery(context, qrBitmap!!, gymName, qrJoinToken, "GymPulse_${qrJoinToken}") },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(brandOrangeGradient), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "Download Icon", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "DOWNLOAD ONBOARDING FLYER", fontFamily = BebasNeue, fontSize = 15.sp, color = Color.White, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { isQrDialogOpen = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "CLOSE PREVIEW WINDOW", fontFamily = BebasNeue, fontSize = 14.sp, color = textMuted, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: ImageVector, iconColor: Color) {
    val bgCard = Color(0xFF1A1D24)
    val textMuted = Color(0xFF8A8D99)
    val borderStrokeColor = Color(0xFF262A35)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderStrokeColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, fontFamily = DMSans, fontSize = 12.sp, color = textMuted, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, fontFamily = BebasNeue, fontSize = 24.sp, color = Color.White, letterSpacing = 0.5.sp)
            }
            Box(modifier = Modifier.size(42.dp).background(iconColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = "$title Icon Indicator", tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ResponsiveGrid(modifier: Modifier = Modifier, spacing: Dp = 12.dp, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val totalWidth = constraints.maxWidth
        val columnsCount = when {
            totalWidth >= 1800 -> 3
            totalWidth >= 1200 -> 2
            else -> 1
        }
        val itemWidth = (totalWidth - (spacingPx * (columnsCount - 1))) / columnsCount
        val itemConstraints = constraints.copy(minWidth = itemWidth, maxWidth = itemWidth, minHeight = 0)
        val placeables = measurables.map { it.measure(itemConstraints) }
        val rowsCount = (placeables.size + columnsCount - 1) / columnsCount
        var currentTotalHeight = 0
        val rowHeights = IntArray(rowsCount) { rowIndex ->
            val start = rowIndex * columnsCount
            val end = minOf(start + columnsCount, placeables.size)
            val maxHeight = placeables.subList(start, end).maxOfOrNull { it.height } ?: 0
            currentTotalHeight += maxHeight
            if (rowIndex > 0) currentTotalHeight += spacingPx
            maxHeight
        }
        layout(totalWidth, currentTotalHeight) {
            var yOffset = 0
            for (rowIndex in 0 until rowsCount) {
                var xOffset = 0
                for (colIndex in 0 until columnsCount) {
                    val index = rowIndex * columnsCount + colIndex
                    if (index < placeables.size) {
                        val placeable = placeables[index]
                        placeable.placeRelative(xOffset, yOffset)
                        xOffset += itemWidth + spacingPx
                    }
                }
                yOffset += rowHeights[rowIndex] + spacingPx
            }
        }
    }
}