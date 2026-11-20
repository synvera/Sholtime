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

class PrayerArrivedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val namaSholat = intent.getStringExtra("EXTRA_NAMA_SHOLAT") ?: return
        val jamSholat = intent.getStringExtra("EXTRA_JAM_SHOLAT") ?: ""

        createNotificationChannel(context)
        tampilkanNotifikasiWaktuTiba(context, namaSholat, jamSholat)

        AlarmScheduler(context).jadwalkanNotifWaktuTiba(namaSholat)
    }

    private fun tampilkanNotifikasiWaktuTiba(context: Context, namaSholat: String, jamSholat: String) {
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
            3000 + namaSholat.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val teksInfo = if (jamSholat.isNotBlank()) {
            "Waktu sholat $namaSholat pukul $jamSholat telah masuk."
        } else {
            "Waktu sholat $namaSholat telah masuk."
        }

        val notification = NotificationCompat.Builder(context, "CHANNEL_WAKTU_SHOLAT_TIBA")
            .setSmallIcon(R.drawable.ic_nav_alarm)
            .setContentTitle("Waktu Sholat $namaSholat Telah Tiba")
            .setContentText(teksInfo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(3000 + namaSholat.hashCode(), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHANNEL_WAKTU_SHOLAT_TIBA",
                "Pemberitahuan Waktu Sholat Tiba",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi panel bar saat waktu sholat telah masuk"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}