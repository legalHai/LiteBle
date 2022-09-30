package com.merit.liteble.scan

/**
 * @Description 蓝牙扫描类
 * @Author lk
 * @Date 2022/9/30 16:14
 */
class BleScanner private constructor(){

    private var scannerState = BleScanState.SCAN_STATE_IDLE

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BleScanner()
        }
    }


}