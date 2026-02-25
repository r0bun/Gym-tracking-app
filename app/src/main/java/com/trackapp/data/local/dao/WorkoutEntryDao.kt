// This DAO handles all database operations for the "workout_entries" table.
// A workout entry is one exercise logged in a session
// (e.g. "Bench Press — 3 sets" inside "Monday Push Day").

package com.trackapp.data.local.dao

import androidx.room.*
import com.trackapp.data.local.entity.EntryWithSetsRelation
import com.trackapp.data.local.entity.WorkoutEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutEntryDao {

    // Returns all entries for a given workout, in insertion order (oldest first).
    // As a Flow, the Workout screen re-renders automatically whenever exercises
    // are added, edited, or deleted.
    @Query("SELECT * FROM workout_entries WHERE workoutId = :workoutId ORDER BY rowid ASC")
    fun getEntriesForWorkout(workoutId: String): Flow<List<WorkoutEntryEntity>>

    // Same as above but also fetches the individual sets for each entry.
    // @Transaction tells Room to run both queries inside a single database
    // transaction, so the data is always consistent (no partial reads).
    // Returns EntryWithSetsRelation objects — each contains one entry + its sets.
    @Transaction
    @Query("SELECT * FROM workout_entries WHERE workoutId = :workoutId ORDER BY rowid ASC")
    fun getEntriesWithSets(workoutId: String): Flow<List<EntryWithSetsRelation>>

    // Looks up the most recent entry for a specific exercise across all workouts.
    // Used to auto-fill weight/reps when the user picks an exercise they've
    // done before — so they don't have to type the same numbers every time.
    @Query("SELECT * FROM workout_entries WHERE exerciseId = :exerciseId ORDER BY rowid DESC LIMIT 1")
    suspend fun getLastEntryForExercise(exerciseId: String): WorkoutEntryEntity?

    // Saves a new exercise entry to the database.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WorkoutEntryEntity)

    // Updates an existing entry (e.g. after the user edits sets or notes).
    @Update
    suspend fun update(entry: WorkoutEntryEntity)

    // Deletes a specific exercise entry (and its sets cascade-delete automatically
    // due to the ForeignKey CASCADE rule in SetEntity).
    @Delete
    suspend fun delete(entry: WorkoutEntryEntity)

    // Deletes all entries for a given workout (used when the whole workout
    // is deleted from History).
    @Query("DELETE FROM workout_entries WHERE workoutId = :workoutId")
    suspend fun deleteAllForWorkout(workoutId: String)
}
