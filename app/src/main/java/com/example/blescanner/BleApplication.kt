package com.example.blescanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Anotasi @HiltAndroidApp membuat Hilt membuat object graph aplikasi di sini.
// Kelasnya harus ada walau kosong - HiltVDC yang mengisi isinya saat runtime.
@HiltAndroidApp
@Suppress("EmptyClassBlock")
class BleApplication : Application()
