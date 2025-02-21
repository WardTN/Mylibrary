package com.dq.mylibrary

import com.dq.mylibrary.tcp.TcpLoopThread
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * 实现 TCP协议
 */
abstract class BaseTCPUtil(private val listener: TCPListener) {

    protected var mSocket: Socket? = null
    protected var mReader: InputStream? = null
    protected var mWriter: OutputStream? = null

    private var loopRecThread: TcpLoopThread? = null

    protected var VALID_LENGTH = 8
    protected var RECEIVE_MAX_LENGTH = 23

    /**
     * 连接TCP
     */
    fun connect() {
        Thread {
            try {
                mSocket = Socket(getServerAddress(), getServerPort())
                mSocket?.keepAlive = true
                mReader = mSocket?.getInputStream()
                mWriter = mSocket?.getOutputStream()

                if (mSocket?.isConnected == true) {
                    listener.tcpConnectSuc()
                }

                revMsg()
            } catch (e: IOException) {
                e.printStackTrace()
                listener.tcpConnectFail(e.message ?: "Unknown error")
            }
        }.start()
    }

    protected abstract fun getServerAddress(): String
    protected abstract fun getServerPort(): Int


    private fun revMsg() {
        loopRecThread = object : TcpLoopThread() {
            override fun run() {
                super.run()
                try {
                    val buffer = ByteArray(RECEIVE_MAX_LENGTH)
                    var readSize: Int
                    while (canLoop()) {
                        readSize = mReader?.read(buffer) ?: -1
                        if (readSize > VALID_LENGTH) {
                            val msg = ByteArray(readSize)
                            System.arraycopy(buffer, 0, msg, 0, readSize)
                            listener.tcpReceiverSuc(msg)
                        } else if (readSize == -1) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        mReader?.close()
                        mWriter?.close()
                        mSocket?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        loopRecThread?.start()
    }

    fun sendMsg(msg: ByteArray) {
        Thread {
            try {
                mWriter?.write(msg)
                mWriter?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    interface TCPListener {
        fun tcpReceiverSuc(msg: ByteArray)
        fun tcpConnectSuc()
        fun tcpConnectFail(msg: String)
    }

    @Throws(IOException::class)
    fun releaseTcp() {
        mSocket?.close()
        loopRecThread?.cancelLoop()
    }
}
