package com.focal.app.ui.components

import androidx.compose.ui.graphics.ImageBitmap

expect object PlatformQrGenerator {
    /**
     * Generates a real black-on-white QR bitmap with ZXing.
     * Error correction level M, 4 quiet zone margin modules.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): ImageBitmap?
}
