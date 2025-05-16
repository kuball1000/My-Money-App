package com.example.mymonkey

import android.app.Activity
import android.content.Context
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
import org.json.JSONArray
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
    onAddExpenseClick: () -> Unit
) {
    val context = LocalContext.current
    val apiKey = getMetaData(context, "SUPABASE_API_KEY")
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val userId = sharedPreferences.getInt("user_id", -1)
    val expensesState = remember { mutableStateListOf<Triple<String, Double, Pair<String, String>>>() }
    val total = expensesState.sumOf { it.second }

    LaunchedEffect(userId) {
        if (userId != -1 && apiKey != null) {
            val fetched = fetchExpensesForUser(userId, apiKey)
            if (fetched.isNotEmpty()) {
                expensesState.clear()
                expensesState.addAll(fetched)

                val cachedJson = JSONArray().apply {
                    fetched.forEach { (desc, amt, locCoord) ->
                        put(
                            JSONObject().apply {
                                put("description", desc)
                                put("amount", amt)
                                put("location", locCoord.first)
                                put("coordinates", locCoord.second)
                            }
                        )
                    }
                }.toString()

                sharedPreferences.edit()
                    .putString("cached_expenses", cachedJson)
                    .apply()
            } else {
                val cached = sharedPreferences.getString("cached_expenses", null)
                cached?.let {
                    val array = JSONArray(it)
                    expensesState.clear()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        expensesState.add(
                            Triple(
                                obj.getString("description"),
                                obj.getDouble("amount"),
                                obj.getString("location") to obj.getString("coordinates")
                            )
                        )
                    }
                }
            }
        }
    }

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
                items(expensesState) { (desc, amt, locCoord) ->
                    val (location, coords) = locCoord
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(desc, style = MaterialTheme.typography.titleMedium)
                            Text("${String.format("%.2f", amt)} zł", style = MaterialTheme.typography.bodyMedium)
                            Text("Lokalizacja: $location", style = MaterialTheme.typography.bodySmall)
                            Text("Koordynaty: $coords", style = MaterialTheme.typography.bodySmall)
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

suspend fun fetchExpensesForUser(
    userId: Int,
    apiKey: String
): List<Triple<String, Double, Pair<String, String>>> {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://hveselshovofxxwuozqj.supabase.co/rest/v1/expenses?user_id=eq.$userId"

            val request = Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")
                .build()

            val response = OkHttpClient().newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                val result = mutableListOf<Triple<String, Double, Pair<String, String>>>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val description = obj.optString("description", "")
                    val amount = obj.optDouble("amount", 0.0)
                    val location = obj.optString("location", "")
                    val coordinates = obj.optString("coordinates", "")
                    result.add(Triple(description, amount, location to coordinates))
                }

                result
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}