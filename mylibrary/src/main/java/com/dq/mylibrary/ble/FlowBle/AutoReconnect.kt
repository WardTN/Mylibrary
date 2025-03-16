package com.dq.mylibrary.ble.FlowBle

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach

class AutoReconnect(
    private val connector: BleConnector,
    private val maxRetries: Int = 3
) {

    private var retryCount = 0

    fun enable() {
        connector.connectionEvents.onEach { state ->
            when(state){
                is ConnectionState.Error  -> {
                    if (retryCount<maxRetries){
                        delay(2000)
                        connector.connect(state.device)
                        retryCount++
                    }
                }

                is ConnectionState.Connected -> {
                    retryCount = 0
                }
                else -> Unit
            }
        }
    }
}

