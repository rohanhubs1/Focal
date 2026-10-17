package com.focal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.focal.app.theme.FocalColors

@Composable
actual fun PlatformCameraScanner(
    modifier: Modifier,
    torchEnabled: Boolean,
    isScanning: Boolean,
    onQrDetected: (String) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Camera scanner ready",
            style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
        )
    }
}
