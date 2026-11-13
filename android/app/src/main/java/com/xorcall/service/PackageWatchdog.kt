package com.xorcall.service

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * PackageWatchdog
 * Detects if a remote screen-sharing or remote desktop trojan (AnyDesk, TeamViewer, RustDesk)
 * is opened while a high-risk scam call is active.
 */
class PackageWatchdog(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val blacklistedRemotePackages = setOf(
        "com.anydesk.anydeskandroid",
        "com.teamviewer.quicksupport.market",
        "com.teamviewer.host.market",
        "com.carriez.rustdesk",
        "com.google.chromeremotedesktop",
        "com.splashtop.remote"
    )

    fun checkActiveForegroundPackage(): String? {
        val time = System.currentTimeMillis()
        val stats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - TimeUnit.SECONDS.toMillis(10),
            time
        ) ?: return null

        var currentApp: String? = null
        var lastTimeUsed: Long = 0

        for (pkg in stats) {
            if (pkg.lastTimeUsed > lastTimeUsed) {
                currentApp = pkg.packageName
                lastTimeUsed = pkg.lastTimeUsed
            }
        }
        return currentApp
    }

    fun isDangerousAppInForeground(): Boolean {
        val currentApp = checkActiveForegroundPackage() ?: return false
        return blacklistedRemotePackages.contains(currentApp)
    }
}
