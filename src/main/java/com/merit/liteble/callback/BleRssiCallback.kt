package com.merit.liteble.callback

import com.merit.liteble.exception.BleException

/**
 * @Description 信号回调
 * @Author lk
 * @Date 2022/10/9 14:31
 */
abstract class BleRssiCallback: BleBaseCallback(){

    abstract fun onRssiFailure(bleException: BleException)

    abstract fun onRssiSuccess(code: Int)
}