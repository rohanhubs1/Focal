package com.focal.app.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeBase64PngToImageBitmap(base64Png: String): ImageBitmap? {
    if (base64Png.isBlank()) return null
    return try {
        val bytes = Base64.decode(base64Png, Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bmp?.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
