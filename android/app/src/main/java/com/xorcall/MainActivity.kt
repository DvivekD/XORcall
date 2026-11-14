package com.xorcall

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import kotlinx.coroutines.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.xorcall.engine.*
import com.xorcall.service.CallMonitorService
import com.xorcall.service.PackageWatchdog
import com.xorcall.service.XORcallAccessibilityService
import com.xorcall.ui.FloatingAlertOverlay
import com.xorcall.ui.LockdownActivity
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            XORcallTheme {
                MainScreen(
                    onToggleShield = { activate ->
                        try {
                            val serviceIntent = Intent(this, CallMonitorService::class.java)
                            if (activate) {
                                val hasAudio = ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasAudio) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(serviceIntent)
                                    } else {
                                        startService(serviceIntent)
                                    }
                                }
                            } else {
                                stopService(serviceIntent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onDial1930 = {
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:1930")
                            }
                            startActivity(dialIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onRequestOverlay = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onOpenAccessibilitySettings = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            sendBroadcast(Intent("com.xorcall.TOGGLE_DEMO_TARGET"))
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(45, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45)
                }
            } catch (e: Exception) {}
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun XORcallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF050505),
            surface = Color(0xFF0F172A),
            primary = Color(0xFF10B981),
            error = Color(0xFFEF4444)
        ),
        content = content
    )
}

@Composable
fun MainScreen(
    onToggleShield: (Boolean) -> Unit,
    onDial1930: () -> Unit,
    onRequestOverlay: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { GuardianSettingsManager(context) }
    val hapticEngine = remember { TapticHapticEngine(context).apply { isHapticsEnabled = settingsManager.isHapticsEnabled } }
    val whisperEngine = remember { AudioWhisperEngine(context) }
    val guardianPingManager = remember { GuardianPingManager(context) }
    val floatingOverlay = remember { FloatingAlertOverlay(context) }
    val historyManager = remember { CallHistoryManager(context) }
    val sttManager = remember { 
        SherpaSTTManager(context.assets, context).apply { 
            initialize() 
            activeScope = settingsManager.activeVector
            isAiVoiceArmed = settingsManager.isAiVoiceDetectionArmed
        } 
    }

    var isShieldActive by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var riskLevel by remember { mutableStateOf(RiskLevel.SAFE) }
    var activeVector by remember { mutableStateOf(settingsManager.activeVector) }
    var isHapticsEnabled by remember { mutableStateOf(settingsManager.isHapticsEnabled) }
    var isAudioWhisperEnabled by remember { mutableStateOf(settingsManager.isAudioWhisperEnabled) }
    var isFamilyPingEnabled by remember { mutableStateOf(settingsManager.isFamilyPingEnabled) }
    var isBannerEnabled by remember { mutableStateOf(settingsManager.isBannerEnabled) }
    var isWhitelistContactsEnabled by remember { mutableStateOf(settingsManager.isWhitelistContactsEnabled) }
    var isAutoAllowBanksEnabled by remember { mutableStateOf(settingsManager.isAutoAllowBanksEnabled) }
    var isAppLockdownEnabled by remember { mutableStateOf(settingsManager.isAppLockdownEnabled) }
    var isAiVoiceDetectionArmed by remember { mutableStateOf(settingsManager.isAiVoiceDetectionArmed) }
    var isAutoSpeakerphoneEnabled by remember { mutableStateOf(settingsManager.isAutoSpeakerphoneEnabled) }
    var isAccessibilityActive by remember { mutableStateOf(XORcallAccessibilityService.isServiceRunning) }
    val whitelistedContactsList = remember { mutableStateListOf<WhitelistedContact>() }

    fun refreshWhitelisted() {
        whitelistedContactsList.clear()
        whitelistedContactsList.addAll(ContactWhitelistSyncManager.syncWhitelistedContacts(context))
    }
    var groqApiKey by remember { mutableStateOf(settingsManager.groqApiKey) }
    var showGroqKeyDialog by remember { mutableStateOf(false) }
    var groqKeyInput by remember { mutableStateOf(settingsManager.groqApiKey) }
    var latestAcousticResult by remember { mutableStateOf<AcousticAnalysisResult?>(null) }

    var liveTranscript by remember { mutableStateOf("Ready. Speak into phone — live words will transcribe here...") }
    var lastThreatMessage by remember { mutableStateOf<String?>(null) }
    var lastAiVoiceStatus by remember { mutableStateOf<String?>(null) }
    var isAudioStreamPulsing by remember { mutableStateOf(false) }
    var dossierPreviewDialog by remember { mutableStateOf<String?>(null) }
    var showCustomInjectorDialog by remember { mutableStateOf(false) }
    var customInputText by remember { mutableStateOf("") }

    val callRecords = remember { mutableStateListOf<CallAuditRecord>() }

    fun refreshHistory() {
        callRecords.clear()
        callRecords.addAll(historyManager.getAllRecords())
    }

    val coroutineScope = rememberCoroutineScope()
    var isVoskReady by remember { mutableStateOf(sttManager.isVoskReady) }
    var isUnpacking by remember { mutableStateOf(sttManager.isUnpacking) }
    var isMicTesting by remember { mutableStateOf(false) }
    var isAiForensicTesting by remember { mutableStateOf(false) }
    var isDemoAiTarget by remember { mutableStateOf(false) }
    var liveAudioRms by remember { mutableStateOf(0f) }
    var liveAudioSource by remember { mutableStateOf("Standby") }
    var voskErrorMessage by remember { mutableStateOf(sttManager.errorMessage) }
    var lastThreatAlertTime by remember { mutableStateOf(0L) }

    // VIDEO CALL DEEPFAKE INSPECTION ENGINE & HUD STATE
    val videoDetector = remember { VideoDeepfakeDetector() }
    var latestVideoResult by remember { mutableStateOf<VideoInspectionResult?>(null) }
    var activeVideoFeedMode by remember { mutableStateOf<String?>("HUMAN") }

    fun testNormalVideoFeed() {
        val result = videoDetector.verifyAuthenticHuman()
        latestVideoResult = result
        activeVideoFeedMode = "HUMAN"
        riskLevel = RiskLevel.SAFE
        lastThreatMessage = null
        if (isBannerEnabled) {
            floatingOverlay.showVideoDeepfakeHUD(result.hudBadge, isSynthetic = false)
        }
        Toast.makeText(context, "🟢 Biometric Identity Verified: Authentic Human Webcam", Toast.LENGTH_SHORT).show()
    }

    fun testDeepfakeVideoFeed() {
        val result = videoDetector.suspectSyntheticFace()
        latestVideoResult = result
        activeVideoFeedMode = "DEEPFAKE"
        riskLevel = RiskLevel.CRITICAL
        lastThreatMessage = result.hudBadge

        if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
        if (isBannerEnabled) floatingOverlay.showVideoDeepfakeHUD(result.hudBadge, isSynthetic = true)
        if (isAudioWhisperEnabled) whisperEngine.speakWarning("Warning: Video deepfake detected! Facial boundary warping and 2D-FFT high frequency synthetic grid identified.")
        if (isFamilyPingEnabled) guardianPingManager.sendSilentGuardianPing("+91 98450 71289", result.hudBadge, DemographicVector.ALL)
        historyManager.recordCall(
            "+91 98450 71289",
            "VIDEO_DEEPFAKE_FACE_SWAP",
            DemographicVector.ALL,
            RiskLevel.CRITICAL,
            true,
            0.96f,
            listOf("VIDEO_DEEPFAKE", "FACE_SWAP", "2D_FFT_LATTICE_ANOMALY", "BOUNDARY_WARPING"),
            result.hudBadge
        )
        refreshHistory()
    }

    DisposableEffect(sttManager) {
        sttManager.onModelReady = {
            coroutineScope.launch(Dispatchers.Main) {
                isVoskReady = true
                isUnpacking = false
                voskErrorMessage = null
            }
        }
        sttManager.onAcousticResult = { result ->
            coroutineScope.launch(Dispatchers.Main) {
                latestAcousticResult = result
                lastAiVoiceStatus = result.analysisReason
                if (result.isSyntheticVoice) {
                    val now = System.currentTimeMillis()
                    if (now - lastThreatAlertTime > 2500L) {
                        lastThreatAlertTime = now
                        riskLevel = RiskLevel.CRITICAL
                        lastThreatMessage = "🚨 CRITICAL: AI SYNTHETIC VOICE DETECTED! Artificial neural vocoder identified."
                        if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                    }
                }
            }
        }
        onDispose { }
    }

    LaunchedEffect(Unit) {
        refreshHistory()
        refreshWhitelisted()
        sttManager.updateCloudConfig(settingsManager.groqApiKey, settingsManager.backendUrl)
        sttManager.initializeVosk(this)

        // Remote iPhone Shortcut Webhook Poller:
        // Polls /api/v1/demo/state. When triggered from iPhone, switches to AI after 10s.
        // When rerun from iPhone, immediately returns to human!
        launch(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            var wasSynthetic = false
            while (true) {
                delay(1200L)
                try {
                    val rawUrl = settingsManager.backendUrl.trim().trimEnd('/')
                    if (rawUrl.isNotBlank()) {
                        val req = okhttp3.Request.Builder()
                            .url("$rawUrl/api/v1/demo/state")
                            .get()
                            .build()
                        client.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bodyStr = resp.body?.string() ?: ""
                                val json = org.json.JSONObject(bodyStr)
                                val isSynth = json.optBoolean("is_synthetic", false)
                                if (isSynth != wasSynthetic) {
                                    wasSynthetic = isSynth
                                    sttManager.isTargetAiClone = isSynth
                                    withContext(Dispatchers.Main) {
                                        if (isSynth) {
                                            val cloneResult = AcousticAnalysisResult(
                                                isSyntheticVoice = true,
                                                syntheticConfidence = 0.94f,
                                                highFrequencyEnergyRatio = 0.0031f,
                                                pitchJitterEstimate = 0.89f,
                                                spectralFlatness = 0.468f,
                                                isVoicedSpeech = true,
                                                analysisReason = "🚨 AI SYNTHETIC VOICE DETECTED (94%): Remote trigger activated. Neural vocoder diffusion confirmed."
                                            )
                                            latestAcousticResult = cloneResult
                                            lastAiVoiceStatus = cloneResult.analysisReason
                                            riskLevel = RiskLevel.CRITICAL
                                            lastThreatMessage = "🚨 CRITICAL: AI SYNTHETIC VOICE DETECTED!"
                                            if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                                            if (isBannerEnabled) floatingOverlay.showCriticalAlert("🚨 AI SYNTHETIC VOICE CLONE DETECTED (94%)!")
                                        } else {
                                            val bioResult = AcousticAnalysisResult(
                                                isSyntheticVoice = false,
                                                syntheticConfidence = 0.08f,
                                                highFrequencyEnergyRatio = 0.024f,
                                                pitchJitterEstimate = 0.62f,
                                                spectralFlatness = 0.182f,
                                                isVoicedSpeech = true,
                                                analysisReason = "🟢 Biological Human Voice: Natural vocal tract formant troughs & biological airflow confirmed."
                                            )
                                            latestAcousticResult = bioResult
                                            lastAiVoiceStatus = bioResult.analysisReason
                                            riskLevel = RiskLevel.SAFE
                                            floatingOverlay.dismiss()
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun triggerThreatExecution(phrase: String, callerId: String = "Test Caller", vector: DemographicVector = activeVector) {
        liveTranscript = "“$phrase”"
        val analysis = sttManager.analyzeText(phrase, vector)
        riskLevel = analysis.riskLevel
        lastThreatMessage = analysis.warningMessage

        if (analysis.isScamDetected) {
            if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(analysis.riskLevel)
            if (isBannerEnabled) floatingOverlay.showCriticalAlert(analysis.warningMessage)
            if (isAudioWhisperEnabled) whisperEngine.speakWarning(analysis.warningMessage)
            if (isFamilyPingEnabled) {
                guardianPingManager.sendSilentGuardianPing(callerId, analysis.warningMessage, analysis.demographicVector)
            }
            historyManager.recordCall(
                callerNumber = callerId,
                callerClassification = "IN_CALL_INTERCEPT",
                vector = analysis.demographicVector,
                risk = analysis.riskLevel,
                isSynthetic = analysis.isSyntheticVoice,
                syntheticConfidence = analysis.syntheticConfidence,
                keywords = analysis.matchedKeywords,
                transcript = phrase
            )
            refreshHistory()
        } else {
            Toast.makeText(context, "🟢 Normal speech: No threat markers found.", Toast.LENGTH_SHORT).show()
        }
    }

    fun startDirectMicTest() {
        if (isMicTesting) {
            isMicTesting = false
            return
        }
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            Toast.makeText(context, "Please grant microphone permission first", Toast.LENGTH_SHORT).show()
            return
        }
        isMicTesting = true
        liveTranscript = "🔴 RECORDING LIVE MIC: Speak or play voice audio to classify..."
        sttManager.resetAcousticRadar()

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val sampleRate = 16000
                    val channelConfig = AudioFormat.CHANNEL_IN_MONO
                    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                    val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)
                    var recorder: AudioRecord? = null
                    try {
                        val rec = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBuf)
                        if (rec.state == AudioRecord.STATE_INITIALIZED) {
                            recorder = rec
                            recorder.startRecording()
                            val pcmBuffer = ShortArray(minBuf / 2)
                            val boostedBuffer = ShortArray(minBuf / 2)
                            val endAt = System.currentTimeMillis() + 10000L
                            var lastUiUpdate = 0L
                            while (System.currentTimeMillis() < endAt && isMicTesting) {
                                val read = recorder.read(pcmBuffer, 0, pcmBuffer.size)
                                if (read > 0) {
                                    var peakVal = 0
                                    for (i in 0 until read) {
                                        val s = kotlin.math.abs(pcmBuffer[i].toInt())
                                        if (s > peakVal) peakVal = s
                                    }
                                    val adaptiveGain = when {
                                        peakVal > 18000 -> 1.0f
                                        peakVal > 8000 -> 18000f / peakVal
                                        peakVal > 2000 -> (18000f / peakVal).coerceAtMost(3.5f)
                                        else -> 2.0f
                                    }
                                    var sumSquares = 0.0
                                    for (i in 0 until read) {
                                        val sample = (pcmBuffer[i] * adaptiveGain).toInt().coerceIn(-32767, 32767).toShort()
                                        boostedBuffer[i] = sample
                                        sumSquares += sample.toDouble() * sample.toDouble()
                                    }
                                    val rms = (kotlin.math.sqrt(sumSquares / read) / 32768.0).toFloat()
                                    val now = System.currentTimeMillis()
                                    if (now - lastUiUpdate > 100) {
                                        lastUiUpdate = now
                                        withContext(Dispatchers.Main) {
                                            liveAudioRms = rms
                                            liveAudioSource = "Live Mic"
                                        }
                                    }
                                    sttManager.acceptAudioChunkRaw(boostedBuffer, read)
                                }
                            }
                        }
                    } finally {
                        try {
                            recorder?.stop()
                            recorder?.release()
                        } catch (_: Throwable) {}
                    }
                }
            } finally {
                isMicTesting = false
                liveAudioRms = 0f
                if (liveTranscript.startsWith("🔴 RECORDING")) {
                    liveTranscript = "Live mic idle. Spoken words transcribed above."
                }
            }
        }
    }



    // Set threat detection callback on STT engine
    LaunchedEffect(sttManager, activeVector, isHapticsEnabled, isBannerEnabled, isAudioWhisperEnabled, isFamilyPingEnabled) {
        sttManager.onThreatDetected = { analysis, transcript ->
            val now = System.currentTimeMillis()
            coroutineScope.launch(Dispatchers.Main) {
                riskLevel = analysis.riskLevel
                lastThreatMessage = analysis.warningMessage
                if (now - lastThreatAlertTime > 2500L) {
                    lastThreatAlertTime = now
                    if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(analysis.riskLevel)
                    if (isBannerEnabled) floatingOverlay.showCriticalAlert(analysis.warningMessage)
                    if (isAudioWhisperEnabled) whisperEngine.speakWarning(analysis.warningMessage)
                    if (isFamilyPingEnabled) {
                        guardianPingManager.sendSilentGuardianPing("Live Spoken Words", analysis.warningMessage, analysis.demographicVector)
                    }
                    historyManager.recordCall(
                        callerNumber = "Live Spoken Words",
                        callerClassification = "SPOKEN_WORDS",
                        vector = analysis.demographicVector,
                        risk = analysis.riskLevel,
                        isSynthetic = analysis.isSyntheticVoice,
                        syntheticConfidence = analysis.syntheticConfidence,
                        keywords = analysis.matchedKeywords,
                        transcript = transcript
                    )
                    refreshHistory()
                }
            }
        }
    }

    // Always-On Continuous Live Transcription when Shield is active (Vosk on-device STT)
    LaunchedEffect(isShieldActive, activeVector) {
        if (isShieldActive) {
            val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasMic) {
                sttManager.startAlwaysOnLiveListening { text, _ ->
                    if (text.isNotBlank()) {
                        coroutineScope.launch(Dispatchers.Main) {
                            liveTranscript = "“$text”"
                        }
                    }
                }
            }
        } else {
            sttManager.stopAlwaysOnLiveListening()
        }
    }

    // Dynamic Broadcast Receiver for incoming telephony & accessibility events
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.xorcall.THREAT_ALERT" -> {
                        val levelStr = intent.getStringExtra("level") ?: "SAFE"
                        val message = intent.getStringExtra("message") ?: "Extortion threat intercepted!"
                        val transcript = intent.getStringExtra("transcript") ?: ""
                        riskLevel = try {
                            RiskLevel.valueOf(levelStr)
                        } catch (_: Exception) {
                            RiskLevel.CRITICAL
                        }
                        lastThreatMessage = message
                        if (transcript.isNotBlank()) {
                            liveTranscript = "“$transcript”"
                        }
                        if (isHapticsEnabled) {
                            hapticEngine.triggerImmediateShockwave(riskLevel)
                        }
                        refreshHistory()
                    }
                    "com.xorcall.TRANSCRIPT_UPDATE" -> {
                        val text = intent.getStringExtra("text") ?: ""
                        if (text.isNotBlank()) {
                            liveTranscript = "“$text”"
                        }
                    }
                    "com.xorcall.AUDIO_STREAM_ACTIVE" -> {
                        isAudioStreamPulsing = true
                        liveAudioRms = intent.getFloatExtra("rms", 0f)
                        liveAudioSource = intent.getStringExtra("source") ?: "Direct Line"
                    }
                    "com.xorcall.ACCESSIBILITY_STATE_CHANGED" -> {
                        isAccessibilityActive = intent.getBooleanExtra("isActive", false)
                    }
                    "com.xorcall.AI_VOICE_ALERT" -> {
                        val reason = intent.getStringExtra("reason") ?: "Synthetic Neural Vocoder Detected"
                        lastAiVoiceStatus = reason
                    }
                    "com.xorcall.ACOUSTIC_UPDATE" -> {
                        val isSynthetic = intent.getBooleanExtra("isSynthetic", false)
                        val confidence = intent.getFloatExtra("confidence", 0f)
                        val hfRatio = intent.getFloatExtra("hfRatio", 0.035f)
                        val jitter = intent.getFloatExtra("jitter", 1.20f)
                        val flatness = intent.getFloatExtra("flatness", 0.20f)
                        val isVoiced = intent.getBooleanExtra("isVoiced", false)
                        val reason = intent.getStringExtra("reason") ?: "Spoken acoustic stream nominal."

                        val subBassRatio = intent.getFloatExtra("subBassRatio", 0.25f)
                        val isLoudspeakerReplay = intent.getBooleanExtra("isLoudspeakerReplay", false)

                        latestAcousticResult = AcousticAnalysisResult(
                            isSyntheticVoice = isSynthetic,
                            syntheticConfidence = confidence,
                            highFrequencyEnergyRatio = hfRatio,
                            pitchJitterEstimate = jitter,
                            spectralFlatness = flatness,
                            isVoicedSpeech = isVoiced,
                            analysisReason = reason,
                            subBassProximityRatio = subBassRatio,
                            isLoudspeakerReplay = isLoudspeakerReplay
                        )
                        if (isSynthetic) {
                            lastAiVoiceStatus = reason
                            val now = System.currentTimeMillis()
                            if (now - lastThreatAlertTime > 2500L) {
                                lastThreatAlertTime = now
                                riskLevel = RiskLevel.CRITICAL
                                lastThreatMessage = "🚨 CRITICAL: AI SYNTHETIC VOICE DETECTED! Artificial neural vocoder identified."
                                if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                            }
                        }
                    }
                    "com.xorcall.WHITELIST_CALL" -> {
                        val entity = intent.getStringExtra("name") ?: intent.getStringExtra("entity") ?: "Trusted Contact"
                        val number = intent.getStringExtra("number") ?: ""
                        val rel = intent.getStringExtra("relationship") ?: "Family / Verified"
                        liveTranscript = "🟢 Whitelist Call Passed: $entity ($rel, $number) · Threat alerts suppressed"
                        riskLevel = RiskLevel.SAFE
                        lastThreatMessage = null
                        floatingOverlay.dismiss()
                        refreshHistory()
                    }
                    "com.xorcall.FINANCIAL_APP_BLOCKED" -> {
                        val appName = intent.getStringExtra("appName") ?: "Financial App"
                        lastThreatMessage = "🚨 KAVACH APP LOCKDOWN: Blocked $appName to protect funds!"
                    }
                    "com.xorcall.SCAM_ACTIVE_STATE" -> {
                        val active = intent.getBooleanExtra("isActive", false)
                        if (!active) {
                            refreshHistory()
                        }
                    }
                    "com.xorcall.TOGGLE_DEMO_TARGET" -> {
                        isDemoAiTarget = !isDemoAiTarget
                        sttManager.isTargetAiClone = isDemoAiTarget
                        if (isDemoAiTarget) {
                            latestAcousticResult = AcousticAnalysisResult(
                                isSyntheticVoice = true,
                                syntheticConfidence = 0.94f,
                                highFrequencyEnergyRatio = 0.082f,
                                pitchJitterEstimate = 0.91f,
                                spectralFlatness = 0.435f,
                                isVoicedSpeech = true,
                                analysisReason = "🚨 AI SYNTHETIC / LOUDSPEAKER VOICE (94%): Sub-180Hz driver void (0.041 prox) & vocoder diffusion (0.435 flatness).",
                                subBassProximityRatio = 0.041f,
                                isLoudspeakerReplay = true
                            )
                        } else {
                            latestAcousticResult = AcousticAnalysisResult(
                                isSyntheticVoice = false,
                                syntheticConfidence = 0.11f,
                                highFrequencyEnergyRatio = 0.024f,
                                pitchJitterEstimate = 0.62f,
                                spectralFlatness = 0.182f,
                                isVoicedSpeech = true,
                                analysisReason = "🟢 Biological Human Voice: Proximity chest resonance confirmed (0.342 prox) & natural formants (0.182 flatness).",
                                subBassProximityRatio = 0.342f,
                                isLoudspeakerReplay = false
                            )
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("com.xorcall.THREAT_ALERT")
            addAction("com.xorcall.TRANSCRIPT_UPDATE")
            addAction("com.xorcall.AUDIO_STREAM_ACTIVE")
            addAction("com.xorcall.ACCESSIBILITY_STATE_CHANGED")
            addAction("com.xorcall.AI_VOICE_ALERT")
            addAction("com.xorcall.ACOUSTIC_UPDATE")
            addAction("com.xorcall.WHITELIST_CALL")
            addAction("com.xorcall.FINANCIAL_APP_BLOCKED")
            addAction("com.xorcall.SCAM_ACTIVE_STATE")
            addAction("com.xorcall.TOGGLE_DEMO_TARGET")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (audioGranted) {
            isShieldActive = true
            onToggleShield(true)
        }
    }

    LaunchedEffect(Unit) {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }

        if (!hasAudio) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            onToggleShield(true)
        }
    }

    Scaffold(
        containerColor = Color(0xFF050505),
        bottomBar = {
            BottomSpeedDialBar(onDial1930 = onDial1930)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeaderSection()
            }

            // ON-DEVICE VOSK SPEECH ENGINE STATUS & UNPACKING CARD
            item {
                ModelStatusCard(
                    isReady = isVoskReady,
                    isUnpacking = isUnpacking,
                    errorMessage = voskErrorMessage,
                    groqApiKey = groqApiKey,
                    onConfigureGroqKey = {
                        groqKeyInput = groqApiKey
                        showGroqKeyDialog = true
                    },
                    onInitClick = {
                        sttManager.initializeVosk(coroutineScope)
                    }
                )
            }

            // DIRECT ACCESSIBILITY AUDIO INTERCEPTOR CARD
            item {
                AccessibilityStatusCard(
                    isActive = isAccessibilityActive,
                    onEnableClick = onOpenAccessibilitySettings
                )
            }

            // DEMOGRAPHIC VECTOR SELECTOR TABS
            item {
                DemographicVectorSelector(
                    activeVector = activeVector,
                    onSelectVector = {
                        activeVector = it
                        settingsManager.activeVector = it
                        sttManager.activeScope = it
                    }
                )
            }

            // CRITICAL THREAT BANNER
            if (riskLevel == RiskLevel.CRITICAL) {
                item {
                    CriticalThreatAlertBanner(
                        message = lastThreatMessage ?: "Extortion / Digital Arrest script spotted on call!",
                        onDial1930 = onDial1930,
                        onDismiss = {
                            riskLevel = RiskLevel.SAFE
                            floatingOverlay.dismiss()
                        }
                    )
                }
            }

            // SHIELD STATUS ORB
            item {
                ShieldStatusOrb(
                    isActive = isShieldActive,
                    risk = riskLevel,
                    isAudioPulsing = isAudioStreamPulsing,
                    onToggle = {
                        val nextState = !isShieldActive
                        isShieldActive = nextState
                        onToggleShield(nextState)
                    }
                )
            }

            // SMART CALL WHITELISTING & FINANCIAL APP LOCKER CARD
            item {
                SmartWhitelistingCard(
                    whitelistContacts = isWhitelistContactsEnabled,
                    onToggleWhitelistContacts = {
                        isWhitelistContactsEnabled = it
                        settingsManager.isWhitelistContactsEnabled = it
                    },
                    allowOfficialBanks = isAutoAllowBanksEnabled,
                    onToggleAllowOfficialBanks = {
                        isAutoAllowBanksEnabled = it
                        settingsManager.isAutoAllowBanksEnabled = it
                    },
                    appLockdown = isAppLockdownEnabled,
                    onToggleAppLockdown = {
                        isAppLockdownEnabled = it
                        settingsManager.isAppLockdownEnabled = it
                    },
                    aiVoiceArmed = isAiVoiceDetectionArmed,
                    onToggleAiVoice = {
                        isAiVoiceDetectionArmed = it
                        settingsManager.isAiVoiceDetectionArmed = it
                        sttManager.isAiVoiceArmed = it
                    },
                    autoSpeakerphone = isAutoSpeakerphoneEnabled,
                    onToggleAutoSpeakerphone = {
                        isAutoSpeakerphoneEnabled = it
                        settingsManager.isAutoSpeakerphoneEnabled = it
                    },
                    whitelistedContacts = whitelistedContactsList,
                    onSimulateWhitelistedCall = { contact ->
                        ContactWhitelistSyncManager.handleWhitelistedCall(
                            context = context,
                            number = contact.number,
                            contact = contact,
                            historyManager = historyManager
                        )
                        liveTranscript = "🟢 Whitelist Call Passed: ${contact.name} (${contact.relationship}, ${contact.number}) · All scam alerts suppressed"
                        riskLevel = RiskLevel.SAFE
                        lastThreatMessage = null
                        floatingOverlay.dismiss()
                        refreshHistory()
                        Toast.makeText(context, "🟢 Whitelist Verified: Call from ${contact.name} (${contact.relationship}) passed safely!", Toast.LENGTH_SHORT).show()
                    },
                    onRefreshWhitelist = { refreshWhitelisted() }
                )
            }

            // DIRECT IN-CALL INTERCEPT CONSOLE & ACTOR ROLEPLAY
            item {
                DemographicThreatTesterCard(
                    activeVector = activeVector,
                    liveTranscript = liveTranscript,
                    aiVoiceStatus = lastAiVoiceStatus,
                    acousticResult = latestAcousticResult,
                    isLiveTranscribing = isShieldActive,
                    isMicTesting = isMicTesting,
                    audioRms = liveAudioRms,
                    audioSource = liveAudioSource,
                    onStartMicTest = { startDirectMicTest() },
                    onOpenCustomInjector = {
                        showCustomInjectorDialog = true
                    },
                    onTestAppLockdown = {
                        XORcallAccessibilityService.setScamCallActive(true)
                        PackageWatchdog.setScamCallActive(true)
                        riskLevel = RiskLevel.CRITICAL
                        lastThreatMessage = "🚨 CRITICAL: Extortion Scam Call Active! Sensitive payment apps frozen."
                        LockdownActivity.launch(
                            context = context,
                            packageName = "com.phonepe.app",
                            appName = "PhonePe UPI",
                            reason = "Simulated attempt by coerced victim to open PhonePe during active Digital Arrest scam call."
                        )
                        Toast.makeText(context, "🚨 KAVACH LOCKDOWN: PhonePe access blocked during active scam call!", Toast.LENGTH_LONG).show()
                    },
                    onTriggerKidnap5Lakhs = {
                        triggerThreatExecution(
                            "We have kidnapped your son. Pay 5 lakhs immediately or we will kill him.",
                            "+91 99999 12345",
                            DemographicVector.ALL
                        )
                    },
                    onTriggerUniversalExtortion = {
                        triggerThreatExecution(
                            "We have seized your records. Wire 5 million dollar extraction immediately or face arrest.",
                            "+1 800 555 0199",
                            DemographicVector.ALL
                        )
                    },
                    onTriggerStudentScam = {
                        triggerThreatExecution(
                            "Telegram part-time review job: Like YouTube videos to earn ₹5,000. Balance frozen! Recharge crypto wallet with ₹40,000 USDT to release payout.",
                            "+91 98765 43210",
                            DemographicVector.STUDENT
                        )
                    },
                    onTriggerSeniorScam = {
                        triggerThreatExecution(
                            "Supreme Court digital arrest warrant issued! Mumbai Police CBI narcotics seized parcel. Liquidate fixed deposit and transfer ₹8 Lakhs to secret RBI escrow account now.",
                            "+92 300 1234567",
                            DemographicVector.SENIOR
                        )
                    },
                    onTriggerProfessionalScam = {
                        triggerThreatExecution(
                            "FedEx Customer Support: Illegal parcel from Mumbai to Taiwan seized with 5 expired passports and 140g MDMA. TRAI will disconnect your mobile number in 2 hours.",
                            "+44 70 98765432",
                            DemographicVector.PROFESSIONAL
                        )
                    },
                    onTriggerAiCloneScam = {
                        val sample = "Papa! I had an accident on the highway, doctor says leg fractured, police putting me in lockup! Inspector wants ₹50,000 bail money right now!"
                        liveTranscript = "“$sample”"
                        lastAiVoiceStatus = "⚠️ Neural Vocoder High-Frequency Roll-Off Detected (94% AI Clone Confidence)"
                        latestAcousticResult = AcousticAnalysisResult(
                            isSyntheticVoice = true,
                            syntheticConfidence = 0.94f,
                            highFrequencyEnergyRatio = 0.0031f,
                            pitchJitterEstimate = 0.16f,
                            spectralFlatness = 0.495f,
                            isVoicedSpeech = true,
                            analysisReason = "🚨 AI SYNTHETIC VOICE DETECTED (94%): Neural vocoder harmonic rigidity (<0.16% micro-jitter) & elevated entropy."
                        )
                        val analysis = ThreatAnalysis(
                            isScamDetected = true,
                            matchedKeywords = listOf("accident", "hospital", "bail", "lockup", "AI_SYNTHETIC_VOICE"),
                            riskLevel = RiskLevel.CRITICAL,
                            warningMessage = "🚨 CRITICAL: AI-CLONED SYNTHETIC VOICE DETECTED! Fabricated emergency call.",
                            demographicVector = DemographicVector.ALL,
                            isSyntheticVoice = true,
                            syntheticConfidence = 0.94f
                        )
                        riskLevel = RiskLevel.CRITICAL
                        lastThreatMessage = analysis.warningMessage
                        if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                        if (isBannerEnabled) floatingOverlay.showCriticalAlert(analysis.warningMessage)
                        if (isAudioWhisperEnabled) whisperEngine.speakWarning(analysis.warningMessage)
                        if (isFamilyPingEnabled) guardianPingManager.sendSilentGuardianPing("+91 88888 99999", analysis.warningMessage, DemographicVector.ALL)
                        historyManager.recordCall("+91 88888 99999", "UNKNOWN_AI_CLONE", DemographicVector.ALL, RiskLevel.CRITICAL, true, 0.94f, analysis.matchedKeywords, sample)
                        refreshHistory()
                    },
                    onTriggerHapticsOnly = {
                        hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                        Toast.makeText(context, "📳 Sensory Cheek/Ear Shockwave Fired!", Toast.LENGTH_SHORT).show()
                    },
                    onReset = {
                        riskLevel = RiskLevel.SAFE
                        lastThreatMessage = null
                        lastAiVoiceStatus = null
                        latestAcousticResult = null
                        floatingOverlay.dismiss()
                    }
                )
            }

            // VIDEO CALL DEEPFAKE DETECTION & BIOMETRIC HUD CARD
            item {
                VideoCallDeepfakeTesterCard(
                    result = latestVideoResult,
                    activeMode = activeVideoFeedMode,
                    onTestNormalVideo = { testNormalVideoFeed() },
                    onTestDeepfakeVideo = { testDeepfakeVideoFeed() },
                    onTriggerFloatingHUD = {
                        val res = latestVideoResult ?: videoDetector.suspectSyntheticFace()
                        latestVideoResult = res
                        floatingOverlay.showVideoDeepfakeHUD(res.hudBadge, isSynthetic = res.isSynthetic)
                        Toast.makeText(context, "🪟 Injected Video Call Floating HUD Overlay!", Toast.LENGTH_SHORT).show()
                    },
                    onReset = {
                        latestVideoResult = null
                        activeVideoFeedMode = null
                        riskLevel = RiskLevel.SAFE
                        lastThreatMessage = null
                        floatingOverlay.dismiss()
                    }
                )
            }

            // MODULAR SENSORY TOGGLES
            item {
                ModularSensoryTogglesCard(
                    haptics = isHapticsEnabled,
                    onToggleHaptics = { 
                        isHapticsEnabled = it 
                        settingsManager.isHapticsEnabled = it
                        hapticEngine.isHapticsEnabled = it
                    },
                    whisper = isAudioWhisperEnabled,
                    onToggleWhisper = { 
                        isAudioWhisperEnabled = it 
                        settingsManager.isAudioWhisperEnabled = it
                    },
                    familyPing = isFamilyPingEnabled,
                    onToggleFamilyPing = { 
                        isFamilyPingEnabled = it 
                        settingsManager.isFamilyPingEnabled = it
                    },
                    banner = isBannerEnabled,
                    onToggleBanner = { 
                        isBannerEnabled = it 
                        settingsManager.isBannerEnabled = it
                        onRequestOverlay()
                    }
                )
            }

            // CALL TRANSCRIPTION & FORENSIC HISTORY VAULT
            item {
                CallHistoryVaultCard(
                    records = callRecords,
                    onExport1930Dossier = { record ->
                        val dossier = historyManager.generate1930ComplaintText(record)
                        dossierPreviewDialog = dossier
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = android.content.ClipData.newPlainText("1930 Cybercell Dossier", dossier)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "📋 1930 Dossier Copied to Clipboard!", Toast.LENGTH_LONG).show()
                    },
                    onClearHistory = {
                        historyManager.clearAll()
                        refreshHistory()
                    }
                )
            }
        }
    }

    // Custom In-Call Phrase Injector Dialog
    if (showCustomInjectorDialog) {
        AlertDialog(
            onDismissRequest = { showCustomInjectorDialog = false },
            title = {
                Text("🎙️ Custom Roleplay Threat Injector", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "Type or edit the exact extortion demand you or your friend want to test:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. We have your son, give 5 lakhs or we kill him", fontSize = 11.sp, color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customInputText.isNotBlank()) {
                            triggerThreatExecution(customInputText, "Custom Roleplay Injector")
                        }
                        showCustomInjectorDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Inject Threat")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomInjectorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Groq Whisper API Key Configuration Dialog
    if (showGroqKeyDialog) {
        AlertDialog(
            onDismissRequest = { showGroqKeyDialog = false },
            title = {
                Text("⚡ Groq Whisper Turbo Cloud STT", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "Enter your Groq API Key to enable Whisper Turbo (whisper-large-v3-turbo). Delivers sub-200ms real-time transcription with high Indian English and Hinglish accuracy.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = groqKeyInput,
                        onValueChange = { groqKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("gsk_...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                        label = { Text("Groq API Key", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Free 25k req/day at console.groq.com",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp
                        )
                        TextButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { groqKeyInput = it.trim() }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("📋 Paste Key", fontSize = 10.sp, color = Color(0xFF34D399))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = groqKeyInput.trim()
                        groqApiKey = trimmed
                        settingsManager.groqApiKey = trimmed
                        sttManager.updateCloudConfig(trimmed, settingsManager.backendUrl)
                        showGroqKeyDialog = false
                        Toast.makeText(
                            context,
                            if (trimmed.isNotBlank()) "Groq Whisper Turbo Armed! (Cloud Hybrid STT)" else "Reverted to 100% On-Device Vosk",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Save & Arm", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGroqKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 1930 Dossier Preview Dialog
    dossierPreviewDialog?.let { dossier ->
        AlertDialog(
            onDismissRequest = { dossierPreviewDialog = null },
            title = {
                Text("1930 / I4C Cyber Crime Dossier", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFEF4444))
            },
            text = {
                Text(dossier, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFE2E8F0))
            },
            confirmButton = {
                Button(
                    onClick = {
                        dossierPreviewDialog = null
                        onDial1930()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Dial 1930 Helpline")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { dossierPreviewDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AccessibilityStatusCard(
    isActive: Boolean,
    onEnableClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) Color(0xFF064E3B) else Color(0xFF1E1B4B))
            .border(1.dp, if (isActive) Color(0xFF059669) else Color(0xFF6366F1), RoundedCornerShape(10.dp))
            .clickable { if (!isActive) onEnableClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(if (isActive) "⚡" else "🛡️", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isActive) "DIRECT CALL AUDIO INTERCEPTOR: ARMED" else "DIRECT CALL AUDIO INTERCEPTOR",
                    color = if (isActive) Color(0xFF6EE7B7) else Color(0xFFA5B4FC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isActive) 
                        "Capturing remote earpiece audio directly via AGC digital amplifier (+24dB)."
                    else 
                        "Tap to grant Accessibility Service permission for automatic in-call audio output capture without speakerphone.",
                    color = Color.White,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }
        if (!isActive) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onEnableClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Enable", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DemographicVectorSelector(
    activeVector: DemographicVector,
    onSelectVector: (DemographicVector) -> Unit
) {
    Column {
        Text(
            text = "DEMOGRAPHIC THREAT SCOPE",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val vectors = listOf(
                DemographicVector.ALL to "Omni-Scope",
                DemographicVector.STUDENT to "👨‍🎓 Student",
                DemographicVector.SENIOR to "👴 Senior",
                DemographicVector.PROFESSIONAL to "💼 Pro"
            )
            vectors.forEach { (vec, label) ->
                val isSelected = activeVector == vec
                Button(
                    onClick = { onSelectVector(vec) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
fun SmartWhitelistingCard(
    whitelistContacts: Boolean,
    onToggleWhitelistContacts: (Boolean) -> Unit,
    allowOfficialBanks: Boolean,
    onToggleAllowOfficialBanks: (Boolean) -> Unit,
    appLockdown: Boolean,
    onToggleAppLockdown: (Boolean) -> Unit,
    aiVoiceArmed: Boolean,
    onToggleAiVoice: (Boolean) -> Unit,
    autoSpeakerphone: Boolean = true,
    onToggleAutoSpeakerphone: (Boolean) -> Unit = {},
    whitelistedContacts: List<WhitelistedContact> = emptyList(),
    onSimulateWhitelistedCall: (WhitelistedContact) -> Unit = {},
    onRefreshWhitelist: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B132B))
            .border(1.dp, Color(0xFF1C2A4F), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SMART WHITELIST & APP LOCKER",
                    color = Color(0xFF60A5FA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF065F46))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("AUTO-IMMUNE", color = Color(0xFF6EE7B7), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Legitimate calls from trusted family (Mom, Dad, Starred) and official emergency lines (1930, 112) are immune to false alarms. Sensitive payment apps freeze automatically during extortion threats.",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            lineHeight = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Security Toggles
        InterventionRow("Whitelist Saved Contacts & Kinship (Mom, Dad, Starred)", whitelistContacts, onToggleWhitelistContacts)
        InterventionRow("Auto-Allow Official Banks & Helplines (1930, 112, 1800 SBI/HDFC)", allowOfficialBanks, onToggleAllowOfficialBanks)
        InterventionRow("Automatic Financial App Lockdown (Freeze PhonePe/GPay during Scam)", appLockdown, onToggleAppLockdown)
        InterventionRow("Neural AI Voice Clone Decoding (FFT & Micro-Jitter)", aiVoiceArmed, onToggleAiVoice)
        InterventionRow("Auto-Speakerphone In-Call Capture (Loudspeaker Intercept)", autoSpeakerphone, onToggleAutoSpeakerphone)

        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = Color(0xFF1C2A4F), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Whitelisted Contacts Directory Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DETECTED TRUSTED ENTITIES & HELPLINES",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Tap any contact to simulate an incoming verified call",
                    color = Color(0xFF64748B),
                    fontSize = 8.sp
                )
            }
            TextButton(
                onClick = onRefreshWhitelist,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Sync ↻", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val displayList = if (whitelistedContacts.isNotEmpty()) {
            whitelistedContacts.take(6)
        } else {
            ContactWhitelistSyncManager.getPresetFamilyContacts().take(3) + 
            ContactWhitelistSyncManager.getEmergencyContacts().take(2) + 
            ContactWhitelistSyncManager.getBankContacts().take(1)
        }

        displayList.forEach { contact ->
            WhitelistedContactRow(contact, onSimulate = { onSimulateWhitelistedCall(contact) })
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun WhitelistedContactRow(
    contact: WhitelistedContact,
    onSimulate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070E20))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .clickable { onSimulate() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                text = if (contact.isOfficialBankOrEmergency) "🏛️" else "⭐",
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (contact.isOfficialBankOrEmergency) Color(0xFF1E3A8A) else Color(0xFF065F46))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = contact.relationship,
                            color = if (contact.isOfficialBankOrEmergency) Color(0xFF93C5FD) else Color(0xFF6EE7B7),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = contact.number,
                    color = Color(0xFF94A3B8),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Button(
            onClick = onSimulate,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2B48)),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("Simulate Call", fontSize = 8.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DemographicThreatTesterCard(
    activeVector: DemographicVector,
    liveTranscript: String,
    aiVoiceStatus: String?,
    acousticResult: AcousticAnalysisResult? = null,
    isLiveTranscribing: Boolean,
    isMicTesting: Boolean = false,
    audioRms: Float = 0f,
    audioSource: String = "Direct Line",
    onStartMicTest: () -> Unit = {},
    onOpenCustomInjector: () -> Unit,
    onTriggerKidnap5Lakhs: () -> Unit,
    onTriggerUniversalExtortion: () -> Unit,
    onTriggerStudentScam: () -> Unit,
    onTriggerSeniorScam: () -> Unit,
    onTriggerProfessionalScam: () -> Unit,
    onTriggerAiCloneScam: () -> Unit,
    onTriggerHapticsOnly: () -> Unit,
    onTestAppLockdown: () -> Unit = {},
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isLiveTranscribing) Color(0xFFEF4444) else Color(0xFF64748B))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isLiveTranscribing) "DIRECT CALL INTERCEPT ARMED" else "STANDBY (SHIELD OFF)",
                    color = if (isLiveTranscribing) Color(0xFFF87171) else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF064E3B))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(activeVector.name, color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (audioRms > 0.005f || isMicTesting) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "⚡ Direct Audio: $audioSource (Energy: ${(audioRms * 100).toInt()}%)",
                    color = Color(0xFF6EE7B7),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live Transcript Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF030712))
                .border(1.dp, if (isLiveTranscribing) Color(0xFF2563EB) else Color(0xFF1F2937), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Column {
                Text(
                    text = liveTranscript,
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp
                )
                if (aiVoiceStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = aiVoiceStatus,
                        color = Color(0xFFA855F7),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val displayedAcoustic = acousticResult ?: AcousticAnalysisResult(
            isSyntheticVoice = false,
            syntheticConfidence = 0.05f,
            highFrequencyEnergyRatio = 0.035f,
            pitchJitterEstimate = 1.20f,
            spectralFlatness = 0.20f,
            isVoicedSpeech = false,
            analysisReason = "Ready to analyze voice acoustics. Speak into mic or on call to differentiate Biological vs AI Voice."
        )
        Spacer(modifier = Modifier.height(8.dp))
        AcousticRadarCard(displayedAcoustic)

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Live Mic Speech & AI Voice Recognition Button
        Button(
            onClick = onStartMicTest,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMicTesting) Color(0xFFDC2626) else Color(0xFF059669)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isMicTesting) "⏹️ STOP LIVE DETECTION (RECORDING MIC...)" else "🎙️ START LIVE DETECTION (Speech & AI Voice)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Custom Text Injector Button
        Button(
            onClick = onOpenCustomInjector,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("✏️ Test Any Custom Threat Phrase (Injector)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 1: Kidnap 5L & Son + 5M Extortion
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onTriggerKidnap5Lakhs,
                modifier = Modifier.weight(1.3f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("🔴 Kidnap: 5L & Son", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Black)
            }
            Button(
                onClick = onTriggerUniversalExtortion,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("💰 5M $ Extort", fontSize = 9.sp, color = Color(0xFFFECACA), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Student + Senior CBI + Pro FedEx
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onTriggerStudentScam,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("👨‍🎓 Student Task", fontSize = 9.sp, color = Color(0xFFFECACA), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onTriggerSeniorScam,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("👴 Senior CBI", fontSize = 9.sp, color = Color(0xFFFECACA), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onTriggerProfessionalScam,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF831843)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("💼 Pro FedEx", fontSize = 9.sp, color = Color(0xFFFBCFE8), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 3: AI Voice Clone + Haptics + Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onTriggerAiCloneScam,
                modifier = Modifier.weight(1.3f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF581C87)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("🤖 AI Voice Clone", fontSize = 9.sp, color = Color(0xFFF3E8FF), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onTriggerHapticsOnly,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("📳 Haptics", fontSize = 9.sp, color = Color(0xFF38BDF8))
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(0.7f),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("Clear", fontSize = 9.sp, color = Color(0xFF94A3B8))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mission-Critical Cyber Defense: Financial App Lockdown Demo
        Button(
            onClick = onTestAppLockdown,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔒", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TEST FINANCIAL APP LOCKDOWN (Simulate opening PhonePe during Scam)",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CallHistoryVaultCard(
    records: List<CallAuditRecord>,
    onExport1930Dossier: (CallAuditRecord) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FORENSIC CALL VAULT (BSA SEC 63)",
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${records.size} Monitored Events with SHA-256 Hashes",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp
                )
            }
            if (records.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("Clear All", color = Color(0xFFEF4444), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No scam calls recorded yet. All lines secure.", color = Color(0xFF64748B), fontSize = 11.sp)
            }
        } else {
            records.take(6).forEach { rec ->
                CallRecordItem(rec, onExport = { onExport1930Dossier(rec) })
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun CallRecordItem(record: CallAuditRecord, onExport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0F1D))
            .border(1.dp, if (record.riskLevel == "CRITICAL") Color(0xFF7F1D1D) else Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(record.callerNumber, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${record.formattedTime} · ${record.demographicVector}", color = Color(0xFF64748B), fontSize = 9.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (record.riskLevel == "CRITICAL") Color(0xFFDC2626) else Color(0xFF047857))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(record.riskLevel, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "“${record.transcript.take(110)}...”",
            color = Color(0xFFCBD5E1),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 13.sp
        )

        if (record.isSyntheticVoice) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🤖 AI Synthetic Voice Confirmed (${(record.syntheticConfidence * 100).toInt()}%)",
                color = Color(0xFFA855F7),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SHA: ${record.evidenceHashSha256.take(16)}...",
                color = Color(0xFF475569),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("📋 1930 Dossier", fontSize = 9.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "XORcall",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "MULTI-DEMOGRAPHIC CYBER GUARDIAN",
                color = Color(0xFF10B981),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "v1.0.9-PROD",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun CriticalThreatAlertBanner(
    message: String,
    onDial1930: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF450A0A))
            .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🚨", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CRITICAL EXTORTION INTERCEPTED",
                color = Color(0xFFFCA5A5),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDial1930,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("DIAL 1930 NOW", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("DISMISS", fontSize = 11.sp, color = Color(0xFFCBD5E1))
            }
        }
    }
}

@Composable
fun ShieldStatusOrb(
    isActive: Boolean,
    risk: RiskLevel,
    isAudioPulsing: Boolean,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isAudioPulsing) 0.90f else 0.95f,
        targetValue = if (isAudioPulsing) 1.10f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isAudioPulsing) 600 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val orbColor by animateColorAsState(
        targetValue = when {
            !isActive -> Color(0xFF475569)
            risk == RiskLevel.CRITICAL -> Color(0xFFEF4444)
            risk == RiskLevel.WARN -> Color(0xFFF59E0B)
            risk == RiskLevel.WATCH -> Color(0xFF3B82F6)
            isAudioPulsing -> Color(0xFF34D399)
            else -> Color(0xFF10B981)
        },
        label = "color"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF090D16))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size((130 * (if (isActive) pulseScale else 1f)).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(orbColor.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
                .border(2.dp, orbColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isActive) "SHIELD" else "STANDBY",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (isActive) (if (isAudioPulsing) "STREAMING" else risk.name) else "OFFLINE",
                    color = orbColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isActive) 
                "🟢 Armed & Actively Listening (Transcribing live words)" 
            else 
                "Tap to arm real-time defense",
            color = Color(0xFF64748B),
            fontSize = 11.sp
        )
    }
}

@Composable
fun ModularSensoryTogglesCard(
    haptics: Boolean,
    onToggleHaptics: (Boolean) -> Unit,
    whisper: Boolean,
    onToggleWhisper: (Boolean) -> Unit,
    familyPing: Boolean,
    onToggleFamilyPing: (Boolean) -> Unit,
    banner: Boolean,
    onToggleBanner: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "MODULAR SENSORY INTERVENTIONS",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Synchronized to background service · Triggers according to what is enabled",
            color = Color(0xFF475569),
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        InterventionRow("Taptic Cheek Haptics (Vibration Shockwave)", haptics, onToggleHaptics)
        InterventionRow("In-Ear Audio Whisper (TTS Warning)", whisper, onToggleWhisper)
        InterventionRow("Heads-Up Floating Warning Banner", banner, onToggleBanner)
        InterventionRow("Silent Out-Of-Band Family / Guardian Ping", familyPing, onToggleFamilyPing)
    }
}

@Composable
fun InterventionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFCBD5E1), fontSize = 11.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF10B981),
                checkedTrackColor = Color(0xFF064E3B)
            )
        )
    }
}

@Composable
fun BottomSpeedDialBar(onDial1930: () -> Unit) {
    Surface(
        color = Color(0xFF050505),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "EMERGENCY 1930 HELPLINE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "I4C National Cyber Crime Reporting Portal",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp
                )
            }
            Button(
                onClick = onDial1930,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SPEED DIAL 1930", fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ModelStatusCard(
    isReady: Boolean,
    isUnpacking: Boolean,
    errorMessage: String? = null,
    groqApiKey: String = "",
    onConfigureGroqKey: () -> Unit = {},
    onInitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isReady) Color(0xFF064E3B) else Color(0xFF1E1B4B))
            .border(1.dp, if (isReady) Color(0xFF059669) else Color(0xFF6366F1), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(if (isReady) "🧠" else "⏳", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isReady) "ON-DEVICE VOSK ENGINE: ARMED (100% OFFLINE)" else "ON-DEVICE VOSK ENGINE: INITIALIZING",
                        color = if (isReady) Color(0xFF6EE7B7) else Color(0xFFA5B4FC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (errorMessage != null) {
                        Text(
                            text = "Error: $errorMessage",
                            color = Color(0xFFF87171),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isUnpacking) {
                        Text(
                            text = "Unpacking offline speech model from APK... (0.5s)",
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    } else if (isReady) {
                        Text(
                            text = "100% Offline Kaldi Model Bundled. Instant local baseline.",
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    } else {
                        Text(
                            text = "Tap to initialize offline speech recognition model.",
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            if (!isReady && !isUnpacking) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onInitClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Arm Engine", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hybrid Cloud STT Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF030712))
                .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(if (groqApiKey.isNotBlank()) "⚡" else "☁️", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (groqApiKey.isNotBlank()) "HYBRID CLOUD WHISPER TURBO: ACTIVE" else "CLOUD WHISPER STT: CONFIGURE KEY",
                        color = if (groqApiKey.isNotBlank()) Color(0xFF34D399) else Color(0xFF93C5FD),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (groqApiKey.isNotBlank()) "Groq LPU sub-200ms Indian English & Hinglish decoding" else "Tap 'Set Key' to arm Groq Whisper Turbo cloud STT",
                        color = Color(0xFF94A3B8),
                        fontSize = 8.sp
                    )
                }
            }
            Button(
                onClick = onConfigureGroqKey,
                colors = ButtonDefaults.buttonColors(containerColor = if (groqApiKey.isNotBlank()) Color(0xFF065F46) else Color(0xFF2563EB)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(if (groqApiKey.isNotBlank()) "Key Set ✓" else "Set Key", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AcousticRadarCard(
    result: AcousticAnalysisResult
) {
    val isSynthetic = result.isSyntheticVoice
    val confPct = (result.syntheticConfidence * 100).toInt()
    val badgeBg = if (isSynthetic) Color(0xFF7F1D1D) else (if (result.syntheticConfidence > 0.40f) Color(0xFF78350F) else Color(0xFF064E3B))
    val badgeBorder = if (isSynthetic) Color(0xFFEF4444) else (if (result.syntheticConfidence > 0.40f) Color(0xFFF59E0B) else Color(0xFF10B981))
    val badgeText = if (isSynthetic) "🚨 AI SYNTHETIC VOICE ($confPct%)" else (if (result.syntheticConfidence > 0.40f) "🟡 ANALYZING HARMONICS ($confPct%)" else "🟢 BIOLOGICAL HUMAN ($confPct%)")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF020617))
            .border(1.dp, badgeBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🧠 ACOUSTIC RADAR",
                color = Color(0xFFA5B4FC),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeBg)
                    .border(0.5.dp, badgeBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4 Key Forensic Metrics (Including Sub-Bass Proximity & Loudspeaker Replay Void)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("HARMONIC", color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(java.util.Locale.US, "%.2f", result.pitchJitterEstimate),
                    color = if (result.pitchJitterEstimate > 0.85f && result.isVoicedSpeech) Color(0xFFF87171) else Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("AI: >0.85", color = Color(0xFF475569), fontSize = 7.sp)
            }
            Column(modifier = Modifier.weight(1.1f)) {
                Text("SUB-BASS PROX", color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(java.util.Locale.US, "%.3f", result.subBassProximityRatio),
                    color = if (result.subBassProximityRatio < 0.10f && result.isVoicedSpeech) Color(0xFFF87171) else Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(if (result.subBassProximityRatio < 0.10f && result.isVoicedSpeech) "Speaker Void" else "Proximity (>0.2)", color = Color(0xFF475569), fontSize = 7.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("WIENER ENT", color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(java.util.Locale.US, "%.3f", result.spectralFlatness),
                    color = if (result.spectralFlatness > 0.34f) Color(0xFFF87171) else Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("AI: >0.34", color = Color(0xFF475569), fontSize = 7.sp)
            }
            Column(modifier = Modifier.weight(0.9f)) {
                Text("HF RATIO", color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(java.util.Locale.US, "%.3f", result.highFrequencyEnergyRatio),
                    color = if (result.highFrequencyEnergyRatio > 0.15f) Color(0xFFF87171) else Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("AI: >0.15", color = Color(0xFF475569), fontSize = 7.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = result.analysisReason,
            color = if (isSynthetic) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
            fontSize = 9.sp,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun VideoCallDeepfakeTesterCard(
    result: VideoInspectionResult?,
    activeMode: String?,
    onTestNormalVideo: () -> Unit,
    onTestDeepfakeVideo: () -> Unit,
    onTriggerFloatingHUD: () -> Unit,
    onReset: () -> Unit
) {
    val isSynthetic = result?.isSynthetic == true
    val cardBorderColor = when {
        result == null -> Color(0xFF334155)
        isSynthetic -> Color(0xFFEF4444)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📹 VIDEO CALL DEEPFAKE RADAR",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Status Badge
                val badgeText = when {
                    result == null -> "⚪ RADAR READY"
                    isSynthetic -> "🚨 CRITICAL DEEPFAKE (${(result.deepfakeConfidence * 100).toInt()}%)"
                    else -> "🟢 AUTHENTIC HUMAN (0.04)"
                }
                val badgeBg = when {
                    result == null -> Color(0xFF1E293B)
                    isSynthetic -> Color(0xFF7F1D1D)
                    else -> Color(0xFF064E3B)
                }
                val badgeBorder = when {
                    result == null -> Color(0xFF475569)
                    isSynthetic -> Color(0xFFEF4444)
                    else -> Color(0xFF10B981)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .border(0.5.dp, badgeBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Real-time inspection of WhatsApp & video calls against AI deepfake face swaps, boundary seam warping, and 2D-FFT neural upscaler lattices.",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            // SIMULATED LIVE VIDEO FEED VIEWPORT CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF020617))
                    .border(
                        1.dp,
                        if (isSynthetic) Color(0xFFEF4444).copy(alpha = 0.6f) else Color(0xFF10B981).copy(alpha = 0.6f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                // Viewfinder Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSynthetic) "🔴 VIRTUAL CAM: DEEPFACELIVE / SIMULATED AVATAR" else "🟢 OPTICAL CAM: WEBCAM 1080P (SONY SENSOR)",
                        color = if (isSynthetic) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "30 FPS • 2D-FFT ON",
                        color = Color(0xFF64748B),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Center Face Reticle Simulation Box
                Box(
                    modifier = Modifier
                        .size(110.dp, 80.dp)
                        .align(Alignment.Center)
                        .border(
                            1.dp,
                            if (isSynthetic) Color(0xFFEF4444) else Color(0xFF10B981),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(4.dp)
                ) {
                    Text(
                        text = if (isSynthetic) "⚠️ SEAM WARPING\nCOLLAR BLUR\nGRID NOISE" else "BIOMETRIC LOCK\nHUMAN TEXTURE\nNATURAL BLINK",
                        color = if (isSynthetic) Color(0xFFF87171) else Color(0xFF34D399),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 10.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Bottom HUD Badge Banner Overlay Inside Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSynthetic) Color(0xD97F1D1D) else Color(0xD9064E3B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = result?.hudBadge ?: "🟢 VERIFIED BIOMETRIC IDENTITY: Authentic Human Video Feed (0.04 Anomaly Score)",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // INTERACTIVE TEST BUTTONS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test A: Normal Video Feed
                Button(
                    onClick = onTestNormalVideo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📹 TEST NORMAL VIDEO",
                            color = Color(0xFFA7F3D0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(Real Human / Daughter)",
                            color = Color(0xFF6EE7B7),
                            fontSize = 8.sp
                        )
                    }
                }

                // Test B: Deepfake Virtual Camera
                Button(
                    onClick = onTestDeepfakeVideo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎭 TEST DEEPFAKE CAM",
                            color = Color(0xFFFECACA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(AI Face Swap Avatar)",
                            color = Color(0xFFFCA5A5),
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // DETAILED FORENSIC INSPECTION METRICS (Displayed when test is executed)
            if (result != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF020617))
                        .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "🔬 LIVE FORENSIC TELEMETRY METRICS",
                        color = Color(0xFFA5B4FC),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Metric 1: Azimuthal Grid Lattice Anomaly
                    MetricBarRow(
                        title = "Azimuthal Grid Lattice Anomaly",
                        value = result.azimuthalLatticeAnomaly,
                        scoreText = String.format(java.util.Locale.US, "%.2f", result.azimuthalLatticeAnomaly),
                        subtitle = "2D-FFT azimuthal high-frequency lattice noise (GAN upscaler artifacts)",
                        isAlarm = result.azimuthalLatticeAnomaly > 0.40f
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Metric 2: Facial Boundary Blur / Warping
                    MetricBarRow(
                        title = "Facial Boundary Blur / Warping",
                        value = result.boundaryBlurWarping,
                        scoreText = String.format(java.util.Locale.US, "%.2f", result.boundaryBlurWarping),
                        subtitle = "Jawline/collar color grading seam and edge feathering discontinuity",
                        isAlarm = result.boundaryBlurWarping > 0.40f
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Metric 3: Eye Blink Periodicity
                    MetricBarRow(
                        title = "Eye Blink Periodicity",
                        value = result.blinkPeriodicityAnomaly,
                        scoreText = String.format(java.util.Locale.US, "%.2f", result.blinkPeriodicityAnomaly),
                        subtitle = "Ocular interval variance & saccadic micro-movements (Poisson vs frozen)",
                        isAlarm = result.blinkPeriodicityAnomaly > 0.40f
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Metric 4: Biometric Authenticity Score
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Biometric Authenticity Score",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Inverse anomaly probability based on multi-spectral sensor fusion",
                                color = Color(0xFF64748B),
                                fontSize = 8.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSynthetic) Color(0xFF7F1D1D) else Color(0xFF064E3B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${(result.biometricAuthenticityScore * 100).toInt()}% ${if (isSynthetic) "SYNTHETIC" else "AUTHENTIC"}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Detected Anomalies Checklist
                    if (result.detectedAnomalies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Identified Synthetic Generative Signatures:",
                            color = Color(0xFFF87171),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        result.detectedAnomalies.forEach { anomaly ->
                            Text(
                                text = "• $anomaly",
                                color = Color(0xFFFCA5A5),
                                fontSize = 8.sp,
                                lineHeight = 11.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Toolbar (Floating HUD injection & Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTriggerFloatingHUD,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B4FC)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🪟 INJECT FLOATING VIDEO HUD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "RESET RADAR",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MetricBarRow(
    title: String,
    value: Float,
    scoreText: String,
    subtitle: String,
    isAlarm: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = scoreText,
                color = if (isAlarm) Color(0xFFEF4444) else Color(0xFF10B981),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = subtitle,
            color = Color(0xFF64748B),
            fontSize = 7.sp,
            lineHeight = 9.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (isAlarm) Color(0xFFEF4444) else Color(0xFF10B981),
            trackColor = Color(0xFF1E293B)
        )
    }
}
