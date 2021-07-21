package com.astar.osterrig.bletest

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.*

sealed class BleOperationType {
    abstract val device: BluetoothDevice
}

data class Connect(
    override val device: BluetoothDevice,
    val context: Context
) : BleOperationType()

data class Disconnect(
    override val device: BluetoothDevice
) : BleOperationType()

data class CharacteristicWrite(
    override val device: BluetoothDevice,
    val characteristicUUID: UUID,
    val writeType: Int,
    val data: ByteArray
): BleOperationType() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharacteristicWrite

        if (device != other.device) return false
        if (characteristicUUID != other.characteristicUUID) return false
        if (writeType != other.writeType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + characteristicUUID.hashCode()
        result = 31 * result + writeType
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class CharacteristicRead(
    override val device: BluetoothDevice,
    val characteristicUUID: UUID
): BleOperationType()

data class MtuRequest(
    override val device: BluetoothDevice,
    val mtu: Int
): BleOperationType()