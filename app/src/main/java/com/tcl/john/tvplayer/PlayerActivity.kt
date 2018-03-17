package com.tcl.john.tvplayer

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_player.*
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * Created by ZhangJun on 2018/3/12.
 */

class PlayerActivity : Activity() {

    private var mInnerMsgHandler: InnerMsgHandler? = null
    private var isRepeat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        //加载so文件
        try {
            IjkMediaPlayer.loadLibrariesOnce(null)
            IjkMediaPlayer.native_profileBegin("libijkplayer.so")
        } catch (e: Exception) {
            this.finish()
        }

        buffer_progress.visibility = View.GONE

        play_sb!!.max = VIDEO_SEEK_MAX
        play_sb!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d(TAG, "onProgressChanged: progress = $progress isRepeat = $isRepeat")
                if (progress >= VIDEO_SEEK_MAX * 0.99) {
                    if (isRepeat) {
                        ijk_player!!.seekTo(0)
                    } else {
                        finish()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        ijk_player!!.setListener(object : VideoPlayerListener() {
            override fun onBufferingUpdate(mp: IMediaPlayer, percent: Int) {
                Log.d(TAG, "onBufferingUpdate: percent = $percent")
            }

            override fun onCompletion(mp: IMediaPlayer) {
                Log.d(TAG, "onCompletion: play finished. isRepeat = $isRepeat")
                if (isRepeat) {
                    mp.seekTo(0)
                    mp.start()
                } else {
                    finish()
                }
            }

            override fun onError(mp: IMediaPlayer, what: Int, extra: Int): Boolean {
                Log.d(TAG, "onError: what = $what")
                Toast.makeText(this@PlayerActivity, "Play error.", Toast.LENGTH_LONG).show()
                finish()
                return false
            }

            override fun onInfo(mp: IMediaPlayer, what: Int, extra: Int): Boolean {
                Log.d(TAG, "onInfo: what = $what")
                if (buffer_progress != null) {
                    if (what == 701) {
                        buffer_progress.visibility = View.VISIBLE
                    } else {
                        buffer_progress.visibility = View.GONE
                    }
                }
                return false
            }

            override fun onPrepared(mp: IMediaPlayer) {
                Log.d(TAG, "onPrepared: prepare finished.")
                mp.start()
            }

            override fun onSeekComplete(mp: IMediaPlayer) {
                Log.d(TAG, "onSeekComplete: seek to the target position.")
            }

            override fun onVideoSizeChanged(mp: IMediaPlayer, width: Int, height: Int, sar_num: Int, sar_den: Int) {
                //获取到视频的宽和高
                Log.d(TAG, "onVideoSizeChanged: width = $width height$height")
            }
        })
        val filePath: String
        if (intent != null) {
            filePath = intent.getStringExtra(KEY_EXTRA_FILE_PATH)
            Log.d(TAG, "onCreate: filePath = $filePath")
            if (!TextUtils.isEmpty(filePath)) {
                loadVideo(filePath)
            } else {
                Toast.makeText(this, "Play error.", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Play error.", Toast.LENGTH_LONG).show()
            finish()
        }

        mInnerMsgHandler = InnerMsgHandler()
    }

    /**
     * @param path 播放文件路径
     */
    private fun loadVideo(path: String) {
        ijk_player!!.setVideoPath(path)
    }

    override fun onStop() {
        ijk_player!!.stop()
        IjkMediaPlayer.native_profileEnd()
        super.onStop()
    }

    override fun onPause() {
        if (ijk_player!!.isPlaying) {
            ijk_player!!.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        ijk_player!!.release()
        overridePendingTransition(R.anim.activity_exit_in_ani, R.anim.activity_exit_out_ani)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        // 1s 刷新一次UI
        refreshUI()
    }

    private fun refreshUI() {
        //Log.d(TAG, "refreshUI: progress = " + ijk_player.getCurrentPosition() + " max = " + ijk_player.getDuration());
        val msg = mInnerMsgHandler!!.obtainMessage(MESSAGE_REFRESH_UI)
        mInnerMsgHandler!!.removeMessages(MESSAGE_REFRESH_UI)
        mInnerMsgHandler!!.sendMessageDelayed(msg, DELAY_TIME.toLong())
        if (ijk_player != null && ijk_player!!.isPlaying && play_sb != null) {
            val pos = VIDEO_SEEK_MAX * ijk_player!!.currentPosition / ijk_player!!.duration
            play_sb!!.progress = pos.toInt()
        }
    }

    private inner class InnerMsgHandler : Handler() {

        override fun handleMessage(msg: Message) {
            val what = msg.what
            //Log.d(TAG, "handleMessage: what = " + what);
            when (what) {
                MESSAGE_REFRESH_UI -> refreshUI()
                else -> {
                }
            }
            super.handleMessage(msg)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.d(TAG, "onKeyDown: keyCode = $keyCode")
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER//播放暂停
            -> {
                if (!ijk_player!!.isPlaying) {
                    ijk_player!!.start()
                } else {
                    ijk_player!!.pause()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT//快退
            -> {
                ijk_player!!.playTimeBackward()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT//快进
            -> {
                ijk_player!!.playTimeForward()
                return true
            }
            KeyEvent.KEYCODE_MENU//设置倍速
            -> {
                showSpeedSelectList()
                return true
            }
            KeyEvent.KEYCODE_PROG_RED//开启循环播放
            -> {
                isRepeat = !isRepeat
                if (isRepeat) {
                    Toast.makeText(this, "switch to recycle play.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "cancel recycle play.", Toast.LENGTH_LONG).show()
                }
                return true
            }
            KeyMap.KEY_BACK -> {
                finish()
                return true
            }
            else -> {
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 倍速选择列表
     */
    private fun showSpeedSelectList() {
        val items = arrayOf("0.5X", "1.0X", "1.25X", "1.5X", "2.0X")
        val speeds = floatArrayOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
        var pos = 0
        for (i in speeds.indices) {
            if (speeds[i] == ijk_player!!.speed) {
                pos = i
                break
            }
        }
        Log.d(TAG, "showSpeedSelectList: pos = $pos")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Change speed")
        builder.setIcon(R.mipmap.ic_launcher)
        builder.setSingleChoiceItems(items, pos
        ) { dialog, which ->
            dialog.dismiss()
            ijk_player!!.speed = speeds[which]
        }
        builder.create().show()
    }

    companion object {

        private val TAG = PlayerActivity::class.java.name
        private const val KEY_EXTRA_FILE_PATH = "extraFilePath"
        private const val MESSAGE_REFRESH_UI = 100
        private const val DELAY_TIME = 1000
        // 进度条最大值
        private const val VIDEO_SEEK_MAX = 1000

        fun navigateTo(activity: Activity, filePath: String) {
            val intent = Intent(activity, PlayerActivity::class.java)
            intent.putExtra(KEY_EXTRA_FILE_PATH, filePath)
            activity.startActivity(intent)
            activity.overridePendingTransition(R.anim.activity_entry_in_ani, R.anim.activity_entry_out_ani)
        }
    }
}
