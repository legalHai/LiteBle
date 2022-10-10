package com.merit.liteble.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.merit.liteble.BleManager
import com.merit.liteble.callback.BleIndicateCallback
import com.merit.liteble.callback.BleNotifyCallback
import com.merit.liteble.exception.OtherException
import com.merit.liteble.exception.TimeOutException
import kotlinx.coroutines.*
import java.util.*

/**
 * @Description
 * @Author lk
 * @Date 2022/10/10 15:30
 */
class BleConnector(bleBluetooth: BleBluetooth) {

    companion object {
        val UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb"
    }

    private var mBleBluetooth: BleBluetooth = bleBluetooth
    private var mBluetoothGatt: BluetoothGatt? = bleBluetooth.getBleGatt()
    private var mBluetoothGattService: BluetoothGattService? = null
    private var mBluetoothGattCharacteristic: BluetoothGattCharacteristic? = null
    private var job: Job? = null

    private fun withUUID(serviceUUID: UUID?, characteristicUUID: UUID?): BleConnector {
        mBluetoothGatt?.let { gatt ->
            serviceUUID?.let {
                mBluetoothGattService = gatt.getService(serviceUUID)
            }
        }
        mBluetoothGattService?.let { service ->
            characteristicUUID?.let {
                mBluetoothGattCharacteristic = service.getCharacteristic(characteristicUUID)
            }
        }
        return this
    }

    private fun withUUIDString(serviceUUID: String?, characteristicUUID: String?): BleConnector {
        return withUUID(UUID.fromString(serviceUUID), UUID.fromString(characteristicUUID))
    }

    fun enableCharacteristicNotify(
        bleNotifyCallback: BleNotifyCallback?,
        uuidNotify: String,
        useCharacteristicDescriptor: Boolean
    ) {
        mBluetoothGattCharacteristic?.let {
            if ((it.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                handleCharacterNotifyCallback(bleNotifyCallback, uuidNotify)
                setCharacteristicNotification(
                    mBluetoothGatt,
                    mBluetoothGattCharacteristic,
                    useCharacteristicDescriptor,
                    true,
                    bleNotifyCallback
                )
            } else {
                bleNotifyCallback?.onNotifyFailure(OtherException("this characteristic not support notify!"))
            }
        } ?: let {
            bleNotifyCallback?.onNotifyFailure(OtherException("this characteristic not support notify!"))
        }
    }

    fun disableCharacteristicNotify(useCharacteristicDescriptor: Boolean): Boolean {
        mBluetoothGatt?.let {
            mBluetoothGattCharacteristic?.let {
                if ((it.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    return setCharacteristicNotification(mBluetoothGatt, mBluetoothGattCharacteristic, useCharacteristicDescriptor, true, null)
                }
            }
        }
        return false
    }

    fun handleCharacterNotifyCallback(bleNotifyCallback: BleNotifyCallback?, uuidNotify: String) {
        job?.cancel()
        bleNotifyCallback?.let {
            mBleBluetooth.addNotifyCallback(uuidNotify, it)
            //add timeout
            operateNotifyTimeout(bleNotifyCallback)
        }
    }

    fun setCharacteristicNotification(
        bluetoothGatt: BluetoothGatt?,
        bluetoothGattCharacteristic: BluetoothGattCharacteristic?,
        useCharacteristicDescriptor: Boolean,
        enable: Boolean,
        bleNotifyCallback: BleNotifyCallback?
    ): Boolean {
        if (bluetoothGatt == null && bluetoothGattCharacteristic == null) {
            bleNotifyCallback?.onNotifyFailure(OtherException("gatt or characteristic equal null"))
            return false
        }
        bluetoothGatt?.let { gatt ->
            bluetoothGattCharacteristic?.let {
                var isSuccess = gatt.setCharacteristicNotification(it, enable)
                if (!isSuccess) {
                    bleNotifyCallback?.onNotifyFailure(OtherException("gatt setCharacteristicNotification fail"))
                }
            }
        }

        var descriptor = if (useCharacteristicDescriptor) {
            bluetoothGattCharacteristic?.getDescriptor(bluetoothGattCharacteristic.uuid)
        } else {
            bluetoothGattCharacteristic?.getDescriptor(
                UUID.fromString(
                    UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
                )
            )
        }

        descriptor?.let {
            it.value = if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            bluetoothGatt?.let { gatt ->
                var descSuccess = gatt.writeDescriptor(it)
                if (!descSuccess) {
                    bleNotifyCallback?.onNotifyFailure(OtherException("gatt writeDescriptor fail"))
                }
                return descSuccess
            }
        } ?: let {
            bleNotifyCallback?.onNotifyFailure(OtherException("gatt writeDescriptor fail"))
        }
        return false
    }

    private fun operateNotifyTimeout(bleNotifyCallback: BleNotifyCallback?) {
        job = GlobalScope.launch(Dispatchers.IO) {
            delay(BleManager.instance.getOperateTimeout())
            bleNotifyCallback?.onNotifyFailure(TimeOutException())
        }
    }

    private fun indicateTimeout(bleIndicateCallback: BleIndicateCallback?) {
        job = GlobalScope.launch(Dispatchers.IO) {
            delay(BleManager.instance.getOperateTimeout())
            bleIndicateCallback?.onIndicateFailure(TimeOutException())
        }
    }

    private fun handleIndicateCallback(
        bleIndicateCallback: BleIndicateCallback?,
        uuidIndicate: String
    ) {
        bleIndicateCallback?.let {
            mBleBluetooth.addIndicateCallback(uuidIndicate, it)
            //add timeout
            indicateTimeout(bleIndicateCallback)
        }
    }

}