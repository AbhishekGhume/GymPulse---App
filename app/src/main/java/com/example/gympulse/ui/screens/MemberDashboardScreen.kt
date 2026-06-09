package com.example.gympulse.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gympulse.ui.theme.BebasNeue
import com.example.gympulse.ui.theme.DMSans
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@Composable
fun MemberDashboardScreen(
    onStartWorkoutClick: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val brandOrange = Color(0xFFFF5722)
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var userName by remember { mutableStateOf("ATHLETE") }

    LaunchedEffect(Unit) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    doc.getString("name")?.let { userName = it.uppercase(Locale.ROOT) }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111215))
    ) {
        // Main Toolbar Panel Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16171D))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚡", color = brandOrange, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("GYMPULSE", fontFamily = BebasNeue, fontSize = 22.sp, color = Color.White)
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFF24252C), RoundedCornerShape(8.dp))
                    .clickable {
                        auth.signOut()
                        onLogoutSuccess()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("LOGOUT", fontFamily = BebasNeue, fontSize = 11.sp, color = Color.LightGray)
            }
        }

        // Main Dashboard Stream
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Greetings Segment Block
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("GOOD MORNING, $userName", fontFamily = BebasNeue, fontSize = 28.sp, color = Color.White, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("👋", fontSize = 22.sp)
                }
                Text("Time to crush your session.", fontFamily = DMSans, fontSize = 13.sp, color = Color.Gray)
            }

            // Streak Tracking Hero Card Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFFD84315), brandOrange)))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("7 Day Streak", fontFamily = BebasNeue, fontSize = 18.sp, color = Color.White)
                            Text("4 workouts this week", fontFamily = DMSans, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Horizontal Weekly Tracking Calendar Status Bar Strip
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16171D)),
                border = BorderStroke(1.dp, Color(0xFF23242A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    val status = listOf(true, true, true, false, false, false, false) // Mock data match

                    days.forEachIndexed { i, day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day, fontFamily = BebasNeue, fontSize = 12.sp, color = if (i == 3) brandOrange else Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (status[i]) Color(0xFF00C853) else if (i == 3) Color(0xFF2C1E1A) else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (i == 3) brandOrange else if (status[i]) Color.Transparent else Color.DarkGray,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (status[i]) Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (i == 3) Box(modifier = Modifier.size(6.dp).background(brandOrange, CircleShape))
                            }
                        }
                    }
                }
            }

            // "TODAY'S WORKOUT" Card Action Trigger Module Block
            Column {
                Text("TODAY'S WORKOUT", fontFamily = BebasNeue, fontSize = 13.sp, color = Color.Gray, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartWorkoutClick() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                    border = BorderStroke(1.dp, Color(0xFF23242A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(3.dp).height(16.dp).background(brandOrange))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("DAY 5 • PUSH", fontFamily = BebasNeue, fontSize = 18.sp, color = brandOrange, letterSpacing = 0.5.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Chest & Shoulders", fontFamily = BebasNeue, fontSize = 22.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚙️ 6 exercises", fontFamily = DMSans, fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("⏱️ ~45 min", fontFamily = DMSans, fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        // Circle Chevron Arrow Trigger Badge Icon Element Layout
                        Box(
                            modifier = Modifier.size(36.dp).background(Color(0xFF24252C), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("→", color = Color.White, fontSize = 18.sp)
                        }
                    }
                }
            }

            // "YOUR PROGRAM" Display Card Layout Info Block
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("YOUR PROGRAM", fontFamily = BebasNeue, fontSize = 13.sp, color = Color.Gray, letterSpacing = 1.sp)
                    Text("View Plan", fontFamily = DMSans, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16171D)),
                    border = BorderStroke(1.dp, Color(0xFF23242A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(Color(0xFF24252C), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏋️", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Beginner 5-Day Split", fontFamily = BebasNeue, fontSize = 16.sp, color = Color.White)
                            Text("Week 3 of 8", fontFamily = DMSans, fontSize = 12.sp, color = Color.Gray)
                        }
                        // Mini orange internal tracker milestone loading indicator bar element layout lines
                        Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.DarkGray, RoundedCornerShape(2.dp))) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.35f).background(brandOrange, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
        }
    }
}