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
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

// Para listas solamente
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items

import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp

// Para poder actualizar el contenido de la pantalla con cada acción
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import com.example.wifiucspapp.ui.theme.WiFiUCSPAppTheme

//
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileOutputStream
import java.io.IOException
//

class MainActivity : ComponentActivity() {

    // Gestor de Wi-Fi para interactuar con el sistema de Wi-Fi del dispositivo.
    private lateinit var wifiManager: WifiManager

    // Lista de resultados de escaneo de Wi-Fi, observable para la UI.
    private val wifiResults = mutableStateListOf<ScanResult>()

    // Estado del permiso de ubicación utilizado para actualizar la UI basado en el estado del permiso.
    private val isLocationPermissionGranted = mutableStateOf(false)

    // Maneja la solicitud de permisos de ubicación y la respuesta del usuario.
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionsResult(permissions)
    }

    // Receiver para capturar y manejar los resultados del escaneo de Wi-Fi.
    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                handleScanResults(context)
            }
        }
    }

    // Registrar el BroadcastReceiver aquí asegura que sólo se activará cuando la actividad esté en primer plano.
    override fun onResume() {
        super.onResume()
        registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    // Desregistrar el BroadcastReceiver aquí evita fugas de memoria.
    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(wifiScanReceiver)
        } catch (e: IllegalArgumentException) {
            // Esto puede ocurrir si el BroadcastReceiver no estaba registrado.
            Log.e("MainActivity", "Error al desregistrar el receiver", e)
        }
    }

    // Inicialización y configuración de la actividad.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        // Actualiza el estado del permiso de ubicación al iniciar la actividad.
        // Iniciar con el texto y botón correcto
        updatePermissionState()

        // Establece el contenido de la actividad utilizando Jetpack Compose.
        setContent {
            WiFiUCSPAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(Modifier.padding(innerPadding))
                }
            }
        }
    }

    // Composable para definir el contenido principal de la UI.
    @Composable
    fun MainContent(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        Column(modifier = modifier) {
            Greeting("Diego")
            WiFiScanCondition(context)
            WifiNetworksList()
        }
    }



    // Composable que muestra el estado del permiso y un botón para solicitarlo.
    @Composable
    fun WiFiScanCondition(context: Context) {
        if (!isLocationPermissionGranted.value) {
            Text("Se requiere permiso de ubicación para acceder a las redes WiFi.")
            RequestPermissionButton(context, "Solicitar permiso")
        } else {
            Text("Permiso de ubicación otorgado.")
            WiFiScanButton("Escanear redes WiFi")
        }
    }

    // Composable para mostrar un saludo.
    @Composable
    fun Greeting(name: String) {
        Text(text = "Hola $name!")
    }

    // Composable para mostrar un botón que al hacer clic solicita permisos.
    @Composable
    fun RequestPermissionButton(context: Context, text: String) {
        Button(onClick = { requestPermissions(context) }) {
            Text(text)
        }
    }

    // Composable para mostrar un botón que al hacer click actualiza la lista de WiFi.
    @Composable
    fun WiFiScanButton(text: String) {
        Button(onClick = { startWifiScan() }) {
            Text(text)
        }
    }

    // Composable que lista las redes Wi-Fi encontradas.
    @Composable
    fun WifiNetworksList() {
        // Estilos para los encabezados y las celdas de la tabla
        val headerStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
        val cellStyle = TextStyle(fontSize = 14.sp)

        // Column que contiene toda la tabla
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Row para los encabezados de la tabla
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(text = "SSID", modifier = Modifier.weight(1f).padding(2.dp), style = headerStyle)
                Text(text = "MAC", modifier = Modifier.weight(1f).padding(2.dp), style = headerStyle)
                Text(text = "RSSI", modifier = Modifier.weight(1f).padding(2.dp), style = headerStyle)
            }
            // Rows para cada elemento de la lista de resultados
            wifiResults.forEach { wifi ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(text = wifi.SSID, modifier = Modifier.weight(1f).padding(2.dp), style = cellStyle)
                    Text(text = wifi.BSSID, modifier = Modifier.weight(1f).padding(2.dp), style = cellStyle)
                    Text(text = "${wifi.level}", modifier = Modifier.weight(1f).padding(2.dp), style = cellStyle)
                }
            }
        }
    }



    // Función para actualizar el estado del permiso basado en el estado actual del sistema
    private fun updatePermissionState() {
        isLocationPermissionGranted.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Maneja los resultados de la solicitud de permisos.
    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        isLocationPermissionGranted.value = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (isLocationPermissionGranted.value) {
            Toast.makeText(this, "Permiso de ubicación aceptado", Toast.LENGTH_SHORT).show()
            startWifiScan()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Solicita permisos de ubicación.
    private fun requestPermissions(context: Context) {
        locationPermissionRequest.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        )
    }

    // Inicia el escaneo de redes Wi-Fi.
    private fun startWifiScan() {
        if (isLocationPermissionGranted.value) {
            registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            wifiManager.startScan()

        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Maneja los resultados del escaneo de Wi-Fi.
    private fun handleScanResults(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            wifiResults.clear()
            wifiResults.addAll(wifiManager.scanResults)
            unregisterReceiver(wifiScanReceiver)  // Importante desregistrar para evitar fugas de memoria.
            writeWifiDataToExcel(context,wifiResults)
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

}

//
private fun writeWifiDataToExcel(context: Context, wifiResults: List<ScanResult>) {
    val workbook: Workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("WiFi Data")

    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("SSID")
    headerRow.createCell(1).setCellValue("BSSID")
    headerRow.createCell(2).setCellValue("Signal Level (RSSI)")

    wifiResults.forEachIndexed { index, scanResult ->
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(scanResult.SSID)
        row.createCell(1).setCellValue(scanResult.BSSID)
        row.createCell(2).setCellValue(scanResult.level.toString())
    }

    val fileName = "WiFiData.xlsx"
    val fileOutputStream: FileOutputStream
    try {
        fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
        workbook.write(fileOutputStream)
        fileOutputStream.close()
        Toast.makeText(context, "File saved as $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
    } finally {
        workbook.close()
    }
}
