package com.dq.mylibrary.ble.FastBle.sample.comm;


import com.dq.mylibrary.ble.FastBle.data.BleDevice;

public interface Observable {

    void addObserver(Observer obj);

    void deleteObserver(Observer obj);

    void notifyObserver(BleDevice bleDevice);
}
