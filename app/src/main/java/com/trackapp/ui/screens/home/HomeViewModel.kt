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

data class HomeUiState(
    val recentWorkouts: List<WorkoutEntity> = emptyList(),
    val isLoading: Boolean = true,
    val userEmail: String = "",
    val isSyncingExercises: Boolean = false,
    val syncMessage: String? = null
)

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            userEmail = authRepository.currentUser?.email ?: ""
        )
        viewModelScope.launch {
            workoutRepository.getAllWorkouts()
                .map { it.take(5) } // show 5 most recent on home
                .collect { workouts ->
                    _uiState.value = _uiState.value.copy(
                        recentWorkouts = workouts,
                        isLoading = false
                    )
                }
        }
    }

    fun startNewWorkout(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val workout = workoutRepository.createWorkout(name)
            onCreated(workout.id)
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

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

    fun clearSyncMessage() {
        _uiState.value = _uiState.value.copy(syncMessage = null)
    }

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
