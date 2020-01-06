package com.jiandanlangman.frameanimationview

open class SimpleAnimationListener : AnimationListener {

    override fun onAnimationStart(animation: FrameAnimation) = Unit

    override fun onAnimationEnd(animation: FrameAnimation) = Unit

    override fun onAnimationCancel(animation: FrameAnimation) = Unit

    override fun onAnimationRepeat(animation: FrameAnimation, repeatCount: Int) = Unit

    override fun onError(animation: FrameAnimation, tr: Throwable) = Unit
}