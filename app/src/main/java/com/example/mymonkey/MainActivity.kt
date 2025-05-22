package com.example.mymonkey

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.mymonkey.dataType.Expense
import com.example.mymonkey.screens.HomeScreenWithHandlers
import com.example.mymonkey.ui.theme.MyMonkeyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyMonkeyTheme {
                ExpenseApp()
            }
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ExpenseApp() {
    val navController = rememberNavController()

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
            "edit/{id}/{desc}/{amount}/{location}/{coordinates}",
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("desc") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType },
                navArgument("location") { type = NavType.StringType },
                navArgument("coordinates") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id")
            val desc = backStackEntry.arguments?.getString("desc") ?: ""
            val amount = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
            val location = backStackEntry.arguments?.getString("location") ?: ""
            val coordinates = backStackEntry.arguments?.getString("coordinates") ?: ""

            AddExpenseScreen(
                expenseId = id,
                initialDesc = desc,
                initialAmount = amount,
                initialLocation = "$location ($coordinates)",
                onAdd = { _, _, _ -> navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}