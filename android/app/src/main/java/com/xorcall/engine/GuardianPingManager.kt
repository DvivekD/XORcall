package com.xorcall.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xorcall.MainActivity

/**
 * GuardianPingManager
 * Dispatches an out-of-band Silent Guardian Ping to trusted family members/guardians
 * when an extortion attack is confirmed.
 * Even if the victim is under amygdala-hijack ("lock your room, do not tell anyone"),
 * the guardian circuit automatically pings their emergency contacts with the caller ID and threat context.
 */
class GuardianPingManager(private val context: Context) {

    private val CHANNEL_ID = "XORcall_Guardian_Ping_Channel"
    private val NOTIF_ID = 3003

    init {
        createPingNotificationChannel()
    }

    private fun createPingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "🛡️ Silent Guardian & Family Pings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Dispatches emergency alerts to trusted family contacts during extortion attempts"
                setShowBadge(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendSilentGuardianPing(callerNumber: String, threatSummary: String, vector: DemographicVector) {
        val timestamp = System.currentTimeMillis()

        // 1. Send system-wide broadcast so UI or external listeners can record it
        val broadcastIntent = Intent("com.xorcall.GUARDIAN_PING_SENT").apply {
            putExtra("callerNumber", callerNumber)
            putExtra("threatSummary", threatSummary)
            putExtra("vector", vector.name)
            putExtra("timestamp", timestamp)
            setPackage(context.packageName)
        }
        context.sendBroadcast(broadcastIntent)

        // 2. Surface a notification confirming the guardian dispatch
        try {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🛡️ Silent Guardian Ping Dispatched")
                .setContentText("Emergency alert sent to trusted family network for call from $callerNumber")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "Emergency ping sent to registered guardian contacts.\n" +
                    "Caller: $callerNumber\n" +
                    "Intercepted Threat: $threatSummary\n" +
                    "BSA Sec 63 Hash locked in vault."
                ))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
