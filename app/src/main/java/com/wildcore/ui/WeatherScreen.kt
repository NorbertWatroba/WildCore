package com.wildcore.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

// Model danych reprezentujący jeden dzień prognozy
data class DailyWeather(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val weatherCode: Int
)

@SuppressLint("MissingPermission")
@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    var weatherData by remember { mutableStateOf<List<DailyWeather>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pobieranie lokalizacji i pogody przy otwarciu ekranu
    LaunchedEffect(Unit) {
        val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            errorMessage = "Brak uprawnień do lokalizacji. Aby sprawdzić pogodę, włącz GPS i nadaj uprawnienia w ustawieniach."
            isLoading = false
            return@LaunchedEffect
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                // Mając współrzędne, odpytujemy API Open-Meteo
                fetchWeather(location.latitude, location.longitude) { result, error ->
                    weatherData = result
                    errorMessage = error
                    isLoading = false
                }
            } else {
                errorMessage = "Nie udało się pobrać lokalizacji. Upewnij się, że GPS jest włączony."
                isLoading = false
            }
        }.addOnFailureListener {
            errorMessage = "Błąd usług lokalizacyjnych."
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Prognoza na 7 dni", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            weatherData != null -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(weatherData!!) { daily ->
                        WeatherCard(daily)
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: DailyWeather) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = weather.date, fontWeight = FontWeight.Bold)
                Text(text = getWeatherDescription(weather.weatherCode), style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Max: ${weather.maxTemp}°C", color = MaterialTheme.colorScheme.error)
                Text(text = "Min: ${weather.minTemp}°C", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// Funkcja sieciowa pobierająca dane z Open-Meteo w tle (Dispatchers.IO)
private fun fetchWeather(lat: Double, lon: Double, onResult: (List<DailyWeather>?, String?) -> Unit) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&daily=weathercode,temperature_2m_max,temperature_2m_min&timezone=auto"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)
                val daily = jsonObject.getJSONObject("daily")

                val times = daily.getJSONArray("time")
                val maxTemps = daily.getJSONArray("temperature_2m_max")
                val minTemps = daily.getJSONArray("temperature_2m_min")
                val codes = daily.getJSONArray("weathercode")

                val weatherList = mutableListOf<DailyWeather>()
                for (i in 0 until times.length()) {
                    weatherList.add(
                        DailyWeather(
                            date = times.getString(i),
                            maxTemp = maxTemps.getDouble(i),
                            minTemp = minTemps.getDouble(i),
                            weatherCode = codes.getInt(i)
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    onResult(weatherList, null)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(null, "Błąd serwera pogody: ${connection.responseCode}")
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onResult(null, "Błąd połączenia. Sprawdź internet.")
            }
        }
    }
}

// Dekoder kodów WMO (Światowej Organizacji Meteorologicznej) na przyjazny tekst i emoji
private fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "☀️ Czyste niebo"
        1, 2, 3 -> "⛅ Przeważnie słonecznie / Pochmurno"
        45, 48 -> "🌫️ Mgła"
        51, 53, 55 -> "🌧️ Mżawka"
        56, 57 -> "🥶 Zamarzająca mżawka"
        61, 63, 65 -> "🌧️ Deszcz"
        66, 67 -> "🧊 Marznący deszcz"
        71, 73, 75 -> "❄️ Śnieg"
        77 -> "🌨️ Ziarna śniegu"
        80, 81, 82 -> "🌦️ Przelotne opady"
        85, 86 -> "🌨️ Przelotne opady śniegu"
        95 -> "⛈️ Burza"
        96, 99 -> "⛈️ Burza z gradem"
        else -> "❓ Nieznana pogoda"
    }
}