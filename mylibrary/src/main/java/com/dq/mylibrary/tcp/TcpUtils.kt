package com.dq.mylibrary.tcp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class TcpUtils {
    private var mSocket: Socket? = null
    private var mReader: InputStream? = null
    private var mWriter: OutputStream? = null

    private var job: Job? = null
    private val eventFlow = MutableSharedFlow<TCPEvent>(replay = 0, extraBufferCapacity = Channel.UNLIMITED)

    /**
     * 连接TCP
     */
    fun connect() {
        job = GlobalScope.launch(Dispatchers.IO) {
            try {
                mSocket = Socket("192.168.1.1", 5007)
                mSocket?.setKeepAlive(true)
                mReader = mSocket?.getInputStream()
                mWriter = mSocket?.getOutputStream()

                if (mSocket?.isConnected == true) {
                    eventFlow.emit(TCPEvent.ConnectSuccess)
                    receiveMessages()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                eventFlow.emit(TCPEvent.ConnectFailure(e.message ?: "Unknown error"))
            }
        }
    }

    private suspend fun receiveMessages() {
        mReader?.let { reader ->
            val buffer = ByteArray(23)
            flow {
                while (isActive) {
                    val readSize = reader.read(buffer)
                    if (readSize > 8) {
                        val msg = buffer.copyOfRange(0, readSize)
                        emit(msg)
                    } else if (readSize == -1) {
                        break
                    }
                }
            }.catch { e ->
                e.printStackTrace()
                eventFlow.emit(TCPEvent.ReceiveFailure(e.message ?: "Unknown error"))
            }.collect { msg ->
                eventFlow.emit(TCPEvent.ReceiveSuccess(msg))
            }
        }
    }

    fun sendMsg(msg: ByteArray) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                mWriter?.write(msg)
                mWriter?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
                eventFlow.emit(TCPEvent.SendFailure(e.message ?: "Unknown error"))
            }
        }
    }

    fun releaseTcp() {
        job?.cancel()
        mSocket?.close()
        mReader?.close()
        mWriter?.close()
    }

    fun getEventFlow(): SharedFlow<TCPEvent> {
        return eventFlow
    }

    sealed class TCPEvent {
        object ConnectSuccess : TCPEvent()
        data class ConnectFailure(val message: String) : TCPEvent()
        data class ReceiveSuccess(val message: ByteArray) : TCPEvent()
        data class ReceiveFailure(val message: String) : TCPEvent()
        data class SendFailure(val message: String) : TCPEvent()
    }

}
