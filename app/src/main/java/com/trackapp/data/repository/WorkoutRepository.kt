// A "Repository" is a middleman layer between the UI and the database.
// Instead of every screen talking directly to DAOs, they all go through
// a repository. This has two benefits:
//   1. The UI doesn't need to know how data is stored or where it comes from.
//   2. If we change the storage strategy (e.g. switch to a different database),
//      we only change the repository, not every screen.
//
// This repository handles workouts, workout entries, and sets.

package com.trackapp.data.repository

import com.trackapp.data.local.dao.SetDao
import com.trackapp.data.local.dao.WorkoutDao
import com.trackapp.data.local.dao.WorkoutEntryDao
import com.trackapp.data.local.entity.EntryWithSetsRelation
import com.trackapp.data.local.entity.SetEntity
import com.trackapp.data.local.entity.WorkoutEntity
import com.trackapp.data.local.entity.WorkoutEntryEntity
import kotlinx.coroutines.flow.Flow

// The DAOs are injected (passed in) rather than created here, which makes
// the repository easier to test in isolation.
class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutEntryDao: WorkoutEntryDao,
    private val setDao: SetDao
) {

    // ── Workouts ──────────────────────────────────────────────────────────

    // Returns a live stream of all workouts. The Home and History screens
    // observe this and automatically refresh when data changes.
    fun getAllWorkouts(): Flow<List<WorkoutEntity>> = workoutDao.getAllWorkouts()

    // Fetches a single workout by ID (one-shot, not a live stream).
    suspend fun getWorkoutById(id: String): WorkoutEntity? = workoutDao.getById(id)

    // Creates a new workout session with an optional name, saves it,
    // and returns the new WorkoutEntity so the caller can navigate to it.
    suspend fun createWorkout(name: String = ""): WorkoutEntity {
        val workout = WorkoutEntity(notes = name)
        workoutDao.insert(workout)
        return workout
    }

    // Saves a new name for an existing workout.
    // .copy() creates a modified copy of the object without changing the original
    // (Kotlin data classes are immutable by convention).
    suspend fun updateWorkoutNotes(workout: WorkoutEntity, notes: String) {
        workoutDao.update(workout.copy(notes = notes))
    }

    // Permanently deletes a workout and all its entries/sets
    // (cascade deletion is handled by the database foreign key rules).
    suspend fun deleteWorkout(workoutId: String) {
        workoutDao.deleteById(workoutId)
    }

    // ── Workout Entries ───────────────────────────────────────────────────

    // Returns a live list of exercise entries for a specific workout.
    fun getEntriesForWorkout(workoutId: String): Flow<List<WorkoutEntryEntity>> =
        workoutEntryDao.getEntriesForWorkout(workoutId)

    // Returns entries together with their individual sets (for display in
    // the Workout screen where we show each set's reps and weight).
    fun getEntriesWithSetsForWorkout(workoutId: String): Flow<List<EntryWithSetsRelation>> =
        workoutEntryDao.getEntriesWithSets(workoutId)

    // Saves a new exercise entry to a workout.
    suspend fun addEntry(entry: WorkoutEntryEntity) = workoutEntryDao.insert(entry)

    // Updates an existing exercise entry (e.g. after editing sets/notes).
    suspend fun updateEntry(entry: WorkoutEntryEntity) = workoutEntryDao.update(entry)

    // Removes an exercise entry from the workout.
    suspend fun deleteEntry(entry: WorkoutEntryEntity) = workoutEntryDao.delete(entry)

    // Finds the most recently logged entry for a given exercise, used to
    // auto-fill sets/weight when the user picks an exercise they've done before.
    suspend fun getLastEntryForExercise(exerciseId: String): WorkoutEntryEntity? =
        workoutEntryDao.getLastEntryForExercise(exerciseId)

    // ── Sets ──────────────────────────────────────────────────────────────

    // Saves a single set.
    suspend fun addSet(set: SetEntity) = setDao.insert(set)

    // Saves multiple sets at once (used when saving a whole exercise).
    suspend fun addSets(sets: List<SetEntity>) = setDao.insertAll(sets)

    // Updates a single set.
    suspend fun updateSet(set: SetEntity) = setDao.update(set)

    // Deletes a single set.
    suspend fun deleteSet(set: SetEntity) = setDao.delete(set)

    // Removes all sets belonging to an entry — called before re-saving
    // edited sets so we don't keep stale data.
    suspend fun deleteAllSetsForEntry(entryId: String) = setDao.deleteAllForEntry(entryId)

    // Returns all sets for a specific entry.
    suspend fun getSetsForEntry(entryId: String): List<SetEntity> = setDao.getSetsForEntry(entryId)

    // Combines the two lookups above: finds the most recent entry for an
    // exercise, then returns its sets. Used to pre-populate the editor when
    // a user selects an exercise they've logged before.
    suspend fun getLastSetsForExercise(exerciseId: String): List<SetEntity> {
        val lastEntry = workoutEntryDao.getLastEntryForExercise(exerciseId) ?: return emptyList()
        return setDao.getSetsForEntry(lastEntry.id)
    }
}
