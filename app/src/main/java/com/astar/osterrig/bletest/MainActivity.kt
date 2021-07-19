package com.astar.osterrig.bletest

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.astar.osterrig.bletest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val deviceAdapter = DeviceAdapter()

    @RequiresApi(Build.VERSION_CODES.M)
    private val locationPermission = registerForActivityResult(RequestPermission()) { granted ->
        when {
            granted -> {
                viewModel.startScan()
                Toast.makeText(this, "Scanning", Toast.LENGTH_SHORT).show()
            }
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                Toast.makeText(this, "Permission rationale", Toast.LENGTH_SHORT).show()
            }
            else -> Toast.makeText(this, "Доступ запрещен", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        setupViews()
        subscribe()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupViews() = with(binding) {
        recycler.setHasFixedSize(true)
        recycler.adapter = deviceAdapter

        buttonScan.setOnClickListener { startScan() }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startScan() {
        locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun subscribe() = with(viewModel) {
        scanResultsLiveData.observe(this@MainActivity, { results ->
            deviceAdapter.setItems(results.map { it.device })
        })
    }
}