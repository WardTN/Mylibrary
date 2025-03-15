package com.dq.mylibrary.tcp;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPUtil {

    private Socket mSocket;
    private InputStream mReader;
    private OutputStream mWriter;

    private TcpLoopThread loopRecThread;
    private TCPListener listener;



    public TCPUtil(TCPListener listener) {
        this.listener = listener;
    }

    /**
     * 连接TCP
     */
    public void connect() {
        new Thread() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket("192.168.1.1", 5007);
                    mSocket.setKeepAlive(true);
//                    mSocket.setSoTimeout(360000);
                    mReader = mSocket.getInputStream();
                    mWriter = mSocket.getOutputStream();

                    if (mSocket.isConnected()) {
//                        Log.d("CHEN", "Socket 已连接")
                        if (listener!= null){
                            listener.tcpConnectSuc();
                        }
                    }

                    revMsg();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (listener!= null){
                        listener.tcpConnectFail(e.getMessage());
                    }
                }
            }
        }.start();
    }

    private void revMsg() {
        loopRecThread = new TcpLoopThread() {
            @Override
            public void run() {
                super.run();
                try {
                    byte[] mbyte = new byte[23];
                    int readSize;
                    while (canLoop()) {
                        readSize = mReader.read(mbyte);
                        if (readSize > 8) {
                            byte[] msg = new byte[readSize];
                            System.arraycopy(mbyte, 0, msg, 0, readSize);
//                            byte[] encryptData = cmdUtil.MsgRead(msg, 2);
//                            String recStr = StringUtilKt.bytesToHexString(encryptData);
//                            LoggerUtil.dq_log("revMsg 解密后数据为" + recStr);
                            if (listener != null) {
                                listener.tcpReceiverSuc(msg);
                            }
                        } else if (readSize == -1) {
//                            LoggerUtil.dq_log("readSize: " + readSize);
                            break;
                        }
                    }
                } catch (Exception e) {
//                    LoggerUtil.dq_log("revMsg 读取失败: " + e.getMessage());
                    e.printStackTrace();
//                    LoggerUtil.dq_log("revMsg 读取失败堆栈跟踪: " + Log.getStackTraceString(e));
                } finally {
//                    LoggerUtil.dq_log("revMsg exit receive thread");
                    try {
                        if (mReader != null) {
                            mReader.close();
                        }
                        if (mWriter != null) {
                            mWriter.close();
                        }
                        if (mSocket != null) {
                            mSocket.close();
                        }
                    } catch (IOException e) {
//                        LoggerUtil.dq_log("revMsg 资源释放失败: " + e.getMessage());
                    }
                }
            }
        };

        loopRecThread.start();
    }

//    private void splitMsg(String msg) {
//        if (listener != null) {
//            listener.tcpReceiverSuc(msg);
//        }
//    }


    public void sendMsg(byte[] msg) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
//                    LoggerUtil.dq_log("当前发送的消息为" + msg);
//                    println("发送首包: ${firstPacket.joinToString(", ") { it.toString() }}");
                    mWriter.write(msg);
                    mWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public interface TCPListener {
        void tcpReceiverSuc(byte[] msg);
        void tcpConnectSuc();
        void tcpConnectFail(String msg);
    }

    public void releaseTcp() throws IOException {
//        LoggerUtil.dq_log("执行release TCP");
        if (mSocket != null) {
            mSocket.close();
        }

        if (loopRecThread != null) {
            loopRecThread.cancelLoop();
        }

    }
}
