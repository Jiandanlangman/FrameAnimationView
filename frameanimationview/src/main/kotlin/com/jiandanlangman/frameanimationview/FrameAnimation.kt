package com.jiandanlangman.frameanimationview

import android.graphics.Bitmap
import android.net.Uri

interface FrameAnimation {

    fun prepare()

    fun getFrameCount(): Int

    fun getFrameDuration(position: Int): Int

    fun getFrame(position: Int) : Bitmap

    fun getRepeatCount(): Int

    fun getRepeatMode(): RepeatMode

    fun getAudioUri() : Uri?

    fun onFrameDrew(position: Int, frame:Bitmap)

    fun recycle()

    enum class RepeatMode {
        RESTART,
        REVERSE
    }
}