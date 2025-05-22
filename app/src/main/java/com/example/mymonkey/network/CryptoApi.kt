package com.example.mymonkey.network

import android.util.Log
import com.example.mymonkey.dataType.Crypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

suspend fun sendCryptoToSupabase(
    userId: Int,
    name: String,
    buyPrice: Double,
    amount: Double,
    apiKey: String?
): Result<Crypto> {
    return withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("user_id", userId)
                put("name", name)
                put("buy_price", buyPrice)
                put("amount", amount)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://hveselshovofxxwuozqj.supabase.co/rest/v1/crypto") // Uwaga: poprawna nazwa tabeli
                .header("apikey", apiKey ?: "")
                .header("Authorization", "Bearer ${apiKey ?: ""}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(requestBody)
                .build()

            val response = OkHttpClient().newCall(request).execute()
            val responseBody = response.body?.string()
            val arr = JSONArray(responseBody)
            val obj = arr.getJSONObject(0)

            val created = Crypto(
                id = obj.getInt("id"),
                name = obj.getString("name"),
                buyPrice = obj.getDouble("buy_price"),
                amount = obj.getDouble("amount")
            )

            Result.success(created)
        } catch (e: Exception) {
            Log.e("CryptoApi", "EXCEPTION: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }
}

suspend fun fetchCryptosForUser(userId: Int, apiKey: String): List<Crypto> {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://hveselshovofxxwuozqj.supabase.co/rest/v1/crypto?user_id=eq.$userId&order=created_at.asc"

            val request = Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")
                .build()

            val response = OkHttpClient().newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && body != null) {
                val arr = JSONArray(body)
                List(arr.length()) { i ->
                    val obj = arr.getJSONObject(i)
                    Crypto(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        buyPrice = obj.getDouble("buy_price"),
                        amount = obj.getDouble("amount")
                    )
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

suspend fun deleteCryptoFromSupabase(id: Int, apiKey: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://hveselshovofxxwuozqj.supabase.co/rest/v1/crypto?id=eq.$id")
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

suspend fun updateCryptoInSupabase(
    id: Int,
    name: String,
    buyPrice: Double,
    amount: Double,
    apiKey: String?
): Boolean = withContext(Dispatchers.IO) {
    try {
        val json = JSONObject().apply {
            put("name", name)
            put("buy_price", buyPrice)
            put("amount", amount)
        }

        val request = Request.Builder()
            .url("https://hveselshovofxxwuozqj.supabase.co/rest/v1/crypto?id=eq.$id")
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

