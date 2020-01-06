package com.jiandanlangman.frameanimationview.animation

import android.content.Context
import android.graphics.Bitmap
import com.jiandanlangman.frameanimationview.FrameAnimation
import java.io.File
import java.io.FileOutputStream
import java.lang.RuntimeException
import java.net.HttpURLConnection
import java.net.URL

class URLZipFrameAnimation(private val context: Context, private val url: String) : FrameAnimation {

    private lateinit var zipFrameAnimation: ZipFrameAnimation

    override fun prepare() {
        val fileName = getFileName(url)
        val outputFile = File(context.cacheDir, "url_zip_frame_animation_$fileName")
        if (!outputFile.exists()) {
            val tempFile = File(context.cacheDir, "_temp_${outputFile.name}")
            var retryCount = 0
            while (retryCount <= 10) {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    var sum = 0L
                    if (tempFile.exists()) {
                        sum = tempFile.length()
                        conn.setRequestProperty("Range", "bytes=$sum-")
                    }
                    if (conn.responseCode == 200 || conn.responseCode == 206) {
                        val ins = conn.inputStream
                        val fos = FileOutputStream(tempFile, true)
                        val buffer = ByteArray(8196)
                        var readLength: Int
                        while (true) {
                            readLength = ins.read(buffer)
                            if (readLength == -1)
                                break
                            fos.write(buffer, 0, readLength)
                            sum += readLength
                        }
                        fos.flush()
                        fos.close()
                        ins.close()
                        conn.disconnect()
                        tempFile.renameTo(outputFile)
                        retryCount = 100
                    } else {
                        retryCount++
                        if (retryCount > 10) {
                            tempFile.delete()
                            throw RuntimeException("download failed!")
                        }
                    }
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount > 10) {
                        tempFile.delete()
                        throw e
                    }
                }
            }
        }
        zipFrameAnimation = ZipFrameAnimation(context, outputFile.absolutePath)
        zipFrameAnimation.prepare()
    }

    override fun getFrameCount() = zipFrameAnimation.getFrameCount()

    override fun getFrameDuration(position: Int) = zipFrameAnimation.getFrameDuration(position)

    override fun getFrame(position: Int) = zipFrameAnimation.getFrame(position)


    override fun getRepeatCount() = zipFrameAnimation.getRepeatCount()

    override fun getRepeatMode() = zipFrameAnimation.getRepeatMode()

    override fun getAudioUri() = zipFrameAnimation.getAudioUri()

    override fun onFrameDrew(position: Int, frame: Bitmap) =
        zipFrameAnimation.onFrameDrew(position, frame)

    override fun recycle() = zipFrameAnimation.recycle()

    private fun getFileName(url: String): String {
        val index = url.lastIndexOf("/")
        return if (index == -1) url else url.substring(index + 1)
    }
}