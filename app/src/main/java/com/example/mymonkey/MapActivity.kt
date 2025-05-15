package com.example.mymonkey

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mymonkey.ui.theme.MyMonkeyTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : ComponentActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private lateinit var requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val DEFAULT_LOCATION = LatLng(52.2297, 21.0122) // Warszawa
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                handleLocationLogic()
            } else {
                Toast.makeText(this, "Brak zgody na lokalizację. Pokazuję Warszawę.", Toast.LENGTH_SHORT).show()
                showDefaultLocation()
            }
        }

        setContent {
            MyMonkeyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MapScreen(mapView)
                }
            }
        }
    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        handleLocationLogic()
    }

    private fun handleLocationLogic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "GPS jest wyłączony. Pokazuję Warszawę.", Toast.LENGTH_SHORT).show()
                showDefaultLocation()
                return
            }

            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap?.addMarker(MarkerOptions().position(userLatLng).title("Twoja lokalizacja"))
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                } else {
                    Toast.makeText(this, "Nie udało się pobrać lokalizacji. Pokazuję Warszawę.", Toast.LENGTH_SHORT).show()
                    showDefaultLocation()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Błąd lokalizacji. Pokazuję Warszawę.", Toast.LENGTH_SHORT).show()
                showDefaultLocation()
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun showDefaultLocation() {
        googleMap?.addMarker(MarkerOptions().position(DEFAULT_LOCATION).title("Warszawa"))
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 12f))
    }

    // Cykl życia MapView
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

@Composable
fun MapScreen(mapView: MapView) {
    AndroidView(factory = { mapView })
}
