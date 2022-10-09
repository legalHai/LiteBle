package com.merit.liteble.utils

import android.util.Log

/**
 * @Description
 * @Author lk
 * @Date 2022/10/9 15:41
 */
object BleLog {

    var needPrint = false
    var TAG = "LiteBle"

    fun i(msg: String?){
        if(!needPrint){
            return
        }
        msg?.let {
            Log.i(TAG, it)
        }
    }

    fun w(msg: String?){
        if(!needPrint){
            return
        }
        msg?.let {
            Log.w(TAG, it)
        }
    }

    fun d(msg: String?){
        if(!needPrint){
            return
        }
        msg?.let {
            Log.d(TAG, it)
        }
    }

    fun e(msg: String?){
        if(!needPrint){
            return
        }
        msg?.let {
            Log.e(TAG, it)
        }
    }


}