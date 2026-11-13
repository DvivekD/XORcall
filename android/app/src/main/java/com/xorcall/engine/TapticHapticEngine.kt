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
 * Evaluates hardware proximity sensor state and fires escalating sensory patterns.
 * Direct sensory vibration alerts the user even when the display is held to the cheek.
 */
class TapticHapticEngine(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    
    private var isNearEar: Boolean = false
    private var currentLevel: RiskLevel = RiskLevel.SAFE

    // Enabled by default so vibration alerts work immediately
    var isHapticsEnabled: Boolean = true
    var isAudioWhisperEnabled: Boolean = true
    var isSilentFamilyPingEnabled: Boolean = true
    var isHeadsUpBannerEnabled: Boolean = true

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun startListening() {
        try {
            proximitySensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(this)
            vibrator.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    /**
     * Forcefully triggers an immediate physical shockwave buzz to pull user's attention.
     */
    fun triggerImmediateShockwave(level: RiskLevel = RiskLevel.CRITICAL) {
        if (!isHapticsEnabled) return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = when (level) {
                    RiskLevel.CRITICAL -> longArrayOf(0, 250, 100, 250, 100, 450)
                    RiskLevel.WARN -> longArrayOf(0, 180, 100, 180)
                    RiskLevel.WATCH -> longArrayOf(0, 100, 100, 100)
                    RiskLevel.SAFE -> longArrayOf(0, 50)
                }
                val effect = if (vibrator.hasAmplitudeControl()) {
                    val amplitudes = when (level) {
                        RiskLevel.CRITICAL -> intArrayOf(0, 255, 0, 255, 0, 255)
                        RiskLevel.WARN -> intArrayOf(0, 180, 0, 180)
                        RiskLevel.WATCH -> intArrayOf(0, 100, 0, 100)
                        RiskLevel.SAFE -> intArrayOf(0, 60)
                    }
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 250, 100, 250, 100, 450)
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyVibrationPattern(level: RiskLevel) {
        if (!isHapticsEnabled) {
            try { vibrator.cancel() } catch (_: Exception) {}
            return
        }
        triggerImmediateShockwave(level)
    }
}
