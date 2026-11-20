package com.sholtime.sholtime.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "Sholtime:AlarmWakeLock"
        )
        wakeLock.acquire(20_000L)

        val namaSholat = intent.getStringExtra("EXTRA_NAMA_SHOLAT") ?: "Subuh"
        val snoozeCount = intent.getIntExtra("EXTRA_SNOOZE_COUNT", 0)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("EXTRA_NAMA_SHOLAT", namaSholat)
            putExtra("EXTRA_SNOOZE_COUNT", snoozeCount)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}