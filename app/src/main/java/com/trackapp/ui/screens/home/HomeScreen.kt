// This file defines the visual layout of the Home screen.
// The Home screen is the main landing page after login. It shows:
//   - A top bar with the user's email, a sync button, history, and sign-out
//   - A hero card ("Ready to train?")
//   - A list of recent workouts (up to 5)
//   - A "Start Workout" floating action button (FAB)

package com.trackapp.ui.screens.home

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
import androidx.compose.ui.unit.dp
import com.trackapp.ui.theme.Accent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartWorkout: (String) -> Unit,  // navigate to a new workout
    onOpenWorkout: (String) -> Unit,   // navigate to an existing workout
    onSignOut: () -> Unit,             // navigate back to Login
    onOpenHistory: () -> Unit,         // navigate to the History screen
    onOpenSettings: () -> Unit         // navigate to the Settings screen (theme color, etc.)
) {
    val uiState by viewModel.uiState.collectAsState()

    // rememberCoroutineScope() gives us a scope for launching coroutines from
    // within a composable (e.g. from a button click handler).
    val scope = rememberCoroutineScope()

    // SnackbarHostState manages the snackbar queue (the toast-like message at
    // the bottom of the screen after a sync).
    val snackbarHostState = remember { SnackbarHostState() }

    // Local UI state for dialog visibility and the new workout name input.
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingWorkoutName by remember { mutableStateOf("") }

    // Show the sync result as a snackbar whenever the syncMessage changes.
    // LaunchedEffect re-runs whenever "message" changes.
    uiState.syncMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSyncMessage()  // clear so the snackbar doesn't re-show
        }
    }

    // ── Name dialog — shown when the user taps "Start Workout" ───────────────
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false; pendingWorkoutName = "" },
            title = { Text("Name your session") },
            text = {
                OutlinedTextField(
                    value = pendingWorkoutName,
                    onValueChange = { pendingWorkoutName = it },
                    placeholder = { Text("e.g. Push Day, Leg Day…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    showNameDialog = false
                    // Create the workout in the DB and navigate to it.
                    viewModel.startNewWorkout(pendingWorkoutName, onStartWorkout)
                    pendingWorkoutName = ""
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false; pendingWorkoutName = "" }) { Text("Cancel") }
            }
        )
    }

    // ── Sign out confirmation dialog ──────────────────────────────────────────
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        // Sign out is a suspend function, so we launch a coroutine.
                        scope.launch {
                            viewModel.signOut()
                            onSignOut()  // navigate back to Login
                        }
                    }
                ) { Text("Sign Out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Main scaffold ─────────────────────────────────────────────────────────
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // renders snackbars
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TrackApp", style = MaterialTheme.typography.titleLarge)
                        // Show the user's email below the app name (if available).
                        if (uiState.userEmail.isNotBlank()) {
                            Text(
                                text = uiState.userEmail,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Sync exercises button — shows a spinner while syncing.
                    IconButton(
                        onClick = { viewModel.syncExercises() },
                        enabled = !uiState.isSyncingExercises
                    ) {
                        if (uiState.isSyncingExercises) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Sync Exercises")
                        }
                    }
                    // History screen button.
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                    // Settings button — opens the theme color picker and other preferences.
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    // Sign out button.
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        // Floating Action Button — the prominent "Start Workout" button.
        // It floats above the list so it's always accessible.
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Start Workout") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { pendingWorkoutName = ""; showNameDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->

        // ── Loading state ─────────────────────────────────────────────────────
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // ── Hero card — always shown at the top ───────────────────────
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ready to train?",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Tap the button below to log your session.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // ── Recent workouts section ───────────────────────────────────
                if (uiState.recentWorkouts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recent workouts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // One card per recent workout.
                    items(uiState.recentWorkouts, key = { it.id }) { workout ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenWorkout(workout.id) },
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
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    // Show name if set, otherwise fall back to the date.
                                    Text(
                                        text = if (workout.notes.isNotBlank()) workout.notes
                                               else viewModel.formatDate(workout.date),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // If named, show the date as secondary info.
                                    if (workout.notes.isNotBlank()) {
                                        Text(
                                            text = viewModel.formatDate(workout.date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                // Chevron ">" indicating the card is tappable.
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Empty state — shown until the user starts their first workout.
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No workouts yet.\nHit the button to start your first one!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Extra space at the bottom so the FAB doesn't overlap the last card.
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
