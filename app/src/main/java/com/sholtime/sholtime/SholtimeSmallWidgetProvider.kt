package com.sholtime.sholtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import java.util.Calendar
import java.util.Locale

class SholtimeSmallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateSmallWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, SholtimeSmallWidgetProvider::class.java)
            )
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.sholtime.sholtime.ACTION_REFRESH_SMALL_WIDGET"

        fun updateSmallWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_jadwal_small)

                val openAppIntent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetSmallRoot, openAppIntent)

                val (namaSholatBerikutnya, jamTargetStr, targetEpochMillis) = cariSholatTerdekat(context)

                views.setTextViewText(R.id.tvWidgetSmallLabel, "MENUJU $namaSholatBerikutnya")
                views.setTextViewText(R.id.tvWidgetSmallJamTarget, "Pukul $jamTargetStr")

                // Hitung sisa waktu dalam jam dan menit saja (dibulatkan ke atas per menit)
                val sisaMillis = (targetEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                val totalMenit = ((sisaMillis + 59_999L) / 60_000L).toInt()
                val jamSisa = totalMenit / 60
                val menitSisa = totalMenit % 60

                // Format tampilan: "05j 37m" (atau ubah ke "%02d:%02d" jika ingin "05:37")
                val teksCountdown = String.format(Locale.US, "%02dj %02dm", jamSisa, menitSisa)
                views.setTextViewText(R.id.tvWidgetSmallCountdown, teksCountdown)

                appWidgetManager.updateAppWidget(appWidgetId, views)

                // Jadwalkan pembaruan tepat pada pergantian menit berikutnya (detik ke-00)
                val calMenitBerikutnya = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, 1)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 100)
                }
                jadwalkanRefreshBerikutnya(context, calMenitBerikutnya.timeInMillis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private data class TargetSholat(
            val nama: String,
            val jamStr: String,
            val epochMillis: Long
        )

        private fun cariSholatTerdekat(context: Context): TargetSholat {
            val sp = context.getSharedPreferences("sholtime_prefs", Context.MODE_PRIVATE)

            val daftarJadwal = listOf(
                "SUBUH" to (sp.getString("jadwal_subuh", null) ?: sp.getString("subuh", null) ?: "04:06"),
                "DZUHUR" to (sp.getString("jadwal_dzuhur", null) ?: sp.getString("dzuhur", null) ?: "11:42"),
                "ASHAR" to (sp.getString("jadwal_ashar", null) ?: sp.getString("ashar", null) ?: "15:05"),
                "MAGHRIB" to (sp.getString("jadwal_maghrib", null) ?: sp.getString("maghrib", null) ?: "17:55"),
                "ISYA" to (sp.getString("jadwal_isya", null) ?: sp.getString("isya", null) ?: "19:09")
            )

            val sekarang = System.currentTimeMillis()

            for ((nama, jamStr) in daftarJadwal) {
                val targetCal = parseJamKeCalendar(jamStr, tambahHari = 0) ?: continue
                if (targetCal.timeInMillis > sekarang) {
                    return TargetSholat(nama, jamStr, targetCal.timeInMillis)
                }
            }

            val subuhBesokStr = daftarJadwal.first().second
            val subuhBesokCal = parseJamKeCalendar(subuhBesokStr, tambahHari = 1)
                ?: Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 6) }

            return TargetSholat("SUBUH", subuhBesokStr, subuhBesokCal.timeInMillis)
        }

        private fun parseJamKeCalendar(jamStr: String, tambahHari: Int): Calendar? {
            val parts = jamStr.trim().split(":")
            if (parts.size < 2) return null
            val jam = parts[0].toIntOrNull() ?: return null
            val menit = parts[1].toIntOrNull() ?: return null

            return Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, tambahHari)
                set(Calendar.HOUR_OF_DAY, jam)
                set(Calendar.MINUTE, menit)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        private fun jadwalkanRefreshBerikutnya(context: Context, triggerAtMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SholtimeSmallWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                4422,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (_: Exception) {
                alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent)
            }
        }
    }
}