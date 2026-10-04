package com.offlineplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.offlineplayer.feature.home.HomeScreen
import com.offlineplayer.navigation.TopLevelDestination

private data class NavItem(
    val destination: TopLevelDestination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val navItems = listOf(
    NavItem(TopLevelDestination.HOME, Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(TopLevelDestination.SEARCH, Icons.Filled.Search, Icons.Outlined.Search),
    NavItem(TopLevelDestination.LIBRARY, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    NavItem(TopLevelDestination.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings),
)

/**
 * Root composable for the app. Hosts the bottom navigation bar and NavHost.
 */
@Composable
fun OfflinePlayerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isPlayerRoute = currentDestination?.route == "player"
    val isQueueRoute = currentDestination?.route == "queue"
    val hideBottomBar = isPlayerRoute || isQueueRoute

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                Column {
                    com.offlineplayer.feature.player.MiniPlayer(
                        onClick = {
                            navController.navigate("player") {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.destination.route
                            } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.destination.label,
                                    )
                                },
                                label = { Text(item.destination.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen()
            }
            composable(TopLevelDestination.SEARCH.route) {
                PlaceholderScreen("Search")
            }
            composable(TopLevelDestination.LIBRARY.route) {
                com.offlineplayer.feature.library.LibraryScreen()
            }
            composable(TopLevelDestination.SETTINGS.route) {
                PlaceholderScreen("Settings")
            }
            composable("player") {
                com.offlineplayer.feature.player.NowPlayingScreen(
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToQueue = { navController.navigate("queue") }
                )
            }
            composable("queue") {
                com.offlineplayer.feature.player.QueueScreen(
                    onNavigateUp = { navController.navigateUp() }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
