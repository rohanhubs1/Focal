package com.focal.app.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun FocalBackHandler(enabled: Boolean = true, onBack: () -> Unit)
