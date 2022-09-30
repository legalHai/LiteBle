package com.merit.liteble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.merit.liteble.bean.BleConstants
import com.merit.liteble.bean.BleContext
import com.merit.liteble.scan.BleScanOption
import com.merit.liteble.utils.BleUtils

/**
 * @Description
 * @Author lk
 * @Date 2022/9/30 11:09
 */
class BleManager private constructor() {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanOption:BleScanOption? = null
    private var mOperateTimeout:Long? = BleConstants.OPERATE_TIME_OUT
    private var mConnectTimeout:Long? = BleConstants.CONN_TIME_OUT
    private var mReConnectCount:Int? = BleConstants.RECONNECT_COUNT
    private var enableLog:Boolean = false

    companion object {
        @JvmStatic
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BleManager
        }
    }

    fun init(context: Context) {
        BleContext.setContext(context)
        if(BleUtils.isSupportBle()){
            mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        }
        mBluetoothAdapter = mBluetoothManager?.adapter
        mScanOption = BleScanOption.Builder().build()
    }

    /**
     * 初始化扫描配置
     */
    fun initScanOption(scanOption:BleScanOption?){
        this.mScanOption = scanOption
    }

    /**
     * 是否开启log
     */
    fun enableLog(enableLog:Boolean):BleManager{
        this.enableLog = enableLog
        return this
    }

    /**
     * 设置连接超时时间
     */
    fun setConnectTimeOut(connectTimeOut: Long): BleManager{
        this.mConnectTimeout = connectTimeOut
        return this
    }

    /**
     * 设置操作超时时间
     */
    fun setOperateTimeOut(operateTimeOut: Long): BleManager{
        this.mOperateTimeout = operateTimeOut
        return this
    }

    fun connect(){

    }

}