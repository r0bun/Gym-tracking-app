// This ViewModel manages all the logic for the Login/Sign-Up screen.
// It holds the form fields (email, password), validation, loading state,
// and error messages — everything the screen needs to decide what to show.
//
// The screen itself just displays what the ViewModel tells it and calls
// ViewModel functions when the user interacts. This separation makes the
// logic easy to test independently of the UI.

package com.trackapp.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackapp.data.repository.AuthRepository
import com.trackapp.data.repository.ExerciseRepository
import com.trackapp.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// All UI state for the auth screen in one place.
data class AuthUiState(
    val email: String = "",          // current text in the email field
    val password: String = "",       // current text in the password field
    val isSignUp: Boolean = false,   // false = Sign In mode, true = Sign Up mode
    val isLoading: Boolean = false,  // true while waiting for the network call
    val error: String? = null,       // non-null = show an error message
    val isSuccess: Boolean = false,  // true once auth succeeds (triggers navigation)
    val keepSignedIn: Boolean = true // state of the "Keep me signed in" checkbox
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val exerciseRepository: ExerciseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Called every time the user types in the email field.
    // Also clears any previous error so the red message disappears as they type.
    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, error = null)
    }

    // Called every time the user types in the password field.
    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    // Called when the user toggles the "Keep me signed in" checkbox.
    fun onKeepSignedInChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(keepSignedIn = value)
    }

    // Switches between Sign In and Sign Up mode (the toggle link at the bottom).
    // Clears any error message when switching.
    fun toggleMode() {
        _uiState.value = _uiState.value.copy(isSignUp = !_uiState.value.isSignUp, error = null)
    }

    // Called when the user taps the "Sign In" / "Create Account" button.
    fun submit() {
        val state = _uiState.value

        // Basic validation — don't even try the network if fields are empty.
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Email and password are required.")
            return
        }

        // Launch a background coroutine for the network operation.
        // If we ran this on the main thread, the UI would freeze.
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            try {
                if (state.isSignUp) {
                    authRepository.signUp(state.email.trim(), state.password)
                } else {
                    // Save "keep signed in" preference BEFORE signing in so the
                    // session manager reads the correct value when it receives
                    // the session token from Supabase.
                    preferencesRepository.setKeepSignedIn(state.keepSignedIn)
                    authRepository.signIn(state.email.trim(), state.password)
                }

                // After successful auth, download the exercise list from Supabase.
                // runCatching ignores errors — the user can sync manually later
                // if this fails (e.g. no internet after auth).
                runCatching { exerciseRepository.syncFromRemote() }

                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                // isSuccess = true tells the screen to trigger the navigation callback.

            } catch (e: Exception) {
                // Network error, wrong password, etc. — show the error message.
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Authentication failed."
                )
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val exerciseRepository: ExerciseRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(authRepository, exerciseRepository, preferencesRepository) as T
    }
}
