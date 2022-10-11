package com.merit.liteble.bluetooth

import android.bluetooth.*
import android.os.Build
import android.text.TextUtils
import com.merit.liteble.BleManager
import com.merit.liteble.bean.BleContext
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.callback.*
import com.merit.liteble.exception.BleException
import com.merit.liteble.exception.GattException
import com.merit.liteble.exception.OtherException
import com.merit.liteble.exception.TimeOutException
import com.merit.liteble.utils.BleLog
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeoutException

/**
 * @Description
 * @Author lk
 * @Date 2022/10/9 14:11
 */
class BleBluetooth(bleDevice: BleDevice?) {

    private var bleGattCallback: BleGattCallback? = null
    private var bleRssiCallback: BleRssiCallback? = null
    private var bleMtuChangedCallback: BleMtuChangedCallback? = null
    private var bleNotifyCallbackHashMap = mutableMapOf<String, BleNotifyCallback>()
    private var bleIndicateCallbackHashMap = mutableMapOf<String, BleIndicateCallback>()
    private var bleReadCallbackHashMap = mutableMapOf<String, BleReadCallback>()
    private var bleWriteCallbackHashMap = mutableMapOf<String, BleWriteCallback>()

    private var lastState: LastState? = LastState.CONNECT_IDLE
    private var isActiveDisconnect: Boolean = false
    private var bleDevice: BleDevice? = bleDevice
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectRetryCount: Int = 0
    private var job: Job? = null
    private var connectStateMap = ConcurrentHashMap<String, Any>()


    fun createBleConnector(): BleConnector {
        return BleConnector(this)
    }
    @Synchronized
    fun addBleGattCallback(bleGattCallback: BleGattCallback?) {
        this.bleGattCallback = bleGattCallback
    }

    @Synchronized
    fun addBleRssiCallback(bleRssiCallback: BleRssiCallback?) {
        this.bleRssiCallback = bleRssiCallback
    }

    @Synchronized
    fun addBleMtuChangedCallback(bleMtuChangedCallback: BleMtuChangedCallback?) {
        this.bleMtuChangedCallback = bleMtuChangedCallback
    }

    @Synchronized
    fun removeBleGattCallback() {
        bleGattCallback = null
    }

    @Synchronized
    fun removeBleRssiCallback() {
        bleRssiCallback = null
    }

    @Synchronized
    fun removeBleMtuChangedCallback() {
        bleMtuChangedCallback = null
    }

    @Synchronized
    fun addNotifyCallback(uuid: String, bleNotifyCallback: BleNotifyCallback) {
        bleNotifyCallbackHashMap[uuid] = bleNotifyCallback
    }

    @Synchronized
    fun addIndicateCallback(uuid: String, bleIndicateCallback: BleIndicateCallback) {
        bleIndicateCallbackHashMap[uuid] = bleIndicateCallback
    }

    @Synchronized
    fun addReadCallback(uuid: String, bleReadCallback: BleReadCallback) {
        bleReadCallbackHashMap[uuid] = bleReadCallback
    }

    @Synchronized
    fun addWriteCallback(uuid: String, bleWriteCallback: BleWriteCallback) {
        bleWriteCallbackHashMap[uuid] = bleWriteCallback
    }

    @Synchronized
    fun removeNotifyCallback(uuid: String) {
        if (bleNotifyCallbackHashMap.containsKey(uuid)) {
            bleNotifyCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeIndicateCallback(uuid: String) {
        if (bleIndicateCallbackHashMap.containsKey(uuid)) {
            bleIndicateCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeReadCallback(uuid: String) {
        if (bleReadCallbackHashMap.containsKey(uuid)) {
            bleReadCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeWriteCallback(uuid: String) {
        if (bleWriteCallbackHashMap.containsKey(uuid)) {
            bleWriteCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun clearCharacterCallback() {
        bleNotifyCallbackHashMap.clear()
        bleIndicateCallbackHashMap.clear()
        bleReadCallbackHashMap.clear()
        bleWriteCallbackHashMap.clear()
    }

    /**
     * 获取设备key值
     */
    fun getDeviceKey(): String{
        return bleDevice?.getKey()?:""
    }

    fun getBleDevice(): BleDevice? {
        return bleDevice
    }

    fun getBleGatt(): BluetoothGatt? {
        return bluetoothGatt
    }

    @Synchronized
    fun connect(
        bleDevice: BleDevice,
        autoConnect: Boolean,
        bleGattCallback: BleGattCallback?
    ): BluetoothGatt? {
        return connect(bleDevice, autoConnect, bleGattCallback, 0)
    }

    @Synchronized
    fun connect(
        bleDevice: BleDevice,
        autoConnect: Boolean,
        bleGattCallback: BleGattCallback?,
        retryCount: Int
    ): BluetoothGatt? {

        if (retryCount == 0) {
            this.connectRetryCount = 0
        }
        addBleGattCallback(bleGattCallback)
        lastState = LastState.CONNECT_CONNECTING
        bluetoothGatt = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            bleDevice.mDevice?.connectGatt(
                BleContext.getContext(),
                autoConnect,
                coreBluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            bleDevice.mDevice?.connectGatt(
                BleContext.getContext(),
                autoConnect,
                coreBluetoothGattCallback
            )
        }
        job?.cancel()
        bluetoothGatt?.let {
            bleGattCallback?.startConnect()
            connectTimeOutControl()
        } ?: let {
            disconnectGatt()
            refreshDeviceCache()
            closeBluetoothGatt()
            lastState = LastState.CONNECT_FAILURE

            bleGattCallback?.onConnectFail(
                bleDevice,
                OtherException("GATT connect exception occurred!")
            )
        }
        return bluetoothGatt
    }

    @Synchronized
    fun connectTimeOutControl() {
        job = GlobalScope.launch(Dispatchers.IO) {
            delay(BleManager.instance.getConnectTimeout())
            stateConnectOverTime()
        }
    }

    @Synchronized
    fun disconnectGatt() {
        bluetoothGatt?.disconnect()
    }

    @Synchronized
    fun refreshDeviceCache() {
        try {
            val refresh = BluetoothGatt::class.java.getMethod("refresh")
            if (refresh != null && bluetoothGatt != null) {
                val success = refresh.invoke(bluetoothGatt) as Boolean
                BleLog.i("refreshDeviceCache, is success:  $success")
            }
        } catch (e: Exception) {
            BleLog.i("exception occur while refreshing device: " + e.message)
            e.printStackTrace()
        }
    }

    @Synchronized
    fun closeBluetoothGatt() {
        bluetoothGatt?.close()
    }

    @Synchronized
    fun disconnectBluetoothGatt() {
        bluetoothGatt?.disconnect()
    }

    @Synchronized
    fun disconnect() {
        isActiveDisconnect = true
        disconnectBluetoothGatt()
    }

    @Synchronized
    fun destroy() {
        lastState = LastState.CONNECT_IDLE
        disconnectGatt()
        refreshDeviceCache()
        closeBluetoothGatt()
        removeBleGattCallback()
        removeBleRssiCallback()
        removeBleMtuChangedCallback()
        clearCharacterCallback()
    }

    @Synchronized
    private fun stateReconnect() {
        bleDevice?.let {
            connect(it, false, bleGattCallback, connectRetryCount)
        }
    }

    @Synchronized
    private fun stateDiscoverFailed() {
        disconnectGatt()
        refreshDeviceCache()
        closeBluetoothGatt()
        lastState = LastState.CONNECT_FAILURE
        bleDevice?.let {
            bleGattCallback?.onConnectFail(
                it,
                OtherException("GATT discover services exception occurred!")
            )
        }
    }

    @Synchronized
    private fun stateDiscoverSuccess() {
        lastState = LastState.CONNECT_CONNECTED
        isActiveDisconnect = false

        bleDevice?.let { device ->
            bluetoothGatt?.let {
                job?.cancel()
                bleGattCallback?.onConnectSuccess(device, it, BluetoothGatt.GATT_SUCCESS)
            }
        }
    }

    @Synchronized
    private fun stateConnectFailed() {
        disconnectGatt()
        refreshDeviceCache()
        closeBluetoothGatt()
        lastState = LastState.CONNECT_FAILURE

        if (connectRetryCount < BleManager.instance.getConnectRetryCount()) {
            ++connectRetryCount

            stateReconnect()
        } else {
            stateDiscoverFailed()
        }
    }

    @Synchronized
    private fun stateConnectOverTime() {
        disconnectGatt()
        refreshDeviceCache()
        closeBluetoothGatt()
        lastState = LastState.CONNECT_FAILURE
        bleDevice?.let {
            bleGattCallback?.onConnectFail(it, TimeOutException())
        }
    }

    @Synchronized
    private fun stateDisconnect() {
        lastState = LastState.CONNECT_DISCONNECT
        disconnect()
        refreshDeviceCache()
        closeBluetoothGatt()
        removeBleMtuChangedCallback()
        removeBleRssiCallback()
        clearCharacterCallback()
        bleDevice?.let { device ->
            bluetoothGatt?.let {
                bleGattCallback?.onDisConnected(
                    isActiveDisconnect,
                    device,
                    it,
                    BluetoothProfile.STATE_DISCONNECTED
                )
            }
        }
    }

    private var coreBluetoothGattCallback: BluetoothGattCallback =
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                bluetoothGatt = gatt
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    var isDiscovered = bluetoothGatt?.discoverServices()
                    if (isDiscovered == false) {
                        stateDiscoverFailed()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (lastState == LastState.CONNECT_CONNECTING) {
                        stateConnectFailed()
                    } else if (lastState == LastState.CONNECT_CONNECTED) {
                        stateDisconnect()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                bluetoothGatt = gatt

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    stateDiscoverSuccess()
                } else {
                    stateDiscoverFailed()
                }
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt?,
                descriptor: BluetoothGattDescriptor?,
                status: Int
            ) {
                super.onDescriptorWrite(gatt, descriptor, status)
                bleNotifyCallbackHashMap.forEach { map ->
                    descriptor?.let {
                        if (map.key.equals(it.uuid.toString(), true)) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                map.value.onNotifySuccess()
                            } else {
                                map.value.onNotifyFailure(GattException(status))
                            }
                        }
                    }
                }
                bleIndicateCallbackHashMap.forEach { map ->
                    descriptor?.let {
                        if (map.key.equals(it.uuid.toString(), true)) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                map.value.onIndicateSuccess()
                            } else {
                                map.value.onIndicateFailure(GattException(status))
                            }
                        }
                    }
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                bleReadCallbackHashMap.forEach { map ->
                    characteristic?.let {
                        if (map.key.equals(it.uuid.toString(), true)) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                map.value.onReadSuccess(it.value)
                            } else {
                                map.value.onReadFailure(GattException(status))
                            }
                        }
                    }
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                bleWriteCallbackHashMap.forEach { map ->
                    characteristic?.let {
                        if (map.key.equals(it.uuid.toString(), true)) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                map.value.onWriteSuccess(1, 1, it.value)
                            } else {
                                map.value.onWriteFailure(GattException(status))
                            }
                        }
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                bleNotifyCallbackHashMap.forEach { map ->
                    characteristic?.let {
                        if (map.key.equals(it.uuid.toString(), true)) {
                            map.value.onCharacteristicChanged(it.value)
                        }
                    }
                }
                bleIndicateCallbackHashMap.forEach { map ->
                    characteristic?.let {
                        if (map.key.equals(it.uuid.toString(), true)) {
                            map.value.onCharacteristicChanged(it.value)
                        }
                    }
                }
            }

            override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                super.onReadRemoteRssi(gatt, rssi, status)
                bleRssiCallback?.let {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        it.onRssiSuccess(rssi)
                    } else {
                        it.onRssiFailure(GattException(status))
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                bleMtuChangedCallback?.let {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        it.onMtuChanged(mtu)
                    } else {
                        it.onSetMtuFailure(GattException(status))
                    }
                }
            }
        }

    enum class LastState {
        CONNECT_IDLE,
        CONNECT_CONNECTING,
        CONNECT_CONNECTED,
        CONNECT_FAILURE,
        CONNECT_DISCONNECT
    }
}