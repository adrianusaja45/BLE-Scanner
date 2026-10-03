package com.example.blescanner.di

import android.content.Context
import androidx.room.Room

import com.example.blescanner.data.local.AppDatabase
import com.example.blescanner.data.local.DeviceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

// 1. Menandakan ini adalah modul Hilt yang bertahan selama aplikasi hidup (SingletonComponent)
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // 2. Memberi tahu Hilt cara membuat AppDatabase
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder<AppDatabase>(
            context,
            "ble_scanner_db" //nama file db yang tersimpan di hp
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // 3. Memberi tahu Hilt cara membuat DeviceDao
    @Provides
    @Singleton
    fun provideDeviceDao(appDatabase: AppDatabase): DeviceDao {
        return appDatabase.deviceDao()
    }

    /**
     * Satu CoroutineScope untuk seluruh pekerjaan background di aplikasi ini.
     *
     * Diberikan sekali lewat DI, bukan dibuat inline di tempat pemakaian.
     * Membuat scope baru di dalam callback yang dipanggil ratusan kali per
     * detik berarti ratusan scope yang tidak pernah di-cancel dan tidak punya
     * Job parent - sesuatu yang boros dan sulit dilacak.
     *
     * SupervisorJob dipilih, bukan Job biasa: kalau satu operasi gagal, scope
     * ini dan operasi lainnya tidak ikut dibatalkan. Untuk pekerjaan seperti
     * penyimpanan ke database, satu kegagalan tidak boleh menghentikan sisanya.
     *
     * Dispatchers.IO dipakai karena operasi ini hanya menunggu I/O (tulis ke
     * database), bukan perhitungan berat.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

