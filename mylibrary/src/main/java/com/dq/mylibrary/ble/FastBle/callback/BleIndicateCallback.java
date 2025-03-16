package com.dq.mylibrary.ble.FastBle.callback;


import com.dq.mylibrary.ble.FastBle.exception.BleException;

public abstract class BleIndicateCallback extends BleBaseCallback{

    public abstract void onIndicateSuccess();

    public abstract void onIndicateFailure(BleException exception);

    public abstract void onCharacteristicChanged(byte[] data);
}
