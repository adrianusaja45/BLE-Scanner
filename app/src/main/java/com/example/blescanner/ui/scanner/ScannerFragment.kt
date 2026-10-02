package com.example.blescanner.ui.scanner

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.blescanner.R
import com.example.blescanner.databinding.FragmentScannerBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScannerFragment : Fragment() {
    // ViewBinding boilerplate untuk Fragment
    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

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

    }

    override fun onDestroyView() {
        super.onDestroyView()
        //untuk menghindari memory leak
        _binding = null
    }
}