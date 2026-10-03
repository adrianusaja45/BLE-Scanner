package com.example.blescanner.ui.radar

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.blescanner.R
import com.example.blescanner.databinding.FragmentRadarBinding
import com.example.blescanner.ui.scanner.ScannerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RadarFragment : Fragment() {

    private var _binding: FragmentRadarBinding? = null
    private val binding get() = _binding!!

    //WAJIB activityViewModels(): harus instance yang SAMA dengan ScannerFragment,
    // kalau tidak state target MAC tidak akan terlihat di halaman ini.
    private val viewModel: ScannerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRadarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Ambil target MAC dari Bundle. Jika tidak ada, hentikan proses (null)
        val targetMac = arguments?.getString("MAC_ADDRESS") ?: return

        // 1. Beritahu ViewModel perangkat mana yang ingin dilacak
        viewModel.setTrackingTarget(targetMac)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 2. Pantaulah LANGSUNG targetDevice.
                // Jangan collect(scannedDevice) lalu collect(targetDevice) di dalamnya:
                // collect() itu suspend loop yang tak pernah selesai, sehingga blok
                // luarnya hanya dijalankan sekali (dead code).
                // Catatan: nilai awal targetDevice adalah null (stateIn initialValue),
                // jadi null SAAT INI belum tentu berarti sinyal hilang.
                viewModel.targetDevice.collect { device ->
                    if (device != null) {
                        binding.radarView.updateDevices(listOf(device))
                        binding.tvRadarStatus.visibility = View.GONE
                    } else {
                        // Kosongkan radar. Gunakan TextView, bukan Toast,
                        // karena Toast memunculkan ulang tiap sinyal sempat putus 1 siklus scan.
                        binding.radarView.updateDevices(emptyList())
                        binding.tvRadarStatus.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearTrackingTarget()
        _binding = null
    }
}