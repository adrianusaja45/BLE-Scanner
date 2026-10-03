package com.example.blescanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blescanner.data.BleScannerRepo
import com.example.blescanner.data.ScanLaunchResult
import com.example.blescanner.model.BluetoothStatus
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

    // Status adapter Bluetooth terakhir yang diketahui aplikasi.
    // Diinisialisasi dari kondisi sebenarnya, bukan dari asumsi, supaya UI tidak
    // sempat menampilkan "aktif" padahal Bluetooth sedang mati.
    private val _bluetoothStatus = MutableStateFlow(
        if (bleScannerRepo.isBluetoothEnabled()) {
            BluetoothStatus.ENABLED
        } else {
            BluetoothStatus.DISABLED
        }
    )
    val bluetoothStatus: StateFlow<BluetoothStatus> = _bluetoothStatus.asStateFlow()

    // Berniat melanjutkan scan otomatis saat Bluetooth dinyalakan kembali.
    private var resumeScanWhenBluetoothReenabled = false

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
        // Segarkan status Bluetooth dari kondisi sebenarnya. Selama aplikasi di
        // background receiver dilepas, jadi perubahan status yang terjadi di luar
        // aplikasi tidak pernah sampai ke sini dan _bluetoothStatus bisa basi.
        // Tanpa refresh ini, Bluetooth yang dimatikan dari quick settings akan
        // tetap terbaca ENABLED dan UI menampilkan "Scan Dihentikan" - menyesatkan,
        // karena penyebab sebenarnya adalah Bluetooth mati.
        _bluetoothStatus.value = if (bleScannerRepo.isBluetoothEnabled()) {
            BluetoothStatus.ENABLED
        } else {
            BluetoothStatus.DISABLED
        }

        if (!resumeScanWhenForegrounded) return null

        resumeScanWhenForegrounded = false
        val result = bleScannerRepo.startScan()
        _isScanning.value = result == ScanLaunchResult.Started

        // Gagal hanya karena Bluetooth mati: simpan niat supaya scan otomatis
        // dilanjutkan begitu user menyalakan Bluetooth lagi. Untuk kegagalan
        // lain (izin kurang, lokasi mati) niatnya tidak disimpan, karena
        // menyalakan Bluetooth tidak akan menyelesaikannya.
        if (result == ScanLaunchResult.BluetoothDisabled) {
            resumeScanWhenBluetoothReenabled = true
        }

        return result
    }

    /**
     * Dipanggil dari BroadcastReceiver saat Android memberi tahu status Bluetooth berubah.
     *
     * Yang dilakukan di sini:
     * - DISABLED/TURNING_OFF: hentikan scan yang sedang berjalan. Tanpa ini, OS
     *   berhenti mengirim hasil scan tetapi UI tetap menampilkan daftar perangkat
     *   terakhir beserta RSSI basi, sehingga terlihat seperti aplikasi hang.
     * - ENABLED: lanjutkan scan otomatis kalau sebelumnya sengaja dihentikan karena
     *   Bluetooth dimatikan.
     */
    fun onBluetoothStateChanged(status: BluetoothStatus) {
        _bluetoothStatus.value = status

        when (status) {
            BluetoothStatus.DISABLED, BluetoothStatus.TURNING_OFF -> {
                if (_isScanning.value) {
                    resumeScanWhenBluetoothReenabled = true
                    bleScannerRepo.stopScan()
                    _isScanning.value = false
                }
            }

            BluetoothStatus.ENABLED -> {
                if (resumeScanWhenBluetoothReenabled) {
                    resumeScanWhenBluetoothReenabled = false
                    // Pakai startScan() biasa supaya prasyarat dicek ulang.
                    val result = bleScannerRepo.startScan()
                    _isScanning.value = result == ScanLaunchResult.Started
                }
            }

            // TURNING_ON belum siap dipakai. Menyalakan scan di sini akan gagal
            // dengan pesan "Bluetooth nonaktif" padahal user sedang menyalakannya.
            BluetoothStatus.TURNING_ON -> Unit
        }
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
