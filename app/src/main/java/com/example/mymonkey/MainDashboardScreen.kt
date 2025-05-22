package com.example.mymonkey

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp


@Composable
fun MainDashboardScreen(
    onNavigateToExpenses: () -> Unit,
    onNavigateToCrypto: () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToSummary: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Twój Finansowy Asystent",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Przykładowe zdjęcie — zamień na własne
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Obraz tytułowy",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 24.dp)
            )

            Button(
                onClick = onNavigateToExpenses,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Wydatki")
            }

            Button(
                onClick = onNavigateToCrypto,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Kryptowaluty")
            }

            Button(
                onClick = onNavigateToInvestments,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Inwestycje")
            }

            Button(
                onClick = onNavigateToSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Podsumowanie")
            }
        }
    }
}
