package com.example.blescanner.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

object BlePermissions {

    /**
     * Android 12 (API 31) mengganti pasangan lama BLUETOOTH/BLUETOOTH_ADMIN dengan
     * BLUETOOTH_SCAN + BLUETOOTH_CONNECT. Di bawah API 31, hasil scan BLE justru
     * dikunci oleh izin lokasi - jadi daftar izin harus dibangun per level SDK.
     *
     * Catatan: di bawah API 31 hanya ACCESS_FINE_LOCATION yang memblokir scan
     * (ACCESS_COARSE_LOCATION baru mulai relevan sejak API 29). Meminta COARSE juga
     * membuat isGranted() selalu false di perangkat API 24-28.
     */
    val required: List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun isGranted(context: Context): Boolean = required.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Di Android 11 ke bawah, izin lokasi yang sudah diberikan belum cukup: pengguna
     * juga harus menyalakan layanan lokasi, kalau tidak scan berjalan normal tapi
     * mengembalikan nol hasil tanpa pesan apa pun.
     */
    fun isLocationServiceEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return true

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        return locationManager?.let { manager ->
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } ?: false
    }
}
