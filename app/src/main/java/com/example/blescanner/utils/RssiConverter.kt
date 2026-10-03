package com.example.blescanner.utils

import android.graphics.Color
import androidx.core.graphics.toColorInt

object RssiConverter {

    // Batas-batas RSSI dalam dBm. Angka-angka ini adalah ambang fisik, bukan
    // angka arbitrer: -30 dianggap sangat dekat, -100 dianggap terlemah yang
    // masih mungkin terdeteksi. Menyimpannya sebagai konstanta membuat tabel
    // di bawah jauh lebih mudah dibaca dan tidak gampang diubah sembarangan.
    private const val VERY_STRONG_MAX = -30
    private const val STRONG_MAX = -50
    private const val FAIR_MAX = -70
    private const val WEAK_MAX = -80
    private const val VERY_WEAK_MAX = -90

    // RSSI terkuat yang mungkin terjadi di dunia nyata.
    private const val RSSI_CEILING = 0

    private const val COLOR_STRONG = "#00FF00"
    private const val COLOR_GOOD = "#32CD32"
    private const val COLOR_FAIR = "#FFD700"
    private const val COLOR_WEAK = "#FFA500"
    private const val COLOR_LOST = "#FF0000"

    // Batas atas tiap kategori ditulis sebagai "batas bawah kategori berikutnya
    // dikurangi satu". Cara ini membuat jelas bahwa rentangnya bersambung tanpa
    // ada angka yang terlihat acak seperti -31 atau -51.
    private fun justBelow(lowerBound: Int) = lowerBound - 1

    // Mengembalikan Pair berisi <Kategori Sinyal, Estimasi Jarak>
    fun getSignalInfo(rssi: Int): Pair<String, String> = when (rssi) {
        in VERY_STRONG_MAX..RSSI_CEILING -> "Sangat Kuat (Sangat Dekat)" to "<1 m"
        in STRONG_MAX..justBelow(VERY_STRONG_MAX) -> "Kuat (Dekat)" to "1-3 m"
        in FAIR_MAX..justBelow(STRONG_MAX) -> "Cukup / Baik" to "3-10 m"
        in WEAK_MAX..justBelow(FAIR_MAX) -> "Lemah" to "10-20 m"
        in VERY_WEAK_MAX..justBelow(WEAK_MAX) -> "Sangat Lemah / Putus-putus" to "> 20 m"
        else -> "Sinyal Hilang (Lost)" to "Tidak Diketahui"
    }

    // Mengembalikan warna berdasarkan nilai RSSI
    fun getSignalColor(rssi: Int): Int = when (rssi) {
        in VERY_STRONG_MAX..RSSI_CEILING -> COLOR_STRONG.toColorInt()
        in STRONG_MAX..justBelow(VERY_STRONG_MAX) -> COLOR_GOOD.toColorInt()
        in FAIR_MAX..justBelow(STRONG_MAX) -> COLOR_FAIR.toColorInt()
        in WEAK_MAX..justBelow(FAIR_MAX) -> COLOR_WEAK.toColorInt()
        in VERY_WEAK_MAX..justBelow(WEAK_MAX) -> COLOR_LOST.toColorInt()
        else -> Color.GRAY
    }
}
