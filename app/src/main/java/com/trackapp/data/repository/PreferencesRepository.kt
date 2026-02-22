package com.trackapp.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("trackapp_prefs", Context.MODE_PRIVATE)

    private val _useLbs = MutableStateFlow(prefs.getBoolean("use_lbs", true))
    val useLbs: StateFlow<Boolean> = _useLbs.asStateFlow()

    fun setUseLbs(value: Boolean) {
        prefs.edit().putBoolean("use_lbs", value).apply()
        _useLbs.value = value
    }

    fun toggleUnit() {
        setUseLbs(!_useLbs.value)
    }

    // ── Keep Signed In ─────────────────────────────────────────────────────────
    private val _keepSignedIn = MutableStateFlow(prefs.getBoolean("keep_signed_in", true))
    val keepSignedIn: StateFlow<Boolean> = _keepSignedIn.asStateFlow()

    fun setKeepSignedIn(value: Boolean) {
        prefs.edit().putBoolean("keep_signed_in", value).apply()
        _keepSignedIn.value = value
    }
}
