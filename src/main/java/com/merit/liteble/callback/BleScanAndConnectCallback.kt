package com.merit.liteble.callback

import com.merit.liteble.bean.BleDevice

/**
 * @Description
 * @Author lk
 * @Date 2022/10/11 16:42
 */
abstract class BleScanAndConnectCallback: BleGattCallback(), BleScanManagerImp{
    abstract fun onScanFinished(bleDevice: BleDevice)

    open fun onBleScan(bleDevice: BleDevice){}
}