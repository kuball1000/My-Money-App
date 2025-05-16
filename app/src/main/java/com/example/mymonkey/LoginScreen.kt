package com.example.mymonkey

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.security.MessageDigest

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiKey = getMetaData(context, "SUPABASE_API_KEY")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = login,
            onValueChange = {
                login = it.filterNot { c -> c.isWhitespace() }
            },
            label = { Text("Login") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                val (success, errorMessage) = checkLogin(login, hashPassword(password), apiKey)
                if (success) {
                    onLoginSuccess()
                } else {
                    Toast.makeText(
                        context,
                        errorMessage ?: "Błędny login lub hasło",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }) {
            Text("Zaloguj")
        }
    }
}

suspend fun checkLogin(login: String, password: String, apiKey: String?): Pair<Boolean, String?> {
    return withContext(Dispatchers.IO) {
        try {

            val encodedLogin = URLEncoder.encode(login, "UTF-8")
            val encodedPassword = URLEncoder.encode(password, "UTF-8")
            val url =
                "https://hveselshovofxxwuozqj.supabase.co/rest/v1/users?login=eq.$encodedLogin&password=eq.$encodedPassword"

            val request = Request.Builder()
                .url(url)
                .header("apikey", apiKey.toString())
                .header("Authorization", "Bearer ${apiKey}")
                .header("Accept", "application/json")
                .build()

            val response = OkHttpClient().newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                val users = JSONArray(responseBody)
                Pair(users.length() > 0, null)
            } else {
                Pair(false, "HTTP ${response.code}: ${responseBody}")
            }
        } catch (e: Exception) {
            Pair(false, "Exception: ${e.localizedMessage}")
        }
    }
}

fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun getMetaData(context: Context, key: String): String? {
    return try {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        appInfo.metaData?.getString(key)
    } catch (e: Exception) {
        null
    }
}