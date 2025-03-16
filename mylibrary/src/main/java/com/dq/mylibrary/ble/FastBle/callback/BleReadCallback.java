package com.dq.mylibrary.ble.FastBle.callback;


import com.dq.mylibrary.ble.FastBle.exception.BleException;

public abstract class BleReadCallback extends BleBaseCallback {

    public abstract void onReadSuccess(byte[] data);

    public abstract void onReadFailure(BleException exception);

}
