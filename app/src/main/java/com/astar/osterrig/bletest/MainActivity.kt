package com.astar.osterrig.bletest

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.astar.osterrig.bletest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val deviceAdapter = DeviceAdapter()

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            binding.buttonScan.text = getString(R.string.stop_scan)
            viewModel.startScan()
        } else {
            Toast.makeText(this, getString(R.string.scan_error), Toast.LENGTH_SHORT).show()
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
}