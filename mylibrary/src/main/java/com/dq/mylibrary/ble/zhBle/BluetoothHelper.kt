package com.dq.mylibrary.ble.zhBle

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Process
import androidx.core.location.LocationManagerCompat
import com.dq.mylibrary.dqLog
import java.lang.IllegalArgumentException

/**
 * BluetoothHelper 是一个用于管理蓝牙状态和操作的辅助对象
 */
internal object BluetoothHelper {

    // 日志标签，用于在日志输出时标识来源
    var logTag = "BluetoothHelper"

    // 蓝牙打开时的回调函数
    private lateinit var turnOn: () -> Unit

    // 蓝牙关闭时的回调函数
    private lateinit var turnOff: () -> Unit

    // BluetoothReceiver 用于监听蓝牙状态的变化
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 当蓝牙状态发生变化时，处理相应的状态
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        dqLog("$logTag --> 蓝牙正在打开...")
                    }

                    BluetoothAdapter.STATE_ON -> {
                        dqLog("$logTag --> 蓝牙已经打开。")
                        turnOn()
                    }

                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        dqLog("$logTag --> 蓝牙正在关闭...")
                        turnOff()
                    }

                    BluetoothAdapter.STATE_OFF -> {
                        dqLog("$logTag --> 蓝牙已经关闭。")
                    }
                }
            }
        }
    }

    // 根据Android版本选择需要的权限数组
    private val permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
    }

    /**
     * 检查蓝牙状态
     * @param context 上下文，用于检查权限和获取系统服务
     * @param bluetoothAdapter 蓝牙适配器，用于检查蓝牙状态
     * @param requestPermission 是否请求权限
     * @return 蓝牙状态
     */
    fun checkState(
        context: Context, bluetoothAdapter: BluetoothAdapter?,
        requestPermission: Boolean
    ): ClientState {
        // 根据不同的条件判断蓝牙状态
        val state = when {
            bluetoothAdapter == null -> {
                ClientState.NOT_SUPPORT
            }

            !checkPermissions(context, requestPermission) -> {
                ClientState.NO_PERMISSIONS
            }
            // Android12之后就不需要定位权限了
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !LocationManagerCompat.isLocationEnabled(
                context
                    .getSystemService(Context.LOCATION_SERVICE) as LocationManager
            ) -> {
                ClientState.LOCATION_DISABLE
            }

            bluetoothAdapter.isEnabled -> {
                ClientState.ENABLE
            }

            else -> {
                ClientState.DISABLE
            }
        }
        return state
    }

    /**
     * 开启或关闭蓝牙
     * @param context 上下文，用于检查权限
     * @param bluetoothAdapter 蓝牙适配器，用于开启或关闭蓝牙
     * @param enable 是否开启蓝牙
     * @param checkPermission 是否检查权限
     * @return 操作是否成功
     */
    @SuppressLint("MissingPermission")
    fun switchBluetooth(
        context: Context, bluetoothAdapter: BluetoothAdapter, enable: Boolean,
        checkPermission: Boolean = false
    ): Boolean {
        // 检查是否需要的权限已经授予
        if (checkPermission && !checkPermissions(context)) {
            return false
        }
        // Android13及以上不允许App启用/关闭蓝牙
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (enable) {
                dqLog("$logTag --> 请求开启蓝牙")
                @Suppress("DEPRECATION")
                return bluetoothAdapter.enable()
            } else {
                dqLog("$logTag --> 请求关闭蓝牙")
                @Suppress("DEPRECATION")
                return bluetoothAdapter.disable()
            }
        }
        return false
    }

    /**
     * 注册蓝牙状态变化的接收器
     * @param context 上下文，用于注册接收器
     * @param turnOn 蓝牙打开时的回调函数
     * @param turnOff 蓝牙关闭时的回调函数
     */
    fun registerSwitchReceiver(context: Context, turnOn: () -> Unit, turnOff: () -> Unit) {
        this.turnOn = turnOn
        this.turnOff = turnOff
        context.registerReceiver(
            bluetoothReceiver, IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED
            )
        )
    }

    /**
     * 注销蓝牙状态变化的接收器
     * @param context 上下文，用于注销接收器
     */
    fun unregisterSwitchReceiver(context: Context) {
        context.unregisterReceiver(bluetoothReceiver)
    }

    /**
     * 检查所需的权限是否已经授予
     * @param context 上下文，用于检查权限
     * @param request 是否请求权限
     * @return 是否已经授予所有权限
     */
    private fun checkPermissions(context: Context, request: Boolean = true): Boolean {
        // 遍历权限数组，检查每个权限是否已经授予
        for (permission in permissions) {
            val grant = context.checkPermission(permission, Process.myPid(), Process.myUid())
            if (grant != PackageManager.PERMISSION_GRANTED) {
                dqLog("$logTag --> 缺少权限$permission, 尝试申请...")
                if (request && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(context)
                }
                return false
            }
        }
        return true
    }

    /**
     * 请求权限
     * @param context 上下文，用于请求权限
     */
    fun requestPermissions(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context as Activity).requestPermissions(permissions, 1200)
        }
    }

    /**
     * 检查MTU值是否在有效范围内
     * @param mtu MTU值
     * @throws IllegalArgumentException 如果MTU值不在有效范围内
     */
    fun checkMtuRange(mtu: Int) {
        if (mtu < 23 || mtu > 512) {
            throw IllegalArgumentException("The mtu value must be in the 23..512 range")
        }
    }
}
