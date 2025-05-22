package com.example.mymonkey.cache

import android.content.Context
import com.example.mymonkey.dataType.Crypto
import com.example.mymonkey.getMetaData
import com.example.mymonkey.network.fetchCryptosForUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

suspend fun refreshCryptoCache(context: Context) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val apiKey = getMetaData(context, "SUPABASE_API_KEY") ?: return
    val userId = sharedPreferences.getInt("user_id", -1)
    if (userId == -1) return

    val fetched = fetchCryptosForUser(userId, apiKey)
    val newCache = JSONArray().apply {
        fetched.forEach { crypto ->
            put(
                JSONObject().apply {
                    put("id", crypto.id)
                    put("name", crypto.name)
                    put("buy_price", crypto.buyPrice)
                    put("amount", crypto.amount)
                }
            )
        }
    }.toString()

    sharedPreferences.edit().putString("cached_cryptos", newCache).apply()
}

suspend fun getCachedCryptos(context: Context): List<Crypto> = withContext(Dispatchers.IO) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val cached = sharedPreferences.getString("cached_cryptos", null) ?: return@withContext emptyList()
    val arr = JSONArray(cached)
    List(arr.length()) { i ->
        val obj = arr.getJSONObject(i)
        Crypto(
            id = obj.getInt("id"),
            name = obj.getString("name"),
            buyPrice = obj.getDouble("buy_price"),
            amount = obj.getDouble("amount")
        )
    }
}
