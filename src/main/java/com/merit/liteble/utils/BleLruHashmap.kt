package com.merit.liteble.utils

import com.merit.liteble.bluetooth.BleBluetooth
import kotlin.math.ceil

/**
 * @Description
 * @Author lk
 * @Date 2022/10/10 13:41
 */
class BleLruHashmap<K,V>(saveSize: Int) :
    LinkedHashMap<K, V>((ceil(saveSize / 0.75) + 1).toInt(), 0.75f, true) {

    private val MAX_SIZE: Int = saveSize

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        if(size > MAX_SIZE && eldest?.value is BleBluetooth){
            (eldest.value as BleBluetooth).disconnect()
        }
        return size > MAX_SIZE
    }

    override fun toString(): String {
        var sb = StringBuilder()
        entries.forEach {
            sb.append("key: ${it.key} value: ${it.value}")
        }
        return sb.toString()
    }
}