package com.example.blescanner.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.example.blescanner.model.ScannedDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class BleScannerRepo @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    //wadah sementara data di map agar mudah menemukan dan memperbarui data berdasarkan MAC Address
    private val scannedDevicesMap = mutableMapOf<String, ScannedDevice>()

    //Pipa aliran data (Flow) internal yang bisa diubah
    private val _scannedDevicesFlow = MutableStateFlow<List<ScannedDevice>>(emptyList())

    //pipa aliran data (Flow) eksternal yang hanya bisa dibaca yang dipantau oleh ViewModel
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevicesFlow.asStateFlow()

    //mengambil layanan bluetooth sistem android
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Proses hasil scan di sini
            super.onScanResult(callbackType, result)

            val device = result.device.name ?: "Unknown Device"
            val macAddress = result.device.address
            val rssi = result.rssi

            //data yang didapat dikumpulkan dan dikirim ke vm

            //cetak data dengan model yang dibuat
            val scannedDevice = ScannedDevice(device, macAddress, rssi)

            //simpan data ke dalam map
            scannedDevicesMap[macAddress] = scannedDevice

            //perbarui aliran data
            _scannedDevicesFlow.value = scannedDevicesMap.values.toList()
        }
    }

    fun startScan() {
        //konfigurasi agar responsif
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()


        bluetoothLeScanner?.startScan(null, settings, scanCallback)

    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        //berhenti scan
        bluetoothLeScanner?.stopScan(scanCallback)
    }
}

