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
import com.merit.liteble.callback.BleGattCallback
import com.merit.liteble.callback.BleNotifyCallback
import com.merit.liteble.callback.BleScanCallback
import com.merit.liteble.exception.GattException
import com.merit.liteble.exception.OtherException
import com.merit.liteble.scan.BleScanOption
import com.merit.liteble.scan.BleScanner
import com.merit.liteble.utils.BleLog
import com.merit.liteble.utils.BleUtils

/**
 * @Description
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
    private var enableLog: Boolean = false

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
        this.enableLog = enableLog
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
    fun getOperateTimeout(): Long{
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

    fun scanAndConnect() {

    }

    fun connect(bleDevice: BleDevice, bleGattCallback: BleGattCallback?): BluetoothGatt? {
        if (!isBlueEnable()) {
            BleLog.e("Bluetooth not enable!")
            bleGattCallback?.onConnectFail(bleDevice, OtherException("Bluetooth not enable!"))
            return null
        }
        var bleBluetooth  = multiBluetoothManager?.buildConnectingBle(bleDevice)
        var isAutoConn = mScanOption?.bleScanBean?.mAutoConnect?:false
        return bleBluetooth?.connect(bleDevice,isAutoConn,bleGattCallback)
    }

    fun connect(mac: String, bleGattCallback: BleGattCallback?): BluetoothGatt? {
        var bluetoothDevice = mBluetoothAdapter?.getRemoteDevice(mac)
        var bleDevice = BleDevice(bluetoothDevice,null,0,0)
        return connect(bleDevice,bleGattCallback)
    }

    fun cancelScan() {
        BleScanner.instance.stopBleScan()
    }

    fun notify(bleDevice: BleDevice?, uuidService: String, uuidNotify: String, bleNotifyCallback: BleNotifyCallback?) {
        notify(bleDevice,uuidService,uuidNotify,false,bleNotifyCallback)
    }

    fun notify(bleDevice: BleDevice?, uuidService: String, uuidNotify: String, useCharacteristicDescriptor: Boolean, bleNotifyCallback: BleNotifyCallback?) {

    }

    fun isBlueEnable(): Boolean {
        return mBluetoothAdapter?.isEnabled ?: false
    }

    fun getAllConnectedDevice(): List<BleBluetooth>?{
        return multiBluetoothManager?.getBleBluetoothList()
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