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

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val keepSignedIn: Boolean = true
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val exerciseRepository: ExerciseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun onKeepSignedInChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(keepSignedIn = value)
    }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(isSignUp = !_uiState.value.isSignUp, error = null)
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Email and password are required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            try {
                if (state.isSignUp) {
                    authRepository.signUp(state.email.trim(), state.password)
                } else {
                    // Persist the "keep signed in" preference before signing in so the
                    // SharedPreferencesSessionManager reads the correct value when it saves
                    // the session returned by Supabase.
                    preferencesRepository.setKeepSignedIn(state.keepSignedIn)
                    authRepository.signIn(state.email.trim(), state.password)
                }
                // Sync exercise list from Supabase after successful auth
                runCatching { exerciseRepository.syncFromRemote() }
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
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

