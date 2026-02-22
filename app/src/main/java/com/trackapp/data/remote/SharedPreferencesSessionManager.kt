package com.trackapp.data.remote

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the Supabase [UserSession] to SharedPreferences so the user stays
 * signed in across app restarts. Saving is skipped when [keepSignedIn] returns
 * false (e.g. the user unchecked "Keep me signed in").
 */
class SharedPreferencesSessionManager(
    context: Context,
    private val keepSignedIn: () -> Boolean
) : SessionManager {

    private val prefs = context.getSharedPreferences("trackapp_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        if (keepSignedIn()) {
            prefs.edit().putString("session_data", json.encodeToString(session)).apply()
        } else {
            deleteSession()
        }
    }

    override suspend fun loadSession(): UserSession? {
        val data = prefs.getString("session_data", null) ?: return null
        return try {
            json.decodeFromString<UserSession>(data)
        } catch (e: Exception) {
            deleteSession()
            null
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove("session_data").apply()
    }
}
