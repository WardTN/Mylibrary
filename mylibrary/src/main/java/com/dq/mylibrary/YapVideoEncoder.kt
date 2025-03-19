package com.dq.mylibrary

import android.graphics.Bitmap
import android.media.*
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class YapVideoEncoder(private val out: File, private val mFrameRate: Int = 15) {

    // 声明一个可变的MediaCodec对象，用于媒体编解码
    private var mediaCodec: MediaCodec? = null

    // 声明一个可变的MediaMuxer对象，用于媒体文件的封装
    private var mediaMuxer: MediaMuxer? = null

    // 标记MediaMuxer是否已经开始工作
    private var mMuxerStarted = false

    // 标记编码过程是否正在运行
    private var isRunning = false

    // 用于跟踪媒体数据的轨道索引
    private var mTrackIndex = 0

    // 存储媒体编解码器的颜色格式
    private var colorFormat = 0

    // 设置媒体编解码器操作的默认超时时间
    private val defaultTimeOutUs = 10000L

    // 生成媒体数据的索引，确保每帧数据的唯一性
    private var generateIndex: Long = 0

    // 创建MediaCodec.BufferInfo对象，用于存储缓冲区信息
    private val info = MediaCodec.BufferInfo()

    // 声明一个ByteBuffer数组，用于存储媒体数据缓冲区
    private var buffers: Array<ByteBuffer?>? = null


    /**
     * 获取支持H.264编码的MediaCodec的颜色格式列表
     *
     * 此属性通过遍历设备上的所有MediaCodec信息，寻找支持H.264编码（MIME类型为"video/avc"）的解码器
     * 并返回其支持的颜色格式列表
     */
    private val mediaCodecList: IntArray
        get() {
            // 获取设备上MediaCodec的总数
            val numCodecs = MediaCodecList.getCodecCount()
            var codecInfo: MediaCodecInfo? = null
            var i = 0
            // 遍历所有MediaCodec，寻找合适的解码器
            while (i < numCodecs && codecInfo == null) {
                // 获取当前索引的MediaCodec信息
                val info = MediaCodecList.getCodecInfoAt(i)
                // 如果当前MediaCodec不是编码器，则跳过
                if (!info.isEncoder) {
                    i++
                    continue
                }
                // 获取当前MediaCodec支持的MIME类型列表
                val types = info.supportedTypes
                var found = false
                // The decoder required by the rotation training
                var j = 0
                // 遍历支持的MIME类型列表，寻找是否支持"video/avc"类型
                while (j < types.size && !found) {
                    if (types[j] == "video/avc") {
                        found = true
                    }
                    j++
                }
                // 如果未找到支持"video/avc"类型的解码器，则继续查找下一个
                if (!found) {
                    i++
                    continue
                }
                // 找到支持"video/avc"类型的解码器，保存其信息
                codecInfo = info
                i++
            }
            // 获取找到的MediaCodec信息所支持的"video/avc"类型的编解码能力
            val capabilities = codecInfo!!.getCapabilitiesForType("video/avc")
            // 返回支持的颜色格式列表
            return capabilities.colorFormats
        }


    /**
     * 初始化视频编码器
     *
     * @param width 视频宽度
     * @param height 视频高度
     *
     * 此函数负责根据给定的宽度和高度初始化视频编码器它选择支持的色彩格式，
     * 调整宽度和高度以确保它们是偶数，创建视频格式，并配置和启动MediaCodec编码器
     * 它还负责创建MediaMuxer实例，用于生成MP4文件
     */
    private fun init(width: Int, height: Int) {
        // 获取支持的色彩格式数组
        val formats: IntArray = mediaCodecList

        // 遍历并打印支持的色彩格式
        for (i in formats) {
            Log.e("CHEN", "当前format 为$i")
        }

        // 寻找并选择合适的色彩格式
        lab@ for (format in formats) {
            when (format) {
                // YUVSP  [YYYY...][UVUV...]
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar -> {
                    colorFormat = format
                    break@lab
                }
                // YUV 420P  [YYYY...][UUUU...][VVVV...]
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> {
                    colorFormat = format
                    break@lab
                }

                // [YYYY...][YUYV...]
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar -> {
                    colorFormat = format
                    break@lab
                }

                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> {
                    colorFormat = format
                    break@lab
                }

                else -> continue@lab
            }
        }

        if (colorFormat <= 0) {
            colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        }

        Log.e("CHEN", "选中的 ColorFormat 为$colorFormat")

        // 确保宽度是偶数
        var widthFix = width
        if (widthFix % 2 != 0) {
            widthFix -= 1
        }
        // 确保高度是偶数
        var heightFix = height
        if (heightFix % 2 != 0) {
            heightFix -= 1
        }
        // 创建视频格式
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, widthFix, heightFix)

        // 设置视频格式的色彩格式
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)

        // 设置比特率
        val bitrate = (widthFix * heightFix * mFrameRate * 1.5).toInt()
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)

        // 设置帧率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate)

        // 设置I帧间隔
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)

        // For planar YUV format, the Y of all pixels is stored consecutively,
        // followed by the U of all pixels, followed by the V of all pixels
        // For the YUV format of packed, the Y,U,
        // and V of each pixel are continuously cross-stored

        try {
            // 创建H264编码器
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)

            // 创建或确认输出文件存在
            if (!out.exists()) {
                out.createNewFile()
            }

            // 创建MediaMuxer实例，用于生成MP4文件
            mediaMuxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 配置MediaCodec编码器
        mediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        // 启动MediaCodec编码器
        mediaCodec!!.start()

        // 设置运行状态为true
        isRunning = true
    }

    fun start(width: Int, height: Int) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            thread(start = true) {
                start(width, height)
            }
            return
        }
        init(width, height)
    }


    fun finish() {
        isRunning = false

        mediaCodec?.let {
            it.stop()
            it.release()
        }

        mediaMuxer?.let {
            try {
                if (mMuxerStarted) {
                    it.stop()
                    it.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 将Bitmap添加到MediaCodec的输入缓冲区
     * 该函数用于将图像数据（Bitmap）转换为NV21格式，并将其放入MediaCodec的输入缓冲区进行编码
     *
     * @param bitmap 要添加到缓冲区的Bitmap对象
     */
    fun addToBuffer(bitmap: Bitmap) {
        // 获取可用的输入缓冲区索引
        val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(defaultTimeOutUs)
        if (inputBufferIndex >= 0) {
            // 计算当前帧的显示时间戳
            val ptsUsec = computePresentationTime(generateIndex)
            var widthFix = bitmap.width
            // 确保宽度为偶数，因为某些编码器要求宽度和高度为偶数
            if (widthFix % 2 != 0) {
                widthFix -= 1
            }
            var heightFix = bitmap.height
            // 确保高度为偶数
            if (heightFix % 2 != 0) {
                heightFix -= 1
            }

            // 将bitmap 转成NV21
            val input = getNV12(widthFix, heightFix, bitmap)

            // Valid empty cache
            val inputBuffer = mediaCodec!!.getInputBuffer(inputBufferIndex)
            // 清空缓冲区并放入转换后的数据
            inputBuffer!!.clear()
            inputBuffer.put(input)

            // 将数据放入编码队列
            mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, input.size, ptsUsec, 0)

            // 处理编码器输出
            drainEncoder(false, info)
        }
        // 增加帧生成索引
        generateIndex++
    }

    /**
     * 将数据流转移到视频文件
     * 此函数负责从输入流中获取数据，并将其编码为视频格式
     * 它使用MediaCodec进行数据编码，并管理编码过程中的缓冲区操作
     */
    fun transferToVideo() {

        // 从输入流队列中取数据进行编码操作
        val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(defaultTimeOutUs)
        if (inputBufferIndex >= 0) {
            // 计算当前帧的显示时间戳
            val ptsUsec = computePresentationTime(generateIndex)
            // 将数据添加到编码器的输入缓冲区，并标记为数据流的结束
            mediaCodec!!.queueInputBuffer(
                inputBufferIndex,
                0,
                0,
                ptsUsec,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
            // 设置编码状态为非运行，表示编码过程即将结束
            isRunning = false
            // 从编码器中提取编码后的数据，true表示阻塞模式操作
            drainEncoder(true, info)
        }

        // 结束当前操作或者进程
        finish()
    }


    var pauseTimeInterval = 0L//暂停间隔的时间

    var startPauseTime = 0L//开始暂停的时间

    private var isPause = false//是否暂停录制

    fun pause() {
        isPause = true
        startPauseTime = System.nanoTime()
    }

    fun resume() {
        isPause = false
        pauseTimeInterval += System.nanoTime() - startPauseTime
    }

    /**
     * 计算当前帧所在时间
     */
    private fun computePresentationTime(frameIndex: Long): Long {
        return 132 + frameIndex * 1000000 / mFrameRate
    }

    /**
     * 从编码器中排空数据
     *
     * 此函数负责从媒体编码器中提取编码后的数据，并将其写入媒体复用器中
     * 它处理编码器的输出缓冲区，直到没有更多的数据或者达到了数据流的末尾
     *
     * @param endOfStream 是否结束数据流
     * @param bufferInfo 用于存储缓冲区信息的对象
     */
    private fun drainEncoder(endOfStream: Boolean, bufferInfo: MediaCodec.BufferInfo) {
        if (endOfStream) {
            try {
                //结束音频
                mediaCodec!!.signalEndOfInputStream()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        while (true) {
            // 从MediaCodec的输出队列中获取一个缓冲区的状态信息
            // 这一步是为了检查编码器的输出状态，确定是否有必要继续处理数据
            val encoderStatus = mediaCodec!!.dequeueOutputBuffer(bufferInfo, defaultTimeOutUs)
            // 当编码器状态为INFO_TRY_AGAIN_LATER时，表示当前无法处理数据
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 如果不是数据流的末尾，则退出当前循环，等待下一次尝试
                if (!endOfStream) {
                    break
                }
            // 当编码器状态为INFO_OUTPUT_FORMAT_CHANGED时，表示输出格式已改变
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (mMuxerStarted) {
                    error { "format changed twice" }
                }
                // 获取编码器的输出格式
                val mediaFormat = mediaCodec!!.outputFormat
                // 向媒体复用器添加轨道，并获取轨道索引
                mTrackIndex = mediaMuxer!!.addTrack(mediaFormat)
                // 准备媒体复用器开始工作
                mediaMuxer!!.start()
                // 标记媒体复用器已启动
                mMuxerStarted = true

            } else if (encoderStatus < 0) {
                Log.d("YapVideoEncoder", "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
            } else {
                // 获取MediaCodec的输出缓冲区，如果获取失败则抛出异常
                val outputBuffer = (mediaCodec!!.getOutputBuffer(encoderStatus)) ?: error { "encoderOutputBuffer $encoderStatus was null" }

                // 检查缓冲区标志是否包含编解码器配置信息，如果是，则将缓冲区大小设置为0
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }

                // 如果缓冲区大小不为0，表示有有效数据待处理
                if (bufferInfo.size != 0) {
                    // 检查媒体复用器是否已启动，如果没有启动则记录错误日志
                    if (!mMuxerStarted) {
                        Log.d("YapVideoEncoder", "error:muxer hasn't started")
                    }
                    // 设置输出缓冲区的位置和限制，以准备写入数据
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    // 尝试将样本数据写入媒体复用器，如果发生异常则打印异常信息
                    try {
                        mediaMuxer!!.writeSampleData(mTrackIndex, outputBuffer, bufferInfo)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }


                // 释放MediaCodec的输出缓冲区，不进行渲染
                mediaCodec!!.releaseOutputBuffer(encoderStatus, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (!endOfStream) {
                        Log.d("YapVideoEncoder", "reached end of stream unexpectedly")
                    } else {
                        Log.d("YapVideoEncoder", "end of stream reached")
                    }
                    break
                }
            }
        }
    }


    private fun getNV12(inputWidth: Int, inputHeight: Int, scaled: Bitmap?): ByteArray {
        val argb = IntArray(inputWidth * inputHeight)
        scaled!!.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
        when (colorFormat) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar -> encodeYUV420SP(
                yuv,
                argb,
                inputWidth,
                inputHeight
            )

            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> encodeYUV420P(
                yuv,
                argb,
                inputWidth,
                inputHeight
            )

            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar -> encodeYUV420PSP(
                yuv,
                argb,
                inputWidth,
                inputHeight
            )

            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> encodeYUV420PP(
                yuv,
                argb,
                inputWidth,
                inputHeight
            )
        }
        return yuv
    }

    /**
     * 将ARGB格式的数据转换为YUV420SP格式
     * YUV420SP是一种常见的图像格式，其中Y代表亮度分量，U和V代表色度分量
     * 这个方法按照YUV420SP的格式要求，将ARGB数据中的R、G、B分量转换为Y、U、V分量，并存储到ByteArray中
     *
     * @param yuv420sp 输出的YUV420SP格式数据数组
     * @param argb 输入的ARGB格式数据数组
     * @param width 图像的宽度
     * @param height 图像的高度
     */
    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                // val a = argb[index] and -0x1000000 shr 24
                val r = getR(argb[index])
                val g = getG(argb[index])
                val b = getB(argb[index])

                val y = getY(r, g, b)
                val u = getU(r, g, b)
                val v = getV(r, g, b)

                // 将Y分量存入yuv420sp数组，注意边界值处理
                yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()

                // 每两行像素共享一个UV分量，这里只在行和列都是偶数的情况下存储UV分量
                if (j % 2 == 0 && index % 2 == 0) {
                    // 将V分量存入yuv420sp数组，注意边界值处理
                    yuv420sp[uvIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()

                    // 将U分量存入yuv420sp数组，注意边界值处理
                    yuv420sp[uvIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                }

                index++
            }
        }
    }

    /**
     * 将ARGB格式的数据转换为YUV420P格式
     * YUV420P是一种常见的视频编码格式，其中Y代表亮度分量，U和V代表色度分量
     * 该函数主要用于将ARGB格式的图像数据转换为YUV420P格式，以便在视频编码中使用
     *
     * @param yuv420sp 输出的YUV420P格式的数据数组
     * @param argb 输入的ARGB格式的数据数组
     * @param width 图像的宽度
     * @param height 图像的高度
     */
    private fun encodeYUV420P(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uIndex = frameSize
        var vIndex = frameSize + width * height / 4
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                // val a = argb[index] and -0x1000000 shr 24
                val r = getR(argb[index])
                val g = getG(argb[index])
                val b = getB(argb[index])

                val y = getY(r, g, b)
                val u = getU(r, g, b)
                val v = getV(r, g, b)
                // 将Y分量存入yuv420sp数组
                yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                // 每两行像素共享一个UV分量，因此需要判断行和列是否为偶数
                if (j % 2 == 0 && index % 2 == 0) {
                    // 将U和V分量存入yuv420sp数组
                    yuv420sp[vIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                    yuv420sp[uIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                }
                index++
            }
        }
    }

    /**
     * 将ARGB格式的数据转换为YUV420PSP格式
     * YUV420PSP格式是一种特定的YUV格式，通常用于视频编码或图像处理中
     * 这个函数按照YUV420PSP格式的要求，将ARGB数据中的R、G、B分量转换为Y、U、V分量，并进行适当的排列
     *
     * @param yuv420sp 输出的YUV420PSP格式字节数组
     * @param argb 输入的ARGB格式整数数组
     * @param width 图像的宽度
     * @param height 图像的高度
     */
    private fun encodeYUV420PSP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        var yIndex = 0
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                // val a = argb[index] and -0x1000000 shr 24
                val r = getR(argb[index])
                val g = getG(argb[index])
                val b = getB(argb[index])

                // 将RGB值转换为YUV格式
                val y = getY(r, g, b)
                val u = getU(r, g, b)
                val v = getV(r, g, b)

                // 将Y值限制在0到255之间，并存入yuv420sp数组
                yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                // 按照YUV420PSP格式的要求，U和V分量每隔一行和一列存储一次
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[yIndex + 1] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                    yuv420sp[yIndex + 3] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                }
                // 当索引为偶数时，增加yIndex，以保证U和V分量的交错存储
                if (index % 2 == 0) {
                    yIndex++
                }
                index++
            }
        }
    }

    /**
     * 将ARGB格式的数据转换为YUV420PP格式的字节数组
     * YUV420PP格式是一种用于视频编码的格式，其中Y分量的采样率为4:2:0，U和V分量共享同一空间位置
     *
     * @param yuv420sp 输出的YUV420PP格式字节数组
     * @param argb 输入的ARGB格式整型数组
     * @param width 图像的宽度
     * @param height 图像的高度
     */
    private fun encodeYUV420PP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        // Y分量的索引
        var yIndex = 0
        // V分量的索引，位于数组的后半部分
        var vIndex = yuv420sp.size / 2
        // ARGB数组的索引
        var index = 0
        // 遍历每个像素点，转换为YUV格式
        for (j in 0 until height) {
            for (i in 0 until width) {
                // 提取ARGB值中的R、G、B分量
                // val a = argb[index] and -0x1000000 shr 24
                val r = getR(argb[index])
                val g = getG(argb[index])
                val b = getB(argb[index])

                // 计算Y、U、V分量
                val y = getY(r, g, b)
                val u = getU(r, g, b)
                val v = getV(r, g, b)

                // 根据行号和像素点的位置，决定Y、U、V分量的存储位置
                if (j % 2 == 0 && index % 2 == 0) { // 0
                    yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                    yuv420sp[yIndex + 1] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                    yuv420sp[vIndex + 1] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                    yIndex++
                } else if (j % 2 == 0 && index % 2 == 1) { //1
                    yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                } else if (j % 2 == 1 && index % 2 == 0) { //2
                    yuv420sp[vIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                    vIndex++
                } else if (j % 2 == 1 && index % 2 == 1) { //3
                    yuv420sp[vIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                }
                // 移动到下一个像素点
                index++
            }
        }
    }



    private fun getR(int: Int): Int {
        return int and 0xff0000 shr 16
    }

    private fun getG(int: Int): Int {
        return int and 0xff00 shr 8
    }

    private fun getB(int: Int): Int {
        return int and 0xff shr 0
    }

    private fun getY(r: Int, g: Int, b: Int): Int {
        return (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
    }

    private fun getU(r: Int, g: Int, b: Int): Int{
        return (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
    }
    private fun getV(r: Int, g: Int, b: Int): Int{
        return (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
    }

}