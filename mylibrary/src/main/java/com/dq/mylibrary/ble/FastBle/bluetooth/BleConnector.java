
package com.dq.mylibrary.ble.FastBle.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.dq.mylibrary.ble.FastBle.BleManager;
import com.dq.mylibrary.ble.FastBle.callback.BleIndicateCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleMtuChangedCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleNotifyCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleReadCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleRssiCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleWriteCallback;
import com.dq.mylibrary.ble.FastBle.data.BleMsg;
import com.dq.mylibrary.ble.FastBle.data.BleWriteState;
import com.dq.mylibrary.ble.FastBle.exception.GattException;
import com.dq.mylibrary.ble.FastBle.exception.OtherException;
import com.dq.mylibrary.ble.FastBle.exception.TimeoutException;

import java.util.UUID;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleConnector {

    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * BluetoothGatt对象，用于表示与远程设备的连接
     * 通过它可以与蓝牙设备建立连接，进行通信
     */
    private BluetoothGatt mBluetoothGatt;

    /**
     * BluetoothGattService对象，代表远程设备提供的Service
     * 每个服务都有唯一的UUID，定义了一组特性和属性
     */
    private BluetoothGattService mGattService;

    /**
     * BluetoothGattCharacteristic对象，表示服务的一个特性
     * 特性包含了一个或多个属性，如读、写或 notify
     */
    private BluetoothGattCharacteristic mCharacteristic;

    /**
     * BleBluetooth对象，可能是自定义的类，用于表示BLE蓝牙设备
     * 它可能封装了一些特定于设备的操作或数据处理逻辑
     */
    private BleBluetooth mBleBluetooth;

    /**
     * Handler对象，用于在主线程中执行操作或发送消息
     * 它是线程与Activity通信的桥梁，可以用来更新UI或处理异步任务
     */
    private Handler mHandler;

    /**
     * 构造函数，初始化BleConnector对象
     *
     * @param bleBluetooth BleBluetooth对象，用于蓝牙通信
     */
    BleConnector(BleBluetooth bleBluetooth) {
        // 初始化成员变量
        this.mBleBluetooth = bleBluetooth;
        this.mBluetoothGatt = bleBluetooth.getBluetoothGatt();

        // 创建一个Handler对象，用于处理UI线程的消息
        this.mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                // 根据消息类型处理不同的蓝牙通知事件
                switch (msg.what) {
                    case BleMsg.MSG_CHA_NOTIFY_START: {
                        // 处理通知开始的消息
                        BleNotifyCallback notifyCallback = (BleNotifyCallback) msg.obj;
                        if (notifyCallback != null)
                            notifyCallback.onNotifyFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_CHA_NOTIFY_RESULT: {
                        // 处理通知结果的消息
                        notifyMsgInit();
                        BleNotifyCallback notifyCallback = (BleNotifyCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_NOTIFY_BUNDLE_STATUS);
                        if (notifyCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                notifyCallback.onNotifySuccess();
                            } else {
                                notifyCallback.onNotifyFailure(new GattException(status));
                            }
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_NOTIFY_DATA_CHANGE: {
                        // 处理通知数据变化的消息
                        BleNotifyCallback notifyCallback = (BleNotifyCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        byte[] value = bundle.getByteArray(BleMsg.KEY_NOTIFY_BUNDLE_VALUE);
                        if (notifyCallback != null) {
                            notifyCallback.onCharacteristicChanged(value);
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_INDICATE_START: {
                        // 处理指示开始的消息
                        BleIndicateCallback indicateCallback = (BleIndicateCallback) msg.obj;
                        if (indicateCallback != null)
                            indicateCallback.onIndicateFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_CHA_INDICATE_RESULT: {
                        // 处理指示结果的消息
                        indicateMsgInit();
                        BleIndicateCallback indicateCallback = (BleIndicateCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_INDICATE_BUNDLE_STATUS);
                        if (indicateCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                indicateCallback.onIndicateSuccess();
                            } else {
                                indicateCallback.onIndicateFailure(new GattException(status));
                            }
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_INDICATE_DATA_CHANGE: {
                        // 处理指示数据变化的消息
                        BleIndicateCallback indicateCallback = (BleIndicateCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        byte[] value = bundle.getByteArray(BleMsg.KEY_INDICATE_BUNDLE_VALUE);
                        if (indicateCallback != null) {
                            indicateCallback.onCharacteristicChanged(value);
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_WRITE_START: {
                        // 处理写操作开始的消息
                        BleWriteCallback writeCallback = (BleWriteCallback) msg.obj;
                        if (writeCallback != null) {
                            writeCallback.onWriteFailure(new TimeoutException());
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_WRITE_RESULT: {
                        // 处理写操作结果的消息
                        writeMsgInit();
                        BleWriteCallback writeCallback = (BleWriteCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_WRITE_BUNDLE_STATUS);
                        byte[] value = bundle.getByteArray(BleMsg.KEY_WRITE_BUNDLE_VALUE);
                        if (writeCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                writeCallback.onWriteSuccess(BleWriteState.DATA_WRITE_SINGLE, BleWriteState.DATA_WRITE_SINGLE, value);
                            } else {
                                writeCallback.onWriteFailure(new GattException(status));
                            }
                        }
                        break;
                    }

                    case BleMsg.MSG_CHA_READ_START: {
                        // 处理读操作开始的消息
                        BleReadCallback readCallback = (BleReadCallback) msg.obj;
                        if (readCallback != null)
                            readCallback.onReadFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_CHA_READ_RESULT: {
                        // 处理读操作结果的消息
                        readMsgInit();
                        BleReadCallback readCallback = (BleReadCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_READ_BUNDLE_STATUS);
                        byte[] value = bundle.getByteArray(BleMsg.KEY_READ_BUNDLE_VALUE);
                        if (readCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                readCallback.onReadSuccess(value);
                            } else {
                                readCallback.onReadFailure(new GattException(status));
                            }
                        }
                        break;
                    }

                    case BleMsg.MSG_READ_RSSI_START: {
                        // 处理读取RSSI开始的消息
                        BleRssiCallback rssiCallback = (BleRssiCallback) msg.obj;
                        if (rssiCallback != null)
                            rssiCallback.onRssiFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_READ_RSSI_RESULT: {
                        // 处理读取RSSI结果的消息
                        rssiMsgInit();
                        BleRssiCallback rssiCallback = (BleRssiCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_READ_RSSI_BUNDLE_STATUS);
                        int value = bundle.getInt(BleMsg.KEY_READ_RSSI_BUNDLE_VALUE);
                        if (rssiCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                rssiCallback.onRssiSuccess(value);
                            } else {
                                rssiCallback.onRssiFailure(new GattException(status));
                            }
                        }
                        break;
                    }

                    case BleMsg.MSG_SET_MTU_START: {
                        // 处理设置MTU开始的消息
                        BleMtuChangedCallback mtuChangedCallback = (BleMtuChangedCallback) msg.obj;
                        if (mtuChangedCallback != null)
                            mtuChangedCallback.onSetMTUFailure(new TimeoutException());
                        break;
                    }

                    case BleMsg.MSG_SET_MTU_RESULT: {
                        // 处理设置MTU结果的消息
                        mtuChangedMsgInit();
                        BleMtuChangedCallback mtuChangedCallback = (BleMtuChangedCallback) msg.obj;
                        Bundle bundle = msg.getData();
                        int status = bundle.getInt(BleMsg.KEY_SET_MTU_BUNDLE_STATUS);
                        int value = bundle.getInt(BleMsg.KEY_SET_MTU_BUNDLE_VALUE);
                        if (mtuChangedCallback != null) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                mtuChangedCallback.onMtuChanged(value);
                            } else {
                                mtuChangedCallback.onSetMTUFailure(new GattException(status));
                            }
                        }
                        break;
                    }
                }
            }
        };
    }


    private BleConnector withUUID(UUID serviceUUID, UUID characteristicUUID) {
        if (serviceUUID != null && mBluetoothGatt != null) {
            mGattService = mBluetoothGatt.getService(serviceUUID);
        }
        if (mGattService != null && characteristicUUID != null) {
            mCharacteristic = mGattService.getCharacteristic(characteristicUUID);
        }
        return this;
    }

    public BleConnector withUUIDString(String serviceUUID, String characteristicUUID) {
        return withUUID(formUUID(serviceUUID), formUUID(characteristicUUID));
    }

    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }


    /*------------------------------- main operation ----------------------------------- */


    /**
     * 启用特征值通知功能
     *
     * 此方法用于启用BLE特征值的通知功能当特征值支持通知属性时，
     * 将注册通知并调用回调方法，否则将返回错误信息
     *
     * @param bleNotifyCallback           通知回调接口，用于接收通知结果
     * @param uuid_notify                 特征值的UUID，用于标识特定的特征值
     * @param userCharacteristicDescriptor 是否使用特征值描述符进行通知设置
     */
    public void enableCharacteristicNotify(BleNotifyCallback bleNotifyCallback, String uuid_notify,
                                           boolean userCharacteristicDescriptor) {
        // 检查特征值是否支持通知
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

            // 处理通知回调
            handleCharacteristicNotifyCallback(bleNotifyCallback, uuid_notify);
            // 设置特征值通知
            setCharacteristicNotification(mBluetoothGatt, mCharacteristic, userCharacteristicDescriptor, true, bleNotifyCallback);
        } else {
            // 如果特征值不支持通知，且回调接口不为空，则调用失败回调
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("this characteristic not support notify!"));
        }
    }
    /**
     * stop notify
     */
    public boolean disableCharacteristicNotify(boolean useCharacteristicDescriptor) {
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicNotification(mBluetoothGatt, mCharacteristic,
                    useCharacteristicDescriptor, false, null);
        } else {
            return false;
        }
    }

    /**
     * notify setting
     */
    private boolean setCharacteristicNotification(BluetoothGatt gatt,
                                                  BluetoothGattCharacteristic characteristic,
                                                  boolean useCharacteristicDescriptor,
                                                  boolean enable,
                                                  BleNotifyCallback bleNotifyCallback) {
        if (gatt == null || characteristic == null) {
            notifyMsgInit();
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            notifyMsgInit();
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor;
        if (useCharacteristicDescriptor) {
            descriptor = characteristic.getDescriptor(characteristic.getUuid());
        } else {
            descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        }
        if (descriptor == null) {
            notifyMsgInit();
            if (bleNotifyCallback != null)
                bleNotifyCallback.onNotifyFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                notifyMsgInit();
                if (bleNotifyCallback != null)
                    bleNotifyCallback.onNotifyFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return success2;
        }
    }

    /**
     * indicate
     */
    public void enableCharacteristicIndicate(BleIndicateCallback bleIndicateCallback, String uuid_indicate,
                                             boolean useCharacteristicDescriptor) {
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            handleCharacteristicIndicateCallback(bleIndicateCallback, uuid_indicate);
            setCharacteristicIndication(mBluetoothGatt, mCharacteristic,
                    useCharacteristicDescriptor, true, bleIndicateCallback);
        } else {
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("this characteristic not support indicate!"));
        }
    }


    /**
     * stop indicate
     */
    public boolean disableCharacteristicIndicate(boolean userCharacteristicDescriptor) {
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicIndication(mBluetoothGatt, mCharacteristic,
                    userCharacteristicDescriptor, false, null);
        } else {
            return false;
        }
    }

    /**
     * indicate setting
     */
    private boolean setCharacteristicIndication(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic,
                                                boolean useCharacteristicDescriptor,
                                                boolean enable,
                                                BleIndicateCallback bleIndicateCallback) {
        if (gatt == null || characteristic == null) {
            indicateMsgInit();
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            indicateMsgInit();
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor;
        if (useCharacteristicDescriptor) {
            descriptor = characteristic.getDescriptor(characteristic.getUuid());
        } else {
            descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        }
        if (descriptor == null) {
            indicateMsgInit();
            if (bleIndicateCallback != null)
                bleIndicateCallback.onIndicateFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                indicateMsgInit();
                if (bleIndicateCallback != null)
                    bleIndicateCallback.onIndicateFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return success2;
        }
    }

    /**
     * write
     */
    public void writeCharacteristic(byte[] data, BleWriteCallback bleWriteCallback, String uuid_write) {
        if (data == null || data.length <= 0) {
            if (bleWriteCallback != null)
                bleWriteCallback.onWriteFailure(new OtherException("the data to be written is empty"));
            return;
        }

        if (mCharacteristic == null
                || (mCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            if (bleWriteCallback != null)
                bleWriteCallback.onWriteFailure(new OtherException("this characteristic not support write!"));
            return;
        }

        if (mCharacteristic.setValue(data)) {
            handleCharacteristicWriteCallback(bleWriteCallback, uuid_write);
            if (!mBluetoothGatt.writeCharacteristic(mCharacteristic)) {
                writeMsgInit();
                if (bleWriteCallback != null)
                    bleWriteCallback.onWriteFailure(new OtherException("gatt writeCharacteristic fail"));
            }
        } else {
            if (bleWriteCallback != null)
                bleWriteCallback.onWriteFailure(new OtherException("Updates the locally stored value of this characteristic fail"));
        }
    }

    /**
     * read
     */
    public void readCharacteristic(BleReadCallback bleReadCallback, String uuid_read) {
        if (mCharacteristic != null
                && (mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

            handleCharacteristicReadCallback(bleReadCallback, uuid_read);
            if (!mBluetoothGatt.readCharacteristic(mCharacteristic)) {
                readMsgInit();
                if (bleReadCallback != null)
                    bleReadCallback.onReadFailure(new OtherException("gatt readCharacteristic fail"));
            }
        } else {
            if (bleReadCallback != null)
                bleReadCallback.onReadFailure(new OtherException("this characteristic not support read!"));
        }
    }

    /**
     * rssi
     */
    public void readRemoteRssi(BleRssiCallback bleRssiCallback) {
        handleRSSIReadCallback(bleRssiCallback);
        if (!mBluetoothGatt.readRemoteRssi()) {
            rssiMsgInit();
            if (bleRssiCallback != null)
                bleRssiCallback.onRssiFailure(new OtherException("gatt readRemoteRssi fail"));
        }
    }

    /**
     * set mtu
     */
    public void setMtu(int requiredMtu, BleMtuChangedCallback bleMtuChangedCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            handleSetMtuCallback(bleMtuChangedCallback);
            if (!mBluetoothGatt.requestMtu(requiredMtu)) {
                mtuChangedMsgInit();
                if (bleMtuChangedCallback != null)
                    bleMtuChangedCallback.onSetMTUFailure(new OtherException("gatt requestMtu fail"));
            }
        } else {
            if (bleMtuChangedCallback != null)
                bleMtuChangedCallback.onSetMTUFailure(new OtherException("API level lower than 21"));
        }
    }

    /**
     * requestConnectionPriority
     *
     * @param connectionPriority Request a specific connection priority. Must be one of
     *                           {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED},
     *                           {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}
     *                           or {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
     * @throws IllegalArgumentException If the parameters are outside of their
     *                                  specified range.
     */
    public boolean requestConnectionPriority(int connectionPriority) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mBluetoothGatt.requestConnectionPriority(connectionPriority);
        }
        return false;
    }


    /**************************************** Handle call back ******************************************/

    /**
     * notify
     */
    private void handleCharacteristicNotifyCallback(BleNotifyCallback bleNotifyCallback,
                                                    String uuid_notify) {
        if (bleNotifyCallback != null) {
            notifyMsgInit();
            bleNotifyCallback.setKey(uuid_notify);
            bleNotifyCallback.setHandler(mHandler);
            mBleBluetooth.addNotifyCallback(uuid_notify, bleNotifyCallback);
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(BleMsg.MSG_CHA_NOTIFY_START, bleNotifyCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * indicate
     */
    private void handleCharacteristicIndicateCallback(BleIndicateCallback bleIndicateCallback,
                                                      String uuid_indicate) {
        if (bleIndicateCallback != null) {
            indicateMsgInit();
            bleIndicateCallback.setKey(uuid_indicate);
            bleIndicateCallback.setHandler(mHandler);
            mBleBluetooth.addIndicateCallback(uuid_indicate, bleIndicateCallback);
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(BleMsg.MSG_CHA_INDICATE_START, bleIndicateCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * write
     */
    private void handleCharacteristicWriteCallback(BleWriteCallback bleWriteCallback,
                                                   String uuid_write) {
        if (bleWriteCallback != null) {
            writeMsgInit();
            bleWriteCallback.setKey(uuid_write);
            bleWriteCallback.setHandler(mHandler);
            mBleBluetooth.addWriteCallback(uuid_write, bleWriteCallback);
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(BleMsg.MSG_CHA_WRITE_START, bleWriteCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * read
     */
    private void handleCharacteristicReadCallback(BleReadCallback bleReadCallback,
                                                  String uuid_read) {
        if (bleReadCallback != null) {
            readMsgInit();
            bleReadCallback.setKey(uuid_read);
            bleReadCallback.setHandler(mHandler);
            mBleBluetooth.addReadCallback(uuid_read, bleReadCallback);
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(BleMsg.MSG_CHA_READ_START, bleReadCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * rssi
     */
    private void handleRSSIReadCallback(BleRssiCallback bleRssiCallback) {
        if (bleRssiCallback != null) {
            rssiMsgInit();
            bleRssiCallback.setHandler(mHandler);
            mBleBluetooth.addRssiCallback(bleRssiCallback);
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(BleMsg.MSG_READ_RSSI_START, bleRssiCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * set mtu
     */
    private void handleSetMtuCallback(BleMtuChangedCallback bleMtuChangedCallback) {
        if (bleMtuChangedCallback != null) {
            mtuChangedMsgInit();
            bleMtuChangedCallback.setHandler(mHandler);
            mBleBluetooth.addMtuChangedCallback(bleMtuChangedCallback);
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(BleMsg.MSG_SET_MTU_START, bleMtuChangedCallback),
                    BleManager.getInstance().getOperateTimeout());
        }
    }

    public void notifyMsgInit() {
        mHandler.removeMessages(BleMsg.MSG_CHA_NOTIFY_START);
    }

    public void indicateMsgInit() {
        mHandler.removeMessages(BleMsg.MSG_CHA_INDICATE_START);
    }

    public void writeMsgInit() {
        mHandler.removeMessages(BleMsg.MSG_CHA_WRITE_START);
    }

    public void readMsgInit() {
        mHandler.removeMessages(BleMsg.MSG_CHA_READ_START);
    }

    public void rssiMsgInit() {
        mHandler.removeMessages(BleMsg.MSG_READ_RSSI_START);
    }

    public void mtuChangedMsgInit() {
        mHandler.removeMessages(BleMsg.MSG_SET_MTU_START);
    }

}
