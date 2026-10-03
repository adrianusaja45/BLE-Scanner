package com.example.blescanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blescanner.data.BleScannerRepo
import com.example.blescanner.data.ScanLaunchResult
import com.example.blescanner.model.ScannedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // State untuk menyimpan MAC Address target yang dipilih di layar Radar
    private val _targetMacAddress = MutableStateFlow<String?>(null)
    //menyimpan status scanning
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Berniat melanjutkan scan otomatis saat aplikasi kembali ke foreground.
    // Sengaja mutable var, bukan StateFlow: ini state internal, tidak ada UI
    // yang perlu mengamatinya.
    private var resumeScanWhenForegrounded = false

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

    //Aliran data khusus untuk 1 target device
    val targetDevice: StateFlow<ScannedDevice?> = combine(
        scannedDevice,
        _targetMacAddress
    ) { devices, targetMac ->
        devices.find { it.mac == targetMac }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun startScan(): ScanLaunchResult {
        val result = bleScannerRepo.startScan()
        // Hanya tandai aktif kalau repo benar-benar berhasil memulai.
        // Kalau tidak, tombol akan menampilkan "Stop Scan" padahal tidak ada scan.
        _isScanning.value = result == ScanLaunchResult.Started
        return result
    }

    fun stopScan() {
        bleScannerRepo.stopScan()
        _isScanning.value = false
    }

    /**
     * Dipanggil dari Activity.onStop() saat aplikasi benar-benar masuk background.
     *
     * Adapter BLE yang aktif di background adalah sumber borosnya baterai, jadi scan
     * harus dihentikan. Menyimpan niatnya dulu supaya onAppForegrounded() bisa
     * melanjutkan otomatis tanpa user menekan tombol lagi.
     */
    fun onAppBackgrounded() {
        if (!_isScanning.value) return

        resumeScanWhenForegrounded = true
        bleScannerRepo.stopScan()
        _isScanning.value = false
    }

    /**
     * Dipanggil dari Activity.onStart() saat aplikasi kembali ke foreground.
     *
     * Sengaja memakai startScan() biasa supaya semua prasyarat dicek ulang:
     * kalau Bluetooth dimatikan atau izin dicabut selama aplikasi di background,
     * hasilnya dikembalikan agar UI bisa memberi tahu, dan niatnya dikosongkan
     * supaya tidak mencoba lagi di setiap kali foreground berikutnya.
     */
    fun onAppForegrounded(): ScanLaunchResult? {
        if (!resumeScanWhenForegrounded) return null

        resumeScanWhenForegrounded = false
        val result = bleScannerRepo.startScan()
        _isScanning.value = result == ScanLaunchResult.Started
        return result
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMinRssiFilter(minRssi: Int) {
        _minRssiFilter.value = minRssi
    }

    // Fungsi untuk dipanggil oleh RadarFragment saat pertama kali dibuka
    fun setTrackingTarget(macAddress: String) {
        _targetMacAddress.value = macAddress
    }

    // Fungsi untuk menghapus target tracking (misal saat RadarFragment ditutup)
    fun clearTrackingTarget() {
        _targetMacAddress.value = null
    }
}
