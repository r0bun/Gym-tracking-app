// This file defines all the colors used throughout the app.
// Colors are written in hexadecimal (0xFF______):
//   - The "FF" at the start is full opacity (100% visible).
//   - The remaining 6 digits are the standard HTML/CSS hex color code (RRGGBB).
// Example: 0xFF4F46E5 = fully opaque blue-violet (#4F46E5).

package com.trackapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary color — the main brand color (deep blue-violet) ──────────────────
val Primary = Color(0xFF4F46E5)          // buttons, active icons
val PrimaryVariant = Color(0xFF3730A3)   // darker shade for pressed states
val OnPrimary = Color(0xFFFFFFFF)        // text/icons drawn ON a primary surface (white)

// ── Secondary color — green accent for success states ─────────────────────────
val Secondary = Color(0xFF10B981)
val OnSecondary = Color(0xFFFFFFFF)

// ── Background and surface colors ─────────────────────────────────────────────
// The app uses a dark theme. Background is the darkest layer (behind everything).
// Surface is slightly lighter — used for cards and bottom sheets.
// SurfaceVariant is a mid-tone for list items and input fields.
val Background = Color(0xFF0F0F11)
val Surface = Color(0xFF1C1C21)
val SurfaceVariant = Color(0xFF27272D)

// "OnX" = the color of content (text/icons) drawn on top of X.
val OnBackground = Color(0xFFF4F4F6)      // light grey — main text on dark background
val OnSurface = Color(0xFFF4F4F6)
val OnSurfaceVariant = Color(0xFF9898A6)  // muted grey — secondary/hint text

// ── Error color — red for destructive actions and error messages ───────────────
val Error = Color(0xFFEF4444)
val OnError = Color(0xFFFFFFFF)

// ── Accent color — amber/gold for highlights, PRs, superset labels ────────────
val Accent = Color(0xFFF59E0B)
