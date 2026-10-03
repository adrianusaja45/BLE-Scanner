package com.example.blescanner.ui.scanner

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.blescanner.R
import com.example.blescanner.databinding.FragmentScannerBinding
import com.example.blescanner.ui.radar.RadarFragment
import dagger.hilt.android.AndroidEntryPoint
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
                binding.btnStartStop.text = "Mulai Scan"
                binding.tvStatus.text = "Scan Dihentikan"
            } else {
                // Jika sedang berhenti, jalankan scan
                viewModel.startScan()
                binding.btnStartStop.text = "Stop Scan"
                binding.tvStatus.text = "Memindai Perangkat .."
            }
        }

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

                //Pantau status scanning
                launch {
                    viewModel.isScanning.collect { isScanning ->
                        if (isScanning) {
                            binding.btnStartStop.text = "Stop Scan"
                        } else {
                            binding.btnStartStop.text = "Mulai Scan"
                            binding.tvStatus.text = "Scan Dihentikan"
                        }
                    }
                }

                //Pantau daftar device
                // Pantau StateFlow 'scannedDevices' dari ViewModel
                viewModel.scannedDevice.collect { devices ->
                    // Setiap kali ada perangkat baru, blok ini akan otomatis tereksekusi
                    // Untuk sementara, kita ubah teks di layar sesuai jumlah perangkat
                    binding.tvStatus.text = "Menemukan ${devices.size} perangkat"

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