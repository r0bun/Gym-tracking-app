// This file applies the app's color palette to Material Design 3 (Material You),
// Google's design system for Android. Material 3 defines a set of named "roles"
// (primary, secondary, surface, error, etc.) and every UI component automatically
// uses the right role — buttons use "primary", cards use "surface", and so on.
//
// CUSTOM COLOR SYSTEM:
// The primary color is now DYNAMIC — it changes based on what the user picks in
// Settings. The color is passed in as parameters (primaryColor / primaryVariantColor)
// from MainActivity, which reads the hex value from PreferencesRepository.
// When the user picks a new color, this composable recomposes and the entire
// app recolors instantly — no restart needed.

package com.trackapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// @Composable means this is a UI function — Jetpack Compose's way of
// building interface. Instead of XML layouts, everything is a function call.
//
// TrackAppTheme wraps the entire app (called in MainActivity) and provides
// the colors and typography to every child composable via CompositionLocal —
// a mechanism that passes values down the UI tree without explicit parameters.
//
// PARAMETERS:
//   primaryColor        — the user's chosen accent color (from hex string)
//   primaryVariantColor — a darker shade auto-derived from primaryColor,
//                         used for pressed states and primaryContainer role
@Composable
fun TrackAppTheme(
    primaryColor: Color = Primary,               // defaults to the original indigo
    primaryVariantColor: Color = PrimaryVariant,  // defaults to the original dark indigo
    content: @Composable () -> Unit
) {
    // Build the color scheme dynamically using the user's chosen primary color.
    // darkColorScheme() is a lightweight object construction — not expensive to
    // recreate on each recomposition when the color changes.
    //
    // Only primary and primaryContainer change with the user's pick.
    // Everything else (surfaces, backgrounds, error, secondary) stays fixed
    // because they're part of the dark theme's neutral palette.
    val colorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = OnPrimary,
        primaryContainer = primaryVariantColor,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,  // defined in Type.kt
        content = content            // everything inside this block gets the theme
    )
}
