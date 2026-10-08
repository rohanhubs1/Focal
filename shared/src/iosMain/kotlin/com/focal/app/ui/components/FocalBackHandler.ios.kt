package com.focal.app.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun FocalBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for iOS platform
}
