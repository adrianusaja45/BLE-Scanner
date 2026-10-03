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
}

