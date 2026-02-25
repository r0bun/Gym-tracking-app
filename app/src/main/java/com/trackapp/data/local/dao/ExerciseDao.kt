// "DAO" stands for Data Access Object.
// Think of it as a menu of database operations — it lists everything the app
// is allowed to do with the "exercises" table (select, insert, delete, etc.).
// Room reads this interface and generates the actual database code for us.

package com.trackapp.data.local.dao

import androidx.room.*
import com.trackapp.data.local.entity.ExerciseEntity
// Flow is like a live subscription: the UI automatically receives new data
// whenever the underlying database table changes, without having to ask again.
import kotlinx.coroutines.flow.Flow

// @Dao marks this as a Room Data Access Object.
@Dao
interface ExerciseDao {

    // SELECT * FROM exercises ... = "give me all rows from the exercises table,
    // sorted alphabetically by muscle group, then by name".
    // Returns a Flow, so the UI refreshes automatically whenever exercises change.
    @Query("SELECT * FROM exercises ORDER BY muscleGroup, name")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    // Returns exercises whose name OR muscle group contains the search text.
    // The '%' symbols are SQL wildcards meaning "any characters here".
    // E.g. searching "chest" would return "Chest Press", "Incline Chest Fly", etc.
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR muscleGroup LIKE '%' || :query || '%'")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    // Returns only unique muscle group names, sorted alphabetically.
    // Used to populate the muscle group filter chips in the exercise picker.
    @Query("SELECT DISTINCT muscleGroup FROM exercises ORDER BY muscleGroup")
    fun getMuscleGroups(): Flow<List<String>>

    // Looks up a single exercise by its ID.
    // "suspend" means this function runs in the background (it may take a moment
    // to access the database) and the caller must wait for the result.
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    // @Upsert = "insert if new, update if already exists".
    // Used when syncing from Supabase: exercises that are already cached get
    // refreshed; new ones get added.
    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    // Deletes every row from the exercises table. Used before a full re-sync.
    @Query("DELETE FROM exercises")
    suspend fun clearAll()
}
