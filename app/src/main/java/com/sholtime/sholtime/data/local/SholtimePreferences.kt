package com.sholtime.sholtime.data.local

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.sholtime.sholtime.R
import com.sholtime.sholtime.data.model.JadwalSholatHarian
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SholtimePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("sholtime_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveKota(id: String, nama: String, provinsi: String = "") {
        prefs.edit()
            .putString("kota_id", id)
            .putString("kota_nama", nama)
            .putString("kota_prov", provinsi)
            .apply()
    }

    fun getKotaId(): String = prefs.getString("kota_id", "eda80a3d5b344bc40f3bc04f65b7a357") ?: "eda80a3d5b344bc40f3bc04f65b7a357"

    fun getKotaNama(): String = prefs.getString("kota_nama", "KOTA JAKARTA") ?: "KOTA JAKARTA"

    fun getKotaProvinsi(): String = prefs.getString("kota_prov", "DKI JAKARTA") ?: "DKI JAKARTA"

    fun saveCacheJadwalBulanan(mapJadwal: Map<String, JadwalSholatHarian>) {
        val currentMap = getAllCacheJadwal().toMutableMap()
        currentMap.putAll(mapJadwal)
        val json = gson.toJson(currentMap)
        prefs.edit().putString("cache_jadwal_map", json).apply()
    }

    fun getAllCacheJadwal(): Map<String, JadwalSholatHarian> {
        val json = prefs.getString("cache_jadwal_map", null) ?: return emptyMap()
        val type = TypeToken.getParameterized(
            Map::class.java,
            String::class.java,
            JadwalSholatHarian::class.java
        ).type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getJadwalByTanggal(tanggalKey: String): JadwalSholatHarian? {
        return getAllCacheJadwal()[tanggalKey]
    }

    fun setAlarmEnabled(namaSholat: String, enabled: Boolean) {
        prefs.edit().putBoolean("alarm_enabled_${namaSholat.lowercase()}", enabled).apply()
    }

    fun isAlarmEnabled(namaSholat: String): Boolean {
        val defaultOn = namaSholat.equals("Subuh", ignoreCase = true)
        return prefs.getBoolean("alarm_enabled_${namaSholat.lowercase()}", defaultOn)
    }

    fun setAlarmToneKey(namaSholat: String, toneKey: String) {
        prefs.edit().putString("alarm_tone_${namaSholat.lowercase()}", toneKey).apply()
    }

    fun setAllAlarmToneKey(toneKey: String) {
        val listSholat = listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
        val editor = prefs.edit()
        for (nama in listSholat) {
            editor.putString("alarm_tone_${nama.lowercase()}", toneKey)
        }
        editor.apply()
    }

    fun getAlarmToneKey(namaSholat: String): String {
        val defaultTone = if (namaSholat.equals("Subuh", ignoreCase = true)) "adzan_subuh" else "adzan_reguler"
        return prefs.getString("alarm_tone_${namaSholat.lowercase()}", defaultTone) ?: defaultTone
    }

    fun getToneResId(toneKey: String): Int {
        return when (toneKey) {
            "adzan_subuh" -> R.raw.adzan_subuh
            "adzan_reguler" -> R.raw.adzan_reguler
            "nada_alarm_1" -> R.raw.nada_alarm_1
            else -> R.raw.adzan_reguler
        }
    }

    fun getToneDisplayName(toneKey: String): String {
        return when (toneKey) {
            "adzan_subuh" -> "Adzan Subuh"
            "adzan_reguler" -> "Adzan Reguler"
            "nada_alarm_1" -> "Nada Alarm Lembut"
            else -> "Adzan Reguler"
        }
    }

    fun setSnoozeMaxCount(count: Int) {
        prefs.edit().putInt("snooze_max_count", count).apply()
    }

    fun getSnoozeMaxCount(): Int = prefs.getInt("snooze_max_count", 3)

    fun setSnoozeIntervalMinutes(minutes: Int) {
        prefs.edit().putInt("snooze_interval_minutes", minutes).apply()
    }

    fun getSnoozeIntervalMinutes(): Int = prefs.getInt("snooze_interval_minutes", 5)

    fun setAlarmVolumePercent(percent: Int) {
        prefs.edit().putInt("alarm_volume_percent", percent).apply()
    }

    fun getAlarmVolumePercent(): Int = prefs.getInt("alarm_volume_percent", 90)

    fun setAlarmVibrate(vibrate: Boolean) {
        prefs.edit().putBoolean("alarm_vibrate", vibrate).apply()
    }

    fun isAlarmVibrate(): Boolean = prefs.getBoolean("alarm_vibrate", true)

    fun saveUserProfile(
        namaLengkap: String,
        username: String,
        domisili: String,
        bio: String,
        photoPath: String?
    ) {
        val editor = prefs.edit()
            .putString("profile_nama", namaLengkap)
            .putString("profile_username", username)
            .putString("profile_domisili", domisili)
            .putString("profile_bio", bio)
        if (photoPath != null) {
            editor.putString("profile_photo_path", photoPath)
        }
        editor.apply()
    }

    fun getProfileNama(): String = prefs.getString("profile_nama", "Hamba Allah") ?: "Hamba Allah"

    fun getProfileUsername(): String = prefs.getString("profile_username", "@muslim_taat") ?: "@muslim_taat"

    fun getProfileDomisili(): String = prefs.getString("profile_domisili", getKotaNama()) ?: getKotaNama()

    fun getProfileBio(): String = prefs.getString("profile_bio", "Menjaga sholat tepat waktu adalah kunci ketenangan hati.")
        ?: "Menjaga sholat tepat waktu adalah kunci ketenangan hati."

    fun getProfilePhotoPath(): String? = prefs.getString("profile_photo_path", null)

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt("app_theme_mode", mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getThemeMode(): Int = prefs.getInt("app_theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun applySavedTheme() {
        AppCompatDelegate.setDefaultNightMode(getThemeMode())
    }
}