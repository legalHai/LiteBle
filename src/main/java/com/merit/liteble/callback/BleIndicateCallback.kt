package com.merit.liteble.callback

import com.merit.liteble.exception.BleException

/**
 * @Description
 * @Author lk
 * @Date 2022/10/9 14:47
 */
abstract class BleIndicateCallback: BleBaseCallback(){

    abstract fun onIndicateSuccess()

    abstract fun onIndicateFailure(bleException: BleException)

    abstract fun onCharacteristicChanged(data: ByteArray)

}