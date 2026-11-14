package com.xorcall.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xorcall.service.PackageWatchdog
import com.xorcall.service.XORcallAccessibilityService
import kotlin.math.roundToInt

/**
 * LockdownActivity
 * High-priority unclosable full-screen defense activity triggered when
 * an active money extortion or Digital Arrest scam call is in progress
 * and the user (or victim under coercion) attempts to launch sensitive
 * financial or UPI payment applications (PhonePe, GPay, Paytm, BHIM, Banking apps, Crypto wallets, Telegram).
 *
 * Implements:
 * 1. Immediate payment freeze: prevents coerced fund transfers and OTP leakage.
 * 2. Unclosable modal shield overriding Back navigation to prevent returning to the victim's payment app.
 * 3. 1-Tap National Cybercrime Helpline (1930 / I4C) speed dialer.
 * 4. Emergency Bypass Slider requiring deliberate, conscious gesture to unfreeze in authentic emergencies.
 */
class LockdownActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_BLOCKED_APP_NAME = "extra_blocked_app_name"
        const val EXTRA_THREAT_REASON = "extra_threat_reason"

        fun launch(context: Context, packageName: String, appName: String, reason: String? = null) {
            val intent = Intent(context, LockdownActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_BLOCKED_PACKAGE, packageName)
                putExtra(EXTRA_BLOCKED_APP_NAME, appName)
                putExtra(EXTRA_THREAT_REASON, reason)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Elevate priority over lockscreen and keep screen turned on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: "com.phonepe.app"
        val blockedAppName = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: PackageWatchdog.getFinancialAppName(blockedPackage)
        val threatReason = intent.getStringExtra(EXTRA_THREAT_REASON) ?: "Active extortion / Digital Arrest threat detected on ongoing call."

        setContent {
            LockdownScreen(
                blockedPackage = blockedPackage,
                blockedAppName = blockedAppName,
                threatReason = threatReason,
                onDial1930 = { dial1930() },
                onEmergencyBypass = { bypassLockdown() },
                onReturnHome = { returnToHomeScreen() }
            )
        }
    }

    private fun dial1930() {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:1930")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(dialIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Dialing 1930 Helpline...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bypassLockdown() {
        // Disarm active scam lockdown state
        XORcallAccessibilityService.setScamCallActive(false)
        PackageWatchdog.setScamCallActive(false)

        val unfreezeIntent = Intent("com.xorcall.SCAM_ACTIVE_STATE").apply {
            putExtra("isActive", false)
            putExtra("bypassedByVictim", true)
            setPackage(packageName)
        }
        sendBroadcast(unfreezeIntent)

        Toast.makeText(this, "Emergency Bypass Activated. Financial apps unfrozen.", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun returnToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun LockdownScreen(
    blockedPackage: String,
    blockedAppName: String,
    threatReason: String,
    onDial1930: () -> Unit,
    onEmergencyBypass: () -> Unit,
    onReturnHome: () -> Unit
) {
    // Intercept Back button so coerced victim cannot simply press back and resume payment
    BackHandler {
        onReturnHome()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lockdownPulse")
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030305))
            .border(4.dp, Color(0xFFEF4444).copy(alpha = pulseBorderAlpha))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Top Warning Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF7F1D1D))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🚨 KAVACH CRITICAL DEFENSE INTERVENTION",
                    color = Color(0xFFFCA5A5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            // Shield Icon & Title
            Text(
                text = "🛡️",
                fontSize = 54.sp
            )

            Text(
                text = "FINANCIAL APPS FROZEN",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "An active high-risk scam call is in progress. To prevent coerced extortion money transfers, financial & payment apps are temporarily locked.",
                color = Color(0xFFE2E8F0),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            // Blocked App Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E0A0A))
                    .border(1.dp, Color(0xFF991B1B), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "INTERCEPTED PAYMENT APP",
                            color = Color(0xFFF87171),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = blockedAppName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = blockedPackage,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDC2626))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("BLOCKED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color(0xFF7F1D1D), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "⚠️ Threat Advisory: Police, CBI, ED, and Supreme Court NEVER place citizens under \"Digital Arrest\" via video call or demand funds into \"RBI Escrow Accounts\". Never share OTPs or approve UPI mandates.",
                    color = Color(0xFFFECACA),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            // Primary Emergency Action: 1-Tap 1930 Speed Dialer
            Button(
                onClick = onDial1930,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📞", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DIAL 1930 CYBER HELPLINE NOW",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Return to Home Screen Button
            OutlinedButton(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Text(
                    text = "🏠 Return to Home Screen (Safe Exit)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Emergency Bypass Slider
            EmergencyBypassSlider(
                onBypassConfirmed = onEmergencyBypass
            )
        }
    }
}

/**
 * Interactive Emergency Bypass Slider
 * Requires a conscious, deliberate drag from left to right to unlock apps in genuine emergencies.
 */
@Composable
fun EmergencyBypassSlider(
    onBypassConfirmed: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val trackWidthDp = 280.dp
    val thumbWidthDp = 50.dp
    val density = LocalDensity.current
    val maxOffsetPx = with(density) { (trackWidthDp - thumbWidthDp).toPx() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EMERGENCY BYPASS (LEGITIMATE USER OVERRIDE)",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(trackWidthDp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track Background Text
            Text(
                text = "SLIDE TO UNFREEZE ➔",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Draggable Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (offsetX >= maxOffsetPx * 0.85f) {
                                    offsetX = maxOffsetPx
                                    onBypassConfirmed()
                                } else {
                                    offsetX = 0f
                                }
                            },
                            onDragCancel = {
                                offsetX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxOffsetPx)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔓",
                    fontSize = 18.sp
                )
            }
        }
    }
}
