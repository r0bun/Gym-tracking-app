package com.trackapp

import android.app.Application
import com.trackapp.data.local.AppDatabase
import com.trackapp.data.remote.SharedPreferencesSessionManager
import com.trackapp.data.remote.SupabaseConfig
import com.trackapp.data.repository.AuthRepository
import com.trackapp.data.repository.ExerciseRepository
import com.trackapp.data.repository.PreferencesRepository
import com.trackapp.data.repository.WorkoutRepository

class TrackApplication : Application() {

    // Declared before onCreate so the lambda below can reference it lazily.
    val preferencesRepository by lazy { PreferencesRepository(this) }

    val database by lazy { AppDatabase.getInstance(this) }

    val authRepository by lazy { AuthRepository() }

    val exerciseRepository by lazy { ExerciseRepository(database.exerciseDao()) }

    val workoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), database.workoutEntryDao(), database.setDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize the Supabase client with a session manager so sessions
        // can be persisted across app restarts when the user stays signed in.
        val sessionMgr = SharedPreferencesSessionManager(
            context = this,
            keepSignedIn = { preferencesRepository.keepSignedIn.value }
        )
        SupabaseConfig.initialize(sessionMgr)
    }
}

