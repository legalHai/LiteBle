package com.merit.liteble.exception

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Description
 * @Author lk
 * @Date 2022/10/9 14:26
 */
@Parcelize
open class BleException(var code: Int? = -1, var description: String? = "") : Parcelable {
    companion object{
        val ERROR_CODE_TIMEOUT: Int = 100
        val ERROR_CODE_GATT: Int = 101
        val ERROR_CODE_OTHER: Int = 102
    }
}