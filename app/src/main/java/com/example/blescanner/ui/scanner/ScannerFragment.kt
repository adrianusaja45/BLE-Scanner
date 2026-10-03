package com.example.blescanner.ui.scanner

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blescanner.R
import com.example.blescanner.data.ScanLaunchResult
import com.example.blescanner.databinding.FragmentScannerBinding
import com.example.blescanner.model.BluetoothStatus
import com.example.blescanner.ui.radar.RadarFragment
import com.example.blescanner.util.BlePermissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScannerFragment : Fragment() {
    // ViewBinding boilerplate untuk Fragment
    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    //WAJIB activityViewModels(): RadarFragment juga memakai ViewModel ini agar
//state scan / search / filter / target tetap sama di kedua halaman.
    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var devicesAdapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //Mengikat layout xml ke kode kotlin
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // untuk manipulasi UI
        //Listener SearchBar
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s.toString())
            }
        })

        //Listener Slider RSSI
        binding.sliderRssi.addOnChangeListener { _, value, _ ->
            val minRssi = value.toInt()

            binding.tvRssiFilterLabel.text = "Batas Min RSSI: $minRssi dBm"

            viewModel.setMinRssiFilter(minRssi)
        }

        // 1. Siapkan Adapter dan pasang ke RecyclerView
        setupRecyclerView()

        // 2. Nyalakan pemantau data dari ViewModel
        observeViewModel()

        // 3. Mulai/Stop pemindaian BLE
        binding.btnStartStop.setOnClickListener {
            if (viewModel.isScanning.value) {
                //Jika sedang scan, hentikan
                viewModel.stopScan()
            } else {
                // startScan() mengembalikan alasan kalau gagal, jadi UI bisa
                // menjelaskan masalahnya alih-alih aplikasi crash.
                val result = viewModel.startScan()
                when (result) {
                    is ScanLaunchResult.Started -> {
                        binding.tvStatus.text = getString(R.string.memindai_perangkat)
                    }
                    else -> {
                        val message = result.toUserMessage()
                        binding.tvStatus.text = message
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        // Izin bisa saja berubah setelah user keluar ke Pengaturan lalu kembali,
        // jadi tombol dievaluasi ulang setiap kali halaman ini terlihat.
        updateScanAvailability()
    }

    /**
     * Nonaktifkan tombol scan kalau prasyarat belum terpenuhi, supaya user
     * mendapat penjelasan sebelum menekan, bukan crash atau diam-diam gagal.
     */
    private fun updateScanAvailability() {
        val hasPermission = BlePermissions.isGranted(requireContext())
        binding.btnStartStop.isEnabled = hasPermission

        if (!hasPermission && !viewModel.isScanning.value) {
            binding.tvStatus.text = getString(R.string.izin_bt_belum_diberikan)
        }
    }

    private fun ScanLaunchResult.toUserMessage(): String = when (this) {
        is ScanLaunchResult.Started -> getString(R.string.memindai_perangkat)
        is ScanLaunchResult.BluetoothUnavailable -> getString(R.string.bt_tidak_tersedia)
        is ScanLaunchResult.BluetoothDisabled -> getString(R.string.bluetooth_nonaktif)
        is ScanLaunchResult.PermissionDenied -> getString(R.string.izin_bt_dibutuhkan)
        is ScanLaunchResult.LocationServiceDisabled -> getString(R.string.lokasi_nonaktif)
        is ScanLaunchResult.Failed -> getString(R.string.gagal_memulai_scan, reason)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //untuk menghindari memory leak
        _binding = null
    }

    private fun setupRecyclerView() {
        // ACTION klik Setup diconfigure SEKALI di awal, bukan di dalam lambda klik
        devicesAdapter = DeviceAdapter { selectedDevice ->
            navigateToRadarScreen(selectedDevice.mac)
        }

        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = devicesAdapter
            setHasFixedSize(true)
        }
    }

    private fun navigateToRadarScreen(macAddress: String) {
        val radarFragment = RadarFragment().apply {
            arguments = Bundle().apply {
                putString("MAC_ADDRESS", macAddress)
            }
        }

        //Pindah Fragment dan tambahkan Backstack agar bisa kembali ke Fragment sebelumnya
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, radarFragment)
            .addToBackStack(null)
            .commit()
    }


    //untuk mengobservasi LiveData dari ViewModel
    private fun observeViewModel() {
        //membuka ruang kerja (scope) yang terikat dengan umur tampilan Fragment
        viewLifecycleOwner.lifecycleScope.launch {

            //Pemantauan hanya aktif saat layar sedang tampil (STARTED)
            // Jika aplikasi disembunyikan ke background, pemantauan otomatis jeda agar hemat baterai
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Ketiga status ini dulu ditulis dari tiga collector terpisah yang
                // saling menimpa tvStatus, jadi hasilnya tidak bisa diprediksi.
                // Sekarang digabung jadi satu sumber teks.
                launch {
                    combine(
                        viewModel.isScanning,
                        viewModel.bluetoothStatus,
                        viewModel.scannedDevice
                    ) { scanning, bluetooth, devices ->
                        Triple(scanning, bluetooth, devices)
                    }.collect { (scanning, bluetooth, devices) ->
                        binding.btnStartStop.text = getString(
                            if (scanning) R.string.stop_scan else R.string.mulai_scan
                        )

                        // Urutan dari paling penting: kalau Bluetooth mati, user
                        // harus tahu itu dulu - bukan melihat jumlah perangkat
                        // terakhir yang sekarang sudah basi.
                        binding.tvStatus.text = when (bluetooth) {
                            BluetoothStatus.DISABLED,
                            BluetoothStatus.TURNING_OFF ->
                                getString(R.string.bt_mati_scan_berhenti)

                            BluetoothStatus.TURNING_ON ->
                                getString(R.string.bt_menyala)

                            BluetoothStatus.ENABLED -> if (scanning) {
                                getString(R.string.jumlah_perangkat, devices.size)
                            } else {
                                getString(R.string.scan_dihentikan)
                            }
                        }

                        //Kirim Data ke Adapter
                        devicesAdapter.submitList(devices)

                        // Cetak perangkat terkuat ke Logcat (jika daftar tidak kosong)
                        if (devices.isNotEmpty()) {
                            val topDevice = devices.first()
                            Log.d("BleScanner", "Terkuat: ${topDevice.name} | ${topDevice.rssi} dBm")
                        }
                    }
                }
            }
        }
    }
}
