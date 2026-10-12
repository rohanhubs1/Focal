package com.focal.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.data.FocalRepository
import com.focal.app.model.*
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.*

// -------------------------------------------------------------
// Super Admin Console Screen (Strict Monochrome Design System)
// -------------------------------------------------------------
@Composable
fun AdminDashboardScreen(
    repository: FocalRepository = FocalRepository.instance
) {
    var activeAdminTab by remember { mutableStateOf("Overview") }
    var showProgrammerModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Header: FOCAL Title + Admin Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FOCAL",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocalColors.TextPrimary,
                        letterSpacing = 2.sp
                    )
                )
                Text(
                    text = "Admin Console & Operations",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocalColors.TextSecondary,
                        fontSize = 13.sp
                    )
                )
            }
            StatusBadge(
                text = "ADMIN",
                badgeType = BadgeType.IDLE
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monochrome Tab Bar (Overview, Users, Devices, Cards, Revenue)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0E0E0E))
                .border(1.dp, Color(0xFF1F1F1F), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Overview", "Users", "Devices", "Cards", "Revenue", "Log").forEach { tab ->
                val isSelected = activeAdminTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { activeAdminTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else FocalColors.TextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Tab Screen Content
        when (activeAdminTab) {
            "Overview" -> AdminOverviewTab(repository = repository)
            "Users" -> AdminUsersTab(repository = repository)
            "Devices" -> AdminDevicesTab(repository = repository)
            "Cards" -> AdminCardsTab(
                repository = repository,
                onProgramCardsClick = { showProgrammerModal = true }
            )
            "Revenue" -> AdminRevenueTab(repository = repository)
            "Log" -> ProtectionLogScreen()
        }
    }

    if (showProgrammerModal) {
        BatchCardProgrammerModal(
            onDismiss = { showProgrammerModal = false },
            repository = repository
        )
    }
}

// -------------------------------------------------------------
// 1. Overview Tab
// -------------------------------------------------------------
@Composable
fun AdminOverviewTab(repository: FocalRepository) {
    val parentsCount = repository.parentAccounts.size
    val childrenCount = repository.children.size
    val activeFocusCount = repository.children.count { it.isFocusing }
    val cardStockCount = repository.cards.count { it.status == CardStatus.IN_STOCK || it.status == CardStatus.REGISTERED || it.status == CardStatus.PROGRAMMED || it.status == CardStatus.UNPROGRAMMED }

    val protectionOffCount = repository.adminDevices.count { !it.isProtectionActive }
    val offlineCount = repository.adminDevices.count { !it.isOnline }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 2-Column Stat Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    label = "Total Parents",
                    value = parentsCount.toString(),
                    deltaCaption = "Registered accounts"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    label = "Total Children",
                    value = childrenCount.toString(),
                    deltaCaption = "Paired profiles"
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    label = "Active Focus",
                    value = activeFocusCount.toString(),
                    deltaCaption = "Sessions running"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    label = "Card Stock",
                    value = cardStockCount.toString(),
                    deltaCaption = "Available NFC cards"
                )
            }
        }

        // System Health Strip
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "System Health Status",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocalColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (protectionOffCount == 0) Color.White else Color(0xFFFF453A))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Protection Off Alert",
                            style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                        )
                    }
                    Text(
                        text = "$protectionOffCount devices",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (protectionOffCount > 0) Color(0xFFFF453A) else FocalColors.TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF1F1F1F))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (offlineCount == 0) Color.White else Color(0xFFFF453A))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline > 30 mins",
                            style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                        )
                    }
                    Text(
                        text = "$offlineCount devices",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (offlineCount > 0) Color(0xFFFF453A) else FocalColors.TextPrimary
                        )
                    )
                }
            }
        }

        // Session Triggers Breakdown
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Focus Session Triggers",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocalColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                val triggers = listOf(
                    Triple("NFC Tap", 0.65f, "65%"),
                    Triple("Remote Start", 0.20f, "20%"),
                    Triple("Schedule", 0.10f, "10%"),
                    Triple("QR Scan", 0.05f, "5%")
                )

                triggers.forEach { (label, ratio, percentStr) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                            )
                            Text(
                                text = percentStr,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FocalColors.TextPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1F1F1F))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(ratio)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }

        // 7-Day New Signups Chart
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "New Signups (Last 7 Days)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocalColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                val dailyData = listOf(
                    "Mon" to 12, "Tue" to 18, "Wed" to 15, "Thu" to 22,
                    "Fri" to 28, "Sat" to 34, "Sun" to 40
                )
                val maxVal = 40

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyData.forEach { (day, count) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocalColors.TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight((count.toFloat() / maxVal).coerceIn(0.1f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocalColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Audit Log Section
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Recent Admin Actions (Audit Log)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocalColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (repository.auditLogs.isEmpty()) {
                    EmptyState(
                        title = "No Audit Records",
                        message = "Admin actions will appear here once performed."
                    )
                } else {
                    repository.auditLogs.take(50).forEachIndexed { idx, log ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.action,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = FocalColors.TextPrimary
                                    )
                                )
                                Text(
                                    text = log.timestamp,
                                    style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Target: ${log.target} · Admin: ${log.adminId}",
                                style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                            )
                        }
                        if (idx < repository.auditLogs.take(50).lastIndex) {
                            HorizontalDivider(color = Color(0xFF1F1F1F))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Users Tab
// -------------------------------------------------------------
@Composable
fun AdminUsersTab(repository: FocalRepository) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedParent by remember { mutableStateOf<ParentAccount?>(null) }

    val filteredParents = repository.parentAccounts.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FocalTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Search",
            placeholder = "Search parent by name or email...",
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredParents.isEmpty()) {
            EmptyState(
                title = "No Parent Accounts Found",
                message = "Try adjusting your search criteria."
            )
        } else {
            filteredParents.forEach { parent ->
                val children = repository.children.filter { parent.childrenIds.contains(it.id) }

                FocalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedParent = parent }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1F1F1F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = parent.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = parent.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FocalColors.TextPrimary
                                )
                            )
                            Text(
                                text = parent.email,
                                style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${children.size} Children · Joined ${parent.registeredDate}",
                                style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                            )
                        }

                        StatusBadge(
                            text = when (parent.subscriptionStatus) {
                                ParentSubscriptionStatus.TRIAL_3_MONTHS -> "Trial"
                                ParentSubscriptionStatus.ACTIVE_MONTHLY -> "Active"
                                ParentSubscriptionStatus.SUSPENDED -> "Suspended"
                                ParentSubscriptionStatus.EXPIRED -> "Expired"
                            },
                            badgeType = when (parent.subscriptionStatus) {
                                ParentSubscriptionStatus.ACTIVE_MONTHLY -> BadgeType.ACTIVE
                                ParentSubscriptionStatus.SUSPENDED -> BadgeType.EXPIRED
                                else -> BadgeType.IDLE
                            }
                        )
                    }
                }
            }
        }
    }

    selectedParent?.let { parent ->
        ParentDetailModal(
            parent = parent,
            repository = repository,
            onDismiss = { selectedParent = null }
        )
    }
}

// -------------------------------------------------------------
// Parent Detail & Actions Modal
// -------------------------------------------------------------
@Composable
fun ParentDetailModal(
    parent: ParentAccount,
    repository: FocalRepository,
    onDismiss: () -> Unit
) {
    var showSuspendConfirm by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    val children = repository.children.filter { parent.childrenIds.contains(it.id) }
    val assignedCards = repository.cards.filter { card ->
        children.any { it.pairedCardId == card.cardId }
    }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Parent Account Detail",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = FocalColors.TextPrimary
                        )
                    )
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.labelLarge.copy(color = FocalColors.TextSecondary),
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = parent.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = parent.email,
                    style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF1F1F1F))
                Spacer(modifier = Modifier.height(12.dp))

                // Subscription details
                Text(
                    text = "Subscription Status",
                    style = MaterialTheme.typography.labelMedium.copy(color = FocalColors.TextSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${parent.subscriptionStatus.name} (${parent.trialDaysLeft} days trial remaining)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Children List
                Text(
                    text = "Linked Children (${children.size})",
                    style = MaterialTheme.typography.labelMedium.copy(color = FocalColors.TextSecondary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                children.forEach { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = child.name,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )
                        Text(
                            text = if (child.pairedCardId != null) "Card ${child.pairedCardId}" else "No Card",
                            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF1F1F1F))
                Spacer(modifier = Modifier.height(16.dp))

                // Admin Action Buttons
                Text(
                    text = "Admin Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                FocalButton(
                    text = "Extend Trial (+14 Days)",
                    onClick = {
                        repository.adminExtendTrial(parent.id)
                        actionMessage = "Extended trial by 14 days"
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (children.isNotEmpty()) {
                    SecondaryButton(
                        text = "Reset Child Pairing",
                        onClick = {
                            repository.adminResetChildPairing(children.first().id)
                            actionMessage = "Child device pairing reset"
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (assignedCards.isNotEmpty()) {
                    SecondaryButton(
                        text = "Revoke NFC Card (${assignedCards.first().cardId})",
                        onClick = {
                            repository.adminRevokeCard(assignedCards.first().cardId)
                            actionMessage = "Card revoked successfully"
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                DangerButton(
                    text = "Suspend Account",
                    onClick = { showSuspendConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                )

                actionMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    if (showSuspendConfirm) {
        Dialog(onDismissRequest = { showSuspendConfirm = false }) {
            FocalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Confirm Suspend Account?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF453A)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will immediately restrict ${parent.name}'s access to all parent and child controls.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SecondaryButton(
                                text = "Cancel",
                                onClick = { showSuspendConfirm = false },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            DangerButton(
                                text = "Suspend",
                                onClick = {
                                    repository.adminSuspendAccount(parent.id)
                                    showSuspendConfirm = false
                                    actionMessage = "Account suspended"
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Devices Tab
// -------------------------------------------------------------
@Composable
fun AdminDevicesTab(repository: FocalRepository) {
    var selectedFilterChip by remember { mutableStateOf("All") }

    val filteredDevices = repository.adminDevices.filter { device ->
        when (selectedFilterChip) {
            "Protection off" -> !device.isProtectionActive
            "Offline" -> !device.isOnline
            "Android" -> device.osVersion.contains("Android", ignoreCase = true)
            "iOS" -> device.osVersion.contains("iOS", ignoreCase = true)
            else -> true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Protection off", "Offline", "Android", "iOS").forEach { chip ->
                val isSelected = selectedFilterChip == chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color.White else Color(0xFF0E0E0E))
                        .border(1.dp, if (isSelected) Color.White else Color(0xFF1F1F1F), RoundedCornerShape(20.dp))
                        .clickable { selectedFilterChip = chip }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else FocalColors.TextSecondary
                        )
                    )
                }
            }
        }

        if (filteredDevices.isEmpty()) {
            EmptyState(
                title = "No Devices Match Filter",
                message = "All connected devices are operating within parameters."
            )
        } else {
            filteredDevices.forEach { device ->
                FocalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (device.isOnline && device.isProtectionActive) Color.White else Color(0xFFFF453A))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = device.deviceModel,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = FocalColors.TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Child: ${device.childName} · ${device.osVersion}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                    )
                                }
                            }

                            StatusBadge(
                                text = if (device.isOnline) "Online" else "Offline",
                                badgeType = if (device.isOnline) BadgeType.ACTIVE else BadgeType.EXPIRED
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFF1F1F1F))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Permission Dots Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PermissionStatusItem("Accessibility", device.accessibilityGranted)
                            PermissionStatusItem("Overlay", device.overlayGranted)
                            PermissionStatusItem("Usage", device.usageGranted)
                            PermissionStatusItem("Notification", device.notificationsGranted)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last Heartbeat: ${device.lastHeartbeat}",
                            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionStatusItem(label: String, isGranted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isGranted) Color.White else Color(0xFFFF453A))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = if (isGranted) FocalColors.TextSecondary else Color(0xFFFF453A)
            )
        )
    }
}

// -------------------------------------------------------------
// 4. Cards Tab
// -------------------------------------------------------------
@Composable
fun AdminCardsTab(
    repository: FocalRepository,
    onProgramCardsClick: () -> Unit
) {
    val totalCount = repository.cards.size
    val inStock = repository.cards.count { it.status == CardStatus.IN_STOCK || it.status == CardStatus.REGISTERED || it.status == CardStatus.PROGRAMMED || it.status == CardStatus.UNPROGRAMMED }
    val assigned = repository.cards.count { it.status == CardStatus.ASSIGNED || it.status == CardStatus.ACTIVE }
    val revoked = repository.cards.count { it.status == CardStatus.REVOKED || it.status == CardStatus.DEACTIVATED || it.status == CardStatus.BLOCKED }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Summary Card
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "In Stock",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                    )
                    Text(
                        text = inStock.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Column {
                    Text(
                        text = "Assigned",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                    )
                    Text(
                        text = assigned.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Column {
                    Text(
                        text = "Revoked",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                    )
                    Text(
                        text = revoked.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Action Button: Program Cards
        FocalButton(
            text = "+ Program New Batch of Cards",
            onClick = onProgramCardsClick,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Programmed NFC Hardware Inventory",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = FocalColors.TextPrimary
            )
        )

        // Cards Inventory List (Secure token display: FC0001 ···LM7)
        repository.cards.forEach { card ->
            val maskedToken = "···" + card.token.takeLast(4)
            val assignedChild = repository.children.find { it.pairedCardId == card.cardId }

            FocalCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = card.cardId,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = maskedToken,
                                style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (assignedChild != null) "Assigned to ${assignedChild.name}" else "Unassigned inventory",
                            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                        )
                    }

                    StatusBadge(
                        text = when (card.status) {
                            CardStatus.IN_STOCK, CardStatus.UNPROGRAMMED -> "In Stock"
                            CardStatus.REGISTERED, CardStatus.PROGRAMMED -> "Registered"
                            CardStatus.ASSIGNED, CardStatus.ACTIVE -> "Assigned"
                            CardStatus.REVOKED, CardStatus.BLOCKED, CardStatus.DEACTIVATED -> "Revoked"
                        },
                        badgeType = when (card.status) {
                            CardStatus.ASSIGNED, CardStatus.ACTIVE -> BadgeType.ACTIVE
                            CardStatus.REVOKED, CardStatus.DEACTIVATED, CardStatus.BLOCKED -> BadgeType.EXPIRED
                            else -> BadgeType.IDLE
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Batch Card Programmer Modal
// -------------------------------------------------------------
@Composable
fun BatchCardProgrammerModal(
    onDismiss: () -> Unit,
    repository: FocalRepository
) {
    var quantityText by remember { mutableStateOf("5") }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Batch NFC Card Programmer",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Generate and register secure token identifiers in card inventory.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                FocalTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = "Batch Quantity",
                    placeholder = "Enter batch quantity (e.g. 5)",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isSuccess) {
                    Text(
                        text = "Successfully programmed $quantityText cards!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FocalButton(
                        text = "Done",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SecondaryButton(
                                text = "Cancel",
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            FocalButton(
                                text = "Program",
                                onClick = {
                                    val count = quantityText.toIntOrNull() ?: 5
                                    repository.adminProgramBatchCards(count)
                                    isSuccess = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. Revenue Tab
// -------------------------------------------------------------
@Composable
fun AdminRevenueTab(repository: FocalRepository) {
    val payingParents = repository.parentAccounts.count { it.subscriptionStatus == ParentSubscriptionStatus.ACTIVE_MONTHLY }
    val mrr = payingParents * 299
    val activeTrials = repository.parentAccounts.count { it.subscriptionStatus == ParentSubscriptionStatus.TRIAL_3_MONTHS }
    val failedPayments = 0

    val hardwareUnitsSold = 12
    val totalHardwareSales = hardwareUnitsSold * 999

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Revenue StatCards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    label = "MRR",
                    value = "₹$mrr",
                    deltaCaption = "Monthly Recurring"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    label = "Paying Parents",
                    value = payingParents.toString(),
                    deltaCaption = "₹299/mo subscribers"
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    label = "Active Trials",
                    value = activeTrials.toString(),
                    deltaCaption = "3-Month free trial"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    label = "Conversion %",
                    value = "18.5%",
                    deltaCaption = "Trial to paid"
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    label = "Churn Rate",
                    value = "1.2%",
                    deltaCaption = "Monthly cancel %"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FocalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Failed Payments",
                            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = failedPayments.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (failedPayments > 0) Color(0xFFFF453A) else Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Card retry queue",
                            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                        )
                    }
                }
            }
        }

        // Hardware Card Sales Section
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "NFC Hardware Card Sales",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Units Sold",
                            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                        )
                        Text(
                            text = "$hardwareUnitsSold cards",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Hardware Revenue",
                            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                        )
                        Text(
                            text = "₹$totalHardwareSales",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "* One-time ₹999 card purchases are recorded separately from MRR.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = FocalColors.TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // 6-Month Revenue Trend Chart
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "6-Month Revenue Trend (₹)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FocalColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                val monthlyRev = listOf(
                    "Apr" to 42000, "May" to 58000, "Jun" to 71000,
                    "Jul" to 85000, "Aug" to 94000, "Sep" to 112000
                )
                val maxRev = 112000

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyRev.forEach { (month, amount) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "₹${amount / 1000}k",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocalColors.TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .fillMaxHeight((amount.toFloat() / maxRev).coerceIn(0.1f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = month,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FocalColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
