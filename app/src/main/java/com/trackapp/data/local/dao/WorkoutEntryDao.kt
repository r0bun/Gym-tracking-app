package com.trackapp.data.local.dao

import androidx.room.*
import com.trackapp.data.local.entity.EntryWithSetsRelation
import com.trackapp.data.local.entity.WorkoutEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutEntryDao {

    @Query("SELECT * FROM workout_entries WHERE workoutId = :workoutId ORDER BY rowid ASC")
    fun getEntriesForWorkout(workoutId: String): Flow<List<WorkoutEntryEntity>>

    @Transaction
    @Query("SELECT * FROM workout_entries WHERE workoutId = :workoutId ORDER BY rowid ASC")
    fun getEntriesWithSets(workoutId: String): Flow<List<EntryWithSetsRelation>>

    @Query("SELECT * FROM workout_entries WHERE exerciseId = :exerciseId ORDER BY rowid DESC LIMIT 1")
    suspend fun getLastEntryForExercise(exerciseId: String): WorkoutEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WorkoutEntryEntity)

    @Update
    suspend fun update(entry: WorkoutEntryEntity)

    @Delete
    suspend fun delete(entry: WorkoutEntryEntity)

    @Query("DELETE FROM workout_entries WHERE workoutId = :workoutId")
    suspend fun deleteAllForWorkout(workoutId: String)
}
