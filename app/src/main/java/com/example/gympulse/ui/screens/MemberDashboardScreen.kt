package com.example.gympulse.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gympulse.ui.theme.BebasNeue
import com.example.gympulse.ui.theme.DMSans
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDashboardScreen(
    onStartWorkoutClick: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    // Premium Dark Onyx Theme Color Palette
    val bgPrimary = Color(0xFF090A0C)
    val bgSecondary = Color(0xFF121418)
    val bgCard = Color(0xFF1A1D24)
    val brandOrange = Color(0xFFFF5722)
    val brandOrangeGradient = Brush.horizontalGradient(listOf(Color(0xFFFF7043), Color(0xFFFF5722)))
    val textMuted = Color(0xFF8A8D99)
    val borderStrokeColor = Color(0xFF262A35)

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUid = auth.currentUser?.uid

    // User Interface State Management Holders
    var userName by remember { mutableStateOf("ATHLETE") }
    var gymId by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf<String?>(null) }
    var pendingPlanRequest by remember { mutableStateOf<String?>(null) }

    var customPlanStatus by remember { mutableStateOf("NONE") }
    var assignedRoutineName by remember { mutableStateOf("No Active Plan Blueprint Loaded") }
    var exerciseCountString by remember { mutableStateOf("0 Exercises") }

    var showCustomBuilderView by remember { mutableStateOf(false) }
    var isProfileSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customRoutineNameInput by remember { mutableStateOf("") }
    var customExerciseCountInput by remember { mutableStateOf("") }

    // REAL-TIME DOCUMENT SNAPSHOT LISTENER PIPELINE
    LaunchedEffect(currentUid) {
        if (currentUid != null) {
            db.collection("users").document(currentUid)
                .addSnapshotListener { docSnapshot, error ->
                    if (error != null) return@addSnapshotListener

                    if (docSnapshot != null && docSnapshot.exists()) {
                        gymId = docSnapshot.getString("gymId") ?: ""
                        experienceLevel = docSnapshot.getString("experienceLevel")
                        pendingPlanRequest = docSnapshot.getString("pendingPlanRequest")

                        docSnapshot.getString("name")?.let {
                            userName = it.uppercase(Locale.ROOT)
                        }

                        // Eviction Failsafe Interceptor Redirection
                        if (gymId.isBlank()) {
                            auth.signOut()
                            onLogoutSuccess()
                            return@addSnapshotListener
                        }

                        // Determine active layout names using currently approved experienceLevel parameters
                        if (experienceLevel == "CUSTOM") {
                            db.collection("custom_routines")
                                .whereEqualTo("userId", currentUid)
                                .whereEqualTo("gymId", gymId)
                                .addSnapshotListener { routineSnapshot, _ ->
                                    if (routineSnapshot != null && !routineSnapshot.isEmpty) {
                                        val routineDoc = routineSnapshot.documents.first()
                                        customPlanStatus = routineDoc.getString("customPlanStatus") ?: "PENDING_VERIFICATION"
                                        assignedRoutineName = routineDoc.getString("routineName") ?: "My Custom Workout Split"
                                        exerciseCountString = "${routineDoc.get("exerciseCount") ?: "0"} Exercises"
                                    }
                                }
                        } else if (!experienceLevel.isNullOrBlank()) {
                            customPlanStatus = "APPROVED"
                            assignedRoutineName = when(experienceLevel) {
                                "BEGINNER" -> "Push Day (Beginner Blueprint)"
                                "INTERMEDIATE" -> "Hypertrophy Upper/Lower Split"
                                else -> "Progressive Overload Push-Pull-Legs Pro"
                            }
                            exerciseCountString = "5-7 Core Target Exercises"
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
                        Text(text = "GYMPULSE ATHLETE", fontFamily = BebasNeue, fontSize = 26.sp, color = Color.White)
                        Text(text = "WELCOME BACK • $userName", fontFamily = DMSans, fontSize = 11.sp, color = brandOrange, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isProfileSheetOpen = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = bgCard),
                        modifier = Modifier.padding(end = 8.dp).clip(CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Profile Options", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgSecondary)
            )
        },
        containerColor = bgPrimary
    ) { paddingValues ->

        if (experienceLevel == null) {
            // LAYER A: FIRST TIME EXPERIENCE PATH LEVEL SELECTOR
            if (!showCustomBuilderView) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "CHOOSE YOUR WORKOUT PATH", fontFamily = BebasNeue, fontSize = 28.sp, color = Color.White, textAlign = TextAlign.Center)
                    Text(text = "Your gym owner provides structured template splits tailored to your baseline skill tier level, or build a verified routine setup blueprint.", fontFamily = DMSans, fontSize = 13.sp, color = textMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 24.dp))

                    val tiers = listOf(
                        "BEGINNER" to "Foundational 3-Day Compound Layout",
                        "INTERMEDIATE" to "Hypertrophy Optimized Upper/Lower 4-Day Split",
                        "ADVANCED" to "Progressive Overload Push-Pull-Legs 5-Day Stack"
                    )

                    tiers.forEach { (tier, desc) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                                if (currentUid != null) {
                                    db.collection("users").document(currentUid)
                                        .update("experienceLevel", tier)
                                        .addOnSuccessListener { Toast.makeText(context, "Welcome to your new routine!", Toast.LENGTH_SHORT).show() }
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = bgSecondary),
                            border = BorderStroke(1.dp, borderStrokeColor),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.size(40.dp).background(brandOrange.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = brandOrange, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text(text = tier, fontFamily = BebasNeue, fontSize = 18.sp, color = Color.White, letterSpacing = 0.5.sp)
                                    Text(text = desc, fontFamily = DMSans, fontSize = 12.sp, color = textMuted)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showCustomBuilderView = true }) {
                        Text(text = "Create My Own Custom Workout Split", fontFamily = BebasNeue, fontSize = 14.sp, color = brandOrange, letterSpacing = 0.5.sp)
                    }
                }
            } else {
                // LAYER B: CUSTOM SPLIT REGISTRATION SETUP BUILDER CONTAINER FORM
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(text = "DESIGN YOUR WORKOUT SPLIT", fontFamily = BebasNeue, fontSize = 28.sp, color = Color.White)
                    Text(text = "Configure details below. Once submitted, your routine will enter the owner's verification workspace approval queue layer.", fontFamily = DMSans, fontSize = 13.sp, color = textMuted, modifier = Modifier.padding(bottom = 24.dp))

                    OutlinedTextField(
                        value = customRoutineNameInput,
                        onValueChange = { customRoutineNameInput = it },
                        label = { Text("Routine Split Name (e.g. Hypertrophy Pro)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, cursorColor = brandOrange)
                    )

                    OutlinedTextField(
                        value = customExerciseCountInput,
                        onValueChange = { customExerciseCountInput = it },
                        label = { Text("Estimated Total Exercise Targets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, cursorColor = brandOrange)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (customRoutineNameInput.isBlank() || customExerciseCountInput.isBlank()) {
                                Toast.makeText(context, "Fill out setup validation fields.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (currentUid != null) {
                                val routinePayload = hashMapOf(
                                    "userId" to currentUid,
                                    "userName" to userName,
                                    "gymId" to gymId,
                                    "routineName" to customRoutineNameInput,
                                    "exerciseCount" to (customExerciseCountInput.toIntOrNull() ?: 5),
                                    "customPlanStatus" to "PENDING_VERIFICATION"
                                )

                                db.collection("custom_routines").add(routinePayload)
                                    .addOnSuccessListener {
                                        db.collection("users").document(currentUid).update("experienceLevel", "CUSTOM")
                                        showCustomBuilderView = false
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandOrange)
                    ) {
                        Text("SUBMIT PLAN TO OWNER", fontFamily = BebasNeue, fontSize = 16.sp, color = Color.White)
                    }

                    TextButton(onClick = { showCustomBuilderView = false }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Go Back to Tier Presets", fontFamily = BebasNeue, fontSize = 14.sp, color = textMuted)
                    }
                }
            }
        } else {
            // LAYER C: CORE MEMBERS FEED DASHBOARD (Stays completely functional during upgrade requests)
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // Subtle dynamic notice block alerting that a layout transition request is pending
                    if (!pendingPlanRequest.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Plan switch request to '$pendingPlanRequest' is pending owner approval. Your current plan remains fully active until verified.",
                                    fontFamily = DMSans, fontSize = 12.sp, color = Color(0xFFFFB300), lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = bgSecondary),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, borderStrokeColor)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "CURRENT ACTIVE TRAINING SPLIT", fontFamily = BebasNeue, fontSize = 14.sp, color = brandOrange, letterSpacing = 0.5.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = assignedRoutineName, fontFamily = DMSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = exerciseCountString, fontFamily = DMSans, fontSize = 13.sp, color = textMuted)
                                }
                                Box(
                                    modifier = Modifier.background(Color(0xFF4CAF50).copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "ACTIVE", fontFamily = BebasNeue, fontSize = 11.sp, color = Color(0xFF4CAF50), letterSpacing = 0.5.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onStartWorkoutClick,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(brandOrangeGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "START WORKOUT ACTIVE SESSION", fontFamily = BebasNeue, fontSize = 15.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // User Status Analytics Rows
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = bgSecondary), border = BorderStroke(1.dp, borderStrokeColor), shape = RoundedCornerShape(14.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("MY ATTENDANCE STREAK", fontFamily = DMSans, fontSize = 11.sp, color = textMuted)
                                Text("5 DAYS", fontFamily = BebasNeue, fontSize = 22.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = bgSecondary), border = BorderStroke(1.dp, borderStrokeColor), shape = RoundedCornerShape(14.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("COMPLETED SPLITS", fontFamily = DMSans, fontSize = 11.sp, color = textMuted)
                                Text("12 TOTAL", fontFamily = BebasNeue, fontSize = 22.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Context Menu Profile Settings Configuration Sheet
    if (isProfileSheetOpen) {
        ModalBottomSheet(onDismissRequest = { isProfileSheetOpen = false }, sheetState = sheetState, containerColor = bgSecondary, dragHandle = { BottomSheetDefaults.DragHandle(color = borderStrokeColor) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(56.dp).background(brandOrange.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = brandOrange, modifier = Modifier.size(36.dp))
                    }
                    Column {
                        Text(text = userName, fontFamily = BebasNeue, fontSize = 22.sp, color = Color.White, letterSpacing = 0.5.sp)
                        Text(text = "CURRENT LEVEL STATUS: ${experienceLevel ?: "UNASSIGNED"}", fontFamily = DMSans, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMuted, letterSpacing = 0.5.sp)
                    }
                }

                HorizontalDivider(color = borderStrokeColor)

                if (experienceLevel != null) {
                    Text(text = "GLOBAL GYM PLANS", fontFamily = BebasNeue, fontSize = 14.sp, color = Color.White, letterSpacing = 0.5.sp)

                    val alternatePlans = listOf(
                        "BEGINNER" to "Foundational 3-Day Compound Layout",
                        "INTERMEDIATE" to "Hypertrophy Optimized Upper/Lower 4-Day Split",
                        "ADVANCED" to "Progressive Overload Push-Pull-Legs 5-Day Stack"
                    )

                    alternatePlans.forEach { (tier, desc) ->
                        val isCurrentlyActive = experienceLevel == tier
                        val isCurrentlyRequested = pendingPlanRequest == tier

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !isCurrentlyActive && !isCurrentlyRequested) {
                                isProfileSheetOpen = false
                                if (currentUid != null) {
                                    db.collection("users").document(currentUid)
                                        .update("pendingPlanRequest", tier)
                                        .addOnSuccessListener { Toast.makeText(context, "Switch request to $tier submitted!", Toast.LENGTH_SHORT).show() }
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = if (isCurrentlyActive) brandOrange.copy(alpha = 0.05f) else bgCard),
                            border = BorderStroke(1.dp, if (isCurrentlyActive) brandOrange.copy(alpha = 0.4f) else borderStrokeColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (isCurrentlyActive) brandOrange else textMuted, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = tier, fontFamily = BebasNeue, fontSize = 16.sp, color = Color.White)
                                        Text(text = desc, fontFamily = DMSans, fontSize = 11.sp, color = textMuted)
                                    }
                                }

                                if (isCurrentlyRequested) {
                                    Text(text = "PENDING OWNER", fontFamily = BebasNeue, fontSize = 11.sp, color = Color(0xFFFFB300))
                                } else if (isCurrentlyActive) {
                                    Text(text = "ACTIVE NOW", fontFamily = BebasNeue, fontSize = 11.sp, color = brandOrange)
                                }
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth().clickable { isProfileSheetOpen = false; auth.signOut(); onLogoutSuccess() }, colors = CardDefaults.cardColors(containerColor = bgCard), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, borderStrokeColor)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size(40.dp).background(Color(0xFFE57373).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
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
}