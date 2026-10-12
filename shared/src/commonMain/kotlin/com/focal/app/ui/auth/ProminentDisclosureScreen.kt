package com.focal.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.data.FocalLocalStorage
import com.focal.app.model.currentTimeMillis
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.*

@Composable
fun ProminentDisclosureScreen(
    onAgree: () -> Unit,
    onCancel: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isScrolledToBottom by remember { mutableStateOf(false) }

    // Check if scrolled to the end (or content is small enough to fit)
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue == 0 || scrollState.value >= (scrollState.maxValue - 20)) {
            isScrolledToBottom = true
        }
    }

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF333333))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!isScrolledToBottom) {
                        Text(
                            text = "Scroll to read all terms before continuing",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocalColors.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    PrimaryButton(
                        text = "I agree, I'm the parent",
                        enabled = isScrolledToBottom,
                        onClick = {
                            val timestamp = currentTimeMillis().toString()
                            FocalLocalStorage.saveString("KEY_DISCLOSURE_AGREED_TIMESTAMP", timestamp)
                            FocalLocalStorage.saveBoolean("KEY_DISCLOSURE_AGREED", true)
                            onAgree()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SecondaryButton(
                        text = "Not now",
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            StatusBadge(text = "BEFORE YOU CONTINUE", badgeType = BadgeType.ACTIVE)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "How FOCAL protects your child's phone",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Please review how FOCAL functions on your child's phone before enabling system permissions.",
                style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Accessibility Service
            FocalCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
                Column {
                    Text(
                        text = "1. How FOCAL uses Accessibility",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FOCAL uses Android's Accessibility Service to:\n" +
                                "• Detect when a blocked app or website is opened during active focus sessions.\n" +
                                "• Display the calm focus screen shield over distracting apps.\n" +
                                "• Prevent your child from force-stopping FOCAL, turning off protection, or clearing storage without parent authorization.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, lineHeight = 18.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Uninstall Protection
            FocalCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
                Column {
                    Text(
                        text = "2. Uninstall protection",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FOCAL uses Device Admin privileges strictly to prevent unauthorized app removal while focus routines are active.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, lineHeight = 18.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: What data we collect
            FocalCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
                Column {
                    Text(
                        text = "3. What data we collect",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• FOCAL does not read personal messages, typed text, photos, or private screen content.\n" +
                                "• Information shared with the linked parent account includes: list of installed apps, active focus session minutes, block attempt counts, and protection status.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, lineHeight = 18.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
