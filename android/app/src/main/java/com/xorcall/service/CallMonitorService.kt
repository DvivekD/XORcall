package com.xorcall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.xorcall.engine.*
import com.xorcall.network.TelemetryListener
import com.xorcall.network.TelemetryWebSocketClient
import com.xorcall.ui.FloatingAlertOverlay
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * CallMonitorService
 * Autonomous Foreground Guardian Service with:
 * - Multi-Demographic Lexicon Interception (Student, Senior Citizen, Working Professional)
 * - Acoustic Neural Vocoder & AI Synthetic Voice Clone Spotter
 * - Proximity-aware sensory Taptic Haptics shockwave
 * - Persistent BSA Section 63 forensic logging
 */
class CallMonitorService : Service(), TelemetryListener {

    private val CHANNEL_ID = "XORcall_Guardian_Channel"
    private val NOTIFICATION_ID = 1001

    private lateinit var hapticEngine: TapticHapticEngine
    private lateinit var sttManager: SherpaSTTManager
    private lateinit var floatingOverlay: FloatingAlertOverlay
    private lateinit var packageWatchdog: PackageWatchdog
    private lateinit var callHistoryManager: CallHistoryManager
    private var webSocketClient: TelemetryWebSocketClient? = null

    private var currentCallerNumber: String = "Unknown Number"
    private var currentCallerClassification: String = "UNKNOWN"
    private var activeDemographicVector: DemographicVector = DemographicVector.ALL

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

    override fun onCreate() {
        super.onCreate()
        try {
            hapticEngine = TapticHapticEngine(this)
            hapticEngine.startListening()

            callHistoryManager = CallHistoryManager(this)

            sttManager = SherpaSTTManager(assets, this)
            sttManager.initialize()
            sttManager.activeScope = activeDemographicVector

            sttManager.onThreatDetected = { analysis, transcript ->
                // 1. Hardware Sensory Shockwave
                hapticEngine.triggerImmediateShockwave(analysis.riskLevel)

                // 2. High-Priority Heads-Up & Floating Alert
                scope.launch(Dispatchers.Main) {
                    floatingOverlay.showCriticalAlert(analysis.warningMessage)
                }

                // 3. Log to BSA Sec 63 Forensic Vault
                callHistoryManager.recordCall(
                    callerNumber = currentCallerNumber,
                    callerClassification = currentCallerClassification,
                    vector = analysis.demographicVector,
                    risk = analysis.riskLevel,
                    isSynthetic = analysis.isSyntheticVoice,
                    syntheticConfidence = analysis.syntheticConfidence,
                    keywords = analysis.matchedKeywords,
                    transcript = transcript
                )

                // 4. System Broadcast
                val intent = Intent("com.xorcall.THREAT_ALERT").apply {
                    putExtra("level", analysis.riskLevel.name)
                    putExtra("message", analysis.warningMessage)
                    putExtra("transcript", transcript)
                    putExtra("vector", analysis.demographicVector.name)
                    putExtra("isSynthetic", analysis.isSyntheticVoice)
                    putExtra("syntheticConfidence", analysis.syntheticConfidence)
                    putExtra("keywords", analysis.matchedKeywords.toTypedArray())
                    setPackage(packageName)
                }
                sendBroadcast(intent)
            }

            sttManager.onAcousticResult = { result ->
                if (result.isSyntheticVoice) {
                    val aiIntent = Intent("com.xorcall.AI_VOICE_ALERT").apply {
                        putExtra("confidence", result.syntheticConfidence)
                        putExtra("reason", result.analysisReason)
                        setPackage(packageName)
                    }
                    sendBroadcast(aiIntent)
                }
            }

            floatingOverlay = FloatingAlertOverlay(this)
            packageWatchdog = PackageWatchdog(this)

            webSocketClient = TelemetryWebSocketClient("ws://10.0.2.2:8000", this)
            try {
                webSocketClient?.connect("ANDROID-SESSION-${System.currentTimeMillis()}")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            createNotificationChannel()
            val notification = buildForegroundNotification("Kavach Shield Active - Intercepting Extortion Scripts")
            
            val hasMicPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (hasMicPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (hasMicPermission) {
                startAudioCapture()
            }
            startWatchdogLoop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("EXTRA_INCOMING_NUMBER")?.let {
            currentCallerNumber = it
        }
        intent?.getStringExtra("EXTRA_CLASSIFICATION")?.let {
            currentCallerClassification = it
        }
        intent?.getStringExtra("EXTRA_VECTOR_SCOPE")?.let { scopeName ->
            try {
                activeDemographicVector = DemographicVector.valueOf(scopeName)
                sttManager.activeScope = activeDemographicVector
            } catch (_: Exception) {}
        }

        val testText = intent?.getStringExtra("EXTRA_TEST_TEXT")
        if (!testText.isNullOrBlank()) {
            val analysis = sttManager.analyzeText(testText, activeDemographicVector)
            if (analysis.isScamDetected) {
                sttManager.onThreatDetected?.invoke(analysis, testText)
            }
        }

        return START_STICKY
    }

    private fun startAudioCapture() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMicPermission) return

        try {
            sttManager.startContinuousListening { text, isEndpoint ->
                if (text.isNotBlank()) {
                    val intent = Intent("com.xorcall.TRANSCRIPT_UPDATE").apply {
                        putExtra("text", text)
                        putExtra("isEndpoint", isEndpoint)
                        setPackage(packageName)
                    }
                    sendBroadcast(intent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startWatchdogLoop() {
        scope.launch {
            while (isActive) {
                delay(2000)
                try {
                    if (packageWatchdog.isDangerousAppInForeground()) {
                        withContext(Dispatchers.Main) {
                            floatingOverlay.showCriticalAlert(
                                "CRITICAL: Remote desktop tool detected during call! Blocked to protect bank accounts."
                            )
                        }
                    }
                } catch (e: Exception) {
                    // OEM security policy bypass
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
                floatingOverlay.showCriticalAlert(summary)
            }
        }
    }

    override fun onConnectionStateChanged(isConnected: Boolean) {
        // Handle WebSocket connection state
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
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
