package com.example.gympulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gympulse.ui.screens.AuthScreen
import com.example.gympulse.ui.screens.GymScannerScreen
import com.example.gympulse.ui.screens.MemberDashboardScreen
import com.example.gympulse.ui.screens.ActiveWorkoutScreen
import com.example.gympulse.ui.screens.OwnerDashboardScreen
import com.example.gympulse.ui.theme.GymPulseTypography
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(typography = GymPulseTypography) {
                val navController = rememberNavController()
                val auth = FirebaseAuth.getInstance()
                val db = FirebaseFirestore.getInstance()

                var isCheckingSession by remember { mutableStateOf(true) }
                var startDestination by remember { mutableStateOf("auth") }

                // Check active authentication session state on app launch
                LaunchedEffect(Unit) {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        db.collection("users").document(currentUser.uid).get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    val role = document.getString("role")
                                    startDestination = if (role == "gym_owner") "owner_dashboard" else "member_dashboard"
                                }
                                isCheckingSession = false
                            }
                            .addOnFailureListener {
                                isCheckingSession = false
                            }
                    } else {
                        isCheckingSession = false
                    }
                }

                if (isCheckingSession) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF5722))
                    }
                } else {
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("auth") {
                            AuthScreen(
                                onAuthSuccess = { role ->
                                    val target = if (role == "gym_owner") "owner_dashboard" else "member_dashboard"
                                    navController.navigate(target) {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onNavigateToScanner = { navController.navigate("scanner") }
                            )
                        }

                        composable("scanner") {
                            GymScannerScreen(
                                onJoinSuccess = {
                                    navController.navigate("member_dashboard") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // OWNER DASHBOARD: Management workspace handler routing module
                        composable("owner_dashboard") {
                            OwnerDashboardScreen(
                                onLogoutSuccess = {
                                    // Wipe clear full history routing stack traces on administrator sign out
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // MEMBER SCREEN 1: Main Dashboard (Streak, split, routine card)
                        composable("member_dashboard") {
                            MemberDashboardScreen(
                                onStartWorkoutClick = {
                                    navController.navigate("active_workout")
                                },
                                onLogoutSuccess = {
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // MEMBER SCREEN 2: Today's Workout Details Logging Window
                        composable("active_workout") {
                            ActiveWorkoutScreen(
                                onBackNav = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}