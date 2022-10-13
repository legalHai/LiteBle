package com.merit.liteble.scan

import com.merit.liteble.bean.BleScanBean
import java.util.*

/**
 * @Description 蓝牙扫描配置
 * @Author lk
 * @Date 2022/9/30 11:28
 */
open class BleScanOption private constructor() {

    lateinit var bleScanBean: BleScanBean

    class Builder {
        private var bleScanBean: BleScanBean = BleScanBean()

        fun setBleScanBean(bleScanBean: BleScanBean): Builder {
            this.bleScanBean = bleScanBean
            return this
        }

        fun setServiceUUIDs(serviceUUIDs: Array<UUID>?): Builder{
            this.bleScanBean.mServiceUUIDs = serviceUUIDs
            return this
        }

        fun setDeviceNames(deviceNames: Array<String>?): Builder {
            this.bleScanBean.mDeviceNames = deviceNames
            return this
        }

        fun setDeviceMac(deviceMac: String?): Builder {
            this.bleScanBean.mDeviceMac = deviceMac
            return this
        }

        fun setScanTimeOut(scanTimeOut: Long?): Builder {
            this.bleScanBean.mScanTimeOut = scanTimeOut
            return this
        }

        fun setAutoConnect(autoConnect: Boolean): Builder {
            this.bleScanBean.mAutoConnect = autoConnect
            return this
        }

        fun setNeedConnect(needConnect: Int): Builder{
            this.bleScanBean.mNeedConnect = needConnect
            return this
        }
        fun setFuzzy(fuzzy: Boolean): Builder{
            this.bleScanBean.mFuzzy = fuzzy
            return this
        }

        fun build(): BleScanOption {
            var option = BleScanOption()
            option.bleScanBean = BleScanBean()
            option.bleScanBean.mDeviceNames = this.bleScanBean.mDeviceNames
            option.bleScanBean.mDeviceMac = this.bleScanBean.mDeviceMac
            option.bleScanBean.mScanTimeOut = this.bleScanBean.mScanTimeOut
            option.bleScanBean.mAutoConnect = this.bleScanBean.mAutoConnect
            option.bleScanBean.mNeedConnect = this.bleScanBean.mNeedConnect
            option.bleScanBean.mFuzzy = this.bleScanBean.mFuzzy
            return option
        }
    }
}