package com.merit.liteble.scan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.text.TextUtils
import android.util.Log
import com.merit.liteble.bean.BleConstants
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.bean.BleScanBean
import com.merit.liteble.callback.BleScanManagerImp
import com.merit.liteble.utils.BleLog
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @Description 蓝牙扫描具体实现
 * @Author lk
 * @Date 2022/9/30 16:43
 */
abstract class BleScanManager : BluetoothAdapter.LeScanCallback {

    private var mScanBean: BleScanBean? = null
    private var scanJob: Job? = null
    private var stopJob: Job? = null
    private var scanDeviceList = ConcurrentLinkedDeque<BleDevice>()
    private var scanDeviceMap = mutableMapOf<String,BleDevice>()
    private var mBleScanManagerImp: BleScanManagerImp? = null
    private var isSearching: AtomicBoolean = AtomicBoolean()

    @SuppressLint("MissingPermission")
    override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
        var bleDevice = BleDevice()
        bleDevice.mDevice = device
        bleDevice.mScanRecord = scanRecord
        bleDevice.mRssi = rssi
        bleDevice.mTimestampNanos = System.currentTimeMillis()
        scanDeviceList.offer(bleDevice)
    }

    @DelicateCoroutinesApi
    fun startScanJob() {
        scanJob?.cancel()
        scanJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                handleScanResult()
            }
        }
    }

    /**
     * 准备开始扫描
     */
    fun prepare(scanBean: BleScanBean, bleScanManagerImp: BleScanManagerImp) {
        mScanBean = scanBean
        mBleScanManagerImp = bleScanManagerImp
        startScanJob()
    }

    fun getBleScanImp(): BleScanManagerImp? {
        return mBleScanManagerImp
    }

    /**
     * 处理扫描结果
     */
    private fun handleScanResult() {
        var bleDevice = scanDeviceList.poll()
        bleDevice?.let { device ->
            mScanBean?.let { scanBean ->
                if (TextUtils.isEmpty(scanBean.mDeviceMac) && scanBean.mDeviceNames.isNullOrEmpty()) {
                    scanResult(device)
                    return
                }
                scanBean.mDeviceMac?.let {
                    if (TextUtils.equals(it, device.getMac())) {
                        scanResult(device)
                        return
                    }
                }
                scanBean.mDeviceNames?.forEach {
                    if (if (scanBean.mFuzzy == true) device.getName().contains(it) else it.equals(
                            device.getName(),
                            true
                        )
                    ) {
                        scanResult(device)
                        return
                    }
                }
            }
        }
    }

    private fun scanResult(bleDevice: BleDevice) {
        bleDevice?.let {
            scanDeviceMap[it.getMac()] = bleDevice
            onLeScan(bleDevice)
            onScanning(bleDevice)
        }
    }

    /**
     * 开始
     */
    fun notifyScanStart(isSuccess: Boolean) {
        scanDeviceList.clear()
        scanDeviceMap.clear()
        if (isSuccess && (mScanBean?.mScanTimeOut ?: BleConstants.SCAN_TIME_OUT) > 0) {
            stopJob = GlobalScope.launch(Dispatchers.IO) {
                delay(mScanBean?.mScanTimeOut ?: BleConstants.SCAN_TIME_OUT)
                BleScanner.instance.stopBleScan()
            }
        }
        onScanStarted(isSuccess)
    }

    /**
     * 停止扫描
     */
    fun notifyScanStop() {
        scanJob?.cancel()
        stopJob?.cancel()
        onScanFinished(scanDeviceMap.values.toList())
    }

    fun isNeedConnect(): Int {
        return mScanBean?.mNeedConnect ?: BleConstants.SCAN_NOT_CONNECT
    }

    abstract fun onScanStarted(success: Boolean)

    abstract fun onLeScan(bleDevice: BleDevice)

    abstract fun onScanning(bleDevice: BleDevice)

    abstract fun onScanFinished(bleDeviceList: List<BleDevice>)
}