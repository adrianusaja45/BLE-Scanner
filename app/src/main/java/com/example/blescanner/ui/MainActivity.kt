package com.example.blescanner.ui

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.blescanner.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    //Permission Request
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                //Do something
                Toast.makeText(this, "Izin Diberikan, Silahkan Scan BLE", Toast.LENGTH_SHORT).show()
            } else {
                //Do something else
                Toast.makeText(
                    this,
                    "Izin Ditolak, Silahkan Mengizinkan Aplikasi Terlebih Dahulu Untuk Melacak BLE",
                    Toast.LENGTH_SHORT
                ).show()
            }


        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkAndRequestPermissions()


    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                //untuk Android 12 atau lebih besar
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                //untuk Android 11 atau dibawah
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

        //Memunculkan Pop up permintaan izin
        permissionLauncher.launch(requiredPermissions)
    }
}

