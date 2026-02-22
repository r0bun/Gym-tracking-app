package com.trackapp.data.repository

import com.trackapp.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthRepository {

    private val auth = SupabaseConfig.client.auth
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val currentUser get() = auth.currentUserOrNull()

    /**
     * Emits `null` while the session is still being loaded from storage,
     * `true` when authenticated, and `false` when not authenticated.
     * Observers in the UI use this to drive reactive navigation.
     */
    private val _isSignedIn = MutableStateFlow<Boolean?>(null)
    val isSignedInFlow: StateFlow<Boolean?> = _isSignedIn.asStateFlow()

    init {
        scope.launch {
            auth.sessionStatus.collect { status ->
                _isSignedIn.value = when (status) {
                    is SessionStatus.Authenticated -> true
                    is SessionStatus.NotAuthenticated -> false
                    else -> null   // LoadingFromStorage / NetworkError — keep waiting
                }
            }
        }
    }

    suspend fun signUp(email: String, password: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    fun isSignedIn(): Boolean = auth.currentUserOrNull() != null
}

