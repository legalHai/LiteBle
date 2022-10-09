package com.merit.liteble.callback

import com.merit.liteble.exception.BleException

/**
 * @Description 数据包回调
 * @Author lk
 * @Date 2022/10/9 14:41
 */
abstract class BleMtuChangedCallback: BleBaseCallback(){

    abstract fun onSetMtuFailure(bleException: BleException)

    abstract fun onMtuChanged(mtu: Int)
}