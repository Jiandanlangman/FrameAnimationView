package com.jiandanlangman.frameanimationview.demo

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.jiandanlangman.frameanimationview.*
import com.jiandanlangman.frameanimationview.animation.AssetsZipFrameAnimation
import com.jiandanlangman.frameanimationview.animation.DrawableFrameAnimation
import kotlin.random.Random

class MainActivity : AppCompatActivity() {


    private lateinit var frameAnimationView: FrameAnimationView

    private var isExit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isExit = false
        setContentView(R.layout.activity_main)
//        initFloatingWindow()
         frameAnimationView = findViewById(R.id.animationView)
        val params = frameAnimationView.layoutParams
        params.width = resources.displayMetrics.widthPixels
        params.height = (params.width / 540f * 1200f).toInt()
//        frameAnimationView.setAnimationListener(object : AnimationListener {
//            override fun onAnimationStart(animation: FrameAnimation) {
//                Log.d("MainActivity", "onAnimationStart")
//            }
//
//            override fun onAnimationEnd(animation: FrameAnimation) {
//                Log.d("MainActivity", "onAnimationEnd")
////                if(isExit)
////                    exit()
//            }
//
//            override fun onAnimationCancel(animation: FrameAnimation) {
//                Log.d("MainActivity", "onAnimationCancel")
////                if(isExit)
////                    exit()
//            }
//
//            override fun onAnimationRepeat(animation: FrameAnimation, repeatCount: Int) {
//                Log.d("MainActivity", "onAnimationRepeat, repeatCount:$repeatCount")
//            }
//
//            override fun onError(animation: FrameAnimation, tr: Throwable) {
//                tr.printStackTrace()
//            }
//
//        })
        findViewById<View>(R.id.start).setOnClickListener { start() }
        findViewById<View>(R.id.stop).setOnClickListener { frameAnimationView.stopAnimation() }
        findViewById<View>(R.id.visible).setOnClickListener { frameAnimationView.visibility = View.VISIBLE }
        findViewById<View>(R.id.gone).setOnClickListener { frameAnimationView.visibility = View.GONE }
    }

//
//    override fun onBackPressed() {
//        isExit = true
//        frameAnimationView.stopAnimation()
//    }

    private fun exit() {
        super.onBackPressed()
    }

    private fun start() {
        val animation = AssetsZipFrameAnimation( this, "final.zip")
        frameAnimationView.startAnimation(animation)
    }

    private fun initFloatingWindow() {
         val systemWindowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
         val wmParams = WindowManager.LayoutParams()
        wmParams.type = systemWindowType
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        wmParams.format = PixelFormat.RGBA_8888
        wmParams.gravity = Gravity.START or Gravity.BOTTOM
        val floawingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null, false)
        val wm = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.addView(floawingView, wmParams)
        floawingView.findViewById<View>(R.id.start).setOnClickListener {
            val animation = AssetsZipFrameAnimation(this, "final.zip")
            val frameAnimationView = FrameAnimationView(this@MainActivity)
            frameAnimationView.setAnimationListener(object :SimpleAnimationListener() {
                override fun onAnimationEnd(animation: FrameAnimation) {
                    (animation as AssetsZipFrameAnimation).recycle()
                    wm.removeView(frameAnimationView)
                }

                override fun onAnimationCancel(animation: FrameAnimation) {
                    (animation as AssetsZipFrameAnimation).recycle()
                    wm.removeView(frameAnimationView)
                }
            })

            val wmParams2 = WindowManager.LayoutParams()
            wmParams2.type = systemWindowType
            wmParams2.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            wmParams2.width = WindowManager.LayoutParams.MATCH_PARENT
            wmParams2.height = WindowManager.LayoutParams.MATCH_PARENT
            wmParams2.format = PixelFormat.RGBA_8888
            wmParams2.gravity = Gravity.CENTER
            wm.addView(frameAnimationView, wmParams2)
            frameAnimationView.startAnimation(animation)
        }
    }
}
