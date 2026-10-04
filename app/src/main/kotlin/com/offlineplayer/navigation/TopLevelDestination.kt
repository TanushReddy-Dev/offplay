package com.offlineplayer.navigation

/**
 * Top-level navigation destinations for the bottom navigation bar.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
) {
    HOME(route = "home", label = "Home"),
    SEARCH(route = "search", label = "Search"),
    LIBRARY(route = "library", label = "Library"),
    SETTINGS(route = "settings", label = "Settings"),
}
