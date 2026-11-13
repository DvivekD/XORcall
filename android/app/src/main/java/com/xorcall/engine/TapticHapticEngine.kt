package com.xorcall.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class RiskLevel {
    SAFE, WATCH, WARN, CRITICAL
}

/**
 * TapticHapticEngine
 * Evaluates hardware proximity sensor state. When the phone is pressed against
 * the user's ear and the display is off, visual UI warnings are completely invisible.
 * This engine vibrates escalating sensory patterns directly into the cheek and hand.
 */
class TapticHapticEngine(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    
    private var isNearEar: Boolean = false
    private var currentLevel: RiskLevel = RiskLevel.SAFE

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun startListening() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        vibrator.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = proximitySensor?.maximumRange ?: 5.0f
            isNearEar = distance < maxRange
            
            // Re-trigger haptics if near ear and risk is elevated
            if (isNearEar && currentLevel != RiskLevel.SAFE) {
                applyVibrationPattern(currentLevel)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun updateRiskLevel(level: RiskLevel) {
        this.currentLevel = level
        applyVibrationPattern(level)
    }

    private fun applyVibrationPattern(level: RiskLevel) {
        if (!vibrator.hasVibrator()) return

        when (level) {
            RiskLevel.SAFE -> vibrator.cancel()
            
            RiskLevel.WATCH -> {
                // Heartbeat: 2 subtle 80ms pulses
                val timings = longArrayOf(0, 80, 150, 80)
                val amplitudes = intArrayOf(0, 60, 0, 60)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            }

            RiskLevel.WARN -> {
                // Staccato SOS: 3 sharp buzzes with escalating amplitude
                val timings = longArrayOf(0, 180, 100, 180, 100, 220)
                val amplitudes = intArrayOf(0, 140, 0, 180, 0, 240)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            }

            RiskLevel.CRITICAL -> {
                // Violent erratic rhythm designed to shock user into pulling phone away from face
                val timings = longArrayOf(0, 350, 80, 350, 80, 500)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0) // Repeat until cancelled
                vibrator.vibrate(effect)
            }
        }
    }
}
