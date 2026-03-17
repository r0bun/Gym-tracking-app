// This ViewModel manages all the data and actions for the Home screen.
// The Home screen is the landing page after login — it shows a welcome card,
// a list of recent workouts, and actions to start a new workout or sign out.

package com.trackapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackapp.data.local.entity.WorkoutEntity
import com.trackapp.data.repository.AuthRepository
import com.trackapp.data.repository.ExerciseRepository
import com.trackapp.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Everything the Home screen needs to display, in one object.
data class HomeUiState(
    val recentWorkouts: List<WorkoutEntity> = emptyList(), // the 5 most recent sessions
    val allWorkouts: List<WorkoutEntity> = emptyList(),    // all workouts — used in the template picker
    val isLoading: Boolean = true,                         // show spinner on first load
    val userEmail: String = "",                            // displayed in the top bar
    val isSyncingExercises: Boolean = false,               // true while syncing — disables button
    val syncMessage: String? = null                        // shown as a snackbar after sync
)

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Set the user's email for display in the top bar (read once, not a flow).
        _uiState.value = _uiState.value.copy(
            userEmail = authRepository.currentUser?.email ?: ""
        )

        viewModelScope.launch {
            workoutRepository.getAllWorkouts()
                .collect { workouts ->
                    _uiState.value = _uiState.value.copy(
                        // Show only the 5 most recent on the home screen card list.
                        recentWorkouts = workouts.take(5),
                        // Expose the full list so the template picker can show all workouts.
                        allWorkouts = workouts,
                        isLoading = false
                    )
                }
        }
    }

    // Creates a new workout in the database and passes its ID to the callback
    // so AppNavigation can open the Workout screen for it.
    // If templateId is provided, the exercises from that workout are copied in.
    fun startNewWorkout(name: String, templateId: String? = null, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val workout = workoutRepository.createWorkout(name)
            if (templateId != null) {
                workoutRepository.copyEntriesFromWorkout(templateId, workout.id)
            }
            onCreated(workout.id)
        }
    }

    // Formats a timestamp for display in workout cards.
    // E.g. 1700000000000 → "Mon, Nov 14 • 3:33 PM"
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Downloads the latest exercise list from Supabase and updates the local cache.
    // Shown as a manual refresh button in the top bar for cases where the
    // initial sync on login failed, or new exercises were added to Supabase.
    fun syncExercises() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncingExercises = true, syncMessage = null)
            try {
                exerciseRepository.syncFromRemote()
                _uiState.value = _uiState.value.copy(
                    isSyncingExercises = false,
                    syncMessage = "Exercises synced successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncingExercises = false,
                    syncMessage = "Sync failed: ${e.message}"
                )
            }
        }
    }

    // Clears the snackbar message after it has been shown.
    fun clearSyncMessage() {
        _uiState.value = _uiState.value.copy(syncMessage = null)
    }

    // Signs the user out. Called from the sign-out dialog.
    // After this, AuthRepository emits isSignedIn = false, which triggers
    // automatic navigation to the Login screen in AppNavigation.
    suspend fun signOut() = authRepository.signOut()

    class Factory(
        private val workoutRepository: WorkoutRepository,
        private val authRepository: AuthRepository,
        private val exerciseRepository: ExerciseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(workoutRepository, authRepository, exerciseRepository) as T
    }
}
