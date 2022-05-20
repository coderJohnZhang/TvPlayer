package com.tcl.john.tvplayer

import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private val mFileList = ArrayList<FileBean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        verifyStoragePermissions()

        updateData(TvUtils.getStoragePath(this))
        local_video_tv!!.setOnClickListener {
            if (mFileList.isEmpty()) {
                Toast.makeText(this@MainActivity,
                        "No video files, make sure you have inserted USB with video files.",
                        Toast.LENGTH_LONG).show()
            } else {
                showFileList()
            }
        }

        net_video_tv!!.setOnClickListener {
            //断网自动重新连接
            val url = "http://flashmedia.eastday.com/newdate/news/2016-11/shznews1125-19.mp4"
            PlayerActivity.navigateTo(this@MainActivity, url)
        }
    }

    private fun showFileList() {
        val singleChoiceDialog = SingleChoiceDialog(this, mFileList)
        singleChoiceDialog.setOnKeyListener(DialogInterface.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyMap.KEY_BACK) {
                singleChoiceDialog.dismiss()
                return@OnKeyListener true
            }
            false
        })
        singleChoiceDialog.show()
    }

    private fun updateData(mountPath: String) {
        mFileList.clear()
        var usbRoot = mountPath
        if (TextUtils.isEmpty(usbRoot)) {
            usbRoot = "/storage"
        }
        getAllFiles(File(usbRoot))
        //按名称排序
        mFileList.sortWith(Comparator { o1, o2 -> o1.fileName.get()!!.compareTo(o2.fileName.get()!!) })
        Log.d(TAG, "onCreate: mFileList.size() = " + mFileList.size)
    }

    private fun getAllFiles(readFile: File) {
        val files = readFile.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) {
                    getAllFiles(f)
                } else {
                    if (isVideoFile(f.name)) {
                        val fileBean = FileBean()
                        fileBean.fileName.set(f.name)
                        fileBean.filePath.set(f.absolutePath)
                        mFileList.add(fileBean)
                    }
                }
            }
        }
    }

    //判断是否是视频文件
    private fun isVideoFile(fileName: String): Boolean {
        val name = fileName.lowercase(Locale.getDefault())
        val suffixs = resources.getStringArray(
                R.array.video_type_suffix)
        return suffixs.any { name.endsWith(it) }
    }

    private fun verifyStoragePermissions() {
        try {
            //检测是否有写的权限
            val permission = ActivityCompat.checkSelfPermission(this,
                    "android.permission.WRITE_EXTERNAL_STORAGE")
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    public override fun onStop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        updateData(TvUtils.getStoragePath(this))
    }

    @Subscribe
    fun onUsbEvent(event: UsbEvent) {
        when (event.eventCode) {
            UsbEvent.EVENT_USB_MOUNT, UsbEvent.EVENT_USB_EJECT -> updateData(event.mountPath!!)

            else -> {
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.d(TAG, "onKeyDown: keyCode = $keyCode")
        when (keyCode) {
            KeyMap.KEY_BACK -> {
                finish()
                return true
            }
            else -> {
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {

        private val TAG = MainActivity::class.java.name
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")
    }
}
