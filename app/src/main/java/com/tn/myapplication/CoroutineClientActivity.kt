package com.tn.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.dq.mylibrary.ble.zhBle.ClientState
import com.dq.mylibrary.ble.zhBle.ClientType
import com.dq.mylibrary.ble.zhBle.ConnectionState
import com.dq.mylibrary.ble.zhBle.CoroutineClient
import com.dq.mylibrary.ble.zhBle.Device
import com.dq.mylibrary.dqLog
import com.tn.myapplication.theme.BluetoothClientTheme
import kotlinx.coroutines.launch
import java.util.UUID

class CoroutineClientActivity : ComponentActivity() {
    private val serviceUid: UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1910")
    private val receiveUid: UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b10")
    private val sendUid: UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b10")

    private var bluetoothType by mutableStateOf(ClientType.BLE)
    private lateinit var bluetoothClient: CoroutineClient
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var discoverDeviceDialog: DiscoverDeviceDialog
    private var deviceName by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog = LoadingDialog(this, false)
        discoverDeviceDialog =
            DiscoverDeviceDialog(this, ::startScanDevice, ::stopScanDevice, ::connectDevice)
        setContent {
            BluetoothClientTheme {
                Content()
            }
        }
    }

    @Composable
    private fun Content() {
        Scaffold(Modifier.fillMaxSize(), topBar = {
            Surface(shadowElevation = 2.5.dp) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)) {
                    Text(
                        "BluetoothClient", Modifier.align(Alignment.Center), fontSize = 18.sp,
                        fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                    )
                }
            }
        }) { paddingValues ->
            Column(
                Modifier.padding(
                    start = 14.dp,
                    top = paddingValues.calculateTopPadding(),
                    end = 14.dp
                )
            ) {
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .height(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var expanded by remember {
                        mutableStateOf(false)
                    }
                    Text(text = "蓝牙类型：$bluetoothType",
                        Modifier
                            .clickable {
                                expanded = true
                            }
                            .padding(8.dp), fontSize = 16.sp, textAlign = TextAlign.Center)
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ClientType.entries.forEach { clientType ->
                            DropdownMenuItem(text = {
                                Text(text = "$clientType", fontSize = 16.sp)
                            }, onClick = {
                                if (bluetoothType != clientType) {
                                    bluetoothClient.release()
                                    bluetoothType = clientType
                                }
                                expanded = false
                            })
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (deviceName.isEmpty()) {
                        Button(onClick = {
                            when (bluetoothClient.checkState()) {
                                ClientState.NOT_SUPPORT -> {
                                    showToast("当前设备不支持蓝牙功能！")
                                }

                                ClientState.DISABLE -> {
                                    showToast("请先开启蓝牙！")
                                }

                                ClientState.ENABLE -> {
                                    discoverDeviceDialog.show()
                                }

                                else -> {}
                            }
                        }) {
                            Text(text = "扫描设备", fontSize = 16.sp)
                        }
                    } else {
                        Button(onClick = {
                            bluetoothClient.disconnect()
                        }) {
                            Text(text = "断开设备", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = deviceName)
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (deviceName.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .wrapContentHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var sendText by remember { mutableStateOf("") }
                        var sendError by remember { mutableStateOf(false) }
                        TextField(sendText, onValueChange = { inputText ->
                            sendText = inputText
                            sendError = false
                        },
                            Modifier
                                .fillMaxWidth()
                                .weight(1f), singleLine = true, label = {
                            Text(text = "数据格式：任意字符")
                        }, isError = sendError)
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = {
                            if (sendText.isNotEmpty()) {
                                sendData(sendText.toByteArray())
                            } else {
                                sendError = true
                            }

                        }) {
                            Text(text = "发送")
                        }
                    }
                }
            }
        }
        LaunchedEffect(bluetoothType) {
            bluetoothClient = CoroutineClient(
                this@CoroutineClientActivity, bluetoothType,
                serviceUid
            )
            bluetoothClient.setSwitchReceive(turnOn = {
                discoverDeviceDialog.show()
            }, turnOff = {
                discoverDeviceDialog.stop()
            })
        }
    }

    private fun startScanDevice() {
        lifecycleScope.launch {
            bluetoothClient.startScan(30000).collect { device ->
                discoverDeviceDialog.add(device)
            }
            discoverDeviceDialog.stop()
        }
    }

    // 停止扫描任务
    private fun stopScanDevice() {
        bluetoothClient.stopScan()
    }

    // 选中连接设备
    private fun connectDevice(device: Device) {
        stopScanDevice()
        lifecycleScope.launch {
            bluetoothClient.connect(device, 85, 15000, 3).collect { connectionState ->
                dqLog("CoroutineClientActivity --> connectionState: $connectionState")
                if (connectionState == ConnectionState.CONNECTING) {
                    loadingDialog.show()
                } else {
                    loadingDialog.hide()
                }
                if (connectionState == ConnectionState.CONNECTED) {
                    deviceName = device.name ?: device.address
                    receiveData()
                    discoverDeviceDialog.dismiss()
                    showToast("连接成功！")
                } else if (connectionState == ConnectionState.DISCONNECTED) {
                    deviceName = ""
                } else if (connectionState == ConnectionState.CONNECT_ERROR) {
                    showToast("连接异常！")
                } else if (connectionState == ConnectionState.CONNECT_TIMEOUT) {
                    showToast("连接超时！")
                }
            }
        }
    }

    private fun receiveData() {
        lifecycleScope.launch {
            bluetoothClient.receiveData(receiveUid).collect { data ->
                dqLog("receiveData: ${data.toHex()}")
            }
        }
    }

    private fun sendData(data: ByteArray) {
        lifecycleScope.launch {
            val success = bluetoothClient.sendData(sendUid, data)
            if (success) {
                showToast("数据发送成功！")
            } else {
                showToast("数据发送失败！")
            }
        }
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothClient.release()
    }

    /**
     * 将字节数组转换为十六进制字符串表示形式。
     *
     * 此函数使用实验性的无符号类型，因此需要使用@OptIn注解来启用实验性功能。
     *
     * @return 十六进制字符串，每个字节由两个十六进制数字表示。
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun ByteArray.toHex(): String =
        asUByteArray().joinToString("") { it.toString(radix = 16).padStart(2, '0') }

}