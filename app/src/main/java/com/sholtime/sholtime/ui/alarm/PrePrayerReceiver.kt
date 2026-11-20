package com.sholtime.sholtime.ui.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sholtime.sholtime.MainActivity
import com.sholtime.sholtime.R

class PrePrayerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val namaSholat = intent.getStringExtra("EXTRA_NAMA_SHOLAT") ?: return
        val jamSholat = intent.getStringExtra("EXTRA_JAM_SHOLAT") ?: ""

        createNotificationChannel(context)
        tampilkanNotifikasiPersiapan(context, namaSholat, jamSholat)

        AlarmScheduler(context).jadwalkanPengingat30Menit(namaSholat)
    }

    private fun tampilkanNotifikasiPersiapan(context: Context, namaSholat: String, jamSholat: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val izinNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!izinNotif) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            namaSholat.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val teksInfo = if (jamSholat.isNotBlank()) {
            "Waktu sholat $namaSholat pukul $jamSholat (30 menit lagi)."
        } else {
            "Waktu sholat $namaSholat 30 menit lagi."
        }

        val notification = NotificationCompat.Builder(context, "CHANNEL_PRE_SHOLAT")
            .setSmallIcon(R.drawable.ic_nav_alarm)
            .setContentTitle("30 Menit Menuju Sholat $namaSholat")
            .setContentText(teksInfo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(2000 + namaSholat.hashCode(), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHANNEL_PRE_SHOLAT",
                "Pengingat 30 Menit Sebelum Sholat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pemberitahuan 30 menit sebelum waktu sholat tiba"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}