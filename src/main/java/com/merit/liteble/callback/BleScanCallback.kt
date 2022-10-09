package com.merit.liteble.callback

import com.merit.liteble.bean.BleDevice

/**
 * @Description 扫描回调
 * @Author lk
 * @Date 2022/10/8 10:09
 */
abstract class BleScanCallback : BleScanManagerImp{

    abstract fun onScanFinish(bleScanDeviceList: List<BleDevice>)

    open fun onBleScan(bleDevice: BleDevice){}
}