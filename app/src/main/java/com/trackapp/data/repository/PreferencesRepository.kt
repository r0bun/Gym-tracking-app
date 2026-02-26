// This repository stores simple user preferences — settings the user can
// toggle that need to persist across app restarts.
// It uses SharedPreferences (a key-value store built into Android) rather
// than the Room database because these are tiny single values, not tables.

package com.trackapp.data.repository

import android.content.Context
// MutableStateFlow and StateFlow are "live" data holders:
//   - MutableStateFlow: can be changed internally (mutable = writable)
//   - StateFlow: read-only view exposed to the outside world
// Any part of the UI that "collects" (observes) a StateFlow will automatically
// recompose (redraw) whenever the value changes.
import com.trackapp.ui.theme.DEFAULT_ACCENT_HEX
import com.trackapp.ui.theme.isValidHex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    // Open (or create) the SharedPreferences file named "trackapp_prefs".
    private val prefs = context.getSharedPreferences("trackapp_prefs", Context.MODE_PRIVATE)

    // ── Weight unit preference ─────────────────────────────────────────────

    // Reads the saved value from SharedPreferences on startup.
    // Default is true (lbs) if nothing has been saved yet.
    private val _useLbs = MutableStateFlow(prefs.getBoolean("use_lbs", true))

    // The public read-only view — the UI observes this to know which unit to show.
    val useLbs: StateFlow<Boolean> = _useLbs.asStateFlow()

    // Saves the new preference to disk AND updates the in-memory StateFlow
    // so the UI reflects the change immediately (no restart needed).
    fun setUseLbs(value: Boolean) {
        prefs.edit().putBoolean("use_lbs", value).apply()
        _useLbs.value = value
    }

    // Convenience method: flips the current unit (lbs → kg or kg → lbs).
    fun toggleUnit() {
        setUseLbs(!_useLbs.value)
    }

    // ── Keep Signed In preference ──────────────────────────────────────────

    // Whether the user wants to stay signed in after closing the app.
    // Default is true (stay signed in).
    private val _keepSignedIn = MutableStateFlow(prefs.getBoolean("keep_signed_in", true))
    val keepSignedIn: StateFlow<Boolean> = _keepSignedIn.asStateFlow()

    fun setKeepSignedIn(value: Boolean) {
        prefs.edit().putBoolean("keep_signed_in", value).apply()
        _keepSignedIn.value = value
    }

    // ── Accent color preference ──────────────────────────────────────────
    // Stores the user's chosen primary/accent color as a 6-char hex string
    // (e.g. "8B5CF6" for violet). This drives the entire app's primary color.
    //
    // We store the raw hex rather than an enum name so the user can pick ANY
    // color — not just the presets. DEFAULT_ACCENT_HEX is the original indigo.

    private val _accentColorHex = MutableStateFlow(
        prefs.getString("accent_color", DEFAULT_ACCENT_HEX) ?: DEFAULT_ACCENT_HEX
    )
    val accentColorHex: StateFlow<String> = _accentColorHex.asStateFlow()

    // Saves a new accent color. Only accepts valid 6-char hex strings —
    // silently ignores invalid input so the app never stores garbage.
    fun setAccentColorHex(hex: String) {
        if (!isValidHex(hex)) return  // reject invalid hex codes
        prefs.edit().putString("accent_color", hex).apply()
        _accentColorHex.value = hex
    }
}
