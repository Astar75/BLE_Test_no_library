package com.astar.osterrig.bletest

import android.bluetooth.*
import android.content.Context
import android.os.Looper
import android.util.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object ConnectionManager {

    const val TAG = "ConnectionManager"

    private val deviceGattMap = ConcurrentHashMap<BluetoothDevice, BluetoothGatt>()
    private val operationQueue = ConcurrentLinkedQueue<BleOperationType>()
    private var pendingOperation: BleOperationType? = null

    private var connectionCallback: Callback? = null

    fun addCallback(callback: ConnectionManager.Callback) {
        connectionCallback = callback
    }

    fun servicesOnDevice(device: BluetoothDevice): List<BluetoothGattService>? =
        deviceGattMap[device]?.services

    fun connect(device: BluetoothDevice, context: Context) {
        if (device.isConnected()) {
            Log.e(TAG, "Already connected to ${device.address}")
        } else {
            enqueueOperation(Connect(device, context.applicationContext))
        }
    }

    fun teardownConnection(device: BluetoothDevice) {
        if (device.isConnected()) {
            enqueueOperation(Disconnect(device))
        } else {
            Log.e(TAG, "Not connected to ${device.address}, cannot teardown connection!")
        }
    }

    fun writeCharacteristic(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ) {
        val writeType = when {
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else -> {
                Log.e(TAG, "Characteristic ${characteristic.uuid} cannot be written to")
                return
            }
        }

        if (device.isConnected()) {
            enqueueOperation(CharacteristicWrite(device, characteristic.uuid, writeType, data))
        } else {
            Log.e(TAG, "Not connected to ${device.address}")
        }
    }

    @Synchronized
    private fun enqueueOperation(operation: BleOperationType) {
        operationQueue.add(operation)
        if (pendingOperation == null) {
            doNextOperation()
        }
    }

    @Synchronized
    private fun signalEndOfOperation() {
        Log.d(TAG, "End of $pendingOperation")
        pendingOperation = null
        if (operationQueue.isNotEmpty()) {
            doNextOperation()
        }
    }

    @Synchronized
    private fun doNextOperation() {
        if (pendingOperation != null) {
            Log.e(TAG, "doNextOperation() called when operation is pending ")
            return
        }

        val operation = operationQueue.poll() ?: run {
            Log.e(TAG, "Operation queue empty, returning ")
            return
        }
        pendingOperation = operation

        if (operation is Connect) {
            with(operation) {
                Log.w(TAG, "Connection to ${device.address}")
                device.connectGatt(context, false, callback)
            }
        }

        // проверить gatt на доступность для другой операции
        val gatt = deviceGattMap[operation.device]
            ?: this.run {
                Log.e(TAG, "Not connected to ${operation.device.address}! Aborting...")
                signalEndOfOperation()
                return
            }

        when (operation) {
            is Disconnect -> with(operation) {
                Log.w(TAG, "Disconnecting from ${device.address}")
                gatt.close()
                deviceGattMap.remove(device)
                // TODO: 21.07.2021 оповестить об отключении устройства
                signalEndOfOperation()
            }
            is CharacteristicWrite -> with(operation) {
                gatt.findCharacteristic(characteristicUUID)
                    ?.let { characteristic: BluetoothGattCharacteristic ->
                        characteristic.writeType = writeType
                        characteristic.value = data
                        gatt.writeCharacteristic(characteristic)
                    } ?: this.run {
                    Log.e(TAG, "Cannot find $characteristicUUID to write to")
                    signalEndOfOperation()
                }
            }
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: connected to $address")
                    deviceGattMap[gatt.device] = gatt
                    android.os.Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e(TAG, "onConnectionStateChange() disconnected from $address")
                    teardownConnection(gatt.device)
                }
            } else {
                Log.e(TAG, "onConnectionStateChange() status $status encountered for $address")
                if (pendingOperation is Connect) {
                    signalEndOfOperation()
                }
                teardownConnection(gatt.device)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(TAG, "Wrote to characteristic $uuid, value: ${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e(TAG, "Write not permitted for $uuid")
                    }
                    else -> {
                        Log.e(TAG, "Characteristic  write failed for $uuid, error: $status")
                    }
                }
            }

            if (pendingOperation is CharacteristicWrite) {
                signalEndOfOperation()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                connectionCallback?.onServiceDiscover(gatt.services)
            } else {
                // TODO: 21.07.2021 Ошибка обнаружения сервисов
            }
        }
    }

    interface Callback {
        fun onServiceDiscover(services: MutableList<BluetoothGattService>)
    }

    private fun BluetoothDevice.isConnected() = deviceGattMap.containsKey(this)

    private fun BluetoothGattCharacteristic.containsProperty(property: Int) =
        properties and property != 0

    private fun BluetoothGattCharacteristic.isWritable() =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    private fun BluetoothGattCharacteristic.isWritableWithoutResponse() =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    private fun BluetoothGatt.findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        services?.forEach { service ->
            service.characteristics?.firstOrNull { characteristic ->
                characteristic.uuid == uuid
            }?.let { matchingCharacteristic ->
                return matchingCharacteristic
            }
        }
        return null
    }

    private fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02x", it) }
}

