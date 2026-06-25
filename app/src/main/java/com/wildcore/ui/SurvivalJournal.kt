package com.wildcore.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import com.wildcore.data.SurvivalSpot
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.GoogleMapOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.MapProperties
import java.util.Locale
import com.wildcore.BuildConfig

enum class WildCoreScreen(val title: String) {
    ADD_SPOT("🌲 Nowy Punkt GPS"),
    SAVED_SPOTS("🗂️ Dziennik Terenowy"),
    Tools("🧭 Narzędzia i Nawigacja"),
    WEATHER("⛅ Prognoza Pogody"),
    ALARM("🚨 Ustawienia SOS")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: SurvivalViewModel = koinViewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf(WildCoreScreen.ADD_SPOT) }

    // Obsługa stanu wysuwanego menu (Drawer) i korutyn do jego otwierania/zamykania
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // GŁÓWNY KONTENER: Komponent wysuwanego menu z automatycznym przyciemnianiem tła
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "🧭 Nawigacja WildCore",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))

                // Opcja 1: Dodaj punkt
                NavigationDrawerItem(
                    label = { Text("🌲 Dodaj nowy punkt") },
                    selected = currentScreen == WildCoreScreen.ADD_SPOT,
                    onClick = {
                        currentScreen = WildCoreScreen.ADD_SPOT
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Opcja 2: Zobacz zapisane punkty
                NavigationDrawerItem(
                    label = { Text("🗂️ Zobacz zapisane punkty") },
                    selected = currentScreen == WildCoreScreen.SAVED_SPOTS,
                    onClick = {
                        currentScreen = WildCoreScreen.SAVED_SPOTS
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Opcja 3: Narzędzia
                NavigationDrawerItem(
                    label = { Text("🧭 Narzędzia (Kompas)") },
                    selected = currentScreen == WildCoreScreen.Tools,
                    onClick = {
                        currentScreen = WildCoreScreen.Tools
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Opcja 4: Prognoza Pogody
                NavigationDrawerItem(
                    label = { Text("⛅ Prognoza Pogody") },
                    selected = currentScreen == WildCoreScreen.WEATHER,
                    onClick = {
                        currentScreen = WildCoreScreen.WEATHER
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Opcja 5: Ustawienia numeru alarmowego
                NavigationDrawerItem(
                    label = { Text("🚨 Ustawienia SOS") },
                    selected = currentScreen == WildCoreScreen.ALARM,
                    onClick = {
                        currentScreen = WildCoreScreen.ALARM
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )


            }
        }
    ) {
        // Tutaj znajduje się reszta aplikacji, która automatycznie się PRZYCIEMNI, gdy drawer się otworzy
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Otwórz menu"
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = currentScreen.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                when (currentScreen) {
                    WildCoreScreen.ADD_SPOT -> AddSpotForm(viewModel = viewModel)
                    WildCoreScreen.SAVED_SPOTS -> SavedSpotsList(viewModel = viewModel)
                    WildCoreScreen.Tools -> ToolsScreen(viewModel = viewModel)
                    WildCoreScreen.WEATHER -> WeatherScreen()
                    WildCoreScreen.ALARM -> AlarmSettingsScreen()
                }
            }
        }
    }
}

// --- PODSTRONA 1: EKRAN NARZĘDZI (CZYSTY KOMPAS) ---
@SuppressLint("MissingPermission")
@Composable
fun ToolsScreen(viewModel: SurvivalViewModel = koinViewModel()) { // ZMIANA: Przyjmujemy viewModel
    // NOWOŚĆ: Pobieranie zapisanych punktów w czasie rzeczywistym
    val spots by viewModel.survivalSpots.collectAsState()

    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    // (reszta istniejącego kodu czujników kompasu pozostaje bez zmian...)
    var azimuth by remember { mutableStateOf(0f) }
    val gravity = remember { FloatArray(3) }
    val geomagnetic = remember { FloatArray(3) }
    val cameraPositionState = rememberCameraPositionState()

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
                }
            }
        }
    }

    // (Blok DisposableEffect dla rejestracji sensorów bez zmian...)

    val directionText = when (azimuth) {
        in 337.5..360.0, in 0.0..22.5 -> "N (Północ)"
        in 22.5..67.5 -> "NE (Pn-Wsch)"
        in 67.5..112.5 -> "E (Wschód)"
        in 112.5..157.5 -> "SE (Pd-Wsch)"
        in 157.5..202.5 -> "S (Południe)"
        in 202.5..247.5 -> "SW (Pd-Zach)"
        in 247.5..292.5 -> "W (Zachód)"
        else -> "NW (Pn-Zach)"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tło: Mapa Google
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
        ) {
            // Pętla nanosząca wszystkie zapisane wcześniej pinezki
            spots.forEach { spot ->
                val emoji = spot.category.substringBefore(" ")

                com.google.maps.android.compose.MarkerComposable(
                    keys = arrayOf(spot.id),
                    state = rememberMarkerState(position = LatLng(spot.latitude, spot.longitude)),
                    title = spot.title,
                    snippet = spot.description
                ) {
                    Box(
                        modifier = Modifier // <-- Tutaj zmienione na czysty Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // Nakładka: Wyśrodkowany, czytelny kompas (karta UI)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = directionText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = String.format(Locale.US, "%.0f°", azimuth),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Igła kompasu",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(65.dp)
                                .rotate(-azimuth)
                        )
                    }
                }
            }
        }
    }
}


// --- PODSTRONA 2: PEŁNOEKRANOWY FORMULARZ ---
@SuppressLint("MissingPermission")
@Composable
fun AddSpotForm(viewModel: SurvivalViewModel) {
    val context = LocalContext.current // POTRZEBNE DO LOKALIZACJI

    val spots by viewModel.survivalSpots.collectAsState()

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("💧 Woda", "⛺ Schron", "🍓 Jedzenie", "⚠️ Zagrożenie")
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    // Domyślne wartości (Gdańsk) zostawiamy jako "fallback" na wypadek braku GPS
    var latitudeInput by remember { mutableStateOf("54.3520") }
    var longitudeInput by remember { mutableStateOf("18.6460") }

    // --- NOWY BLOK: Pobieranie aktualnej pozycji przy starcie ekranu ---
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    // Aktualizujemy inputy prawdziwą lokalizacją, co automatycznie przesunie mapę
                    latitudeInput = String.format(Locale.US, "%.5f", location.latitude)
                    longitudeInput = String.format(Locale.US, "%.5f", location.longitude)
                }
            }
        }
    }
    // ------------------------------------------------------------------

    val latDouble = latitudeInput.toDoubleOrNull()
    val lonDouble = longitudeInput.toDoubleOrNull()
    val isLatValid = latDouble != null && latDouble >= -90.0 && latDouble <= 90.0
    val isLonValid = lonDouble != null && lonDouble >= -180.0 && lonDouble <= 180.0
    val showLatError = latitudeInput.isNotEmpty() && !isLatValid
    val showLonError = longitudeInput.isNotEmpty() && !isLonValid
    val isFormValid = title.isNotBlank() && isLatValid && isLonValid

    // 1. Stan kamery i pozycja startowa mapy
    val defaultLatLng = LatLng(54.3520, 18.6460)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 12f)
    }

    // 2. Stan pozycji samej pinezki (Markera)
    val markerState = rememberMarkerState(position = defaultLatLng)

    // 3. Automatyczna synchronizacja: Klawiatura -> Mapa
    LaunchedEffect(latDouble, lonDouble) {
        if (isLatValid && isLonValid && latDouble != null && lonDouble != null) {
            val newTarget = LatLng(latDouble, lonDouble)
            markerState.position = newTarget
            cameraPositionState.position = CameraPosition.fromLatLngZoom(newTarget, cameraPositionState.position.zoom)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Nazwa punktu (np. Krzaki jagód)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Opis / Notatki") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Column {
            Text(text = "Kategoria:", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }

        // --- SEKCJA INTERAKTYWNEJ MAPY GOOGLE ---
        Text(text = "🗺️ Wybierz lokalizację na mapie (kliknij, aby postawić pinezkę):", style = MaterialTheme.typography.titleSmall)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                googleMapOptionsFactory = {
                    GoogleMapOptions().mapId(BuildConfig.MAP_ID)
                },
                onMapClick = { latLng ->
                    latitudeInput = String.format(Locale.US, "%.5f", latLng.latitude)
                    longitudeInput = String.format(Locale.US, "%.5f", latLng.longitude)
                }
            ) {
                // 1. Istniejący Marker aktywnie tworzonego/klikanego punktu
                Marker(
                    state = markerState,
                    title = if (title.isBlank()) "Nowy punkt krytyczny" else title
                )

                // 2. Wyświetlanie pozostałych punktów z unikalnymi emotkami w tle
                spots.forEach { spot ->
                    val emoji = spot.category.substringBefore(" ")

                    com.google.maps.android.compose.MarkerComposable(
                        keys = arrayOf(spot.id),
                        state = rememberMarkerState(position = LatLng(spot.latitude, spot.longitude)),
                        title = spot.title,
                        snippet = spot.description
                    ) {
                        Box(
                            modifier = Modifier // <-- Tutaj zmienione na czysty Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Text(text = "📍 Współrzędne geograficzne (DD):", style = MaterialTheme.typography.titleSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = latitudeInput,
                onValueChange = { latitudeInput = it },
                label = { Text("Szerokość (Lat)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = showLatError,
                supportingText = { if (showLatError) Text("Wpisz od -90 do 90") },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = longitudeInput,
                onValueChange = { longitudeInput = it },
                label = { Text("Długość (Lon)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = showLonError,
                supportingText = { if (showLonError) Text("Wpisz od -180 do 180") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isFormValid) {
                    viewModel.addSpot(
                        title = title,
                        description = description,
                        lat = latDouble ?: 0.0,
                        lon = lonDouble ?: 0.0,
                        category = selectedCategory
                    )
                    title = ""
                    description = ""
                }
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zapisz punkt w dzienniku")
        }
    }
}

// --- PODSTRONA 3: PEŁNOEKRANOWA LISTA ---
@Composable
fun SavedSpotsList(viewModel: SurvivalViewModel) {
    val spots by viewModel.survivalSpots.collectAsState()

    if (spots.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "📭 Brak zapisanych punktów w dzienniku.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(spots) { spot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${spot.title} ${spot.category}", style = MaterialTheme.typography.titleMedium)
                            if (spot.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = spot.description, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Współrzędne: ${spot.latitude}, ${spot.longitude}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.removeSpot(spot) }) {
                            Text("❌")
                        }
                    }
                }
            }
        }
    }
}