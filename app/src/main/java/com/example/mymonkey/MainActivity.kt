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
import com.example.mymonkey.cache.getCachedCryptos
import com.example.mymonkey.cache.refreshCryptoCache
import com.example.mymonkey.dataType.Crypto
import com.example.mymonkey.dataType.CryptoWithPrice
import com.example.mymonkey.dataType.Expense
import com.example.mymonkey.network.deleteCryptoFromSupabase
import com.example.mymonkey.network.fetchCryptoPrice
import com.example.mymonkey.screens.CryptoScreen
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
            LoginScreen(onLoginSuccess = { navController.navigate("dashboard") })
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
        composable("dashboard") {
            MainDashboardScreen(
                onNavigateToExpenses = { navController.navigate("home") },
                onNavigateToCrypto = { navController.navigate("crypto") },
                onNavigateToInvestments = { /* TODO */ },
                onNavigateToSummary = { /* TODO */ }
            )
        }
        composable("crypto") {
            var cryptoList by remember { mutableStateOf<List<Crypto>>(emptyList()) }
            var cryptoWithPrices by remember { mutableStateOf<List<CryptoWithPrice>>(emptyList()) }

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
            val newCrypto = savedStateHandle?.get<Crypto>("new_crypto")
            var isLoading by remember { mutableStateOf(true) }

            val coinMap = mapOf(
                "bitcoin" to "bitcoin",
                "ethereum" to "ethereum",
                "xrp" to "ripple",
                "dogecoin" to "dogecoin"
            )
            val prices = mutableMapOf<String, Double>()

            LaunchedEffect(newCrypto) {
                newCrypto?.let {
                    cryptoList = cryptoList + it
                    savedStateHandle.remove<Crypto>("new_crypto")
                }
            }

            LaunchedEffect(Unit) {
                isLoading = true
                refreshCryptoCache(context)
                val cached = getCachedCryptos(context)
                val coinMap = mapOf(
                    "bitcoin" to "bitcoin",
                    "ethereum" to "ethereum",
                    "xrp" to "ripple",
                    "dogecoin" to "dogecoin"
                )

                val prices = mutableMapOf<String, Double>()

                for (crypto in cached) {
                    val id = coinMap[crypto.name.lowercase()] ?: continue
                    val price = fetchCryptoPrice(context, crypto.name)
                    prices[crypto.name.lowercase()] = price
                }

                cryptoWithPrices = cached.map {
                    CryptoWithPrice(it, prices[it.name.lowercase()] ?: 0.0)
                }

                isLoading = false
            }

            val updatedCrypto = savedStateHandle?.get<Crypto>("updated_crypto")
            LaunchedEffect(updatedCrypto) {
                updatedCrypto?.let {
                    isLoading = true
                    refreshCryptoCache(context)
                    val cached = getCachedCryptos(context)
                    cryptoList = cached

                    val prices = mutableMapOf<String, Double>()
                    val coinMap = mapOf(
                        "bitcoin" to "bitcoin",
                        "ethereum" to "ethereum",
                        "xrp" to "ripple",
                        "dogecoin" to "dogecoin"
                    )

                    for (crypto in cached) {
                        val id = coinMap[crypto.name.lowercase()] ?: continue
                        val price = fetchCryptoPrice(context, crypto.name)
                        prices[crypto.name.lowercase()] = price
                    }

                    cryptoWithPrices = cached.map {
                        CryptoWithPrice(it, prices[it.name.lowercase()] ?: 0.0)
                    }
                    isLoading = false
                    savedStateHandle.remove<Crypto>("updated_crypto")
                }
            }


            CryptoScreen(
                cryptoList = cryptoWithPrices,
                isLoading = isLoading,
                onAddClick = { navController.navigate("add_crypto") },
                onEditCrypto = { crypto ->
                    navController.navigate("edit_crypto/${crypto.id}/${crypto.name}/${crypto.buyPrice.toFloat()}/${crypto.amount.toFloat()}")
                },
                onDeleteCrypto = { crypto ->
                    scope.launch {
                        val apiKey = getMetaData(context, "SUPABASE_API_KEY") ?: return@launch
                        val success = deleteCryptoFromSupabase(crypto.id, apiKey)
                        if (success) {
                            refreshCryptoCache(context)
                            val cached = getCachedCryptos(context)

                            cryptoList = cached


                            for (item in cached) {
                                val id = coinMap[item.name.lowercase()] ?: continue
                                val price = fetchCryptoPrice(context, item.name)
                                prices[item.name.lowercase()] = price
                            }
                            cryptoWithPrices = cached.map {
                                CryptoWithPrice(it, prices[it.name.lowercase()] ?: 0.0)
                            }

                            // aktualizuj cache
                            val current = sharedPreferences.getString("cached_cryptos", null)
                            current?.let {
                                val updatedArray = JSONArray(it).let { arr ->
                                    JSONArray().apply {
                                        for (i in 0 until arr.length()) {
                                            val obj = arr.getJSONObject(i)
                                            if (obj.getInt("id") != crypto.id) {
                                                put(obj)
                                            }
                                        }
                                    }
                                }
                                sharedPreferences.edit().putString("cached_cryptos", updatedArray.toString()).apply()
                            }
                            val coinMap = mapOf(
                                "bitcoin" to "bitcoin",
                                "ethereum" to "ethereum",
                                "xrp" to "ripple",
                                "dogecoin" to "dogecoin"
                            )
                            val prices = mutableMapOf<String, Double>()
                            for (crypto in cryptoList) {
                                val id = coinMap[crypto.name.lowercase()] ?: continue
                                val price = fetchCryptoPrice(context, crypto.name)
                                prices[crypto.name.lowercase()] = price
                            }
                            cryptoWithPrices = cryptoList.map {
                                CryptoWithPrice(it, prices[it.name.lowercase()] ?: 0.0)
                            }
                        }
                    }
                }
            )
        }


        composable("add_crypto") {
            AddCryptoScreen(
                onAdd = { newCrypto ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("new_crypto", newCrypto)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            "edit_crypto/{id}/{name}/{buyPrice}/{amount}",
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType },
                navArgument("buyPrice") { type = NavType.FloatType },
                navArgument("amount") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments!!.getInt("id")
            val name = backStackEntry.arguments!!.getString("name") ?: ""
            val buyPrice = backStackEntry.arguments!!.getFloat("buyPrice").toDouble()
            val amount = backStackEntry.arguments!!.getFloat("amount").toDouble()

            AddCryptoScreen(
                editingId = id,
                initialName = name,
                initialPrice = buyPrice,
                initialAmount = amount,
                onAdd = { updated ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("updated_crypto", updated)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

    }
}