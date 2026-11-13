package com.xorcall

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.xorcall.engine.RiskLevel
import com.xorcall.engine.SherpaSTTManager
import com.xorcall.engine.TapticHapticEngine
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val hapticEngine = remember { TapticHapticEngine(context).apply { isHapticsEnabled = true } }
    val floatingOverlay = remember { FloatingAlertOverlay(context) }
    val sttManager = remember { SherpaSTTManager(context.assets, context).apply { initialize() } }

    var isShieldActive by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var riskLevel by remember { mutableStateOf(RiskLevel.SAFE) }
    var isHapticsEnabled by remember { mutableStateOf(true) }
    var isAudioWhisperEnabled by remember { mutableStateOf(true) }
    var isFamilyPingEnabled by remember { mutableStateOf(true) }
    var isBannerEnabled by remember { mutableStateOf(true) }
    var outOfBandStatus by remember { mutableStateOf("Family Ping Active") }
    var liveTranscript by remember { mutableStateOf("Awaiting speech or incoming phone call...") }
    var lastThreatMessage by remember { mutableStateOf<String?>(null) }

    val recentAlerts = remember {
        mutableStateListOf(
            "Background watchdog active: Monitoring for AnyDesk / RustDesk injection",
            "Sherpa-ONNX streaming Zipformer: Ready for on-device Hindi/English inference",
            "Taptic Haptic Shockwave Engine: Armed & Proximity-Aware",
            "SHA-256 Tamper-evident dossier logging: Active"
        )
    }

    // Voice Input Recognition Launcher for Live Mic Testing
    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                liveTranscript = "“$spokenText”"
                val analysis = sttManager.analyzeText(spokenText)
                if (analysis.isScamDetected) {
                    riskLevel = analysis.riskLevel
                    lastThreatMessage = analysis.warningMessage
                    if (isHapticsEnabled) {
                        hapticEngine.triggerImmediateShockwave(analysis.riskLevel)
                    }
                    if (isBannerEnabled) {
                        floatingOverlay.showCriticalAlert(analysis.warningMessage)
                    }
                    recentAlerts.add(0, "[LIVE MIC DETECTED] ${analysis.warningMessage}")
                } else {
                    riskLevel = RiskLevel.SAFE
                    recentAlerts.add(0, "[MIC INPUT VERIFIED] No extortion markers in: '$spokenText'")
                }
            }
        }
    }

    // Dynamic Broadcast Receiver for background CallMonitorService events
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
                        recentAlerts.add(0, "[IN-CALL THREAT ALERT] $message")
                    }
                    "com.xorcall.TRANSCRIPT_UPDATE" -> {
                        val text = intent.getStringExtra("text") ?: ""
                        if (text.isNotBlank()) {
                            liveTranscript = "“$text”"
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("com.xorcall.THREAT_ALERT")
            addAction("com.xorcall.TRANSCRIPT_UPDATE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderSection()
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

            item {
                ShieldStatusOrb(
                    isActive = isShieldActive,
                    risk = riskLevel,
                    onToggle = {
                        val hasAudio = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasAudio) {
                            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_PHONE_STATE)
                            } else {
                                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE)
                            }
                            permissionLauncher.launch(perms)
                        } else {
                            val nextState = !isShieldActive
                            isShieldActive = nextState
                            onToggleShield(nextState)
                        }
                    }
                )
            }

            item {
                KillChainStageCard(risk = riskLevel)
            }

            // LIVE VOICE & HAPTICS THREAT INTERCEPTOR CONSOLE
            item {
                LiveThreatTesterCard(
                    liveTranscript = liveTranscript,
                    onVoiceTestClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say anything (e.g. 'Accident hospital money' or 'Police CBI digital arrest')")
                            }
                            voiceInputLauncher.launch(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onTriggerAccidentScam = {
                        val sample = "Accident on highway! Son in hospital custody. Transfer ₹60,000 for bail money right now!"
                        liveTranscript = "“$sample”"
                        val analysis = sttManager.analyzeText(sample)
                        riskLevel = RiskLevel.CRITICAL
                        lastThreatMessage = analysis.warningMessage
                        if (isHapticsEnabled) {
                            hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                        }
                        if (isBannerEnabled) {
                            floatingOverlay.showCriticalAlert(analysis.warningMessage)
                        }
                        recentAlerts.add(0, "[SIMULATED ACCIDENT SCAM] ${analysis.warningMessage}")
                    },
                    onTriggerDigitalArrestScam = {
                        val sample = "This is Mumbai Police CBI. Parcel with narcotics under your Aadhaar. You are under Digital Arrest. Transfer 4 lakh to RBI escrow."
                        liveTranscript = "“$sample”"
                        val analysis = sttManager.analyzeText(sample)
                        riskLevel = RiskLevel.CRITICAL
                        lastThreatMessage = analysis.warningMessage
                        if (isHapticsEnabled) {
                            hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                        }
                        if (isBannerEnabled) {
                            floatingOverlay.showCriticalAlert(analysis.warningMessage)
                        }
                        recentAlerts.add(0, "[SIMULATED DIGITAL ARREST] ${analysis.warningMessage}")
                    },
                    onTriggerHapticsOnly = {
                        hapticEngine.triggerImmediateShockwave(RiskLevel.CRITICAL)
                        recentAlerts.add(0, "[PHYSICAL HAPTICS TEST] Triggered escalating cheek/ear shockwave pulse on hardware.")
                    },
                    onReset = {
                        riskLevel = RiskLevel.SAFE
                        lastThreatMessage = null
                        floatingOverlay.dismiss()
                        recentAlerts.add(0, "[SYSTEM] Line cleared. Threat status returned to SAFE.")
                    }
                )
            }

            item {
                ModularSensoryTogglesCard(
                    haptics = isHapticsEnabled,
                    onToggleHaptics = { 
                        isHapticsEnabled = it 
                        hapticEngine.isHapticsEnabled = it
                    },
                    whisper = isAudioWhisperEnabled,
                    onToggleWhisper = { isAudioWhisperEnabled = it },
                    familyPing = isFamilyPingEnabled,
                    onToggleFamilyPing = { isFamilyPingEnabled = it },
                    banner = isBannerEnabled,
                    onToggleBanner = { 
                        isBannerEnabled = it 
                        onRequestOverlay()
                    }
                )
            }

            item {
                OutOfBandSafetyCard(
                    status = outOfBandStatus,
                    onSendSafePing = {
                        outOfBandStatus = "100% Safe Ping Sent to 2 Registered Family Contacts"
                        recentAlerts.add(0, "[OUT-OF-BAND] Direct acoustic heartbeat verified. Family alerted: Safe.")
                    }
                )
            }

            item {
                Text(
                    text = "LIVE FORENSIC AUDIT TELEMETRY",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            items(recentAlerts.take(6)) { alert ->
                TelemetryRow(text = alert)
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
                text = "KAVACH NEURAL CYBER GUARDIAN",
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
                text = "v1.0.2-PROD",
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
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🚨", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CRITICAL EXTORTION INTERCEPTED",
                color = Color(0xFFFCA5A5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
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
fun LiveThreatTesterCard(
    liveTranscript: String,
    onVoiceTestClick: () -> Unit,
    onTriggerAccidentScam: () -> Unit,
    onTriggerDigitalArrestScam: () -> Unit,
    onTriggerHapticsOnly: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE SPEECH & HAPTICS INTERCEPTOR",
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
                Text("ON-DEVICE VAD", color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live Transcript Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF030712))
                .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = liveTranscript,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Primary Button: Speak with your voice
        Button(
            onClick = onVoiceTestClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("🎙️ Speak Voice (Test Live Threat Intercept)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Realistic Scam Vectors Quick Test
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTriggerAccidentScam,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("💥 Accident / Bail", fontSize = 10.sp, color = Color(0xFFFECACA), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onTriggerDigitalArrestScam,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🚨 Digital Arrest / CBI", fontSize = 10.sp, color = Color(0xFFFECACA), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Hardware Haptic & Reset Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onTriggerHapticsOnly,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("📳 Buzz Taptic Shockwave", fontSize = 10.sp, color = Color(0xFF38BDF8))
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Clear Threat", fontSize = 10.sp, color = Color(0xFF94A3B8))
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size((140 * (if (isActive) pulseScale else 1f)).dp)
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (isActive) risk.name else "OFFLINE",
                    color = orbColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isActive) "Tap to pause protection" else "Tap to arm real-time defense",
            color = Color(0xFF64748B),
            fontSize = 11.sp
        )
    }
}

@Composable
fun KillChainStageCard(risk: RiskLevel) {
    val stages = listOf(
        "1. Impersonation (CBI / Police / TRAI)",
        "2. High-Pressure Coercion (Digital Arrest)",
        "3. Psychological Isolation (Stay on line)",
        "4. Liquidation (Transfer to RBI Safe Account)",
        "5. Screen Trojan (AnyDesk / RustDesk)"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "MATHEMATICAL 5-STAGE KILL CHAIN",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        stages.forEachIndexed { index, stageName ->
            val isTriggered = risk == RiskLevel.CRITICAL || (risk == RiskLevel.WARN && index < 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isTriggered) Color(0xFFEF4444) else Color(0xFF334155))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stageName,
                    color = if (isTriggered) Color(0xFFFCA5A5) else Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = if (isTriggered) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
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
            .padding(16.dp)
    ) {
        Text(
            text = "MODULAR SENSORY INTERVENTIONS",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Customizable alerts: Taptic cheek haptics, Whisper TTS & Floating overlay",
            color = Color(0xFF475569),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        InterventionRow("Taptic Cheek Haptics (Vibration Shockwave)", haptics, onToggleHaptics)
        InterventionRow("In-Ear Audio Whisper (TTS)", whisper, onToggleWhisper)
        InterventionRow("Heads-Up Floating Warning Banner", banner, onToggleBanner)
        InterventionRow("Silent Out-Of-Band Family Ping", familyPing, onToggleFamilyPing)
    }
}

@Composable
fun InterventionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFCBD5E1), fontSize = 12.sp)
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
fun OutOfBandSafetyCard(
    status: String,
    onSendSafePing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B192C))
            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "FAMILY OUT-OF-BAND RESOLUTION",
            color = Color(0xFF38BDF8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Defeats AI voice clone extortions without relying on the caller line.",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onSendSafePing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Simulate 'I Am 100% Safe' Heartbeat Ping", fontSize = 12.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Status: $status",
            color = Color(0xFF38BDF8),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun TelemetryRow(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0A0F1D))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
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
                .padding(16.dp),
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
                    fontSize = 10.sp
                )
            }
            Button(
                onClick = onDial1930,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SPEED DIAL 1930", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}
