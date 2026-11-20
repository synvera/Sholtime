package com.sholtime.sholtime

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sholtime.sholtime.data.local.SholtimePreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SholtimeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SholtimeWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = SholtimePreferences(context)
            val views = RemoteViews(context.packageName, R.layout.widget_jadwal_sholat)

            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
            val jadwal = prefs.getJadwalByTanggal(todayKey)

            views.setTextViewText(R.id.tvWidgetKota, prefs.getKotaNama())

            if (jadwal != null) {
                val daftarSholat = listOf(
                    "Subuh" to jadwal.subuh,
                    "Dzuhur" to jadwal.dzuhur,
                    "Ashar" to jadwal.ashar,
                    "Maghrib" to jadwal.maghrib,
                    "Isya" to jadwal.isya
                )

                val calSekarang = Calendar.getInstance()
                var namaBerikutnya = "Subuh (Besok)"
                var jamBerikutnya = jadwal.subuh

                for ((nama, jamStr) in daftarSholat) {
                    val parts = jamStr.split(":")
                    if (parts.size < 2) continue
                    val calTarget = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
                        set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                        set(Calendar.SECOND, 0)
                    }
                    if (calTarget.after(calSekarang)) {
                        namaBerikutnya = nama
                        jamBerikutnya = jamStr
                        break
                    }
                }

                views.setTextViewText(R.id.tvWidgetNamaSholat, namaBerikutnya.uppercase(Locale.getDefault()))
                views.setTextViewText(R.id.tvWidgetJamSholat, jamBerikutnya)
                views.setTextViewText(R.id.tvWidgetJamSubuh, jadwal.subuh)
                views.setTextViewText(R.id.tvWidgetJamDzuhur, jadwal.dzuhur)
                views.setTextViewText(R.id.tvWidgetJamAshar, jadwal.ashar)
                views.setTextViewText(R.id.tvWidgetJamMaghrib, jadwal.maghrib)
                views.setTextViewText(R.id.tvWidgetJamIsya, jadwal.isya)
            } else {
                views.setTextViewText(R.id.tvWidgetNamaSholat, "BUKA SHOLTIME")
                views.setTextViewText(R.id.tvWidgetJamSholat, "--:--")
                views.setTextViewText(R.id.tvWidgetJamSubuh, "--:--")
                views.setTextViewText(R.id.tvWidgetJamDzuhur, "--:--")
                views.setTextViewText(R.id.tvWidgetJamAshar, "--:--")
                views.setTextViewText(R.id.tvWidgetJamMaghrib, "--:--")
                views.setTextViewText(R.id.tvWidgetJamIsya, "--:--")
            }

            val openAppIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}