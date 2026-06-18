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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wildcore.data.SurvivalSpot
import org.koin.androidx.compose.koinViewModel

@Composable
fun JournalScreen(
    viewModel: SurvivalViewModel = koinViewModel()
) {
    val spots by viewModel.survivalSpots.collectAsState()

    // Stany formularza
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("💧 Woda", "⛺ Schron", "🍓 Jedzenie", "⚠️ Zagrożenie")
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    // Ręczne wprowadzanie współrzędnych (jako String, żeby łatwo się pisało)
    var latitudeInput by remember { mutableStateOf("54.3520") }
    var longitudeInput by remember { mutableStateOf("18.6460") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🌲 Panel Nawigacji WildCore",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Górna sekcja: Formularz wprowadzania danych
        Column(
            modifier = Modifier
                .weight(1.3f)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nazwa punktu (np. Krzaki jagód)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis / Notatki") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Kategoria:", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "📍 Współrzędne geograficzne (DD):",
                style = MaterialTheme.typography.titleSmall
            )

            // Dwa pola obok siebie na Szerokość (Lat) i Długość (Lon) geograficzną
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = latitudeInput,
                    onValueChange = { latitudeInput = it },
                    label = { Text("Szerokość (Lat)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = longitudeInput,
                    onValueChange = { longitudeInput = it },
                    label = { Text("Długość (Lon)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val lat = latitudeInput.toDoubleOrNull() ?: 0.0
                    val lon = longitudeInput.toDoubleOrNull() ?: 0.0

                    if (title.isNotBlank()) {
                        viewModel.addSpot(
                            title = title,
                            description = description,
                            lat = lat,
                            lon = lon,
                            category = selectedCategory
                        )
                        // Czyszczenie pól (oprócz współrzędnych, żeby łatwiej dodawać punkty obok siebie)
                        title = ""
                        description = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Zapisz punkt w dzienniku")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "🗂️ Zapisane punkty w terenie:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Dolna sekcja: Lista punktów z bazy danych
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(spots) { spot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${spot.title} ${spot.category}", style = MaterialTheme.typography.titleMedium)
                            if (spot.description.isNotEmpty()) {
                                Text(text = spot.description, style = MaterialTheme.typography.bodyMedium)
                            }
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