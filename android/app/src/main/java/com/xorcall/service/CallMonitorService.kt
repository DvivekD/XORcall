package com.xorcall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
 * - Multi-Demographic Lexicon Interception (Universal, Student, Senior Citizen, Working Professional)
 * - Acoustic Neural Vocoder & AI Synthetic Voice Clone Spotter
 * - Proximity-aware sensory Taptic Haptics shockwave
 * - Out-of-band In-Ear Audio Whisper Alert (TextToSpeech)
 * - Silent Guardian Ping circuit to family/guardians
 * - Persistent BSA Section 63 forensic logging
 */
class CallMonitorService : Service(), TelemetryListener {

    private val CHANNEL_ID = "XORcall_Guardian_Channel"
    private val NOTIFICATION_ID = 1001

    private lateinit var settingsManager: GuardianSettingsManager
    private lateinit var hapticEngine: TapticHapticEngine
    private lateinit var whisperEngine: AudioWhisperEngine
    private lateinit var guardianPingManager: GuardianPingManager
    private lateinit var sttManager: SherpaSTTManager
    private lateinit var floatingOverlay: FloatingAlertOverlay
    private lateinit var packageWatchdog: PackageWatchdog
    private lateinit var callHistoryManager: CallHistoryManager
    private var webSocketClient: TelemetryWebSocketClient? = null

    private var currentCallerNumber: String = "Unknown Caller"
    private var currentCallerClassification: String = "UNKNOWN"

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == GuardianSettingsManager.ACTION_SETTINGS_CHANGED) {
                reloadSettings()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            settingsManager = GuardianSettingsManager(this)
            hapticEngine = TapticHapticEngine(this).apply {
                isHapticsEnabled = settingsManager.isHapticsEnabled
                startListening()
            }
            whisperEngine = AudioWhisperEngine(this)
            guardianPingManager = GuardianPingManager(this)
            callHistoryManager = CallHistoryManager(this)

            sttManager = SherpaSTTManager(assets, this)
            sttManager.initialize()
            sttManager.activeScope = settingsManager.activeVector

            sttManager.onThreatDetected = { analysis, transcript ->
                handleThreatDetected(analysis, transcript)
            }

            sttManager.onAcousticResult = { result ->
                if (result.isSyntheticVoice && settingsManager.isAiVoiceDetectionArmed) {
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

            // Register dynamic receiver for real-time settings changes from MainActivity
            val filter = IntentFilter(GuardianSettingsManager.ACTION_SETTINGS_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(settingsReceiver, filter)
            }

            createNotificationChannel()
            val notification = buildForegroundNotification("Kavach Shield Armed - Intercepting Extortion Scripts")

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

    private fun reloadSettings() {
        hapticEngine.isHapticsEnabled = settingsManager.isHapticsEnabled
        sttManager.activeScope = settingsManager.activeVector
    }

    private fun handleThreatDetected(analysis: ThreatAnalysis, transcript: String) {
        // 1. Hardware Sensory Shockwave
        if (settingsManager.isHapticsEnabled) {
            hapticEngine.triggerImmediateShockwave(analysis.riskLevel)
        }

        // 2. High-Priority Heads-Up & Floating Alert Banner
        if (settingsManager.isBannerEnabled) {
            scope.launch(Dispatchers.Main) {
                floatingOverlay.showCriticalAlert(analysis.warningMessage)
            }
        }

        // 3. Discreet In-Ear Audio Whisper
        if (settingsManager.isAudioWhisperEnabled) {
            whisperEngine.speakWarning(analysis.warningMessage)
        }

        // 4. Silent Guardian Ping
        if (settingsManager.isFamilyPingEnabled) {
            guardianPingManager.sendSilentGuardianPing(
                callerNumber = currentCallerNumber,
                threatSummary = analysis.warningMessage,
                vector = analysis.demographicVector
            )
        }

        // 5. Log to BSA Sec 63 Forensic Vault
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

        // 6. Broadcast Threat Event to UI
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("EXTRA_INCOMING_NUMBER")?.let {
            currentCallerNumber = it
        }
        intent?.getStringExtra("EXTRA_CLASSIFICATION")?.let {
            currentCallerClassification = it
        }
        intent?.getStringExtra("EXTRA_VECTOR_SCOPE")?.let { scopeName ->
            try {
                val vector = DemographicVector.valueOf(scopeName)
                settingsManager.activeVector = vector
                sttManager.activeScope = vector
            } catch (_: Exception) {}
        }

        val testText = intent?.getStringExtra("EXTRA_TEST_TEXT")
        if (!testText.isNullOrBlank()) {
            val analysis = sttManager.analyzeText(testText, settingsManager.activeVector)
            if (analysis.isScamDetected) {
                handleThreatDetected(analysis, testText)
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

        // 1. Start Hardware PCM Ingestion Loop for Vocoder AI Voice Clone Spotting
        startHardwareAudioRecordLoop()

        // 2. Start Continuous Speech Recognizer for Spoken Script Transcription
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

    private fun startHardwareAudioRecordLoop() {
        if (isRecording) return

        scope.launch(Dispatchers.IO) {
            try {
                // Try VOICE_COMMUNICATION first, fallback to standard MIC
                val audioSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION
                } else {
                    MediaRecorder.AudioSource.MIC
                }

                audioRecord = try {
                    AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
                } catch (_: Exception) {
                    AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
                }

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
                }

                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()
                    isRecording = true

                    val pcmBuffer = ShortArray(bufferSize / 2)
                    while (isRecording && isActive) {
                        val readCount = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: 0
                        if (readCount > 0) {
                            val floatSamples = FloatArray(readCount) { pcmBuffer[it] / 32768.0f }
                            sttManager.acceptAudioChunk(floatSamples)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        if (level == RiskLevel.CRITICAL && settingsManager.isBannerEnabled) {
            val summary = json.optString("vernacular_summary", "No police agency arrests anyone by video call. Hang up.")
            scope.launch(Dispatchers.Main) {
                floatingOverlay.showCriticalAlert(summary)
            }
        }
    }

    override fun onConnectionStateChanged(isConnected: Boolean) {}

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        try {
            unregisterReceiver(settingsReceiver)
        } catch (_: Exception) {}

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        sttManager.release()
        whisperEngine.shutdown()
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
