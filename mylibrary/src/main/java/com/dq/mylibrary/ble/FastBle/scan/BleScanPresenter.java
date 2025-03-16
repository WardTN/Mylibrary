package com.dq.mylibrary.ble.FastBle.scan;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.dq.mylibrary.ble.FastBle.callback.BleScanPresenterImp;
import com.dq.mylibrary.ble.FastBle.data.BleDevice;
import com.dq.mylibrary.ble.FastBle.data.BleMsg;
import com.dq.mylibrary.ble.FastBle.utils.BleLog;
import com.dq.mylibrary.ble.FastBle.utils.HexUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抽象类BleScanPresenter，实现BluetoothAdapter.LeScanCallback接口
 * LeScanCallback 当应用通过 BluetoothAdapter.startLeScan() 方法启动 BLE 扫描时，
 * 系统会周期性地调用 LeScanCallback 的 onLeScan 方法，将扫描到的蓝牙设备信息传递给应用。
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleScanPresenter implements BluetoothAdapter.LeScanCallback {

    // 存储目标蓝牙设备的名称数组
    private String[] mDeviceNames;
    // 存储目标蓝牙设备的MAC地址
    private String mDeviceMac;
    // 表示是否进行模糊匹配
    private boolean mFuzzy;
    // 表示是否需要连接设备
    private boolean mNeedConnect;
    // 扫描蓝牙设备的超时时间
    private long mScanTimeout;
    // BleScanPresenter的实现类对象
    private BleScanPresenterImp mBleScanPresenterImp;

    // 蓝牙设备列表
    private final List<BleDevice> mBleDeviceList = new ArrayList<>();

    // 主线程的Handler，用于在主线程中处理消息
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    // 处理扫描蓝牙设备消息的线程
    private HandlerThread mHandlerThread;
    // 扫描蓝牙设备的Handler
    private Handler mHandler;
    // 表示是否正在处理蓝牙设备扫描结果
    private boolean mHandling;

    // 内部静态类ScanHandler，用于处理扫描蓝牙设备的消息
    private static final class ScanHandler extends Handler {

        // 弱引用指向BleScanPresenter对象
        private final WeakReference<BleScanPresenter> mBleScanPresenter;

        // ScanHandler构造函数
        ScanHandler(Looper looper, BleScanPresenter bleScanPresenter) {
            super(looper);
            mBleScanPresenter = new WeakReference<>(bleScanPresenter);
        }

        // 处理消息的方法
        @Override
        public void handleMessage(Message msg) {
            BleScanPresenter bleScanPresenter = mBleScanPresenter.get();
            if (bleScanPresenter != null) {
                if (msg.what == BleMsg.MSG_SCAN_DEVICE) {
                    final BleDevice bleDevice = (BleDevice) msg.obj;
                    // 回调 蓝牙设备信息
                    if (bleDevice != null) {
                        bleScanPresenter.handleResult(bleDevice);
                    }
                }
            }
        }
    }

    // 处理扫描结果的方法
    private void handleResult(final BleDevice bleDevice) {
        mMainHandler.post(() -> onLeScan(bleDevice));
        checkDevice(bleDevice);
    }

    /**
     * 填充配置信息
     *
     * @param names       目标蓝牙设备名称数组
     * @param mac         目标蓝牙设备MAC地址
     * @param fuzzy       是否进行模糊匹配
     * @param needConnect 是否需要连接设备
     * @param timeOut     扫描蓝牙设备的超时时间
     * @param bleScanPresenterImp BleScanPresenter的实现类对象
     */
    public void prepare(String[] names, String mac, boolean fuzzy, boolean needConnect,
                        long timeOut, BleScanPresenterImp bleScanPresenterImp) {
        mDeviceNames = names;
        mDeviceMac = mac;
        mFuzzy = fuzzy;
        mNeedConnect = needConnect;
        mScanTimeout = timeOut;
        mBleScanPresenterImp = bleScanPresenterImp;

        mHandlerThread = new HandlerThread(BleScanPresenter.class.getSimpleName());
        mHandlerThread.start();
        mHandler = new ScanHandler(mHandlerThread.getLooper(), this);
        mHandling = true;
    }

    // 获取是否需要连接设备的值
    public boolean ismNeedConnect() {
        return mNeedConnect;
    }

    // 获取BleScanPresenter的实现类对象
    public BleScanPresenterImp getBleScanPresenterImp() {
        return mBleScanPresenterImp;
    }

    /**
     * 扫描结果回调
     *
     * @param device 蓝牙设备对象
     * @param rssi   信号强度
     * @param scanRecord 设备信息
     */
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;

        if (!mHandling)
            return;

        Message message = mHandler.obtainMessage();
        message.what = BleMsg.MSG_SCAN_DEVICE;
        message.obj = new BleDevice(device, rssi, scanRecord, System.currentTimeMillis());
        mHandler.sendMessage(message);
    }

    // 检查设备是否符合要求
    private void checkDevice(BleDevice bleDevice) {
        if (TextUtils.isEmpty(mDeviceMac) && (mDeviceNames == null || mDeviceNames.length < 1)) {
            correctDeviceAndNextStep(bleDevice);
            return;
        }

        if (!TextUtils.isEmpty(mDeviceMac)) {
            if (!mDeviceMac.equalsIgnoreCase(bleDevice.getMac()))
                return;
        }

        if (mDeviceNames != null && mDeviceNames.length > 0) {
            AtomicBoolean equal = new AtomicBoolean(false);
            for (String name : mDeviceNames) {
                String remoteName = bleDevice.getName();
                if (remoteName == null)
                    remoteName = "";
                if (mFuzzy ? remoteName.contains(name) : remoteName.equals(name)) {
                    equal.set(true);
                }
            }
            if (!equal.get()) {
                return;
            }
        }

        correctDeviceAndNextStep(bleDevice);
    }

    // 处理符合要求的设备
    private void correctDeviceAndNextStep(final BleDevice bleDevice) {

        if (mNeedConnect) {
            BleLog.i("devices detected  ------"
                    + "  name:" + bleDevice.getName()
                    + "  mac:" + bleDevice.getMac()
                    + "  Rssi:" + bleDevice.getRssi()
                    + "  scanRecord:" + HexUtil.formatHexString(bleDevice.getScanRecord()));

            mBleDeviceList.add(bleDevice);
            mMainHandler.post(() -> BleScanner.getInstance().stopLeScan());

        } else {

            AtomicBoolean hasFound = new AtomicBoolean(false);

            for (BleDevice result : mBleDeviceList) {
                if (result.getDevice().equals(bleDevice.getDevice())) {
                    hasFound.set(true);
                }
            }

            if (!hasFound.get()) {
                BleLog.i("device detected  ------"
                        + "  name: " + bleDevice.getName()
                        + "  mac: " + bleDevice.getMac()
                        + "  Rssi: " + bleDevice.getRssi()
                        + "  scanRecord: " + HexUtil.formatHexString(bleDevice.getScanRecord(), true));

                mBleDeviceList.add(bleDevice);
                mMainHandler.post(() -> onScanning(bleDevice));
            }
        }
    }

    // 通知扫描开始的结果
    public final void notifyScanStarted(final boolean success) {
        mBleDeviceList.clear();

        removeHandlerMsg();

        if (success && mScanTimeout > 0) {
            mMainHandler.postDelayed(() -> BleScanner.getInstance().stopLeScan(), mScanTimeout);
        }

        mMainHandler.post(() -> onScanStarted(success));
    }

    // 通知扫描停止
    public final void notifyScanStopped() {
        mHandling = false;
        mHandlerThread.quit();
        removeHandlerMsg();
        mMainHandler.post(() -> onScanFinished(mBleDeviceList));
    }

    // 移除Handler中的消息
    public final void removeHandlerMsg() {
        mMainHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
    }

    // 抽象方法，子类需要实现
    public abstract void onScanStarted(boolean success);

    // 抽象方法，子类需要实现
    public abstract void onLeScan(BleDevice bleDevice);

    // 抽象方法，子类需要实现
    public abstract void onScanning(BleDevice bleDevice);

    // 抽象方法，子类需要实现
    public abstract void onScanFinished(List<BleDevice> bleDeviceList);
}
