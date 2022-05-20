package com.tcl.john.tvplayer

import android.content.Context
import android.net.ConnectivityManager
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import android.util.Patterns
import java.io.File
import java.lang.reflect.Array
import java.lang.reflect.InvocationTargetException

object TvUtils {

    private var TAG = TvUtils::class.java.name

    fun getStoragePath(mContext: Context): String {
        var targetPath = ""
        val mStorageManager = mContext
                .getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolumeClazz: Class<*>
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")

            val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")

            val getVolumeState = mStorageManager.javaClass.getMethod("getVolumeState", String::class.java)

            val getPath = storageVolumeClazz.getMethod("getPath")

            val result = getVolumeList.invoke(mStorageManager)

            val length = Array.getLength(result)

            for (i in 0 until length) {

                val storageVolumeElement = Array.get(result, i)

                val path = getPath.invoke(storageVolumeElement) as String

                val state = getVolumeState.invoke(mStorageManager, path) as String
                Log.d(TAG, "getStoragePath: state = $state")
                if (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) {
                    if (isSdcard(path)) {
                        val sdRootPath = "$path/userdata"
                        val file = File(sdRootPath)
                        if (!file.exists()) {
                            file.mkdir()
                        } else if (!file.isDirectory) {
                            file.delete()
                            file.mkdir()
                        }
                    } else {
                        targetPath = path.substring(0, path.lastIndexOf("/"))
                    }
                }
            }
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        return targetPath
    }

    private fun isSdcard(path: String): Boolean {

        return path.contains("sdcard") || path.contains("emulated")
    }

    fun checkNetWorkStatus(context: Context): Boolean {
        val result: Boolean
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        if (netInfo != null && netInfo.isConnected) {
            result = true
            Log.i(TAG, "The net was connected")
        } else {
            result = false
            Log.i(TAG, "The net was bad!")
        }
        return result
    }

    /**
     * 判断字符串是否为有效网址
     */
    fun isValidUrl(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url).matches()
    }
}
