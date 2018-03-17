package com.tcl.john.tvplayer

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import java.util.*
import kotlinx.android.synthetic.main.file_list_layout.*

/**
 * 单选对话框
 * Created by ZhangJun on 2017/7/28.
 */

class SingleChoiceDialog(context: Context, data: List<FileBean>) : Dialog(context) {
    private var data = ArrayList<FileBean>()
    private var mFileListAdapter: ListViewAdapter<FileBean>? = null

    init {
        if (context is Activity) {
            ownerActivity = context
        }
        this.data = data as ArrayList<FileBean>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.file_list_layout)
        mFileListAdapter = ListViewAdapter(context, data,
                R.layout.file_list_item, com.tcl.john.tvplayer.BR.fileBean)

        file_lv!!.adapter = mFileListAdapter

        file_lv!!.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val file = data[position]
            Log.d(TAG, "onItemClick: name = " + file.fileName.get() + " filePath = " + file.filePath.get())
            dismiss()
            PlayerActivity.navigateTo(ownerActivity, file.filePath.get())
        }
        setCanceledOnTouchOutside(false)
    }

    companion object {

        val TAG = SingleChoiceDialog::class.java.simpleName!!
    }
}

