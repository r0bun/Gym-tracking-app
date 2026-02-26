// ViewModel for the Settings screen — manages the accent color preference.
//
// This follows the same pattern as HomeViewModel and other ViewModels in
// the app: a Factory inner class for dependency injection, and StateFlow
// for reactive UI state.
//
// The ViewModel is intentionally minimal — it just reads/writes a single
// preference. If more settings are added later, they can go here too.

package com.trackapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trackapp.data.repository.PreferencesRepository
import com.trackapp.ui.theme.isValidHex
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // The current accent color as a 6-char hex string (e.g. "4F46E5").
    // The Settings screen observes this to highlight the selected color
    // and populate the hex input field.
    val accentColorHex: StateFlow<String> = preferencesRepository.accentColorHex

    // Called when the user taps a preset or confirms a custom hex code.
    // Only saves valid hex codes — invalid input is silently ignored
    // (the repository also validates, but we check here too for clarity).
    fun setAccentColor(hex: String) {
        if (isValidHex(hex)) {
            preferencesRepository.setAccentColorHex(hex)
        }
    }

    // ── Factory ─────────────────────────────────────────────────────────
    // ViewModels can't take constructor parameters directly — Android needs
    // to recreate them across configuration changes (screen rotation, etc.).
    // The Factory tells Android HOW to create the ViewModel with dependencies.
    class Factory(
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferencesRepository) as T
        }
    }
}
