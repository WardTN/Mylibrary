package com.dq.mylibrary.Wifi.event


/**
 *
 * @Description: java类作用描述
 * @Author: Jpor
 * @CreateDate: 2021/1/11 16:06
 *
 */

data class EndoAlbumChangeEvents(val change: Boolean)

data class EndoSocketCloseEvents(val close: Boolean)

data class EndoShowTipWiFiEvents(val show: Boolean)

data class EndoSetWiFiEvents(val success: Boolean)

data class EndoClearWiFiEvents(val success: Boolean)

data class EndoBatteryEvents(val battery: Int, val reCharge: Boolean)

data class EndoConnectEvents(val connect: Boolean)

data class EndoSpeedEvents(val speed: Int)

data class EndoRadioEvents(val page: Int)

data class EndoFinishAllEvents(val finish: Boolean)
data class RegisterSuccess(val isSuc: Boolean)
data class SkinPicEvents(val finish: Boolean)

data class EndoFinishAlbumEvents(val position: Int)
data class EndoDeletePicVideoEvents(val path: String)
data class EndoFinishEvents(var isFinish:Boolean)
data class EndoConSucEvents(var isFinish:Boolean)//是否连接成功
data class notifySkinType(var skinType:Int)//测肤历史报告部位选择
data class notifyTimeType(var timeType:Int)//测肤历史报告时间选择
data class notifyCheck(var pos:Int,var isNext : Boolean)//测肤界面 checkbox 更新 pos:位置 ， isNext:是否清晰
data class notifySumitPic(var isSuc:Boolean)//测肤界面 通知照片是否上传成功
data class notifyCon(var isSuc:Boolean)//热点连接  是否成功

data class notifyRyClick(var pos:Int)//
data class contentAgain(val type: Int)//断开后 再次连接通知 1:面部，2：水油
data class notifyWater(var value:Int)//水分值
data class EndoWiFiChangeEvents(val disConnected: Boolean)
data class notifyTimeHis(var timeData:String,var isFirst:Boolean)//测肤历史报告时间选择 通知
data class notifyWaterData(var timeData:String)//测肤历史 水油包括数据  打开界面时加载
data class notifyConnect(var connect:Boolean)//是否连接测伏笔
data class ShowBigView(var type:Int,var lis:List<String>)//显示绘制图
data class ShowNoDataBigView(var isSuc:Boolean)//显示大图
data class ShowQuestionBigView(var path:String)//显示大图

data class ChooseToExam(var page: Int)


data class ScalpSkinType(var skinType: Int) //头皮部位选择
data class saveLargeViewToPhoto(val position:Int)//保存截图
data class showClearCache(val position:Int)//展示清理缓存
data class clearCacheEvent(val position:Int)//清理缓存