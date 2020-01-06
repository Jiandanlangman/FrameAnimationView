package com.jiandanlangman.frameanimationview.animation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.jiandanlangman.frameanimationview.FrameAnimation
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.SoftReference
import java.util.*
import java.util.zip.ZipFile
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class ZipFrameAnimation(private val context: Context, path: String) : FrameAnimation {


    private val reusable = Reusable()
    private val zipFile = ZipFile(path)
    private val options = BitmapFactory.Options()
    private val frameList = ArrayList<InnerFrame>()
    private var width = 0
    private var height = 0
    private var repeatCount = 0
    private var repeatMode = FrameAnimation.RepeatMode.RESTART
    private var frameCount = 0
    private var audioPath = ""


    override fun prepare() {
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inSampleSize = 1
        options.inPurgeable = true
        options.inInputShareable = true
        options.inMutable = true
        val sb = StringBuilder()
        val reader = zipFile.getInputStream(zipFile.getEntry("animation.json")).bufferedReader()
        reader.lineSequence().forEach { sb.append(it) }
        reader.close()
        val config = JSONObject(sb.toString())
        width = config.optInt("width", 0)
        height = config.optInt("height", 0)
        repeatCount = config.optInt("repeat_count", 0)
        repeatMode = if (config.optInt("repeat_mode", 0) == 0) FrameAnimation.RepeatMode.RESTART else FrameAnimation.RepeatMode.REVERSE
        val frameList = config.optJSONArray("frame_list")!!
        frameCount = frameList.length()
        for (i in 0 until frameList.length()) {
            val frame = frameList.optJSONObject(i)
            this.frameList.add(InnerFrame(frame.optString("drawable", ""), frame.optInt("duration", 0)))
        }
        val audio = config.optString("audio", "")
        if (audio.isNotEmpty()) {
            val audioFile = File(context.cacheDir, "frame_animation_audio_${System.currentTimeMillis()}")
            val iss = zipFile.getInputStream(zipFile.getEntry(audio))
            val oss = FileOutputStream(audioFile)
            iss.copyTo(oss, 81960)
            oss.close()
            iss.close()
            audioPath = audioFile.absolutePath
        }
    }

    override fun getFrameCount() = frameCount

    override fun getFrameDuration(position: Int) = frameList[position].duration


    override fun getFrame(position: Int): Bitmap {
        val entry = zipFile.getEntry(frameList[position].drawable)
        val iss = zipFile.getInputStream(entry).buffered(256)
        iss.mark(0)
        if (width > 0 && height > 0) {
            options.outWidth = width
            options.outHeight = height
        } else {
            options.inBitmap = null
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(iss, null, options)
        }
        iss.reset()
        options.inJustDecodeBounds = false
        options.inBitmap = reusable.getBitmapFromReusableSet(options)
        val frame = BitmapFactory.decodeStream(iss, null, options)!!
        iss.close()
        return frame
    }


    override fun getRepeatCount() = repeatCount

    override fun getRepeatMode() = repeatMode

    override fun getAudioUri() = if (audioPath.isEmpty()) null else Uri.fromFile(File(audioPath))

    override fun onFrameDrew(position: Int, frame: Bitmap) = reusable.addToReusableBitmapSet(frame)

    override fun recycle() {
        reusable.recycleReusableBitmaps()
        frameList.clear()
        zipFile.close()
        options.inBitmap?.recycle()
        options.inBitmap = null
        if (audioPath.isNotEmpty())
            File(audioPath).delete()

    }

    private inner class InnerFrame(val drawable: String, val duration: Int)

    private inner class Reusable {
        private val reusableBitmaps = Collections.synchronizedSet(HashSet<SoftReference<Bitmap>>())

        fun addToReusableBitmapSet(bitmap: Bitmap) {
            reusableBitmaps.add(SoftReference(bitmap))
        }


        fun getBitmapFromReusableSet(options: BitmapFactory.Options): Bitmap? {
            synchronized(reusableBitmaps) {
                val it = reusableBitmaps.iterator()
                while (it.hasNext()) {
                    val bitmap = it.next().get()
                    if (bitmap == null) {
                        it.remove()
                        continue
                    }
                    if (canUseForInBitmap(bitmap, options)) {
                        it.remove()
                        return bitmap
                    }
                }
            }
            return null
        }

        private fun canUseForInBitmap(bitmap: Bitmap, options: BitmapFactory.Options): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                var width = options.outWidth
                var height = options.outHeight
                if (options.inSampleSize > 1) {
                    width /= options.inSampleSize
                    height /= options.inSampleSize
                }
                val byteCount = width * height * 4
                return byteCount <= bitmap.allocationByteCount
            }
            return bitmap.width == options.outWidth && bitmap.height == options.outHeight && options.inSampleSize == 1
        }

        fun recycleReusableBitmaps() {
            reusableBitmaps.forEach { it.get()?.recycle() }
            reusableBitmaps.clear()
        }
    }

}