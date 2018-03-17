package com.tcl.john.tvplayer


import android.app.Activity
import android.content.Context
import android.net.Uri
import android.support.annotation.AttrRes
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.IOException

/**
 * 视频播放器
 * Created by ZhangJun on 2018/3/12.
 */

class VideoPlayer : FrameLayout {

    /**
     * 由ijkplayer提供，用于播放视频，需要给他传入一个surfaceView
     */
    private var mMediaPlayer: IMediaPlayer? = null

    /**
     * 视频文件地址
     */
    private var mPath = ""

    private var surfaceView: SurfaceRenderView? = null

    private var mContext: Context? = null
    private var listener: VideoPlayerListener? = null

    val duration: Long
        get() = if (mMediaPlayer != null) {
            mMediaPlayer!!.duration
        } else {
            0
        }

    val currentPosition: Long
        get() = if (mMediaPlayer != null) {
            mMediaPlayer!!.currentPosition
        } else {
            0
        }

    var speed: Float
        get() = if (mMediaPlayer != null) {
            (mMediaPlayer as IjkMediaPlayer).getSpeed(1.0f)
        } else 1.0f
        set(speed) {
            if (mMediaPlayer != null) {
                (mMediaPlayer as IjkMediaPlayer).setSpeed(speed)
            }
        }

    val tcpSpeed: Long
        get() = if (mMediaPlayer != null) {
            (mMediaPlayer as IjkMediaPlayer).tcpSpeed
        } else 0

    val isPlaying: Boolean
        get() = if (mMediaPlayer != null) {
            mMediaPlayer!!.isPlaying
        } else false

    constructor(context: Context) : super(context) {
        initVideoView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initVideoView(context)
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initVideoView(context)
    }

    private fun initVideoView(context: Context) {
        mContext = context
        // 获取关联 Activity 的 DecorView
        val decorView = (context as Activity).window.decorView
        // 沉浸式使用这些Flag
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        context.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    /**
     * 设置视频地址。
     * 根据是否第一次播放视频，做不同的操作。
     *
     * @param path the path of the video.
     */
    fun setVideoPath(path: String) {
        if (TextUtils.isEmpty(mPath)) {
            //如果是第一次播放视频，那就创建一个新的surfaceView
            mPath = path
            createSurfaceView()
        } else {
            //否则就直接load
            mPath = path
            load()
        }
    }

    /**
     * 新建一个surfaceview
     */
    private fun createSurfaceView() {
        //生成一个新的surface view
        surfaceView = SurfaceRenderView(this.mContext!!)
        val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER)
        surfaceView!!.layoutParams = lp
        surfaceView!!.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT)
        addView(surfaceView)
        surfaceView!!.holder.addCallback(SurfaceCallback())
    }

    /**
     * surfaceView的监听器
     */
    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {}

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            //surfaceview创建成功后，加载视频
            load()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    /**
     * 加载视频
     */
    private fun load() {
        //每次都要重新创建IMediaPlayer
        createPlayer()
        Log.d(TAG, "load: isNetWorkAvailable = " +
                TvUtils.checkNetWorkStatus(this.mContext!!) + " mPath = " + mPath + " isUrLValid = " + TvUtils.isValidUrl(mPath))
        try {
            if (TvUtils.checkNetWorkStatus(this.mContext!!) && TvUtils.isValidUrl(mPath)) {
                //断网自动重新连接
                mMediaPlayer!!.setDataSource(mContext, Uri.parse("ijkhttphook:$mPath"))
            } else {
                mMediaPlayer!!.dataSource = mPath
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d(TAG, "load: e.getMessage() = " + e.message)
        }

        //给mediaPlayer设置视图
        mMediaPlayer!!.setDisplay(surfaceView!!.holder)

        mMediaPlayer!!.prepareAsync()
    }

    /**
     * 创建一个新的player
     */
    private fun createPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
            mMediaPlayer!!.setDisplay(null)
            mMediaPlayer!!.release()
        }
        val ijkMediaPlayer = IjkMediaPlayer()
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG)

        //开启硬解码
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg4", 1)

        //变速不变调
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1)

        //允许掉帧
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5)

        //m3u8本地播放
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist",
                "ijkhttphook,crypto,file,http,https,tcp,tls,udp")

        //url切换400（http与https域名共用）
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)

        //减小缓冲区，减少缓冲时长
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max-buffer-size", 1024)
        //视频的话，设置100帧即开始播放
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 100)

        //播放中断重连
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1)

        mMediaPlayer = ijkMediaPlayer

        //断网自动重新连接
        (mMediaPlayer as IjkMediaPlayer).setOnNativeInvokeListener { _, _ -> true }

        if (listener != null) {
            Log.d(TAG, "createPlayer: listener.")
            mMediaPlayer!!.setOnPreparedListener(listener)
            mMediaPlayer!!.setOnInfoListener(listener)
            mMediaPlayer!!.setOnSeekCompleteListener(listener)
            mMediaPlayer!!.setOnBufferingUpdateListener(listener)
            mMediaPlayer!!.setOnErrorListener(listener)
        }
    }

    fun setListener(listener: VideoPlayerListener) {
        Log.d(TAG, "setListener: listener = $listener")
        this.listener = listener
    }

    //======================下面封装了一些控制视频的方法===========================

    fun start() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.start()
        }
    }

    fun release() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }

    fun pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.pause()
        }
    }

    fun stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
        }
    }

    fun reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.reset()
        }
    }

    fun seekTo(l: Long) {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.seekTo(l)
        }
    }

    fun playTimeBackward() {
        var playTime = currentPosition
        playTime = if (playTime - SPEED_STEP_SIZE < 0) 0 else playTime - SPEED_STEP_SIZE
        if (playTime >= 0) {
            Log.d(TAG, "playTimeBackward: playTime = $playTime")
            seekTo(playTime)
        }
    }

    fun playTimeForward() {
        var playTime = currentPosition
        val totalTime = duration
        playTime = if (playTime + SPEED_STEP_SIZE > totalTime) totalTime else playTime + SPEED_STEP_SIZE
        if (playTime >= totalTime) {
            seekTo(0)
        } else {
            Log.d(TAG, "playTimeForward: playTime = $playTime")
            seekTo(playTime)
        }
    }

    companion object {

        private val TAG = VideoPlayer::class.java.name
        private const val SPEED_STEP_SIZE = 15000 //按照15s快进快退
    }
}
