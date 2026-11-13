package com.xorcall

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
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
import com.xorcall.ui.FloatingAlertOverlay
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
                    }
                )
            }
        }
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
    onRequestOverlay: () -> Unit
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
    var isAiVoiceDetectionArmed by remember { mutableStateOf(settingsManager.isAiVoiceDetectionArmed) }

    var liveTranscript by remember { mutableStateOf("Ready. Monitoring unknown callers for extortion & AI clones...") }
    var lastThreatMessage by remember { mutableStateOf<String?>(null) }
    var lastAiVoiceStatus by remember { mutableStateOf<String?>(null) }
    var dossierPreviewDialog by remember { mutableStateOf<String?>(null) }

    val callRecords = remember { mutableStateListOf<CallAuditRecord>() }

    fun refreshHistory() {
        callRecords.clear()
        callRecords.addAll(historyManager.getAllRecords())
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    // Voice recognition launcher for live microphone speech testing
    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                liveTranscript = "“$spokenText”"
                val analysis = sttManager.analyzeText(spokenText, activeVector)
                if (analysis.isScamDetected) {
                    riskLevel = analysis.riskLevel
                    lastThreatMessage = analysis.warningMessage
                    if (isHapticsEnabled) {
                        hapticEngine.triggerImmediateShockwave(analysis.riskLevel)
                    }
                    if (isBannerEnabled) {
                        floatingOverlay.showCriticalAlert(analysis.warningMessage)
                    }
                    if (isAudioWhisperEnabled) {
                        whisperEngine.speakWarning(analysis.warningMessage)
                    }
                    if (isFamilyPingEnabled) {
                        guardianPingManager.sendSilentGuardianPing(
                            callerNumber = "Live Voice Mic Test",
                            threatSummary = analysis.warningMessage,
                            vector = analysis.demographicVector
                        )
                    }
                    historyManager.recordCall(
                        callerNumber = "Live Voice Mic Test",
                        callerClassification = "MICROPHONE_TEST",
                        vector = analysis.demographicVector,
                        risk = analysis.riskLevel,
                        isSynthetic = analysis.isSyntheticVoice,
                        syntheticConfidence = analysis.syntheticConfidence,
                        keywords = analysis.matchedKeywords,
                        transcript = spokenText
                    )
                    refreshHistory()
                } else {
                    riskLevel = RiskLevel.SAFE
                    Toast.makeText(context, "🟢 Normal speech: No extortion markers detected.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Dynamic Broadcast Receiver for incoming telephony events
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
                    "com.xorcall.AI_VOICE_ALERT" -> {
                        val reason = intent.getStringExtra("reason") ?: "Synthetic Neural Vocoder Detected"
                        lastAiVoiceStatus = reason
                    }
                    "com.xorcall.WHITELIST_CALL" -> {
                        val entity = intent.getStringExtra("entity") ?: "Trusted Contact"
                        val number = intent.getStringExtra("number") ?: ""
                        liveTranscript = "🟢 Whitelist Call Passed: $entity ($number)"
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("com.xorcall.THREAT_ALERT")
            addAction("com.xorcall.TRANSCRIPT_UPDATE")
            addAction("com.xorcall.AI_VOICE_ALERT")
            addAction("com.xorcall.WHITELIST_CALL")
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

            // SPEAKERPHONE TESTING ADVISORY
            item {
                SpeakerphoneTipCard()
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
                    onToggle = {
                        val nextState = !isShieldActive
                        isShieldActive = nextState
                        onToggleShield(nextState)
                    }
                )
            }

            // SMART CALL WHITELISTING & VERIFIED ENTITIES CARD
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
                    aiVoiceArmed = isAiVoiceDetectionArmed,
                    onToggleAiVoice = {
                        isAiVoiceDetectionArmed = it
                        settingsManager.isAiVoiceDetectionArmed = it
                    }
                )
            }

            // LIVE DEMOGRAPHIC THREAT TESTER SUITE
            item {
                DemographicThreatTesterCard(
                    activeVector = activeVector,
                    liveTranscript = liveTranscript,
                    aiVoiceStatus = lastAiVoiceStatus,
                    onVoiceTestClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak extortion phrase (Actor Roleplay)...")
                            }
                            voiceInputLauncher.launch(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onTriggerUniversalExtortion = {
                        val sample = "We have seized your records. Wire 5 million dollar extraction immediately or face severe consequences and arrest."
                        liveTranscript = "“$sample”"
                        val analysis = sttManager.analyzeText(sample, activeVector)
                        riskLevel = RiskLevel.CRITICAL
                        lastThreatMessage = analysis.warningMessage
                        if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                        if (isBannerEnabled) floatingOverlay.showCriticalAlert(analysis.warningMessage)
                        if (isAudioWhisperEnabled) whisperEngine.speakWarning(analysis.warningMessage)
                        if (isFamilyPingEnabled) guardianPingManager.sendSilentGuardianPing("+1 800 555 0199", analysis.warningMessage, activeVector)
                        historyManager.recordCall("+1 800 555 0199", "EXTORTION_RANSOM_DEMAND", analysis.demographicVector, RiskLevel.CRITICAL, false, 0f, analysis.matchedKeywords, sample)
                        refreshHistory()
                    },
                    onTriggerStudentScam = {
                        val sample = "Telegram part-time review job: Like YouTube videos to earn ₹5,000. Balance frozen! Recharge crypto wallet with ₹40,000 USDT to release payout."
                        liveTranscript = "“$sample”"
                        val analysis = sttManager.analyzeText(sample, DemographicVector.STUDENT)
                        riskLevel = RiskLevel.CRITICAL
                        lastThreatMessage = analysis.warningMessage
                        if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                        if (isBannerEnabled) floatingOverlay.showCriticalAlert(analysis.warningMessage)
                        if (isAudioWhisperEnabled) whisperEngine.speakWarning(analysis.warningMessage)
                        if (isFamilyPingEnabled) guardianPingManager.sendSilentGuardianPing("+91 98765 43210", analysis.warningMessage, DemographicVector.STUDENT)
                        historyManager.recordCall("+91 98765 43210", "UNKNOWN_TASK_SCAMMER", DemographicVector.STUDENT, RiskLevel.CRITICAL, false, 0f, analysis.matchedKeywords, sample)
                        refreshHistory()
                    },
                    onTriggerSeniorScam = {
                        val sample = "Supreme Court digital arrest warrant issued! Mumbai Police CBI narcotics seized parcel. Liquidate fixed deposit and transfer ₹8 Lakhs to secret RBI escrow account now."
                        liveTranscript = "“$sample”"
                        val analysis = sttManager.analyzeText(sample, DemographicVector.SENIOR)
                        riskLevel = RiskLevel.CRITICAL
                        lastThreatMessage = analysis.warningMessage
                        if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                        if (isBannerEnabled) floatingOverlay.showCriticalAlert(analysis.warningMessage)
                        if (isAudioWhisperEnabled) whisperEngine.speakWarning(analysis.warningMessage)
                        if (isFamilyPingEnabled) guardianPingManager.sendSilentGuardianPing("+92 300 1234567", analysis.warningMessage, DemographicVector.SENIOR)
                        historyManager.recordCall("+92 300 1234567", "HIGH_RISK_DIGITAL_ARREST", DemographicVector.SENIOR, RiskLevel.CRITICAL, false, 0f, analysis.matchedKeywords, sample)
                        refreshHistory()
                    },
                    onTriggerProfessionalScam = {
                        val sample = "FedEx Customer Support: Illegal parcel from Mumbai to Taiwan seized with 5 expired passports and 140g MDMA. TRAI will disconnect your mobile number in 2 hours."
                        liveTranscript = "“$sample”"
                        val analysis = sttManager.analyzeText(sample, DemographicVector.PROFESSIONAL)
                        riskLevel = RiskLevel.CRITICAL
                        lastThreatMessage = analysis.warningMessage
                        if (isHapticsEnabled) hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                        if (isBannerEnabled) floatingOverlay.showCriticalAlert(analysis.warningMessage)
                        if (isAudioWhisperEnabled) whisperEngine.speakWarning(analysis.warningMessage)
                        if (isFamilyPingEnabled) guardianPingManager.sendSilentGuardianPing("+44 70 98765432", analysis.warningMessage, DemographicVector.PROFESSIONAL)
                        historyManager.recordCall("+44 70 98765432", "VOIP_FEDEX_IMPOSTER", DemographicVector.PROFESSIONAL, RiskLevel.CRITICAL, false, 0f, analysis.matchedKeywords, sample)
                        refreshHistory()
                    },
                    onTriggerAiCloneScam = {
                        val sample = "Papa! I had an accident on the highway, doctor says leg fractured, police putting me in lockup! Inspector wants ₹50,000 bail money right now!"
                        liveTranscript = "“$sample”"
                        lastAiVoiceStatus = "⚠️ Neural Vocoder High-Frequency Roll-Off Detected (94% AI Clone Confidence)"
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
fun SpeakerphoneTipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E1B4B))
            .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔊", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "IN-CALL AUDIO TIP (VIVO / ANDROID 14)",
                color = Color(0xFFA5B4FC),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "When your friend calls, keep the call on Speakerphone (Hands-Free) so the microphone ingests and transcribes both sides of the conversation in real-time.",
                color = Color(0xFFE0E7FF),
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
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
    aiVoiceArmed: Boolean,
    onToggleAiVoice: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B132B))
            .border(1.dp, Color(0xFF1C2A4F), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🛡️", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SMART WHITELIST & CALL CLASSIFICATION",
                color = Color(0xFF60A5FA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Text(
            text = "Mandatory audio monitoring on unknown calls. Bypasses trusted entities automatically.",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        InterventionRow("Whitelist Saved Contacts (Friends / Manager)", whitelistContacts, onToggleWhitelistContacts)
        InterventionRow("Auto-Allow Verified Banks & Police (1800 SBI/HDFC, 112)", allowOfficialBanks, onToggleAllowOfficialBanks)
        InterventionRow("Neural AI Voice Clone Decoding (8kHz Shelf & Jitter)", aiVoiceArmed, onToggleAiVoice)
    }
}

@Composable
fun DemographicThreatTesterCard(
    activeVector: DemographicVector,
    liveTranscript: String,
    aiVoiceStatus: String?,
    onVoiceTestClick: () -> Unit,
    onTriggerUniversalExtortion: () -> Unit,
    onTriggerStudentScam: () -> Unit,
    onTriggerSeniorScam: () -> Unit,
    onTriggerProfessionalScam: () -> Unit,
    onTriggerAiCloneScam: () -> Unit,
    onTriggerHapticsOnly: () -> Unit,
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
            Text(
                text = "INTERCEPT CONSOLE & ACTOR ROLEPLAY",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF064E3B))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(activeVector.name, color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Transcript Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF030712))
                .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(8.dp))
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

        Spacer(modifier = Modifier.height(10.dp))

        // Live Voice Mic Test
        Button(
            onClick = onVoiceTestClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("🎙️ Speak Live Voice (Actor Roleplay Mic)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 1: Universal 5M Extortion + Student
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onTriggerUniversalExtortion,
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("💰 5M $ Extort", fontSize = 9.sp, color = Color(0xFFFECACA), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onTriggerStudentScam,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text("👨‍🎓 Student Task", fontSize = 9.sp, color = Color(0xFFFECACA), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Senior CBI + Pro FedEx
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                    text = "${records.size} Monitored Calls with SHA-256 Hashes",
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
                text = "v1.0.4-PROD",
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
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
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
                    text = if (isActive) risk.name else "OFFLINE",
                    color = orbColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isActive) "Armed: Auto-monitoring unknown callers" else "Tap to arm real-time defense",
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
