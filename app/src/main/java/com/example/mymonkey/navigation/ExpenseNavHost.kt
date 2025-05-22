// navigation/ExpenseNavHost.kt
package com.example.mymonkey.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mymonkey.AddExpenseScreen
import com.example.mymonkey.screens.HomeScreenWithHandlers
import com.example.mymonkey.LoginScreen

@Composable
fun ExpenseNavHost(navController: androidx.navigation.NavHostController) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = { navController.navigate("home") })
        }
        composable("home") {
            HomeScreenWithHandlers(navController)
        }
        composable("add") {
            AddExpenseScreen(
                initialDesc = "",
                initialAmount = 0.0,
                initialLocation = "",
                onAdd = { _, _, _ -> navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            "edit/{desc}/{amount}/{location}/{coordinates}",
            arguments = listOf(
                navArgument("desc") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType },
                navArgument("location") { type = NavType.StringType },
                navArgument("coordinates") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val desc = backStackEntry.arguments?.getString("desc") ?: ""
            val amount = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
            val location = backStackEntry.arguments?.getString("location") ?: ""
            val coordinates = backStackEntry.arguments?.getString("coordinates") ?: ""

            AddExpenseScreen(
                initialDesc = desc,
                initialAmount = amount,
                initialLocation = "$location ($coordinates)",
                onAdd = { _, _, _ -> navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
