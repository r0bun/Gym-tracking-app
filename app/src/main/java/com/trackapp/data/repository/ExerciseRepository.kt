// This repository manages the exercise list — both the local cached copy
// (stored in Room) and the master copy on Supabase (cloud).
//
// The app follows an "offline-first" strategy:
//   - Exercises are fetched from Supabase once (on login) and cached locally.
//   - After that, all reads come from the local Room database.
//   - The user can manually trigger a re-sync from the Home screen.
// This means the app still shows exercises even without internet.

package com.trackapp.data.repository

import com.trackapp.data.local.dao.ExerciseDao
import com.trackapp.data.local.entity.ExerciseEntity
import com.trackapp.data.remote.RemoteExercise
import com.trackapp.data.remote.SupabaseConfig
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val exerciseDao: ExerciseDao) {

    // Returns the full cached exercise list as a live stream.
    // The UI automatically updates whenever exercises are synced.
    fun getAllExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    // Returns exercises matching a search query, or all exercises if the query
    // is blank (empty search = show everything).
    fun searchExercises(query: String): Flow<List<ExerciseEntity>> =
        if (query.isBlank()) exerciseDao.getAllExercises()
        else exerciseDao.searchExercises(query)

    // Returns the distinct list of muscle groups (for the filter chips in the
    // exercise picker — "Chest", "Back", "Legs", etc.).
    fun getMuscleGroups(): Flow<List<String>> = exerciseDao.getMuscleGroups()

    // Looks up a single exercise by its ID. Returns null if not found.
    suspend fun getExerciseById(id: String): ExerciseEntity? = exerciseDao.getById(id)

    // Downloads the full exercise list from Supabase and saves it locally.
    // Called once on login and whenever the user taps the sync button.
    suspend fun syncFromRemote() {
        // .from("exercises") = access the "exercises" table in Supabase
        // .select()          = run a SELECT * (get all rows)
        // .decodeList<...>() = parse the JSON response into a Kotlin list
        val remoteExercises = SupabaseConfig.client.postgrest
            .from("exercises")
            .select()
            .decodeList<RemoteExercise>()

        // Convert each RemoteExercise (cloud format) to ExerciseEntity (local format).
        // The only real difference is that RemoteExercise uses @SerialName for the
        // JSON key and ExerciseEntity uses Room annotations for the database column.
        val entities = remoteExercises.map { remote ->
            ExerciseEntity(
                id = remote.id,
                name = remote.name,
                muscleGroup = remote.muscleGroup
            )
        }

        // Upsert = insert new exercises, update existing ones (by ID).
        // This preserves local exercises while adding/updating remote ones.
        exerciseDao.upsertAll(entities)
    }
}
