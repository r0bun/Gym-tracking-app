// Navigation in Jetpack Compose works like a web router:
// each "screen" has a unique string "route" (like a URL path).
// To navigate to a screen, you call navController.navigate("the_route").
//
// This file defines all the screens in the app and their route strings,
// keeping them in one place so we never mistype a route string.

package com.trackapp.ui.navigation

// "sealed class" means all subclasses must be defined in this file.
// It's like an enum but allows each variant to carry extra data/functions.
// Every Screen has a "route" string used by the navigation system.
sealed class Screen(val route: String) {

    // The login/sign-up screen. Route: "login"
    data object Login : Screen("login")

    // The home screen listing recent workouts. Route: "home"
    data object Home : Screen("home")

    // The active workout screen. Route: "workout/{workoutId}"
    // {workoutId} is a placeholder — it gets replaced with the actual ID
    // when navigating, e.g. "workout/abc-123-def".
    data object Workout : Screen("workout/{workoutId}") {
        // Helper to build the actual route string with a real ID.
        // Usage: navController.navigate(Screen.Workout.createRoute("abc-123"))
        fun createRoute(workoutId: String) = "workout/$workoutId"
    }

    // The workout history screen. Route: "history"
    data object History : Screen("history")

    // The settings screen for changing app preferences (theme color, etc.).
    // Route: "settings"
    data object Settings : Screen("settings")
}
