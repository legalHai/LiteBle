package com.merit.liteble.bean

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Description 蓝牙设备详情
 * @Author lk
 * @Date 2022/9/30 16:27
 */
@Parcelize
data class BleDevice(
    var mDevice: BluetoothDevice? = null,
    var mScanRecord: ByteArray? = null,
    var mRssi: Int? = 0,
    var mTimestampNanos: Long? = 0,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleDevice

        if (mDevice != other.mDevice) return false
        if (mScanRecord != null) {
            if (other.mScanRecord == null) return false
            if (!mScanRecord.contentEquals(other.mScanRecord)) return false
        } else if (other.mScanRecord != null) return false
        if (mRssi != other.mRssi) return false
        if (mTimestampNanos != other.mTimestampNanos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mDevice?.hashCode() ?: 0
        result = 31 * result + (mScanRecord?.contentHashCode() ?: 0)
        result = 31 * result + (mRssi ?: 0)
        result = 31 * result + (mTimestampNanos?.hashCode() ?: 0)
        return result
    }
}

