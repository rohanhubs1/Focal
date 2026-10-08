package com.focal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.SessionState
import com.focal.app.model.UserRole
import com.focal.app.theme.FocalColors


@Composable
fun RoleSwitcherModal(
    onDismiss: () -> Unit,
    onRoleSelected: (UserRole) -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEV",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Debug Role Switcher",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "CURRENT CONTEXT",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Role: ${repository.currentUser.role.name} • Account: ${repository.currentUser.name} (${repository.currentUser.email.ifEmpty { "Child Device" }})",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Options
                RoleSelectCard(
                    title = "Parent Controller",
                    subtitle = "Priya Sharma (Mother) • 2 children paired • Manager access",
                    isSelected = repository.currentUser.role == UserRole.CONTROLLER,
                    onClick = {
                        repository.switchUserRole(UserRole.CONTROLLER)
                        onRoleSelected(UserRole.CONTROLLER)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                RoleSelectCard(
                    title = "Child Device",
                    subtitle = "Rahul Sharma • Paired OnePlus 11 • NFC FC0001",
                    isSelected = repository.currentUser.role == UserRole.CHILD,
                    onClick = {
                        repository.switchChild(repository.controllerUser)
                        repository.currentUser = repository.currentUser.copy(role = UserRole.CHILD, name = "Rahul Sharma")
                        onRoleSelected(UserRole.CHILD)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                RoleSelectCard(
                    title = "Admin Console",
                    subtitle = "Aman Verma (Admin) • Platform operations & SaaS metrics",
                    isSelected = repository.currentUser.role == UserRole.ADMIN,
                    onClick = {
                        repository.switchUserRole(UserRole.ADMIN)
                        onRoleSelected(UserRole.ADMIN)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                SecondaryButton(
                    text = "Reset Test Data",
                    onClick = {
                        repository.isProtectionDisabledOnChild = false
                        repository.protectionDisabledReason = ""
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RoleSelectCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FocalCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isSelected) Color.White else FocalColors.Border,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary))
            }
            if (isSelected) {
                Icon(imageVector = FocalIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
