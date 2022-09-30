package com.merit.liteble.scan

import com.merit.liteble.bean.BleConstants

/**
 * @Description 蓝牙扫描配置
 * @Author lk
 * @Date 2022/9/30 11:28
 */
open class BleScanOption private constructor(){

    private var mDeviceNames: Array<String>? = null
    private var mDeviceMac: String? = null
    private var mScanTimeOut: Long? = BleConstants.SCAN_TIME_OUT
    private var mAutoConnect = false
    private var mFuzzy = false

    class Builder {
        private var mDeviceNames: Array<String>? = null
        private var mDeviceMac: String? = null
        private var mScanTimeOut: Long? = BleConstants.SCAN_TIME_OUT
        private var mAutoConnect = false
        private var mFuzzy = false

        fun setDeviceNames(deviceNames: Array<String>?): Builder {
            this.mDeviceNames = deviceNames
            return this
        }

        fun setDeviceMac(deviceMac: String?): Builder {
            this.mDeviceMac = deviceMac
            return this
        }

        fun setScanTimeOut(scanTimeOut: Long?): Builder {
            this.mScanTimeOut = scanTimeOut
            return this
        }

        fun setAutoConnect(autoConnect: Boolean): Builder {
            this.mAutoConnect = autoConnect
            return this
        }

        fun build(): BleScanOption {
            var option = BleScanOption()
            option.mDeviceNames = this.mDeviceNames
            option.mDeviceMac = this.mDeviceMac
            option.mScanTimeOut = this.mScanTimeOut
            option.mAutoConnect = this.mAutoConnect
            option.mFuzzy = this.mFuzzy
            return option
        }
    }
}