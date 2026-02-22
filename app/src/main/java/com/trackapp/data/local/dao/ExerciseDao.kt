package com.trackapp.data.local.dao

import androidx.room.*
import com.trackapp.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY muscleGroup, name")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR muscleGroup LIKE '%' || :query || '%'")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT DISTINCT muscleGroup FROM exercises ORDER BY muscleGroup")
    fun getMuscleGroups(): Flow<List<String>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises")
    suspend fun clearAll()
}
