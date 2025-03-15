package com.dq.mylibrary.mvi.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.dq.mylibrary.R
import com.dq.mylibrary.databinding.ActivityMviBinding
import com.dq.mylibrary.mvi.data.intent.MainIntent
import com.dq.mylibrary.mvi.data.state.MainState
import com.dq.mylibrary.mvi.network.NetworkUtils
import com.dq.mylibrary.mvi.ui.adapter.WallpaperAdapter
import com.dq.mylibrary.mvi.ui.viewmodel.MainViewModel
import com.dq.mylibrary.mvi.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class MVIActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMviBinding

    private lateinit var mainViewModel: MainViewModel

    private var wallPaperAdapter = WallpaperAdapter(arrayListOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //使用ViewBinding
        binding = ActivityMviBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //绑定ViewModel
        mainViewModel = ViewModelProvider(this, ViewModelFactory(NetworkUtils.apiService))[MainViewModel::class.java]
        //初始化
        initView()
        //观察ViewModel
        observeViewModel()
    }

    /**
     * 初始化
     */
    private fun initView() {
        //RV配置
        binding.rvWallpaper.apply {
            layoutManager = GridLayoutManager(this@MVIActivity, 2)
            adapter  = wallPaperAdapter
        }
        //按钮点击
        binding.btnGetWallpaper.setOnClickListener {
            lifecycleScope.launch{
                //发送意图
                mainViewModel.mainIntentChannel.send(MainIntent.GetWallpaper)
            }
        }
    }


    /**
     * 观察ViewModel
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            //状态收集
            mainViewModel.state.collect {
                when(it) {
                    is MainState.Idle -> {

                    }
                    is MainState.Loading -> {
                        binding.btnGetWallpaper.visibility = View.GONE
                        binding.pbLoading.visibility = View.VISIBLE
                    }
                    is MainState.Wallpapers -> {     //数据返回
                        binding.btnGetWallpaper.visibility = View.GONE
                        binding.pbLoading.visibility = View.GONE

                        binding.rvWallpaper.visibility = View.VISIBLE
                        it.wallpaper.let { paper ->
                            wallPaperAdapter.addData(paper.res.vertical)
                        }
                        wallPaperAdapter.notifyDataSetChanged()
                    }
                    is MainState.Error -> {
                        binding.pbLoading.visibility = View.GONE
                        binding.btnGetWallpaper.visibility = View.VISIBLE
                        Log.d("TAG", "observeViewModel: $it.error")
                        Toast.makeText(this@MVIActivity, it.error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}