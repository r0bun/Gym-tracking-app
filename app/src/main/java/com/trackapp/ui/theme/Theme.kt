// This file applies the app's color palette to Material Design 3 (Material You),
// Google's design system for Android. Material 3 defines a set of named "roles"
// (primary, secondary, surface, error, etc.) and every UI component automatically
// uses the right role — buttons use "primary", cards use "surface", and so on.
// By defining the colors here once, every screen in the app inherits them.

package com.trackapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// darkColorScheme builds a Material 3 color scheme optimized for dark UI.
// We map our custom colors (defined in Color.kt) to the Material roles.
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    onError = OnError
)

// @Composable means this is a UI function — Jetpack Compose's way of
// building interface. Instead of XML layouts, everything is a function call.
//
// TrackAppTheme wraps the entire app (called in MainActivity) and provides
// the colors and typography to every child composable via CompositionLocal —
// a mechanism that passes values down the UI tree without explicit parameters.
@Composable
fun TrackAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,  // defined in Type.kt
        content = content            // everything inside this block gets the theme
    )
}
