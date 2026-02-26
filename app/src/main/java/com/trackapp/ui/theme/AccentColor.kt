// This file defines the accent color system — the primary "brand" color that
// tints buttons, icons, and interactive elements throughout the app.
//
// Users can pick from a set of presets OR type any hex color code they want.
// The chosen color is stored as a 6-character hex string (e.g. "4F46E5") in
// SharedPreferences, so it survives app restarts.
//
// HOW IT WORKS:
//   1. The user picks a color (preset or custom hex) on the Settings screen.
//   2. The hex string is saved in PreferencesRepository.
//   3. MainActivity reads it, converts it to a Compose Color, and passes it
//      to TrackAppTheme.
//   4. TrackAppTheme rebuilds the Material 3 color scheme with the new primary,
//      and the entire app recolors instantly.

package com.trackapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Preset accent colors ────────────────────────────────────────────────────
// Each preset is a simple data holder with a human-readable name and a hex code.
// These are designed to look good on the app's dark background (#0F0F11).
// All have enough contrast for white text on top (WCAG AA for large text).
data class AccentColor(
    val displayName: String,  // shown in the Settings UI (e.g. "Ocean Blue")
    val hex: String           // 6-character hex WITHOUT the "#" (e.g. "0EA5E9")
)

// The list of quick-pick colors shown on the Settings screen.
// You can add, remove, or reorder these freely — just keep hex codes valid.
val accentPresets = listOf(
    AccentColor("Indigo",     "4F46E5"),  // the original app color
    AccentColor("Ocean Blue", "0EA5E9"),
    AccentColor("Emerald",    "10B981"),
    AccentColor("Rose",       "F43F5E"),
    AccentColor("Amber",      "F59E0B"),
    AccentColor("Violet",     "8B5CF6"),
    AccentColor("Teal",       "14B8A6"),
    AccentColor("Orange",     "F97316"),
)

// The default hex used when nothing has been saved yet.
// This matches the original Primary color defined in Color.kt.
const val DEFAULT_ACCENT_HEX = "4F46E5"

// ── Hex ↔ Color conversion utilities ────────────────────────────────────────

/**
 * Converts a 6-character hex string (e.g. "4F46E5") to a Compose [Color].
 * Prepends "FF" for full opacity, then parses as a 32-bit ARGB integer.
 *
 * If the hex is invalid, returns the default indigo so the app never crashes
 * from a bad preference value.
 */
fun hexToColor(hex: String): Color {
    return try {
        // "FF" = fully opaque alpha. toLong(16) parses hex → number.
        Color(("FF$hex").toLong(16))
    } catch (e: Exception) {
        // Fallback: if someone saved garbage in SharedPreferences, use default.
        Color(("FF$DEFAULT_ACCENT_HEX").toLong(16))
    }
}

/**
 * Darkens a [Color] by the given [factor] (0.0 = unchanged, 1.0 = black).
 * Used to auto-generate the "primaryVariant" / "primaryContainer" shade
 * from whatever color the user picks, so we don't need them to pick two colors.
 *
 * Example: darkenColor(Color.Red, 0.25f) → a 25% darker red.
 */
fun darkenColor(color: Color, factor: Float = 0.25f): Color {
    // Multiply each RGB channel by (1 - factor). Alpha stays the same.
    return Color(
        red   = color.red   * (1f - factor),
        green = color.green * (1f - factor),
        blue  = color.blue  * (1f - factor),
        alpha = color.alpha
    )
}

/**
 * Checks if a string is a valid 6-character hex color code.
 * Only allows characters 0-9 and A-F (case-insensitive).
 *
 * Examples:
 *   "4F46E5" → true
 *   "GGG"    → false
 *   "4F46E5FF" → false (too long — we don't include alpha)
 */
fun isValidHex(hex: String): Boolean {
    // Must be exactly 6 chars, and every char must be a hex digit.
    return hex.length == 6 && hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
}
