package com.astar.osterrig.bletest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private val scanResults = mutableMapOf<String, ScanResult>()
    private var scannerCallback: DeviceScannerCallback? = null
    private var scanner: BluetoothLeScanner? = null

    private val scanSettings: ScanSettings
    private val scanFilters: List<ScanFilter>

    private val _scanResultsLiveData = MutableLiveData<List<ScanResult>>()
    val scanResultsLiveData : LiveData<List<ScanResult>> = _scanResultsLiveData

    init {
        scanSettings = buildScanSettings()
        scanFilters = buildScanFilters()
    }

    fun startScan() {
        if (scannerCallback == null) {
            scanner = adapter.bluetoothLeScanner
            scannerCallback = DeviceScannerCallback()
            scanner?.startScan(scannerCallback)
        } else {
            Log.e("MainViewModel", "startScan: Сканнирование уже запущено")
        }
    }

    fun stopScan() {
        if (scannerCallback != null) {
            scanner?.stopScan(scannerCallback)
            scannerCallback = null
        }
    }

    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
    }

    private fun buildScanFilters(): List<ScanFilter> {
        val builder = ScanFilter.Builder()
        val filter = builder.build()
        return listOf(filter)
    }

    private inner class DeviceScannerCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.device?.let {
                scanResults[it.address] = result
                _scanResultsLiveData.value = scanResults.values.toList()
                Log.e("MainViewModel", "onScanResult: ${result.device.address}")
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { result ->
                scanResults[result.device.address] = result
                _scanResultsLiveData.value = scanResults.values.toList()
                Log.e("MainViewModel", "onBatchScanResults: ${result.device.address}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("MainViewModel", "onScanFailed: $errorCode")
        }
    }
}

