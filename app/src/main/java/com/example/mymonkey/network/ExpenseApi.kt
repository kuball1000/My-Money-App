package com.example.mymonkey.network

import com.example.mymonkey.dataType.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

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


suspend fun updateExpenseInSupabase(
    id: Int,
    description: String,
    amount: Double,
    location: String,
    coordinates: String,
    apiKey: String?
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("description", description)
                put("amount", amount)
                put("location", location)
                put("coordinates", coordinates)
            }

            val request = Request.Builder()
                .url("https://hveselshovofxxwuozqj.supabase.co/rest/v1/expenses?id=eq.$id")
                .patch(json.toString().toRequestBody("application/json".toMediaType()))
                .header("apikey", apiKey ?: "")
                .header("Authorization", "Bearer ${apiKey ?: ""}")
                .header("Content-Type", "application/json")
                .build()

            val response = OkHttpClient().newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

