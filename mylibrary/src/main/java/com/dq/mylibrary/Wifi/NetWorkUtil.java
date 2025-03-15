package com.dq.mylibrary.Wifi;

import static android.content.Context.TELEPHONY_SERVICE;
import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;

public class NetWorkUtil {
    static ConnectivityManager connectivityManager;

    public static void forceSendRequestByMobileData(Context context) {
        //判断移动网络是否可用
        if (getDataEnabled(context)) {
            //WiFi已经链接,判断系统版本，如果在5.0以上，则可以强制使用移动网络
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //系统版本大于或者等于5.0
                connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                builder.addCapability(NET_CAPABILITY_INTERNET);
                //强制使用蜂窝数据网络-移动数据
                builder.addTransportType(TRANSPORT_CELLULAR);
                NetworkRequest build = builder.build();
                connectivityManager.requestNetwork(build, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        try {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                connectivityManager.setProcessDefaultNetwork(network);
                            } else {
                                connectivityManager.bindProcessToNetwork(network);
                            }
//                            Toast.makeText(context, "使用移动网络访问", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e("1111", "ConnectivityManager.NetworkCallback.onAvailable: ", e);
                        }
                    }
                });
            } else {
                //提示用户
                Toast.makeText(context, "请关闭WIFI", Toast.LENGTH_LONG).show();
            }
        } else {
            //移动网络不可用，提示打开
            Toast.makeText(context, "请打开移动网络", Toast.LENGTH_LONG).show();
        }

    }

    public static Network networkHot;
    public static ConnectivityManager.NetworkCallback networkCallback;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void ContentWifiNew(Context context, String name, String pwd) {
        NetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(name)
                .setWpa2Passphrase(pwd)
                .build();

        NetworkRequest request;
        request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)//网络不受限
                .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)//信任网络
                .setNetworkSpecifier(specifier)
                .build();
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // do success processing here..
                connectivityManager.bindProcessToNetwork(network);
                networkHot = network;
//                Log.e("1111", "连接成功");
                Log.e("CHEN", "Wifi 连接成功");
//                EventBus.getDefault().post(new notifyCon(true));
            }

            @Override
            public void onUnavailable() {
                // do failure processing here..
//                Log.e("1111", "连接失/**/败");
                Log.e("CHEN", "Wifi 连接失败");
//                EventBus.getDefault().post(new notifyCon(false));
            }
        };
        connectivityManager.registerNetworkCallback(request, networkCallback);
        connectivityManager.requestNetwork(request, networkCallback);
    }


    public static void UbindWifi() {
//        connectivityManager.bindProcessToNetwork(null);
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                connectivityManager.setProcessDefaultNetwork(null);
            } else {
                connectivityManager.bindProcessToNetwork(null);
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (IllegalStateException e) {
            Log.e("1111", "ConnectivityManager.NetworkCallback.onAvailable: ", e);
        } catch (NullPointerException e) {
            Log.e("1111", "ConnectivityManager.NetworkCallback.onAvailable: ", e);
        }
    }

    public static void bindWifi() {
        if (networkHot == null || connectivityManager == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                connectivityManager.setProcessDefaultNetwork(networkHot);
            } else {
                connectivityManager.bindProcessToNetwork(networkHot);
            }
        } catch (Exception e) {
            Log.e("1111", "ConnectivityManager.NetworkCallback.onAvailable: ", e);
        }
    }

    /**
     * 判断移动数据是否打开
     *
     * @return {@code true}: 是<br>{@code false}: 否
     */
    public static boolean getDataEnabled(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
            Method getMobileDataEnabledMethod = tm.getClass().getDeclaredMethod("getDataEnabled");
            if (null != getMobileDataEnabledMethod) {
                return (boolean) getMobileDataEnabledMethod.invoke(tm);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 检测网络是否连接
     *
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        // 得到网络连接信息
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // 去进行判断网络是否连接
        if (manager.getActiveNetworkInfo() != null) {
            return manager.getActiveNetworkInfo().isAvailable();
        }
        return false;
    }

    /**
     * 判断是否包含SIM卡
     *
     * @return 状态
     */
    public static boolean hasSimCard(Context context) {
        TelephonyManager telMgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        boolean result = true;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                result = false; // 没有SIM卡
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                result = false;
                break;
        }
        Log.e("1111", result ? "有SIM卡" : "无SIM卡");
        return result;
    }

    /**
     * 检查wifi是否可用
     */
    public static boolean checkWifiIsEnable(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return null != wifiManager && wifiManager.isWifiEnabled();
    }
}
