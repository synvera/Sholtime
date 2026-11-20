package com.sholtime.sholtime.data.api

import com.sholtime.sholtime.data.model.JadwalBulananResponse
import com.sholtime.sholtime.data.model.KotaSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface MyQuranApiService {

    @GET("sholat/kabkota/cari/{keyword}")
    suspend fun cariKota(
        @Path("keyword") keyword: String
    ): KotaSearchResponse

    @GET("sholat/jadwal/{id}/{periode}")
    suspend fun getJadwalBulanan(
        @Path("id") idKota: String,
        @Path("periode") tahunBulan: String
    ): JadwalBulananResponse
}