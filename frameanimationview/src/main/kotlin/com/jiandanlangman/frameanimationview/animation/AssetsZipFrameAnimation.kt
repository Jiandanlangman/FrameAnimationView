package com.jiandanlangman.frameanimationview.animation

import android.content.Context
import android.graphics.Bitmap
import com.jiandanlangman.frameanimationview.FrameAnimation
import java.io.File
import java.io.FileOutputStream

class AssetsZipFrameAnimation(private val context: Context, private val assetsName: String) : FrameAnimation {

    private val extractZipAnimationFile = File(context.cacheDir, "assets_zip_frame_animation_$assetsName")
    private lateinit var zipFrameAnimation : ZipFrameAnimation


    override fun prepare() {
        if(!extractZipAnimationFile.exists()) {
            val inputStream = context.assets.open(assetsName)
            val outputStream = FileOutputStream(extractZipAnimationFile)
            val buffer = ByteArray(81960)
            var readLength: Int
            while (true) {
                readLength = inputStream.read(buffer)
                if (readLength == -1)
                    break
                outputStream.write(buffer, 0, readLength)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        }
        zipFrameAnimation = ZipFrameAnimation(context, extractZipAnimationFile.absolutePath)
        zipFrameAnimation.prepare()
    }

    override fun getFrameCount() = zipFrameAnimation.getFrameCount()

    override fun getFrameDuration(position: Int) = zipFrameAnimation.getFrameDuration(position)

    override fun getFrame(position: Int) = zipFrameAnimation.getFrame(position)


    override fun getRepeatCount() = zipFrameAnimation.getRepeatCount()

    override fun getRepeatMode() = zipFrameAnimation.getRepeatMode()

    override fun getAudioUri() = zipFrameAnimation.getAudioUri()

    override fun onFrameDrew(position: Int, frame: Bitmap) = zipFrameAnimation.onFrameDrew(position, frame)

    override fun recycle()  =  zipFrameAnimation.recycle()
}