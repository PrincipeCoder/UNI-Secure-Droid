package com.example.unisecuredroid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unisecuredroid.ui.screens.HomeScreen
import com.example.unisecuredroid.ui.screens.LoginScreen
import com.example.unisecuredroid.ui.screens.ReporteScreen

@Composable
fun AppController() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    isLoggedIn = true
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(navController = navController)
        }

        composable(
            route = "reporte/{jobId}",
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            ReporteScreen(
                navController = navController,
                jobId = jobId ?: "error_no_job_id"
            )
        }
    }
}