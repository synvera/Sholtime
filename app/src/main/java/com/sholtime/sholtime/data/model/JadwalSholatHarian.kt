package com.sholtime.sholtime.data.model

data class JadwalSholatHarian(
    val tanggalKey: String,
    val tanggalTeks: String,
    val kota: String,
    val provinsi: String,
    val subuh: String,
    val dzuhur: String,
    val ashar: String,
    val maghrib: String,
    val isya: String
)

enum class StatusWaktuSholat {
    TERLEWATI,
    BERIKUTNYA,
    AKAN_DATANG
}

data class ItemWaktuSholat(
    val nama: String,
    val jam: String,
    val status: StatusWaktuSholat
)