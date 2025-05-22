// screens/HomeScreen.kt
package com.example.mymonkey.screens

import android.content.Context
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
import androidx.navigation.NavHostController
import com.example.mymonkey.dataType.Expense
import com.example.mymonkey.cache.handleDeleteExpense
import com.example.mymonkey.cache.refreshExpensesCache
import com.example.mymonkey.network.fetchExpensesForUser
import com.example.mymonkey.getMetaData
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun HomeScreenWithHandlers(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expensesState = remember { mutableStateListOf<Expense>() }

    HomeScreen(
        expenses = expensesState,
        onAddExpenseClick = { navController.navigate("add") },
        onEditExpense = { expense ->
            navController.navigate("edit/${expense.id}/${expense.description}/${expense.amount}/${expense.location}/${expense.coordinates}")
        },
        onDeleteExpense = { expense ->
            scope.launch {
                handleDeleteExpense(context, expense)
                val refreshed = fetchExpensesForUser(
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getInt("user_id", -1),
                    getMetaData(context, "SUPABASE_API_KEY") ?: return@launch
                )
                expensesState.clear()
                expensesState.addAll(refreshed)
                refreshExpensesCache(context)
            }
        }
    )
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
