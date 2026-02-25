// When a user logs in, Supabase gives the app a "session" — a token that
// proves the user is authenticated. Normally this session disappears when the
// app is closed. This class saves the session to the phone's permanent storage
// (SharedPreferences) so the user stays signed in across app restarts.
//
// SharedPreferences is Android's built-in key-value store — think of it like
// a tiny settings file on the phone. It persists until the app is uninstalled.

package com.trackapp.data.remote

import android.content.Context
// SessionManager is a Supabase interface — it defines the contract for saving
// and loading a user session. We implement it our own way here.
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
// encodeToString / decodeFromString convert objects to/from JSON strings
// so they can be stored as plain text in SharedPreferences.
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// This class implements Supabase's SessionManager interface, giving us full
// control over where and how session data is stored.
class SharedPreferencesSessionManager(
    context: Context,
    // A function (lambda) that returns whether "Keep me signed in" is enabled.
    // Using a lambda instead of a plain Boolean means we always read the
    // latest preference value, not a snapshot from when this class was created.
    private val keepSignedIn: () -> Boolean
) : SessionManager {

    // Open (or create) the SharedPreferences file named "trackapp_session".
    // MODE_PRIVATE means only this app can read/write it.
    private val prefs = context.getSharedPreferences("trackapp_session", Context.MODE_PRIVATE)

    // JSON parser configured to ignore unknown fields — safe if Supabase ever
    // adds new fields to the session object that we haven't accounted for.
    private val json = Json { ignoreUnknownKeys = true }

    // Called by Supabase whenever a session is established (after login).
    // We only save the session if the user checked "Keep me signed in".
    override suspend fun saveSession(session: UserSession) {
        if (keepSignedIn()) {
            // Serialize the session object to a JSON string and store it.
            prefs.edit().putString("session_data", json.encodeToString(session)).apply()
        } else {
            // User doesn't want to stay signed in — clear any saved session.
            deleteSession()
        }
    }

    // Called by Supabase on app startup to restore a previous session.
    // If a saved session string exists, we decode it back into a UserSession.
    override suspend fun loadSession(): UserSession? {
        val data = prefs.getString("session_data", null) ?: return null
        return try {
            json.decodeFromString<UserSession>(data)
        } catch (e: Exception) {
            // If the saved data is corrupted or outdated, wipe it and
            // return null (user will need to log in again).
            deleteSession()
            null
        }
    }

    // Removes the saved session from storage (called on sign-out).
    override suspend fun deleteSession() {
        prefs.edit().remove("session_data").apply()
    }
}
