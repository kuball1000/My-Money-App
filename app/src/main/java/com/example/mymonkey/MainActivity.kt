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

@Composable
fun HomeScreenWithHandlers(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expensesState = remember { mutableStateListOf<Expense>() }

    HomeScreen(
        expenses = expensesState,
        onAddExpenseClick = { navController.navigate("add") },
        onEditExpense = { expense ->
            navController.navigate("edit/${expense.description}/${expense.amount}/${expense.location}/${expense.coordinates}")
        },
        onDeleteExpense = { expense ->
            scope.launch {
                handleDeleteExpense(context, expense)
                val refreshed = fetchExpensesForUser(
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getInt("user_id", -1),
                    getMetaData(context, "SUPABASE_API_KY") ?: return@launch
                )
                expensesState.clear()
                expensesState.addAll(refreshed)
                refreshExpensesCache(context)
            }
        }
    )
}

suspend fun handleDeleteExpense(context: Context, expense: Expense) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val apiKey = getMetaData(context, "SUPABASE_API_KEY") ?: return

    val success = deleteExpenseFromSupabase(expense.id, apiKey)
    if (success) {
        val current = sharedPreferences.getString("cached_expenses", null)
        current?.let {
            val updatedArray = JSONArray(it).let { arr ->
                JSONArray().apply {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        if (obj.getInt("id") != expense.id) {
                            put(obj)
                        }
                    }
                }
            }
            sharedPreferences.edit().putString("cached_expenses", updatedArray.toString()).apply()
        }
    }
}


@Composable
fun HomeScreen(
    expenses: SnapshotStateList<Expense>,
    onAddExpenseClick: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val context = LocalContext.current
    val apiKey = getMetaData(context, "SUPABASE_API_KEY")
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val userId = sharedPreferences.getInt("user_id", -1)
    val scope = rememberCoroutineScope()
    val deletingIds = remember { mutableStateListOf<Int>() }

    LaunchedEffect(userId) {
        if (userId != -1 && apiKey != null) {
            val fetched = fetchExpensesForUser(userId, apiKey)
            expenses.clear()
            expenses.addAll(fetched)

            val cachedJson = JSONArray().apply {
                fetched.forEach { expense ->
                    put(
                        JSONObject().apply {
                            put("id", expense.id)
                            put("description", expense.description)
                            put("amount", expense.amount)
                            put("location", expense.location)
                            put("coordinates", expense.coordinates)
                        }
                    )
                }
            }.toString()

            sharedPreferences.edit()
                .putString("cached_expenses", cachedJson)
                .apply()
        }
    }

    val total = expenses.sumOf { it.amount }

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
                items(expenses) { expense ->
                    val isDeleting = expense.id in deletingIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !isDeleting) { onEditExpense(expense) },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(expense.description, style = MaterialTheme.typography.titleMedium)
                                Text("${String.format("%.2f", expense.amount)} zł", style = MaterialTheme.typography.bodyMedium)
                                Text("Lokalizacja: ${expense.location}", style = MaterialTheme.typography.bodySmall)
                                Text("Koordynaty: ${expense.coordinates}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = {
                                    deletingIds.add(expense.id)
                                    scope.launch {
                                        // Remove from UI and cache first
                                        expenses.remove(expense)
                                        val current = sharedPreferences.getString("cached_expenses", null)
                                        current?.let {
                                            val updatedArray = JSONArray(it).let { arr ->
                                                JSONArray().apply {
                                                    for (i in 0 until arr.length()) {
                                                        val obj = arr.getJSONObject(i)
                                                        if (obj.getInt("id") != expense.id) {
                                                            put(obj)
                                                        }
                                                    }
                                                }
                                            }
                                            sharedPreferences.edit().putString("cached_expenses", updatedArray.toString()).apply()
                                        }
                                        onDeleteExpense(expense)
                                        deletingIds.remove(expense.id)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Usuń"
                                    )
                                }
                            }
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
): List<Expense> {
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
                val result = mutableListOf<Expense>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    result.add(
                        Expense(
                            id = obj.getInt("id"),
                            description = obj.optString("description", ""),
                            amount = obj.optDouble("amount", 0.0),
                            location = obj.optString("location", ""),
                            coordinates = obj.optString("coordinates", "")
                        )
                    )
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

suspend fun deleteExpenseFromSupabase(
    id: Int,
    apiKey: String
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://hveselshovofxxwuozqj.supabase.co/rest/v1/expenses?id=eq.$id"

            val request = Request.Builder()
                .url(url)
                .delete()
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $apiKey")
                .build()

            val response = OkHttpClient().newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

suspend fun refreshExpensesCache(context: Context) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val apiKey = getMetaData(context, "SUPABASE_API_KY") ?: return
    val userId = sharedPreferences.getInt("user_id", -1)
    if (userId == -1) return

    val fetched = fetchExpensesForUser(userId, apiKey)
    val newCache = JSONArray().apply {
        fetched.forEach { expense ->
            put(
                JSONObject().apply {
                    put("id", expense.id)
                    put("description", expense.description)
                    put("amount", expense.amount)
                    put("location", expense.location)
                    put("coordinates", expense.coordinates)
                }
            )
        }
    }.toString()
    sharedPreferences.edit().putString("cached_expenses", newCache).apply()
}
