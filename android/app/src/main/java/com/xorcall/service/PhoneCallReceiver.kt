package com.xorcall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.xorcall.engine.NumberClassificationEngine

/**
 * PhoneCallReceiver
 * Listens for incoming and outgoing telephony state transitions.
 * Directly routes call state to CallMonitorService:
 * - OFFHOOK (Call Connected): Instantly triggers direct duplex call audio capture.
 * - IDLE (Call Ended): Returns audio engine to standby.
 */
class PhoneCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            val hasAudio = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasAudio) return

            if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                // Call answered / connected -> Engage active direct in-call audio capture
                val classification = NumberClassificationEngine.classifyNumber(context, incomingNumber)
                val serviceIntent = Intent(context, CallMonitorService::class.java).apply {
                    putExtra("EXTRA_CALL_STATE", "OFFHOOK")
                    putExtra("EXTRA_INCOMING_NUMBER", classification.rawNumber)
                    putExtra("EXTRA_CLASSIFICATION", classification.category.name)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val classification = NumberClassificationEngine.classifyNumber(context, incomingNumber)
                val serviceIntent = Intent(context, CallMonitorService::class.java).apply {
                    putExtra("EXTRA_CALL_STATE", "RINGING")
                    putExtra("EXTRA_INCOMING_NUMBER", classification.rawNumber)
                    putExtra("EXTRA_CLASSIFICATION", classification.category.name)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended: Notify service of call completion without tearing down the protective shield
                val idleIntent = Intent(context, CallMonitorService::class.java).apply {
                    putExtra("EXTRA_CALL_STATE", "IDLE")
                }
                try {
                    context.startService(idleIntent)
                } catch (_: Exception) {}
            }
        }
    }
}
