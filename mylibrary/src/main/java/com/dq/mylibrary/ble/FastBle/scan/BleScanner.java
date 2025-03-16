package com.dq.mylibrary.ble.FastBle.scan;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.dq.mylibrary.ble.FastBle.BleManager;
import com.dq.mylibrary.ble.FastBle.callback.BleScanAndConnectCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleScanCallback;
import com.dq.mylibrary.ble.FastBle.callback.BleScanPresenterImp;
import com.dq.mylibrary.ble.FastBle.data.BleDevice;
import com.dq.mylibrary.ble.FastBle.data.BleScanState;
import com.dq.mylibrary.ble.FastBle.utils.BleLog;

import java.util.List;
import java.util.UUID;

/**
 * 蓝牙扫描类
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleScanner {

    public static BleScanner getInstance() {
        return BleScannerHolder.sBleScanner;
    }

    private static class BleScannerHolder {
        private static final BleScanner sBleScanner = new BleScanner();
    }

    private BleScanState mBleScanState = BleScanState.STATE_IDLE;

    /**
     *
     */
    private final BleScanPresenter mBleScanPresenter = new BleScanPresenter() {

        // 会回到主线程，参数表示本次扫描动作是否开启成功。
        // 由于蓝牙没有打开，上一次扫描没有结束等原因，会造成扫描开启失败。
        @Override
        public void onScanStarted(boolean success) {
            BleScanPresenterImp callback = mBleScanPresenter.getBleScanPresenterImp();
            if (callback != null) {
                callback.onScanStarted(success);
            }
        }

        // 扫描过程中所有被扫描到的结果回调。由于扫描及过滤的过程是在工作线程中的，此方法也处于工作线程中。
        // 同一个设备会在不同的时间，携带自身不同的状态（比如信号强度等），出现在这个回调方法中，
        // 出现次数取决于周围的设备量及外围设备的广播间隔。
        @Override
        public void onLeScan(BleDevice bleDevice) {
            if (mBleScanPresenter.ismNeedConnect()) {
                BleScanAndConnectCallback callback = (BleScanAndConnectCallback) mBleScanPresenter.getBleScanPresenterImp();
                if (callback != null) {
                    callback.onLeScan(bleDevice);
                }
            } else {
                BleScanCallback callback = (BleScanCallback) mBleScanPresenter.getBleScanPresenterImp();
                if (callback != null) {
                    callback.onLeScan(bleDevice);
                }
            }
        }

        // 扫描过程中的所有过滤后的结果回调。与onLeScan区别之处在于：它会回到主线程；
        // 同一个设备只会出现一次；出现的设备是经过扫描过滤规则过滤后的设备。
        @Override
        public void onScanning(BleDevice result) {
            BleScanPresenterImp callback = mBleScanPresenter.getBleScanPresenterImp();
            if (callback != null) {
                callback.onScanning(result);
            }
        }

        // 本次扫描时段内所有被扫描且过滤后的设备集合。
        // 它会回到主线程，相当于onScanning设备之和
        @Override
        public void onScanFinished(List<BleDevice> bleDeviceList) {
            // 检查是否需要连接设备
            if (mBleScanPresenter.ismNeedConnect()) {
                // 获取BLE扫描并连接回调接口实例
                final BleScanAndConnectCallback callback = (BleScanAndConnectCallback) mBleScanPresenter.getBleScanPresenterImp();
                // 判断设备列表是否为空或数量小于1
                if (bleDeviceList == null || bleDeviceList.isEmpty()) {
                    // 如果设备列表为空，通知回调扫描结束但无设备可连接
                    if (callback != null) {
                        callback.onScanFinished(null);
                    }
                } else {
                    // 如果设备列表不为空，通知回调扫描结束并尝试连接第一个设备
                    if (callback != null) {
                        callback.onScanFinished(bleDeviceList.get(0));
                    }
                    final List<BleDevice> list = bleDeviceList;
                    // 延迟100毫秒在主线程中执行连接操作，以避免阻塞主线程
                    new Handler(Looper.getMainLooper()).postDelayed(() -> BleManager.getInstance().connect(list.get(0), callback), 100);
                }
            } else {
                // 如果不需要连接设备，获取BLE扫描回调接口实例
                BleScanCallback callback = (BleScanCallback) mBleScanPresenter.getBleScanPresenterImp();
                // 通知回调扫描结束，传递整个设备列表
                if (callback != null) {
                    callback.onScanFinished(bleDeviceList);
                }
            }
        }
    };

    public void scan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy, long timeOut, final BleScanCallback callback) {
        startLeScan(serviceUuids, names, mac, fuzzy, false, timeOut, callback);
    }

    public void scanAndConnect(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy, long timeOut, BleScanAndConnectCallback callback) {
        startLeScan(serviceUuids, names, mac, fuzzy, true, timeOut, callback);
    }

    private synchronized void startLeScan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy, boolean needConnect, long timeOut, BleScanPresenterImp imp) {

        // 判断当前扫描状态
        if (mBleScanState != BleScanState.STATE_IDLE) {
            BleLog.w("scan action already exists, complete the previous scan action first");
            if (imp != null) {
                imp.onScanStarted(false);
            }
            return;
        }
        // 填充配置信息
        mBleScanPresenter.prepare(names, mac, fuzzy, needConnect, timeOut, imp);

        // 启用扫描
        // 配置扫描设置，包括回调类型和扫描模式
        //            ScanSettings settings = new ScanSettings.Builder().setCallbackType(
        //                    ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        //                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        //                    .build();
        //
        //            // 配置扫描过滤器
        //            List<ScanFilter> filters = new ArrayList<ScanFilter>();
        //            if (serviceUuids != null && serviceUuids.length > 0) {
        //                // 创建一个扫描过滤器，仅匹配第一个UUID
        //                ScanFilter filter =
        //                        new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUuids[0]))
        //                                .build();
        //                filters.add(filter);
        //            }
        //
        //            // 启动扫描
        //            scanner.startScan(filters, settings, scanCallback);
        boolean success = BleManager.getInstance().getBluetoothAdapter().startLeScan(serviceUuids, mBleScanPresenter);

        // 蓝牙扫描状态
        mBleScanState = success ? BleScanState.STATE_SCANNING : BleScanState.STATE_IDLE;

        mBleScanPresenter.notifyScanStarted(success);
    }

    /**
     * 取消扫描
     */
    public synchronized void stopLeScan() {
        BleManager.getInstance().getBluetoothAdapter().stopLeScan(mBleScanPresenter);
        mBleScanState = BleScanState.STATE_IDLE;
        mBleScanPresenter.notifyScanStopped();
    }

    public BleScanState getScanState() {
        return mBleScanState;
    }


}
