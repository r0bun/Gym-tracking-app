// This repository is responsible for all authentication-related actions:
// signing up, signing in, signing out, and tracking whether the user is
// currently logged in.
//
// "Authentication" = verifying who the user is (via email + password here).
// Supabase handles the actual auth logic; this class is a clean wrapper
// so the rest of the app doesn't depend directly on the Supabase library.

package com.trackapp.data.repository

import com.trackapp.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
// Coroutine imports — coroutines are Kotlin's way of doing background work
// (like network calls) without freezing the UI.
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers    // determines which thread work runs on
import kotlinx.coroutines.SupervisorJob  // keeps the scope alive even if one job fails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthRepository {

    // Get the auth module from the Supabase client.
    private val auth = SupabaseConfig.client.auth

    // A background coroutine scope tied to IO threads — appropriate for
    // network and database work that shouldn't block the main (UI) thread.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Returns the currently signed-in user, or null if nobody is logged in.
    val currentUser get() = auth.currentUserOrNull()

    // A three-state live value that tells the UI the current auth status:
    //   null  → still checking stored session (app just opened)
    //   true  → user is authenticated
    //   false → user is not authenticated
    //
    // The navigation system watches this flow to decide which screen to show.
    private val _isSignedIn = MutableStateFlow<Boolean?>(null)
    val isSignedInFlow: StateFlow<Boolean?> = _isSignedIn.asStateFlow()

    // The init block runs automatically when the repository is created.
    // It starts collecting Supabase's session status stream and maps it to
    // our simpler true/false/null representation.
    init {
        scope.launch {
            // sessionStatus is a Flow from Supabase — it emits a new value
            // whenever the authentication state changes.
            auth.sessionStatus.collect { status ->
                _isSignedIn.value = when (status) {
                    is SessionStatus.Authenticated -> true
                    is SessionStatus.NotAuthenticated -> false
                    // LoadingFromStorage or NetworkError — the verdict isn't in yet,
                    // keep showing the loading spinner.
                    else -> null
                }
            }
        }
    }

    // Creates a new user account in Supabase with the given credentials.
    // This is a suspend function — the caller must wait for the network call.
    suspend fun signUp(email: String, password: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    // Signs in an existing user. On success, Supabase emits an Authenticated
    // status, which the collect block above turns into _isSignedIn = true.
    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    // Ends the user's session. Supabase emits NotAuthenticated, which the
    // collect block turns into _isSignedIn = false, triggering navigation
    // back to the Login screen.
    suspend fun signOut() {
        auth.signOut()
    }

    // Quick synchronous check — true if a user is currently signed in.
    fun isSignedIn(): Boolean = auth.currentUserOrNull() != null
}
