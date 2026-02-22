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
import androidx.compose.ui.unit.dp
import com.trackapp.data.local.entity.ExerciseEntity
import com.trackapp.ui.theme.Accent

// â”€â”€ Unit conversion helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun lbsToDisplay(lbs: String, useLbs: Boolean): String {
    if (useLbs) return lbs
    val f = lbs.toFloatOrNull() ?: return lbs
    return String.format("%.1f", f / 2.20462f)
}

private fun displayToLbs(display: String, useLbs: Boolean): String {
    if (useLbs) return display
    val f = display.toFloatOrNull() ?: return display
    return String.format("%.2f", f * 2.20462f)
}

private fun unitLabel(useLbs: Boolean) = if (useLbs) "lbs" else "kg"

// â”€â”€ Screen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    workoutId: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(workoutId) { viewModel.loadWorkout(workoutId) }

    // Exercise picker bottom sheet
    if (uiState.showExercisePicker) {
        ModalBottomSheet(onDismissRequest = viewModel::closeExercisePicker) {
            ExercisePickerSheet(
                exercises = uiState.exercises,
                searchQuery = uiState.exerciseSearch,
                onSearchChange = viewModel::setExerciseSearch,
                onSelect = viewModel::selectExercise
            )
        }
    }

    // Entry editor dialog
    uiState.editingDraft?.let { draft ->
        EntryEditorDialog(
            draft = draft,
            isEditing = uiState.editingEntryId != null,
            isSaving = uiState.isSaving,
            onDraftChange = viewModel::updateDraft,
            onAddSet = viewModel::addSetToDraft,
            onRemoveSet = viewModel::removeSetFromDraft,
            onUpdateSet = viewModel::updateSetInDraft,
            onSave = viewModel::saveDraft,
            onCancel = viewModel::cancelDraft
        )
    }

    // Superset picker dialog
    uiState.showSupersetPickerForEntryId?.let { sourceId ->
        SupersetPickerDialog(
            sourceEntryId = sourceId,
            allEntries = uiState.entriesWithSets,
            onLink = { targetId -> viewModel.linkSuperset(sourceId, targetId) },
            onDismiss = viewModel::dismissSupersetPicker
        )
    }

    uiState.error?.let { LaunchedEffect(it) { viewModel.clearError() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.workoutName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openExercisePicker,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Exercise")
            }
        }
    ) { padding ->
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.entriesWithSets, key = { it.entry.id }) { ews ->
                    EntryCard(
                        entryWithSets = ews,
                        onEdit = { viewModel.editEntry(ews) },
                        onDelete = { viewModel.deleteEntry(ews.entry) },
                        onRequestSuperset = { viewModel.requestLinkSuperset(ews.entry.id) },
                        onUnlinkSuperset = { viewModel.unlinkSuperset(ews.entry) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// â”€â”€ EntryCard â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun EntryCard(
    entryWithSets: EntryWithSets,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRequestSuperset: () -> Unit,
    onUnlinkSuperset: () -> Unit
) {
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

    val isSuperset = entryWithSets.entry.supersetGroupId != null

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entryWithSets.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                // Edit button
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Accent)
                }
                // Delete button
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
            }

            // Per-set list
            Spacer(Modifier.height(8.dp))
            if (entryWithSets.sets.isEmpty()) {
                Text("No sets recorded", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    entryWithSets.sets.forEach { set ->
                        val unit = unitLabel(entryWithSets.entry.useLbs)
                        val weightDisplay = lbsToDisplay(set.weightLbs.toString(), entryWithSets.entry.useLbs)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Set ${set.setNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(44.dp)
                            )
                            if (set.toFailure) {
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

            // Notes
            if (entryWithSets.entry.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(entryWithSets.entry.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            // Superset actions
            Spacer(Modifier.height(8.dp))
            Row {
                if (isSuperset) {
                    TextButton(onClick = onUnlinkSuperset, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Unlink Superset", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
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

// â”€â”€ EntryEditorDialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun EntryEditorDialog(
    draft: EntryDraft,
    isEditing: Boolean,
    isSaving: Boolean,
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
                // Per-exercise unit toggle
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Set rows
                draft.sets.forEachIndexed { index, set ->
                    SetEditorRow(
                        index = index,
                        set = set,
                        useLbs = draft.useLbs,
                        canRemove = draft.sets.size > 1,
                        onUpdate = { onUpdateSet(index, it) },
                        onRemove = { onRemoveSet(index) }
                    )
                    if (index < draft.sets.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    }
                }

                // Add Set button
                TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Set")
                }

                // Notes
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

@Composable
private fun SetEditorRow(
    index: Int,
    set: SetDraft,
    useLbs: Boolean,
    canRemove: Boolean,
    onUpdate: (SetDraft) -> Unit,
    onRemove: () -> Unit
) {
    Column {
        // Set label + remove button
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Set ${index + 1}",
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

        // To Failure toggle
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("To Failure", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f))
            Switch(
                checked = set.toFailure,
                onCheckedChange = { onUpdate(set.copy(toFailure = it)) }
            )
        }

        // Reps field (hidden when toFailure) + Weight field (always shown)
        Spacer(Modifier.height(4.dp))
        if (!set.toFailure) {
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
                    value = lbsToDisplay(set.weightLbs, useLbs),
                    onValueChange = { onUpdate(set.copy(weightLbs = displayToLbs(it, useLbs))) },
                    label = { Text(unitLabel(useLbs)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        } else {
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

// â”€â”€ SupersetPickerDialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SupersetPickerDialog(
    sourceEntryId: String,
    allEntries: List<EntryWithSets>,
    onLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// â”€â”€ ExercisePickerSheet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search exercises or muscle group...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(exercises, key = { it.id }) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    supportingContent = { Text(exercise.muscleGroup, color = MaterialTheme.colorScheme.primary) },
                    leadingContent = { Icon(Icons.Filled.FitnessCenter, null, tint = Accent) },
                    modifier = Modifier.clickable { onSelect(exercise) }
                )
                HorizontalDivider()
            }
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

