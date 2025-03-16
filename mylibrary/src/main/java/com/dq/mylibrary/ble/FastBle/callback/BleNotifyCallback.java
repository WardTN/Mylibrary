package com.dq.mylibrary.ble.FastBle.callback;


import com.dq.mylibrary.ble.FastBle.exception.BleException;

/**
 * Notify 回调
 */
public abstract class BleNotifyCallback extends BleBaseCallback {

    public abstract void onNotifySuccess();

    public abstract void onNotifyFailure(BleException exception);

    public abstract void onCharacteristicChanged(byte[] data);

}
