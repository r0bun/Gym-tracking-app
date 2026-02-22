package com.trackapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.trackapp.ui.navigation.AppNavigation
import com.trackapp.ui.theme.TrackAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TrackApplication

        setContent {
            TrackAppTheme {
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
