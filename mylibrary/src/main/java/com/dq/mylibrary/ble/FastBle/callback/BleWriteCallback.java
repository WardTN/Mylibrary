package com.dq.mylibrary.ble.FastBle.callback;


import com.dq.mylibrary.ble.FastBle.exception.BleException;

public abstract class BleWriteCallback extends BleBaseCallback{

    public abstract void onWriteSuccess(int current, int total, byte[] justWrite);

    public abstract void onWriteFailure(BleException exception);

}
