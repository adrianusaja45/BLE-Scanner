package com.example.blescanner.ui.scanner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.databinding.ItemDevicesBinding
import com.example.blescanner.model.ScannedDevice


class DeviceAdapter : ListAdapter<ScannedDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    // 1. ViewHolder: Wadah untuk menampung referensi komponen UI di item_device.xml
    inner class DeviceViewHolder(private val binding: ItemDevicesBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: ScannedDevice) {
            binding.tvDeviceName.text = device.name.ifBlank { "Unknown Device" }
            binding.tvMacAddress.text = device.mac
            binding.tvRssi.text = device.rssi.toString()
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