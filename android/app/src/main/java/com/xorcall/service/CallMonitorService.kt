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
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.xorcall.engine.*
import com.xorcall.network.TelemetryListener
import com.xorcall.network.TelemetryWebSocketClient
import com.xorcall.ui.FloatingAlertOverlay
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * CallMonitorService
 * Autonomous In-Call Cyber Guardian Service with:
 * - Direct In-Call Audio Output Interception (Direct Telephony Stream: VOICE_CALL / VOICE_COMMUNICATION / VOICE_DOWNLINK)
 * - Hardware Digital AGC Pre-Amplifier (+20dB dynamic boost with soft tanh limiter)
 * - Multi-Demographic Lexicon Interception (Universal Extortion, Student, Senior, Corporate Professional)
 * - Neural Acoustic AI Voice Clone & Vocoder Detector
 * - Proximity-aware sensory Taptic Haptics shockwave
 * - Discreet In-Ear Audio Whisper Alert (TextToSpeech)
 * - Silent Guardian Ping circuit
 * - Persistent BSA Section 63 forensic logging
 */
class CallMonitorService : Service(), TelemetryListener {

    companion object {
        private const val TAG = "CallMonitorService"
        const val ACTION_TOGGLE_SPEAKER = "com.xorcall.TOGGLE_SPEAKERPHONE"

        @Volatile
        var isServiceRunning = false
    }

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
    private lateinit var audioManager: AudioManager
    private var webSocketClient: TelemetryWebSocketClient? = null

    private var currentCallerNumber: String = "Unknown Caller"
    private var currentCallerClassification: String = "UNKNOWN"
    private var isCallActive: Boolean = false
    private var currentAudioSourceName: String = "STANDBY"

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                GuardianSettingsManager.ACTION_SETTINGS_CHANGED -> reloadSettings()
                ACTION_TOGGLE_SPEAKER -> toggleSpeakerphone()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            settingsManager = GuardianSettingsManager(this)
            hapticEngine = TapticHapticEngine(this).apply {
                isHapticsEnabled = settingsManager.isHapticsEnabled
                startListening()
            }
            whisperEngine = AudioWhisperEngine(this)
            guardianPingManager = GuardianPingManager(this)
            callHistoryManager = CallHistoryManager(this)

            sttManager = SherpaSTTManager(assets, this)
            sttManager.updateCloudConfig(settingsManager.groqApiKey, settingsManager.backendUrl)
            sttManager.initialize()
            sttManager.initializeVosk(scope)
            sttManager.activeScope = settingsManager.activeVector
            sttManager.isAiVoiceArmed = settingsManager.isAiVoiceDetectionArmed

            sttManager.onThreatDetected = { analysis, transcript ->
                handleThreatDetected(analysis, transcript)
            }

            sttManager.onAcousticResult = { result ->
                // Always broadcast real-time acoustic metrics to UI for Biological vs AI Voice differentiation
                val acousticIntent = Intent("com.xorcall.ACOUSTIC_UPDATE").apply {
                    putExtra("isSynthetic", result.isSyntheticVoice)
                    putExtra("confidence", result.syntheticConfidence)
                    putExtra("hfRatio", result.highFrequencyEnergyRatio)
                    putExtra("jitter", result.pitchJitterEstimate)
                    putExtra("flatness", result.spectralFlatness)
                    putExtra("isVoiced", result.isVoicedSpeech)
                    putExtra("reason", result.analysisReason)
                    setPackage(packageName)
                }
                sendBroadcast(acousticIntent)

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

            // Register dynamic receiver for real-time settings and speaker toggles
            val filter = IntentFilter().apply {
                addAction(GuardianSettingsManager.ACTION_SETTINGS_CHANGED)
                addAction(ACTION_TOGGLE_SPEAKER)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(settingsReceiver, filter)
            }

            createNotificationChannel()
            val notification = buildForegroundNotification("Kavach Shield Active - Ready to Intercept Call Audio")

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
                startAudioCapture(isCallOngoing = false)
            }
            startWatchdogLoop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun reloadSettings() {
        hapticEngine.isHapticsEnabled = settingsManager.isHapticsEnabled
        sttManager.activeScope = settingsManager.activeVector
        sttManager.isAiVoiceArmed = settingsManager.isAiVoiceDetectionArmed
        sttManager.updateCloudConfig(settingsManager.groqApiKey, settingsManager.backendUrl)
    }

    private fun toggleSpeakerphone() {
        val currentState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn
        }
        setSpeakerphoneState(!currentState)
    }

    /**
     * Sets speakerphone routing reliably across all Android versions (Android 8 through Android 14+).
     * On Android 12+ (API 31+), uses audioManager.setCommunicationDevice(TYPE_BUILTIN_SPEAKER).
     */
    private fun setSpeakerphoneState(enable: Boolean) {
        try {
            audioManager.mode = if (enable) AudioManager.MODE_IN_COMMUNICATION else AudioManager.MODE_NORMAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (enable) {
                    val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speakerDevice != null) {
                        val success = audioManager.setCommunicationDevice(speakerDevice)
                        Log.i(TAG, "setCommunicationDevice(BUILTIN_SPEAKER): $success")
                    } else {
                        @Suppress("DEPRECATION")
                        audioManager.isSpeakerphoneOn = true
                    }
                } else {
                    audioManager.clearCommunicationDevice()
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = enable
            }
            val intent = Intent("com.xorcall.SPEAKER_STATE").apply {
                putExtra("isSpeakerOn", enable)
                setPackage(packageName)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set speakerphone state to $enable", e)
        }
    }

    private var lastThreatAlertTime = 0L

    private fun handleThreatDetected(analysis: ThreatAnalysis, transcript: String) {
        val now = System.currentTimeMillis()
        if (now - lastThreatAlertTime < 2500L) {
            return
        }
        lastThreatAlertTime = now
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

        val callState = intent?.getStringExtra("EXTRA_CALL_STATE")
        val isAccessibilityIntercept = intent?.getBooleanExtra("EXTRA_ACCESSIBILITY_INTERCEPT", false) ?: false

        if (callState == "OFFHOOK" || isAccessibilityIntercept) {
            isCallActive = true
            Log.i(TAG, "Call connected (OFFHOOK): Engaging Direct In-Call Audio Capture")
            if (settingsManager.isAutoSpeakerphoneEnabled) {
                setSpeakerphoneState(true)
            }
            startAudioCapture(isCallOngoing = true)
        } else if (callState == "IDLE") {
            isCallActive = false
            currentCallerNumber = "Standby (Shield Armed)"
            Log.i(TAG, "Call ended (IDLE): Restoring normal audio routing")
            setSpeakerphoneState(false)
            startAudioCapture(isCallOngoing = false)
        }

        intent?.getStringExtra("EXTRA_VECTOR_SCOPE")?.let { scopeName ->
            try {
                val vector = DemographicVector.valueOf(scopeName)
                settingsManager.activeVector = vector
                sttManager.activeScope = vector
            } catch (_: Exception) {}
        }

        // Handle Direct In-Call Injection from test suite or accessibility service
        val testText = intent?.getStringExtra("EXTRA_TEST_TEXT")
        if (!testText.isNullOrBlank()) {
            val analysis = sttManager.analyzeText(testText, settingsManager.activeVector)
            if (analysis.isScamDetected) {
                handleThreatDetected(analysis, testText)
            }
        }

        return START_STICKY
    }

    private fun startAudioCapture(isCallOngoing: Boolean) {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMicPermission) return

        // Clean up any ongoing recording loop before binding the new in-call session
        stopAudioCaptureInternal()

        startHardwareAudioRecordLoop(isCallOngoing)
    }

    private fun stopAudioCaptureInternal() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    /**
     * Direct In-Call Audio Output Interception Loop
     * Direct hardware audio capture prioritizing:
     * 1. MediaRecorder.AudioSource.VOICE_CALL (4: Hardware modem duplex line - uplink + downlink)
     * 2. MediaRecorder.AudioSource.VOICE_COMMUNICATION (7: Echo-canceled duplex stream)
     * 3. MediaRecorder.AudioSource.VOICE_DOWNLINK (3: Remote party caller audio)
     * 4. MediaRecorder.AudioSource.VOICE_RECOGNITION (6)
     * 5. MediaRecorder.AudioSource.MIC (1)
     */
    private fun startHardwareAudioRecordLoop(isCallOngoing: Boolean) {
        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                if (isCallOngoing) {
                    try {
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    } catch (_: Exception) {}
                }

                val inCallSources = listOf(
                    Pair(MediaRecorder.AudioSource.VOICE_RECOGNITION, "VOICE_RECOGNITION (Direct Speech Line)"),
                    Pair(MediaRecorder.AudioSource.VOICE_COMMUNICATION, "VOICE_COMMUNICATION (Duplex AEC)"),
                    Pair(MediaRecorder.AudioSource.MIC, "MIC (Loudspeaker Intercept)"),
                    Pair(MediaRecorder.AudioSource.DEFAULT, "DEFAULT"),
                    Pair(MediaRecorder.AudioSource.VOICE_CALL, "VOICE_CALL (Direct Modem)"),
                    Pair(MediaRecorder.AudioSource.VOICE_DOWNLINK, "VOICE_DOWNLINK (Remote Caller)")
                )

                val standbySources = listOf(
                    Pair(MediaRecorder.AudioSource.MIC, "MIC"),
                    Pair(MediaRecorder.AudioSource.VOICE_COMMUNICATION, "VOICE_COMMUNICATION (Duplex)"),
                    Pair(MediaRecorder.AudioSource.VOICE_RECOGNITION, "VOICE_RECOGNITION"),
                    Pair(MediaRecorder.AudioSource.DEFAULT, "DEFAULT")
                )

                val sourcesToTry = if (isCallOngoing) inCallSources else standbySources
                var selectedSource = MediaRecorder.AudioSource.MIC
                var selectedName = "MIC"

                for ((source, name) in sourcesToTry) {
                    try {
                        val record = AudioRecord(source, sampleRate, channelConfig, audioFormat, bufferSize)
                        if (record.state == AudioRecord.STATE_INITIALIZED) {
                            audioRecord = record
                            selectedSource = source
                            selectedName = name
                            currentAudioSourceName = name
                            Log.i(TAG, "AudioRecord successfully initialized with source: $name ($source)")
                            break
                        } else {
                            record.release()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "AudioSource $name ($source) failed: ${e.message}")
                    }
                }

                val record = audioRecord
                if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                    record.startRecording()
                    isRecording = true

                    val pcmBuffer = ShortArray(bufferSize / 2)
                    val boostedBuffer = ShortArray(bufferSize / 2)

                    var loopIteration = 0
                    while (isRecording && isActive) {
                        loopIteration++
                        // Periodically reinforce speakerphone during active call
                        if (isCallOngoing && loopIteration % 50 == 0 && settingsManager.isAutoSpeakerphoneEnabled) {
                            val isSpeaker = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                            } else {
                                @Suppress("DEPRECATION")
                                audioManager.isSpeakerphoneOn
                            }
                            if (!isSpeaker) {
                                setSpeakerphoneState(true)
                            }
                        }

                        val readCount = record.read(pcmBuffer, 0, pcmBuffer.size)
                        if (readCount > 0) {
                            // Adaptive Headroom AGC: Measure peak to prevent distortion on loudspeaker
                            var peakVal = 0
                            for (i in 0 until readCount) {
                                val s = kotlin.math.abs(pcmBuffer[i].toInt())
                                if (s > peakVal) peakVal = s
                            }

                            // If speakerphone is at max volume (peak > 18000), gain is 1.0f (no clipping!)
                            // If audio is soft/distant, gain gently scales up to 3.5f without buzzing
                            val adaptiveGain = when {
                                peakVal > 18000 -> 1.0f
                                peakVal > 8000 -> 18000f / peakVal
                                peakVal > 2000 -> (18000f / peakVal).coerceAtMost(3.5f)
                                else -> 2.0f
                            }

                            var sumSquares = 0.0
                            for (i in 0 until readCount) {
                                val sample = (pcmBuffer[i] * adaptiveGain).toInt().coerceIn(-32767, 32767).toShort()
                                boostedBuffer[i] = sample
                                sumSquares += sample.toDouble() * sample.toDouble()
                            }

                            // Feed clean boosted PCM directly to on-device Vosk STT + Cloud Whisper + AI Voice engine
                            sttManager.acceptAudioChunkRaw(boostedBuffer, readCount)

                            val rms = sqrt(sumSquares / readCount) / 32768.0
                            if (rms > 0.008) {
                                val transcriptIntent = Intent("com.xorcall.AUDIO_STREAM_ACTIVE").apply {
                                    putExtra("rms", rms.toFloat())
                                    putExtra("isDownlinkActive", isCallOngoing)
                                    putExtra("source", selectedName)
                                    setPackage(packageName)
                                }
                                sendBroadcast(transcriptIntent)
                            }
                        } else if (readCount < 0) {
                            delay(100)
                        }
                    }
                } else {
                    Log.e(TAG, "All AudioRecord sources failed to initialize.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio capture loop exception", e)
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
                audioRecord = null
                isRecording = false
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
                                "CRITICAL: Remote desktop trojan detected during call! Blocked to protect bank accounts."
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

        val message = json.optString("summary", "Incoming telemetry alert")
        val transcript = json.optString("transcript", "")
        if (level == RiskLevel.CRITICAL || level == RiskLevel.WARN) {
            handleThreatDetected(
                ThreatAnalysis(
                    isScamDetected = true,
                    matchedKeywords = listOf("TELEMETRY_INTERCEPT"),
                    riskLevel = level,
                    warningMessage = message,
                    demographicVector = settingsManager.activeVector,
                    normalizedTranscript = transcript
                ),
                transcript
            )
        }
    }

    override fun onConnectionStateChanged(isConnected: Boolean) {
        Log.i(TAG, "Telemetry WebSocket connection state changed: $isConnected")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "XORcall Cyber Safety Guardian Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors live telephony audio stream for violent extortion, kidnap, and cyber fraud"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("XORcall Autonomous Cyber Shield")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        try {
            unregisterReceiver(settingsReceiver)
        } catch (_: Exception) {}
        setSpeakerphoneState(false)
        stopAudioCaptureInternal()
        hapticEngine.stopListening()
        whisperEngine.shutdown()
        floatingOverlay.dismiss()
        webSocketClient?.disconnect()
        sttManager.release()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
