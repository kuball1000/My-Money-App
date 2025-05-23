package com.example.mymonkey.cache

import android.content.Context
import com.example.mymonkey.dataType.Expense
import com.example.mymonkey.network.deleteExpenseFromSupabase
import com.example.mymonkey.network.fetchExpensesForUser
import com.example.mymonkey.getMetaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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

suspend fun refreshExpensesCache(context: Context) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val apiKey = getMetaData(context, "SUPABASE_API_KEY") ?: return
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
