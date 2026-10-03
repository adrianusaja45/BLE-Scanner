package com.example.blescanner.utils

import android.graphics.Color
import androidx.core.graphics.toColorInt

object RssiConverter {
    // Mengembalikan Pair berisi <Kategori Sinyal, Estimasi Jarak>
    fun getSignalInfo(rssi: Int): Pair<String, String>{
        return when(rssi){
            in -30..0 -> "Sangat Kuat (Sangat Dekat)" to "<1 m"
            in -50..-31 -> "Kuat (Dekat)" to "1-3 m"
            in -70..-51 -> "Cukup / Baik" to "3-10 m"
            in -80..-71 -> "Lemah" to "10-20 m"
            in -90..-81 -> "Sangat Lemah / Putus-putus" to "> 20 m"
            else -> "Sinyal Hilang (Lost)" to "Tidak Diketahui"
        }
    }

    //Mengembalikan warna berdasarkan nilai RSSI
    fun getSignalColor(rssi: Int): Int {
        return when(rssi){
            in -30..0 -> "#00FF00".toColorInt() // Hijau terang (Sangat Kuat)
            in -50..-31 -> "#32CD32".toColorInt() // Hijau (Kuat)
            in -70..-51 -> "#FFD700".toColorInt() // Kuning (Cukup)
            in -80..-71 -> "#FFA500".toColorInt() // Oranye (Lemah)
            in -90..-81 -> "#FF0000".toColorInt() // Merah (Sangat Lemah)
            else -> Color.GRAY // Hilang
        }
    }
}