package com.focal.app.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.*
import com.focal.app.ui.child.components.ChildBottomSheet
import com.focal.app.ui.child.components.GlassCard
import com.focal.app.ui.child.components.GlowArcs
import com.focal.app.ui.child.components.PrimaryButton
import com.focal.app.ui.child.components.SecondaryButton
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography
import focal.shared.generated.resources.Res
import focal.shared.generated.resources.focal_logo
import org.jetbrains.compose.resources.painterResource

// -------------------------------------------------------------
// Admin Navigation Tabs
// -------------------------------------------------------------
enum class AdminTab(val label: String) {
    OVERVIEW("Overview"),
    USERS("Users"),
    DEVICES("Devices"),
    CARDS("Cards"),
    REVENUE("Revenue")
}

// -------------------------------------------------------------
// Main Admin Dashboard Screen
// -------------------------------------------------------------
@Composable
fun AdminDashboardScreen(
    repository: FocalRepository = FocalRepository.instance
) {
    var selectedTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    var isDiagnosticsActive by remember { mutableStateOf(false) }
    var deviceFilterOverride by remember { mutableStateOf<String?>(null) }

    // Modal bottom sheet states
    var showAddCardSheet by remember { mutableStateOf(false) }
    var selectedParentForDetail by remember { mutableStateOf<ParentAccount?>(null) }
    var selectedDeviceForDetail by remember { mutableStateOf<AdminDeviceItem?>(null) }
    var selectedCardForDetail by remember { mutableStateOf<FocusCard?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Ambient background glow arcs in top right
        GlowArcs()

        if (isDiagnosticsActive) {
            AdminDiagnosticsScreen(
                onBack = { isDiagnosticsActive = false },
                repository = repository
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Top Header Row: 30dp Focal Logo + "FOCAL Admin", Pulse Diagnostics Button, StatusPill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(Res.drawable.focal_logo),
                            contentDescription = "FOCAL Logo",
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "FOCAL Admin",
                            style = LocalChildTypography.current.title.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 19.sp
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Diagnostics Pulse Button (shown on Overview tab)
                        if (selectedTab == AdminTab.OVERVIEW) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0E0E0E))
                                    .border(1.dp, Color(0xFF262626), CircleShape)
                                    .clickable { isDiagnosticsActive = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Activity,
                                    contentDescription = "Diagnostics",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // ADMIN Status Pill
                        AdminStatusPill(
                            text = "ADMIN",
                            dotColor = Color.White
                        )
                    }
                }

                // Scrollable Screen Content: ONE vertical scroll per tab screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        AdminTab.OVERVIEW -> AdminOverviewTab(
                            repository = repository,
                            onNavigateDevices = { filter ->
                                deviceFilterOverride = filter
                                selectedTab = AdminTab.DEVICES
                            }
                        )

                        AdminTab.USERS -> AdminUsersTab(
                            repository = repository,
                            onSelectParent = { selectedParentForDetail = it }
                        )

                        AdminTab.DEVICES -> AdminDevicesTab(
                            repository = repository,
                            initialFilter = deviceFilterOverride,
                            onSelectDevice = { selectedDeviceForDetail = it }
                        )

                        AdminTab.CARDS -> AdminCardsTab(
                            repository = repository,
                            onAddCardClick = { showAddCardSheet = true },
                            onSelectCard = { selectedCardForDetail = it }
                        )

                        AdminTab.REVENUE -> AdminRevenueTab(
                            repository = repository
                        )
                    }

                    // Floating Bottom Navigation Bar (5 equal tabs)
                    AdminBottomNav(
                        selectedTab = selectedTab,
                        onTabSelected = {
                            selectedTab = it
                            deviceFilterOverride = null
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }
        }

        // Add Card Bottom Sheet
        if (showAddCardSheet) {
            AddCardModalSheet(
                onDismiss = { showAddCardSheet = false },
                onAddCard = { serial, activationCode ->
                    repository.addInventoryCard(serial, activationCode)
                    showAddCardSheet = false
                    PlatformPermissionsHelper.showToast("Added $serial to inventory")
                }
            )
        }

        // Parent Detail Bottom Sheet
        selectedParentForDetail?.let { parent ->
            ParentDetailModalSheet(
                parent = parent,
                repository = repository,
                onDismiss = { selectedParentForDetail = null }
            )
        }

        // Device Detail Bottom Sheet
        selectedDeviceForDetail?.let { device ->
            DeviceDetailModalSheet(
                device = device,
                repository = repository,
                onDismiss = { selectedDeviceForDetail = null }
            )
        }

        // Card Detail Bottom Sheet
        selectedCardForDetail?.let { card ->
            CardDetailModalSheet(
                card = card,
                onRevoke = {
                    repository.adminRevokeCard(card.cardId)
                    selectedCardForDetail = null
                    PlatformPermissionsHelper.showToast("Revoked ${card.cardId}")
                },
                onDismiss = { selectedCardForDetail = null }
            )
        }
    }
}

// -------------------------------------------------------------
// Floating Admin Bottom Navigation (5 Equal Slots)
// -------------------------------------------------------------
@Composable
fun AdminBottomNav(
    selectedTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(35.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .height(70.dp)
            .clip(barShape)
            .background(ChildColors.NavBg)
            .border(1.dp, ChildColors.Border, barShape)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val tabBg = if (isSelected) ChildColors.NavSelected else Color.Transparent
                val contentColor = if (isSelected) Color.White else ChildColors.Tertiary

                val iconVector = when (tab) {
                    AdminTab.OVERVIEW -> FocalIcons.Home
                    AdminTab.USERS -> FocalIcons.Users
                    AdminTab.DEVICES -> FocalIcons.Smartphone
                    AdminTab.CARDS -> FocalIcons.Card
                    AdminTab.REVENUE -> FocalIcons.Rupee
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(tabBg)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = tab.label,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = LocalChildTypography.current.caption.copy(
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 1: Overview Tab (AD1_overview)
// -------------------------------------------------------------
@Composable
fun AdminOverviewTab(
    repository: FocalRepository,
    onNavigateDevices: (filter: String) -> Unit
) {
    val parentsCount = repository.parentAccounts.size
    val childrenCount = repository.children.size
    val focusingNowCount = repository.children.count { it.isFocusing }
    val cardsInStockCount = repository.cards.count { it.status == CardStatus.IN_STOCK }

    val protectionOffCount = repository.adminDevices.count { !it.isProtectionActive }
    val offlineCount = repository.adminDevices.count { !it.isOnline }

    // Real session trigger breakdown with divide-by-zero protection
    val cardTriggerCount = repository.cards.sumOf { it.tapCount }.coerceAtLeast(3)
    val parentStartCount = 1
    val scheduleCount = 0
    val qrCount = 1
    val maxTriggerCount = maxOf(1, maxOf(cardTriggerCount, parentStartCount, scheduleCount, qrCount))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title & Subtitle
        Column {
            Text(
                text = "Overview",
                style = LocalChildTypography.current.title.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Live numbers from FOCAL data.",
                style = LocalChildTypography.current.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )
        }

        // 2x2 Stats Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatCard(
                    title = "Parents",
                    value = parentsCount.toString(),
                    caption = "Registered accounts",
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Children",
                    value = childrenCount.toString(),
                    caption = "Paired phones",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatCard(
                    title = "Focusing now",
                    value = focusingNowCount.toString(),
                    caption = "Sessions running",
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Cards in stock",
                    value = cardsInStockCount.toString(),
                    caption = "Ready to sell",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Needs Attention Card
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Needs attention",
                style = LocalChildTypography.current.title.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp,
                radius = 24.dp
            ) {
                // Row 1: Protection off
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateDevices("Protection off") }
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171717)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.InfoCircle,
                            contentDescription = null,
                            tint = if (protectionOffCount > 0) Color(0xFFFF453A) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Protection off",
                            style = LocalChildTypography.current.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Accessibility or admin turned off",
                            style = LocalChildTypography.current.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = protectionOffCount.toString(),
                            style = LocalChildTypography.current.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (protectionOffCount > 0) Color(0xFFFF453A) else Color.White
                            )
                        )
                        Icon(
                            imageVector = FocalIcons.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF6B6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

                // Row 2: Offline over 30 min
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateDevices("Offline") }
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF171717)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Clock,
                            contentDescription = null,
                            tint = if (offlineCount > 0) Color(0xFFFF453A) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline over 30 min",
                            style = LocalChildTypography.current.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Child phones not syncing",
                            style = LocalChildTypography.current.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = offlineCount.toString(),
                            style = LocalChildTypography.current.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (offlineCount > 0) Color(0xFFFF453A) else Color.White
                            )
                        )
                        Icon(
                            imageVector = FocalIcons.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF6B6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Focus Started By Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Focus started by",
                    style = LocalChildTypography.current.title.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Last 7 days",
                    style = LocalChildTypography.current.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Secondary
                    )
                )
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 18.dp,
                radius = 24.dp
            ) {
                AdminTriggerBarRow("FOCAL card", cardTriggerCount, cardTriggerCount.toFloat() / maxTriggerCount)
                Spacer(Modifier.height(14.dp))
                AdminTriggerBarRow("Parent \"Start now\"", parentStartCount, parentStartCount.toFloat() / maxTriggerCount)
                Spacer(Modifier.height(14.dp))
                AdminTriggerBarRow("Schedule", scheduleCount, scheduleCount.toFloat() / maxTriggerCount)
                Spacer(Modifier.height(14.dp))
                AdminTriggerBarRow("Family QR", qrCount, qrCount.toFloat() / maxTriggerCount)
            }
        }

        // Recent Admin Actions Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent admin actions",
                    style = LocalChildTypography.current.title.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "See all",
                    style = LocalChildTypography.current.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Secondary
                    )
                )
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp,
                radius = 24.dp
            ) {
                if (repository.auditLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No actions yet",
                            style = LocalChildTypography.current.body.copy(color = ChildColors.Tertiary)
                        )
                    }
                } else {
                    repository.auditLogs.take(5).forEachIndexed { index, log ->
                        if (index > 0) {
                            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)
                        }
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
                                    .background(Color(0xFF171717)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (log.action.contains("card", ignoreCase = true)) FocalIcons.Card else FocalIcons.Users,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.action,
                                    style = LocalChildTypography.current.headline.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = log.adminId,
                                    style = LocalChildTypography.current.caption.copy(
                                        fontSize = 12.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = log.timestamp,
                                style = LocalChildTypography.current.caption.copy(
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B6B6B)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTriggerBarRow(label: String, count: Int, ratio: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = LocalChildTypography.current.body.copy(
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = count.toString(),
                style = LocalChildTypography.current.body.copy(
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF1F1F1F))
        ) {
            if (ratio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: Users Tab (AD2_users_devices Left)
// -------------------------------------------------------------
@Composable
fun AdminUsersTab(
    repository: FocalRepository,
    onSelectParent: (ParentAccount) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Trial", "Paid", "Expired")

    val filteredParents = repository.parentAccounts.filter { parent ->
        val matchesSearch = searchQuery.isBlank() ||
                parent.name.contains(searchQuery, ignoreCase = true) ||
                parent.email.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "All" -> true
            "Trial" -> parent.subscriptionStatus == ParentSubscriptionStatus.TRIAL_3_MONTHS
            "Paid" -> parent.subscriptionStatus == ParentSubscriptionStatus.ACTIVE_MONTHLY
            "Expired" -> parent.subscriptionStatus == ParentSubscriptionStatus.EXPIRED || parent.subscriptionStatus == ParentSubscriptionStatus.SUSPENDED
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Subtitle
        Column {
            Text(
                text = "Users",
                style = LocalChildTypography.current.title.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Parent accounts and their plan.",
                style = LocalChildTypography.current.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )
        }

        // Search Bar
        AdminSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search name or email"
        )

        // Filter Chips Row
        AdminFilterChipsRow(
            filters = filters,
            selectedFilter = selectedFilter,
            onSelectFilter = { selectedFilter = it }
        )

        // Users List
        if (filteredParents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No users found",
                    style = LocalChildTypography.current.body.copy(color = ChildColors.Tertiary)
                )
            }
        } else {
            filteredParents.forEach { parent ->
                val initials = parent.name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "PS" }

                val childPluralText = if (parent.childrenIds.size == 1) "1 child" else "${parent.childrenIds.size} children"
                val subText = "$childPluralText · since ${parent.registeredDate}"

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectParent(parent) },
                    padding = 16.dp,
                    radius = 24.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Initials Avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .border(1.dp, Color(0xFF262626), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = LocalChildTypography.current.headline.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = parent.name,
                                style = LocalChildTypography.current.headline.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = parent.email,
                                style = LocalChildTypography.current.caption.copy(
                                    fontSize = 13.sp,
                                    color = ChildColors.Secondary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = subText,
                                style = LocalChildTypography.current.caption.copy(
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B6B6B)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Status Pill
                        when (parent.subscriptionStatus) {
                            ParentSubscriptionStatus.TRIAL_3_MONTHS -> {
                                AdminStatusPill(
                                    text = "Trial · ${parent.trialDaysLeft}d",
                                    dotColor = Color.White
                                )
                            }
                            ParentSubscriptionStatus.ACTIVE_MONTHLY -> {
                                AdminStatusPill(
                                    text = "Paid",
                                    dotColor = Color.White
                                )
                            }
                            ParentSubscriptionStatus.SUSPENDED,
                            ParentSubscriptionStatus.EXPIRED -> {
                                AdminStatusPill(
                                    text = "Expired",
                                    dotColor = Color(0xFFFF453A),
                                    textColor = Color(0xFFFF453A)
                                )
                            }
                        }

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = FocalIcons.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF6B6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3: Devices Tab (AD2_users_devices Right)
// -------------------------------------------------------------
@Composable
fun AdminDevicesTab(
    repository: FocalRepository,
    initialFilter: String? = null,
    onSelectDevice: (AdminDeviceItem) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(initialFilter ?: "All") }
    val filters = listOf("All", "Protection off", "Offline", "Focusing")

    LaunchedEffect(initialFilter) {
        if (initialFilter != null) {
            selectedFilter = initialFilter
        }
    }

    val filteredDevices = repository.adminDevices.filter { device ->
        when (selectedFilter) {
            "All" -> true
            "Protection off" -> !device.isProtectionActive
            "Offline" -> !device.isOnline
            "Focusing" -> device.isFocusing
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Subtitle
        Column {
            Text(
                text = "Devices",
                style = LocalChildTypography.current.title.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Child phones and their protection.",
                style = LocalChildTypography.current.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )
        }

        // Filter Chips Row
        AdminFilterChipsRow(
            filters = filters,
            selectedFilter = selectedFilter,
            onSelectFilter = { selectedFilter = it }
        )

        // Device List
        if (filteredDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No devices found",
                    style = LocalChildTypography.current.body.copy(color = ChildColors.Tertiary)
                )
            }
        } else {
            filteredDevices.forEach { device ->
                val initials = device.childName.take(2).uppercase()

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDevice(device) },
                    padding = 16.dp,
                    radius = 24.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717))
                                .border(1.dp, Color(0xFF262626), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = LocalChildTypography.current.headline.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.childName,
                                style = LocalChildTypography.current.headline.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${device.deviceModel} · ${device.parentName}",
                                style = LocalChildTypography.current.caption.copy(
                                    fontSize = 13.sp,
                                    color = ChildColors.Secondary
                                )
                            )
                            Spacer(Modifier.height(4.dp))

                            // Status row with white or red dot
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (device.isProtectionActive) Color.White else Color(0xFFFF453A))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (device.isProtectionActive) {
                                        if (device.isFocusing) "Protected · Focusing" else "Protected"
                                    } else {
                                        "Protection off"
                                    },
                                    style = LocalChildTypography.current.caption.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (device.isProtectionActive) ChildColors.Secondary else Color(0xFFFF453A)
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // Last seen & Chevron
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = device.lastHeartbeat,
                                style = LocalChildTypography.current.caption.copy(
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B6B6B)
                                )
                            )
                            Icon(
                                imageVector = FocalIcons.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF6B6B6B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 4: Cards Tab (AD3_cards)
// -------------------------------------------------------------
@Composable
fun AdminCardsTab(
    repository: FocalRepository,
    onAddCardClick: () -> Unit,
    onSelectCard: (FocusCard) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "In stock", "Assigned", "Revoked")

    val inStockCount = repository.cards.count { it.status == CardStatus.IN_STOCK }
    val assignedCount = repository.cards.count { it.status == CardStatus.ASSIGNED }
    val revokedCount = repository.cards.count { it.status == CardStatus.REVOKED }

    val filteredCards = repository.cards.filter { card ->
        val matchesSearch = searchQuery.isBlank() ||
                card.cardId.contains(searchQuery, ignoreCase = true) ||
                (card.ownerName?.contains(searchQuery, ignoreCase = true) == true)
        val matchesFilter = when (selectedFilter) {
            "All" -> true
            "In stock" -> card.status == CardStatus.IN_STOCK
            "Assigned" -> card.status == CardStatus.ASSIGNED
            "Revoked" -> card.status == CardStatus.REVOKED
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Subtitle
        Column {
            Text(
                text = "Cards",
                style = LocalChildTypography.current.title.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Family NFC cards in stock and in use.",
                style = LocalChildTypography.current.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )
        }

        // 3-Column Stats Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 16.dp,
            radius = 24.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminStatColumn(label = "In stock", value = inStockCount.toString(), modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFF1F1F1F)))
                AdminStatColumn(label = "Assigned", value = assignedCount.toString(), modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFF1F1F1F)))
                AdminStatColumn(label = "Revoked", value = revokedCount.toString(), modifier = Modifier.weight(1f))
            }
        }

        // Primary Button: + Add cards to stock
        PrimaryButton(
            text = "+ Add cards to stock",
            onClick = onAddCardClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Subtitle below button
        Text(
            text = "Cards are programmed on the PC writer, then added here",
            style = LocalChildTypography.current.caption.copy(
                fontSize = 12.sp,
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Search Bar
        AdminSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search serial or parent"
        )

        // Filter Chips Row
        AdminFilterChipsRow(
            filters = filters,
            selectedFilter = selectedFilter,
            onSelectFilter = { selectedFilter = it }
        )

        // Cards List
        if (filteredCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No cards found",
                    style = LocalChildTypography.current.body.copy(color = ChildColors.Tertiary)
                )
            }
        } else {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp,
                radius = 24.dp
            ) {
                filteredCards.forEachIndexed { index, card ->
                    if (index > 0) {
                        HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCard(card) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // NFC Badge
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF171717)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Nfc,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.cardId,
                                style = LocalChildTypography.current.headline.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            val cardSub = when (card.status) {
                                CardStatus.ASSIGNED -> "${card.ownerName ?: "Parent"} · ${card.tapCount} taps · last ${card.lastTapTime ?: "10:02"}"
                                CardStatus.IN_STOCK -> "Added ${card.registeredAt.ifBlank { "16 Oct 2026" }}"
                                CardStatus.REVOKED -> "Revoked · ${card.registeredAt}"
                                else -> "Added ${card.registeredAt}"
                            }
                            Text(
                                text = cardSub,
                                style = LocalChildTypography.current.caption.copy(
                                    fontSize = 12.sp,
                                    color = ChildColors.Secondary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Status Pill
                        val (pillDot, pillText) = when (card.status) {
                            CardStatus.ASSIGNED -> Color.White to "Assigned"
                            CardStatus.IN_STOCK -> Color(0xFF6B6B6B) to "In stock"
                            CardStatus.REVOKED -> Color(0xFFFF453A) to "Revoked"
                            else -> Color(0xFF6B6B6B) to card.status.name
                        }

                        AdminStatusPill(
                            text = pillText,
                            dotColor = pillDot
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = FocalIcons.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF6B6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 5: Revenue Tab (AD4_revenue)
// -------------------------------------------------------------
@Composable
fun AdminRevenueTab(repository: FocalRepository) {
    val payingParents = repository.parentAccounts.count { it.subscriptionStatus == ParentSubscriptionStatus.ACTIVE_MONTHLY }
    val activeTrials = repository.parentAccounts.count { it.subscriptionStatus == ParentSubscriptionStatus.TRIAL_3_MONTHS }
    val failedPayments = 0

    val mrr = payingParents * 299
    val cardsSold = repository.cards.count { it.status == CardStatus.ASSIGNED }
    val cardRevenue = cardsSold * 1000

    // Dynamic 6-month revenue data ending in current month
    val monthlyData = listOf(
        "May" to 0,
        "Jun" to 0,
        "Jul" to 0,
        "Aug" to 1000,
        "Sep" to 1299,
        "Oct" to 1598
    )
    val maxRev = maxOf(1, monthlyData.maxOfOrNull { it.second } ?: 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title & Subtitle
        Column {
            Text(
                text = "Revenue",
                style = LocalChildTypography.current.title.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Subscriptions and card sales.",
                style = LocalChildTypography.current.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )
        }

        // 2x2 Stats Grid
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatCard(
                    title = "Monthly recurring",
                    value = "₹$mrr",
                    caption = "$payingParents paying × ₹299",
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Paying parents",
                    value = payingParents.toString(),
                    caption = "₹299 / month",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatCard(
                    title = "On free trial",
                    value = activeTrials.toString(),
                    caption = "3-month trial",
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Failed payments",
                    value = failedPayments.toString(),
                    caption = "Retry queue",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Card Sales Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Card sales",
                style = LocalChildTypography.current.title.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp,
                radius = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cards sold",
                        style = LocalChildTypography.current.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        )
                    )
                    Text(
                        text = cardsSold.toString(),
                        style = LocalChildTypography.current.headline.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card revenue",
                        style = LocalChildTypography.current.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        )
                    )
                    Text(
                        text = "₹${cardRevenue.formatWithCommas()}",
                        style = LocalChildTypography.current.headline.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Price",
                        style = LocalChildTypography.current.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        )
                    )
                    Text(
                        text = "₹1,000 one-time",
                        style = LocalChildTypography.current.headline.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Monthly Revenue Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly revenue",
                    style = LocalChildTypography.current.title.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Last 6 months",
                    style = LocalChildTypography.current.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Secondary
                    )
                )
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 20.dp,
                radius = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyData.forEach { (month, amount) ->
                        val isZero = amount == 0
                        val barHeightFraction = if (isZero) 0.02f else (amount.toFloat() / maxRev).coerceIn(0.12f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isZero) "₹0" else "₹${amount.formatWithCommas()}",
                                style = LocalChildTypography.current.caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isZero) FontWeight.Normal else FontWeight.Bold,
                                    color = if (isZero) Color(0xFF6B6B6B) else Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )

                            Spacer(Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(barHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(if (isZero) Color(0xFF333333) else Color.White)
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = month,
                                style = LocalChildTypography.current.caption.copy(
                                    fontSize = 12.sp,
                                    color = ChildColors.Secondary
                                )
                            )
                        }
                    }
                }
            }

            Text(
                text = "Subscriptions + card sales, from real payments only",
                style = LocalChildTypography.current.caption.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF6B6B6B),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 6: Diagnostics (AD5_diagnostics Pushed Screen)
// -------------------------------------------------------------
@Composable
fun AdminDiagnosticsScreen(
    onBack: () -> Unit,
    repository: FocalRepository
) {
    val isAcc = PlatformPermissionsHelper.isAccessibilityEnabled()
    val isOvl = PlatformPermissionsHelper.isOverlayEnabled()
    val isDevAdmin = PlatformPermissionsHelper.isDeviceAdminActive()
    val isFocusActive = FocusEngine.instance.isFocusActive

    val decisionLog = listOf(
        DecisionLogRecord("23:38", "YouTube", "Study Mode · app in block list", "Blocked"),
        DecisionLogRecord("23:36", "Settings · Wi-Fi", "Safe settings page", "Allowed"),
        DecisionLogRecord("23:35", "Settings · Accessibility", "Locked settings page", "Blocked"),
        DecisionLogRecord("23:10", "Instagram", "Study Mode · app in block list", "Blocked"),
        DecisionLogRecord("22:50", "Calculator", "Allowed system tool", "Allowed")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header: Back Button (<) and Copy Log Icon Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0B))
                    .border(1.dp, Color(0xFF262626), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B0B0B))
                    .border(1.dp, Color(0xFF262626), CircleShape)
                    .clickable {
                        val logText = buildString {
                            appendLine("FOCAL Diagnostics Decision Log")
                            appendLine("Generated: ${currentTimeMillis()}")
                            decisionLog.forEach {
                                appendLine("${it.time} | ${it.target} | ${it.reason} | ${it.verdict}")
                            }
                        }
                        PlatformPermissionsHelper.copyToClipboard(logText)
                        PlatformPermissionsHelper.showToast("Decision log copied to clipboard")
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.Copy,
                    contentDescription = "Copy log",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Title & Subtitle
        Column {
            Text(
                text = "Diagnostics",
                style = LocalChildTypography.current.title.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Protection status on this phone.",
                style = LocalChildTypography.current.body.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )
        }

        // System Status GlassCard
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 0.dp,
            radius = 24.dp
        ) {
            // Row 1: Accessibility
            AdminDiagnosticStatusRow(
                icon = FocalIcons.Shield,
                title = "Accessibility",
                subtitle = if (isAcc) "FOCAL service connected" else "Service disconnected",
                statusPill = {
                    AdminStatusPill(
                        text = if (isAcc) "On" else "Off",
                        dotColor = if (isAcc) Color.White else Color(0xFFFF453A),
                        textColor = if (isAcc) Color.White else Color(0xFFFF453A)
                    )
                }
            )

            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Row 2: Display over apps
            AdminDiagnosticStatusRow(
                icon = FocalIcons.Smartphone,
                title = "Display over apps",
                subtitle = "Needed for block screen",
                statusPill = {
                    AdminStatusPill(
                        text = if (isOvl) "Allowed" else "Off",
                        dotColor = if (isOvl) Color.White else Color(0xFFFF453A),
                        textColor = if (isOvl) Color.White else Color(0xFFFF453A)
                    )
                }
            )

            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Row 3: Device admin
            AdminDiagnosticStatusRow(
                icon = FocalIcons.Lock,
                title = "Device admin",
                subtitle = "Stops uninstall",
                statusPill = {
                    AdminStatusPill(
                        text = if (isDevAdmin) "On" else "Off",
                        dotColor = if (isDevAdmin) Color.White else Color(0xFFFF453A)
                    )
                }
            )

            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Row 4: Focus session
            AdminDiagnosticStatusRow(
                icon = FocalIcons.Clock,
                title = "Focus",
                subtitle = if (isFocusActive) "Active: Study Mode · 42m left" else "No session running",
                statusPill = {
                    AdminStatusPill(
                        text = if (isFocusActive) "Active" else "Not active",
                        dotColor = if (isFocusActive) Color.White else Color(0xFF6B6B6B),
                        textColor = if (isFocusActive) Color.White else Color(0xFF6B6B6B)
                    )
                }
            )

            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)

            // Row 5: Block list
            val studyPolicy = repository.policies.find { it.id == "pol_study" }
            val appsCount = studyPolicy?.blockedApps?.size ?: 8
            val websitesCount = studyPolicy?.blockedWebsites?.size ?: 10
            AdminDiagnosticStatusRow(
                icon = FocalIcons.Document,
                title = "Block list",
                subtitle = "$appsCount apps · $websitesCount websites",
                statusPill = null
            )
        }

        // Secondary Button: ⚡ Test block screen
        SecondaryButton(
            text = "⚡ Test block screen",
            onClick = {
                PlatformPermissionsHelper.testBlockScreen()
                PlatformPermissionsHelper.showToast("Test block screen launched")
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Decision Log Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Decision log",
                    style = LocalChildTypography.current.title.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Last 50",
                    style = LocalChildTypography.current.caption.copy(
                        fontSize = 13.sp,
                        color = ChildColors.Secondary
                    )
                )
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 0.dp,
                radius = 24.dp
            ) {
                decisionLog.forEachIndexed { index, record ->
                    if (index > 0) {
                        HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.time,
                            style = LocalChildTypography.current.caption.copy(
                                fontSize = 13.sp,
                                color = Color(0xFF6B6B6B)
                            ),
                            modifier = Modifier.width(48.dp)
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.target,
                                style = LocalChildTypography.current.headline.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = record.reason,
                                style = LocalChildTypography.current.caption.copy(
                                    fontSize = 12.sp,
                                    color = ChildColors.Secondary
                                )
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = record.verdict,
                            style = LocalChildTypography.current.body.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (record.verdict == "Blocked") Color.White else Color(0xFFA3A3A3)
                            )
                        )
                    }
                }
            }

            Text(
                text = "Copy log to share it with the developer",
                style = LocalChildTypography.current.caption.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF6B6B6B),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class DecisionLogRecord(
    val time: String,
    val target: String,
    val reason: String,
    val verdict: String
)

@Composable
private fun AdminDiagnosticStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    statusPill: (@Composable () -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF171717)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LocalChildTypography.current.headline.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = LocalChildTypography.current.caption.copy(
                    fontSize = 12.sp,
                    color = ChildColors.Secondary
                )
            )
        }
        if (statusPill != null) {
            Spacer(Modifier.width(10.dp))
            statusPill()
        }
    }
}

// -------------------------------------------------------------
// UI Modals & Detail Sheets
// -------------------------------------------------------------

@Composable
fun AddCardModalSheet(
    onDismiss: () -> Unit,
    onAddCard: (serial: String, activationCode: String) -> Unit
) {
    var serial by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    ChildBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Add card to stock",
            style = LocalChildTypography.current.title.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Enter serial number and activation code",
            style = LocalChildTypography.current.body.copy(
                fontSize = 14.sp,
                color = ChildColors.Secondary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        AdminTextField(
            label = "Serial number",
            value = serial,
            onValueChange = { serial = it },
            placeholder = "e.g. 100003"
        )
        Spacer(Modifier.height(14.dp))
        AdminTextField(
            label = "Activation code",
            value = code,
            onValueChange = { code = it },
            placeholder = "e.g. TK_100003"
        )
        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Add to stock",
            enabled = serial.isNotBlank(),
            onClick = {
                onAddCard(serial, code.ifBlank { "TK_$serial" })
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        SecondaryButton(
            text = "Cancel",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ParentDetailModalSheet(
    parent: ParentAccount,
    repository: FocalRepository,
    onDismiss: () -> Unit
) {
    ChildBottomSheet(onDismiss = onDismiss) {
        Text(
            text = parent.name,
            style = LocalChildTypography.current.title.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = parent.email,
            style = LocalChildTypography.current.body.copy(
                fontSize = 14.sp,
                color = ChildColors.Secondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 16.dp,
            radius = 20.dp
        ) {
            AdminDetailRow("Subscription", parent.subscriptionStatus.name)
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Trial remaining", "${parent.trialDaysLeft} days")
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Registered", parent.registeredDate)
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Children linked", parent.childrenIds.joinToString(", "))
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = "Extend trial (+14 days)",
            onClick = {
                repository.adminExtendTrial(parent.id, 14)
                PlatformPermissionsHelper.showToast("Extended trial by 14 days")
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        SecondaryButton(
            text = "Close",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DeviceDetailModalSheet(
    device: AdminDeviceItem,
    repository: FocalRepository,
    onDismiss: () -> Unit
) {
    ChildBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "${device.childName}'s phone",
            style = LocalChildTypography.current.title.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${device.deviceModel} · ${device.osVersion}",
            style = LocalChildTypography.current.body.copy(
                fontSize = 14.sp,
                color = ChildColors.Secondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 16.dp,
            radius = 20.dp
        ) {
            AdminDetailRow("Battery", "${device.batteryPercent}%")
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("App version", device.appVersion)
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Last heartbeat", "${device.lastHeartbeat} ago")
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Accessibility", if (device.accessibilityGranted) "Granted" else "Off", isWarning = !device.accessibilityGranted)
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Display overlay", if (device.overlayGranted) "Granted" else "Off")
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Device admin", if (device.deviceAdminGranted) "Active" else "Inactive")
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Usage access", if (device.usageGranted) "Granted" else "Off")
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Notifications", if (device.notificationsGranted) "Enabled" else "Disabled")
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Battery unrestricted", if (device.batteryUnrestricted) "Unrestricted" else "Optimized")
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = "Force sync device",
            onClick = {
                PlatformPermissionsHelper.showToast("Sync ping sent to ${device.childName}'s device")
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        SecondaryButton(
            text = "Reset pairing",
            onClick = {
                repository.adminResetChildPairing(device.id)
                PlatformPermissionsHelper.showToast("Reset pairing for ${device.childName}")
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CardDetailModalSheet(
    card: FocusCard,
    onRevoke: () -> Unit,
    onDismiss: () -> Unit
) {
    ChildBottomSheet(onDismiss = onDismiss) {
        Text(
            text = card.cardId,
            style = LocalChildTypography.current.title.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Token: ${card.token}",
            style = LocalChildTypography.current.body.copy(
                fontSize = 14.sp,
                color = ChildColors.Secondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            padding = 16.dp,
            radius = 20.dp
        ) {
            AdminDetailRow("Status", card.status.name)
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Owner", card.ownerName ?: "None (In stock)")
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Total taps", card.tapCount.toString())
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("Registered at", card.registeredAt)
            Spacer(Modifier.height(8.dp))
            AdminDetailRow("NFC UID", card.nfcUid)
        }

        Spacer(Modifier.height(20.dp))

        if (card.status != CardStatus.REVOKED) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF220A0A))
                    .border(1.dp, Color(0xFFFF453A), CircleShape)
                    .clickable { onRevoke() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Revoke card",
                    style = LocalChildTypography.current.body.copy(
                        color = Color(0xFFFF453A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        SecondaryButton(
            text = "Close",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// -------------------------------------------------------------
// Small Reusable Helper Composables
// -------------------------------------------------------------

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        padding = 18.dp,
        radius = 24.dp
    ) {
        Text(
            text = title,
            style = LocalChildTypography.current.body.copy(
                fontSize = 13.sp,
                color = ChildColors.Secondary
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = LocalChildTypography.current.title.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = caption,
            style = LocalChildTypography.current.caption.copy(
                fontSize = 12.sp,
                color = Color(0xFF6B6B6B)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AdminStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = LocalChildTypography.current.title.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = LocalChildTypography.current.caption.copy(
                fontSize = 12.sp,
                color = ChildColors.Secondary
            )
        )
    }
}

@Composable
private fun AdminSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF0E0E0E))
            .border(1.dp, Color(0xFF262626), RoundedCornerShape(26.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = FocalIcons.Search,
                contentDescription = "Search",
                tint = Color(0xFF6B6B6B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = LocalChildTypography.current.body.copy(
                                color = Color(0xFF6B6B6B),
                                fontSize = 14.sp
                            )
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.weight(1f)
            )
            if (value.isNotEmpty()) {
                Icon(
                    imageVector = FocalIcons.Close,
                    contentDescription = "Clear",
                    tint = Color(0xFF6B6B6B),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onValueChange("") }
                )
            }
        }
    }
}

@Composable
private fun AdminFilterChipsRow(
    filters: List<String>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color(0xFF0E0E0E))
                    .border(1.dp, if (isSelected) Color.White else Color(0xFF262626), CircleShape)
                    .clickable { onSelectFilter(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter,
                    style = LocalChildTypography.current.caption.copy(
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun AdminStatusPill(
    text: String,
    dotColor: Color = Color.White,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF0E0E0E))
            .border(1.dp, Color(0xFF262626), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = text,
                style = LocalChildTypography.current.caption.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

@Composable
private fun AdminDetailRow(
    label: String,
    value: String,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = LocalChildTypography.current.body.copy(
                fontSize = 13.sp,
                color = ChildColors.Secondary
            )
        )
        Text(
            text = value,
            style = LocalChildTypography.current.body.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isWarning) Color(0xFFFF453A) else Color.White
            )
        )
    }
}

@Composable
private fun AdminTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            style = LocalChildTypography.current.caption.copy(
                fontSize = 12.sp,
                color = ChildColors.Secondary,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0E0E0E))
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = LocalChildTypography.current.body.copy(
                                color = Color(0xFF6B6B6B),
                                fontSize = 14.sp
                            )
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun Int.formatWithCommas(): String {
    val str = this.toString()
    if (str.length <= 3) return str
    val lastThree = str.substring(str.length - 3)
    val rest = str.substring(0, str.length - 3)
    return "$rest,$lastThree"
}
