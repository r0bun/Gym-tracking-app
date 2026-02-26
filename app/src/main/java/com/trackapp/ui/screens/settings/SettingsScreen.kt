// Settings screen — lets the user customize the app's primary/accent color.
//
// The screen has two ways to pick a color:
//   1. PRESETS: A grid of colored circles (quick picks). Tap one to apply.
//   2. CUSTOM HEX: A text field where you type any 6-digit hex color code.
//
// When a color is selected (either way), the app recolors INSTANTLY because
// the hex value flows through PreferencesRepository → MainActivity →
// TrackAppTheme → MaterialTheme, triggering a full recomposition.

package com.trackapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trackapp.ui.theme.accentPresets
import com.trackapp.ui.theme.hexToColor
import com.trackapp.ui.theme.isValidHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit   // called when the user presses the back arrow
) {
    // Observe the current accent color hex from the ViewModel.
    // This updates live — if the color changes, the UI recomposes.
    val currentHex by viewModel.accentColorHex.collectAsState()

    // Local state for the hex text input field.
    // Initialized to the saved color so the field shows the current value.
    var hexInput by remember(currentHex) { mutableStateOf(currentHex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // ── Section header ──────────────────────────────────────────
            Text(
                text = "Theme Color",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose your app's primary color or enter a custom hex code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Preset color grid ───────────────────────────────────────
            // A 4-column grid of colored circles. Tapping one immediately
            // saves it and recolors the app. The selected circle gets a
            // white border and a checkmark icon.
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(200.dp)  // enough for 2 rows of 4
            ) {
                items(accentPresets) { preset ->
                    // Check if this preset matches the currently saved color.
                    val isSelected = currentHex.equals(preset.hex, ignoreCase = true)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // The colored circle — tap to select.
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(hexToColor(preset.hex))
                                // White border on the selected circle for visibility.
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        Color.White,
                                        CircleShape
                                    )
                                    else Modifier
                                )
                                .clickable {
                                    // Apply this preset: save to preferences and
                                    // update the text field to match.
                                    viewModel.setAccentColor(preset.hex)
                                    hexInput = preset.hex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Show a checkmark on the selected circle.
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Preset name below the circle (e.g. "Violet").
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Custom hex input ────────────────────────────────────────
            // A text field prefixed with "#" where the user can type any
            // 6-digit hex color code. The "Apply" button saves it.
            Text(
                text = "Custom Color",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter a 6-digit hex code (e.g. FF6B6B)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live color preview circle — shows what the typed hex looks like.
                // Only renders the color if the input is a valid hex code.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isValidHex(hexInput)) hexToColor(hexInput)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )

                // The hex text input field.
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { newValue ->
                        // Only allow hex characters (0-9, a-f, A-F) and max 6 chars.
                        // This filters out pasting "#" or other invalid characters.
                        val filtered = newValue.filter {
                            it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
                        }.take(6)
                        hexInput = filtered
                    },
                    // The "#" prefix is purely visual — we don't store it.
                    prefix = { Text("#") },
                    placeholder = { Text("4F46E5") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                // Apply button — saves the custom hex color.
                // Disabled when the input isn't a valid 6-char hex code.
                Button(
                    onClick = { viewModel.setAccentColor(hexInput) },
                    enabled = isValidHex(hexInput),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply")
                }
            }

            // Validation hint — shown when the input is partially typed.
            if (hexInput.isNotEmpty() && !isValidHex(hexInput)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter exactly 6 hex characters (0-9, A-F)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
