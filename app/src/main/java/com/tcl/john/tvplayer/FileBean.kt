package com.tcl.john.tvplayer

import android.databinding.ObservableField

/**
 * 视频文件实体类
 * Created by ZhangJun on 2018/3/13.
 */

class FileBean {

    val fileName = ObservableField<String>()
    val filePath = ObservableField<String>()
}
