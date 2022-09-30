package com.merit.liteble

/**
 * @Description
 * @Author lk
 * @Date 2022/9/30 11:09
 */
class BleManager private constructor(){

    companion object{
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BleManager
        }
    }
}