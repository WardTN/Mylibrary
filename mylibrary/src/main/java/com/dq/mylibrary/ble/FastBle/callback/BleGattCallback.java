
package com.dq.mylibrary.ble.FastBle.callback;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;

import com.dq.mylibrary.ble.FastBle.data.BleDevice;
import com.dq.mylibrary.ble.FastBle.exception.BleException;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleGattCallback extends BluetoothGattCallback {

    // 开始进行连接
    public abstract void onStartConnect();

    // 连接不成功
    public abstract void onConnectFail(BleDevice bleDevice, BleException exception);

    // 连接成功并发现服务
    public abstract void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status);

    // 连接断开，特指连接后再断开的情况。在这里可以监控设备的连接状态，一旦连接断开，可以根据自身情况考虑对BleDevice对象进行重连操作。
    // 需要注意的是，断开和重连之间最好间隔一段时间，否则可能会出现长时间连接不上的情况。
    // 此外，如果通过调用disconnect(BleDevice bleDevice)方法，主动断开蓝牙连接的结果也会在这个方法中回调，此时isActiveDisConnected将会是true
    public abstract void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status);

}