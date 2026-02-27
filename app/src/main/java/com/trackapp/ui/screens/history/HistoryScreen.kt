// This file defines the visual layout of the History screen.
// In Jetpack Compose, a "Composable" function describes what the UI looks like
// right now — Compose figures out what changed and updates only those parts.
// There is no XML; the entire layout is written as Kotlin function calls.

package com.trackapp.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trackapp.data.local.entity.WorkoutEntity
import com.trackapp.ui.theme.Accent
import com.trackapp.ui.theme.TrackAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenWorkout: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    HistoryScreenContent(
        uiState = uiState,
        formatDate = viewModel::formatDate,
        onOpenWorkout = onOpenWorkout,
        onBack = onBack,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete,
        onExecuteDelete = viewModel::executeDelete
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    uiState: HistoryUiState,
    formatDate: (Long) -> String = { "" },
    onOpenWorkout: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onConfirmDelete: (String) -> Unit = {},
    onCancelDelete: () -> Unit = {},
    onExecuteDelete: () -> Unit = {}
) {
    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (uiState.deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Delete Workout?") },
            text = { Text("This will permanently delete the workout and all its exercises.") },
            confirmButton = {
                TextButton(onClick = onExecuteDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") },
                navigationIcon = {
                    // Back arrow — calls the lambda passed in from AppNavigation.
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        // "padding" is the inner padding that avoids overlap with the top bar.
        // We must pass it to our content so content isn't hidden behind the bar.

        when {
            // Still loading from the database — show a centered spinner.
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            // No workouts yet — show an empty state with an icon and message.
            uiState.workouts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No workout history yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Workouts exist — render the scrollable list.
            else -> {
                // LazyColumn = efficient scrollable list. It only renders the
                // items currently visible on screen (like RecyclerView in XML).
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),  // gap between cards
                    contentPadding = PaddingValues(vertical = 16.dp)    // padding top and bottom
                ) {
                    // "items" iterates the list. key = {} helps Compose animate
                    // additions/removals efficiently by identifying each item uniquely.
                    items(uiState.workouts, key = { it.id }) { workout ->
                        // Each workout is displayed as a Material "Card" (an elevated rectangle).
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenWorkout(workout.id) }, // tap → open workout
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dumbbell icon on the left.
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                // Middle column: workout name (or date if unnamed) + date.
                                Column(modifier = Modifier.weight(1f)) {
                                    // If the workout has a name, show it as the primary text.
                                    // Otherwise show the formatted date.
                                    Text(
                                        text = if (workout.notes.isNotBlank()) workout.notes
                                               else formatDate(workout.date),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis // "…" if too long
                                    )
                                    // If there IS a name, show the date as secondary text below.
                                    if (workout.notes.isNotBlank()) {
                                        Text(
                                            text = formatDate(workout.date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Trash icon on the right — opens the delete confirmation dialog.
                                IconButton(onClick = { onConfirmDelete(workout.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0F0F11)
@Composable
internal fun HistoryScreenPreview_WithWorkouts() {
    TrackAppTheme {
        HistoryScreenContent(
            uiState = HistoryUiState(
                isLoading = false,
                workouts = listOf(
                    WorkoutEntity(id = "1", notes = "Push Day", date = 1700000000000),
                    WorkoutEntity(id = "2", notes = "Pull Day", date = 1699900000000),
                    WorkoutEntity(id = "3", notes = "", date = 1699800000000)
                )
            ),
            formatDate = { "Mon, Nov 14 2023 \u2022 3:33 PM" }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F11)
@Composable
internal fun HistoryScreenPreview_Empty() {
    TrackAppTheme {
        HistoryScreenContent(
            uiState = HistoryUiState(isLoading = false, workouts = emptyList())
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F11)
@Composable
internal fun HistoryScreenPreview_Loading() {
    TrackAppTheme {
        HistoryScreenContent(
            uiState = HistoryUiState(isLoading = true)
        )
    }
}
