package com.merit.liteble.scan

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.bean.BleScanBean
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * @Description 蓝牙扫描具体实现
 * @Author lk
 * @Date 2022/9/30 16:43
 */
abstract class BleScanManager : BluetoothAdapter.LeScanCallback {

    private var mScanBean:BleScanBean? = null
    private var scanJob:Job? = null
    private var scanDeviceList = ConcurrentLinkedDeque<BleDevice>()

    override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
        var bleDevice = BleDevice()
        bleDevice.mDevice = device
        bleDevice.mScanRecord = scanRecord
        bleDevice.mRssi = rssi
        bleDevice.mTimestampNanos = System.currentTimeMillis()
        scanDeviceList.offer(bleDevice)
    }

    @DelicateCoroutinesApi
    fun startScanJob(){
        scanJob = GlobalScope.launch(Dispatchers.IO) {
            while (true){
                handleScanResult()
            }
        }
    }

    fun prepare(scanBean: BleScanBean) {
        mScanBean = scanBean
        startScanJob()
    }

    /**
     * 停止扫描
     */
    fun notifyScanStop(){
        scanJob?.cancel()
    }

    /**
     * 处理扫描结果
     */
    private fun handleScanResult(){
        var bleDevice = scanDeviceList.poll()
        checkDevice(bleDevice)
    }

    private fun checkDevice(bleDevice: BleDevice){


    }

    abstract fun onScanStarted(success: Boolean)

    abstract fun onLeScan(bleDevice: BleDevice)

    abstract fun onScanning(bleDevice: BleDevice)

    abstract fun onScanFinished(bleDeviceList: List<BleDevice>)
}