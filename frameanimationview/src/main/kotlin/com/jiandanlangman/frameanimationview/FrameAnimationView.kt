package com.jiandanlangman.frameanimationview

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Process
import android.os.SystemClock
import android.util.AttributeSet
import android.view.TextureView
import android.view.ViewTreeObserver
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class FrameAnimationView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : TextureView(context, attrs, defStyleAttr) {

    private companion object {
        const val MAX_CACHE_FRAME = 4
    }

    private val lock = Object()
    private val drawLock = Object()
    private val cacheFrames = ArrayList<Frame>()
    private val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {

        if (isShown && isTextureAvailable) {
            synchronized(lock) {
                if (animation == null) {
                    val canvas = lockCanvas()
                    if (canvas != null) {
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                        unlockCanvasAndPost(canvas)
                    }
                } else
                    lock.notifyAll()
            }
        }
    }


    private val listenerInvokeHandler = Handler()
    private val audioPlayer = FrameAnimationAudioPlayer(context)

    private var animationListener: AnimationListener = object : SimpleAnimationListener() {}
    private var delayStrategy = DelayStrategy.PRECISE_TIME
    private var cleanCanvas = true
    private var isTextureAvailable = false
    private var isPreLoading = false

    private var preloadFrameThreadPool: ExecutorService? = null
    private var workThreadPool: ExecutorService? = null

    private var tempAnimation: FrameAnimation? = null
    private var animation: FrameAnimation? = null

    init {
        isOpaque = false
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
                synchronized(lock) { isTextureAvailable = false }
                preloadFrameThreadPool?.shutdown()
                workThreadPool?.shutdown()
                synchronized(drawLock) {}
                return true
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                synchronized(lock) { isTextureAvailable = true }
                viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
                preloadFrameThreadPool?.shutdown()
                workThreadPool?.shutdown()
                preloadFrameThreadPool = Executors.newSingleThreadExecutor()
                workThreadPool = Executors.newSingleThreadExecutor()
                if (tempAnimation != null) {
                    startAnimation(tempAnimation!!)
                    tempAnimation = null
                }
            }
        }
    }


    fun startAnimation(animation: FrameAnimation) {
        if (!isTextureAvailable) {
            if (animation != tempAnimation)
                tempAnimation = animation
            return
        }
        if (animation == this.animation)
            return
        this.animation = animation
        workThreadPool!!.execute {
            Process.setThreadPriority(-19)
            try {
                animation.prepare()
            } catch (tr: Throwable) {
                animationListener.onError(animation, tr)
                if (animation == this.animation)
                    this.animation = null
            }
            startPreload(animation)
            audioPlayer.init(animation)
            drawFrame(animation)
            animation.recycle()
            synchronized(lock) {
                if (isTextureAvailable && cleanCanvas) {
                    synchronized(drawLock) {
                        val canvas = lockCanvas()
                        if (canvas != null) {
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                            unlockCanvasAndPost(canvas)
                        }
                    }
                }
                lock.notifyAll()
            }
            if (animation == this.animation && isTextureAvailable)
                listenerInvokeHandler.post { animationListener.onAnimationEnd(animation) }
            else
                listenerInvokeHandler.post { animationListener.onAnimationCancel(animation) }
            if (animation == this.animation)
                this.animation = null
            audioPlayer.release()
        }
    }


    fun stopAnimation() {
        tempAnimation = null
        animation = null
        audioPlayer.release()
    }

    fun setAnimationListener(animationListener: AnimationListener) {
        this.animationListener = animationListener
    }

    /**
     * 设置延迟策略
     * @param delayStrategy 延迟策略
     */
    fun setDelayStrategy(delayStrategy: DelayStrategy) {
        this.delayStrategy = delayStrategy
    }

    /**
     * 在动画播放完成或取消播放后，是否清除画布，如果传入false，动画停止后，屏幕上会残留最后一帧
     * @param cleanCanvas 是否清除画布
     */
    fun setCleanCanvas(cleanCanvas: Boolean) {
        this.cleanCanvas = cleanCanvas
    }


    private fun startPreload(animation: FrameAnimation) {
        synchronized(lock) { isPreLoading = true }
        cacheFrames.clear()
        preloadFrameThreadPool!!.execute {
            Process.setThreadPriority(-19)
            var position = 0
            var repeatCount = 0
            var isReversed = false
            while (true) {
                var breakWhile = false
                synchronized(lock) {
                    if (isTextureAvailable && animation == this.animation) {
                        cacheFrames.add(Frame(animation.getFrame(position), animation.getFrameDuration(position), position, repeatCount, isReversed))
                        lock.notifyAll()
                        if (cacheFrames.size >= MAX_CACHE_FRAME)
                            try {
                                lock.wait()
                            } catch (ignore: Throwable) {
                                breakWhile = true
                            }
                    } else
                        breakWhile = true
                }
                if (breakWhile)
                    break
                if (position == animation.getFrameCount() - 1 && !isReversed) {
                    if (animation.getRepeatCount() < 0 || repeatCount < animation.getRepeatCount()) {
                        isReversed = animation.getRepeatMode() == FrameAnimation.RepeatMode.REVERSE
                        if (!isReversed)
                            position = 0
                        repeatCount++
                        continue
                    } else
                        break
                } else if (position == 0 && isReversed) {
                    isReversed = false
                    if (repeatCount < animation.getRepeatCount())
                        repeatCount++
                    else
                        break
                    continue
                }
                if (isReversed)
                    position--
                else
                    position++
            }
            synchronized(lock) {
                isPreLoading = false
                lock.notifyAll()
            }
        }
    }

    private fun drawFrame(animation: FrameAnimation) {
        var timeOffset = 0L
        while (true) {
            val time = SystemClock.elapsedRealtime()
            var flag = 0
            var frame: Frame? = null
            synchronized(lock) {
                if (isTextureAvailable && animation == this.animation  && cacheFrames.isNotEmpty()) {
                    frame = cacheFrames.removeAt(0)
                    lock.notifyAll()
                } else if (cacheFrames.isEmpty() && isPreLoading) {
                    try {
                        flag = 1
                        audioPlayer.pause()
                        lock.wait()
                    } catch (ignore: Throwable) {
                        flag = 2
                    }
                } else
                    flag = 2
            }
            if (flag == 1)
                continue
            if (flag == 2)
                break
            synchronized(drawLock) {
                val canvas = lockCanvas()
                if (canvas != null) {
                    drawFrame(canvas, frame!!.drawable)
                    unlockCanvasAndPost(canvas)
                }
            }
            animation.onFrameDrew(frame!!.position, frame!!.drawable)
            if (frame!!.position == 0) {
                if (frame!!.repeatCount == 0) {
                    audioPlayer.pauseAndSeekToStart()
                    handler.post { animationListener.onAnimationStart(animation) }
                } else {
                    audioPlayer.pauseAndSeekToStart()
                    handler.post { animationListener.onAnimationRepeat(animation, frame!!.repeatCount) }
                }
            } else if (frame!!.position == animation.getFrameCount() - 1 && frame!!.isReversed) {
                audioPlayer.pauseAndSeekToStart()
                handler.post { animationListener.onAnimationRepeat(animation, frame!!.repeatCount) }
            }
            audioPlayer.start()
            val duration = (if (delayStrategy == DelayStrategy.PRECISE_TIME) frame!!.duration.toLong() - (SystemClock.elapsedRealtime() - time) else frame!!.duration.toLong()) - timeOffset
            if (duration > 0L)
                try {
                    val t = SystemClock.elapsedRealtime()
                    SystemClock.sleep(duration)
                    timeOffset = (SystemClock.elapsedRealtime() - t) - duration
                } catch (ignore: Throwable) {

                }
        }
    }


    private fun drawFrame(canvas: Canvas, drawable: Bitmap) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val scale = if (abs(canvas.width - drawable.width) <= (canvas.height - drawable.height)) canvas.width / drawable.width.toFloat() else canvas.height / drawable.height.toFloat()
        canvas.save()
        canvas.translate((canvas.width - drawable.width * scale) / 2f, (canvas.height - drawable.height * scale) / 2f)
        canvas.scale(scale, scale)
        canvas.drawBitmap(drawable, 0f, 0f, null)
        canvas.restore()
    }


    private inner class Frame(val drawable: Bitmap, val duration: Int, val position: Int, val repeatCount: Int, val isReversed: Boolean)


    enum class DelayStrategy {
        //精准时间，控制动画一定会在指定的时间内播放完成，当设备性能较低时，可能发生跳帧
        PRECISE_TIME,
        //全部帧率，保证所有帧都会显示且至少会显示指定的duration，当设备性能较低时，可能会发生实际播放时间远大于指定的时间
        FULL_FRAME
    }

}