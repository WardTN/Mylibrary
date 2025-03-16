package com.dq.mylibrary.ble.FastBle.callback;


import com.dq.mylibrary.ble.FastBle.data.BleDevice;

/**
 * 扫描状态
 */
public interface BleScanPresenterImp {
    // 开始扫描（是否成功）
    void onScanStarted(boolean success);
    // 扫描中
    void onScanning(BleDevice bleDevice);

}
