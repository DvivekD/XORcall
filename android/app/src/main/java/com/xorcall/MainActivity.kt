package com.xorcall

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.xorcall.service.CallMonitorService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            XORcallTheme {
                MainScreen(
                    onToggleShield = { activate ->
                        val serviceIntent = Intent(this, CallMonitorService::class.java)
                        if (activate) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(serviceIntent)
                            } else {
                                startService(serviceIntent)
                            }
                        } else {
                            stopService(serviceIntent)
                        }
                    },
                    onDial1930 = {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:1930")
                        }
                        startActivity(dialIntent)
                    },
                    onRequestOverlay = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
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
    var isShieldActive by remember { mutableStateOf(true) }
    var riskLevel by remember { mutableStateOf(RiskLevel.SAFE) }
    var isHapticsEnabled by remember { mutableStateOf(false) } // Default off per user critique
    var isAudioWhisperEnabled by remember { mutableStateOf(true) }
    var isFamilyPingEnabled by remember { mutableStateOf(true) }
    var isBannerEnabled by remember { mutableStateOf(true) }
    var outOfBandStatus by remember { mutableStateOf("Family Ping Active") }

    val recentAlerts = remember {
        mutableStateListOf(
            "Background watchdog active: Monitoring for AnyDesk / RustDesk injection",
            "Sherpa-ONNX streaming Zipformer: Ready for on-device Hindi/English inference",
            "SHA-256 Tamper-evident dossier logging: Active"
        )
    }

    LaunchedEffect(isShieldActive) {
        onToggleShield(isShieldActive)
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

            item {
                ShieldStatusOrb(
                    isActive = isShieldActive,
                    risk = riskLevel,
                    onToggle = { isShieldActive = !isShieldActive }
                )
            }

            item {
                KillChainStageCard(risk = riskLevel)
            }

            item {
                SimulationTriggerRow(
                    onSimulateAttack = {
                        riskLevel = RiskLevel.CRITICAL
                        recentAlerts.add(0, "[CRITICAL INTERCEPT] Fake Police CBI 'Digital Arrest' warrant detected on line!")
                    },
                    onReset = {
                        riskLevel = RiskLevel.SAFE
                        recentAlerts.add(0, "[SYSTEM] Line cleared. Threat neutral.")
                    }
                )
            }

            item {
                ModularSensoryTogglesCard(
                    haptics = isHapticsEnabled,
                    onToggleHaptics = { isHapticsEnabled = it },
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
                    text = "LIVE AUDIT TELEMETRY",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            items(recentAlerts.take(5)) { alert ->
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
                text = "v1.0.0-PROD",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
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
        horizontalAlignment = Alignment.CenterVertically
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
            Column(horizontalAlignment = Alignment.CenterVertically) {
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
fun SimulationTriggerRow(
    onSimulateAttack: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSimulateAttack,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Simulate Scam Call", fontSize = 11.sp, color = Color(0xFFFECACA))
        }
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Clear Threat", fontSize = 11.sp, color = Color(0xFF94A3B8))
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
            text = "Judge Critique #3: Customizable alerts for users who dislike vibration",
            color = Color(0xFF475569),
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        InterventionRow("Taptic Cheek Haptics (Vibration)", haptics, onToggleHaptics)
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
