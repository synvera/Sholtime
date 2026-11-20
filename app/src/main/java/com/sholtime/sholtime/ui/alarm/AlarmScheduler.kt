package com.sholtime.sholtime.ui.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sholtime.sholtime.MainActivity
import com.sholtime.sholtime.data.local.SholtimePreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = SholtimePreferences(context)

    fun jadwalkanSemuaAlarmAktif() {
        val daftarSholat = listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
        for (nama in daftarSholat) {
            if (prefs.isAlarmEnabled(nama)) {
                jadwalkanAlarmSholat(nama)
            } else {
                batalkanAlarm(nama)
            }
            jadwalkanPengingat30Menit(nama)
            jadwalkanNotifWaktuTiba(nama)
        }
    }

    fun jadwalkanAlarmSholat(namaSholat: String) {
        if (!prefs.isAlarmEnabled(namaSholat)) {
            batalkanAlarm(namaSholat)
            return
        }

        val hasil = hitungWaktuBerikutnya(namaSholat, kurangiMenit = 0) ?: return
        pasangExactAlarm(
            requestCode = getRequestCode(namaSholat),
            triggerAtMillis = hasil.first,
            namaSholat = namaSholat,
            snoozeCount = 0
        )
    }

    fun jadwalkanPengingat30Menit(namaSholat: String) {
        val hasil = hitungWaktuBerikutnya(namaSholat, kurangiMenit = 30) ?: return
        val triggerAtMillis = hasil.first
        val jamAsliSholat = hasil.second

        val intent = Intent(context, PrePrayerReceiver::class.java).apply {
            putExtra("EXTRA_NAMA_SHOLAT", namaSholat)
            putExtra("EXTRA_JAM_SHOLAT", jamAsliSholat)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(namaSholat) + 500,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun jadwalkanNotifWaktuTiba(namaSholat: String) {
        val hasil = hitungWaktuBerikutnya(namaSholat, kurangiMenit = 0) ?: return
        val triggerAtMillis = hasil.first
        val jamAsliSholat = hasil.second

        val intent = Intent(context, PrayerArrivedReceiver::class.java).apply {
            putExtra("EXTRA_NAMA_SHOLAT", namaSholat)
            putExtra("EXTRA_JAM_SHOLAT", jamAsliSholat)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(namaSholat) + 700,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun jadwalkanSnooze(namaSholat: String, snoozeCountBerikutnya: Int) {
        val jedaMenit = prefs.getSnoozeIntervalMinutes()
        val triggerAtMillis = System.currentTimeMillis() + (jedaMenit * 60_000L)
        pasangExactAlarm(
            requestCode = getRequestCode(namaSholat) + 100,
            triggerAtMillis = triggerAtMillis,
            namaSholat = namaSholat,
            snoozeCount = snoozeCountBerikutnya
        )
    }

    fun batalkanAlarm(namaSholat: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingMain = PendingIntent.getBroadcast(
            context,
            getRequestCode(namaSholat),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingMain)

        val pendingSnooze = PendingIntent.getBroadcast(
            context,
            getRequestCode(namaSholat) + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingSnooze)
    }

    private fun pasangExactAlarm(
        requestCode: Int,
        triggerAtMillis: Long,
        namaSholat: String,
        snoozeCount: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_NAMA_SHOLAT", namaSholat)
            putExtra("EXTRA_SNOOZE_COUNT", snoozeCount)
        }

        val operationPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, operationPendingIntent)
    }

    private fun hitungWaktuBerikutnya(namaSholat: String, kurangiMenit: Int): Pair<Long, String>? {
        val calSekarang = Calendar.getInstance()
        val formatTgl = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (offsetHari in 0..1) {
            val calCek = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offsetHari)
            }
            val keyTgl = formatTgl.format(calCek.time)
            val jadwal = prefs.getJadwalByTanggal(keyTgl) ?: continue

            val jamStr = when (namaSholat.lowercase()) {
                "subuh" -> jadwal.subuh
                "dzuhur" -> jadwal.dzuhur
                "ashar" -> jadwal.ashar
                "maghrib" -> jadwal.maghrib
                "isya" -> jadwal.isya
                else -> return null
            }

            val parts = jamStr.split(":")
            if (parts.size < 2) continue

            calCek.set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
            calCek.set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
            calCek.set(Calendar.SECOND, 0)
            calCek.set(Calendar.MILLISECOND, 0)

            if (kurangiMenit > 0) {
                calCek.add(Calendar.MINUTE, -kurangiMenit)
            }

            if (calCek.after(calSekarang)) {
                return Pair(calCek.timeInMillis, jamStr)
            }
        }
        return null
    }

    private fun getRequestCode(namaSholat: String): Int {
        return when (namaSholat.lowercase()) {
            "subuh" -> 1001
            "dzuhur" -> 1002
            "ashar" -> 1003
            "maghrib" -> 1004
            "isya" -> 1005
            else -> 1099
        }
    }
}