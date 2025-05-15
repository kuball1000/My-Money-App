package com.example.mymonkey

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
            MyMonkeyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocationInputWithMapSelector()
                }
            }
        }
    }
}

@Composable
fun LocationInputWithMapSelector() {
    val context = LocalContext.current
    var locationText by remember { mutableStateOf("") }

    // Launcher do odbierania wyniku z MapActivity
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", 0.0)
            val lng = result.data?.getDoubleExtra("lng", 0.0)
            var title = result.data?.getStringExtra("title") ?: "Brak danych"
            if (lat != null && lng != null) {
                locationText = "$title ($lat, $lng)"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = locationText,
            onValueChange = { locationText = it },
            label = { Text("Lokalizacja") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            val intent = Intent(context, MapActivity::class.java)
            launcher.launch(intent)
        }) {
            Text("Wybierz na mapie")
        }
    }
}
