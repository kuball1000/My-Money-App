package com.example.mymonkey

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mymonkey.ui.theme.MyMonkeyTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

class MapActivity : ComponentActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private var selectedLatLng: LatLng? = null
    private var marker: Marker? = null
    private var selectedTitle: String = ""

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setContent {
            MyMonkeyTheme {
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
                                val intent = Intent().apply {
                                    putExtra("lat", selectedLatLng!!.latitude)
                                    putExtra("lng", selectedLatLng!!.longitude)
                                    putExtra("title", selectedTitle)
                                }
                                setResult(RESULT_OK, intent)
                                finish()
                            }) {
                                Text("Dodaj lokalizację")
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    mapView.getMapAsync { map ->
                        map.setOnMapLongClickListener { latLng ->
                            marker?.remove()
                            marker = map.addMarker(
                                MarkerOptions().position(latLng).title("Wybrane miejsce")
                            )
                            selectedLatLng = latLng
                            showAddButton = true
                        }

                        map.setOnMarkerClickListener { clickedMarker ->
                            selectedLatLng = clickedMarker.position
                            marker = clickedMarker
                            showAddButton = true
                            false
                        }

                        map.setOnPoiClickListener { poi ->
                            marker?.remove()
                            marker = map.addMarker(
                                MarkerOptions().position(poi.latLng).title(poi.name)
                            )
                            marker?.showInfoWindow()
                            selectedLatLng = poi.latLng
                            selectedTitle = poi.name
                            showAddButton = true
                        }
                    }
                }

            }
        }
    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        enableUserLocation()
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    marker?.remove()
                    marker = googleMap?.addMarker(
                        MarkerOptions().position(userLatLng).title("Twoja lokalizacja")
                    )
                    selectedLatLng = userLatLng
                } else {
                    showWarsaw()
                }
            }.addOnFailureListener {
                showWarsaw()
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun showWarsaw() {
        val warsaw = LatLng(52.2297, 21.0122)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(warsaw, 12f))
        marker?.remove()
        marker = googleMap?.addMarker(
            MarkerOptions().position(warsaw).title("Warszawa")
        )
        selectedLatLng = warsaw
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableUserLocation()
            } else {
                Toast.makeText(this, "Brak zgody na lokalizację. Pokazuję Warszawę.", Toast.LENGTH_SHORT).show()
                showWarsaw()
            }
        }

    // MapView lifecycle forwarding
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }
}
