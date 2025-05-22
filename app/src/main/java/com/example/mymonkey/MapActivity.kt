package com.example.mymonkey

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.mymonkey.ui.theme.MyMonkeyTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import android.location.Geocoder
import java.util.Locale

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyMonkeyTheme {
                MapScreen(onConfirmLocation = { lat, lng, title ->
                    val intent = Intent().apply {
                        putExtra("lat", lat)
                        putExtra("lng", lng)
                        putExtra("title", title)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                })
            }
        }
    }
}

@Composable
fun MapScreen(onConfirmLocation: (Double, Double, String) -> Unit) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var selectedTitle by remember { mutableStateOf("") }
    var showAddButton by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        if (showAddButton && selectedLatLng != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Button(onClick = {
                    selectedLatLng?.let {
                        onConfirmLocation(it.latitude, it.longitude, selectedTitle)
                    }
                }) {
                    Text("Dodaj lokalizację")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    val userLatLng = location?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(52.2297, 21.0122)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    selectedLatLng = userLatLng
                    selectedTitle = getAddressFromLatLng(context, userLatLng)
                    showAddButton = true
                }.addOnFailureListener {
                    val warsaw = LatLng(52.2297, 21.0122)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(warsaw, 12f))
                    selectedLatLng = warsaw
                    selectedTitle = "Warszawa"
                    showAddButton = true
                }
            } else {
                val warsaw = LatLng(52.2297, 21.0122)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(warsaw, 12f))
                selectedLatLng = warsaw
                selectedTitle = "Warszawa"
                showAddButton = true
            }

            map.setOnMapLongClickListener { latLng ->
                map.clear()
                map.addMarker(MarkerOptions().position(latLng).title("Wybrane miejsce"))
                selectedLatLng = latLng
                selectedTitle = getAddressFromLatLng(context, latLng)
                showAddButton = true
            }

            map.setOnPoiClickListener { poi ->
                map.clear()
                map.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))?.showInfoWindow()
                selectedLatLng = poi.latLng
                selectedTitle = "${poi.name}, ${getAddressFromLatLng(context, poi.latLng)}"
                showAddButton = true
            }
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) = mapView.onCreate(null)
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }

        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return mapView
}

fun getAddressFromLatLng(context: android.content.Context, latLng: LatLng): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val street = address.thoroughfare ?: ""
            val number = address.subThoroughfare ?: ""
            val city = address.locality ?: ""
            "$street $number, $city".trim()
        } else {
            "Nieznany adres"
        }
    } catch (e: Exception) {
        "Nieznany adres"
    }
}
