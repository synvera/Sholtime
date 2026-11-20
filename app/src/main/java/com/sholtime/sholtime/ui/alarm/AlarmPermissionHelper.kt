package com.sholtime.sholtime.ui.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object AlarmPermissionHelper {

    fun periksaSemuaIzin(context: Context) {
        // 1. Izin Exact Alarm (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                tampilkanDialogIzin(
                    context,
                    "Izin Alarm Tepat Waktu",
                    "Agar adzan berbunyi tepat detik itu juga di semua HP, izinkan 'Alarm & pengingat' untuk Sholtime."
                ) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
                return
            }
        }

        // 2. Izin Full-Screen Notification (Android 14+ / Samsung One UI 6+ / Tecno HiOS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notifManager = context.getSystemService(NotificationManager::class.java)
            if (!notifManager.canUseFullScreenIntent()) {
                tampilkanDialogIzin(
                    context,
                    "Izin Layar Penuh Saat Terkunci",
                    "Aktifkan 'Notifikasi layar penuh' agar layar alarm muncul menembus layar mati."
                ) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
                return
            }
        }

        // 3. Izin Display Over Other Apps (Cadangan universal semua merek HP)
        if (!Settings.canDrawOverlays(context)) {
            tampilkanDialogIzin(
                context,
                "Izin Tampil di Atas Aplikasi Lain",
                "Diperlukan agar layar alarm otomatis muncul di atas layar kunci pada HP Samsung, Xiaomi, Oppo, Vivo, dan Tecno."
            ) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
            return
        }

        // 4. Bebaskan dari Pembatasan Baterai (Doze Mode malam hari)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            tampilkanDialogIzin(
                context,
                "Bebaskan Optimasi Baterai",
                "Pilih 'Tidak dibatasi' agar sistem HP tidak mematikan alarm Sholtime saat HP tidur semalaman."
            ) {
                try {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }
    }

    private fun tampilkanDialogIzin(
        context: Context,
        judul: String,
        pesan: String,
        onBukaPengaturan: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(judul)
            .setMessage(pesan)
            .setPositiveButton("Buka Pengaturan") { _, _ -> onBukaPengaturan() }
            .setNegativeButton("Nanti", null)
            .show()
    }
}