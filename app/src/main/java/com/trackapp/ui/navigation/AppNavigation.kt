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
import com.trackapp.ui.screens.workout.WorkoutScreen
import com.trackapp.ui.screens.workout.WorkoutViewModel

@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
    preferencesRepository: PreferencesRepository
) {
    val navController = rememberNavController()

    // Observe session status:
    //   null  → still loading from storage (show spinner)
    //   true  → authenticated            (go to Home)
    //   false → not authenticated        (go to Login)
    val isSignedIn by authRepository.isSignedInFlow.collectAsState()

    LaunchedEffect(isSignedIn) {
        val currentRoute = navController.currentDestination?.route
        when (isSignedIn) {
            true -> if (currentRoute != Screen.Home.route) {
                navController.navigate(Screen.Home.route) {
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

    // Start on a neutral loading screen so neither Login nor Home flashes
    // before the persisted session is resolved.
    NavHost(navController = navController, startDestination = "loading") {

        // ── Loading ─────────────────────────────────────────────────────────
        composable("loading") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        }

        // â”€â”€ Login â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(Screen.Login.route) {
            val vm: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(authRepository, exerciseRepository, preferencesRepository)
            )
            LoginScreen(
                viewModel = vm,
                // Navigation to Home is driven by the isSignedIn LaunchedEffect above,
                // so this callback is intentionally a no-op.
                onAuthSuccess = { }
            )
        }

        // â”€â”€ Home â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(Screen.Home.route) {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(workoutRepository, authRepository)
            )
            HomeScreen(
                viewModel = vm,
                onStartWorkout = { workoutId ->
                    navController.navigate(Screen.Workout.createRoute(workoutId))
                },
                onOpenWorkout = { workoutId ->
                    navController.navigate(Screen.Workout.createRoute(workoutId))
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        // â”€â”€ Workout â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(
            route = Screen.Workout.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
            val vm: WorkoutViewModel = viewModel(
                factory = WorkoutViewModel.Factory(workoutRepository, exerciseRepository, preferencesRepository)
            )
            WorkoutScreen(
                viewModel = vm,
                workoutId = workoutId,
                onBack = { navController.popBackStack() }
            )
        }

        // â”€â”€ History â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
    }
}
