package com.merit.liteble.scan

/**
 * @Description 蓝牙扫描状态
 * @Author lk
 * @Date 2022/9/30 16:18
 */
enum class BleScanState(val state: Int) {

    SCAN_STATE_IDLE(-1),

    SCAN_STATE_SCANNING(1)

}