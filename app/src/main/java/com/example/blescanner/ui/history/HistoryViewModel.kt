package com.example.blescanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blescanner.data.local.DeviceDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Anotasi wajib agar Hilt tahu ini adalah ViewModel dan otomatis menyuntikkan DAO
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val deviceDao: DeviceDao
) : ViewModel() {

    // 2. Mengambil Flow dari database dan mengubahnya menjadi StateFlow.
    val historyList = deviceDao.getAllHistory()
        .stateIn(
            scope = viewModelScope, // Terikat dengan siklus hidup ViewModel
            started = SharingStarted.WhileSubscribed(5000), // Hemat baterai: berhenti pantau
            // database 5 detik setelah layar ditutup
            initialValue = emptyList() // Nilai awal sebelum data dari database selesai dimuat
        )

    // 3. Menghapus Seluruh Riwayat
    fun deleteAllHistory() {
        viewModelScope.launch {
            deviceDao.deleteAllHistory()
        }
    }
}

