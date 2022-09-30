package com.merit.liteble.callback

import com.merit.liteble.bean.BleDevice

/**
 * @Description 扫描设备接口
 * @Author lk
 * @Date 2022/9/30 16:59
 */
interface BleScanManagerImp {

    fun onScanStart(isSuccess: Boolean)

    fun onScanning(bleDevice: BleDevice)

}