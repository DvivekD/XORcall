package com.xorcall.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.xorcall.engine.GuardianSettingsManager
import com.xorcall.ui.LockdownActivity

/**
 * XORcallAccessibilityService
 * Android Accessibility Service for direct in-call audio output interception and automatic app lockdown.
 * Operates with system-level UI & audio priority:
 * 1. Financial App Lockdown: Freezes sensitive payment/banking apps (PhonePe, GPay, Paytm, BHIM, Telegram)
 *    during active extortion calls or Digital Arrest scenarios to prevent coerced money transfers.
 * 2. In-Call Screen Interception: Elevates hardware audio routing to MODE_IN_COMMUNICATION
 *    to capture both earpiece downlink and mic uplink without manual speakerphone toggling.
 */
class XORcallAccessibilityService : AccessibilityService() {

    private val TAG = "XORcallA11yService"
    private lateinit var audioManager: AudioManager
    private lateinit var settingsManager: GuardianSettingsManager

    companion object {
        var isServiceRunning = false
            private set

        @Volatile
        var isScamCallActive = false
            private set

        fun setScamCallActive(active: Boolean) {
            isScamCallActive = active
            Log.i("XORcallA11yService", "isScamCallActive updated to: $active")
        }
    }

    private val securityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.xorcall.SCAM_ACTIVE_STATE" -> {
                    val active = intent.getBooleanExtra("isActive", false)
                    setScamCallActive(active)
                }
                "com.xorcall.WHITELIST_CALL" -> {
                    // Suppress threat lockdown when legitimate whitelisted call is in progress
                    setScamCallActive(false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        settingsManager = GuardianSettingsManager(this)

        val filter = IntentFilter().apply {
            addAction("com.xorcall.SCAM_ACTIVE_STATE")
            addAction("com.xorcall.WHITELIST_CALL")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(securityReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(securityReceiver, filter)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        broadcastServiceState(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: ""

        // 1. Automatic Financial & UPI App Lockdown Enforcement
        // If an active extortion or Digital Arrest threat is ongoing and the user attempts to open
        // a sensitive financial app (PhonePe, GPay, Paytm, BHIM, Banking apps, Crypto wallets, Telegram),
        // immediately abort execution, return to home, and display the unclosable lockdown screen.
        if (isScamCallActive && settingsManager.isAppLockdownEnabled && PackageWatchdog.isSensitiveFinancialPackage(pkg)) {
            Log.w(TAG, "🚨 SECURITY LOCKDOWN TRIGGERED: Prevented coerced access to $pkg during active scam call!")

            try {
                // Immediate hardware intervention: kick out of the financial application
                performGlobalAction(GLOBAL_ACTION_HOME)

                // Launch unclosable full-screen LockdownActivity
                val appName = PackageWatchdog.getFinancialAppName(pkg)
                LockdownActivity.launch(
                    context = this,
                    packageName = pkg,
                    appName = appName,
                    reason = "Active money extortion / Digital Arrest threat call in progress."
                )

                // Broadcast event for UI telemetry and floating HUD
                val broadcastIntent = Intent("com.xorcall.FINANCIAL_APP_BLOCKED").apply {
                    putExtra("package", pkg)
                    putExtra("appName", appName)
                    setPackage(packageName)
                }
                sendBroadcast(broadcastIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing financial app lockdown", e)
            }
            return
        }

        // 2. Detect Dialer / Phone / In-Call UI & Elevate Duplex Audio Intercept
        val isInCallApp = pkg.contains("dialer", ignoreCase = true) ||
                pkg.contains("incall", ignoreCase = true) ||
                pkg.contains("telecom", ignoreCase = true) ||
                pkg.contains("phone", ignoreCase = true) ||
                pkg.contains("whatsapp", ignoreCase = true)

        if (isInCallApp) {
            try {
                // Elevate hardware audio routing to duplex communication
                if (audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                }

                // Ensure CallMonitorService is running with duplex line capture
                val serviceIntent = Intent(this, CallMonitorService::class.java).apply {
                    putExtra("EXTRA_ACCESSIBILITY_INTERCEPT", true)
                    putExtra("EXTRA_IN_CALL_PKG", pkg)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        broadcastServiceState(false)
        try {
            unregisterReceiver(securityReceiver)
        } catch (_: Exception) {}
    }

    private fun broadcastServiceState(isActive: Boolean) {
        val intent = Intent("com.xorcall.ACCESSIBILITY_STATE_CHANGED").apply {
            putExtra("isActive", isActive)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}
