package com.sholtime.sholtime.data.model

data class KotaSearchResponse(
    val status: Boolean,
    val data: List<KotaItem>?
)

data class KotaItem(
    val id: String,
    val lokasi: String
)

data class JadwalBulananResponse(
    val status: Boolean,
    val data: JadwalBulananData?
)

data class JadwalBulananData(
    val id: String,
    val kabko: String,
    val prov: String,
    val jadwal: Map<String, JadwalHarianRaw>?
)

data class JadwalHarianRaw(
    val tanggal: String,
    val imsak: String,
    val subuh: String,
    val terbit: String,
    val dhuha: String,
    val dzuhur: String,
    val ashar: String,
    val maghrib: String,
    val isya: String
)