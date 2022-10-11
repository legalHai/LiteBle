package com.merit.liteble.scan

import com.merit.liteble.BleManager
import com.merit.liteble.bean.BleDevice
import com.merit.liteble.bean.BleScanBean
import com.merit.liteble.callback.BleScanAndConnectCallback
import com.merit.liteble.callback.BleScanCallback
import com.merit.liteble.callback.BleScanManagerImp
import com.merit.liteble.utils.BleLog
import kotlinx.coroutines.*

/**
 * @Description 蓝牙扫描类
 * @Author lk
 * @Date 2022/9/30 16:14
 */
class BleScanner private constructor() {

    private var mScannerState = BleScanState.SCAN_STATE_IDLE
    private var job: Job? = null
    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BleScanner()
        }
    }

    private val mBleScanManager = object : BleScanManager() {
        override fun onLeScan(bleDevice: BleDevice) {
            if (isNeedConnect()) {
                if(getBleScanImp() is BleScanAndConnectCallback){
                    (getBleScanImp() as BleScanAndConnectCallback).onBleScan(bleDevice)
                }else{
                    (getBleScanImp() as BleScanCallback).onBleScan(bleDevice)
                }
            } else {
                (getBleScanImp() as BleScanCallback).onBleScan(bleDevice)
            }
        }

        override fun onScanStarted(success: Boolean) {
            getBleScanImp()?.onScanStart(success)
        }

        override fun onScanning(bleDevice: BleDevice) {
            getBleScanImp()?.onScanning(bleDevice)
        }

        override fun onScanFinished(bleDeviceList: List<BleDevice>) {
            BleLog.d( "onScanFinished: ")
            if (isNeedConnect()) {
                if (getBleScanImp() is BleScanAndConnectCallback) {
                    val callback = (getBleScanImp() as BleScanAndConnectCallback)
                    if (bleDeviceList.isEmpty()) {
                        callback.onScanFinished(BleDevice())
                    } else {
                        job?.cancel()
                        callback.onScanFinished(bleDeviceList[0])
                        //connect device by delay 100ms
                        connectDelay(bleDeviceList[0], callback)
                    }
                } else {
                    (getBleScanImp() as BleScanCallback).onScanFinish(bleDeviceList)
                }
            } else {
                (getBleScanImp() as BleScanCallback).onScanFinish(bleDeviceList)
            }
        }
    }

    private fun connectDelay(bleDevice: BleDevice, bleScanAndConnectCallback: BleScanAndConnectCallback){
        job = GlobalScope.launch(Dispatchers.IO) {
            delay(200)
            BleManager.instance.connect(bleDevice, bleScanAndConnectCallback)
        }
    }

    @Synchronized
    fun startBleScan(mScanBean: BleScanBean, bleScanManagerImp: BleScanManagerImp) {
        if (mScannerState != BleScanState.SCAN_STATE_IDLE) {
            mBleScanManager.getBleScanImp()?.onScanStart(false)
            return
        }

        mBleScanManager.prepare(mScanBean, bleScanManagerImp)
        var bleScan = BleManager.instance.getBluetoothScanAdapter()
        var success = bleScan?.startLeScan(mScanBean.mServiceUUIDs, mBleScanManager)
        mScannerState =
            if (success == true) BleScanState.SCAN_STATE_SCANNING else BleScanState.SCAN_STATE_IDLE
        mBleScanManager.notifyScanStart(success == true)
    }

    @Synchronized
    fun stopBleScan() {
        BleManager.instance.getBluetoothScanAdapter()?.stopLeScan(mBleScanManager)
        mScannerState = BleScanState.SCAN_STATE_IDLE
        mBleScanManager.notifyScanStop()
    }
}