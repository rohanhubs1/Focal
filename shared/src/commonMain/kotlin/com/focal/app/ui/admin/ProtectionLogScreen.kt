package com.focal.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.protection.DecisionLogEntry
import com.focal.app.protection.DecisionType
import com.focal.app.protection.ProtectionStateRepository
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.FocalButton
import com.focal.app.ui.components.FocalCard
import com.focal.app.ui.components.PlatformPermissionsHelper

@Composable
fun ProtectionLogScreen() {
    val state by ProtectionStateRepository.instance.state.collectAsState()
    val logs = state.lastDecisions

    var copyMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Top Header + Copy Log Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Protection Log",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Last ${logs.size} engine decisions (Rules a–i)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF8A8A8A)
                    )
                )
            }

            FocalButton(
                text = "Copy Log",
                onClick = {
                    val formatted = logs.joinToString("\n") { log ->
                        "[${log.ruleLetter}] ${log.decisionType} | pkg=${log.packageName} | reason=${log.reason} | action=${log.actionTaken}"
                    }
                    PlatformPermissionsHelper.copyToClipboard(formatted)
                    copyMessage = "Copied ${logs.size} log entries!"
                },
                modifier = Modifier.width(110.dp)
            )
        }

        copyMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Live Diagnostics Panel
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Live Protection Diagnostics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                val isAccessOn = PlatformPermissionsHelper.isAccessibilityEnabled()
                val isOverlayOn = PlatformPermissionsHelper.isOverlayEnabled()
                val isAdminOn = PlatformPermissionsHelper.isDeviceAdminActive()
                val lastLog = logs.firstOrNull()

                Text("• Accessibility (Component Match): ${if (isAccessOn) "CONNECTED ✅" else "OFF / DISCONNECTED ❌"}", color = if (isAccessOn) Color.White else Color(0xFFFF3B30), fontSize = 12.sp)
                Text("• Overlay Permission: ${if (isOverlayOn) "GRANTED ✅" else "MISSING ❌"}", color = Color.White, fontSize = 12.sp)
                Text("• Device Admin Active: ${if (isAdminOn) "ACTIVE ✅" else "INACTIVE ❌"}", color = Color.White, fontSize = 12.sp)
                Text("• Focus Active: ${state.focusActive} (ends in ${((state.focusEndsAtElapsed - com.focal.app.model.currentTimeMillis()) / 1000).coerceAtLeast(0)}s)", color = Color.White, fontSize = 12.sp)
                Text("• Block List Size: ${state.policy.blockedApps.size} apps, ${state.policy.blockedWebsites.size} websites", color = Color.White, fontSize = 12.sp)
                Text("• Last Decision: ${lastLog?.let { "[${it.ruleLetter}] ${it.decisionType} on ${it.packageName}" } ?: "None"}", color = Color.White, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                FocalButton(
                    text = "Test Block Now",
                    onClick = {
                        val testDecision = com.focal.app.protection.ProtectionDecision.BlockApp(
                            packageName = "com.google.android.youtube",
                            appName = "YouTube (Test Block)",
                            ruleLetter = 'g',
                            reason = "Manual Test Block from DEV menu"
                        )
                        val dummyInfo = com.focal.app.protection.WindowInfo(packageName = "com.google.android.youtube")
                        com.focal.app.protection.ProtectionStateRepository.instance.recordDecision(
                            com.focal.app.protection.DecisionLogEntry(
                                timestampMs = com.focal.app.model.currentTimeMillis(),
                                packageName = "com.google.android.youtube",
                                windowClass = "",
                                decisionType = com.focal.app.protection.DecisionType.BLOCK_APP,
                                ruleLetter = 'g',
                                reason = "Manual Test Block from DEV menu",
                                actionTaken = "Test Overlay Displayed"
                            )
                        )
                        // Trigger test block overlay
                        val focusEngine = com.focal.app.engine.FocusEngine.instance
                        focusEngine.triggerBlockedAppAttempt("YouTube (Test Block)")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No decision log entries recorded yet",
                    color = Color(0xFF8A8A8A),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    DecisionLogItemCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun DecisionLogItemCard(log: DecisionLogEntry) {
    val isAllow = log.decisionType == DecisionType.ALLOW

    val badgeBg = if (isAllow) Color(0xFF1E1E1E) else Color.White
    val badgeFg = if (isAllow) Color.White else Color.Black

    FocalCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rule Badge [Rule Letter]
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "[Rule ${log.ruleLetter}] ${log.decisionType}",
                        color = badgeFg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = log.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Reason: ${log.reason}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF8A8A8A),
                    fontSize = 12.sp
                )
            )

            Text(
                text = "Action: ${log.actionTaken}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
