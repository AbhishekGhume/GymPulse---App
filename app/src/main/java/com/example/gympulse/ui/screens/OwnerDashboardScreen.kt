package com.example.gympulse.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gympulse.ui.theme.BebasNeue
import com.example.gympulse.ui.theme.DMSans

@Composable
fun OwnerDashboardScreen() {
    val brandOrange = Color(0xFFFF5722)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111215))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16171D))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("POWERHOUSE GYM", fontFamily = BebasNeue, fontSize = 24.sp, color = Color.White)
                Text("MANAGEMENT PORTAL • ADMIN", fontFamily = DMSans, fontSize = 10.sp, color = Color.Gray)
            }
            Box(
                modifier = Modifier
                    .background(Color.DarkGray, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("LOGOUT", fontFamily = BebasNeue, fontSize = 11.sp, color = Color.LightGray)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val items = listOf("142\nMembers", "₹84.5k\nRevenue", "8\nRoutines")
                items.forEach { data ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E))
                    ) {
                        Text(
                            text = data, fontFamily = BebasNeue, fontSize = 16.sp,
                            color = Color.White, modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                border = BorderStroke(1.dp, Color.DarkGray)
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("STUDIO SIGNUP QR CODE", fontFamily = BebasNeue, fontSize = 18.sp, color = Color.White)
                        Text(
                            "Display this code at the front desk. Beginners scan it to instantly download routines.",
                            fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📱", fontSize = 28.sp)
                    }
                }
            }

            Text("LIVE GYM FLOOR ACTIVITY", fontFamily = BebasNeue, fontSize = 14.sp, color = Color.Gray)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Injecting mock active users
                    val users = listOf("Abhishek Ghume" to "Push Day v1", "Rohan Sharma" to "Beginner Wk 1")
                    users.forEach { (name, routine) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(name, fontFamily = DMSans, fontSize = 14.sp, color = Color.White)
                                Text(routine, fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("ACTIVE", fontFamily = BebasNeue, fontSize = 12.sp, color = brandOrange)
                        }
                    }
                }
            }
        }
    }
}