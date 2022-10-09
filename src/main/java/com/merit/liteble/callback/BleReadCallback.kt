package com.merit.liteble.callback

import com.merit.liteble.exception.BleException

/**
 * @Description
 * @Author lk
 * @Date 2022/10/9 14:50
 */
abstract class BleReadCallback: BleBaseCallback(){

    abstract fun onReadSuccess(data: ByteArray)

    abstract fun onReadFailure(bleException: BleException)

}