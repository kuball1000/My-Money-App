package com.example.mymonkey

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AddExpenseScreen(
    onAdd: (String, Double) -> Unit,
    onCancel: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Dodaj nowy wydatek", style = MaterialTheme.typography.headlineSmall)

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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Anuluj")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (description.isNotBlank() && amount != null) {
                        onAdd(description, amount)
                    } else {
                        Toast.makeText(context, "Wprowadź poprawne dane", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Dodaj")
            }
        }
    }
}
