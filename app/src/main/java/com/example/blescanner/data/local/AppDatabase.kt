package com.example.blescanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

//1. Daftarkan semua entity (tabel) yang ada di aplikasi ini
//jika ada tabel baru nanti, tambah ke dalam array ini
// exportSchema = false digunakan agar Room tidak perlu membuat file history skema database saat proses compile.
@Database(
    entities = [DeviceEntity::class],
    version = 1,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {

    // 2. Berikan Akses ke DAO (Data Access Object) agar bisa dipanggil oleh Repository
    abstract fun deviceDao(): DeviceDao

}
