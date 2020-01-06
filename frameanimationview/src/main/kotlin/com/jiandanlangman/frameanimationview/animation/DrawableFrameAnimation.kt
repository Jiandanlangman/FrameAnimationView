package com.jiandanlangman.frameanimationview.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import com.jiandanlangman.frameanimationview.FrameAnimation

class DrawableFrameAnimation(private val animationDrawable: AnimationDrawable, private val audioUri: Uri?) : FrameAnimation {

    constructor(context: Context, resId: Int, audioUri: Uri?) : this(context.resources.getDrawable(resId) as AnimationDrawable, audioUri)

    private var frameCount = 0
    private var repeatCount = 0

    override fun prepare() {
        frameCount = animationDrawable.numberOfFrames
        repeatCount = if (animationDrawable.isOneShot) 0 else -1
    }

    override fun getFrameCount() = frameCount

    override fun getFrameDuration(position: Int) = animationDrawable.getDuration(position)

    override fun getFrame(position: Int) = (animationDrawable.getFrame(position) as BitmapDrawable).bitmap!!

    override fun getRepeatCount() = repeatCount

    override fun getRepeatMode() = FrameAnimation.RepeatMode.RESTART

    override fun getAudioUri() = audioUri

    override fun onFrameDrew(position: Int, frame: Bitmap) = Unit

    override fun recycle() = Unit

}