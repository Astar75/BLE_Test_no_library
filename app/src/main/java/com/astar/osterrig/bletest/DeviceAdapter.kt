package com.astar.osterrig.bletest

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val items = ArrayList<BluetoothDevice>()

    fun setItems(newDevices: List<BluetoothDevice>) {
        this.items.clear()
        this.items.addAll(newDevices)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DeviceViewHolder(
        LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_device, parent, false)
    )

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textName = itemView.findViewById<TextView>(R.id.textName)
        private val textAddress = itemView.findViewById<TextView>(R.id.textAddress)

        fun bind(item: BluetoothDevice) {
            textName.text = item.name ?: itemView.context.getString(R.string.device_unnamed)
            textAddress.text = item.address
        }
    }
}

