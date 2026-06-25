package com.wildcore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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


enum class WildCoreScreen(val title: String) {
    ADD_SPOT("🌲 Nowy Punkt GPS"),
    SAVED_SPOTS("🗂️ Dziennik Terenowy"),
    ALARM("🚨 Ustawienia SOS")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: SurvivalViewModel = koinViewModel()
) {
    var currentScreen by remember { mutableStateOf(WildCoreScreen.ADD_SPOT) }

    // Obsługa stanu wysuwanego menu (Drawer) i korutyn do jego otwierania/zamykania
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // GŁÓWNY KONTENER: Komponent wysuwanego menu z automatycznym przyciemnianiem tła
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Wygląd wnętrza naszego pięknego menu
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp) // Szerokość wysuwanego paska
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Nagłówek w menu
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
                        // Zamykamy menu z powrotem w korutynie
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

                // Opcja 3: Ustawienia numeru alarmowego
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
                // Używamy zwykłego Surface, aby mieć 100% kontroli nad rozmiarem
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp) // <--- TUTAJ ustawiasz idealną, małą wysokość paska!
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically // Środkujemy elementy w pionie
                    ) {
                        // Nasz hamburger
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Otwórz menu"
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tytuł podstrony (czcionka titleMedium świetnie pasuje do małego paska)
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
                    WildCoreScreen.ALARM -> AlarmSettingsScreen()
                }
            }
        }
    }
}

// --- PODSTRONA 1: PEŁNOEKRANOWY FORMULARZ (Bez zmian) ---
@Composable
fun AddSpotForm(viewModel: SurvivalViewModel) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("💧 Woda", "⛺ Schron", "🍓 Jedzenie", "⚠️ Zagrożenie")
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    // Początkowe współrzędne (np. Gdańsk)
    var latitudeInput by remember { mutableStateOf("54.3520") }
    var longitudeInput by remember { mutableStateOf("18.6460") }

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
    // Jeśli użytkownik wpisze poprawny punkt ręcznie, przesuwamy tam mapę i pinezkę
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
                .height(220.dp), // Zgrabna wysokość, żeby zmieścić resztę pól na ekranie
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,

                // 🔥 PRAWIDŁOWE MIEJSCE NA ID MAPY W COMPOSE:
                googleMapOptionsFactory = {
                    GoogleMapOptions().mapId("TU DODAC ID MAPY")
                },

                onMapClick = { latLng ->
                    // Automatyczne uzupełnianie pól po kliknięciu
                    latitudeInput = String.format(Locale.US, "%.5f", latLng.latitude)
                    longitudeInput = String.format(Locale.US, "%.5f", latLng.longitude)
                }
            ) {
                // Wyświetlamy naszą survivalową pinezkę
                Marker(
                    state = markerState,
                    title = if (title.isBlank()) "Nowy punkt krytyczny" else title
                )
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

// --- PODSTRONA 2: PEŁNOEKRANOWA LISTA (Bez zmian) ---
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