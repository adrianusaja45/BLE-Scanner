package com.example.blescanner.data

/**
 * Hasil percobaan memulai pemindaian BLE.
 *
 * Dipakai sebagai nilai balik (bukan Flow) karena ini kejadian satu kali:
 * pesan error cukup ditampilkan sekali saat tombol ditekan, tidak perlu
 * berulang tiap kali UI di-recollect.
 */
sealed interface ScanLaunchResult {

    /** Scan benar-benar berjalan. */
    data object Started : ScanLaunchResult

    /** Perangkat tidak punya adapter, atau tidak mendukung Bluetooth LE. */
    data object BluetoothUnavailable : ScanLaunchResult

    /** Adapter ada tapi sedang nonaktif. */
    data object BluetoothDisabled : ScanLaunchResult

    /** Izin BLUETOOTH_SCAN/CONNECT (atau lokasi) belum diberikan. */
    data object PermissionDenied : ScanLaunchResult

    /** Android 11 ke bawah: izin ada tapi layanan lokasi masih mati. */
    data object LocationServiceDisabled : ScanLaunchResult

    /** Gagal karena hal lain; [reason] dipakai untuk pesan di logcat. */
    data class Failed(val reason: String) : ScanLaunchResult
}
