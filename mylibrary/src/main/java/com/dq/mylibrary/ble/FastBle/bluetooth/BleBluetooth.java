package com.dq.mylibrary.ble.FastBle.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.dq.mylibrary.ble.FastBle.BleManager;
import com.dq.mylibrary.ble.FastBle.callback.BleGattCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleIndicateCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleMtuChangedCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleNotifyCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleReadCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleRssiCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleWriteCallback;
import com.dq.mylibrary.ble.FastBle.data.BleConnectStateParameter;
import com.dq.mylibrary.ble.FastBle.data.BleDevice;
import com.dq.mylibrary.ble.FastBle.data.BleMsg;
import com.dq.mylibrary.ble.FastBle.exception.ConnectException;
import com.dq.mylibrary.ble.FastBle.exception.OtherException;
import com.dq.mylibrary.ble.FastBle.exception.TimeoutException;
import com.dq.mylibrary.ble.FastBle.utils.BleLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;


/**
 * BleBluetooth类负责管理与蓝牙LE设备的连接和通信
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleBluetooth {


    // Bluetooth GATT回调
    private BleGattCallback bleGattCallback;
    // Bluetooth RSSI回调
    private BleRssiCallback bleRssiCallback;
    // Bluetooth MTU变化回调
    private BleMtuChangedCallback bleMtuChangedCallback;
    // 通知回调的HashMap
    private final HashMap<String, BleNotifyCallback> bleNotifyCallbackHashMap = new HashMap<>();
    // 指示回调的HashMap
    private final HashMap<String, BleIndicateCallback> bleIndicateCallbackHashMap = new HashMap<>();
    // 写操作回调的HashMap
    private final HashMap<String, BleWriteCallback> bleWriteCallbackHashMap = new HashMap<>();
    // 读操作回调的HashMap
    private final HashMap<String, BleReadCallback> bleReadCallbackHashMap = new HashMap<>();

    // 最后一个连接状态
    private LastState lastState;
    // 是否主动断开连接
    private boolean isActiveDisconnect = false;
    // 蓝牙设备
    private final BleDevice bleDevice;
    // Bluetooth GATT对象
    private BluetoothGatt bluetoothGatt;
    // 主Handler，用于在主线程处理消息
    private final MainHandler mainHandler = new MainHandler(Looper.getMainLooper());
    // 连接重试次数
    private int connectRetryCount = 0;

    /**
     * 构造函数，初始化BleBluetooth实例
     *
     * @param bleDevice 蓝牙设备对象
     */
    public BleBluetooth(BleDevice bleDevice) {
        this.bleDevice = bleDevice;
    }


    /**
     * 创建一个新的BleConnector实例
     *
     * @return 新的BleConnector实例
     */
    public BleConnector newBleConnector() {
        return new BleConnector(this);
    }

    /**
     * 添加一个连接Gatt的回调
     *
     * @param callback 连接回调接口
     */
    public synchronized void addConnectGattCallback(BleGattCallback callback) {
        bleGattCallback = callback;
    }

    /**
     * 移除连接Gatt的回调
     */
    public synchronized void removeConnectGattCallback() {
        bleGattCallback = null;
    }

    /**
     * 添加一个通知回调
     *
     * @param uuid              特征值的UUID
     * @param bleNotifyCallback 通知回调接口
     */
    public synchronized void addNotifyCallback(String uuid, BleNotifyCallback bleNotifyCallback) {
        bleNotifyCallbackHashMap.put(uuid, bleNotifyCallback);
    }

    /**
     * 添加一个指示回调
     *
     * @param uuid                特征值的UUID
     * @param bleIndicateCallback 指示回调接口
     */
    public synchronized void addIndicateCallback(String uuid, BleIndicateCallback bleIndicateCallback) {
        bleIndicateCallbackHashMap.put(uuid, bleIndicateCallback);
    }

    /**
     * 添加一个写操作回调
     *
     * @param uuid             特征值的UUID
     * @param bleWriteCallback 写操作回调接口
     */
    public synchronized void addWriteCallback(String uuid, BleWriteCallback bleWriteCallback) {
        bleWriteCallbackHashMap.put(uuid, bleWriteCallback);
    }


    /**
     * 添加一个读操作回调
     *
     * @param uuid            特征值的UUID
     * @param bleReadCallback 读操作回调接口
     */
    public synchronized void addReadCallback(String uuid, BleReadCallback bleReadCallback) {
        bleReadCallbackHashMap.put(uuid, bleReadCallback);
    }

    /**
     * 移除一个通知回调
     *
     * @param uuid 特征值的UUID
     */
    public synchronized void removeNotifyCallback(String uuid) {
        if (bleNotifyCallbackHashMap.containsKey(uuid))
            bleNotifyCallbackHashMap.remove(uuid);
    }

    /**
     * 移除一个指示回调
     *
     * @param uuid 特征值的UUID
     */
    public synchronized void removeIndicateCallback(String uuid) {
        if (bleIndicateCallbackHashMap.containsKey(uuid))
            bleIndicateCallbackHashMap.remove(uuid);
    }


    /**
     * 移除一个写操作回调
     *
     * @param uuid 特征值的UUID
     */
    public synchronized void removeWriteCallback(String uuid) {
        if (bleWriteCallbackHashMap.containsKey(uuid))
            bleWriteCallbackHashMap.remove(uuid);
    }


    /**
     * 移除一个读操作回调
     *
     * @param uuid 特征值的UUID
     */
    public synchronized void removeReadCallback(String uuid) {
        if (bleReadCallbackHashMap.containsKey(uuid))
            bleReadCallbackHashMap.remove(uuid);
    }

    /**
     * 清除所有的特征值回调
     */
    public synchronized void clearCharacterCallback() {
        bleNotifyCallbackHashMap.clear();
        bleIndicateCallbackHashMap.clear();
        bleWriteCallbackHashMap.clear();
        bleReadCallbackHashMap.clear();
    }

    /**
     * 添加一个RSSI回调
     *
     * @param callback RSSI回调接口
     */
    public synchronized void addRssiCallback(BleRssiCallback callback) {
        bleRssiCallback = callback;
    }


    /**
     * 移除RSSI回调
     */
    public synchronized void removeRssiCallback() {
        bleRssiCallback = null;
    }

    /**
     * 添加一个MTU变化回调
     *
     * @param callback MTU变化回调接口
     */
    public synchronized void addMtuChangedCallback(BleMtuChangedCallback callback) {
        bleMtuChangedCallback = callback;
    }

    /**
     * 移除MTU变化回调
     */
    public synchronized void removeMtuChangedCallback() {
        bleMtuChangedCallback = null;
    }

    /**
     * 获取设备的唯一键值
     *
     * @return 设备的唯一键值
     */
    public String getDeviceKey() {
        return bleDevice.getKey();
    }

    /**
     * 获取蓝牙设备对象
     *
     * @return 蓝牙设备对象
     */
    public BleDevice getDevice() {
        return bleDevice;
    }

    /**
     * 获取BluetoothGatt对象
     *
     * @return BluetoothGatt对象
     */
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    /**
     * 连接到蓝牙设备
     *
     * @param bleDevice   蓝牙设备对象
     * @param autoConnect 是否自动连接
     * @param callback    连接回调接口
     * @return BluetoothGatt对象
     */
    public synchronized BluetoothGatt connect(BleDevice bleDevice,
                                              boolean autoConnect,
                                              BleGattCallback callback) {
        return connect(bleDevice, autoConnect, callback, 0);
    }


    /**
     * 连接到蓝牙设备，带重试次数
     *
     * @param bleDevice         蓝牙设备对象
     * @param autoConnect       是否自动连接
     * @param callback          连接回调接口
     * @param connectRetryCount 重试次数
     * @return BluetoothGatt对象
     */
    public synchronized BluetoothGatt connect(BleDevice bleDevice,
                                              boolean autoConnect,
                                              BleGattCallback callback,
                                              int connectRetryCount) {
        // 日志输出连接信息
        BleLog.i("connect device: " + bleDevice.getName()
                + "\nmac: " + bleDevice.getMac()
                + "\nautoConnect: " + autoConnect
                + "\ncurrentThread: " + Thread.currentThread().getId()
                + "\nconnectCount:" + (connectRetryCount + 1));
        if (connectRetryCount == 0) {
            this.connectRetryCount = 0;
        }

        addConnectGattCallback(callback);

        lastState = LastState.CONNECT_CONNECTING;

        // 根据Android版本选择合适的连接方式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(), autoConnect, coreGattCallback, TRANSPORT_LE);
        } else {
            bluetoothGatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(), autoConnect, coreGattCallback);
        }

        // 连接成功则发送广播，否则断开连接并清除资源
        if (bluetoothGatt != null) {
            if (bleGattCallback != null) {
                bleGattCallback.onStartConnect();
            }
            Message message = mainHandler.obtainMessage();
            message.what = BleMsg.MSG_CONNECT_OVER_TIME;
            mainHandler.sendMessageDelayed(message, BleManager.getInstance().getConnectOverTime());

        } else {
            disconnectGatt();
            refreshDeviceCache();
            closeBluetoothGatt();
            lastState = LastState.CONNECT_FAILURE;
            BleManager.getInstance().getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);
            if (bleGattCallback != null)
                bleGattCallback.onConnectFail(bleDevice, new OtherException("GATT connect exception occurred!"));

        }
        return bluetoothGatt;
    }

    /**
     * 主动断开蓝牙设备连接
     */
    public synchronized void disconnect() {
        isActiveDisconnect = true;
        disconnectGatt();
    }

    /**
     * 销毁对象，断开连接并清除所有资源
     */
    public synchronized void destroy() {
        lastState = LastState.CONNECT_IDLE;
        disconnectGatt();
        refreshDeviceCache();
        closeBluetoothGatt();
        removeConnectGattCallback();
        removeRssiCallback();
        removeMtuChangedCallback();
        clearCharacterCallback();
        mainHandler.removeCallbacksAndMessages(null);
    }


    /**
     * 断开Gatt连接
     */
    private synchronized void disconnectGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    /**
     * 刷新设备缓存
     */
    private synchronized void refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (bluetoothGatt != null) {
                boolean success = (Boolean) refresh.invoke(bluetoothGatt);
                BleLog.i("refreshDeviceCache, is success:  " + success);
            }
        } catch (Exception e) {
            BleLog.i("exception occur while refreshing device: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 关闭BluetoothGatt对象
     */
    private synchronized void closeBluetoothGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

    /**
     * 主处理程序类，用于处理与蓝牙设备通信相关的消息
     */
    private final class MainHandler extends Handler {

        /**
         * 构造函数
         *
         * @param looper Looper对象，用于初始化Handler
         */
        MainHandler(Looper looper) {
            super(looper);
        }

        /**
         * 处理接收到的消息
         *
         * @param msg Message对象，包含要处理的信息
         */
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BleMsg.MSG_CONNECT_FAIL: {
                    // 处理连接失败的情况
                    disconnectGatt();
                    refreshDeviceCache();
                    closeBluetoothGatt();

                    if (connectRetryCount < BleManager.getInstance().getReConnectCount()) {
                        // 尝试重新连接
                        BleLog.e("Connect fail, try reconnect " + BleManager.getInstance().getReConnectInterval() + " millisecond later");
                        ++connectRetryCount;

                        Message message = mainHandler.obtainMessage();
                        message.what = BleMsg.MSG_RECONNECT;
                        mainHandler.sendMessageDelayed(message, BleManager.getInstance().getReConnectInterval());
                    } else {
                        // 连接失败，执行相关回调
                        lastState = LastState.CONNECT_FAILURE;
                        BleManager.getInstance().getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);

                        BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
                        int status = para.getStatus();
                        if (bleGattCallback != null)
                            bleGattCallback.onConnectFail(bleDevice, new ConnectException(bluetoothGatt, status));
                    }
                }
                break;

                case BleMsg.MSG_DISCONNECTED: {
                    // 处理断开连接的情况
                    lastState = LastState.CONNECT_DISCONNECT;
                    BleManager.getInstance().getMultipleBluetoothController().removeBleBluetooth(BleBluetooth.this);

                    disconnect();
                    refreshDeviceCache();
                    closeBluetoothGatt();
                    removeRssiCallback();
                    removeMtuChangedCallback();
                    clearCharacterCallback();
                    mainHandler.removeCallbacksAndMessages(null);

                    BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
                    boolean isActive = para.isActive();
                    int status = para.getStatus();
                    if (bleGattCallback != null)
                        bleGattCallback.onDisConnected(isActive, bleDevice, bluetoothGatt, status);
                }
                break;

                case BleMsg.MSG_RECONNECT: {
                    // 执行重新连接操作
                    connect(bleDevice, false, bleGattCallback, connectRetryCount);
                }
                break;

                case BleMsg.MSG_CONNECT_OVER_TIME: {
                    // 处理连接超时的情况
                    disconnectGatt();
                    refreshDeviceCache();
                    closeBluetoothGatt();

                    lastState = LastState.CONNECT_FAILURE;
                    BleManager.getInstance().getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);

                    if (bleGattCallback != null)
                        bleGattCallback.onConnectFail(bleDevice, new TimeoutException());
                }
                break;

                case BleMsg.MSG_DISCOVER_SERVICES: {
                    // 发现服务
                    if (bluetoothGatt != null) {
                        boolean discoverServiceResult = bluetoothGatt.discoverServices();
                        if (!discoverServiceResult) {
                            Message message = mainHandler.obtainMessage();
                            message.what = BleMsg.MSG_DISCOVER_FAIL;
                            mainHandler.sendMessage(message);
                        }
                    } else {
                        Message message = mainHandler.obtainMessage();
                        message.what = BleMsg.MSG_DISCOVER_FAIL;
                        mainHandler.sendMessage(message);
                    }
                }
                break;

                case BleMsg.MSG_DISCOVER_FAIL: {
                    // 服务发现失败
                    disconnectGatt();
                    refreshDeviceCache();
                    closeBluetoothGatt();

                    lastState = LastState.CONNECT_FAILURE;
                    BleManager.getInstance().getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);

                    if (bleGattCallback != null)
                        bleGattCallback.onConnectFail(bleDevice,
                                new OtherException("GATT discover services exception occurred!"));
                }
                break;

                case BleMsg.MSG_DISCOVER_SUCCESS: {
                    // 服务发现成功
                    lastState = LastState.CONNECT_CONNECTED;
                    isActiveDisconnect = false;
                    BleManager.getInstance().getMultipleBluetoothController().removeConnectingBle(BleBluetooth.this);
                    BleManager.getInstance().getMultipleBluetoothController().addBleBluetooth(BleBluetooth.this);

                    BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
                    int status = para.getStatus();
                    if (bleGattCallback != null)
                        bleGattCallback.onConnectSuccess(bleDevice, bluetoothGatt, status);
                }
                break;

                default:
                    // 其他消息类型交由父类处理
                    super.handleMessage(msg);
                    break;
            }
        }
    }


    /**
     * BluetoothGatt回调实现类，用于处理蓝牙连接的各种状态变化和数据传输
     */
    private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        /**
         * 处理蓝牙连接状态变化的回调
         *
         * @param gatt BluetoothGatt实例，表示与蓝牙设备的连接
         * @param status 操作状态，表示连接操作的结果
         * @param newState 新的连接状态，表示蓝牙设备的当前连接状态
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            // 记录日志，包括状态、新连接状态和当前线程ID
            BleLog.i("BluetoothGattCallback：onConnectionStateChange "
                    + '\n' + "status: " + status
                    + '\n' + "newState: " + newState
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            // 更新全局的BluetoothGatt实例
            bluetoothGatt = gatt;

            // 移除连接超时的消息
            mainHandler.removeMessages(BleMsg.MSG_CONNECT_OVER_TIME);

            // 根据新的连接状态进行相应的处理
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 如果连接成功，发送消息发现服务
                Message message = mainHandler.obtainMessage();
                message.what = BleMsg.MSG_DISCOVER_SERVICES;
                mainHandler.sendMessageDelayed(message, 500);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 处理断开连接的情况，根据上一个状态进行不同的处理
                if (lastState == LastState.CONNECT_CONNECTING) {
                    // 如果之前处于连接中状态，发送连接失败的消息
                    Message message = mainHandler.obtainMessage();
                    message.what = BleMsg.MSG_CONNECT_FAIL;
                    message.obj = new BleConnectStateParameter(status);
                    mainHandler.sendMessage(message);

                } else if (lastState == LastState.CONNECT_CONNECTED) {
                    // 如果之前已经连接成功，发送断开连接的消息
                    Message message = mainHandler.obtainMessage();
                    message.what = BleMsg.MSG_DISCONNECTED;
                    BleConnectStateParameter para = new BleConnectStateParameter(status);
                    para.setActive(isActiveDisconnect);
                    message.obj = para;
                    mainHandler.sendMessage(message);
                }
            }
        }

        /**
         * 处理发现服务的回调
         *
         * @param gatt BluetoothGatt实例，表示与蓝牙设备的连接
         * @param status 操作状态，表示发现服务操作的结果
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // 记录日志，包括状态和当前线程ID
            BleLog.i("BluetoothGattCallback：onServicesDiscovered "
                    + '\n' + "status: " + status
                    + '\n' + "currentThread: " + Thread.currentThread().getId());

            // 更新全局的BluetoothGatt实例
            bluetoothGatt = gatt;

            // 根据发现服务的状态发送相应的消息
            Message message = mainHandler.obtainMessage();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                message.what = BleMsg.MSG_DISCOVER_SUCCESS;
                message.obj = new BleConnectStateParameter(status);

            } else {
                message.what = BleMsg.MSG_DISCOVER_FAIL;
            }
            mainHandler.sendMessage(message);
        }

        /**
         * 处理特征值变化的回调
         *
         * @param gatt BluetoothGatt实例，表示与蓝牙设备的连接
         * @param characteristic 发生变化的特征值
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            // 遍历通知回调哈希表，发送通知数据变化的消息
            Iterator iterator = bleNotifyCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object callback = entry.getValue();
                if (callback instanceof BleNotifyCallback) {
                    BleNotifyCallback bleNotifyCallback = (BleNotifyCallback) callback;
                    if (characteristic.getUuid().toString().equalsIgnoreCase(bleNotifyCallback.getKey())) {
                        Handler handler = bleNotifyCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_NOTIFY_DATA_CHANGE;
                            message.obj = bleNotifyCallback;
                            Bundle bundle = new Bundle();
                            bundle.putByteArray(BleMsg.KEY_NOTIFY_BUNDLE_VALUE, characteristic.getValue());
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }

            // 遍历指示回调哈希表，发送指示数据变化的消息
            iterator = bleIndicateCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object callback = entry.getValue();
                if (callback instanceof BleIndicateCallback) {
                    BleIndicateCallback bleIndicateCallback = (BleIndicateCallback) callback;
                    if (characteristic.getUuid().toString().equalsIgnoreCase(bleIndicateCallback.getKey())) {
                        Handler handler = bleIndicateCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_INDICATE_DATA_CHANGE;
                            message.obj = bleIndicateCallback;
                            Bundle bundle = new Bundle();
                            bundle.putByteArray(BleMsg.KEY_INDICATE_BUNDLE_VALUE, characteristic.getValue());
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }
        }


        /**
         * 处理描述符写入的回调
         *
         * @param gatt BluetoothGatt实例，表示与蓝牙设备的连接
         * @param descriptor 被写入的描述符
         * @param status 操作状态，表示写入操作的结果
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            // 遍历通知回调哈希表，发送通知结果的消息
            Iterator iterator = bleNotifyCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object callback = entry.getValue();
                if (callback instanceof BleNotifyCallback) {
                    BleNotifyCallback bleNotifyCallback = (BleNotifyCallback) callback;
                    if (descriptor.getCharacteristic().getUuid().toString().equalsIgnoreCase(bleNotifyCallback.getKey())) {
                        Handler handler = bleNotifyCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_NOTIFY_RESULT;
                            message.obj = bleNotifyCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_NOTIFY_BUNDLE_STATUS, status);
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }

            // 遍历指示回调哈希表，发送指示结果的消息
            iterator = bleIndicateCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object callback = entry.getValue();
                if (callback instanceof BleIndicateCallback) {
                    BleIndicateCallback bleIndicateCallback = (BleIndicateCallback) callback;
                    if (descriptor.getCharacteristic().getUuid().toString().equalsIgnoreCase(bleIndicateCallback.getKey())) {
                        Handler handler = bleIndicateCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_INDICATE_RESULT;
                            message.obj = bleIndicateCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_INDICATE_BUNDLE_STATUS, status);
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }
        }


        /**
         * 处理特征值写入的回调
         *
         * @param gatt BluetoothGatt实例，表示与蓝牙设备的连接
         * @param characteristic 被写入的特征值
         * @param status 操作状态，表示写入操作的结果
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            // 遍历写入回调哈希表，发送写入结果的消息
            Iterator iterator = bleWriteCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object callback = entry.getValue();
                if (callback instanceof BleWriteCallback) {
                    BleWriteCallback bleWriteCallback = (BleWriteCallback) callback;
                    if (characteristic.getUuid().toString().equalsIgnoreCase(bleWriteCallback.getKey())) {
                        Handler handler = bleWriteCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_WRITE_RESULT;
                            message.obj = bleWriteCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_WRITE_BUNDLE_STATUS, status);
                            bundle.putByteArray(BleMsg.KEY_WRITE_BUNDLE_VALUE, characteristic.getValue());
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }
        }


        /**
         * 处理特征值读取的回调
         *
         * @param gatt BluetoothGatt实例，表示与蓝牙设备的连接
         * @param characteristic 被读取的特征值
         * @param status 操作状态，表示读取操作的结果
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            // 遍历读取回调哈希表，发送读取结果的消息
            Iterator iterator = bleReadCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object callback = entry.getValue();
                if (callback instanceof BleReadCallback) {
                    BleReadCallback bleReadCallback = (BleReadCallback) callback;
                    if (characteristic.getUuid().toString().equalsIgnoreCase(bleReadCallback.getKey())) {
                        Handler handler = bleReadCallback.getHandler();
                        if (handler != null) {
                            Message message = handler.obtainMessage();
                            message.what = BleMsg.MSG_CHA_READ_RESULT;
                            message.obj = bleReadCallback;
                            Bundle bundle = new Bundle();
                            bundle.putInt(BleMsg.KEY_READ_BUNDLE_STATUS, status);
                            bundle.putByteArray(BleMsg.KEY_READ_BUNDLE_VALUE, characteristic.getValue());
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            }
        }


        /**
         * 当读取远程BLE设备的RSSI（接收信号强度指示）时回调该方法
         *
         * @param gatt BluetoothGatt实例，表示与远程设备的连接
         * @param rssi int类型，表示读取到的RSSI值
         * @param status int类型，表示读取RSSI的操作状态
         */
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);

            // 检查是否设置了RSSI读取回调
            if (bleRssiCallback != null) {
                Handler handler = bleRssiCallback.getHandler();
                // 确认Handler不为空，以便进行消息传递
                if (handler != null) {
                    Message message = handler.obtainMessage();
                    message.what = BleMsg.MSG_READ_RSSI_RESULT;
                    message.obj = bleRssiCallback;
                    // 创建Bundle以存储RSSI读取结果和状态
                    Bundle bundle = new Bundle();
                    bundle.putInt(BleMsg.KEY_READ_RSSI_BUNDLE_STATUS, status);
                    bundle.putInt(BleMsg.KEY_READ_RSSI_BUNDLE_VALUE, rssi);
                    message.setData(bundle);
                    // 发送消息以通知RSSI读取结果
                    handler.sendMessage(message);
                }
            }
        }


        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            if (bleMtuChangedCallback != null) {
                Handler handler = bleMtuChangedCallback.getHandler();
                if (handler != null) {
                    Message message = handler.obtainMessage();
                    message.what = BleMsg.MSG_SET_MTU_RESULT;
                    message.obj = bleMtuChangedCallback;
                    Bundle bundle = new Bundle();
                    bundle.putInt(BleMsg.KEY_SET_MTU_BUNDLE_STATUS, status);
                    bundle.putInt(BleMsg.KEY_SET_MTU_BUNDLE_VALUE, mtu);
                    message.setData(bundle);
                    handler.sendMessage(message);
                }
            }
        }
    };

    enum LastState {
        CONNECT_IDLE,
        CONNECT_CONNECTING,
        CONNECT_CONNECTED,
        CONNECT_FAILURE,
        CONNECT_DISCONNECT
    }

}
