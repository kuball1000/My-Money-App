package com.example.mymonkey

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mymonkey.ui.theme.MyMonkeyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseApp()
        }
    }
}

@Composable
fun ExpenseApp() {
    val navController = rememberNavController()
    val expenses = remember { mutableStateListOf<Pair<String, Double>>() }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                expenses = expenses,
                onAddExpenseClick = { navController.navigate("add") }
            )
        }
        composable("add") {
            AddExpenseScreen(
                onAdd = { description, amount ->
                    expenses.add(description to amount)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun HomeScreen(expenses: List<Pair<String, Double>>, onAddExpenseClick: () -> Unit) {
    val total = expenses.sumOf { it.second }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpenseClick) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Suma: ${String.format("%.2f", total)} zł",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                items(expenses) { (desc, amt) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(desc, style = MaterialTheme.typography.titleMedium)
                            Text("${String.format("%.2f", amt)} zł", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
