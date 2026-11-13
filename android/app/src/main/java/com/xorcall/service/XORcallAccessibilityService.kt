package com.xorcall.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.xorcall.engine.GuardianSettingsManager

/**
 * XORcallAccessibilityService
 * Android Accessibility Service for direct in-call audio output interception.
 * Operates with system-level UI & audio priority:
 * 1. Detects incoming and ongoing dialer / in-call screens (Google Dialer, Vivo Dialer, WhatsApp, Skype).
 * 2. Elevates audio routing to MODE_IN_COMMUNICATION to intercept both earpiece downlink and mic uplink.
 * 3. Couples with CallMonitorService to capture the remote caller's audio directly without requiring manual speakerphone.
 */
class XORcallAccessibilityService : AccessibilityService() {

    private lateinit var audioManager: AudioManager
    private lateinit var settingsManager: GuardianSettingsManager

    companion object {
        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        settingsManager = GuardianSettingsManager(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        broadcastServiceState(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: ""
        
        // Detect Dialer / Phone / In-Call UI
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
    }

    private fun broadcastServiceState(isActive: Boolean) {
        val intent = Intent("com.xorcall.ACCESSIBILITY_STATE_CHANGED").apply {
            putExtra("isActive", isActive)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}
