package com.tn.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.blankj.utilcode.util.ToastUtils
import com.dq.mylibrary.dqLog
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ToastUtils.showLong("aaaa")
    }

    fun main() = runBlocking {
        // 创建 3 个 Flow 生产数据
        val firstFlow = flowOf(1, 2)
        val secondFlow = flow {
            emit(3)
            emit(4)
        }

        val thirdFlow = listOf(5, 6).asFlow()

        //挨个收集 消费者
        firstFlow.collect {
            dqLog("firstFlow: $it")
        }
        secondFlow.collect {
            dqLog("secondFlow: $it")
        }
        thirdFlow.collect {
            dqLog("thirdFlow: $it")
        }

        firstFlow.map {
            it+2
        }.collect{
            dqLog("firstFlow: $it")
        }


    }
}