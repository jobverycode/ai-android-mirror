package com.ai.mirror.data.streaming

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object FrameProcessor {

    @OptIn(ExperimentalGetImage::class)
    fun processImageProxyToJpeg(
        imageProxy: ImageProxy,
        quality: Int = 80,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ): ByteArray? {
        val image = imageProxy.image ?: return null

        return try {
            if (image.format == ImageFormat.YUV_420_888) {
                val nv21 = yuv420888ToNv21(imageProxy)
                val yuvImage = YuvImage(
                    nv21,
                    ImageFormat.NV21,
                    imageProxy.width,
                    imageProxy.height,
                    null
                )
                val out = ByteArrayOutputStream()
                val rect = Rect(0, 0, imageProxy.width, imageProxy.height)
                yuvImage.compressToJpeg(rect, quality.coerceIn(10, 100), out)
                out.toByteArray()
            } else {
                // Fallback using Bitmap
                val bitmap = imageProxy.toBitmap()
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), out)
                out.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val yRowStride = planes[0].rowStride
        val yPixelStride = planes[0].pixelStride
        val uvRowStride = planes[1].rowStride
        val uvPixelStride = planes[1].pixelStride

        var pos = 0
        if (yRowStride == width && yPixelStride == 1) {
            yBuffer.get(nv21, 0, ySize)
            pos = ySize
        } else {
            for (row in 0 until height) {
                yBuffer.position(row * yRowStride)
                yBuffer.get(nv21, pos, width)
                pos += width
            }
        }

        // Interleave V and U for NV21
        val uvHeight = height / 2
        val uvWidth = width / 2

        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val vIndex = row * uvRowStride + col * uvPixelStride
                val uIndex = row * uvRowStride + col * uvPixelStride

                nv21[pos++] = vBuffer.get(vIndex)
                nv21[pos++] = uBuffer.get(uIndex)
            }
        }

        return nv21
    }

    fun rotateBitmap(source: Bitmap, angle: Float, flipHorizontal: Boolean = false): Bitmap {
        if (angle == 0f && !flipHorizontal) return source
        val matrix = Matrix()
        if (angle != 0f) {
            matrix.postRotate(angle)
        }
        if (flipHorizontal) {
            matrix.postScale(-1f, 1f, source.width / 2f, source.height / 2f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
