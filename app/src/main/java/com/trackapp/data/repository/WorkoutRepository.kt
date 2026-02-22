package com.trackapp.data.repository

import com.trackapp.data.local.dao.SetDao
import com.trackapp.data.local.dao.WorkoutDao
import com.trackapp.data.local.dao.WorkoutEntryDao
import com.trackapp.data.local.entity.EntryWithSetsRelation
import com.trackapp.data.local.entity.SetEntity
import com.trackapp.data.local.entity.WorkoutEntity
import com.trackapp.data.local.entity.WorkoutEntryEntity
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutEntryDao: WorkoutEntryDao,
    private val setDao: SetDao
) {

    // ── Workouts ──────────────────────────────────────────────────────────

    fun getAllWorkouts(): Flow<List<WorkoutEntity>> = workoutDao.getAllWorkouts()

    suspend fun getWorkoutById(id: String): WorkoutEntity? = workoutDao.getById(id)

    suspend fun createWorkout(name: String = ""): WorkoutEntity {
        val workout = WorkoutEntity(notes = name)
        workoutDao.insert(workout)
        return workout
    }

    suspend fun updateWorkoutNotes(workout: WorkoutEntity, notes: String) {
        workoutDao.update(workout.copy(notes = notes))
    }

    suspend fun deleteWorkout(workoutId: String) {
        workoutDao.deleteById(workoutId)
    }

    // ── Workout Entries ───────────────────────────────────────────────────

    fun getEntriesForWorkout(workoutId: String): Flow<List<WorkoutEntryEntity>> =
        workoutEntryDao.getEntriesForWorkout(workoutId)

    fun getEntriesWithSetsForWorkout(workoutId: String): Flow<List<EntryWithSetsRelation>> =
        workoutEntryDao.getEntriesWithSets(workoutId)

    suspend fun addEntry(entry: WorkoutEntryEntity) = workoutEntryDao.insert(entry)

    suspend fun updateEntry(entry: WorkoutEntryEntity) = workoutEntryDao.update(entry)

    suspend fun deleteEntry(entry: WorkoutEntryEntity) = workoutEntryDao.delete(entry)

    /** Useful for auto-filling last weight/sets/reps for a given exercise. */
    suspend fun getLastEntryForExercise(exerciseId: String): WorkoutEntryEntity? =
        workoutEntryDao.getLastEntryForExercise(exerciseId)

    // ── Sets ──────────────────────────────────────────────────────────────

    suspend fun addSet(set: SetEntity) = setDao.insert(set)

    suspend fun addSets(sets: List<SetEntity>) = setDao.insertAll(sets)

    suspend fun updateSet(set: SetEntity) = setDao.update(set)

    suspend fun deleteSet(set: SetEntity) = setDao.delete(set)

    suspend fun deleteAllSetsForEntry(entryId: String) = setDao.deleteAllForEntry(entryId)

    suspend fun getSetsForEntry(entryId: String): List<SetEntity> = setDao.getSetsForEntry(entryId)

    /** Returns sets from the most recent workout entry for this exercise (for auto-fill). */
    suspend fun getLastSetsForExercise(exerciseId: String): List<SetEntity> {
        val lastEntry = workoutEntryDao.getLastEntryForExercise(exerciseId) ?: return emptyList()
        return setDao.getSetsForEntry(lastEntry.id)
    }
}
