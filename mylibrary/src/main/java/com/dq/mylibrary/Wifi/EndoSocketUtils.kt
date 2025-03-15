package com.dq.mylibrary.Wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.dq.mylibrary.Wifi.EndoCmdUtils.Companion.getInstance
import com.dq.mylibrary.Wifi.event.EndoSocketCloseEvents
import com.dq.mylibrary.Wifi.cmd.CmdEndoBroadCast
import com.dq.mylibrary.printStack
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.net.*
import java.util.Arrays

/**
 * Socket 工具类
 */
object EndoSocketUtils {
    var mSendPort = 8019
    var mReceivePort = 8021
    var mReceiveAnglePort = 8025
    var mReceiveBitmapPort = 7080

    var ADDRESS = ""

    /**
     * 向网络中心发送广播消息
     * 该函数通过获取设备当前的IP地址，构造广播地址，并通过UDP协议发送广播消息
     * 主要用于在网络中发现或通知设备
     *
     * @param context 上下文环境，用于获取系统服务和应用上下文
     */
    fun sendBroadCastToCenter(context: Context) {
        // 获取WiFi服务管理器，用于获取WiFi连接信息
        val wifiMgr =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        // 获取当前WiFi连接的信息
        val wifiInfo = wifiMgr.connectionInfo
        // 获取当前设备的IP地址
        val ip = wifiInfo.ipAddress
        // 构造广播地址，将IP地址的最高字节设置为255
        val broadCastIP = ip or 0xFF000000.toInt()
        // 声明Socket变量
        var theSocket: DatagramSocket? = null
        try {
            // 根据广播地址获取服务器地址
            val server = InetAddress.getByName(Formatter.formatIpAddress(broadCastIP))
            // 创建DatagramSocket实例
            theSocket = DatagramSocket()
            // 构造广播消息
            val cmd = CmdEndoBroadCast().initCmd("")
            // 创建数据包，包含广播消息、服务器地址和端口
            val theOutput = DatagramPacket(cmd, cmd.size, server, mSendPort)
            // 发送数据包
            theSocket.send(theOutput)
        } catch (e: IOException) {
            // 打印异常堆栈信息
            printStack(e)
        } finally {
            // 关闭Socket
            theSocket?.close()
        }
    }

    /**
     * 启动UDP服务器以接收和处理数据包
     * 该函数创建一个DatagramSocket实例，绑定到指定的端口，并监听传入的数据包
     * 当接收到数据包时，它将调用另一个函数来解析和处理命令
     */
    fun server() {
        // 创建一个字节缓冲区，用于接收数据包
        val buffer = ByteArray(30)
        // 初始化DatagramSocket对象，开始时设置为null
        var server: DatagramSocket? = null
        try {
            // 创建一个可重复使用的DatagramSocket实例
            server = DatagramSocket(null)
            // 设置套接字为可重复使用地址
            server.reuseAddress = true
            // 绑定套接字到指定的接收端口
            server.bind(InetSocketAddress(mReceivePort))
            // 创建一个DatagramPacket实例，用于接收数据
            val packet = DatagramPacket(buffer, buffer.size)
            // 无限循环，监听和处理传入的数据包
            while (true) {
                try {
                    // 接收传入的数据包
                    server.receive(packet)
                    // 解析和处理接收到的命令
                    getInstance().parseCmd(packet)
                } catch (e: IOException) {
                    // 处理I/O异常
                    printStack(e)
                }
            }
        } catch (e: SocketException) {
            // 处理套接字异常
            printStack(e)
        } finally {
            // 确保在结束时关闭套接字
            if (server != null) {
                // 发送事件，通知套接字已关闭
                EventBus.getDefault().post(EndoSocketCloseEvents(true))
                // 关闭套接字
                server.close()
            }
        }
    }

//    fun angleServer() {
//        val buffer = ByteArray(30)
//        /*在这里同样使用约定好的端口*/
//        var server: DatagramSocket? = null
//        try {
//            server = DatagramSocket(mReceiveAnglePort)
//            val packet = DatagramPacket(buffer, buffer.size)
//            while (true) {
//                try {
//                    server.receive(packet)
//                    getInstance().parseCmd(packet)
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        } catch (e: SocketException) {
//            e.printStackTrace()
//        } finally {
//            if (server != null) {
//                server.close()
//            }
//        }
//    }

    fun bitmapServer() {
        // DatagramSocket 基于UDP 连接
        var theSocket: DatagramSocket? = null
        try {
            val server = InetAddress.getByName(ADDRESS)
            theSocket = DatagramSocket()
            val data1 = ByteArray(2)
            data1[0] = 0x20
            data1[1] = 0x36
            val theOutput = DatagramPacket(data1, 2, server, mReceiveBitmapPort)
            /*这一句就是发送广播了，其实255就代表所有的该网段的IP地址，是由路由器完成的工作*/
            //发送请求视频流 广播
            theSocket.send(theOutput)

            //接收服务器响应的数据包
            val info = ByteArray(1472)
            val infoPacket = DatagramPacket(info, info.size)
            theSocket.receive(infoPacket)
            var imgResult: ByteArray? = null
            var packageCount = 0
            while (true) {
                try {
                    packageCount++
                    theSocket.receive(infoPacket)
                    val resluts = infoPacket.data
                    val bEof = resluts[1].toInt() //判断本包数据是否为该帧数据的帧尾，bEof为1则为帧尾
                    val pkgCnt = resluts[2].toInt()
                    if (bEof == 1) {
                        //结束 形成一张图片
                        if (imgResult == null) {
                            val enen = Arrays.copyOfRange(resluts, 4, resluts.size)
                            imgResult = enen
                        } else {
                            val temp: ByteArray = imgResult
                            val enen = Arrays.copyOfRange(resluts, 4, resluts.size)
                            imgResult = ByteArray(temp.size + enen.size)
                            for (i in imgResult.indices) {
                                if (i < temp.size) {
                                    imgResult[i] = temp[i]
                                } else {
                                    imgResult[i] = enen[i - temp.size]
                                }
                            }
                        }
                        val finalResByte = imgResult
                        imgResult = null
                        if (packageCount == pkgCnt) {
                            // 未丢包
//                            byteReceiveListener?.getBytes(finalResByte)
                        }
                        packageCount = 0
                    } else {
                        if (imgResult == null) {
                            val enen = Arrays.copyOfRange(resluts, 4, resluts.size)
                            imgResult = enen
                        } else {
                            val temp: ByteArray = imgResult
                            val enen = Arrays.copyOfRange(resluts, 4, resluts.size)
                            imgResult = ByteArray(temp.size + enen.size)
                            for (i in imgResult.indices) {
                                if (i < temp.size) {
                                    imgResult[i] = temp[i]
                                } else {
                                    imgResult[i] = enen[i - temp.size]
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (theSocket != null) {
                //Log.d("Jpor", "sendBroadCastToCenter close");
                //theSocket.close();
            }
        }
    }


    fun close() {
        var theSocket: DatagramSocket? = null
        try {
            val server = InetAddress.getByName(ADDRESS)
            theSocket = DatagramSocket()
            val data1 = ByteArray(2)
            data1[0] = 0x20
            data1[1] = 0x37
            val theOutput = DatagramPacket(data1, 2, server, mReceiveBitmapPort)
            /*这一句就是发送广播了，其实255就代表所有的该网段的IP地址，是由路由器完成的工作*/
            theSocket.send(theOutput)
        } catch (e: SocketException) {
            e.printStackTrace()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 把byte转为字符串的bit
     */
    fun byteToBit(b: Byte): String {
        return (""
                + (b.toInt() shr 7 and 0x1).toByte() + (b.toInt() shr 6 and 0x1).toByte()
                + (b.toInt() shr 5 and 0x1).toByte() + (b.toInt() shr 4 and 0x1).toByte()
                + (b.toInt() shr 3 and 0x1).toByte() + (b.toInt() shr 2 and 0x1).toByte()
                + (b.toInt() shr 1 and 0x1).toByte() + (b.toInt() shr 0 and 0x1).toByte())
    }

    fun byteToInt(b1: Byte, b2: Byte): Int {
        return b1.toInt() and 0xFF shl 8 or (b2.toInt() and 0xFF)
    }
    fun byteToInt(b1: Byte): Int {
        return b1.toInt() and 0xFF
    }

    /*
     * 字节数组转16进制字符串
     */
    fun bytesToHexString(bArr: ByteArray?): String? {
        if (bArr == null) {
            return null
        }
        val sb = StringBuffer()
        var sTmp: String
        for (i in bArr.indices) {
            if (i == 0) {
                sb.append("[")
            }
            sTmp = Integer.toHexString(0xFF and bArr[i].toInt())
            if (sTmp.length < 2) sb.append(0)
            sb.append(sTmp)

            if (i == bArr.size - 1) {
                sb.append("]")
            } else {
                sb.append(", ")
            }
        }
        return sb.toString()
    }
}