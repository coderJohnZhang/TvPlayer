package com.tcl.john.tvplayer

/**
 * USB插拔事件
 * Created by ZhangJun on 2017/11/1.
 */

class UsbEvent(var eventCode: Int, var mountPath: String?) {
    companion object {
        const val EVENT_USB_MOUNT = 0x01
        const val EVENT_USB_EJECT = 0x02
    }
}
