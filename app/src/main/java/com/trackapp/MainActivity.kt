// MainActivity is the entry point for the entire app.
// Android creates this Activity when the user taps the app icon.
//
// An "Activity" is Android's concept of a single screen window.
// Modern Android apps typically have just ONE Activity (this one) that hosts
// the entire navigation graph — different "screens" are Composable functions
// swapped in and out inside this single Activity window.

package com.trackapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// enableEdgeToEdge makes the app draw behind the status bar and navigation bar
// for a more immersive, modern look.
import androidx.activity.enableEdgeToEdge
import com.trackapp.ui.navigation.AppNavigation
import com.trackapp.ui.theme.TrackAppTheme

class MainActivity : ComponentActivity() {

    // onCreate is called once when the Activity is created (when the app opens).
    // savedInstanceState is a Bundle that can restore state after a process kill,
    // but we rely on Room and StateFlow for data persistence instead.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Makes the app render edge-to-edge (behind system bars).
        enableEdgeToEdge()

        // Get the custom Application instance to access the shared repositories.
        // "as TrackApplication" is a type cast — we know it's a TrackApplication
        // because that's what's declared in AndroidManifest.xml.
        val app = application as TrackApplication

        // setContent replaces the traditional XML layout with a Compose UI tree.
        // Everything inside this block is our app's UI.
        setContent {
            // Wrap everything in our custom dark theme so colors and typography
            // are applied globally to all screens.
            TrackAppTheme {
                // AppNavigation is the root composable — it owns the nav controller
                // and renders whichever screen the user is currently on.
                AppNavigation(
                    authRepository = app.authRepository,
                    exerciseRepository = app.exerciseRepository,
                    workoutRepository = app.workoutRepository,
                    preferencesRepository = app.preferencesRepository
                )
            }
        }
    }
}
