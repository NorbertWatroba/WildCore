package com.wildcore.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Rozbudowany model danych o parametry survivalowe
data class DailyWeather(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val weatherCode: Int,
    val windSpeedMax: Double, // Maksymalna prędkość wiatru (km/h)
    val rainSum: Double       // Suma opadów (mm)
)

@SuppressLint("MissingPermission")
@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    var weatherData by remember { mutableStateOf<List<DailyWeather>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            errorMessage = "Brak uprawnień do lokalizacji. Nadaj je w ustawieniach telefonu."
            isLoading = false
            return@LaunchedEffect
        }

        val amplifiedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        amplifiedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                fetchWeather(location.latitude, location.longitude) { result, error ->
                    weatherData = result
                    errorMessage = error
                    isLoading = false
                }
            } else {
                errorMessage = "Nie udało się ustalić obecnej lokalizacji. Upewnij się, że GPS jest włączony."
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
        Text(text = "Prognoza pogody dla Ciebie", style = MaterialTheme.typography.titleLarge)
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
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            // Płynna animacja rozwijania kafelka (teraz obsłuży też pojawienie się dodatkowego tekstu!)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Główny wiersz (zawsze widoczny)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sekcja Tekstowa po lewej
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(text = weather.date, fontWeight = FontWeight.Bold)
                    Text(
                        text = getWeatherDescription(weather.weatherCode),
                        style = MaterialTheme.typography.bodyMedium,
                        // DYNAMICZNA ZMIANA: 1 linia gdy zamknięte, brak limitu gdy otwarte
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
                    )
                }

                // Sekcja Temperatur po prawej
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "${weather.maxTemp}°C", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        Text(text = "${weather.minTemp}°C", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Zwiń" else "Rozwiń"
                    )
                }
            }

            // Sekcja dodatkowa (widoczna po kliknięciu)
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "💨 Wiatr (max):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(text = "${weather.windSpeedMax} km/h", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "💧 Opad dobowy:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(text = "${weather.rainSum} mm", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun fetchWeather(lat: Double, lon: Double, onResult: (List<DailyWeather>?, String?) -> Unit) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            // Dodaliśmy parametry windspeed_10m_max oraz rain_sum do zapytania API
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&daily=weathercode,temperature_2m_max,temperature_2m_min,windspeed_10m_max,rain_sum&timezone=auto"
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
                val winds = daily.getJSONArray("windspeed_10m_max")
                val rains = daily.getJSONArray("rain_sum")

                val weatherList = mutableListOf<DailyWeather>()
                for (i in 0 until times.length()) {
                    weatherList.add(
                        DailyWeather(
                            date = times.getString(i),
                            maxTemp = maxTemps.getDouble(i),
                            minTemp = minTemps.getDouble(i),
                            weatherCode = codes.getInt(i),
                            windSpeedMax = winds.getDouble(i),
                            rainSum = rains.getDouble(i)
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    onResult(weatherList, null)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(null, "Błąd serwera: ${connection.responseCode}")
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

private fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "☀️ Czyste niebo"
        1, 2, 3 -> "⛅ Przeważnie słonecznie / Pochmurno"
        45, 48 -> "🌫️ Mgła leśna"
        51, 53, 55 -> "🌧️ Lekka mżawka"
        56, 57 -> "🥶 Zamarzająca mżawka"
        61, 63, 65 -> "🌧️ Opady deszczu"
        66, 67 -> "🧊 Marznący deszcz"
        71, 73, 75 -> "❄️ Opady śniegu"
        77 -> "🌨️ Ziarna śniegu"
        80, 81, 82 -> "🌦️ Przelotne opady"
        85, 86 -> "🌨️ Przelotne opady śniegu"
        95 -> "⛈️ Burza z piorunami"
        96, 99 -> "⛈️ Burza z gradem"
        else -> "❓ Nieznana pogoda"
    }
}