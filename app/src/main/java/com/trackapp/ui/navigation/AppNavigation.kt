// This file is the "router" for the entire app — it defines which composable
// (screen) is shown for each route, and handles automatic navigation based on
// whether the user is signed in or not.
//
// Think of it like a traffic controller: it decides where to send the user
// based on their current auth state (null/true/false).

package com.trackapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trackapp.ui.theme.Accent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trackapp.data.repository.AuthRepository
import com.trackapp.data.repository.ExerciseRepository
import com.trackapp.data.repository.PreferencesRepository
import com.trackapp.data.repository.WorkoutRepository
import com.trackapp.ui.screens.auth.LoginScreen
import com.trackapp.ui.screens.auth.LoginViewModel
import com.trackapp.ui.screens.history.HistoryScreen
import com.trackapp.ui.screens.history.HistoryViewModel
import com.trackapp.ui.screens.home.HomeScreen
import com.trackapp.ui.screens.home.HomeViewModel
import com.trackapp.ui.screens.settings.SettingsScreen
import com.trackapp.ui.screens.settings.SettingsViewModel
import com.trackapp.ui.screens.workout.WorkoutScreen
import com.trackapp.ui.screens.workout.WorkoutViewModel

// This is the top-level composable called from MainActivity.
// The repositories are passed in so every screen can access data.
@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
    preferencesRepository: PreferencesRepository
) {
    // navController manages the navigation back-stack (the history of screens
    // the user has visited, like browser history). Pressing Back pops the stack.
    val navController = rememberNavController()

    // Observe the sign-in state as a Compose State — every time isSignedIn
    // changes, the LaunchedEffect below re-runs.
    val isSignedIn by authRepository.isSignedInFlow.collectAsState()

    // LaunchedEffect runs a coroutine whenever its key (isSignedIn) changes.
    // This is where automatic navigation happens:
    //   null  → do nothing (still loading, show the spinner on "loading" route)
    //   true  → go to Home, clear the back-stack (can't go back to login)
    //   false → go to Login, clear the back-stack (can't go back to home)
    LaunchedEffect(isSignedIn) {
        val currentRoute = navController.currentDestination?.route
        when (isSignedIn) {
            true -> if (currentRoute != Screen.Home.route) {
                navController.navigate(Screen.Home.route) {
                    // popUpTo(0) clears every previous destination from the stack.
                    popUpTo(0) { inclusive = true }
                }
            }
            false -> if (currentRoute != Screen.Login.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            null -> { /* Still loading — stay on the loading screen */ }
        }
    }

    // NavHost is the container that swaps screens in and out.
    // startDestination = the first screen shown when the app opens.
    // We start on "loading" (a spinner) so neither Login nor Home flashes
    // briefly before the stored session is resolved.
    NavHost(navController = navController, startDestination = "loading") {

        // ── Loading screen ───────────────────────────────────────────────────
        // Shows a spinning indicator while the app checks if the user is
        // already logged in from a previous session.
        composable("loading") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        }

        // ── Login screen ─────────────────────────────────────────────────────
        // viewModel(factory = ...) creates the ViewModel with its dependencies.
        // The Factory pattern is needed because ViewModels can't take arbitrary
        // constructor parameters — we must tell Compose how to build them.
        composable(Screen.Login.route) {
            val vm: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(authRepository, exerciseRepository, preferencesRepository)
            )
            LoginScreen(
                viewModel = vm,
                // Navigation to Home is driven by the isSignedIn LaunchedEffect
                // above, so this callback doesn't need to do anything.
                onAuthSuccess = { }
            )
        }

        // ── Home screen ──────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(workoutRepository, authRepository, exerciseRepository)
            )
            HomeScreen(
                viewModel = vm,
                // Navigate to the Workout screen when the user taps "Start Workout"
                // or taps a recent workout card.
                onStartWorkout = { workoutId ->
                    navController.navigate(Screen.Workout.createRoute(workoutId))
                },
                onOpenWorkout = { workoutId ->
                    navController.navigate(Screen.Workout.createRoute(workoutId))
                },
                onSignOut = {
                    // Go back to Login and clear the entire back-stack.
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenHistory = {
                    navController.navigate(Screen.History.route)
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // ── Workout screen ───────────────────────────────────────────────────
        composable(
            route = Screen.Workout.route,
            // Declare that this route accepts a "workoutId" path argument
            // of type String (e.g. "workout/abc-123" → workoutId = "abc-123").
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Extract the workoutId from the URL path.
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
            val vm: WorkoutViewModel = viewModel(
                factory = WorkoutViewModel.Factory(workoutRepository, exerciseRepository, preferencesRepository)
            )
            WorkoutScreen(
                viewModel = vm,
                workoutId = workoutId,
                // popBackStack() goes back to the previous screen (Home or History).
                onBack = { navController.popBackStack() }
            )
        }

        // ── History screen ───────────────────────────────────────────────────
        composable(Screen.History.route) {
            val vm: HistoryViewModel = viewModel(
                factory = HistoryViewModel.Factory(workoutRepository)
            )
            HistoryScreen(
                viewModel = vm,
                onOpenWorkout = { workoutId ->
                    navController.navigate(Screen.Workout.createRoute(workoutId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Settings screen ────────────────────────────────────────────────
        // Lets the user change the app's accent color (presets or custom hex).
        composable(Screen.Settings.route) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(preferencesRepository)
            )
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
