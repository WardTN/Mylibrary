package com.dq.mylibrary.ble.FastBle.callback;


import com.dq.mylibrary.ble.FastBle.exception.BleException;

public abstract class BleRssiCallback extends BleBaseCallback{

    public abstract void onRssiFailure(BleException exception);

    public abstract void onRssiSuccess(int rssi);

}