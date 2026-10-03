package com.example.blescanner.di

import javax.inject.Qualifier

/**
 * Penanda untuk CoroutineScope yang hidup selama aplikasi berjalan.
 *
 * Qualifier ini perlu karena Hilt bisa menyediakan lebih dari satu
 * CoroutineScope dengan tipe yang sama. Tanpa penanda, titik injeksi tidak
 * bisa membedakan scope yang satu dari yang lain.
 *
 * Lihat AppModule.provideApplicationScope.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
