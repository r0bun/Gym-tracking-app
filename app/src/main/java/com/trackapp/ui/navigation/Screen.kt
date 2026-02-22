package com.trackapp.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Workout : Screen("workout/{workoutId}") {
        fun createRoute(workoutId: String) = "workout/$workoutId"
    }
    data object History : Screen("history")
}
