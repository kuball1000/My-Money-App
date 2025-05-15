package com.example.mymonkey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.mymonkey.ui.theme.MyMonkeyTheme

class MapActivity : ComponentActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        enableEdgeToEdge()
        setContent {
            MyMonkeyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MapScreen(mapView)
                }
            }
        }
    }

    override fun onMapReady(googleMap: com.google.android.gms.maps.GoogleMap) {
        val location = LatLng(52.2297, 21.0122) // Warszawa
        googleMap.addMarker(MarkerOptions().position(location).title("Marker in Warsaw"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
    }

    // Pamiętaj o forwardowaniu cyklu życia MapView
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
