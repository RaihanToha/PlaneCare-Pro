package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.PlantDatabase
import com.example.data.PlantRepository
import com.example.i18n.I18nManager
import com.example.ui.PlantViewModel
import com.example.ui.ViewModelFactory
import com.example.ui.screens.AllPlantsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginRegisterScreen
import com.example.ui.screens.MockShell
import com.example.ui.screens.PlantDetailScreen
import com.example.ui.screens.PlantEditScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize dual-language I18n manager
        I18nManager.init(this)
        
        // Initialize User settings (nickname and dark mode)
        com.example.data.UserSettings.init(this)
        
        // Initialize SQLite Room database structures
        val database = PlantDatabase.getDatabase(this)
        val repository = PlantRepository(database.plantDao())
        val factory = ViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[PlantViewModel::class.java]

        enableEdgeToEdge()
        
        setContent {
            val isDarkState = com.example.data.UserSettings.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkState.value) {
                // Constrained Mobile shell simulator
                MockShell {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard"
                    ) {
                        // 1. DASHBOARD VIEW (maps to /)
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onAddPlant = {
                                    navController.navigate("plant_edit/-1")
                                },
                                onViewPlant = { id ->
                                    navController.navigate("plant_detail/$id")
                                },
                                onNavigateToAllPlants = { query, category ->
                                    val q = query ?: ""
                                    val c = category ?: "All"
                                    navController.navigate("all_plants?query=$q&category=$c")
                                },
                                onNavigateToAuth = {
                                    navController.navigate("auth")
                                }
                            )
                        }

                        // 1b. AUTHENTICATION VIEW
                        composable("auth") {
                            LoginRegisterScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onAuthSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 2. ALL PLANTS LIST VIEW (maps to /plants) with search/category arguments
                        composable(
                            route = "all_plants?query={query}&category={category}",
                            arguments = listOf(
                                navArgument("query") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                navArgument("category") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val query = backStackEntry.arguments?.getString("query")
                            val category = backStackEntry.arguments?.getString("category")
                            
                            AllPlantsScreen(
                                viewModel = viewModel,
                                initialSearchQuery = query,
                                initialCategory = category,
                                onBack = { navController.popBackStack() },
                                onViewPlant = { id ->
                                    navController.navigate("plant_detail/$id")
                                },
                                onNavigateToLogin = { navController.navigate("auth") }
                            )
                        }

                        // 3. PLANT DETAIL VIEW (maps to /plant/:id)
                        composable(
                            route = "plant_detail/{plantId}",
                            arguments = listOf(
                                navArgument("plantId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val plantId = backStackEntry.arguments?.getInt("plantId") ?: 0
                            
                            PlantDetailScreen(
                                plantId = plantId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onEditPlant = { id ->
                                    navController.navigate("plant_edit/$id")
                                }
                            )
                        }

                        // 4. PLANT ADD/EDIT VIEW (maps to /add & /edit/:id)
                        composable(
                            route = "plant_edit/{plantId}",
                            arguments = listOf(
                                navArgument("plantId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val plantId = backStackEntry.arguments?.getInt("plantId") ?: -1
                            
                            PlantEditScreen(
                                plantId = plantId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onComplete = { savedId ->
                                    if (savedId == -1) {
                                        navController.popBackStack("dashboard", false)
                                    } else {
                                        navController.popBackStack()
                                        navController.navigate("plant_detail/$savedId") {
                                            popUpTo("dashboard") { saveState = false }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
