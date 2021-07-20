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
            Toast.makeText(this, "Фиаско потерплено!", Toast.LENGTH_SHORT).show()
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
        // the new method
        locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        // the old method
        /* if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Permission rationale", Toast.LENGTH_SHORT).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_CODE)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_CODE)
            }
            return
        }*/
    }

    private fun stopScan() {
        viewModel.stopScan()
    }

    private fun subscribe() = with(viewModel) {
        scanResultsLiveData.observe(this@MainActivity, { results ->
            deviceAdapter.setItems(results.map { it.device })
        })
    }

    // the old method
    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.startScan()
            } else {
                Toast.makeText(this, "Фиаско потерплено!", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }*/
}