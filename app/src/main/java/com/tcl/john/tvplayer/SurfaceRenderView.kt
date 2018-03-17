package com.tcl.john.tvplayer

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.ISurfaceTextureHolder

class SurfaceRenderView : SurfaceView, IRenderView {
    private var mMeasureHelper: MeasureHelper? = null

    override val view: View
        get() = this

    private var mSurfaceCallback: SurfaceCallback? = null

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initView()
    }

    private fun initView() {
        mMeasureHelper = MeasureHelper(this)
        mSurfaceCallback = SurfaceCallback(this)
        holder.addCallback(mSurfaceCallback)

        holder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL)
    }

    override fun shouldWaitForResize(): Boolean {
        return true
    }

    //--------------------
    // Layout & Measure
    //--------------------
    override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        if (videoWidth > 0 && videoHeight > 0) {
            mMeasureHelper!!.setVideoSize(videoWidth, videoHeight)
            holder.setFixedSize(videoWidth, videoHeight)
            requestLayout()
        }
    }

    override fun setVideoSampleAspectRatio(videoSarNum: Int, videoSarDen: Int) {
        if (videoSarNum > 0 && videoSarDen > 0) {
            mMeasureHelper!!.setVideoSampleAspectRatio(videoSarNum, videoSarDen)
            requestLayout()
        }
    }

    override fun setVideoRotation(degree: Int) {
        Log.e("", "SurfaceView doesn't support rotation ($degree)!\n")
    }

    override fun setAspectRatio(aspectRatio: Int) {
        mMeasureHelper!!.setAspectRatio(aspectRatio)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mMeasureHelper!!.doMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mMeasureHelper!!.measuredWidth, mMeasureHelper!!.measuredHeight)
    }

    //--------------------
    // SurfaceViewHolder
    //--------------------

    private class InternalSurfaceHolder(private val mSurfaceView: SurfaceRenderView,
                                        override val surfaceHolder: SurfaceHolder?) : IRenderView.ISurfaceHolder {

        override val renderView: IRenderView
            get() = mSurfaceView

        override val surfaceTexture: SurfaceTexture?
            get() = null

        override fun bindToMediaPlayer(mp: IMediaPlayer) {
            if (mp is ISurfaceTextureHolder) {
                val textureHolder = mp as ISurfaceTextureHolder?
                textureHolder!!.surfaceTexture = null
            }
            mp.setDisplay(surfaceHolder)
        }

        override fun openSurface(): Surface? {
            return surfaceHolder?.surface
        }
    }

    //-------------------------
    // SurfaceHolder.Callback
    //-------------------------

    override fun addRenderCallback(callback: IRenderView.IRenderCallback) {
        mSurfaceCallback!!.addRenderCallback(callback)
    }

    override fun removeRenderCallback(callback: IRenderView.IRenderCallback) {
        mSurfaceCallback!!.removeRenderCallback(callback)
    }

    private class SurfaceCallback(surfaceView: SurfaceRenderView) : SurfaceHolder.Callback {
        private var mSurfaceHolder: SurfaceHolder? = null
        private var mIsFormatChanged: Boolean = false
        private var mFormat: Int = 0
        private var mWidth: Int = 0
        private var mHeight: Int = 0

        private val mWeakSurfaceView: WeakReference<SurfaceRenderView> = WeakReference(surfaceView)
        private val mRenderCallbackMap = ConcurrentHashMap<IRenderView.IRenderCallback, Any>()

        fun addRenderCallback(callback: IRenderView.IRenderCallback) {
            mRenderCallbackMap[callback] = callback

            var surfaceHolder: IRenderView.ISurfaceHolder? = null
            if (mSurfaceHolder != null) {
                if (surfaceHolder == null) {
                    surfaceHolder = InternalSurfaceHolder(mWeakSurfaceView.get()!!, mSurfaceHolder)
                }
                callback.onSurfaceCreated(surfaceHolder, mWidth, mHeight)
            }

            if (mIsFormatChanged) {
                if (surfaceHolder == null) {
                    surfaceHolder = InternalSurfaceHolder(mWeakSurfaceView.get()!!, mSurfaceHolder)
                }
                callback.onSurfaceChanged(surfaceHolder, mFormat, mWidth, mHeight)
            }
        }

        fun removeRenderCallback(callback: IRenderView.IRenderCallback) {
            mRenderCallbackMap.remove(callback)
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            mSurfaceHolder = holder
            mIsFormatChanged = false
            mFormat = 0
            mWidth = 0
            mHeight = 0

            val surfaceHolder = InternalSurfaceHolder(mWeakSurfaceView.get()!!, mSurfaceHolder)
            for (renderCallback in mRenderCallbackMap.keys) {
                renderCallback.onSurfaceCreated(surfaceHolder, 0, 0)
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            mSurfaceHolder = null
            mIsFormatChanged = false
            mFormat = 0
            mWidth = 0
            mHeight = 0

            val surfaceHolder = InternalSurfaceHolder(mWeakSurfaceView.get()!!, mSurfaceHolder)
            for (renderCallback in mRenderCallbackMap.keys) {
                renderCallback.onSurfaceDestroyed(surfaceHolder)
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int,
                                    width: Int, height: Int) {
            mSurfaceHolder = holder
            mIsFormatChanged = true
            mFormat = format
            mWidth = width
            mHeight = height

            val surfaceHolder = InternalSurfaceHolder(mWeakSurfaceView.get()!!, mSurfaceHolder)
            for (renderCallback in mRenderCallbackMap.keys) {
                renderCallback.onSurfaceChanged(surfaceHolder, format, width, height)
            }
        }
    }

    //--------------------
    // Accessibility
    //--------------------

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = SurfaceRenderView::class.java.name
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = SurfaceRenderView::class.java.name
    }
}
