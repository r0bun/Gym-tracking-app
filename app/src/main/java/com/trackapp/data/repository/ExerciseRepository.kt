package com.trackapp.data.repository

import com.trackapp.data.local.dao.ExerciseDao
import com.trackapp.data.local.entity.ExerciseEntity
import com.trackapp.data.remote.RemoteExercise
import com.trackapp.data.remote.SupabaseConfig
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val exerciseDao: ExerciseDao) {

    /** All exercises from the local cache, grouped / searched as needed. */
    fun getAllExercises(): Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    fun searchExercises(query: String): Flow<List<ExerciseEntity>> =
        if (query.isBlank()) exerciseDao.getAllExercises()
        else exerciseDao.searchExercises(query)

    fun getMuscleGroups(): Flow<List<String>> = exerciseDao.getMuscleGroups()

    suspend fun getExerciseById(id: String): ExerciseEntity? = exerciseDao.getById(id)

    /**
     * Fetches the master exercise list from Supabase and upserts it into the local cache.
     * Call once on login / app launch.
     */
    suspend fun syncFromRemote() {
        val remoteExercises = SupabaseConfig.client.postgrest
            .from("exercises")
            .select()
            .decodeList<RemoteExercise>()

        val entities = remoteExercises.map { remote ->
            ExerciseEntity(
                id = remote.id,
                name = remote.name,
                muscleGroup = remote.muscleGroup
            )
        }
        exerciseDao.upsertAll(entities)
    }
}
