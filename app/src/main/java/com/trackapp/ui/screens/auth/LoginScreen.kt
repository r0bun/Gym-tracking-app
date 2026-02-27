// This file defines the visual layout of the Login / Sign-Up screen.
// The screen has two modes (toggled by the user):
//   - Sign In: email + password + "Keep me signed in" checkbox
//   - Sign Up: email + password (no keep-signed-in option)

package com.trackapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trackapp.ui.theme.Accent
import com.trackapp.ui.theme.TrackAppTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onAuthSuccess: () -> Unit  // called when login/signup succeeds (navigation callback)
) {
    val uiState by viewModel.uiState.collectAsState()

    // LaunchedEffect runs when isSuccess becomes true (after successful login).
    // It calls onAuthSuccess(), which AppNavigation ignores (navigation is driven
    // by the isSignedIn flow in AuthRepository instead).
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onAuthSuccess()
    }

    LoginScreenContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onKeepSignedInChange = viewModel::onKeepSignedInChange,
        onToggleMode = viewModel::toggleMode
    )
}

@Composable
fun LoginScreenContent(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onKeepSignedInChange: (Boolean) -> Unit = {},
    onToggleMode: () -> Unit = {}
) {
    // LocalFocusManager lets us move the keyboard focus between fields
    // (e.g. pressing "Next" on the email field jumps to the password field).
    val focusManager = LocalFocusManager.current

    // remember {} keeps this value alive across recompositions.
    // mutableStateOf creates a Compose state variable — changing it triggers a redraw.
    var passwordVisible by remember { mutableStateOf(false) }

    // Box centers all its children horizontally and vertically.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Column stacks children vertically with 16.dp gaps between them.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // App logo icon (dumbbell).
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,   // null = decorative, ignored by accessibility
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            // App name heading.
            Text(
                text = "TrackApp",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Sub-heading changes based on mode.
            Text(
                text = if (uiState.isSignUp) "Create your account" else "Welcome back",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Email field ──────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,  // update ViewModel on every keystroke
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,   // shows @ on keyboard
                    imeAction = ImeAction.Next           // shows "Next" button on keyboard
                ),
                keyboardActions = KeyboardActions(
                    // Pressing "Next" moves focus to the password field below.
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // ── Password field ───────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                // PasswordVisualTransformation shows dots instead of characters.
                // VisualTransformation.None shows the actual characters.
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done  // shows "Done" / checkmark on keyboard
                ),
                keyboardActions = KeyboardActions(
                    // Pressing "Done" hides the keyboard and submits the form.
                    onDone = {
                        focusManager.clearFocus()
                        onSubmit()
                    }
                ),
                // Eye icon in the right side of the field — toggles password visibility.
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                          else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )

            // ── Error banner ─────────────────────────────────────────────────
            // Only rendered when there is an error message to show.
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error!!,  // !! asserts non-null (safe here because of the if above)
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ── "Keep me signed in" row (Sign In mode only) ──────────────────
            if (!uiState.isSignUp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = uiState.keepSignedIn,
                        onCheckedChange = onKeepSignedInChange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Keep me signed in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Submit button ────────────────────────────────────────────────
            // Disabled while the network request is in progress to prevent
            // the user from tapping it twice.
            Button(
                onClick = onSubmit,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    // Show a small spinner inside the button while loading.
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (uiState.isSignUp) "Create Account" else "Sign In",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // ── Toggle Sign In / Sign Up ─────────────────────────────────────
            // A text link at the bottom that switches between the two modes.
            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (uiState.isSignUp) "Already have an account? Sign In"
                           else "Don't have an account? Sign Up",
                    color = Accent
                )
            }
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0F0F11)
@Composable
internal fun LoginScreenPreview_SignIn() {
    TrackAppTheme {
        LoginScreenContent(uiState = AuthUiState())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F11)
@Composable
internal fun LoginScreenPreview_SignUp() {
    TrackAppTheme {
        LoginScreenContent(uiState = AuthUiState(isSignUp = true))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F11)
@Composable
internal fun LoginScreenPreview_Loading() {
    TrackAppTheme {
        LoginScreenContent(
            uiState = AuthUiState(
                email = "user@example.com",
                password = "password",
                isLoading = true
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F11)
@Composable
internal fun LoginScreenPreview_Error() {
    TrackAppTheme {
        LoginScreenContent(
            uiState = AuthUiState(
                email = "user@example.com",
                password = "wrong",
                error = "Invalid login credentials"
            )
        )
    }
}
