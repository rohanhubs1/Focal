package com.focal.app.ui.components

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeBase64PngToImageBitmap(base64Png: String): ImageBitmap?
