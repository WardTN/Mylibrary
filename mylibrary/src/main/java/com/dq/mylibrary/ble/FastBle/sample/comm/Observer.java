package com.dq.mylibrary.ble.FastBle.sample.comm;


import com.dq.mylibrary.ble.FastBle.data.BleDevice;

public interface Observer {

    void disConnected(BleDevice bleDevice);
}
