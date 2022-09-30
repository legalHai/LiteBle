package com.merit.liteble.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Description 蓝牙扫描配置实体类
 * @Author lk
 * @Date 2022/9/30 16:52
 */
@Parcelize
data class BleScanBean(
    var mDeviceNames: Array<String>? = null,
    var mDeviceMac: String? = null,
    var mScanTimeOut: Long? = 10000L,
    var mAutoConnect: Boolean? = false,
    var mFuzzy: Boolean? = false
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleScanBean

        if (mDeviceNames != null) {
            if (other.mDeviceNames == null) return false
            if (!mDeviceNames.contentEquals(other.mDeviceNames)) return false
        } else if (other.mDeviceNames != null) return false
        if (mDeviceMac != other.mDeviceMac) return false
        if (mScanTimeOut != other.mScanTimeOut) return false
        if (mAutoConnect != other.mAutoConnect) return false
        if (mFuzzy != other.mFuzzy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mDeviceNames?.contentHashCode() ?: 0
        result = 31 * result + (mDeviceMac?.hashCode() ?: 0)
        result = 31 * result + (mScanTimeOut?.hashCode() ?: 0)
        result = 31 * result + (mAutoConnect?.hashCode() ?: 0)
        result = 31 * result + (mFuzzy?.hashCode() ?: 0)
        return result
    }
}