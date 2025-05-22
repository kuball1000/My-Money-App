package com.example.mymonkey

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.mymonkey.network.sendExpenseToSupabase
import com.example.mymonkey.network.updateExpenseInSupabase
import kotlinx.coroutines.launch


@Composable
fun AddExpenseScreen(
    expenseId: Int? = null,
    initialDesc: String,
    initialAmount: Double,
    initialLocation: String,
    onAdd: (String, Double, String) -> Unit,
    onCancel: () -> Unit
) {
    var description by remember { mutableStateOf(initialDesc) }
    var amountText by remember { mutableStateOf(initialAmount.toString()) }
    var locationText by remember { mutableStateOf(initialLocation) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSuccessDialog by remember { mutableStateOf(false) }
    val coordinates = Regex("\\(([^)]+)\\)").find(locationText)?.groupValues?.get(1) ?: ""

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val lat = result.data?.getDoubleExtra("lat", 0.0)
            val lng = result.data?.getDoubleExtra("lng", 0.0)
            val title = result.data?.getStringExtra("title") ?: "Brak danych"
            if (lat != null && lng != null) {
                locationText = "$title ($lat, $lng)"
            }
        } else {
             Log.w("AddExpenseScreen", "Nie otrzymano danych z MapActivity")
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = if (expenseId == null) "Dodaj nowy wydatek" else "Edytuj wydatek",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis wydatku") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Kwota") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            OutlinedTextField(
                value = locationText,
                onValueChange = {},
                label = { Text("Lokalizacja") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            Button(
                onClick = {
                    val intent = Intent(context, MapActivity::class.java)
                    launcher.launch(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Wybierz lokalizację na mapie")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Anuluj")
                }
                Spacer(modifier = Modifier.width(8.dp))
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                val apiKey = getMetaData(context, "SUPABASE_API_KEY")
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val userId = sharedPreferences.getInt("user_id", -1)

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (description.isNotBlank() && amount != null) {
                            val locationName = locationText.substringBefore(" (").trim()
                            val coordinates = Regex("\\(([^)]+)\\)").find(locationText)?.groupValues?.get(1) ?: ""


                            scope.launch {
                                val success = if (expenseId != null) {
                                    updateExpenseInSupabase(
                                        id = expenseId,
                                        description = description,
                                        amount = amount,
                                        location = locationName,
                                        coordinates = coordinates,
                                        apiKey = apiKey
                                    )
                                } else {
                                    sendExpenseToSupabase(
                                        userId = userId,
                                        description = description,
                                        amount = amount,
                                        location = locationName,
                                        coordinates = coordinates,
                                        apiKey = apiKey
                                    )
                                }
                                if (success) {
                                    showSuccessDialog = true
                                } else {
                                    Toast.makeText(context, "Błąd zapisu do bazy", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Wprowadź poprawne dane", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (expenseId == null) "Dodaj" else "Zapisz zmiany")
                }
            }
        }
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false; onAdd(description, amountText.toDouble(), locationText) },
                confirmButton = {
                    TextButton(onClick = {
                        showSuccessDialog = false
                        onAdd(description, amountText.toDouble(), locationText)
                    }) {
                        Text("OK")
                    }
                },
                title = { Text("Sukces") },
                text = { Text("Dane zostały zapisane") }
            )
        }
    }
}
