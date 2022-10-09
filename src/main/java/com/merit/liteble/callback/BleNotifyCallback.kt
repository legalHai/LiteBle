package com.merit.liteble.callback

import com.merit.liteble.exception.BleException

/**
 * @Description 数据监听
 * @Author lk
 * @Date 2022/10/9 14:45
 */
abstract class BleNotifyCallback: BleBaseCallback(){

    abstract fun onNotifySuccess()

    abstract fun onNotifyFailure(bleException: BleException)

    abstract fun onCharacteristicChanged(data: ByteArray)

}