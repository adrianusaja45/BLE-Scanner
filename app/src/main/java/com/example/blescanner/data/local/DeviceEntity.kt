package com.example.blescanner.data.local

import android.net.MacAddress
import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. @Entity menandakan bahwa class ini adalah sebuah tabel di dalam database SQLite
@Entity(tableName = "scanned_devices_history")
data class DeviceEntity(
    //2. @PrimaryKey berarti MAC Address ini unik, tidak boleh ada 2 perangkat dengan MAC yang sama di tabel
    @PrimaryKey
    val macAddress: String,

    val deviceName: String,

    // 3. Menyimpan waktu kapan perangkat ini terakhir dipindai (dalam bentuk milliseconds)
    val timeStamp: Long
)
