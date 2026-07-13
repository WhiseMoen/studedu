package com.sapraliev.studedu.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sapraliev.studedu.R
import com.sapraliev.studedu.ui.settings.SettingsScreen
import com.sapraliev.studedu.ui.tasks.TasksScreen
import com.sapraliev.studedu.ui.students.StudentsScreen
import com.sapraliev.studedu.ui.today.TodayScreen

/** Четыре вкладки нижней навигации. */
enum class AppTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    TODAY("today", R.string.tab_today, Icons.Filled.Today),
    TASKS("tasks", R.string.tab_notes, Icons.AutoMirrored.Filled.EventNote),
    STUDENTS("students", R.string.tab_students, Icons.Filled.People),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings),
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppTab.TODAY.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppTab.TODAY.route) { TodayScreen() }
            composable(AppTab.TASKS.route) { TasksScreen() }
            composable(AppTab.STUDENTS.route) { StudentsScreen() }
            composable(AppTab.SETTINGS.route) { SettingsScreen() }
        }
    }
}
