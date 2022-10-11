package com.merit.liteble.bluetooth

import com.merit.liteble.BleManager
import com.merit.liteble.callback.BleWriteCallback
import com.merit.liteble.exception.BleException
import com.merit.liteble.utils.BleLog
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.roundToInt

/**
 * @Description
 * @Author lk
 * @Date 2022/10/11 15:01
 */
class SplitWriter {

    var bleBluetooth: BleBluetooth? = null
    var uuidService: String = ""
    var uuidWrite: String = ""
    var data: ByteArray? = null
    var mSendNextWhenLastSuccess: Boolean = true
    var mIntervalBetweenTwoPackage: Long = 0
    var bleWriteCallback: BleWriteCallback? = null
    var mCount = BleManager.instance.getSplitMaxNum()
    var mDataQueue: Queue<ByteArray>? = null
    var mTotalNum: Int = 0
    var job: Job? = null


    fun splitWrite(
        bleBluetooth: BleBluetooth?,
        uuidService: String,
        uuidWrite: String,
        data: ByteArray?,
        sendNextWhenLastSuccess: Boolean,
        intervalBetweenTwoPackage: Long,
        bleWriteCallback: BleWriteCallback?
    ) {
        this.bleBluetooth = bleBluetooth
        this.uuidService = uuidService
        this.uuidWrite = uuidWrite
        this.data = data
        this.mSendNextWhenLastSuccess = sendNextWhenLastSuccess
        this.mIntervalBetweenTwoPackage = intervalBetweenTwoPackage
        this.bleWriteCallback = bleWriteCallback

    }

    private fun write(){
        mDataQueue?.let {
            if(it.peek() == null){
                job?.cancel()
                return
            }
            val data = it.poll()
            bleBluetooth?.let { bleBluetooth ->
                bleBluetooth.createBleConnector().withUUIDString(uuidService, uuidWrite)
                    .writeCharacteristic(data, object : BleWriteCallback() {
                        override fun onWriteSuccess(
                            current: Int,
                            total: Int,
                            justWrite: ByteArray
                        ) {
                            val position = mTotalNum - it.size
                            bleWriteCallback?.onWriteSuccess(position,mTotalNum, justWrite)
                            if (mSendNextWhenLastSuccess) {
                                writeDelay(mIntervalBetweenTwoPackage)
                            }

                        }

                        override fun onWriteFailure(bleException: BleException) {
                            bleWriteCallback?.onWriteFailure(bleException)
                            if (mSendNextWhenLastSuccess) {
                                writeDelay(mIntervalBetweenTwoPackage)
                            }
                        }
                    }, uuidWrite)
                if (mSendNextWhenLastSuccess) {
                    writeDelay(mIntervalBetweenTwoPackage)
                }
            }
        }
    }
    private fun writeDelay(interval: Long){
        job = GlobalScope.launch(Dispatchers.IO) {
            delay(interval)
            write()
        }
    }

    fun splitWrite() {
        if (data == null || data!!.isEmpty()) {
            BleLog.e("data is null or empty")
            return
        }
        if (mCount < 1) {
            BleLog.e("split count should higher than 0!")
            return
        }
        mDataQueue = splitByte(data!!,mCount)
        mTotalNum = mDataQueue?.size ?: 0
        write()
    }

    private fun splitByte(data: ByteArray, count: Int): Queue<ByteArray> {
        if (count > 20) {
            BleLog.w("Be careful: split count beyond 20! Ensure MTU higher than 23!")
        }

        val byteQueue: Queue<ByteArray> = LinkedList()

        var pkgCount = if (data.size % count == 0) {
            data.size / count
        } else {
            (data.size / count + 1f).roundToInt()
        }
        if (pkgCount > 0) {
            for (index in 0 until pkgCount) {
                var dataPkg: ByteArray
                var j: Int
                if (index == 1 || index == pkgCount - 1) {
                    j = if (data.size % count == 0) count else data.size % count
                    System.arraycopy(data, index * count, ByteArray(j).also { dataPkg = it }, 0, j)
                } else {
                    System.arraycopy(data, index * count, ByteArray(count).also { dataPkg = it }, 0, count)
                }
                byteQueue.offer(dataPkg)
            }
        }
        return byteQueue
    }

}