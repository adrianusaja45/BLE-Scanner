package com.example.blescanner.ui.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

//Memberi tahu Hilt untuk menyuntikkan dependencies (seperti ViewModel) ke Fragment ini
@AndroidEntryPoint
class HistoryFragment : Fragment() {

    // 2. Mengambil ViewModel yang sudah disiapkan oleh Hilt
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //3. Setup Adapter dan RecyclerView
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvHistory)
        historyAdapter = HistoryAdapter()

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter

        //4. setup tombol clear
        val btnDeleteHistory = view.findViewById<Button>(R.id.btnDeleteHistory)
        btnDeleteHistory.setOnClickListener {
            viewModel.deleteAllHistory()
        }

        //5. Mengamati perubahan data di ViewModel
        // repeatOnLifecycle(STARTED) memastikan aplikasi hanya memantau database saat layar ini sedang dibuka
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.historyList.collect{ historyList ->
                    // Memasukkan data terbaru ke dalam adapter
                    // DiffUtil di dalam adapter akan otomatis mendeteksi baris mana yang perlu diperbarui
                    historyAdapter.submitList(historyList)
                }
            }
        }
    }
}
