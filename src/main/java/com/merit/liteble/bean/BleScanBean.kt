package com.merit.liteble.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * @Description 蓝牙扫描配置实体类
 * @Author lk
 * @Date 2022/9/30 16:52
 */
@Parcelize
data class BleScanBean(
    var mServiceUUIDs: Array<UUID>? = null,
    var mDeviceNames: Array<String>? = null,
    var mDeviceMac: String? = null,
    var mScanTimeOut: Long? = 10000L,
    var mAutoConnect: Boolean? = false,
    var mNeedConnect: Boolean? = false,
    var mFuzzy: Boolean? = false
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleScanBean

        if (mServiceUUIDs != null) {
            if (other.mServiceUUIDs == null) return false
            if (!mServiceUUIDs.contentEquals(other.mServiceUUIDs)) return false
        } else if (other.mServiceUUIDs != null) return false
        if (mDeviceNames != null) {
            if (other.mDeviceNames == null) return false
            if (!mDeviceNames.contentEquals(other.mDeviceNames)) return false
        } else if (other.mDeviceNames != null) return false
        if (mDeviceMac != other.mDeviceMac) return false
        if (mScanTimeOut != other.mScanTimeOut) return false
        if (mAutoConnect != other.mAutoConnect) return false
        if (mNeedConnect != other.mNeedConnect) return false
        if (mFuzzy != other.mFuzzy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mServiceUUIDs?.contentHashCode() ?: 0
        result = 31 * result + (mDeviceNames?.contentHashCode() ?: 0)
        result = 31 * result + (mDeviceMac?.hashCode() ?: 0)
        result = 31 * result + (mScanTimeOut?.hashCode() ?: 0)
        result = 31 * result + (mAutoConnect?.hashCode() ?: 0)
        result = 31 * result + (mNeedConnect?.hashCode() ?: 0)
        result = 31 * result + (mFuzzy?.hashCode() ?: 0)
        return result
    }

}