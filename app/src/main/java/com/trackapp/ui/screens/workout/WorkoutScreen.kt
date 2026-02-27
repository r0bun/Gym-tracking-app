// This file defines the visual layout of the Workout screen — the most complex
// screen in the app. It contains several composables:
//
//   WorkoutScreen       — the main screen with top bar, exercise list, and FAB
//   EntryCard           — one card per exercise (shows sets, edit/delete/superset)
//   EntryEditorDialog   — the dialog for adding or editing an exercise entry
//   SetEditorRow        — one row per set inside the editor dialog
//   SupersetPickerDialog— dialog to link two exercises as a superset
//   ExercisePickerSheet — bottom sheet for searching and selecting an exercise

package com.trackapp.ui.screens.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trackapp.data.local.entity.ExerciseEntity
import com.trackapp.data.local.entity.SetEntity
import com.trackapp.data.local.entity.WorkoutEntryEntity
import com.trackapp.ui.theme.Accent
import com.trackapp.ui.theme.TrackAppTheme

// ── Unit conversion helpers ──────────────────────────────────────────────────
// Weight is always stored internally in lbs (as a Float).
// These helpers convert between the stored lbs value and what the user sees.

// Converts the stored lbs string to the display value.
// If the user prefers kg, divide by 2.20462 (the lbs→kg factor).
private fun lbsToDisplay(lbs: String, useLbs: Boolean): String {
    if (useLbs) return lbs
    val f = lbs.toFloatOrNull() ?: return lbs
    return String.format("%.1f", f / 2.20462f)
}

// Converts the user-typed display value back to the internal lbs storage value.
// If the user is typing in kg, multiply by 2.20462 to get lbs.
private fun displayToLbs(display: String, useLbs: Boolean): String {
    if (useLbs) return display
    val f = display.toFloatOrNull() ?: return display
    return String.format("%.2f", f * 2.20462f)
}

// Returns "lbs" or "kg" for use as a field label.
private fun unitLabel(useLbs: Boolean) = if (useLbs) "lbs" else "kg"

// ── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    workoutId: String,  // the ID of the workout to display
    onBack: () -> Unit  // navigate back when the user taps the back arrow
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load the workout when this screen first appears (or when workoutId changes).
    LaunchedEffect(workoutId) { viewModel.loadWorkout(workoutId) }

    // Auto-clear the error state once the UI has had a chance to display it.
    uiState.error?.let { LaunchedEffect(it) { viewModel.clearError() } }

    WorkoutScreenContent(
        uiState = uiState,
        onBack = onBack,
        onShowRenameDialog = viewModel::showRenameDialog,
        onDismissRenameDialog = viewModel::dismissRenameDialog,
        onUpdateWorkoutName = viewModel::updateWorkoutName,
        onOpenExercisePicker = viewModel::openExercisePicker,
        onCloseExercisePicker = viewModel::closeExercisePicker,
        onExerciseSearchChange = viewModel::setExerciseSearch,
        onSelectExercise = viewModel::selectExercise,
        onUpdateDraft = viewModel::updateDraft,
        onAddSetToDraft = viewModel::addSetToDraft,
        onRemoveSetFromDraft = viewModel::removeSetFromDraft,
        onUpdateSetInDraft = viewModel::updateSetInDraft,
        onSaveDraft = viewModel::saveDraft,
        onCancelDraft = viewModel::cancelDraft,
        onEditEntry = viewModel::editEntry,
        onDeleteEntry = { viewModel.deleteEntry(it.entry) },
        onRequestLinkSuperset = { viewModel.requestLinkSuperset(it) },
        onUnlinkSuperset = { viewModel.unlinkSuperset(it.entry) },
        onLinkSuperset = viewModel::linkSuperset,
        onDismissSupersetPicker = viewModel::dismissSupersetPicker
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreenContent(
    uiState: WorkoutUiState,
    onBack: () -> Unit = {},
    onShowRenameDialog: () -> Unit = {},
    onDismissRenameDialog: () -> Unit = {},
    onUpdateWorkoutName: (String) -> Unit = {},
    onOpenExercisePicker: () -> Unit = {},
    onCloseExercisePicker: () -> Unit = {},
    onExerciseSearchChange: (String) -> Unit = {},
    onSelectExercise: (ExerciseEntity) -> Unit = {},
    onUpdateDraft: (EntryDraft) -> Unit = {},
    onAddSetToDraft: () -> Unit = {},
    onRemoveSetFromDraft: (Int) -> Unit = {},
    onUpdateSetInDraft: (Int, SetDraft) -> Unit = { _, _ -> },
    onSaveDraft: () -> Unit = {},
    onCancelDraft: () -> Unit = {},
    onEditEntry: (EntryWithSets) -> Unit = {},
    onDeleteEntry: (EntryWithSets) -> Unit = {},
    onRequestLinkSuperset: (String) -> Unit = {},
    onUnlinkSuperset: (EntryWithSets) -> Unit = {},
    onLinkSuperset: (String, String) -> Unit = { _, _ -> },
    onDismissSupersetPicker: () -> Unit = {}
) {
    // Local state for the rename text field (kept here so it doesn't live in ViewModel).
    var renameText by remember { mutableStateOf("") }

    // ── Rename workout dialog ────────────────────────────────────────────────
    if (uiState.showRenameDialog) {
        // Pre-fill the text field with the current name (but not "Workout" placeholder).
        LaunchedEffect(Unit) { renameText = uiState.workoutName.let { if (it == "Workout") "" else it } }
        AlertDialog(
            onDismissRequest = onDismissRenameDialog,
            title = { Text("Rename Workout") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    placeholder = { Text("e.g. Push Day, Leg Day…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateWorkoutName(renameText)
                    renameText = ""
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { onDismissRenameDialog(); renameText = "" }) { Text("Cancel") }
            }
        )
    }

    // ── Exercise picker bottom sheet ─────────────────────────────────────────
    if (uiState.showExercisePicker) {
        ModalBottomSheet(onDismissRequest = onCloseExercisePicker) {
            ExercisePickerSheet(
                exercises = uiState.exercises,
                searchQuery = uiState.exerciseSearch,
                onSearchChange = onExerciseSearchChange,
                onSelect = onSelectExercise
            )
        }
    }

    // ── Entry editor dialog ──────────────────────────────────────────────────
    uiState.editingDraft?.let { draft ->
        EntryEditorDialog(
            draft = draft,
            isEditing = uiState.editingEntryId != null,
            isSaving = uiState.isSaving,
            onDraftChange = onUpdateDraft,
            onAddSet = onAddSetToDraft,
            onRemoveSet = onRemoveSetFromDraft,
            onUpdateSet = onUpdateSetInDraft,
            onSave = onSaveDraft,
            onCancel = onCancelDraft
        )
    }

    // ── Superset picker dialog ───────────────────────────────────────────────
    uiState.showSupersetPickerForEntryId?.let { sourceId ->
        SupersetPickerDialog(
            sourceEntryId = sourceId,
            allEntries = uiState.entriesWithSets,
            onLink = { targetId -> onLinkSuperset(sourceId, targetId) },
            onDismiss = onDismissSupersetPicker
        )
    }

    // ── Scaffold (main layout) ────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // The workout title is tappable — clicking opens the rename dialog.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onShowRenameDialog() }
                    ) {
                        Text(uiState.workoutName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(4.dp))
                        // Small pencil icon next to the title hints it's editable.
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Rename workout",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        // FAB (+ button) opens the exercise picker to add a new exercise.
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenExercisePicker,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Exercise")
            }
        }
    ) { padding ->

        // ── Empty state ─────────────────────────────────────────────────────
        if (uiState.entriesWithSets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FitnessCenter, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No exercises yet", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap + to add your first exercise", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // ── Exercise card list ────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // One EntryCard per exercise logged in this workout.
                items(uiState.entriesWithSets, key = { it.entry.id }) { ews ->
                    EntryCard(
                        entryWithSets = ews,
                        onEdit = { onEditEntry(ews) },
                        onDelete = { onDeleteEntry(ews) },
                        onRequestSuperset = { onRequestLinkSuperset(ews.entry.id) },
                        onUnlinkSuperset = { onUnlinkSuperset(ews) }
                    )
                }
                // Extra padding at the bottom so the FAB doesn't cover the last card.
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ── EntryCard ────────────────────────────────────────────────────────────────
// Displays one exercise and all its sets in a card.
// "private" means it can only be used within this file.

@Composable
private fun EntryCard(
    entryWithSets: EntryWithSets,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRequestSuperset: () -> Unit,
    onUnlinkSuperset: () -> Unit
) {
    // Local state: whether to show the delete confirmation for this card.
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove exercise?") },
            text = { Text("Remove \"${entryWithSets.exerciseName}\" from this workout?") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // True if this exercise is part of a superset (has a group ID).
    val isSuperset = entryWithSets.entry.supersetGroupId != null

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row: exercise name + edit/delete buttons ───────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entryWithSets.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Show "SUPERSET" badge below the name if linked.
                    if (isSuperset) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "SUPERSET",
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Accent)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
            }

            // ── Per-set rows ──────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            if (entryWithSets.sets.isEmpty()) {
                Text("No sets recorded", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    entryWithSets.sets.forEach { set ->
                        val unit = unitLabel(entryWithSets.entry.useLbs)
                        // Convert the stored lbs value to the preferred display unit.
                        val weightDisplay = lbsToDisplay(set.weightLbs.toString(), entryWithSets.entry.useLbs)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "Set 1", "Set 2", etc.
                            Text(
                                text = "Set ${set.setNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(44.dp)
                            )
                            if (set.toFailure) {
                                // To-failure sets don't show reps — just weight + FAILURE badge.
                                Text(
                                    text = "weight: $weightDisplay $unit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "FAILURE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "reps: ${set.reps}  weight: $weightDisplay $unit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // ── Optional notes ────────────────────────────────────────────────
            if (entryWithSets.entry.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(entryWithSets.entry.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            // ── Superset action row ───────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Row {
                if (isSuperset) {
                    // Already in a superset — offer to unlink.
                    TextButton(onClick = onUnlinkSuperset, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Unlink Superset", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    // Not in a superset — offer to link with another exercise.
                    TextButton(onClick = onRequestSuperset, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = Accent)
                        Spacer(Modifier.width(4.dp))
                        Text("Superset", style = MaterialTheme.typography.labelSmall, color = Accent)
                    }
                }
            }
        }
    }
}

// ── EntryEditorDialog ────────────────────────────────────────────────────────
// The dialog where the user enters (or edits) all the details of an exercise:
// sets, reps, weight, notes, and the kg/lbs unit toggle.

@Composable
private fun EntryEditorDialog(
    draft: EntryDraft,        // the current in-memory data being edited
    isEditing: Boolean,       // true = updating an existing entry
    isSaving: Boolean,        // true = DB write in progress
    onDraftChange: (EntryDraft) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateSet: (Int, SetDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isEditing) "Edit ${draft.exerciseName}" else draft.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // ── kg / lbs toggle (per-exercise) ─────────────────────────
                // Each exercise independently remembers its unit preference.
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDraftChange(draft.copy(useLbs = false)) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "kg",
                            style = MaterialTheme.typography.labelLarge,
                            // Active unit is highlighted in Accent color and bold.
                            color = if (!draft.useLbs) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (!draft.useLbs) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text("|", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall)
                    TextButton(
                        onClick = { onDraftChange(draft.copy(useLbs = true)) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "lbs",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (draft.useLbs) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (draft.useLbs) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        text = {
            // verticalScroll allows the dialog content to scroll if there are many sets.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // One SetEditorRow per set in the draft.
                draft.sets.forEachIndexed { index, set ->
                    SetEditorRow(
                        index = index,
                        set = set,
                        useLbs = draft.useLbs,
                        canRemove = draft.sets.size > 1,  // can't remove the last set
                        onUpdate = { onUpdateSet(index, it) },
                        onRemove = { onRemoveSet(index) }
                    )
                    // Divider between sets (but not after the last one).
                    if (index < draft.sets.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    }
                }

                // "Add Set" button appends a new set row.
                TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Set")
                }

                // Optional notes field for this exercise.
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { onDraftChange(draft.copy(notes = it)) },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isSaving) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

// ── SetEditorRow ─────────────────────────────────────────────────────────────
// One row of the editor dialog — represents a single set.
// Shows: set number label, "to failure" toggle, reps + weight fields.

@Composable
private fun SetEditorRow(
    index: Int,         // zero-based position in the list
    set: SetDraft,
    useLbs: Boolean,
    canRemove: Boolean, // false when this is the only remaining set
    onUpdate: (SetDraft) -> Unit,
    onRemove: () -> Unit
) {
    Column {
        // ── Set label + optional remove button ─────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Set ${index + 1}",  // display as 1-based ("Set 1", "Set 2", …)
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                modifier = Modifier.weight(1f)
            )
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove set",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }

        // ── "To Failure" toggle ─────────────────────────────────────────────
        // When enabled, the reps field is hidden (irrelevant for failure sets).
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("To Failure", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f))
            Switch(
                checked = set.toFailure,
                onCheckedChange = { onUpdate(set.copy(toFailure = it)) }
            )
        }

        // ── Reps + Weight fields ────────────────────────────────────────────
        Spacer(Modifier.height(4.dp))
        if (!set.toFailure) {
            // Normal set: show both reps and weight side-by-side.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = set.reps,
                    onValueChange = { onUpdate(set.copy(reps = it)) },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    // Convert stored lbs to display unit on read,
                    // then convert back to lbs when the user types.
                    value = lbsToDisplay(set.weightLbs, useLbs),
                    onValueChange = { onUpdate(set.copy(weightLbs = displayToLbs(it, useLbs))) },
                    label = { Text(unitLabel(useLbs)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        } else {
            // To-failure set: only show weight (full width, no reps).
            OutlinedTextField(
                value = lbsToDisplay(set.weightLbs, useLbs),
                onValueChange = { onUpdate(set.copy(weightLbs = displayToLbs(it, useLbs))) },
                label = { Text("Weight at failure (${unitLabel(useLbs)})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

// ── SupersetPickerDialog ─────────────────────────────────────────────────────
// Lists all exercises in the current workout that are not yet in a superset,
// so the user can pick one to link with the source exercise.

@Composable
private fun SupersetPickerDialog(
    sourceEntryId: String,              // the entry we are linking FROM
    allEntries: List<EntryWithSets>,    // every entry in the current workout
    onLink: (String) -> Unit,           // called with the target entry ID
    onDismiss: () -> Unit
) {
    // Candidates = entries that are not the source AND not already in a superset.
    val candidates = allEntries.filter {
        it.entry.id != sourceEntryId && it.entry.supersetGroupId == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link as Superset") },
        text = {
            if (candidates.isEmpty()) {
                Text("No other exercises available to link. All may already be in a superset.")
            } else {
                Column {
                    candidates.forEach { ews ->
                        ListItem(
                            headlineContent = { Text(ews.exerciseName) },
                            modifier = Modifier.clickable { onLink(ews.entry.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},  // no confirm button — selection closes the dialog
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── ExercisePickerSheet ──────────────────────────────────────────────────────
// The bottom sheet that appears when the user taps "+" to add an exercise.
// Shows a search bar and a scrollable list of all available exercises.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<ExerciseEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelect: (ExerciseEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Pick an Exercise", style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // Search field — filters by exercise name or muscle group.
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search exercises or muscle group...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        // Scrollable exercise list.
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(exercises, key = { it.id }) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    // Muscle group shown in the primary color below the name.
                    supportingContent = { Text(exercise.muscleGroup, color = MaterialTheme.colorScheme.primary) },
                    leadingContent = { Icon(Icons.Filled.FitnessCenter, null, tint = Accent) },
                    modifier = Modifier.clickable { onSelect(exercise) }
                )
                HorizontalDivider()
            }
            // Show a message when the search returns no results.
            if (exercises.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No exercises found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0F0F11)
@Composable
internal fun WorkoutScreenPreview_WithExercises() {
    val sampleEntry1 = WorkoutEntryEntity(
        id = "e1", workoutId = "w1", exerciseId = "ex1",
        sets = 3, reps = 10, weightKg = 135f, useLbs = true
    )
    val sampleEntry2 = WorkoutEntryEntity(
        id = "e2", workoutId = "w1", exerciseId = "ex2",
        sets = 4, reps = 8, weightKg = 185f, useLbs = true
    )
    TrackAppTheme {
        WorkoutScreenContent(
            uiState = WorkoutUiState(
                workoutName = "Push Day",
                entriesWithSets = listOf(
                    EntryWithSets(
                        entry = sampleEntry1,
                        sets = listOf(
                            SetEntity(id = "s1", entryId = "e1", setNumber = 1, reps = 10, weightLbs = 135f),
                            SetEntity(id = "s2", entryId = "e1", setNumber = 2, reps = 8, weightLbs = 135f),
                            SetEntity(id = "s3", entryId = "e1", setNumber = 3, reps = 6, weightLbs = 145f, toFailure = true)
                        ),
                        exerciseName = "Bench Press"
                    ),
                    EntryWithSets(
                        entry = sampleEntry2,
                        sets = listOf(
                            SetEntity(id = "s4", entryId = "e2", setNumber = 1, reps = 12, weightLbs = 30f),
                            SetEntity(id = "s5", entryId = "e2", setNumber = 2, reps = 10, weightLbs = 30f)
                        ),
                        exerciseName = "Lateral Raise"
                    )
                )
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F11)
@Composable
internal fun WorkoutScreenPreview_Empty() {
    TrackAppTheme {
        WorkoutScreenContent(
            uiState = WorkoutUiState(workoutName = "New Workout")
        )
    }
}
