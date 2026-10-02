package com.example.blescanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blescanner.data.BleScannerRepo
import com.example.blescanner.model.ScannedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val bleScannerRepo: BleScannerRepo
) : ViewModel() {
    //Pantau Flow dari Repo, urutkan, lalu ubah lagi ke state flow untuk UI

    //Penampung Input dari UI Search
    private val _searchQuery = MutableStateFlow("")
    private val _minRssiFilter = MutableStateFlow(-100) // Default: tampilkan semua (hingga -100 dBm)
    // Menggabungkan 3 aliran data secara reaktif
    val scannedDevice: StateFlow<List<ScannedDevice>> = combine(
        bleScannerRepo.scannedDevices,
        _searchQuery,
        _minRssiFilter
    ){ devices, query, minRssi ->
        devices.filter { device ->
            // Filter 1: Nama atau MAC Address cocok dengan input pencarian
            val matchesSearch = device.name.contains(query, ignoreCase = true) ||
                                device.mac.contains(query, ignoreCase = true)
            val matchesMinRssi = device.rssi >= minRssi
            // Filter 2: Sinyal lebih besar/sama dengan ambang batas (contoh: >= -80 dBm)
            matchesSearch && matchesMinRssi
        }.sortedByDescending { it.rssi }
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startScan() {
        bleScannerRepo.startScan()
    }

    fun stopScan() {
        bleScannerRepo.stopScan()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMinRssiFilter(minRssi: Int) {
        _minRssiFilter.value = minRssi
    }
}
