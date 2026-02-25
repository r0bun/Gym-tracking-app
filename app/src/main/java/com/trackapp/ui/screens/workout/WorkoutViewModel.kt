// This ViewModel manages all data and logic for the Workout screen.
// It is the most complex ViewModel in the app because the Workout screen
// has many interactive elements: adding/editing/deleting exercises,
// managing individual sets, linking supersets, and renaming the workout.
//
// Key concept — "draft" pattern:
// When the user picks an exercise and starts entering data, a temporary
// "draft" object is created in memory. Nothing is written to the database
// until the user taps "Save". If they tap "Cancel", the draft is discarded
// and the database is untouched. This prevents partial/corrupt saves.

package com.trackapp.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackapp.data.local.entity.ExerciseEntity
import com.trackapp.data.local.entity.SetEntity
import com.trackapp.data.local.entity.WorkoutEntryEntity
import com.trackapp.data.repository.ExerciseRepository
import com.trackapp.data.repository.PreferencesRepository
import com.trackapp.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// ── Draft data classes ──────────────────────────────────────────────────────
// These exist only in memory while the user is filling out the editor dialog.
// They are NOT database entities — they get converted to database objects on Save.

// Represents one set while the user is editing it.
// All values are Strings because they come from text input fields.
data class SetDraft(
    val tempId: String = UUID.randomUUID().toString(), // temporary ID for list key
    val reps: String = "8",        // default to 8 reps
    val weightLbs: String = "0",   // weight stored as lbs string internally
    val toFailure: Boolean = false  // whether this is a "to failure" set
)

// Represents the full exercise entry while the user is editing it.
data class EntryDraft(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val sets: List<SetDraft> = listOf(SetDraft()),  // start with one empty set
    val notes: String = "",
    val supersetGroupId: String? = null,
    val useLbs: Boolean = true
)

// ── UI state ────────────────────────────────────────────────────────────────

// A convenience wrapper combining an entry entity with its sets AND the
// exercise name (so the card doesn't have to look up the name from the DB).
data class EntryWithSets(
    val entry: WorkoutEntryEntity,
    val sets: List<SetEntity>,
    val exerciseName: String
)

// Everything the Workout screen needs to render, in one snapshot.
data class WorkoutUiState(
    val workoutId: String = "",
    val workoutName: String = "",                          // shown in the top bar
    val entriesWithSets: List<EntryWithSets> = emptyList(),// the exercise cards
    val exercises: List<ExerciseEntity> = emptyList(),     // exercise picker list
    val exerciseSearch: String = "",                       // current search query
    val showExercisePicker: Boolean = false,               // whether the picker sheet is open
    val editingDraft: EntryDraft? = null,                  // non-null = editor dialog open
    val editingEntryId: String? = null,                    // non-null = editing existing entry
    val isSaving: Boolean = false,                         // true while DB write is happening
    val error: String? = null,                             // validation error message
    val showSupersetPickerForEntryId: String? = null,      // non-null = superset picker open
    val showRenameDialog: Boolean = false                  // whether the rename dialog is open
)

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    // Cache of exerciseId → exercise name so we don't re-query the database
    // every time the entry list is rebuilt (it can change frequently).
    private val exerciseNameCache = mutableMapOf<String, String>()

    // Called when the screen is first displayed. Loads the workout metadata
    // and subscribes to the live entry+sets stream.
    fun loadWorkout(workoutId: String) {
        _uiState.value = _uiState.value.copy(workoutId = workoutId)

        // Fetch the workout name (one-shot — it doesn't change on its own).
        viewModelScope.launch {
            val workout = workoutRepository.getWorkoutById(workoutId)
            _uiState.value = _uiState.value.copy(
                // If notes is blank, fall back to "Workout" as the display name.
                workoutName = workout?.notes?.ifBlank { null } ?: "Workout"
            )
        }

        // Subscribe to the live entry+sets stream. This Flow re-emits every
        // time an entry is added, edited, or deleted — the UI updates automatically.
        viewModelScope.launch {
            workoutRepository.getEntriesWithSetsForWorkout(workoutId).collect { relations ->
                val entriesWithSets = relations.map { rel ->
                    // Look up the exercise name from cache or DB.
                    val name = exerciseNameCache[rel.entry.exerciseId]
                        ?: exerciseRepository.getExerciseById(rel.entry.exerciseId)?.name
                            ?.also { exerciseNameCache[rel.entry.exerciseId] = it }
                        ?: rel.entry.exerciseId  // fallback: show the ID if name not found
                    EntryWithSets(entry = rel.entry, sets = rel.sets, exerciseName = name)
                }
                _uiState.value = _uiState.value.copy(entriesWithSets = entriesWithSets)
            }
        }
    }

    // ── Exercise picker ───────────────────────────────────────────────────────

    // Updates the search query and re-queries the exercise list from the DB.
    fun setExerciseSearch(query: String) {
        _uiState.value = _uiState.value.copy(exerciseSearch = query)
        viewModelScope.launch {
            exerciseRepository.searchExercises(query).collect { exercises ->
                _uiState.value = _uiState.value.copy(exercises = exercises)
            }
        }
    }

    // Opens the exercise picker bottom sheet and pre-loads all exercises.
    fun openExercisePicker() {
        _uiState.value = _uiState.value.copy(showExercisePicker = true, exerciseSearch = "")
        setExerciseSearch("")  // trigger a load with empty search (returns everything)
    }

    // Closes the picker and discards any in-progress draft.
    fun closeExercisePicker() {
        _uiState.value = _uiState.value.copy(
            showExercisePicker = false,
            editingDraft = null,
            editingEntryId = null
        )
    }

    // Called when the user taps an exercise in the picker.
    // Looks up the last time this exercise was used to pre-fill the sets/weight.
    fun selectExercise(exercise: ExerciseEntity) {
        exerciseNameCache[exercise.id] = exercise.name
        viewModelScope.launch {
            // Auto-fill: use the sets from the most recent session with this exercise.
            val lastSets = workoutRepository.getLastSetsForExercise(exercise.id)
            val draftSets = if (lastSets.isNotEmpty()) {
                lastSets.map { s ->
                    SetDraft(reps = s.reps.toString(), weightLbs = s.weightLbs.toString(), toFailure = s.toFailure)
                }
            } else {
                listOf(SetDraft())  // no history — start with one blank set
            }
            _uiState.value = _uiState.value.copy(
                showExercisePicker = false,
                editingDraft = EntryDraft(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    sets = draftSets,
                    useLbs = preferencesRepository.useLbs.value  // use the global preference
                ),
                editingEntryId = null  // null = this is a NEW entry, not an edit
            )
        }
    }

    // ── Entry editing ──────────────────────────────────────────────────────────

    // Opens the editor dialog pre-populated with an existing entry's data.
    fun editEntry(entryWithSets: EntryWithSets) {
        val draftSets = if (entryWithSets.sets.isNotEmpty()) {
            entryWithSets.sets.map { s ->
                SetDraft(tempId = s.id, reps = s.reps.toString(), weightLbs = s.weightLbs.toString(), toFailure = s.toFailure)
            }
        } else {
            listOf(SetDraft())
        }
        _uiState.value = _uiState.value.copy(
            editingDraft = EntryDraft(
                exerciseId = entryWithSets.entry.exerciseId,
                exerciseName = entryWithSets.exerciseName,
                sets = draftSets,
                notes = entryWithSets.entry.notes,
                supersetGroupId = entryWithSets.entry.supersetGroupId,
                useLbs = entryWithSets.entry.useLbs
            ),
            editingEntryId = entryWithSets.entry.id  // non-null = UPDATE mode
        )
    }

    // Replaces the current draft with an updated version (called by the UI on
    // every change in the editor, e.g. toggling kg/lbs).
    fun updateDraft(draft: EntryDraft) {
        _uiState.value = _uiState.value.copy(editingDraft = draft)
    }

    // Discards the draft and closes the editor dialog without saving.
    fun cancelDraft() {
        _uiState.value = _uiState.value.copy(editingDraft = null, editingEntryId = null)
    }

    // ── Per-set mutations ──────────────────────────────────────────────────────

    // Adds a new set to the draft, copying the values from the last set as a
    // convenient starting point (the user usually does similar sets).
    fun addSetToDraft() {
        val draft = _uiState.value.editingDraft ?: return
        val lastSet = draft.sets.lastOrNull() ?: SetDraft()
        // Give the new set a fresh tempId but keep toFailure = false.
        val newSet = lastSet.copy(tempId = UUID.randomUUID().toString(), toFailure = false)
        _uiState.value = _uiState.value.copy(editingDraft = draft.copy(sets = draft.sets + newSet))
    }

    // Removes a set at the given position. Guard: can't go below 1 set.
    fun removeSetFromDraft(index: Int) {
        val draft = _uiState.value.editingDraft ?: return
        if (draft.sets.size <= 1) return
        val newSets = draft.sets.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(editingDraft = draft.copy(sets = newSets))
    }

    // Replaces the set at a given position with an updated version.
    // Called whenever the user changes reps, weight, or the to-failure toggle.
    fun updateSetInDraft(index: Int, updated: SetDraft) {
        val draft = _uiState.value.editingDraft ?: return
        val newSets = draft.sets.toMutableList()
        if (index in newSets.indices) newSets[index] = updated
        _uiState.value = _uiState.value.copy(editingDraft = draft.copy(sets = newSets))
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    // Converts the in-memory draft into real database entities and saves them.
    // Handles both the "add new entry" and "update existing entry" cases.
    fun saveDraft() {
        val state = _uiState.value
        val draft = state.editingDraft ?: return

        // Validate — must have an exercise and at least one set.
        if (draft.exerciseId.isBlank() || draft.sets.isEmpty()) {
            _uiState.value = state.copy(error = "Add at least one set.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                // Compute summary fields stored on the WorkoutEntryEntity.
                val setsCount = draft.sets.size
                val avgReps = draft.sets.mapNotNull { it.reps.toIntOrNull() }
                    .takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
                val maxWeightLbs = draft.sets.mapNotNull { it.weightLbs.toFloatOrNull() }.maxOrNull() ?: 0f

                if (state.editingEntryId != null) {
                    // ── UPDATE existing entry ─────────────────────────────────
                    val existing = state.entriesWithSets.find { it.entry.id == state.editingEntryId }?.entry
                    if (existing != null) {
                        workoutRepository.updateEntry(
                            existing.copy(sets = setsCount, reps = avgReps, weightKg = maxWeightLbs,
                                notes = draft.notes, supersetGroupId = draft.supersetGroupId,
                                useLbs = draft.useLbs)
                        )
                        // Wipe the old sets and re-insert from the draft.
                        // Simpler than diffing old vs new sets individually.
                        workoutRepository.deleteAllSetsForEntry(existing.id)
                        workoutRepository.addSets(draft.sets.mapIndexed { i, s ->
                            SetEntity(entryId = existing.id, setNumber = i + 1,
                                reps = s.reps.toIntOrNull() ?: 0,
                                weightLbs = s.weightLbs.toFloatOrNull() ?: 0f,
                                toFailure = s.toFailure)
                        })
                    }
                } else {
                    // ── INSERT new entry ──────────────────────────────────────
                    val entry = WorkoutEntryEntity(
                        workoutId = state.workoutId, exerciseId = draft.exerciseId,
                        sets = setsCount, reps = avgReps, weightKg = maxWeightLbs,
                        notes = draft.notes, supersetGroupId = draft.supersetGroupId,
                        useLbs = draft.useLbs
                    )
                    workoutRepository.addEntry(entry)
                    workoutRepository.addSets(draft.sets.mapIndexed { i, s ->
                        SetEntity(entryId = entry.id, setNumber = i + 1,
                            reps = s.reps.toIntOrNull() ?: 0,
                            weightLbs = s.weightLbs.toFloatOrNull() ?: 0f,
                            toFailure = s.toFailure)
                    })
                }
                // Close the editor dialog.
                _uiState.value = _uiState.value.copy(isSaving = false, editingDraft = null, editingEntryId = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    // Deletes an exercise entry from the workout.
    // The database CASCADE rule automatically deletes its sets too.
    fun deleteEntry(entry: WorkoutEntryEntity) {
        viewModelScope.launch { workoutRepository.deleteEntry(entry) }
    }

    // ── Superset ───────────────────────────────────────────────────────────────
    // A "superset" is when two exercises are performed back-to-back with no rest.
    // They are linked by a shared random UUID (supersetGroupId).

    // Opens the superset picker for the given entry.
    fun requestLinkSuperset(entryId: String) {
        _uiState.value = _uiState.value.copy(showSupersetPickerForEntryId = entryId)
    }

    // Closes the superset picker without making any change.
    fun dismissSupersetPicker() {
        _uiState.value = _uiState.value.copy(showSupersetPickerForEntryId = null)
    }

    // Links two entries as a superset by giving them the same random group ID.
    fun linkSuperset(entryIdA: String, entryIdB: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val a = state.entriesWithSets.find { it.entry.id == entryIdA }?.entry ?: return@launch
            val b = state.entriesWithSets.find { it.entry.id == entryIdB }?.entry ?: return@launch
            val groupId = UUID.randomUUID().toString()
            workoutRepository.updateEntry(a.copy(supersetGroupId = groupId))
            workoutRepository.updateEntry(b.copy(supersetGroupId = groupId))
            _uiState.value = _uiState.value.copy(showSupersetPickerForEntryId = null)
        }
    }

    // Removes the superset link from an entry by setting its groupId to null.
    fun unlinkSuperset(entry: WorkoutEntryEntity) {
        viewModelScope.launch { workoutRepository.updateEntry(entry.copy(supersetGroupId = null)) }
    }

    // ── Rename workout ─────────────────────────────────────────────────────────

    // Opens the rename dialog.
    fun showRenameDialog() {
        _uiState.value = _uiState.value.copy(showRenameDialog = true)
    }

    // Closes the rename dialog without saving.
    fun dismissRenameDialog() {
        _uiState.value = _uiState.value.copy(showRenameDialog = false)
    }

    // Saves the new workout name to the database and updates the top bar.
    fun updateWorkoutName(newName: String) {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkoutById(_uiState.value.workoutId) ?: return@launch
            workoutRepository.updateWorkoutNotes(workout, newName)
            _uiState.value = _uiState.value.copy(
                workoutName = newName.ifBlank { "Workout" },
                showRenameDialog = false
            )
        }
    }

    // Clears the error message (called after the error has been displayed).
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val exerciseRepository: ExerciseRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WorkoutViewModel(workoutRepository, exerciseRepository, preferencesRepository) as T
    }
}
