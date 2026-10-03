package com.example.blescanner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.blescanner.R
import com.example.blescanner.ui.history.HistoryFragment
import com.example.blescanner.ui.scanner.ScannerFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        //pengecekan untuk mencegah penumpukan fragment saat user pindah - pindah halaman
        if (savedInstanceState == null) {
            // Initial setup
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ScannerFragment())
                .commit()
        }

        //Pengecekan untuk mencegah penumpukan fragment saat user pindah - pindah halaman
        if (savedInstanceState == null) {
            // Initial setup
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ScannerFragment())
                .commit()
        }

        //Logika Navigasi
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener { item ->
            val  selectedFragment = when (item.itemId) {
                R.id.nav_scanner -> ScannerFragment()
                R.id.nav_history -> HistoryFragment()
                else -> return@setOnItemSelectedListener false
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit()
            true
        }

        checkAndRequestPermissions()

    }



    // FUNGSI 1: Hanya bertugas mengecek, tidak melakukan apa-apa selain menjawab True/False
    private fun hasRequiredPermissions(): Boolean {

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

        //Mengecek apakah izin sudah diberikan atau belum
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // FUNGSI 2: Bertugas menindaklanjuti hasil dari Fungsi 1
    private fun checkAndRequestPermissions() {
        if (!hasRequiredPermissions()) {
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

            // Meminta izin dari pengguna
            permissionLauncher.launch(requiredPermissions)
        }

    }


}

