package com.dq.mylibrary.ble.FastBle.bluetooth;


import android.bluetooth.BluetoothDevice;
import android.os.Build;

import com.dq.mylibrary.ble.FastBle.BleManager;
import com.dq.mylibrary.ble.FastBle.data.BleDevice;
import com.dq.mylibrary.ble.FastBle.utils.BleLruHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 多蓝牙控制器类，用于管理多个蓝牙设备的连接和断开
 */
public class MultipleBluetoothController {

    // 使用Lru缓存策略的HashMap来存储蓝牙设备，限制最大连接数
    private final BleLruHashMap<String, BleBluetooth> bleLruHashMap;

    // 临时存储正在连接的蓝牙设备
    private final HashMap<String, BleBluetooth> bleTempHashMap;

    /**
     * 构造方法，初始化蓝牙设备缓存和临时缓存
     */
    public MultipleBluetoothController() {
        // 初始化Lru缓存，最大连接数由BleManager的实例决定
        bleLruHashMap = new BleLruHashMap<>(BleManager.getInstance().getMaxConnectCount());
        // 初始化临时缓存
        bleTempHashMap = new HashMap<>();
    }

    /**
     * 创建并返回一个正在连接的蓝牙设备实例
     *
     * @param bleDevice 蓝牙设备对象
     * @return 正在连接的蓝牙设备实例
     */
    public synchronized BleBluetooth buildConnectingBle(BleDevice bleDevice) {

        // 创建一个新的蓝牙设备实例
        BleBluetooth bleBluetooth = new BleBluetooth(bleDevice);

        // 如果临时缓存中不存在该设备，则添加到临时缓存
        if (!bleTempHashMap.containsKey(bleBluetooth.getDeviceKey())) {
            bleTempHashMap.put(bleBluetooth.getDeviceKey(), bleBluetooth);
        }
        return bleBluetooth;
    }

    /**
     * 从临时缓存中移除正在连接的蓝牙设备
     *
     * @param bleBluetooth 蓝牙设备实例
     */
    public synchronized void removeConnectingBle(BleBluetooth bleBluetooth) {
        // 如果设备为空，则直接返回
        if (bleBluetooth == null) {
            return;
        }
        // 如果临时缓存中存在该设备，则移除
        if (bleTempHashMap.containsKey(bleBluetooth.getDeviceKey())) {
            bleTempHashMap.remove(bleBluetooth.getDeviceKey());
        }
    }

    /**
     * 将蓝牙设备添加到Lru缓存中
     *
     * @param bleBluetooth 蓝牙设备实例
     */
    public synchronized void addBleBluetooth(BleBluetooth bleBluetooth) {
        // 如果设备为空，则直接返回
        if (bleBluetooth == null) {
            return;
        }
        // 如果Lru缓存中不存在该设备，则添加
        if (!bleLruHashMap.containsKey(bleBluetooth.getDeviceKey())) {
            bleLruHashMap.put(bleBluetooth.getDeviceKey(), bleBluetooth);
        }
    }

    /**
     * 从Lru缓存中移除蓝牙设备
     *
     * @param bleBluetooth 蓝牙设备实例
     */
    public synchronized void removeBleBluetooth(BleBluetooth bleBluetooth) {
        // 如果设备为空，则直接返回
        if (bleBluetooth == null) {
            return;
        }
        // 如果Lru缓存中存在该设备，则移除
        if (bleLruHashMap.containsKey(bleBluetooth.getDeviceKey())) {
            bleLruHashMap.remove(bleBluetooth.getDeviceKey());
        }
    }

    /**
     * 检查Lru缓存中是否包含指定的蓝牙设备
     *
     * @param bleDevice 蓝牙设备对象
     * @return true表示包含，false表示不包含
     */
    public synchronized boolean isContainDevice(BleDevice bleDevice) {
        // 检查设备是否为空，并判断Lru缓存中是否包含该设备的键
        return bleDevice != null && bleLruHashMap.containsKey(bleDevice.getKey());
    }

    /**
     * 检查Lru缓存中是否包含指定的蓝牙设备
     *
     * @param bluetoothDevice BluetoothDevice对象
     * @return true表示包含，false表示不包含
     */
    public synchronized boolean isContainDevice(BluetoothDevice bluetoothDevice) {
        // 检查设备是否为空，并判断Lru缓存中是否包含拼接的设备键
        return bluetoothDevice != null && bleLruHashMap.containsKey(bluetoothDevice.getName() + bluetoothDevice.getAddress());
    }

    /**
     * 获取Lru缓存中的蓝牙设备实例
     *
     * @param bleDevice 蓝牙设备对象
     * @return 蓝牙设备实例，如果不存在则返回null
     */
    public synchronized BleBluetooth getBleBluetooth(BleDevice bleDevice) {
        // 检查设备是否为空，并从Lru缓存中获取对应的蓝牙设备实例
        if (bleDevice != null) {
            if (bleLruHashMap.containsKey(bleDevice.getKey())) {
                return bleLruHashMap.get(bleDevice.getKey());
            }
        }
        return null;
    }

    /**
     * 断开指定蓝牙设备的连接
     *
     * @param bleDevice 蓝牙设备对象
     */
    public synchronized void disconnect(BleDevice bleDevice) {
        // 如果设备列表中包含该设备，则断开其连接
        if (isContainDevice(bleDevice)) {
            getBleBluetooth(bleDevice).disconnect();
        }
    }

    /**
     * 断开所有蓝牙设备的连接，并清空Lru缓存
     */
    public synchronized void disconnectAllDevice() {
        // 遍历Lru缓存，断开所有设备的连接
        for (Map.Entry<String, BleBluetooth> stringBleBluetoothEntry : bleLruHashMap.entrySet()) {
            stringBleBluetoothEntry.getValue().disconnect();
        }
        // 清空Lru缓存
        bleLruHashMap.clear();
    }

    /**
     * 销毁所有蓝牙设备实例，并清空所有缓存
     */
    public synchronized void destroy() {
        // 销毁Lru缓存中的所有蓝牙设备实例，并清空缓存
        for (Map.Entry<String, BleBluetooth> stringBleBluetoothEntry : bleLruHashMap.entrySet()) {
            stringBleBluetoothEntry.getValue().destroy();
        }
        bleLruHashMap.clear();
        // 销毁临时缓存中的所有蓝牙设备实例，并清空缓存
        for (Map.Entry<String, BleBluetooth> stringBleBluetoothEntry : bleTempHashMap.entrySet()) {
            stringBleBluetoothEntry.getValue().destroy();
        }
        bleTempHashMap.clear();
    }

    /**
     * 获取蓝牙设备实例列表
     *
     * @return 蓝牙设备实例列表
     */
    public synchronized List<BleBluetooth> getBleBluetoothList() {
        // 获取Lru缓存中的所有蓝牙设备实例，并按设备键排序
        List<BleBluetooth> bleBluetoothList = new ArrayList<>(bleLruHashMap.values());
        Collections.sort(bleBluetoothList, new Comparator<BleBluetooth>() {
            @Override
            public int compare(BleBluetooth lhs, BleBluetooth rhs) {
                return lhs.getDeviceKey().compareToIgnoreCase(rhs.getDeviceKey());
            }
        });
        return bleBluetoothList;
    }

    /**
     * 获取蓝牙设备对象列表
     *
     * @return 蓝牙设备对象列表
     */
    public synchronized List<BleDevice> getDeviceList() {
        // 刷新连接设备，确保设备列表是最新的
        refreshConnectedDevice();
        // 从蓝牙设备实例列表中提取蓝牙设备对象
        List<BleDevice> deviceList = new ArrayList<>();
        for (BleBluetooth BleBluetooth : getBleBluetoothList()) {
            if (BleBluetooth != null) {
                deviceList.add(BleBluetooth.getDevice());
            }
        }
        return deviceList;
    }

    /**
     * 刷新连接的蓝牙设备列表，移除已断开连接的设备
     */
    public void refreshConnectedDevice() {
        // 遍历蓝牙设备列表，检查每个设备的连接状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            List<BleBluetooth> bluetoothList = getBleBluetoothList();
            for (int i = 0; bluetoothList != null && i < bluetoothList.size(); i++) {
                BleBluetooth bleBluetooth = bluetoothList.get(i);
                // 如果设备未连接，则从Lru缓存中移除
                if (!BleManager.getInstance().isConnected(bleBluetooth.getDevice())) {
                    removeBleBluetooth(bleBluetooth);
                }
            }
        }
    }
}

