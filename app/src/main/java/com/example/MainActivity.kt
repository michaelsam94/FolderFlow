package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.core.presentation.ViewModelFactory
import com.example.feature.folders.presentation.FolderDetailScreen
import com.example.feature.folders.presentation.FolderListScreen
import com.example.feature.folders.presentation.FolderViewModel
import com.example.feature.organizer.presentation.AppOrganizerScreen
import com.example.feature.organizer.presentation.AppOrganizerViewModel
import com.example.feature.tags.presentation.TagsScreen
import com.example.feature.tags.presentation.TagsViewModel
import com.example.feature.unused.presentation.UnusedAppsScreen
import com.example.feature.unused.presentation.UnusedAppsViewModel
import com.example.ui.theme.FolderFlowTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FolderFlowApplication
        val factory = ViewModelFactory(app)

        setContent {
            FolderFlowTheme {
                MainLayout(factory)
            }
        }
    }
}

@Composable
fun MainLayout(factory: ViewModelFactory) {
    val navController = rememberNavController()

    // Retrieve ViewModels with our custom factory provider
    val folderViewModel: FolderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
    val appOrganizerViewModel: AppOrganizerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
    val tagsViewModel: TagsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
    val unusedAppsViewModel: UnusedAppsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

    // Current backstack entry to determine selected tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Only show bottom navigation when on primary top-level routes
            val primaryRoutes = listOf("folders", "organizer", "tags", "unused")
            if (currentRoute in primaryRoutes) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "folders",
                        onClick = {
                            navController.navigate("folders") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Folders") },
                        label = { Text("Folders") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "organizer",
                        onClick = {
                            navController.navigate("organizer") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Organizer") },
                        label = { Text("App Index") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "tags",
                        onClick = {
                            navController.navigate("tags") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = "Tags") },
                        label = { Text("Tags") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "unused",
                        onClick = {
                            navController.navigate("unused") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Warning, contentDescription = "Idle Scanner") },
                        label = { Text("Idle") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "folders",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("folders") {
                FolderListScreen(
                    viewModel = folderViewModel,
                    onNavigateToDetail = { folderId ->
                        navController.navigate("folder_detail/$folderId")
                    }
                )
            }
            
            composable(
                route = "folder_detail/{folderId}",
                arguments = listOf(navArgument("folderId") { type = NavType.StringType })
            ) {
                FolderDetailScreen(
                    viewModel = folderViewModel,
                    onBack = {
                        folderViewModel.selectFolder(null)
                        navController.popBackStack()
                    }
                )
            }

            composable("organizer") {
                AppOrganizerScreen(viewModel = appOrganizerViewModel)
            }

            composable("tags") {
                TagsScreen(viewModel = tagsViewModel)
            }

            composable("unused") {
                UnusedAppsScreen(viewModel = unusedAppsViewModel)
            }
        }
    }
}
