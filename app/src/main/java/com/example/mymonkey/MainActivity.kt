package com.example.mymonkey

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.mymonkey.ui.theme.MyMonkeyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject


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

@Composable
fun ExpenseApp() {
    val navController = rememberNavController()
    val expenses = remember { mutableStateListOf<Triple<String, Double, String>>() }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = { navController.navigate("home") })
        }
        composable("home") {
            HomeScreen(
                expenses = expenses,
                onAddExpenseClick = { navController.navigate("add") }
            )
        }
        composable("add") {
            AddExpenseScreen(
                onAdd = { description, amount, location ->
                    expenses.add(Triple(description, amount, location))
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun HomeScreen(
    expenses: List<Triple<String, Double, String>>,
    onAddExpenseClick: () -> Unit
) {
    val total = expenses.sumOf { it.second }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpenseClick) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Suma: ${String.format("%.2f", total)} zł",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                items(expenses) { (desc, amt, loc) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(desc, style = MaterialTheme.typography.titleMedium)
                            Text("${String.format("%.2f", amt)} zł", style = MaterialTheme.typography.bodyMedium)
                            Text("Lokalizacja: $loc", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

suspend fun sendExpenseToSupabase(
    userId: Int,
    description: String,
    amount: Double,
    location: String,
    coordinates: String,
    apiKey: String?
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("user_id", userId)
                put("description", description)
                put("amount", amount)
                put("location", location)
                put("coordinates", coordinates)
            }

            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://hveselshovofxxwuozqj.supabase.co/rest/v1/expenses")
                .header("apikey", apiKey ?: "")
                .header("Authorization", "Bearer ${apiKey ?: ""}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(requestBody)
                .build()

            val response = OkHttpClient().newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}