package com.wildcore

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.wildcore.ui.JournalScreen
import com.wildcore.ui.FallWarningScreen
import com.wildcore.ui.theme.WildCoreTheme
import com.wildcore.ui.triggerEmergencyAlarm
import com.wildcore.utils.FallDetector

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WildCoreTheme {
                var isFallWarningVisible by remember { mutableStateOf(false) }
                var countdown by remember { mutableStateOf(15) }

                val context = this

                // Inicjalizacja detektora upadku
                val fallDetector = remember {
                    FallDetector(context) {
                        countdown = 15
                        isFallWarningVisible = true
                    }
                }

                // Cykl życia detektora powiązany z Activity
                DisposableEffect(Unit) {
                    fallDetector.start()
                    onDispose { fallDetector.stop() }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                        // Główna aplikacja zawsze pod spodem
                        JournalScreen()

                        // Nakładka ostrzegawcza
                        if (isFallWarningVisible) {
                            FallWarningScreen(
                                countdown = countdown,
                                onCountdownChange = { countdown = it },
                                onCancel = {
                                    isFallWarningVisible = false
                                    fallDetector.reset()
                                },
                                onSendNow = {
                                    isFallWarningVisible = false
                                    fallDetector.reset()

                                    // Pobieramy numer i odpalamy bezpośrednio istniejącą funkcję globalną
                                    val sharedPreferences = context.getSharedPreferences("WildCorePrefs", Context.MODE_PRIVATE)
                                    val phoneNumber = sharedPreferences.getString("emergency_number", "") ?: ""

                                    triggerEmergencyAlarm(context, phoneNumber)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}