package com.merit.liteble.bluetooth

import android.bluetooth.BluetoothDevice
import com.merit.liteble.BleManager
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.utils.BleLruHashmap

/**
 * @Description 多设备管理
 * @Author lk
 * @Date 2022/10/10 13:41
 */
class MultiBluetoothManager {

    private var bleLruHashmap: BleLruHashmap<String, BleBluetooth> =
        BleLruHashmap(BleManager.instance.getMaxConnectDevice())
    private var bleTempHashmap = mutableMapOf<String, BleBluetooth>()

    @Synchronized
    fun buildConnectingBle(bleDevice: BleDevice): BleBluetooth {
        var bleBluetooth = BleBluetooth(bleDevice)
        if (!bleTempHashmap.containsKey(bleBluetooth.getDeviceKey())) {
            bleTempHashmap[bleBluetooth.getDeviceKey()] = bleBluetooth
        }
        return bleBluetooth
    }

    @Synchronized
    fun removeConnectingBle(bleBluetooth: BleBluetooth?) {
        bleBluetooth?.let {
            bleTempHashmap.remove(it.getDeviceKey())
        }
    }

    @Synchronized
    fun addBleBluetooth(bleBluetooth: BleBluetooth?) {
        bleBluetooth?.let {
            if (!bleLruHashmap.containsKey(it.getDeviceKey())) {
                bleLruHashmap[it.getDeviceKey()] = it
            }
        }
    }

    @Synchronized
    fun removeBleBluetooth(bleBluetooth: BleBluetooth?) {
        bleBluetooth?.let {
            bleLruHashmap.remove(it.getDeviceKey())
        }
    }

    @Synchronized
    fun isContainsDevice(bleDevice: BleDevice?): Boolean {
        bleDevice?.let {
            return bleLruHashmap.containsKey(it.getKey())
        }
        return false
    }

    @Synchronized
    fun isContainsDevice(bluetoothDevice: BluetoothDevice?): Boolean {
        bluetoothDevice?.let {
            return bleLruHashmap.containsKey(it.name + it.address)
        }
        return false
    }

    @Synchronized
    fun getBleBluetooth(bleDevice: BleDevice?): BleBluetooth? {
        bleDevice?.let {
            return bleLruHashmap[it.getKey()]
        }
        return null
    }

    @Synchronized
    fun disconnect(bleDevice: BleDevice?) {
        if (isContainsDevice(bleDevice)) {
            getBleBluetooth(bleDevice)?.disconnect()
        }
    }

    @Synchronized
    fun disconnectAllDevice() {
        bleLruHashmap.values.forEach {
            it.disconnect()
        }
        bleLruHashmap.clear()
    }

    @Synchronized
    fun destroy() {
        bleLruHashmap.values.forEach {
            it.destroy()
        }
        bleLruHashmap.clear()
        bleTempHashmap.values.forEach {
            it.destroy()
        }
        bleTempHashmap.clear()
    }

    @Synchronized
    fun getBleBluetoothList(): List<BleBluetooth>{
        var bleBluetoothList = bleLruHashmap.values.toMutableList()
        bleBluetoothList.sortedBy { bluetooth ->
            bluetooth.getDeviceKey()
        }
        return bleBluetoothList
    }

    @Synchronized
    fun getDeviceList(): List<BleDevice> {
        var bleDeviceList = mutableListOf<BleDevice>()
        bleLruHashmap.values.forEach { it ->
            it.getBleDevice()?.let {
                bleDeviceList.add(it)
            }
        }
        return bleDeviceList
    }

    @Synchronized
    fun refreshConnectedDevice(){
        var bleBluetoothList = getBleBluetoothList()
        bleBluetoothList.forEach {
            if (!BleManager.instance.isConnected(it.getBleDevice())) {
                removeBleBluetooth(it)
            }
        }
    }

}