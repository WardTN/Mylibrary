package com.dq.mylibrary.ble.FastBle.callback;


import com.dq.mylibrary.ble.FastBle.data.BleDevice;

public abstract class BleScanAndConnectCallback extends BleGattCallback implements BleScanPresenterImp {

    public abstract void onScanFinished(BleDevice scanResult);

    public void onLeScan(BleDevice bleDevice) {
    }

}
