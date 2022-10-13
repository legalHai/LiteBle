package com.merit.liteble.bean

/**
 * @Description 蓝牙配置常量
 * @Author lk
 * @Date 2022/9/30 15:24
 */
object BleConstants {
    var SCAN_TIME_OUT = 30000L
    var CONN_TIME_OUT = 10000L
    var OPERATE_TIME_OUT = 10000L
    var RECONNECT_COUNT = 3
    var MAX_MULTIPLE_DEVICE = 7
    var WRITE_DATA_SPLIT_COUNT = 20
    var MAX_MTU = 512
    var MIN_MTU = 23
    var SCAN_NOT_CONNECT = 1 //扫描后不连接
    var SCAN_AND_CONNECT_UNTIL_FINISH = 2 //扫描结束后连接
    var SCAN_AND_CONNECT_QUICK = 3 //扫描到就进行连接
}