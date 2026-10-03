package com.example.blescanner.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.blescanner.R
import com.example.blescanner.ui.history.HistoryFragment
import com.example.blescanner.ui.scanner.ScannerFragment
import com.example.blescanner.ui.scanner.ScannerViewModel
import com.example.blescanner.util.BlePermissions
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Instance yang sama dengan ScannerFragment/RadarFragment karena semuanya
    // activity-scoped. Dipakai untuk menghentikan scan saat aplikasi background.
    private val viewModel: ScannerViewModel by viewModels()

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

    override fun onStart() {
        super.onStart()
        // Lanjutkan scan yang tertahan saat aplikasi masuk background.
        // by viewModels() pada Activity memakai ViewModelStore activity yang sama
        // dengan activityViewModels() di Fragment, jadi ini instance yang sama.
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()

        // Rotasi, ganti tema, dan ganti bahasa juga memicu onStop(), tapi activity
        // sedang direkonstruksi - bukan benar-benar background. Kalau scan dihentikan
        // di sana, setiap rotasi akan mematikan scan dan user harus menekan ulang.
        if (isChangingConfigurations) return

        viewModel.onAppBackgrounded()
    }



    // FUNGSI 1: Hanya bertugas mengecek, tidak melakukan apa-apa selain menjawab True/False
    private fun hasRequiredPermissions(): Boolean = BlePermissions.isGranted(this)

    // FUNGSI 2: Bertugas menindaklanjuti hasil dari Fungsi 1
    private fun checkAndRequestPermissions() {
        if (!hasRequiredPermissions()) {
            // Daftar izin dibangun di BlePermissions supaya tidak terduplikasi.
            permissionLauncher.launch(BlePermissions.required.toTypedArray())
        }

    }


}
