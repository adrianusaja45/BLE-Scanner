package com.example.blescanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blescanner.data.BleScannerRepo
import com.example.blescanner.model.ScannedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val bleScannerRepo: BleScannerRepo
) : ViewModel() {
    //Pantau Flow dari Repo, urutkan, lalu ubah lagi ke state flow untuk UI
    val scannedDevice: StateFlow<List<ScannedDevice>> = bleScannerRepo.scannedDevices
        .map { devices ->
            //Mengurutkan berdasarkan nilai RSSI paling tinggi (mendekati 0)
            // ke terkecil (mendekati -100)
            devices.sortedByDescending { it.rssi }
        }
        .stateIn(
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
}
