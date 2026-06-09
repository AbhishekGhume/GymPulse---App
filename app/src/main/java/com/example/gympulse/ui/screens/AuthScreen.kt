package com.example.gympulse.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gympulse.ui.theme.BebasNeue
import com.example.gympulse.ui.theme.DMSans
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@Composable
fun AuthScreen(
    onAuthSuccess: (role: String) -> Unit,
    onNavigateToScanner: () -> Unit
) {
    var view by remember { mutableStateOf("landing") }
    var ownerStep by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Form States
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var gymName by remember { mutableStateOf("") }
    var gymAddress by remember { mutableStateOf("") }
    var gymPhone by remember { mutableStateOf("") }

    val brandOrange = Color(0xFFFF5722)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF241713), Color(0xFF111215), Color(0xFF111215))
    )

    // Helper functions for Firebase Connection Handshakes
    fun handleSignIn(role: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Please enter all credentials", Toast.LENGTH_SHORT).show()
            return
        }
        loading = true
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { authResult ->
            val uid = authResult.user?.uid ?: ""
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    loading = false
                    val dbRole = document.getString("role") ?: role
                    Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                    onAuthSuccess(dbRole)
                }
                .addOnFailureListener {
                    loading = false
                    onAuthSuccess(role)
                }
        }
            .addOnFailureListener { e ->
                loading = false
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    fun handleGymOwnerSignUp() {
        if (email.isBlank() || password.isBlank() || fullName.isBlank() || gymName.isBlank()) {
            Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
            return
        }
        loading = true
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""
                val token = gymName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "-") + "-" + (1000..9999).random()

                val userMap = hashMapOf(
                    "uid" to uid,
                    "name" to fullName,
                    "email" to email.trim().lowercase(Locale.ROOT),
                    "role" to "gym_owner",
                    "createdAt" to System.currentTimeMillis().toString()
                )

                val gymMap = hashMapOf(
                    "id" to uid,
                    "gymName" to gymName,
                    "address" to gymAddress,
                    "phone" to gymPhone,
                    "ownerUid" to uid,
                    "brandColor" to "#FF5722",
                    "qrJoinToken" to token,
                    "createdAt" to System.currentTimeMillis().toString()
                )

                // Batch writing fields to safe distinct Firestore collections
                db.collection("users").document(uid).set(userMap)
                    .addOnSuccessListener {
                        db.collection("gyms").document(uid).set(gymMap)
                            .addOnSuccessListener {
                                loading = false
                                Toast.makeText(context, "Gym Registered!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess("gym_owner")
                            }
                    }
            }
            .addOnFailureListener { e ->
                loading = false
                Toast.makeText(context, "Registration Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    fun handleMemberSignUp() {
        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            Toast.makeText(context, "Please enter name, email, and password", Toast.LENGTH_SHORT).show()
            return
        }
        loading = true
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""
                val userMap = hashMapOf(
                    "uid" to uid,
                    "name" to fullName,
                    "email" to email.trim().lowercase(Locale.ROOT),
                    "role" to "member",
                    "gymId" to "",
                    "createdAt" to System.currentTimeMillis().toString()
                )

                db.collection("users").document(uid).set(userMap)
                    .addOnSuccessListener {
                        loading = false
                        Toast.makeText(context, "Member Profile Saved!", Toast.LENGTH_SHORT).show()
                        onNavigateToScanner()
                    }
            }
            .addOnFailureListener { e ->
                loading = false
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 28.dp, vertical = 40.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = brandOrange)
        } else {
            when (view) {
                "landing" -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .background(Brush.radialGradient(listOf(brandOrange.copy(alpha = 0.15f), Color.Transparent)))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .background(Color(0xFF1C1D22), CircleShape)
                                        .border(1.dp, Color(0xFF2E2F38), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚡", color = brandOrange, fontSize = 28.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("POWERHOUSE GYM", fontFamily = BebasNeue, fontSize = 42.sp, color = Color.White, letterSpacing = 1.sp)
                            Text(
                                text = "Precision engineering for the elite athlete.\nUnleash your raw potential.",
                                fontFamily = DMSans, fontSize = 12.sp, color = Color.Gray,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("MEMBER", fontFamily = BebasNeue, fontSize = 14.sp, color = Color.Gray, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { view = "signup_member" },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("JOIN GYM", fontFamily = BebasNeue, fontSize = 20.sp, color = Color.White, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Already a member? ", fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    text = "Member Sign In",
                                    fontFamily = DMSans, fontSize = 11.sp, color = Color.White,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { view = "login_member" }
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF2E2F38)))
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(modifier = Modifier.size(4.dp).background(Color(0xFF2E2F38), CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF2E2F38)))
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Text("GYM OWNER", fontFamily = BebasNeue, fontSize = 14.sp, color = Color.Gray, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { view = "register_owner_steps" },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                border = BorderStroke(1.dp, Color(0xFF2E2F38)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("REGISTER YOUR GYM", fontFamily = BebasNeue, fontSize = 20.sp, color = Color.White, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Need to manage? ", fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    text = "Admin Login",
                                    fontFamily = DMSans, fontSize = 11.sp, color = Color.White,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { view = "login_owner" }
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text("🏋️", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("POWERED BY GYMPULSE", fontFamily = DMSans, fontSize = 10.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }

                "login_member" -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text("WELCOME BACK", fontFamily = BebasNeue, fontSize = 32.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(brandOrange))
                            Text("MEMBER WORKOUT PROFILE", fontFamily = BebasNeue, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = email, onValueChange = { email = it },
                                    label = { Text("Email Address", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = password, onValueChange = { password = it },
                                    label = { Text("Password", color = Color.Gray) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                                )
                                Button(
                                    onClick = { handleSignIn("member") },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("SIGN IN", fontFamily = BebasNeue, fontSize = 20.sp, letterSpacing = 1.sp)
                                }
                            }
                        }

                        Text(
                            text = "Cancel and return to hub",
                            fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.fillMaxWidth().clickable { view = "landing" },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                "login_owner" -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text("ADMIN LOGIN", fontFamily = BebasNeue, fontSize = 32.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(brandOrange))
                            Text("MANAGEMENT HUB ACCESS", fontFamily = BebasNeue, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = email, onValueChange = { email = it },
                                    label = { Text("Admin Email", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                                )
                                OutlinedTextField(
                                    value = password, onValueChange = { password = it },
                                    label = { Text("Password", color = Color.Gray) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                                )
                                Button(
                                    onClick = { handleSignIn("gym_owner") },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("SIGN IN TO DASHBOARD", fontFamily = BebasNeue, fontSize = 20.sp, letterSpacing = 1.sp)
                                }
                            }
                        }

                        Text(
                            text = "Cancel and return to hub",
                            fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.fillMaxWidth().clickable { view = "landing" },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                "register_owner_steps" -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("REGISTER YOUR GYM", fontFamily = BebasNeue, fontSize = 32.sp, color = Color.White)
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f).height(6.dp).background(if (ownerStep >= 1) brandOrange else Color.DarkGray, RoundedCornerShape(3.dp)))
                                Box(modifier = Modifier.weight(1f).height(6.dp).background(if (ownerStep >= 2) brandOrange else Color.DarkGray, RoundedCornerShape(3.dp)))
                                Box(modifier = Modifier.weight(1f).height(6.dp).background(if (ownerStep >= 3) brandOrange else Color.DarkGray, RoundedCornerShape(3.dp)))
                            }
                            Text("STEP $ownerStep OF 3", fontFamily = BebasNeue, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (ownerStep == 1) {
                                    OutlinedTextField(value = gymName, onValueChange = { gymName = it }, label = { Text("Gym Name", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                                    OutlinedTextField(value = gymAddress, onValueChange = { gymAddress = it }, label = { Text("Gym Address", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                                    OutlinedTextField(value = gymPhone, onValueChange = { gymPhone = it }, label = { Text("Contact Phone", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                                }
                                if (ownerStep == 2) {
                                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Owner Full Name", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                                }
                                if (ownerStep == 3) {
                                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Admin Email", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Secure Password", color = Color.Gray) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (ownerStep > 1) {
                                        OutlinedButton(onClick = { ownerStep-- }, modifier = Modifier.height(56.dp), border = BorderStroke(1.dp, Color(0xFF2E2F38))) { Text("BACK", color = Color.White) }
                                    }
                                    Button(
                                        onClick = {
                                            if (ownerStep < 3) ownerStep++ else handleGymOwnerSignUp()
                                        },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (ownerStep < 3) "CONTINUE" else "COMPLETE SETUP", fontFamily = BebasNeue, fontSize = 20.sp, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Cancel and return to hub",
                            fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.fillMaxWidth().clickable { view = "landing" },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                "signup_member" -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("MEMBER REGISTRATION", fontFamily = BebasNeue, fontSize = 28.sp, color = Color.White)
                                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password", color = Color.Gray) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brandOrange, unfocusedTextColor = Color.White, focusedTextColor = Color.White))

                                Button(
                                    onClick = { handleMemberSignUp() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("REGISTER", fontFamily = BebasNeue, fontSize = 20.sp, letterSpacing = 1.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Cancel and return to hub",
                            fontFamily = DMSans, fontSize = 11.sp, color = Color.Gray,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.fillMaxWidth().clickable { view = "landing" },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}