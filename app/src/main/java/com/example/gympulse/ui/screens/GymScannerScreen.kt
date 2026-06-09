package com.example.gympulse.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gympulse.ui.theme.BebasNeue
import com.example.gympulse.ui.theme.DMSans

@Composable
fun GymScannerScreen(onJoinSuccess: () -> Unit) {
    var manualCode by remember { mutableStateOf("") }
    val brandOrange = Color(0xFFFF5722)

    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 240f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "laserPosition"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF241713), Color(0xFF111215))))
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("LINK YOUR GYM", fontFamily = BebasNeue, fontSize = 32.sp, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(brandOrange))
            Text(
                "Scan the official onboarding QR Code displayed at your gym's front desk to sync your training routines.",
                fontFamily = DMSans, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(260.dp)
                .border(2.dp, brandOrange, RoundedCornerShape(16.dp))
                .background(Color(0xFF18191E).copy(alpha = 0.6f))
                .clickable { onJoinSuccess() },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .offset(y = laserYOffset.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, brandOrange, Color.Transparent)))
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔍", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("TAP TO SIMULATE SCAN", fontFamily = BebasNeue, fontSize = 12.sp, color = Color.Gray)
            }
        }

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
                        placeholder = { Text("e.g. powerhouse-pune") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                    )
                    Button(
                        onClick = { onJoinSuccess() },
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