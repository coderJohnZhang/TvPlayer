package com.tcl.john.tvplayer

import tv.danmaku.ijk.media.player.IMediaPlayer

/**
 * 提供回调的接口
 * Created by ZhangJun on 2018/3/12.
 */
abstract class VideoPlayerListener : IMediaPlayer.OnBufferingUpdateListener,
        IMediaPlayer.OnCompletionListener, IMediaPlayer.OnPreparedListener,
        IMediaPlayer.OnInfoListener, IMediaPlayer.OnVideoSizeChangedListener,
        IMediaPlayer.OnErrorListener, IMediaPlayer.OnSeekCompleteListener
