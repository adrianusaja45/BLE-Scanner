package com.example.blescanner.model

/**
 * Status adapter Bluetooth yang diketahui aplikasi.
 *
 * Dipisah dari Boolean karena ada kondisi antara yang penting: TURNING_ON.
 * Kalau kondisi transisi ini diplakukan sebagai "sudah aktif", aplikasi akan
 * mencoba memulai scan sebelum LE benar-benar siap, dan hasilnya akan gagal
 * dengan pesan yang menyesatkan.
 */
enum class BluetoothStatus {
    /** Adapter aktif dan siap dipakai. */
    ENABLED,

    /** Adapter nonaktif. Scan tidak mungkin berjalan. */
    DISABLED,

    /** Sedang menyala. Scan belum bisa dimulai, tunggu status ENABLED. */
    TURNING_ON,

    /** Sedang dimatikan. Perlakukan seperti DISABLED supaya scan dihentikan cepat. */
    TURNING_OFF
}
