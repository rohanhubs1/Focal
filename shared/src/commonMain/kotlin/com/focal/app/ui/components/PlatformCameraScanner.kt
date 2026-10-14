package com.focal.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformCameraScanner(
    modifier: Modifier = Modifier,
    torchEnabled: Boolean = false,
    onQrDetected: (String) -> Unit
)
