package com.xorcall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.xorcall.engine.RiskLevel
import com.xorcall.engine.SherpaSTTManager
import com.xorcall.engine.TapticHapticEngine
import com.xorcall.network.TelemetryListener
import com.xorcall.network.TelemetryWebSocketClient
import com.xorcall.ui.FloatingAlertOverlay
import kotlinx.coroutines.*
import org.json.JSONObject

class CallMonitorService : Service(), TelemetryListener {

    private val CHANNEL_ID = "XORcall_Guardian_Channel"
    private val NOTIFICATION_ID = 1001

    private lateinit var hapticEngine: TapticHapticEngine
    private lateinit var sttManager: SherpaSTTManager
    private lateinit var floatingOverlay: FloatingAlertOverlay
    private lateinit var packageWatchdog: PackageWatchdog
    private var webSocketClient: TelemetryWebSocketClient? = null

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    override fun onCreate() {
        super.onCreate()
        hapticEngine = TapticHapticEngine(this)
        hapticEngine.startListening()

        sttManager = SherpaSTTManager(assets)
        sttManager.initialize()

        floatingOverlay = FloatingAlertOverlay(this)
        packageWatchdog = PackageWatchdog(this)

        webSocketClient = TelemetryWebSocketClient("ws://10.0.2.2:8000", this)
        webSocketClient?.connect("ANDROID-SESSION-${System.currentTimeMillis()}")

        createNotificationChannel()
        val notification = buildForegroundNotification("Kavach Shield Active - Monitoring In-Call Audio")
        startForeground(NOTIFICATION_ID, notification)

        startAudioCapture()
        startWatchdogLoop()
    }

    private fun startAudioCapture() {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            audioRecord?.startRecording()
            isRecording = true

            scope.launch {
                val shortBuffer = ShortArray(1600) // 100ms at 16kHz
                val floatBuffer = FloatArray(1600)

                while (isActive && isRecording) {
                    val readCount = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (readCount > 0) {
                        for (i in 0 until readCount) {
                            floatBuffer[i] = shortBuffer[i] / 32768.0f
                        }
                        sttManager.acceptAudioChunk(floatBuffer) { text, isEndpoint ->
                            if (text.isNotBlank() && isEndpoint) {
                                webSocketClient?.sendUtterance(text)
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun startWatchdogLoop() {
        scope.launch {
            while (isActive) {
                delay(1000)
                if (packageWatchdog.isDangerousAppInForeground()) {
                    withContext(Dispatchers.Main) {
                        floatingOverlay.showCriticalAlert(
                            "CRITICAL: Remote desktop tool detected during call! Blocked to protect bank accounts."
                        ) {
                            // On dismiss
                        }
                    }
                }
            }
        }
    }

    override fun onTelemetryReceived(json: JSONObject) {
        val levelStr = json.optString("level", "SAFE")
        val level = when (levelStr) {
            "CRITICAL" -> RiskLevel.CRITICAL
            "WARN" -> RiskLevel.WARN
            "WATCH" -> RiskLevel.WATCH
            else -> RiskLevel.SAFE
        }

        hapticEngine.updateRiskLevel(level)

        if (level == RiskLevel.CRITICAL) {
            val summary = json.optString("vernacular_summary", "No police agency arrests anyone by video call. Hang up.")
            scope.launch(Dispatchers.Main) {
                floatingOverlay.showCriticalAlert(summary) {
                    // Hangup / Emergency connect
                }
            }
        }
    }

    override fun onConnectionStateChanged(isConnected: Boolean) {
        // Handle WebSocket connection state
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        sttManager.release()
        hapticEngine.stopListening()
        floatingOverlay.dismiss()
        webSocketClient?.disconnect()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "XORcall In-Call Protection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("XORcall Cyber Safety")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }
}
