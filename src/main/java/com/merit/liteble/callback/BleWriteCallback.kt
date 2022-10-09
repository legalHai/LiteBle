package com.merit.liteble.callback

import com.merit.liteble.exception.BleException

/**
 * @Description
 * @Author lk
 * @Date 2022/10/9 14:48
 */
abstract class BleWriteCallback: BleBaseCallback(){

    abstract fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray)

    abstract fun onWriteFailure(bleException: BleException)

}