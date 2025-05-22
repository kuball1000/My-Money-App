package com.example.mymonkey

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.mymonkey.dataType.Crypto
import com.example.mymonkey.network.fetchCryptoPrice
import com.example.mymonkey.network.sendCryptoToSupabase
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import com.example.mymonkey.dataType.AvailableCryptos
import com.example.mymonkey.network.updateCryptoInSupabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCryptoScreen(
    editingId: Int? = null,
    initialName: String = "",
    initialPrice: Double = 0.0,
    initialAmount: Double = 0.0,
    onAdd: (Crypto) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var price by remember { mutableStateOf(initialPrice.toString()) }
    var amount by remember { mutableStateOf(initialAmount.toString()) }
    val context = LocalContext.current


    val computedSum = price.toDoubleOrNull()?.let { p ->
        amount.toDoubleOrNull()?.let { a -> p * a }
    } ?: 0.0

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Wybierz kryptowalutę") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AvailableCryptos.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                name = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Cena zakupu") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val fetchedPrice = fetchCryptoPrice(context, name)
                            price = fetchedPrice.toString()
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Pobierz")
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Ilość") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            OutlinedTextField(
                value = String.format("%.2f", computedSum),
                onValueChange = {},
                label = { Text("Suma") },
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Anuluj")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val buy = price.toDoubleOrNull() ?: 0.0
                        val qty = amount.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            val sharedPreferences =
                                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            val userId = sharedPreferences.getInt("user_id", -1)
                            val apiKey = getMetaData(context, "SUPABASE_API_KEY")

                            scope.launch {
                                val success = if (editingId != null) {
                                    updateCryptoInSupabase(
                                        id = editingId,
                                        name = name,
                                        buyPrice = buy,
                                        amount = qty,
                                        apiKey = apiKey
                                    )
                                } else {
                                    val result = sendCryptoToSupabase(userId, name, buy, qty, apiKey)
                                    result.isSuccess
                                }

                                if (success) {
                                    onAdd(Crypto(name, buy, qty, editingId ?: -1))
                                } else {
                                    Toast.makeText(context, "Błąd zapisu do bazy", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (editingId != null) "Zapisz zmiany" else "Dodaj")
                }
            }
        }
    }
}
