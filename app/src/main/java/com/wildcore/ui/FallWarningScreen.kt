package com.wildcore.ui

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FallWarningScreen(
    countdown: Int,
    onCountdownChange: (Int) -> Unit,
    onCancel: () -> Unit,
    onSendNow: () -> Unit
) {
    val context = LocalContext.current

    // Przygotowanie Dźwięku i Wibracji
    val ringtone = remember {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        RingtoneManager.getRingtone(context, uri)
    }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Pętla odliczająca czas i zarządzająca dźwiękiem
    LaunchedEffect(Unit) {
        ringtone.play()
        val pattern = longArrayOf(0, 500, 500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }

        var currentCount = countdown
        while (currentCount > 0) {
            delay(1000L)
            currentCount--
            onCountdownChange(currentCount)
        }

        ringtone.stop()
        vibrator.cancel()
        onSendNow()
    }

    // Zatrzymanie dźwięków, gdy użytkownik zamknie ekran
    DisposableEffect(Unit) {
        onDispose {
            ringtone.stop()
            vibrator.cancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD32F2F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 56.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "WYKRYTO UPADEK",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Wiadomość SOS z Twoją lokalizacją zostanie wysłana za:",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "$countdown",
                color = Color.White,
                fontSize = 96.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFD32F2F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text("NIC MI NIE JEST - ANULUJ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSendNow,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(2.dp, Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("WYŚLIJ SOS NATYCHMIAST")
            }
        }
    }
}