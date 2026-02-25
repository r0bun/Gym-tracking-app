// This file defines the app's typography — the text styles used for headings,
// body text, labels, and so on. Material 3 has a predefined set of text roles
// (headlineLarge, bodyMedium, labelSmall, etc.) and every Text() composable
// in the app references one of these roles via MaterialTheme.typography.
//
// Centralising typography here means changing the font size of all headings
// is a one-line edit, not a find-and-replace across every screen.

package com.trackapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
// "sp" = "scale-independent pixels" — like dp but respects the user's font
// size preference in Android system settings.
import androidx.compose.ui.unit.sp

val AppTypography = Typography(

    // Large titles — e.g. the "TrackApp" heading on the login screen.
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),

    // Section headings — e.g. "Ready to train?" on the home card.
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),

    // Smaller headings inside cards.
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),

    // Screen / dialog titles (e.g. "Workout History" in the top bar).
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),

    // Card titles (e.g. "Recent workouts" section header).
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    // Default body text — most readable content uses this.
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    // Slightly smaller body text — workout names, set details, etc.
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    // Small labels — button text, tags, "SUPERSET" badge.
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
