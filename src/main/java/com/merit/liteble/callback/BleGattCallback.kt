package com.merit.liteble.callback

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.exception.BleException

/**
 * @Description 连接回调
 * @Author lk
 * @Date 2022/10/9 14:25
 */
abstract class BleGattCallback: BluetoothGattCallback() {

    abstract fun startConnect()

    abstract fun onConnectFail(bleDevice: BleDevice, bleException: BleException)

    abstract fun onConnectSuccess(bleDevice: BleDevice, bluetoothGatt: BluetoothGatt, status: Int)

    abstract fun onDisConnected(isActiveDisConnected: Boolean, bleDevice: BleDevice, bluetoothGatt: BluetoothGatt, status: Int)

}