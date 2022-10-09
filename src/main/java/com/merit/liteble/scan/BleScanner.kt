package com.merit.liteble.scan

import android.util.Log
import com.merit.liteble.BleManager
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.bean.BleScanBean
import com.merit.liteble.callback.BleScanCallback
import com.merit.liteble.callback.BleScanManagerImp

/**
 * @Description 蓝牙扫描类
 * @Author lk
 * @Date 2022/9/30 16:14
 */
class BleScanner private constructor(){

    private var mScannerState = BleScanState.SCAN_STATE_IDLE

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BleScanner()
        }
    }

    private val mBleScanManager = object : BleScanManager() {
        override fun onLeScan(bleDevice: BleDevice) {
            Log.d("BleScanner", "onLeScan: ")
            (getBleScanImp() as BleScanCallback).onBleScan(bleDevice)
        }

        override fun onScanStarted(success: Boolean) {
//            Log.d("BleScanner", "onScanStarted: ")
            getBleScanImp()?.onScanStart(success)
        }

        override fun onScanning(bleDevice: BleDevice) {
//            Log.d("BleScanner", "onScanning: ")
            getBleScanImp()?.onScanning(bleDevice)
        }

        override fun onScanFinished(bleDeviceList: List<BleDevice>) {
            Log.d("BleScanner", "onScanFinished: ")
            (getBleScanImp() as BleScanCallback).onScanFinish(bleDeviceList)
        }
    }

    @Synchronized
    fun startBleScan(mScanBean: BleScanBean, bleScanManagerImp: BleScanManagerImp){
        if(mScannerState != BleScanState.SCAN_STATE_IDLE){
            mBleScanManager.getBleScanImp()?.onScanStart(false)
            return
        }

        mBleScanManager.prepare(mScanBean,bleScanManagerImp)
        var bleScan = BleManager.instance.getBluetoothScanAdapter()
        var success = bleScan?.startLeScan(mScanBean.mServiceUUIDs,mBleScanManager)
        mScannerState = if(success == true) BleScanState.SCAN_STATE_SCANNING else BleScanState.SCAN_STATE_IDLE
        mBleScanManager.notifyScanStart(success == true)
    }

    @Synchronized
    fun stopBleScan(){
        BleManager.instance.getBluetoothScanAdapter()?.stopLeScan(mBleScanManager)
        mScannerState = BleScanState.SCAN_STATE_IDLE
        mBleScanManager.notifyScanStop()
    }
}