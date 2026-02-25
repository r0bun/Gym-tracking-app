// This DAO handles all database operations for the "workouts" table.
// Each function corresponds to one thing the app might need to do with
// workout sessions (create, read, update, delete).

package com.trackapp.data.local.dao

import androidx.room.*
import com.trackapp.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // Returns all workouts sorted by date, newest first.
    // As a Flow, the Home and History screens automatically update
    // whenever a workout is added or deleted.
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    // Fetches a single workout by its unique ID.
    // Returns null if no workout with that ID exists.
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: String): WorkoutEntity?

    // Saves a new workout to the database.
    // OnConflictStrategy.REPLACE: if a workout with the same ID already exists,
    // replace it (effectively an update). Returns the row ID assigned by SQLite.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity): Long

    // Updates an existing workout row (e.g. to change its name).
    // Room matches the row by the WorkoutEntity's @PrimaryKey (id field).
    @Update
    suspend fun update(workout: WorkoutEntity)

    // Deletes a specific workout object (Room uses the id field to find the row).
    @Delete
    suspend fun delete(workout: WorkoutEntity)

    // Deletes by ID directly — useful when you only have the ID string,
    // not the full WorkoutEntity object.
    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: String)
}
