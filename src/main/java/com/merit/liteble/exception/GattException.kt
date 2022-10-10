package com.merit.liteble.exception

/**
 * @Description
 * @Author lk
 * @Date 2022/10/10 11:11
 */
class GattException(status: Int): BleException(ERROR_CODE_GATT, "Gatt Exception Occurred! status $status"){
}