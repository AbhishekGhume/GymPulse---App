package com.example.gympulse.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gympulse.ui.theme.BebasNeue
import com.example.gympulse.ui.theme.DMSans

@Composable
fun ActiveWorkoutScreen(onBackNav: () -> Unit) {
    val brandOrange = Color(0xFFFF5722)
    val scrollState = rememberScrollState()

    val exercisesList = listOf(
        WorkoutItem("Incline Barbell Bench Press", "4 SETS x 8-10 REPS", "CHEST", "Focus on the stretch at the bottom. Keep elbows tucked at 45 degrees for shoulder safety.", true),
        WorkoutItem("Dumbbell Lateral Raises", "4 SETS x 12-15 REPS", "SHOULDERS", "", false),
        WorkoutItem("Military Press", "4 SETS x 8 REPS", "SHOULDERS", "", false),
        WorkoutItem("Pec Deck Flyes", "3 SETS x 15 REPS", "CHEST", "", false),
        WorkoutItem("Dips (Weighted)", "3 SETS x AMRAP", "CHEST", "", false),
        WorkoutItem("Rear Delt Reverse Flyes", "3 SETS x 20 REPS", "SHOULDERS", "", false)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111215))
    ) {
        // Top Back Bar Navigation Segment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16171D))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { onBackNav() }
                    .padding(end = 16.dp)
            )
            Column {
                Text("CHEST & SHOULDERS", fontFamily = BebasNeue, fontSize = 20.sp, color = Color.White)
                Text("Day 5 • 6 exercises", fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray)
            }
        }

        // Exercises List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            exercisesList.forEach { exercise ->
                ActiveExerciseCard(exercise = exercise, brandOrange = brandOrange)
            }

            // Bottom Complete Action Guard Button
            Button(
                onClick = { /* Finish and sync pipeline records */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24252C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("COMPLETE ALL SETS FIRST", fontFamily = BebasNeue, fontSize = 16.sp, color = Color.Gray, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ActiveExerciseCard(exercise: WorkoutItem, brandOrange: Color) {
    var completedSets by remember { mutableStateOf(if (exercise.isExpandedDefault) 2 else 0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
        border = BorderStroke(1.dp, if (exercise.isExpandedDefault) brandOrange.copy(alpha = 0.5f) else Color(0xFF23242A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏋️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(exercise.name, fontFamily = BebasNeue, fontSize = 18.sp, color = Color.White)
                        Text("${exercise.setsMeta} • ${exercise.tag}", fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray, letterSpacing = 0.5.sp)
                    }
                }

                // Status circular trigger indicator tracker
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(if (completedSets >= 4) brandOrange else Color.Transparent, CircleShape)
                        .border(2.dp, if (completedSets >= 4) brandOrange else Color.DarkGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (completedSets >= 4) Text("✓", color = Color.White, fontSize = 12.sp)
                }
            }

            // Expanded view container parameters matching your mockup cards details
            if (exercise.isExpandedDefault) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("SET TRACKER", fontFamily = BebasNeue, fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Logged indicator sets
                    for (i in 1..4) {
                        val isLogged = i <= completedSets
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isLogged) Color(0xFF00C853) else Color(0xFF24252C), CircleShape)
                                .clickable { completedSets = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isLogged) "✓" else "$i",
                                fontFamily = BebasNeue,
                                fontSize = 14.sp,
                                color = if (isLogged) Color.White else Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Rest Timer Pill Component
                    Box(
                        modifier = Modifier
                            .background(brandOrange.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .border(1.dp, brandOrange.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("⏱️ Rest: 0s remaining", fontFamily = DMSans, fontSize = 11.sp, color = brandOrange)
                    }
                }

                // Coaching Tip Segment Context Bubble
                if (exercise.tip.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF221815), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("💡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(exercise.tip, fontFamily = DMSans, fontSize = 11.sp, color = Color(0xFFE0E0E0), lineHeight = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Watch Demo Button Trigger Block
                OutlinedButton(
                    onClick = { /* Media stream link trigger execution */ },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E2F38)),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1C1D24))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▶ ", color = brandOrange, fontSize = 10.sp)
                        Text("Watch Demo", fontFamily = BebasNeue, fontSize = 14.sp, color = Color.White, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

data class WorkoutItem(val name: String, val setsMeta: String, val tag: String, val tip: String, val isExpandedDefault: Boolean)