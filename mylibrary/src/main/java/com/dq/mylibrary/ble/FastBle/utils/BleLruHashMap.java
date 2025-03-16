package com.dq.mylibrary.ble.FastBle.utils;


import com.dq.mylibrary.ble.FastBle.bluetooth.BleBluetooth;

import java.util.LinkedHashMap;

/**
 * BleLruHashMap 是一个自定义的 LRU（最近最少使用）缓存实现，继承自 LinkedHashMap
 * 它用于存储键值对，并在缓存大小超过预定义的最大值时自动移除最近最少使用的项
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public class BleLruHashMap<K, V> extends LinkedHashMap<K, V> {

    /**
     * 最大缓存大小，当缓存中的项数超过这个值时，最近最少使用的项将被移除
     */
    private final int MAX_SIZE;

    /**
     * 构造一个 BleLruHashMap 实例，指定缓存的最大大小
     *
     * @param saveSize 缓存的最大大小
     */
    public BleLruHashMap(int saveSize) {
        // 调用父类构造方法，初始化容量为 saveSize/0.75+1（向上取整），负载因子为0.75，访问顺序为true
        super((int) Math.ceil(saveSize / 0.75) + 1, 0.75f, true);
        MAX_SIZE = saveSize;
    }

    /**
     * 移除最老的条目吗？当缓存大小超过最大值时，考虑移除最老的条目
     *
     * @param eldest 最老的条目
     * @return 如果移除了条目，则返回 true；否则返回 false
     */
    @Override
    protected boolean removeEldestEntry(Entry eldest) {
        // 当缓存大小超过最大值且最老的条目的值是 BleBluetooth 实例时，断开其连接
        if (size() > MAX_SIZE && eldest.getValue() instanceof BleBluetooth) {
            ((BleBluetooth) eldest.getValue()).disconnect();
        }
        // 当缓存大小超过最大值时，返回 true 表示移除最老的条目，否则返回 false
        return size() > MAX_SIZE;
    }

    /**
     * 返回一个描述缓存中所有键值对的字符串
     *
     * @return 描述缓存中所有键值对的字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // 遍历缓存中的所有条目，并将其格式化为字符串追加到 StringBuilder 中
        for (Entry<K, V> entry : entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

}
