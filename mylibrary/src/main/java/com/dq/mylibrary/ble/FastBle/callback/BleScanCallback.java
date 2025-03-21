package com.dq.mylibrary.ble.FastBle.callback;


import com.dq.mylibrary.ble.FastBle.data.BleDevice;

import java.util.List;

public abstract class BleScanCallback implements BleScanPresenterImp {

    public abstract void onScanFinished(List<BleDevice> scanResultList);

    public void onLeScan(BleDevice bleDevice) {
    }
}
