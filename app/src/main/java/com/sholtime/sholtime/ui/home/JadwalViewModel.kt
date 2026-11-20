package com.sholtime.sholtime.ui.home


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sholtime.sholtime.data.api.RetrofitClient
import com.sholtime.sholtime.data.local.SholtimePreferences
import com.sholtime.sholtime.data.model.JadwalSholatHarian
import com.sholtime.sholtime.data.model.KotaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class JadwalViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SholtimePreferences(application)
    private val api = RetrofitClient.apiService

    private val _jadwalHariIni = MutableLiveData<JadwalSholatHarian?>()
    val jadwalHariIni: LiveData<JadwalSholatHarian?> = _jadwalHariIni

    private val _namaKotaAktif = MutableLiveData<String>(prefs.getKotaNama())
    val namaKotaAktif: LiveData<String> = _namaKotaAktif

    private val _isOfflineMode = MutableLiveData<Boolean>(false)
    val isOfflineMode: LiveData<Boolean> = _isOfflineMode

    private val _statusInfo = MutableLiveData<String>()
    val statusInfo: LiveData<String> = _statusInfo

    private val _hasilCariKota = MutableLiveData<List<KotaItem>>()
    val hasilCariKota: LiveData<List<KotaItem>> = _hasilCariKota

    init {
        muatJadwalHariIni()
    }

    fun muatJadwalHariIni() {
        val todayKey = getTanggalKey(0)
        val cachedToday = prefs.getJadwalByTanggal(todayKey)
        if (cachedToday != null) {
            _jadwalHariIni.postValue(cachedToday)
            _namaKotaAktif.postValue(cachedToday.kota)
        }
        sinkronisasiJadwalBulanan(prefs.getKotaId())
    }

    fun sinkronisasiJadwalBulanan(idKota: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val periodeBulanIni = getPeriodeBulan(0)
                val periodeBulanDepan = getPeriodeBulan(1)

                val resBulanIni = api.getJadwalBulanan(idKota, periodeBulanIni)
                val mapSimpan = mutableMapOf<String, JadwalSholatHarian>()

                if (resBulanIni.status && resBulanIni.data != null) {
                    val data = resBulanIni.data
                    prefs.saveKota(data.id, data.kabko, data.prov)
                    data.jadwal?.forEach { (tglKey, raw) ->
                        mapSimpan[tglKey] = JadwalSholatHarian(
                            tanggalKey = tglKey,
                            tanggalTeks = raw.tanggal,
                            kota = data.kabko,
                            provinsi = data.prov,
                            subuh = raw.subuh,
                            dzuhur = raw.dzuhur,
                            ashar = raw.ashar,
                            maghrib = raw.maghrib,
                            isya = raw.isya
                        )
                    }
                }

                try {
                    val resBulanDepan = api.getJadwalBulanan(idKota, periodeBulanDepan)
                    if (resBulanDepan.status && resBulanDepan.data != null) {
                        val dataNext = resBulanDepan.data
                        dataNext.jadwal?.forEach { (tglKey, raw) ->
                            mapSimpan[tglKey] = JadwalSholatHarian(
                                tanggalKey = tglKey,
                                tanggalTeks = raw.tanggal,
                                kota = dataNext.kabko,
                                provinsi = dataNext.prov,
                                subuh = raw.subuh,
                                dzuhur = raw.dzuhur,
                                ashar = raw.ashar,
                                maghrib = raw.maghrib,
                                isya = raw.isya
                            )
                        }
                    }
                } catch (_: Exception) {
                }

                if (mapSimpan.isNotEmpty()) {
                    prefs.saveCacheJadwalBulanan(mapSimpan)
                    _isOfflineMode.postValue(false)
                    val todayData = mapSimpan[getTanggalKey(0)]
                    if (todayData != null) {
                        _jadwalHariIni.postValue(todayData)
                        _namaKotaAktif.postValue(todayData.kota)
                    }
                }
            } catch (e: Exception) {
                val fallback = prefs.getJadwalByTanggal(getTanggalKey(0))
                if (fallback != null) {
                    _isOfflineMode.postValue(true)
                    _jadwalHariIni.postValue(fallback)
                    _namaKotaAktif.postValue(fallback.kota)
                } else {
                    _statusInfo.postValue("Gagal memuat jadwal. Periksa koneksi internet.")
                }
            }
        }
    }

    fun cariDanPilihKotaOtomatis(keywordKota: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = api.cariKota(keywordKota)
                val pertama = response.data?.firstOrNull()
                if (pertama != null) {
                    prefs.saveKota(pertama.id, pertama.lokasi)
                    _namaKotaAktif.postValue(pertama.lokasi)
                    sinkronisasiJadwalBulanan(pertama.id)
                } else {
                    _statusInfo.postValue("Kota '$keywordKota' tidak ditemukan di server.")
                }
            } catch (e: Exception) {
                _statusInfo.postValue("Tidak dapat mendeteksi kota saat offline.")
            }
        }
    }

    fun cariKotaManual(keyword: String) {
        if (keyword.trim().length < 2) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = api.cariKota(keyword.trim())
                _hasilCariKota.postValue(response.data ?: emptyList())
            } catch (e: Exception) {
                _hasilCariKota.postValue(emptyList())
                _statusInfo.postValue("Pencarian gagal, perangkat sedang offline.")
            }
        }
    }

    fun pilihKotaManual(kota: KotaItem) {
        prefs.saveKota(kota.id, kota.lokasi)
        _namaKotaAktif.postValue(kota.lokasi)
        sinkronisasiJadwalBulanan(kota.id)
    }

    private fun getTanggalKey(tambahHari: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, tambahHari)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun getPeriodeBulan(tambahBulan: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, tambahBulan)
        return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }
}