package com.dq.mylibrary.ble.FastBle.callback;


import com.dq.mylibrary.ble.FastBle.exception.BleException;

public abstract class BleMtuChangedCallback extends BleBaseCallback {

    public abstract void onSetMTUFailure(BleException exception);

    public abstract void onMtuChanged(int mtu);

}
