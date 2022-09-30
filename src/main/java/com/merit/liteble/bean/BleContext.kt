package com.merit.liteble.bean

import android.annotation.SuppressLint
import android.content.Context

/**
 * @Description
 * @Author lk
 * @Date 2022/9/30 15:10
 */
class BleContext {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mContext: Context? = null

        @JvmStatic
        fun setContext(context: Context) {
            mContext = context
        }

        @JvmStatic
        fun getContext(): Context? {
            return mContext
        }
    }
}