package com.focal.app.ui.components

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler

@Composable
actual fun FocalBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
