package com.example.wifiucspapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.wifiucspapp.ui.theme.WiFiUCSPAppTheme

// Main activity class that manages the UI and interactions.
class MainActivity : ComponentActivity() {
    // Lateinit for late initialization of WifiManager.
    private lateinit var wifiManager: WifiManager

    // Mutable list to store the results of Wi-Fi scans.
    private val wifiResults = mutableStateListOf<ScanResult>()

    // Setup to request location permissions and handle the result.
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            Toast.makeText(this, "Permiso de ubicación aceptado", Toast.LENGTH_SHORT).show()
            startWifiScan()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // BroadcastReceiver to handle actions when Wi-Fi scan results are available.
    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Check if the received intent is for the Wi-Fi scan results.
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                // Safely access Wi-Fi scan results, considering permission checks.
                safeAccessWifiScanResults(context)
            }
        }
    }

    // Called when the activity is first created.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)  // Call to the super class's onCreate method
        // Get the WiFi system service to manage WiFi
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Setting the content of the activity using Jetpack Compose
        setContent {
            WiFiUCSPAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(Modifier.padding(innerPadding))
                }
            }
        }
    }

    // Composable function to define the main content of the UI.
    @Composable
    fun MainContent(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        Column(modifier = modifier) {
            Greeting("Diegoooo")
            if (shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Text("Se requiere permiso de ubicación para acceder a las redes WiFi.")
                RequestPermissionButton(context, "Solicitar permiso")
            } else {
                Text("Permiso de ubicación otorgado.")
                RequestPermissionButton(context, "Escanear redes WiFi")
            }
            WifiNetworksList()
        }
    }

    // Composable function to display a greeting.
    @Composable
    fun Greeting(name: String) {
        Text(text = "Hola $name!")
    }

    // Composable function to display a button that requests permissions when clicked.
    @Composable
    fun RequestPermissionButton(context: Context, text: String) {
        Button(onClick = { requestPermissions(context) }) {
            Text(text)
        }
    }

    // Composable function to list Wi-Fi networks.
    @Composable
    fun WifiNetworksList() {
        LazyColumn {
            items(wifiResults) { wifi ->
                Text("SSID: ${wifi.SSID}, MAC: ${wifi.BSSID}, RSSI: ${wifi.level}")
            }
        }
    }

    // Helper function to decide if the app should show rationale for requested permissions.
    private fun shouldShowRequestPermissionRationale(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    // Function to request location permissions.
    private fun requestPermissions(context: Context) {
        locationPermissionRequest.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        )
    }

    // Function to start scanning for Wi-Fi networks.
    private fun startWifiScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            wifiManager.startScan()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to safely access Wi-Fi scan results and handle permissions.
    private fun safeAccessWifiScanResults(context: Context) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                wifiResults.clear()
                wifiResults.addAll(wifiManager.scanResults)
                unregisterReceiver(wifiScanReceiver)  // Unregister receiver after receiving scan results
            } else {
                Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Fallo en el scan de redes debido al permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }
}
