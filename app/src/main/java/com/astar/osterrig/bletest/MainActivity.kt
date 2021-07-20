package com.astar.osterrig.bletest

import android.Manifest
import android.bluetooth.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.astar.osterrig.bletest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val deviceAdapter = DeviceAdapter()

    private var bluetoothGatt: BluetoothGatt? = null


    private val onClickItemDevice = object : DeviceAdapter.Callback {
        override fun onItemClick(device: BluetoothDevice) {
            connectToDevice(device)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    showToastMainThread("Connected to $deviceAddress")
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    showToastMainThread("Disconnected $deviceAddress")
                    gatt.close()
                }
            } else {
                showToastMainThread("Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
            super.onConnectionStateChange(gatt, status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.w(
                "BluetoothGattCallback",
                "Discovered $ ${gatt.services.size} services for ${gatt.device.address}"
            )
            gatt.printGattTable()
            super.onServicesDiscovered(gatt, status)
        }
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            binding.buttonScan.text = getString(R.string.stop_scan)
            viewModel.startScan()
        } else {
            Toast.makeText(this, getString(R.string.scan_error_message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        setupViews()
        subscribe()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopScan()
    }

    private fun setupViews() = with(binding) {
        recycler.setHasFixedSize(true)
        recycler.adapter = deviceAdapter
        deviceAdapter.addCallback(onClickItemDevice)

        buttonScan.setOnClickListener {
            if (viewModel.isScanning()) {
                binding.buttonScan.text = getString(R.string.start_scan)
                stopScan()
            } else {
                viewModel.clearResults()
                startScan()
            }
        }
    }

    private fun startScan() {
        locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun stopScan() {
        viewModel.stopScan()
    }

    private fun subscribe() = with(viewModel) {
        scanResultsLiveData.observe(this@MainActivity, { results ->
            deviceAdapter.setItems(results.map { it.device })
        })
    }

    private fun connectToDevice(device: BluetoothDevice) {
        // TODO: 20.07.2021 connect to device
        if (viewModel.isScanning()) {
            viewModel.stopScan()
        }
        device.connectGatt(this, false, gattCallback)
    }

    private fun showToastMainThread(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i(
                "printGattTable",
                "No services and characteristics available, call discoverServices() first?"
            )
            return
        }

        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i(
                "printGattTable",
                "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )

        }
    }

    private fun BluetoothGattCharacteristic.isReadable() : Boolean {
        return containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)
    }

    private fun BluetoothGattCharacteristic.isWritable() : Boolean {
        return containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)
    }

    private fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean {
        return containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
    }

    private fun BluetoothGattCharacteristic.containsProperty(property: Int) : Boolean {
        return properties and property != 0
    }
}

