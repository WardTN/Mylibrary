

# iot日常工作使用避免复写

1.设备连接 TCP、UDP、BLE、WIFI、串口、USB
2.播放 RTSP、UDP 
3.权限 
4.图片
5.3D渲染



# 1.蓝牙
 1. 判断定位权限 Android 12 之后就不需要定位权限
 2. 申请相关权限 
    31 之前 
       android.Manifest.permission.BLUETOOTH,
       android.Manifest.permission.BLUETOOTH_ADMIN,
       android.Manifest.permission.ACCESS_FINE_LOCATION,
       android.Manifest.permission.ACCESS_COARSE_LOCATION
    31 之后
       android.Manifest.permission.BLUETOOTH_SCAN,
       android.Manifest.permission.BLUETOOTH_CONNECT

 3. 搜索设备
    bluetoothAdapter!!.bluetoothLeScanner!!.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
    在回调中 拿到对应设备信息 BluetoothDevice

 4. 选中对应设备进行连接
    val BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(device.address)
    根据版本不同对应不同参数进行  BluetoothDevice.connectGatt() ---> 返回值为 BluetoothGatt

 5. BluetoothGattCallback -> onConnectionStateChange
    如果判断连接成功后 -> 选择功能模块是否要给 MTU 扩容  bluetoothGatt?.requestMtu(this.mtu)

 6. 发现BLE 设备的服务  bluetoothGatt!!.discoverServices()

 7. BluetoothGattCallback -> onServicesDiscovered(gatt: BluetoothGatt, status: Int)
    如果 state 为成功  通过 bluetoothGatt!!.getService(serviceUUID) 拿到对应服务 gattService
 
 8. 判断可以拿到对应 服务之后 开始 通过 receivedID 接收消息
    gattService.getCharacteristic(uuid) 拿到 BluetoothGattCharacteristic
    BluetoothGatt.setCharacteristicNotification(BluetoothGattCharacteristic) 设置特征值

 9. 远端设备发送消息会回调 BluetoothGattCallback -> onCharacteristicChanged 接收数据

 10. 写入消息
     Android 13 及以上 writeType 为 WRITE_TYPE_DEFAULT 以下为 -1

     Android 13以下  
     writeCharacteristic.value = data
     writeCharacteristic.writeType = writeType
 
     Android 13 以上
     bluetoothGatt!!.writeCharacteristic(writeCharacteristic, data, writeType)   



WIFI连接
1.WifiManager.startScan() 搜索WIFI设备
2.WifiManager.getScanResults() 获取搜索结果
3.调用 ConnectivityManager.requestNetwork(request, callback) 实现自动连接



2.WIFI UDP 设备通信
    1.发送广播 搜寻对应设备 

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
        }

    2.监听对应端口
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
        }


    2.间隔 1S 定时任务 如果当前地址为空 则接着发送广播 如果不为空 则发送心跳 通过最后收到心跳的时间判断是否连接成功
    GlobalScope.launch {
            var theSocket: DatagramSocket? = null
            try {
                val server = InetAddress.getByName(EndoSocketUtils.ADDRESS)
                theSocket = DatagramSocket()
                val cmd = initCmd(data)
                val theOutput = DatagramPacket(cmd, cmd.size, server, sendPort)
                theSocket.send(theOutput)
                } catch (e: IOException) {
                //                logi(" receive bytesToHexString   异常 ")
                //                SolexLogUtil.dq_log("发送数据包异常 " + e.message)
                printStack(e)
                } finally {
                theSocket?.close()
            }
    }


3.TCP 设备通信
    1.建立连接
        mSocket = new Socket("192.168.1.1", 5007);
        mSocket.setKeepAlive(true);
        //mSocket.setSoTimeout(360000);
        mReader = mSocket.getInputStream();
        mWriter = mSocket.getOutputStream();
    2.接收消息

    byte[] mbyte = new byte[23];
    int readSize;
    while (canLoop()) {
         readSize = mReader.read(mbyte);    
    }

    3.发送消息
    mWriter.write(msg);
    mWriter.flush();


4.UDP 拼包



ARouter







MVI
1.页面UI（点击时间发送意图）
2.Viewmodel 收集意图，处理业务逻辑
3.Viewmodel 更新状态(修改_state)
4.页面观察Viewmodel状态（收集state,执行相关的UI）











