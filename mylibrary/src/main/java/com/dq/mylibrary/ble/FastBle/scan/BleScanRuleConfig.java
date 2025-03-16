package com.dq.mylibrary.ble.FastBle.scan;


import com.dq.mylibrary.ble.FastBle.BleManager;

import java.util.UUID;

/**
 * 扫描蓝牙设备规则
 */
public class BleScanRuleConfig {
    // 只扫描指定的服务的设备
    private UUID[] mServiceUuids = null;
    // 只扫描指定广播名的设备，可选
    private String[] mDeviceNames = null;
    // 只扫描指定mac的设备，可选
    private String mDeviceMac = null;
    // 连接时的autoConnect参数，可选，默认false
    private boolean mAutoConnect = false;

    private boolean mFuzzy = false;

    // 扫描超时时间，可选，默认10秒
    private long mScanTimeOut = BleManager.DEFAULT_SCAN_TIME;

    public UUID[] getServiceUuids() {
        return mServiceUuids;
    }

    public String[] getDeviceNames() {
        return mDeviceNames;
    }

    public String getDeviceMac() {
        return mDeviceMac;
    }

    public boolean isAutoConnect() {
        return mAutoConnect;
    }

    public boolean isFuzzy() {
        return mFuzzy;
    }

    public long getScanTimeOut() {
        return mScanTimeOut;
    }

    public static class Builder {

        private UUID[] mServiceUuids = null;
        private String[] mDeviceNames = null;
        private String mDeviceMac = null;
        private boolean mAutoConnect = false;
        private boolean mFuzzy = false;
        private long mTimeOut = BleManager.DEFAULT_SCAN_TIME;

        public Builder setServiceUuids(UUID[] uuids) {
            this.mServiceUuids = uuids;
            return this;
        }

        public Builder setDeviceName(boolean fuzzy, String... name) {
            this.mFuzzy = fuzzy;
            this.mDeviceNames = name;
            return this;
        }

        public Builder setDeviceMac(String mac) {
            this.mDeviceMac = mac;
            return this;
        }

        public Builder setAutoConnect(boolean autoConnect) {
            this.mAutoConnect = autoConnect;
            return this;
        }

        public Builder setScanTimeOut(long timeOut) {
            this.mTimeOut = timeOut;
            return this;
        }

        void applyConfig(BleScanRuleConfig config) {
            config.mServiceUuids = this.mServiceUuids;
            config.mDeviceNames = this.mDeviceNames;
            config.mDeviceMac = this.mDeviceMac;
            config.mAutoConnect = this.mAutoConnect;
            config.mFuzzy = this.mFuzzy;
            config.mScanTimeOut = this.mTimeOut;
        }

        public BleScanRuleConfig build() {
            BleScanRuleConfig config = new BleScanRuleConfig();
            applyConfig(config);
            return config;
        }

    }


}
