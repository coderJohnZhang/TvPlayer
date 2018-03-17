package com.tcl.john.tvplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log

import org.greenrobot.eventbus.EventBus

class UsbBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        //收到挂载广播时，通知刷新USB相关界面
        if (!TextUtils.isEmpty(action) && action == Intent.ACTION_MEDIA_MOUNTED) {
            val mountPath = intent.data!!.path
            Log.d(TAG, "onReceive: mount mountPath = $mountPath")
            if (!TextUtils.isEmpty(mountPath)) {
                //读取到U盘路径再做其他业务逻辑
                EventBus.getDefault().post(UsbEvent(UsbEvent.EVENT_USB_MOUNT, mountPath))
            }
        }

        //收到移除广播时，通知刷新USB相关界面
        if (!TextUtils.isEmpty(action) && action == Intent.ACTION_MEDIA_EJECT) {
            Log.d(TAG, "onReceive: eject")
            EventBus.getDefault().post(UsbEvent(UsbEvent.EVENT_USB_EJECT, null))
        }
    }

    companion object {

        private val TAG = UsbBroadcastReceiver::class.java.name
    }
}
