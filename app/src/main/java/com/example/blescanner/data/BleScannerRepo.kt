package com.example.blescanner.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class BleScannerRepo @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
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
            val macAdress = result.device.address
            val rssi = result.rssi

            //data yang didapat dikumpulkan dan dikirim ke vm
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

