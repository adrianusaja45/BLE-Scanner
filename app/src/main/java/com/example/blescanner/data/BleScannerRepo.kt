package com.example.blescanner.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.blescanner.data.local.DeviceDao
import com.example.blescanner.data.local.DeviceEntity
import com.example.blescanner.model.ScannedDevice
import com.example.blescanner.util.BlePermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

//WAJIB @Singleton: Repo ini menyimpan sesi scan dan data perangkat.
//Tanpa scope, Hilt membuat instance baru per Fragment sehingga
//halaman Radar membaca repo yang berbeda (kosong) dari halaman Scanner.
@Singleton
class BleScannerRepo @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceDao: DeviceDao
) {
    private companion object {
        const val TAG = "BleScannerRepo"
    }

    //wadah sementara data di map agar mudah menemukan dan memperbarui data berdasarkan MAC Address
    private val scannedDevicesMap = mutableMapOf<String, ScannedDevice>()

    //Pipa aliran data (Flow) internal yang bisa diubah
    private val _scannedDevicesFlow = MutableStateFlow<List<ScannedDevice>>(emptyList())

    //pipa aliran data (Flow) eksternal yang hanya bisa dibaca yang dipantau oleh ViewModel
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevicesFlow.asStateFlow()

    // Sengaja nullable: perangkat tanpa hardware bluetooth akan mengembalikan null di sini.
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    /**
     * Adapter di-resolve ulang setiap akses, BUKAN disimpan sebagai val di constructor.
     * Alasannya: butuh disimpan null-safety (getAdapter() bisa null), dan objek adapter
     * bisa berganti setelah pengguna mematikan/menyalakan bluetooth.
     */
    private val bluetoothAdapter get() = bluetoothManager?.adapter

    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Proses hasil scan di sini
            super.onScanResult(callbackType, result)

            // BluetoothDevice.getName() membutuhkan BLUETOOTH_CONNECT di API 31+.
            // Izin bisa saja ditolak atau dicabut saat aplikasi berjalan, jadi
            // SecurityException tidak boleh lolos keluar dari dalam callback.
            val device = try {
                result.device.name ?: "Unknown Device"
            } catch (e: SecurityException) {
                Log.w(TAG, "Nama perangkat tidak terbaca, BLUETOOTH_CONNECT dicabut?", e)
                "Unknown Device"
            }

            val macAddress = result.device.address
            val rssi = result.rssi

            //data yang didapat dikumpulkan dan dikirim ke vm

            //cetak data dengan model yang dibuat
            val scannedDevice = ScannedDevice(device, macAddress, rssi)

            //simpan data ke dalam map
            scannedDevicesMap[macAddress] = scannedDevice

            //perbarui aliran data
            _scannedDevicesFlow.value = scannedDevicesMap.values.toList()

            //1. Bungkus data yang didapat kedalam bentuk tabel (DeviceEntity)
            val entity = DeviceEntity(
                macAddress = macAddress,
                deviceName = device, // Variabel 'device' ini berisi nama atau "Unknown Device"
                timeStamp = System.currentTimeMillis() // Ambil waktu saat ini (dalam milidetik)
            )

            //2. Kirim ke database melalui background thread (Dispatchers.IO)
            CoroutineScope(Dispatchers.IO).launch{
                deviceDao.insertDevice(entity)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            // Catatan: callback ini jalan di thread binder, bukan main thread,
            // jadi jangan menyentuh UI langsung dari sini.
            // Penyampaian kegagalan ke UI sengaja ditunda ke branch berikutnya,
            // karena butuh StateFlow status yang baru.
            Log.e(TAG, "Scan gagal dimulai, errorCode=$errorCode")
            isScanning = false
        }
    }

    /**
     * Memulai pemindaian hanya kalau semua prasyaratnya terpenuhi.
     *
     * startScan() melempar IllegalStateException saat adapter nonaktif dan
     * SecurityException saat izin kurang - keduanya akan menutup aplikasi kalau
     * tidak diceg lebih dulu. Karena itu prasyarat dicek eksplisit dan hasilnya
     * dikembalikan ke UI sebagai penjelasan.
     *
     * URUTAN PEMERIKSAAN PENTING. BluetoothAdapter.getBluetoothLeScanner()
     * mengembalikan null ketika LE sedang nonaktif, bukan hanya ketika LE tidak
     * didukung hardware. Jadi isEnabled() harus dicek lebih dulu; kalau tidak,
     * BT yang dimatikan akan dilaporkan sebagai "tidak mendukung LE" dan
     * pemeriksaan isEnabled() tidak pernah tereksekusi.
     */
    // ReturnCount dilewati secara sengaja: pola guard clause di sini justru
    // yang membuat urutan pemeriksaan prasyarat terbaca jelas. Menyatukannya
    // jadi satu blok if/else hanya membuat kode ini lebih sulit dibaca.
    @Suppress("ReturnCount")
    fun startScan(): ScanLaunchResult {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            Log.w(TAG, "Adapter Bluetooth tidak tersedia di perangkat ini")
            return ScanLaunchResult.BluetoothUnavailable
        }

        if (!BlePermissions.isGranted(context)) {
            Log.w(TAG, "Izin Bluetooth belum diberikan, scan dibatalkan")
            return ScanLaunchResult.PermissionDenied
        }

        // WAJIB dicek sebelum getBluetoothLeScanner(): AOSP mengembalikan null dari
        // getBluetoothLeScanner() ketika LE sedang nonaktif, bukan hanya saat
        // hardwarenya tidak ada. Kalau urutannya dibalik, BT mati akan terbaca
        // sebagai "perangkat tidak mendukung LE" dan pesan yang tampil menyesatkan.
        if (!adapter.isEnabled) {
            Log.w(TAG, "Bluetooth sedang nonaktif, scan dibatalkan")
            return ScanLaunchResult.BluetoothDisabled
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.w(TAG, "Bluetooth LE tidak tersedia di perangkat ini")
            return ScanLaunchResult.BluetoothUnavailable
        }

        if (!BlePermissions.isLocationServiceEnabled(context)) {
            Log.w(TAG, "Layanan lokasi nonaktif, hasil scan akan kosong")
            return ScanLaunchResult.LocationServiceDisabled
        }

        //konfigurasi agar responsif
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        return try {
            scanner.startScan(null, settings, scanCallback)
            isScanning = true
            Log.d(TAG, "Scan dimulai")
            ScanLaunchResult.Started
        } catch (e: IllegalStateException) {
            // Jaring pengaman: prasyarat di atas sudah dicek, tapi kondisi bisa
            // berubah antara pemeriksaan dan pemanggilan.
            Log.e(TAG, "Gagal memulai scan: adapter tidak aktif", e)
            isScanning = false
            ScanLaunchResult.Failed(e.message ?: "Bluetooth tidak aktif")
        } catch (e: SecurityException) {
            Log.e(TAG, "Gagal memulai scan: izin kurang", e)
            isScanning = false
            ScanLaunchResult.Failed(e.message ?: "Izin ditolak")
        }
    }

    fun stopScan() {
        if (!isScanning) return

        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            Log.d(TAG, "Scan dihentikan")
        } catch (e: IllegalStateException) {
            // Adapter dimatikan saat scan berjalan, jadi tidak ada apa pun untuk dihentikan.
            Log.w(TAG, "stopScan dilewati karena adapter tidak aktif", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "stopScan gagal: izin kurang", e)
        } finally {
            isScanning = false
        }
    }
}
