package com.example.blescanner.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Instance yang sama dengan ScannerFragment/RadarFragment karena semuanya
    // activity-scoped. Dipakai untuk menghentikan scan saat aplikasi background.
    private val viewModel: ScannerViewModel by viewModels()

    /**
     * Pernahkah kita benar-benar menampilkan dialog permintaan izin ke user?
     *
     * Dipakai untuk membedakan dua arti dari shouldShowRequestPermissionRationale()
     * yang sama-sama bernilai false: "belum pernah diminta" (masih boleh launch)
     * dan "sudah ditolak permanen" (launch tidak akan menampilkan apa pun).
     * Tanpa penanda ini, user yang baru memasang aplikasi akan diberi dialog
     * "buka pengaturan" padahal belum pernah menolak apa pun.
     */
    private var hasRequestedPermission = false

    /**
     * Apakah kita sedang menunggu user kembali dari halaman Pengaturan aplikasi?
     *
     * Android tidak memberi tahu activity saat user menutup Pengaturan, jadi
     * pemeriksaan harus dilakukan di onResume(). Penanda ini membatasi pemeriksaan
     * itu hanya terjadi setelah user benar-benar menekan tombol "Buka Pengaturan",
     * supaya tidak muncul Toast di setiap kali aplikasi naik ke foreground.
     */
    private var awaitingSettingsReturn = false

    //Permission Request
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Toast.makeText(this, getString(R.string.izin_diberikan), Toast.LENGTH_SHORT).show()
            } else {
                // Dua kemungkinan yang harus diperlakukan berbeda:
                // 1. User masih boleh diminta lagi (baru ditolak sekali) - sistem
                //    akan menampilkan dialog lagi di peluncuran berikutnya.
                // 2. User sudah menolak dua kali atau menekan "Jangan tanya lagi"
                //    (Android 11+), sistem TIDAK akan menampilkan dialog lagi.
                //    Memanggil launch() lagi hanya membuang waktu dan membingungkan,
                //    jadi di kasus ini kita arahkan ke Pengaturan aplikasi.
                //
                // Di dalam callback ini kita pasti sudah pernah meminta, jadi tidak
                // perlu memeriksa hasRequestedPermission lagi.
                if (BlePermissions.isPermanentlyDenied(this)) {
                    showPermanentlyDeniedDialog()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.izin_ditolak),
                        Toast.LENGTH_LONG
                    ).show()
                }
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

    override fun onResume() {
        super.onResume()

        // User baru saja kembali dari Pengaturan. Periksa sendiri apakah izin sudah
        // diaktifkan, karena tidak ada callback yang dikirim ke aplikasi.
        if (awaitingSettingsReturn) {
            awaitingSettingsReturn = false
            if (BlePermissions.isGranted(this)) {
                Toast.makeText(this, getString(R.string.izin_diberikan), Toast.LENGTH_SHORT).show()
            }
        }
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
        if (hasRequiredPermissions()) return

        // Kalau user sudah menolak permanen, jangan buang waktu dengan memanggil
        // launch() karena sistem tidak akan menampilkan dialog apa pun lagi.
        // Langsung tunjukkan jalan keluar ke Pengaturan.
        if (hasRequestedPermission && BlePermissions.isPermanentlyDenied(this)) {
            showPermanentlyDeniedDialog()
            return
        }

        // Daftar izin dibangun di BlePermissions supaya tidak terduplikasi.
        hasRequestedPermission = true
        permissionLauncher.launch(BlePermissions.required.toTypedArray())
    }

    /**
     * Menampilkan dialog yang mengarahkan user ke Pengaturan aplikasi.
     *
     * Ini satu-satunya jalan keluar ketika Android berhenti menampilkan dialog
     * permintaan izin (ditolak dua kali atau "Jangan tanya lagi"). Tanpa dialog ini
     * user akan melihat tombol scan yang terkunci tanpa penjelasan dan tidak tahu
     * harus ke mana.
     */
    private fun showPermanentlyDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.izin_permanen_ditolak_judul)
            .setMessage(R.string.izin_permanen_ditolak_pesan)
            .setPositiveButton(R.string.buka_pengaturan) { _, _ ->
                awaitingSettingsReturn = true
                openAppSettings()
            }
            .setNegativeButton(R.string.nanti, null)
            .show()
    }

    /**
     * Membuka halaman Pengaturan aplikasi ini. Tidak ada API resmi untuk membuka
     * langsung ke daftar izin, jadi kita arahkan ke halaman detail aplikasi dan
     * biarkan user menekan "Izin" sendiri.
     */
    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }


}
