package com.merit.liteble.utils

import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import com.merit.liteble.bean.BleContext

/**
 * @Description 工具类
 * @Author lk
 * @Date 2022/9/30 11:30
 */
object BleUtils {

    var bluetoothManager: BluetoothManager? = null

    /**
     * is support ble?
     *
     * @return
     */
    fun isSupportBle(): Boolean {
        return BleContext.getContext()?.let {
            (it.applicationContext.packageManager
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        }?:let {
            false
        }
    }
}