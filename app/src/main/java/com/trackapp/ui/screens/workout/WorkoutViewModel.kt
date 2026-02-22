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

data class SetDraft(
    val tempId: String = UUID.randomUUID().toString(),
    val reps: String = "8",
    val weightLbs: String = "0",
    val toFailure: Boolean = false
)

data class EntryDraft(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val sets: List<SetDraft> = listOf(SetDraft()),
    val notes: String = "",
    val supersetGroupId: String? = null,
    val useLbs: Boolean = true
)

// ── UI state ────────────────────────────────────────────────────────────────

data class EntryWithSets(
    val entry: WorkoutEntryEntity,
    val sets: List<SetEntity>,
    val exerciseName: String
)

data class WorkoutUiState(
    val workoutId: String = "",
    val workoutName: String = "",
    val entriesWithSets: List<EntryWithSets> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val exerciseSearch: String = "",
    val showExercisePicker: Boolean = false,
    val editingDraft: EntryDraft? = null,
    val editingEntryId: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showSupersetPickerForEntryId: String? = null
)

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    /** Local cache of exerciseId → name so we don't re-query DB on every list update. */
    private val exerciseNameCache = mutableMapOf<String, String>()

    fun loadWorkout(workoutId: String) {
        _uiState.value = _uiState.value.copy(workoutId = workoutId)

        viewModelScope.launch {
            val workout = workoutRepository.getWorkoutById(workoutId)
            _uiState.value = _uiState.value.copy(
                workoutName = workout?.notes?.ifBlank { null } ?: "Workout"
            )
        }

        viewModelScope.launch {
            workoutRepository.getEntriesWithSetsForWorkout(workoutId).collect { relations ->
                val entriesWithSets = relations.map { rel ->
                    val name = exerciseNameCache[rel.entry.exerciseId]
                        ?: exerciseRepository.getExerciseById(rel.entry.exerciseId)?.name
                            ?.also { exerciseNameCache[rel.entry.exerciseId] = it }
                        ?: rel.entry.exerciseId
                    EntryWithSets(entry = rel.entry, sets = rel.sets, exerciseName = name)
                }
                _uiState.value = _uiState.value.copy(entriesWithSets = entriesWithSets)
            }
        }
    }

    // ── Exercise picker ───────────────────────────────────────────────────

    fun setExerciseSearch(query: String) {
        _uiState.value = _uiState.value.copy(exerciseSearch = query)
        viewModelScope.launch {
            exerciseRepository.searchExercises(query).collect { exercises ->
                _uiState.value = _uiState.value.copy(exercises = exercises)
            }
        }
    }

    fun openExercisePicker() {
        _uiState.value = _uiState.value.copy(showExercisePicker = true, exerciseSearch = "")
        setExerciseSearch("")
    }

    fun closeExercisePicker() {
        _uiState.value = _uiState.value.copy(
            showExercisePicker = false,
            editingDraft = null,
            editingEntryId = null
        )
    }

    fun selectExercise(exercise: ExerciseEntity) {
        exerciseNameCache[exercise.id] = exercise.name
        viewModelScope.launch {
            val lastSets = workoutRepository.getLastSetsForExercise(exercise.id)
            val draftSets = if (lastSets.isNotEmpty()) {
                lastSets.map { s ->
                    SetDraft(reps = s.reps.toString(), weightLbs = s.weightLbs.toString(), toFailure = s.toFailure)
                }
            } else {
                listOf(SetDraft())
            }
            _uiState.value = _uiState.value.copy(
                showExercisePicker = false,
                editingDraft = EntryDraft(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    sets = draftSets,
                    useLbs = preferencesRepository.useLbs.value
                ),
                editingEntryId = null
            )
        }
    }

    // ── Entry editing ─────────────────────────────────────────────────────

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
            editingEntryId = entryWithSets.entry.id
        )
    }

    fun updateDraft(draft: EntryDraft) {
        _uiState.value = _uiState.value.copy(editingDraft = draft)
    }

    fun cancelDraft() {
        _uiState.value = _uiState.value.copy(editingDraft = null, editingEntryId = null)
    }

    // ── Per-set mutations ─────────────────────────────────────────────────

    fun addSetToDraft() {
        val draft = _uiState.value.editingDraft ?: return
        val lastSet = draft.sets.lastOrNull() ?: SetDraft()
        val newSet = lastSet.copy(tempId = UUID.randomUUID().toString(), toFailure = false)
        _uiState.value = _uiState.value.copy(editingDraft = draft.copy(sets = draft.sets + newSet))
    }

    fun removeSetFromDraft(index: Int) {
        val draft = _uiState.value.editingDraft ?: return
        if (draft.sets.size <= 1) return
        val newSets = draft.sets.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(editingDraft = draft.copy(sets = newSets))
    }

    fun updateSetInDraft(index: Int, updated: SetDraft) {
        val draft = _uiState.value.editingDraft ?: return
        val newSets = draft.sets.toMutableList()
        if (index in newSets.indices) newSets[index] = updated
        _uiState.value = _uiState.value.copy(editingDraft = draft.copy(sets = newSets))
    }

    // ── Save ──────────────────────────────────────────────────────────────

    fun saveDraft() {
        val state = _uiState.value
        val draft = state.editingDraft ?: return
        if (draft.exerciseId.isBlank() || draft.sets.isEmpty()) {
            _uiState.value = state.copy(error = "Add at least one set.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val setsCount = draft.sets.size
                val avgReps = draft.sets.mapNotNull { it.reps.toIntOrNull() }
                    .takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
                val maxWeightLbs = draft.sets.mapNotNull { it.weightLbs.toFloatOrNull() }.maxOrNull() ?: 0f

                if (state.editingEntryId != null) {
                    val existing = state.entriesWithSets.find { it.entry.id == state.editingEntryId }?.entry
                    if (existing != null) {
                        workoutRepository.updateEntry(
                            existing.copy(sets = setsCount, reps = avgReps, weightKg = maxWeightLbs,
                                notes = draft.notes, supersetGroupId = draft.supersetGroupId,
                                useLbs = draft.useLbs)
                        )
                        workoutRepository.deleteAllSetsForEntry(existing.id)
                        workoutRepository.addSets(draft.sets.mapIndexed { i, s ->
                            SetEntity(entryId = existing.id, setNumber = i + 1,
                                reps = s.reps.toIntOrNull() ?: 0,
                                weightLbs = s.weightLbs.toFloatOrNull() ?: 0f,
                                toFailure = s.toFailure)
                        })
                    }
                } else {
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
                _uiState.value = _uiState.value.copy(isSaving = false, editingDraft = null, editingEntryId = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun deleteEntry(entry: WorkoutEntryEntity) {
        viewModelScope.launch { workoutRepository.deleteEntry(entry) }
    }

    // ── Superset ──────────────────────────────────────────────────────────

    fun requestLinkSuperset(entryId: String) {
        _uiState.value = _uiState.value.copy(showSupersetPickerForEntryId = entryId)
    }

    fun dismissSupersetPicker() {
        _uiState.value = _uiState.value.copy(showSupersetPickerForEntryId = null)
    }

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

    fun unlinkSuperset(entry: WorkoutEntryEntity) {
        viewModelScope.launch { workoutRepository.updateEntry(entry.copy(supersetGroupId = null)) }
    }

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
