package com.michael.folderflow.playstore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.DisplayMetrics
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import com.michael.folderflow.R
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@Config(sdk = [35])
class PlayStoreIconTest {
    @Test
    fun app_icon_512() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val foreground = requireNotNull(
            ResourcesCompat.getDrawableForDensity(
                context.resources,
                R.mipmap.ic_launcher_foreground,
                DisplayMetrics.DENSITY_XXXHIGH,
                context.theme,
            ),
        )
            .toBitmap(width = 512, height = 512)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(9, 13, 22))
            drawBitmap(foreground, 0f, 0f, null)
        }
        val output = File("../play-store/app-icon-512.png")
        output.parentFile?.mkdirs()
        writeCompressedPng(bitmap, output)
    }

    private fun writeCompressedPng(bitmap: Bitmap, output: File) {
        val image = BufferedImage(bitmap.width, bitmap.height, BufferedImage.TYPE_INT_ARGB)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        image.setRGB(0, 0, bitmap.width, bitmap.height, pixels, 0, bitmap.width)

        val writer = ImageIO.getImageWritersByFormatName("png").next()
        val params = writer.defaultWriteParam.apply {
            if (canWriteCompressed()) {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = 0f
            }
        }
        FileImageOutputStream(output).use { stream ->
            writer.output = stream
            writer.write(null, IIOImage(image, null, null), params)
        }
        writer.dispose()
    }
}
