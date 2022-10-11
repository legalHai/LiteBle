package com.merit.liteble.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.Build
import com.merit.liteble.BleManager
import com.merit.liteble.callback.*
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
    private var writeJob: Job? = null
    private var readJob: Job? = null
    private var mtuJob: Job? = null
    private var rssiJob: Job? = null
    private var notifyJob: Job? = null
    private var indicateJob: Job? = null

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

    fun withUUIDString(serviceUUID: String?, characteristicUUID: String?): BleConnector {
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
                    return setCharacteristicNotification(
                        mBluetoothGatt,
                        mBluetoothGattCharacteristic,
                        useCharacteristicDescriptor,
                        true,
                        null
                    )
                }
            }
        }
        return false
    }

    fun handleCharacterNotifyCallback(bleNotifyCallback: BleNotifyCallback?, uuidNotify: String) {
        notifyJob?.cancel()
        bleNotifyCallback?.let {
            mBleBluetooth.addNotifyCallback(uuidNotify, it)
            //add timeout
            notifyTimeout(bleNotifyCallback)
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
            it.value =
                if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
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

    fun enableCharacteristicIndicate(
        bleIndicateCallback: BleIndicateCallback?,
        uuidIndicate: String,
        useCharacteristicDescriptor: Boolean
    ) {
        mBluetoothGattCharacteristic?.let {
            if ((it.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                handleIndicateCallback(bleIndicateCallback, uuidIndicate)
                setCharacteristicIndication(
                    mBluetoothGatt,
                    it,
                    bleIndicateCallback,
                    useCharacteristicDescriptor,
                    true
                )
            } else {
                bleIndicateCallback?.onIndicateFailure(OtherException("this characteristic not support indicate!"))
            }
        }
    }

    fun disableCharacteristicIndicate(useCharacteristicDescriptor: Boolean): Boolean {
        mBluetoothGattCharacteristic?.let {
            if ((it.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                return setCharacteristicIndication(
                    mBluetoothGatt,
                    it,
                    null,
                    useCharacteristicDescriptor,
                    false
                )
            }
        }
        return false
    }

    fun setCharacteristicIndication(
        bluetoothGatt: BluetoothGatt?,
        bluetoothGattCharacteristic: BluetoothGattCharacteristic?,
        bleIndicateCallback: BleIndicateCallback?,
        useCharacteristicDescriptor: Boolean,
        enable: Boolean,
    ): Boolean {
        if (bluetoothGatt == null && bluetoothGattCharacteristic == null) {
            bleIndicateCallback?.onIndicateFailure(OtherException("gatt or characteristic equal null"))
            return false
        }
        var notifySuccess =
            bluetoothGatt?.setCharacteristicNotification(bluetoothGattCharacteristic, enable)
        if (notifySuccess == false) {
            bleIndicateCallback?.onIndicateFailure(OtherException("gatt setCharacteristicNotification fail"))
            return false
        }
        var descriptor =
            bluetoothGattCharacteristic?.let {
                if (useCharacteristicDescriptor)
                    it.getDescriptor(it.uuid)
                else
                    it.getDescriptor(UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR))
            }
        descriptor?.let {
            it.value =
                if (enable) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            var descriptorSuccess = bluetoothGatt?.writeDescriptor(it)
            if (descriptorSuccess == false) {
                bleIndicateCallback?.onIndicateFailure(OtherException("gatt writeDescriptor fail"))
                return false
            }
            return true
        } ?: let {
            bleIndicateCallback?.onIndicateFailure(OtherException("descriptor equals null"))
        }
        return false
    }

    fun writeCharacteristic(
        data: ByteArray?,
        bleWriteCallback: BleWriteCallback?,
        uuidWrite: String
    ) {
        if (data == null || data.isEmpty()) {
            bleWriteCallback?.onWriteFailure(OtherException("data write is null or empty"))
            return
        }
        mBluetoothGattCharacteristic?.let {
            if (it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
                bleWriteCallback?.onWriteFailure(OtherException("this characteristic not support write!"))
                return
            }
            if (it.setValue(data)) {
                handleCharacteristicWriteCallback(bleWriteCallback, uuidWrite)
                mBluetoothGatt?.let { gatt ->
                    if (!gatt.writeCharacteristic(it)) {
                        bleWriteCallback?.onWriteFailure(OtherException("gatt writeCharacteristic fail"))
                    }
                }
            } else {
                bleWriteCallback?.onWriteFailure(OtherException("Updates the locally stored value of this characteristic fail"))
            }
        }
    }

    fun readCharacteristic(
        bleReadCallback: BleReadCallback?,
        uuidRead: String
    ) {
        mBluetoothGattCharacteristic?.let {
            if ((it.properties and BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                handleCharacteristicReadCallback(bleReadCallback,uuidRead)
                mBluetoothGatt?.let { gatt ->
                    if (!gatt.readCharacteristic(it)) {
                        bleReadCallback?.onReadFailure(OtherException("gatt readCharacteristic fail"))
                    }
                }
            } else {
                bleReadCallback?.onReadFailure(OtherException("this characteristic not support read!"))
            }
        }
    }

    fun readRemoteRssi(bleRssiCallback: BleRssiCallback?){
        handleRssiCallback(bleRssiCallback)
        mBluetoothGatt?.let {
            if(!it.readRemoteRssi()){
                bleRssiCallback?.onRssiFailure(OtherException("gatt readRemoteRssi fail"))
            }
        }
    }

    fun setMtu(mtu: Int,bleMtuChangedCallback: BleMtuChangedCallback?){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            handleMtuCallback(bleMtuChangedCallback)
            mBluetoothGatt?.let {
                if(!it.requestMtu(mtu)){
                    bleMtuChangedCallback?.onSetMtuFailure(OtherException("gatt requestMtu fail"))
                }
            }
        }else{
            bleMtuChangedCallback?.onSetMtuFailure(OtherException("API level lower than 21"))
        }
    }

    fun requestConnectionPriority(connectionPriority: Int): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothGatt?.let {
                return it.requestConnectionPriority(connectionPriority)
            }
        }
        return false
    }

    fun handleMtuCallback(bleMtuChangedCallback: BleMtuChangedCallback?) {
        //add timeout
        writeJob?.cancel()
        bleMtuChangedCallback?.let {
            mBleBluetooth.addBleMtuChangedCallback(it)
            mtuTimeout(it)
        }
    }

    fun handleRssiCallback(bleRssiCallback: BleRssiCallback?) {
        //add timeout
        rssiJob?.cancel()
        bleRssiCallback?.let {
            mBleBluetooth.addBleRssiCallback(it)
            rssiTimeout(it)
        }
    }

    fun handleCharacteristicWriteCallback(
        bleWriteCallback: BleWriteCallback?,
        uuidWrite: String
    ) {
        //add timeout
        writeJob?.cancel()
        bleWriteCallback?.let {
            mBleBluetooth.addWriteCallback(uuidWrite,it)
            writeTimeout(it)
        }
    }

    fun handleCharacteristicReadCallback(
        bleReadCallback: BleReadCallback?,
        uuidRead: String
    ) {
        //add timeout
        readJob?.cancel()
        bleReadCallback?.let {
            mBleBluetooth.addReadCallback(uuidRead,it)
            readTimeout(it)
        }
    }

    private fun mtuTimeout(bleMtuChangedCallback: BleMtuChangedCallback?) {
        mtuJob = GlobalScope.launch(Dispatchers.IO) {
            delay(BleManager.instance.getOperateTimeout())
            bleMtuChangedCallback?.onSetMtuFailure(TimeOutException())
        }
    }
    private fun rssiTimeout(bleRssiCallback: BleRssiCallback?) {
        rssiJob = GlobalScope.launch(Dispatchers.IO) {
            delay(BleManager.instance.getOperateTimeout())
            bleRssiCallback?.onRssiFailure(TimeOutException())
        }
    }
    private fun writeTimeout(bleWriteCallback: BleWriteCallback?) {
        writeJob = GlobalScope.launch(Dispatchers.IO) {
            delay(BleManager.instance.getOperateTimeout())
            bleWriteCallback?.onWriteFailure(TimeOutException())
        }
    }
    private fun readTimeout(bleReadCallback: BleReadCallback?) {
        readJob = GlobalScope.launch(Dispatchers.IO) {
            delay(BleManager.instance.getOperateTimeout())
            bleReadCallback?.onReadFailure(TimeOutException())
        }
    }

    private fun notifyTimeout(bleNotifyCallback: BleNotifyCallback?) {
        notifyJob = GlobalScope.launch(Dispatchers.IO) {
            delay(BleManager.instance.getOperateTimeout())
            bleNotifyCallback?.onNotifyFailure(TimeOutException())
        }
    }

    private fun indicateTimeout(bleIndicateCallback: BleIndicateCallback?) {
        indicateJob = GlobalScope.launch(Dispatchers.IO) {
            delay(BleManager.instance.getOperateTimeout())
            bleIndicateCallback?.onIndicateFailure(TimeOutException())
        }
    }

    private fun handleIndicateCallback(
        bleIndicateCallback: BleIndicateCallback?,
        uuidIndicate: String
    ) {
        indicateJob?.cancel()
        bleIndicateCallback?.let {
            mBleBluetooth.addIndicateCallback(uuidIndicate, it)
            //add timeout
            indicateTimeout(bleIndicateCallback)
        }
    }

}