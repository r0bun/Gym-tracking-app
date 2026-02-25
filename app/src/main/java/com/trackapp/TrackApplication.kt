// TrackApplication is the global application class — it runs before any Activity
// or screen is created. Think of it as the app's "setup" code that runs once
// when the app process starts.
//
// We use it to:
//   1. Initialize the Supabase client (network connection to the backend)
//   2. Create shared repository instances that every screen can access
//
// Repositories are created here (not inside each screen) so they share the same
// database connection and data state across all screens. This is called
// "manual dependency injection".

package com.trackapp

import android.app.Application
import com.trackapp.data.local.AppDatabase
import com.trackapp.data.remote.SharedPreferencesSessionManager
import com.trackapp.data.remote.SupabaseConfig
import com.trackapp.data.repository.AuthRepository
import com.trackapp.data.repository.ExerciseRepository
import com.trackapp.data.repository.PreferencesRepository
import com.trackapp.data.repository.WorkoutRepository

// Application is the Android base class for the global app state.
// It must be declared in AndroidManifest.xml with android:name=".TrackApplication"
// so Android knows to use this class instead of the default Application.
class TrackApplication : Application() {

    // "by lazy" means the value is only created the first time it is accessed,
    // not when the Application object is constructed. This avoids slow startup
    // and ordering issues (e.g. Supabase needs to be initialized before AuthRepository
    // tries to use it).

    // Stores simple user preferences (unit system, keep-signed-in flag).
    val preferencesRepository by lazy { PreferencesRepository(this) }

    // The Room database singleton — one connection shared by all repositories.
    val database by lazy { AppDatabase.getInstance(this) }

    // Handles sign-up, sign-in, sign-out, and session status tracking.
    val authRepository by lazy { AuthRepository() }

    // Manages the exercise list (local cache + Supabase sync).
    val exerciseRepository by lazy { ExerciseRepository(database.exerciseDao()) }

    // Manages workouts, entries, and sets (all local — no cloud sync).
    val workoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), database.workoutEntryDao(), database.setDao())
    }

    // Called by Android once, right after the Application object is created.
    override fun onCreate() {
        super.onCreate()

        // Initialize the Supabase client BEFORE any repository tries to use it.
        // We wire in our custom session manager so that if the user checked
        // "Keep me signed in", their session is saved to disk and automatically
        // restored the next time the app opens (no re-login required).
        val sessionMgr = SharedPreferencesSessionManager(
            context = this,
            // Lambda: read the current preference value at the time Supabase
            // asks to save/load the session — not at startup time.
            keepSignedIn = { preferencesRepository.keepSignedIn.value }
        )
        SupabaseConfig.initialize(sessionMgr)
    }
}
