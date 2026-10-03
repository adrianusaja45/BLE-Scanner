package com.example.blescanner.ui.scanner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.databinding.ItemDevicesBinding
import com.example.blescanner.model.ScannedDevice


class DeviceAdapter (private val onDeviceClick: (ScannedDevice) -> Unit) : ListAdapter<ScannedDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    // 1. ViewHolder: Wadah untuk menampung referensi komponen UI di item_device.xml
    inner class DeviceViewHolder(private val binding: ItemDevicesBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: ScannedDevice) {
            binding.tvDeviceName.text = device.name.ifBlank { "Unknown Device" }
            binding.tvMacAddress.text = device.mac
            binding.tvRssi.text = "${device.rssi} dBm"

            //konversi RSSI ke estimasi jarak
            val distanceEstimation = when(device.rssi){
                in -30..0 -> "<1 m (Sangat Kuat)"
                in -50..-31 -> "1-3 m (Kuat)"
                in -70..-51 -> "3-10 m (Cukup)"
                in -80..-71 -> "10-20 m (Lemah)"
                in -90..-81 -> "> 20 m (Sangat Lemah)"
                else -> "Di luar jangkauan (Lost)"
            }
            binding.tvDistance.text = distanceEstimation

            binding.root.setOnClickListener() {
                onDeviceClick(device)
            }
        }
    }
    // 2. Membuat kotak kartu baru saat dibutuhkan oleh RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDevicesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        return DeviceViewHolder(binding)
    }
    // 3. Menempelkan data perangkat ke dalam kartu pada posisi tertentu
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = getItem(position)
        holder.bind(device)
    }

    // 4. DiffUtil: Otak cerdas untuk membandingkan daftar lama dan daftar baru agar animasi mulus
    class DeviceDiffCallback : DiffUtil.ItemCallback<ScannedDevice>() {
        override fun areItemsTheSame(oldItem: ScannedDevice, newItem: ScannedDevice): Boolean {
            // Perangkat dianggap sama jika MAC Address-nya identik
            return oldItem.mac == newItem.mac
        }

        override fun areContentsTheSame(oldItem: ScannedDevice, newItem: ScannedDevice): Boolean {
            // Perangkat dianggap sama jika semua properti sama
            return oldItem == newItem
        }
    }

}