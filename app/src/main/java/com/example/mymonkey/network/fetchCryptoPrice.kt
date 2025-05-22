package com.example.mymonkey.network

import android.content.Context
import com.example.mymonkey.getMetaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

suspend fun fetchCryptoPrice(context: Context, name: String): Double {
    return withContext(Dispatchers.IO) {
        try {
            val apiKey = getMetaData(context, "COINGECKO_API_KEY") ?: return@withContext -1.0

            val coinId = when (name.lowercase()) {
                "bitcoin" -> "bitcoin"
                "ethereum" -> "ethereum"
                "xrp" -> "ripple"
                "dogecoin" -> "dogecoin"
                else -> return@withContext -1.0
            }

            val url = "https://api.coingecko.com/api/v3/coins/$coinId"
            val request = Request.Builder()
                .url(url)
                .header("x-cg-demo-api-key", apiKey)
                .build()

            val response = OkHttpClient().newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext -1.0
                val json = JSONObject(body)
                json.getJSONObject("market_data")
                    .getJSONObject("current_price")
                    .getDouble("pln")
            } else {
                -1.0
            }
        } catch (e: Exception) {
            -1.0
        }
    }
}
