package com.jiandanlangman.frameanimationview

import android.content.Context
import android.media.MediaPlayer

class FrameAnimationAudioPlayer internal constructor(private val context: Context) {

    private var mediaPlayer:MediaPlayer? = null

     fun init(animation: FrameAnimation) {
        release()
        val animationAudioUri = animation.getAudioUri()
        if (animationAudioUri != null) {
            try {
                mediaPlayer = MediaPlayer()
                mediaPlayer!!.setDataSource(context, animationAudioUri)
                mediaPlayer!!.prepare()
                mediaPlayer!!.start()
            } catch (ignore: Throwable) {
                try {
                    mediaPlayer!!.release()
                } catch (ignore: Throwable) {

                }
                mediaPlayer = null
            }
        }
    }

     fun pause() = try {
        mediaPlayer?.pause()
    } catch (ignore: Throwable) {

    }

     fun pauseAndSeekToStart() = try {
        mediaPlayer?.pause()
        mediaPlayer?.seekTo(0)
    } catch (ignore: Throwable) {

    }


     fun start() = try {
        mediaPlayer?.start()
    } catch (ignore: Throwable) {

    }

     fun release() = try {
        mediaPlayer?.stop()
    } catch (ignore: Throwable) {

    } finally {
        try {

        } catch (ignore: Throwable) {

        }
        mediaPlayer = null
    }

}