package com.example.blescanner.ui.scanner

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.blescanner.databinding.FragmentScannerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScannerFragment : Fragment() {
    // ViewBinding boilerplate untuk Fragment
    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    //Hilt otomatis mencarikaan dan menyuntikkan ScannerViewModel ke sini
    private val viewModel: ScannerViewModel by viewModels()

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
        observeViewModel()
        viewModel.startScan()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        //untuk menghindari memory leak
        _binding = null
    }

    //untuk mengobservasi LiveData dari ViewModel
    private fun observeViewModel() {
        //membuka ruang kerja (scope) yang terikat dengan umur tampilan Fragment
        viewLifecycleOwner.lifecycleScope.launch {

            //Pemantauan hanya aktif saat layar sedang tampil (STARTED)
            // Jika aplikasi disembunyikan ke background, pemantauan otomatis jeda agar hemat baterai
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Pantau StateFlow 'scannedDevices' dari ViewModel
                viewModel.scannedDevice.collect { devices ->
                    // Setiap kali ada perangkat baru, blok ini akan otomatis tereksekusi
                    // Untuk sementara, kita ubah teks di layar sesuai jumlah perangkat
                    binding.tvStatus.text = "Menemukan ${devices.size} perangkat"

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