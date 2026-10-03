package com.example.blescanner.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.R
import com.example.blescanner.data.local.DeviceEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<DeviceEntity, HistoryAdapter.HistoryViewHolder>(HistoryViewHolder.DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvMacAddress: TextView = itemView.findViewById(R.id.tvMacAddress)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(device: DeviceEntity) {
            // Implementation for binding device data to the view
            tvDeviceName.text = device.deviceName
            tvMacAddress.text = device.macAddress

            //Convert angka timestamp (Long) ke format tanggal & waktu yang rapi
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = Date(device.timeStamp)
            tvTimestamp.text = "Terakhir dilihat: ${sdf.format(date)}"
        }


        //DiffUtil membantu RecyclerView hanya memperbarui baris data yang berubah, bukan me-refresh seluruh daftar
        class DiffCallback : DiffUtil.ItemCallback<DeviceEntity>() {
            override fun areItemsTheSame(oldItem: DeviceEntity, newItem: DeviceEntity): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: DeviceEntity, newItem: DeviceEntity): Boolean {
                return oldItem == newItem
            }
        }


    }





}
