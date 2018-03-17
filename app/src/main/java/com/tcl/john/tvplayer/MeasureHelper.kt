package com.tcl.john.tvplayer

import android.content.Context
import android.view.View

import java.lang.ref.WeakReference

class MeasureHelper(view: View) {
    private val mWeakView: WeakReference<View>?

    private var mVideoWidth: Int = 0
    private var mVideoHeight: Int = 0
    private var mVideoSarNum: Int = 0
    private var mVideoSarDen: Int = 0

    private var mVideoRotationDegree: Int = 0

    var measuredWidth: Int = 0
        private set
    var measuredHeight: Int = 0
        private set

    private var mCurrentAspectRatio = IRenderView.AR_ASPECT_FIT_PARENT

    val view: View?
        get() = mWeakView?.get()

    init {
        mWeakView = WeakReference(view)
    }

    fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        mVideoWidth = videoWidth
        mVideoHeight = videoHeight
    }

    fun setVideoSampleAspectRatio(videoSarNum: Int, videoSarDen: Int) {
        mVideoSarNum = videoSarNum
        mVideoSarDen = videoSarDen
    }

    fun setVideoRotation(videoRotationDegree: Int) {
        mVideoRotationDegree = videoRotationDegree
    }

    /**
     * Must be called by View.onMeasure(int, int)
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    fun doMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) {
            val tempSpec = widthMeasureSpec
            widthMeasureSpec = heightMeasureSpec
            heightMeasureSpec = tempSpec
        }

        var width = View.getDefaultSize(mVideoWidth, widthMeasureSpec)
        var height = View.getDefaultSize(mVideoHeight, heightMeasureSpec)
        if (mCurrentAspectRatio == IRenderView.AR_MATCH_PARENT) {
            width = widthMeasureSpec
            height = heightMeasureSpec
        } else if (mVideoWidth > 0 && mVideoHeight > 0) {
            val widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec)
            val widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec)
            val heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec)
            val heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec)

            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                val specAspectRatio = widthSpecSize.toFloat() / heightSpecSize.toFloat()
                var displayAspectRatio: Float
                when (mCurrentAspectRatio) {
                    IRenderView.AR_16_9_FIT_PARENT -> {
                        displayAspectRatio = 16.0f / 9.0f
                        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270)
                            displayAspectRatio = 1.0f / displayAspectRatio
                    }
                    IRenderView.AR_4_3_FIT_PARENT -> {
                        displayAspectRatio = 4.0f / 3.0f
                        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270)
                            displayAspectRatio = 1.0f / displayAspectRatio
                    }
                    IRenderView.AR_ASPECT_FIT_PARENT, IRenderView.AR_ASPECT_FILL_PARENT, IRenderView.AR_ASPECT_WRAP_CONTENT -> {
                        displayAspectRatio = mVideoWidth.toFloat() / mVideoHeight.toFloat()
                        if (mVideoSarNum > 0 && mVideoSarDen > 0)
                            displayAspectRatio = displayAspectRatio * mVideoSarNum / mVideoSarDen
                    }
                    else -> {
                        displayAspectRatio = mVideoWidth.toFloat() / mVideoHeight.toFloat()
                        if (mVideoSarNum > 0 && mVideoSarDen > 0)
                            displayAspectRatio = displayAspectRatio * mVideoSarNum / mVideoSarDen
                    }
                }
                val shouldBeWider = displayAspectRatio > specAspectRatio

                when (mCurrentAspectRatio) {
                    IRenderView.AR_ASPECT_FIT_PARENT, IRenderView.AR_16_9_FIT_PARENT, IRenderView.AR_4_3_FIT_PARENT -> if (shouldBeWider) {
                        // too wide, fix width
                        width = widthSpecSize
                        height = (width / displayAspectRatio).toInt()
                    } else {
                        // too high, fix height
                        height = heightSpecSize
                        width = (height * displayAspectRatio).toInt()
                    }
                    IRenderView.AR_ASPECT_FILL_PARENT -> if (shouldBeWider) {
                        // not high enough, fix height
                        height = heightSpecSize
                        width = (height * displayAspectRatio).toInt()
                    } else {
                        // not wide enough, fix width
                        width = widthSpecSize
                        height = (width / displayAspectRatio).toInt()
                    }
                    IRenderView.AR_ASPECT_WRAP_CONTENT -> if (shouldBeWider) {
                        // too wide, fix width
                        width = Math.min(mVideoWidth, widthSpecSize)
                        height = (width / displayAspectRatio).toInt()
                    } else {
                        // too high, fix height
                        height = Math.min(mVideoHeight, heightSpecSize)
                        width = (height * displayAspectRatio).toInt()
                    }
                    else -> if (shouldBeWider) {
                        width = Math.min(mVideoWidth, widthSpecSize)
                        height = (width / displayAspectRatio).toInt()
                    } else {
                        height = Math.min(mVideoHeight, heightSpecSize)
                        width = (height * displayAspectRatio).toInt()
                    }
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize
                height = heightSpecSize

                // for compatibility, we adjust size based on aspect ratio
                if (mVideoWidth * height < width * mVideoHeight) {
                    width = height * mVideoWidth / mVideoHeight
                } else if (mVideoWidth * height > width * mVideoHeight) {
                    height = width * mVideoHeight / mVideoWidth
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize
                height = width * mVideoHeight / mVideoWidth
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize
                }
            } else if (heightSpecMode == View.MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize
                width = height * mVideoWidth / mVideoHeight
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mVideoWidth
                height = mVideoHeight
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize
                    width = height * mVideoWidth / mVideoHeight
                }
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize
                    height = width * mVideoHeight / mVideoWidth
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }

        measuredWidth = width
        measuredHeight = height
    }

    fun setAspectRatio(aspectRatio: Int) {
        mCurrentAspectRatio = aspectRatio
    }

    companion object {

        fun getAspectRatioText(context: Context, aspectRatio: Int): String {
            return when (aspectRatio) {
                IRenderView.AR_ASPECT_FIT_PARENT -> context.getString(R.string.VideoView_ar_aspect_fit_parent)
                IRenderView.AR_ASPECT_FILL_PARENT -> context.getString(R.string.VideoView_ar_aspect_fill_parent)
                IRenderView.AR_ASPECT_WRAP_CONTENT -> context.getString(R.string.VideoView_ar_aspect_wrap_content)
                IRenderView.AR_MATCH_PARENT -> context.getString(R.string.VideoView_ar_match_parent)
                IRenderView.AR_16_9_FIT_PARENT -> context.getString(R.string.VideoView_ar_16_9_fit_parent)
                IRenderView.AR_4_3_FIT_PARENT -> context.getString(R.string.VideoView_ar_4_3_fit_parent)
                else -> context.getString(R.string.N_A)
            }
        }
    }
}
