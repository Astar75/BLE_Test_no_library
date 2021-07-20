package com.astar.osterrig.bletest

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val items = ArrayList<BluetoothDevice>()
    private var callback: Callback? = null

    fun setItems(newDevices: List<BluetoothDevice>) {
        this.items.clear()
        this.items.addAll(newDevices)
        notifyDataSetChanged()
    }

    fun addCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size

    interface Callback {
        fun onItemClick(device: BluetoothDevice)
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textName = itemView.findViewById<TextView>(R.id.textName)
        private val textAddress = itemView.findViewById<TextView>(R.id.textAddress)

        init {
            itemView.setOnClickListener { callback?.onItemClick(items[adapterPosition]) }
        }

        fun bind(item: BluetoothDevice) {
            textName.text = item.name ?: "Unnamed"
            textAddress.text = item.address
        }
    }
}

