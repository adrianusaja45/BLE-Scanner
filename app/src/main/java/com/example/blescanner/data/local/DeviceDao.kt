package com.example.blescanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    //1. Menyimpan atau memperbarui data perangkat
    // Jika MAC Address sudah ada di database, REPLACE akan otomatis menimpa
    // data lama dengan data baru (timestamp terbaru).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    // 2. Membaca seluruh riwayat dari database
    @Query("SELECT * FROM scanned_devices_history ORDER BY timeStamp DESC")
    fun getAllHistory(): Flow<List<DeviceEntity>>

    //3. Menghapus semua riwayat dari database
    @Query("DELETE FROM scanned_devices_history")
    suspend fun deleteAllHistory()
}
