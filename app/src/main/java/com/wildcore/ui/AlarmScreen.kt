package com.wildcore.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.SmsManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

@Composable
fun AlarmSettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("WildCorePrefs", Context.MODE_PRIVATE)

    // Pobranie zapisanego wcześniej numeru (lub puste pole)
    var phoneNumber by remember {
        mutableStateOf(sharedPreferences.getString("emergency_number", "") ?: "")
    }
    var isSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Konfiguracja Alarmu SOS", style = MaterialTheme.typography.titleLarge)
        Text(text = "Podaj numer telefonu, na który zostanie wysłany SMS z Twoją lokalizacją w razie niebezpieczeństwa.", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                isSaved = false
            },
            label = { Text("Numer ratunkowy") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                sharedPreferences.edit().putString("emergency_number", phoneNumber).apply()
                isSaved = true
                Toast.makeText(context, "Numer został zapisany!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaved) "Zapisano numer" else "Zapisz numer")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Przycisk tymczasowy - do testowania wywoływania funkcji
        Button(
            onClick = {
                triggerEmergencyAlarm(context, phoneNumber)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚨 WYŚLIJ TESTOWY ALARM SOS 🚨")
        }
    }
}

// Główna funkcja alarmu
fun triggerEmergencyAlarm(context: Context, phoneNumber: String) {
    if (phoneNumber.isBlank()) {
        Toast.makeText(context, "Brak numeru! Skonfiguruj alarm SOS.", Toast.LENGTH_SHORT).show()
        return
    }

    val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    if (!hasSmsPermission || !hasLocationPermission) {
        Toast.makeText(context, "Brak uprawnień do SMS lub Lokalizacji!", Toast.LENGTH_LONG).show()
        return
    }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val message = if (location != null) {
                "ALARM SOS! Potrzebuję pomocy. Moja lokalizacja: http://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                "ALARM SOS! Potrzebuję pomocy. (Brak dokładnej lokalizacji GPS)."
            }

            sendSms(context, phoneNumber, message)
        }.addOnFailureListener {
            sendSms(context, phoneNumber, "ALARM SOS! Potrzebuję pomocy. (Błąd pobierania lokalizacji).")
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}

private fun sendSms(context: Context, phoneNumber: String, message: String) {
    try {
        val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)

        // Zabezpieczenie na wypadek braku numeru kierunkowego (opcjonalne, ale przydatne w Polsce)
        val formattedNumber = if (!phoneNumber.startsWith("+")) "+48$phoneNumber" else phoneNumber

        // Dzielimy wiadomość na części (jeśli przekracza limit znaków)
        val parts = smsManager.divideMessage(message)

        if (parts.size > 1) {
            // Wysłanie długiej, wieloczęściowej wiadomości
            smsManager.sendMultipartTextMessage(formattedNumber, null, parts, null, null)
        } else {
            // Wysłanie standardowej, krótkiej wiadomości
            smsManager.sendTextMessage(formattedNumber, null, message, null, null)
        }

        Toast.makeText(context, "Sygnał SOS przekazany do wysłania!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Krytyczny błąd! Nie udało się wysłać SOS.", Toast.LENGTH_LONG).show()
    }
}