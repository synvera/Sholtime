package com.sholtime.sholtime.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun detectCityName(onResult: (String?) -> Unit) {
        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                extractCityFromCoordinates(location.latitude, location.longitude, onResult)
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        extractCityFromCoordinates(lastLoc.latitude, lastLoc.longitude, onResult)
                    } else {
                        onResult(null)
                    }
                }.addOnFailureListener {
                    onResult(null)
                }
            }
        }.addOnFailureListener {
            onResult(null)
        }
    }

    private fun extractCityFromCoordinates(lat: Double, lon: Double, onResult: (String?) -> Unit) {
        try {
            val geocoder = Geocoder(context, Locale("id", "ID"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    val rawCity = addresses.firstOrNull()?.subAdminArea
                        ?: addresses.firstOrNull()?.locality
                    onResult(cleanCityKeyword(rawCity))
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val rawCity = addresses?.firstOrNull()?.subAdminArea
                    ?: addresses?.firstOrNull()?.locality
                onResult(cleanCityKeyword(rawCity))
            }
        } catch (e: Exception) {
            onResult(null)
        }
    }

    private fun cleanCityKeyword(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw.replace("Kota ", "", ignoreCase = true)
            .replace("Kabupaten ", "", ignoreCase = true)
            .replace("Kab. ", "", ignoreCase = true)
            .replace("City", "", ignoreCase = true)
            .replace("Regency", "", ignoreCase = true)
            .trim()
    }
}