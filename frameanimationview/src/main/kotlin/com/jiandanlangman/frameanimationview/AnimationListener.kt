package com.jiandanlangman.frameanimationview

interface AnimationListener {

    fun onAnimationStart(animation: FrameAnimation)

    fun onAnimationEnd(animation: FrameAnimation)

    fun onAnimationCancel(animation: FrameAnimation)

    fun onAnimationRepeat(animation: FrameAnimation, repeatCount: Int)

    fun onError(animation: FrameAnimation, tr:Throwable)

}