package com.merit.liteble.scan

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.merit.liteble.bean.BleConstants
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.bean.BleScanBean
import com.merit.liteble.callback.BleScanManagerImp
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * @Description 蓝牙扫描具体实现
 * @Author lk
 * @Date 2022/9/30 16:43
 */
abstract class BleScanManager : BluetoothAdapter.LeScanCallback {

    private var mScanBean: BleScanBean? = null
    private var scanJob: Job? = null
    private var scanDeviceList = ConcurrentLinkedDeque<BleDevice>()
    private var mBleScanManagerImp: BleScanManagerImp? = null

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
        bleDevice?.let {
            onLeScan(bleDevice)
            onScanning(bleDevice)
        }
    }

    /**
     * 开始
     */
    fun notifyScanStart(isSuccess: Boolean) {
        scanDeviceList.clear()
        if (isSuccess && (mScanBean?.mScanTimeOut ?: BleConstants.SCAN_TIME_OUT) > 0) {
            GlobalScope.launch(Dispatchers.IO) {
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
        onScanFinished(scanDeviceList.toList())
    }

    fun isNeedConnect(): Boolean {
        return mScanBean?.mNeedConnect ?: false
    }
    abstract fun onScanStarted(success: Boolean)

    abstract fun onLeScan(bleDevice: BleDevice)

    abstract fun onScanning(bleDevice: BleDevice)

    abstract fun onScanFinished(bleDeviceList: List<BleDevice>)
}