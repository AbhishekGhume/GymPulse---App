package com.example.gympulse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.gympulse.R

// Load the custom fonts from res/font/
val BebasNeue = FontFamily(Font(R.font.bebas_neue, FontWeight.Normal))
val DMSans = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    // If you have bold/medium weights, add them here:
    // Font(R.font.dm_sans_bold, FontWeight.Bold)
)

val GymPulseTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BebasNeue,
        fontSize = 42.sp,
        letterSpacing = 1.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BebasNeue,
        fontSize = 32.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DMSans,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    labelSmall = TextStyle(
        fontFamily = DMSans,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
)