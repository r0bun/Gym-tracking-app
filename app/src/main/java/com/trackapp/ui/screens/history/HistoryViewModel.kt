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

data class HistoryUiState(
    val workouts: List<WorkoutEntity> = emptyList(),
    val isLoading: Boolean = true,
    val deleteTargetId: String? = null
)

class HistoryViewModel(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            workoutRepository.getAllWorkouts().collect { workouts ->
                _uiState.value = _uiState.value.copy(workouts = workouts, isLoading = false)
            }
        }
    }

    fun confirmDelete(workoutId: String) {
        _uiState.value = _uiState.value.copy(deleteTargetId = workoutId)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(deleteTargetId = null)
    }

    fun executeDelete() {
        val id = _uiState.value.deleteTargetId ?: return
        _uiState.value = _uiState.value.copy(deleteTargetId = null)
        viewModelScope.launch {
            workoutRepository.deleteWorkout(id)
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    class Factory(
        private val workoutRepository: WorkoutRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(workoutRepository) as T
    }
}
