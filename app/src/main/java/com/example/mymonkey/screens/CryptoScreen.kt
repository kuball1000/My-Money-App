package com.example.mymonkey.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mymonkey.dataType.Crypto
import com.example.mymonkey.dataType.CryptoWithPrice
import kotlinx.coroutines.launch


@Composable
fun CryptoScreen(
    cryptoList: List<CryptoWithPrice>,
    isLoading: Boolean,
    onAddClick: () -> Unit,
    onEditCrypto: (Crypto) -> Unit,
    onDeleteCrypto: (Crypto) -> Unit
) {
    val total = cryptoList.sumOf { it.crypto.buyPrice * it.crypto.amount }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val deletingIds = remember { mutableStateListOf<Int>() }

    val totalProfit = cryptoList.sumOf { it.profit }
    val profitColor = if (totalProfit >= 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
    val profitLabel = if (totalProfit >= 0) "Zysk" else "Strata"

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text("+")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ładowanie danych...", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Suma: ${String.format("%.2f", total)} zł",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "$profitLabel: ${String.format("%.2f", totalProfit)} zł",
                        style = MaterialTheme.typography.titleMedium,
                        color = profitColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn {
                        items(cryptoList) { cryptoWithPrice ->
                            val crypto = cryptoWithPrice.crypto
                            val current = cryptoWithPrice.currentPrice
                            val profit = cryptoWithPrice.profit
                            val isGain = profit >= 0
                            val isDeleting = crypto.id in deletingIds
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(enabled = !isDeleting) { onEditCrypto(crypto) },
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
                                        Text(crypto.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${crypto.buyPrice} zł x ${crypto.amount}", style = MaterialTheme.typography.bodyMedium)
                                        Text("Suma: ${String.format("%.2f", crypto.buyPrice * crypto.amount)} zł", style = MaterialTheme.typography.bodySmall)
                                        Text("Aktualny kurs: ${String.format("%.2f", current)} zł", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = (if (isGain) "Zysk" else "Strata") + ": ${String.format("%.2f", profit)} zł",
                                            color = if (isGain) Color(0xFF388E3C) else Color(0xFFD32F2F),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (isDeleting) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        IconButton(onClick = {
                                            deletingIds.add(crypto.id)
                                            scope.launch {
                                                onDeleteCrypto(crypto)
                                                deletingIds.remove(crypto.id)
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
    }
}

