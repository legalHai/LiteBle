package com.merit.liteble.bluetooth

import android.bluetooth.*
import android.os.Build
import com.merit.liteble.bean.BleContext
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.callback.*
import com.merit.liteble.exception.OtherException
import com.merit.liteble.utils.BleLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * @Description
 * @Author lk
 * @Date 2022/10/9 14:11
 */
class BleBluetooth(bleDevice: BleDevice) {

    private var bleGattCallback: BleGattCallback? = null
    private var bleRssiCallback: BleRssiCallback? = null
    private var bleMtuChangedCallback: BleMtuChangedCallback? = null
    private var bleNotifyCallbackHashMap = mutableMapOf<String,BleNotifyCallback>()
    private var bleIndicateCallbackHashMap = mutableMapOf<String,BleIndicateCallback>()
    private var bleReadCallbackHashMap = mutableMapOf<String,BleReadCallback>()
    private var bleWriteCallbackHashMap = mutableMapOf<String,BleWriteCallback>()

    private var lastState: LastState? = LastState.CONNECT_IDLE
    private var isActiveDisconnect: Boolean = false
    private var bleDevice: BleDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectRetryCount: Int = 0
    private var job: Job? = null
    private var connectStateMap = ConcurrentHashMap<String,Any>()

    @Synchronized
    fun addBleGattCallback(bleGattCallback: BleGattCallback?){
        this.bleGattCallback = bleGattCallback
    }

    @Synchronized
    fun addBleRssiCallback(bleRssiCallback: BleRssiCallback?){
        this.bleRssiCallback = bleRssiCallback
    }

    @Synchronized
    fun addBleMtuChangedCallback(bleMtuChangedCallback: BleMtuChangedCallback?){
        this.bleMtuChangedCallback = bleMtuChangedCallback
    }

    @Synchronized
    fun removeBleGattCallback(){
        bleGattCallback = null
    }

    @Synchronized
    fun removeBleRssiCallback(){
        bleRssiCallback = null
    }

    @Synchronized
    fun removeBleMtuChangedCallback(){
        bleMtuChangedCallback = null
    }

    @Synchronized
    fun addNotifyCallback(uuid: String, bleNotifyCallback: BleNotifyCallback){
        bleNotifyCallbackHashMap[uuid] = bleNotifyCallback
    }

    @Synchronized
    fun addIndicateCallback(uuid: String, bleIndicateCallback: BleIndicateCallback){
        bleIndicateCallbackHashMap[uuid] = bleIndicateCallback
    }

    @Synchronized
    fun addReadCallback(uuid: String, bleReadCallback: BleReadCallback){
        bleReadCallbackHashMap[uuid] = bleReadCallback
    }

    @Synchronized
    fun addWriteCallback(uuid: String, bleWriteCallback: BleWriteCallback){
        bleWriteCallbackHashMap[uuid] = bleWriteCallback
    }

    @Synchronized
    fun removeNotifyCallback(uuid: String){
        if(bleNotifyCallbackHashMap.containsKey(uuid)){
            bleNotifyCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeIndicateCallback(uuid: String){
        if(bleIndicateCallbackHashMap.containsKey(uuid)){
            bleIndicateCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeReadCallback(uuid: String){
        if(bleReadCallbackHashMap.containsKey(uuid)){
            bleReadCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeWriteCallback(uuid: String){
        if(bleWriteCallbackHashMap.containsKey(uuid)){
            bleWriteCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun clearCharacterCallback(){
        bleNotifyCallbackHashMap.clear()
        bleIndicateCallbackHashMap.clear()
        bleReadCallbackHashMap.clear()
        bleWriteCallbackHashMap.clear()
    }

    fun getBleDevice(): BleDevice?{
        return bleDevice
    }

    fun getBleGatt(): BluetoothGatt?{
        return bluetoothGatt
    }

    @Synchronized
    fun connect(bleDevice: BleDevice, autoConnect: Boolean, bleGattCallback: BleGattCallback?): BluetoothGatt?{
        return connect(bleDevice,autoConnect,bleGattCallback,0)
    }

    @Synchronized
    fun connect(bleDevice: BleDevice, autoConnect: Boolean, bleGattCallback: BleGattCallback?, retryCount: Int): BluetoothGatt?{

        if (retryCount == 0) {
            this.connectRetryCount = 0
        }
        addBleGattCallback(bleGattCallback)
        lastState = LastState.CONNECT_CONNECTING
        bluetoothGatt = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            bleDevice.mDevice?.connectGatt(BleContext.getContext(),autoConnect,coreBluetoothGattCallback,BluetoothDevice.TRANSPORT_LE)
        } else {
            bleDevice.mDevice?.connectGatt(BleContext.getContext(),autoConnect,coreBluetoothGattCallback)
        }
        bluetoothGatt?.let {
            bleGattCallback?.startConnect()
            connectControl()
        }?: let {
            disconnectGatt()
            refreshDeviceCache()
            closeBluetoothGatt()
            lastState = LastState.CONNECT_FAILURE

            bleGattCallback?.onConnectFail(bleDevice,OtherException("GATT connect exception occurred!"))
        }
        return bluetoothGatt
    }

    @Synchronized
    fun connectControl(){

        job = GlobalScope.launch(Dispatchers.IO){
            while (true){
                dealConnectState()
            }
        }
    }

    @Synchronized
    private fun dealConnectState(){

    }

    @Synchronized
    fun disconnectGatt(){
        bluetoothGatt?.disconnect()
    }

    @Synchronized
    fun refreshDeviceCache(){
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
    fun closeBluetoothGatt(){
        bluetoothGatt?.close()
    }

    @Synchronized
    fun destroy(){
        lastState = LastState.CONNECT_IDLE
        disconnectGatt()
        refreshDeviceCache()
        closeBluetoothGatt()
        removeBleGattCallback()
        removeBleRssiCallback()
        removeBleMtuChangedCallback()
        clearCharacterCallback()

    }

    private var coreBluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            bluetoothGatt = gatt

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
        }
    }

    enum class LastState{
        CONNECT_IDLE,
        CONNECT_CONNECTING,
        CONNECT_CONNECTED,
        CONNECT_FAILURE,
        CONNECT_DISCONNECT
    }
}