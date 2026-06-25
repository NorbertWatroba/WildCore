package com.wildcore.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class FallDetector(
    context: Context,
    private val onFallDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isFreeFalling = false
    private var fallTimestamp = 0L
    private var isAlarmTriggered = false // Blokada przed wielokrotnym wysłaniem podczas jednego upadku

    fun start() {
        // SENSOR_DELAY_GAME daje nam odświeżanie około 50x na sekundę - idealne do złapania szybkiego uderzenia
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        isAlarmTriggered = false
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Obliczanie długości wektora przyspieszenia
            val acceleration = sqrt((x * x + y * y + z * z).toDouble())

            // FAZA 1: Swobodne spadanie (zbliżamy się do stanu nieważkości)
            if (acceleration < 3.0) {
                isFreeFalling = true
                fallTimestamp = System.currentTimeMillis()
            }

            // FAZA 2: Uderzenie (Nagły skok przeciążenia)
            if (isFreeFalling && acceleration > 25.0) {
                val timeDifference = System.currentTimeMillis() - fallTimestamp

                // Uderzenie musi nastąpić w ułamku sekundy po swobodnym spadaniu (pomiędzy 100ms a 1000ms)
                if (timeDifference in 100..1000 && !isAlarmTriggered) {
                    isAlarmTriggered = true
                    isFreeFalling = false
                    onFallDetected() // Uruchamiamy wysyłanie SMS!
                }
            }

            // FAZA 3: Reset flagi, jeśli minęła sekunda swobodnego spadania bez uderzenia (fałszywy alarm)
            if (isFreeFalling && System.currentTimeMillis() - fallTimestamp > 1000) {
                isFreeFalling = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}