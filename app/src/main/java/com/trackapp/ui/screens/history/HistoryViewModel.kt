// This ViewModel holds and manages all the data for the History screen.
//
// ViewModel is an Android architecture component designed to survive screen
// rotations and other configuration changes. If the user rotates their phone,
// the screen is rebuilt but the ViewModel (and its data) stays alive.
// Without ViewModel, the workout list would be re-fetched on every rotation.
//
// The History screen shows a list of past workouts and lets the user delete them.

package com.trackapp.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackapp.data.local.entity.WorkoutEntity
import com.trackapp.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// UiState = a single data class that captures everything the UI needs to render.
// Using a single state object makes it easy to see all possible screen states
// at a glance, and ensures the UI is always consistent.
data class HistoryUiState(
    val workouts: List<WorkoutEntity> = emptyList(), // the list of past workouts
    val isLoading: Boolean = true,                   // show a spinner while fetching
    val deleteTargetId: String? = null               // non-null = show a delete dialog
)

class HistoryViewModel(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    // MutableStateFlow = the internal, writable state container.
    // Only this ViewModel can change the state; the UI only reads it.
    private val _uiState = MutableStateFlow(HistoryUiState())

    // The public read-only version exposed to the screen.
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // init runs immediately when the ViewModel is created.
    // viewModelScope is a coroutine scope that is automatically cancelled
    // when the ViewModel is destroyed (e.g. user navigates away permanently).
    init {
        viewModelScope.launch {
            // Collect the live workout list from the repository.
            // Every time a workout is added or deleted, this block runs again
            // and the UI recomposes automatically.
            workoutRepository.getAllWorkouts().collect { workouts ->
                _uiState.value = _uiState.value.copy(workouts = workouts, isLoading = false)
            }
        }
    }

    // Step 1 of deletion: the user taps the delete icon.
    // We save the target ID in state, which triggers the confirmation dialog.
    fun confirmDelete(workoutId: String) {
        _uiState.value = _uiState.value.copy(deleteTargetId = workoutId)
    }

    // The user tapped "Cancel" in the dialog — clear the target, hide the dialog.
    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(deleteTargetId = null)
    }

    // Step 2 of deletion: the user confirmed. Actually delete from the database.
    fun executeDelete() {
        val id = _uiState.value.deleteTargetId ?: return  // safety check
        _uiState.value = _uiState.value.copy(deleteTargetId = null) // close dialog immediately
        viewModelScope.launch {
            workoutRepository.deleteWorkout(id)
            // The Flow in init {} will automatically emit the updated list
            // without the deleted workout — no manual refresh needed.
        }
    }

    // Converts a Unix timestamp (milliseconds) to a human-readable date string.
    // E.g. 1700000000000 → "Mon, Nov 14 2023 • 3:33 PM"
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Factory tells Android how to create this ViewModel with its dependencies.
    // Android creates ViewModels internally and won't pass constructor arguments
    // unless we provide a Factory like this.
    class Factory(
        private val workoutRepository: WorkoutRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(workoutRepository) as T
    }
}
