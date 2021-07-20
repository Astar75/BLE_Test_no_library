package com.astar.osterrig.bletest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class MainViewModel : ViewModel() {

    private companion object {
        const val TAG = "MainViewModel"
    }

    private val adapter = BluetoothAdapter.getDefaultAdapter()

    private val scanResults = mutableMapOf<String, ScanResult>()
    private var scannerCallback: DeviceScannerCallback? = null

    private var scanner: BluetoothLeScanner? = null

    private val scanSettings: ScanSettings
    private val scanFilters: List<ScanFilter>

    private val _scanResultsLiveData = MutableLiveData<List<ScanResult>>()
    val scanResultsLiveData: LiveData<List<ScanResult>> = _scanResultsLiveData

    init {
        scanSettings = buildScanSettings()
        scanFilters = buildScanFilters()
    }

    fun isScanning() = scannerCallback != null

    fun startScan() {
        if (scannerCallback == null) {
            scanner = adapter.bluetoothLeScanner
            scannerCallback = DeviceScannerCallback()
            scanner?.startScan(scanFilters, scanSettings, scannerCallback)
        } else {
            Log.e("MainViewModel", "startScan: Сканнирование уже запущено")
        }
    }

    fun stopScan() {
        if (scannerCallback != null) {
            scanner?.stopScan(scannerCallback!!)
            scannerCallback = null
        }
    }

    fun clearResults() {
        scanResults.clear()
        _scanResultsLiveData.value = scanResults.values.toList()
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
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { scanResult ->
                scanResults[scanResult.device.address] = scanResult
            }
            _scanResultsLiveData.value = scanResults.values.toList()
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { scanResult ->
                scanResults[scanResult.device.address] = scanResult
            }
            _scanResultsLiveData.value = scanResults.values.toList()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "onScanFailed: $errorCode")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }


}

