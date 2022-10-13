package com.merit.liteble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.merit.liteble.bean.BleConstants
import com.merit.liteble.bean.BleContext
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.bluetooth.BleBluetooth
import com.merit.liteble.bluetooth.MultiBluetoothManager
import com.merit.liteble.bluetooth.SplitWriter
import com.merit.liteble.callback.*
import com.merit.liteble.exception.OtherException
import com.merit.liteble.scan.BleScanOption
import com.merit.liteble.scan.BleScanner
import com.merit.liteble.utils.BleLog
import com.merit.liteble.utils.BleUtils

/**
 * @Description 蓝牙搜索连接管理总类
 * @Author lk
 * @Date 2022/9/30 11:09
 */
class BleManager private constructor() {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanOption: BleScanOption? = null
    private var multiBluetoothManager: MultiBluetoothManager? = null
    private var mOperateTimeout: Long? = BleConstants.OPERATE_TIME_OUT
    private var mConnectTimeout: Long? = BleConstants.CONN_TIME_OUT
    private var mReConnectCount: Int? = BleConstants.RECONNECT_COUNT
    private var maxConnectCount: Int? = BleConstants.MAX_MULTIPLE_DEVICE
    private var mSplitWriteNum = BleConstants.WRITE_DATA_SPLIT_COUNT

    companion object {
        @JvmStatic
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BleManager()
        }
    }

    fun init(context: Context) {
        BleContext.setContext(context)
        if (BleUtils.isSupportBle()) {
            mBluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        }
        mBluetoothAdapter = mBluetoothManager?.adapter
        multiBluetoothManager = MultiBluetoothManager()
        mScanOption = BleScanOption.Builder().build()
    }

    /**
     * 初始化扫描配置
     */
    fun initScanOption(scanOption: BleScanOption) {
        this.mScanOption = scanOption
    }

    /**
     * 是否开启log
     */
    fun enableLog(enableLog: Boolean): BleManager {
        BleLog.needPrint = enableLog
        return this
    }

    /**
     * 设置连接超时时间
     */
    fun setConnectTimeOut(connectTimeOut: Long): BleManager {
        this.mConnectTimeout = connectTimeOut
        return this
    }

    /**
     * 设置操作超时时间
     */
    fun setOperateTimeout(operateTimeOut: Long): BleManager {
        this.mOperateTimeout = operateTimeOut
        return this
    }

    fun getOperateTimeout(): Long {
        return this.mOperateTimeout ?: BleConstants.OPERATE_TIME_OUT
    }

    fun getBluetoothScanManager(): BluetoothManager? {
        return mBluetoothManager
    }

    fun getBluetoothScanAdapter(): BluetoothAdapter? {
        return mBluetoothAdapter
    }

    fun getConnectRetryCount(): Int {
        return mReConnectCount ?: BleConstants.RECONNECT_COUNT
    }

    fun getConnectTimeout(): Long {
        return mConnectTimeout ?: BleConstants.CONN_TIME_OUT
    }

    fun getMaxConnectDevice(): Int {
        return maxConnectCount ?: BleConstants.MAX_MULTIPLE_DEVICE
    }

    fun scan(bleScanCallback: BleScanCallback?) {
        requireNotNull(bleScanCallback) { "BleScanCallback can not be Null!" }
        if (!isBlueEnable()) {
            BleLog.e("Bluetooth is not enable")
            bleScanCallback.onScanStart(false)
        }
        mScanOption?.bleScanBean?.let {
            BleScanner.instance.startBleScan(it, bleScanCallback)
        }
    }

    fun scanAndConnect(bleScanAndConnectCallback: BleScanAndConnectCallback?) {
        requireNotNull(bleScanAndConnectCallback) { "bleScanAndConnectCallback can not be Null!" }
        if (!isBlueEnable()) {
            BleLog.e("Bluetooth is not enable")
            bleScanAndConnectCallback.onScanStart(false)
        }
        mScanOption?.bleScanBean?.let {
            if(it.mNeedConnect == BleConstants.SCAN_NOT_CONNECT){
                it.mNeedConnect = BleConstants.SCAN_AND_CONNECT_UNTIL_FINISH
            }
            BleScanner.instance.startBleScan(it, bleScanAndConnectCallback)
        }
    }

    fun connect(bleDevice: BleDevice, bleGattCallback: BleGattCallback?): BluetoothGatt? {
        if (!isBlueEnable()) {
            BleLog.e("Bluetooth not enable!")
            bleGattCallback?.onConnectFail(bleDevice, OtherException("Bluetooth not enable!"))
            return null
        }
        val bleBluetooth = multiBluetoothManager?.buildConnectingBle(bleDevice)
        val isAutoConn = mScanOption?.bleScanBean?.mAutoConnect ?: false
        return bleBluetooth?.connect(bleDevice, isAutoConn, bleGattCallback)
    }

    fun connect(mac: String, bleGattCallback: BleGattCallback?): BluetoothGatt? {
        val bluetoothDevice = mBluetoothAdapter?.getRemoteDevice(mac)
        val bleDevice = BleDevice(bluetoothDevice, null, 0, 0)
        return connect(bleDevice, bleGattCallback)
    }

    fun cancelScan() {
        BleScanner.instance.stopBleScan()
    }

    fun notify(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidNotify: String,
        bleNotifyCallback: BleNotifyCallback?
    ) {
        notify(bleDevice, uuidService, uuidNotify, false, bleNotifyCallback)
    }

    fun notify(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidNotify: String,
        useCharacteristicDescriptor: Boolean,
        bleNotifyCallback: BleNotifyCallback?
    ) {
        val bleBluetooth = multiBluetoothManager?.getBleBluetooth(bleDevice)
        bleBluetooth?.let {
            it.createBleConnector().withUUIDString(uuidService, uuidNotify)
                .enableCharacteristicNotify(
                    bleNotifyCallback,
                    uuidNotify,
                    useCharacteristicDescriptor
                )
        } ?: let {
            bleNotifyCallback?.onNotifyFailure(OtherException("This device not connect!"))
        }
    }

    fun indicate(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidIndicate: String,
        bleIndicateCallback: BleIndicateCallback?
    ) {
        indicate(bleDevice, uuidService, uuidIndicate, false, bleIndicateCallback)
    }

    fun indicate(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidIndicate: String,
        useCharacteristicDescriptor: Boolean,
        bleIndicateCallback: BleIndicateCallback?
    ) {
        val bleBluetooth = multiBluetoothManager?.getBleBluetooth(bleDevice)
        bleBluetooth?.let {
            it.createBleConnector().withUUIDString(uuidService, uuidIndicate)
                .enableCharacteristicIndicate(
                    bleIndicateCallback,
                    uuidIndicate,
                    useCharacteristicDescriptor
                )
        } ?: let {
            bleIndicateCallback?.onIndicateFailure(OtherException("This device not connect!"))
        }
    }

    fun stopNotify(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidNotify: String,
    ): Boolean {
        return stopNotify(bleDevice, uuidService, uuidNotify, false)
    }

    fun stopNotify(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidNotify: String,
        useCharacteristicDescriptor: Boolean,
    ): Boolean {
        val bleBluetooth = multiBluetoothManager?.getBleBluetooth(bleDevice)
        bleBluetooth?.let {
            val disableSuccess = it.createBleConnector().withUUIDString(uuidService, uuidNotify)
                .disableCharacteristicNotify(useCharacteristicDescriptor)
            if (disableSuccess) {
                it.removeNotifyCallback(uuidNotify)
            }
            return disableSuccess
        }
        return false
    }

    fun disableIndicate(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidIndicate: String,
    ): Boolean {
        return disableIndicate(bleDevice, uuidService, uuidIndicate, false)
    }

    fun disableIndicate(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidIndicate: String,
        useCharacteristicDescriptor: Boolean
    ): Boolean {
        val bleBluetooth = multiBluetoothManager?.getBleBluetooth(bleDevice)
        bleBluetooth?.let {
            val disableSuccess = it.createBleConnector().withUUIDString(uuidService, uuidIndicate)
                .disableCharacteristicIndicate(useCharacteristicDescriptor)
            if (disableSuccess) {
                it.removeIndicateCallback(uuidIndicate)
            }
            return disableSuccess
        }
        return false
    }

    fun write(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidWrite: String,
        data: ByteArray?,
        bleWriteCallback: BleWriteCallback?
    ) {
        write(bleDevice, uuidService, uuidWrite, data, true, bleWriteCallback)
    }

    fun write(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidWrite: String,
        data: ByteArray?,
        split: Boolean,
        bleWriteCallback: BleWriteCallback?
    ) {
        write(bleDevice, uuidService, uuidWrite, data, split, true, 0, bleWriteCallback)
    }

    fun write(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidWrite: String,
        data: ByteArray?,
        split: Boolean,
        sendNextWhenLastSuccess: Boolean,
        intervalBetweenTwoPackage: Long,
        bleWriteCallback: BleWriteCallback?
    ) {
        bleWriteCallback?.let { callback ->
            if (data == null || data.isEmpty()) {
                callback.onWriteFailure(OtherException("data is null or empty!"))
                return
            }
            if (data.size > 20 && !split) {
                BleLog.w("Be careful: data's length beyond 20! Ensure MTU higher than 23, or use spilt write!")
            }
            val bleBluetooth = multiBluetoothManager?.getBleBluetooth(bleDevice)
            bleBluetooth?.let {
                if (split && data.size > getSplitMaxNum()) {
                    //split data to write
                    SplitWriter().splitWrite(
                        it,
                        uuidService,
                        uuidWrite,
                        data,
                        sendNextWhenLastSuccess,
                        intervalBetweenTwoPackage,
                        bleWriteCallback
                    )
                } else {
                    it.createBleConnector().withUUIDString(uuidService, uuidWrite)
                        .writeCharacteristic(data, bleWriteCallback, uuidWrite)
                }
            } ?: let {
                callback.onWriteFailure(OtherException("this device is not connect!"))
            }
        }
    }

    fun read(
        bleDevice: BleDevice?,
        uuidService: String,
        uuidRead: String,
        bleReadCallback: BleReadCallback?
    ) {
        bleReadCallback?.let { callback ->
            val bleBluetooth = multiBluetoothManager?.getBleBluetooth(bleDevice)
            bleBluetooth?.let {
                it.createBleConnector().withUUIDString(uuidService, uuidRead)
                    .readCharacteristic(bleReadCallback, uuidRead)
            } ?: let {
                callback.onReadFailure(OtherException("This device is not connected!"))
            }
        }
    }

    fun readRssi(
        bleDevice: BleDevice?,
        bleRssiCallback: BleRssiCallback?
    ) {
        bleRssiCallback?.let { callback ->
            val bleBluetooth = multiBluetoothManager?.getBleBluetooth(bleDevice)
            bleBluetooth?.let {
                it.createBleConnector().readRemoteRssi(bleRssiCallback)
            } ?: let {
                callback.onRssiFailure(OtherException("This device is not connected!"))
            }
        }
    }

    fun setMtu(
        bleDevice: BleDevice?,
        mtu: Int,
        bleMtuChangedCallback: BleMtuChangedCallback?
    ) {
        bleMtuChangedCallback?.let { callback ->
            if (mtu > BleConstants.MAX_MTU || mtu < BleConstants.MIN_MTU) {
                BleLog.e("requiredMtu should lower than 512 or more than 23!")
                callback.onSetMtuFailure(OtherException("requiredMtu should lower than 512 or more than 23!"))
            }
            val bleBluetooth = multiBluetoothManager?.getBleBluetooth(bleDevice)
            bleBluetooth?.let {
                it.createBleConnector().setMtu(mtu, bleMtuChangedCallback)
            } ?: let {
                callback.onSetMtuFailure(OtherException("This device is not connected!"))
            }
        }
    }

    fun requestConnectionPriority(bleDevice: BleDevice?, connectionPriority: Int): Boolean {
        val bleBluetooth = multiBluetoothManager?.getBleBluetooth(bleDevice)
        bleBluetooth?.let {
            return it.createBleConnector().requestConnectionPriority(connectionPriority)
        }
        return false
    }

    fun getSplitMaxNum(): Int {
        return mSplitWriteNum
    }

    fun isBlueEnable(): Boolean {
        return mBluetoothAdapter?.isEnabled ?: false
    }

    fun getAllConnectedDevice(): List<BleBluetooth>? {
        return multiBluetoothManager?.getBleBluetoothList()
    }

    fun getMultiBluetoothManager(): MultiBluetoothManager? {
        return multiBluetoothManager
    }

    fun getConnectState(bleDevice: BleDevice?): Int {
        bleDevice?.let {
            return mBluetoothManager?.getConnectionState(it.mDevice, BluetoothProfile.GATT)
                ?: BluetoothProfile.STATE_DISCONNECTED
        }
        return BluetoothProfile.STATE_DISCONNECTED
    }

    fun isConnected(bleDevice: BleDevice?): Boolean {
        return getConnectState(bleDevice) == BluetoothProfile.STATE_CONNECTED
    }

}