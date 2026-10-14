package com.focal.app.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

actual object PlatformQrGenerator {
    private val qrCache = mutableMapOf<Pair<String, Int>, ImageBitmap>()

    actual fun generateQrBitmap(content: String, sizePx: Int): ImageBitmap? {
        if (content.isBlank()) return null
        val cacheKey = content to sizePx
        qrCache[cacheKey]?.let { return it }

        return try {
            val hints = mapOf<EncodeHintType, Any>(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 4,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            val imageBitmap = bitmap.asImageBitmap()
            qrCache[cacheKey] = imageBitmap
            imageBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
