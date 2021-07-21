package com.astar.osterrig.bletest

import android.Manifest
import android.bluetooth.*
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.astar.osterrig.bletest.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var device: BluetoothDevice
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val deviceAdapter = DeviceAdapter()

    private var serviceControlUuid: UUID = UUID.fromString("d4d4dc12-0493-44fa-bc55-477388a6565c")
    private var characteristicControlUuid: UUID =
        UUID.fromString("faba7480-acd6-4136-bac9-b4e812233e01")

    private var serviceControl: BluetoothGattService? = null
    private var characteristicControl: BluetoothGattCharacteristic? = null

    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }

    private val onClickItemDevice = object : DeviceAdapter.Callback {
        override fun onItemClick(device: BluetoothDevice) {
            stopScan()
            this@MainActivity.device = device
            ConnectionManager.connect(device, this@MainActivity)
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

    private val onLightnessChanges = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (progress % 5 == 0) {
                sendLightness(progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            sendLightness(seekBar.progress)
        }
    }

    private fun sendLightness(lightness: Int) {
        val command = byteArrayOf(0x1, lightness.toByte())
        characteristicControl?.let {
            ConnectionManager.writeCharacteristic(
                device,
                it, command
            )
        }
    }

    private val onClickColorButtonListener = View.OnClickListener {
        when (it) {
            binding.buttonRedColor -> sendColor(Color.RED)
            binding.buttonYellowColor -> sendColor(Color.YELLOW)
            binding.buttonGreenColor -> sendColor(Color.GREEN)
            binding.buttonCyanColor -> sendColor(Color.CYAN)
            binding.buttonBlueColor -> sendColor(Color.BLUE)
        }
    }

    private fun sendColor(@ColorInt color: Int) {
        val command = byteArrayOf(
            0x16,
            Color.red(color).toByte(),
            Color.green(color).toByte(),
            Color.blue(color).toByte()
        )
        characteristicControl?.let {
            ConnectionManager.writeCharacteristic(
                device,
                it, command
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val connectionManagerListener = object : ConnectionManager.Callback {
            override fun onServiceDiscover(services: MutableList<BluetoothGattService>) {
                serviceControl = services.firstOrNull { it.uuid == serviceControlUuid }
                serviceControl?.let { control ->
                    characteristicControl =
                        control.characteristics.firstOrNull { it.uuid == characteristicControlUuid }
                }
            }
        }

        ConnectionManager.addCallback(connectionManagerListener)

        setupViews()
        subscribe()
    }

    override fun onStop() {
        super.onStop()
        ConnectionManager.teardownConnection(device)
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

        buttonRedColor.setOnClickListener(onClickColorButtonListener)
        buttonYellowColor.setOnClickListener(onClickColorButtonListener)
        buttonGreenColor.setOnClickListener(onClickColorButtonListener)
        buttonCyanColor.setOnClickListener(onClickColorButtonListener)
        buttonBlueColor.setOnClickListener(onClickColorButtonListener)

        seekBarIntensity.setOnSeekBarChangeListener(onLightnessChanges)

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
}
