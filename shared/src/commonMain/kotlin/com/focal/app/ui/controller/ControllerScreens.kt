package com.focal.app.ui.controller

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.*
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.*

// -------------------------------------------------------------
// 1. PARENT HOME DASHBOARD
// -------------------------------------------------------------
@Composable
fun ControllerDashboardScreen(
    onChildSelected: (ChildProfile) -> Unit,
    onAddChildClick: () -> Unit,
    onCreatePolicyClick: () -> Unit,
    onOpenPermissions: () -> Unit = {},
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    val user = repository.currentUser
    val pendingRequests = repository.emergencyRequests.filter { it.status == "PENDING" }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showFocusQrModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Top Greeting & Bell Icon (with white unread dot)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Good morning,",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocalColors.TextSecondary
                    )
                )
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // Bell icon button with unread white dot
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(FocalColors.Surface)
                    .border(1.dp, FocalColors.Border, CircleShape)
                    .clickable { showNotificationsModal = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.Bell,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                // White unread indicator dot
                if (repository.notifications.isNotEmpty() || pendingRequests.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, Color.Black, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Three Equal StatCards: Children | Focusing now | Today
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = "Children",
                value = "${repository.childrenList.size}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Focusing now",
                value = "${repository.childrenList.count { it.isFocusing }}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Today",
                value = "5h 12m",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pending Emergency Requests Banner
        if (pendingRequests.isNotEmpty()) {
            val req = pendingRequests.first()
            FocalCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = FocalColors.Danger
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(FocalColors.Danger)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Emergency Exit Request",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${req.childName}: '${req.reason}'",
                        style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(
                            text = "Approve Exit",
                            onClick = { repository.resolveEmergencyRequest(req.id, true) },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Decline",
                            onClick = { repository.resolveEmergencyRequest(req.id, false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: "Children"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Children",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            IconButton(
                onClick = onAddChildClick,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FocalColors.Surface)
                    .border(1.dp, FocalColors.Border, CircleShape)
            ) {
                Icon(imageVector = FocalIcons.Add, contentDescription = "Add Child", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Children Section Rows: Avatar, Name, Status ("Focusing · 1h 24m left" or "Not focusing · 2h ago")
        if (repository.childrenList.isEmpty()) {
            EmptyState(
                title = "No children paired yet",
                message = "Tap + to pair your child's phone using a QR code.",
                actionText = "Add Child",
                onActionClick = onAddChildClick
            )
        } else {
            repository.childrenList.forEach { child ->
                ParentChildRowCard(
                    child = child,
                    onClick = { onChildSelected(child) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Row
        Text(
            text = "Quick Management",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FocalCard(
                modifier = Modifier.weight(1f),
                onClick = onCreatePolicyClick
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = FocalIcons.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "New Policy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp))
                    Text(text = "Create rules", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontSize = 10.sp))
                }
            }

            FocalCard(
                modifier = Modifier.weight(1f),
                onClick = onAddChildClick
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = FocalIcons.Users, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Pair Child", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp))
                    Text(text = "Scan pairing", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontSize = 10.sp))
                }
            }

            FocalCard(
                modifier = Modifier.weight(1f),
                onClick = { showFocusQrModal = true }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = FocalIcons.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Focus QR", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp))
                    Text(text = "Generate QR", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontSize = 10.sp))
                }
            }
        }
    }

    if (showNotificationsModal) {
        NotificationsModal(
            onDismiss = { showNotificationsModal = false },
            repository = repository
        )
    }

    if (showFocusQrModal) {
        FocusQrGeneratorModal(
            onDismiss = { showFocusQrModal = false },
            repository = repository
        )
    }
}

/**
 * Child Row Component for Parent Home Dashboard
 */
@Composable
fun ParentChildRowCard(
    child: ChildProfile,
    onClick: () -> Unit
) {
    val statusText = if (child.isFocusing) "Focusing · 1h 24m left" else "Not focusing · 2h ago"

    FocalCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (child.isFocusing) Color.White else FocalColors.Border,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = child.name, size = 44.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(active = child.isFocusing)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FocalColors.TextSecondary
                            )
                        )
                    }
                }
            }

            Icon(
                imageVector = FocalIcons.ArrowForward,
                contentDescription = "View Child",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// 2. CHILDREN TAB (LIST + ADD CHILD)
// -------------------------------------------------------------
@Composable
fun ChildrenDirectoryScreen(
    onChildSelected: (ChildProfile) -> Unit,
    onAddChildClick: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Children",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            IconButton(
                onClick = onAddChildClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(imageVector = FocalIcons.Add, contentDescription = "Add Child", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (repository.childrenList.isEmpty()) {
            EmptyState(
                title = "No children paired yet",
                message = "Tap + to pair your child's phone and manage screen time rules remotely.",
                actionText = "Add Child",
                onActionClick = onAddChildClick
            )
        } else {
            repository.childrenList.forEach { child ->
                ParentChildRowCard(
                    child = child,
                    onClick = { onChildSelected(child) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun ChildManagementScreen(
    child: ChildProfile,
    onBack: () -> Unit,
    onCreateFocus: () -> Unit,
    onGenerateQr: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    ChildDetailScreen(
        child = child,
        onBack = onBack,
        onCreateFocus = onCreateFocus,
        onGenerateQr = onGenerateQr,
        repository = repository
    )
}

// -------------------------------------------------------------
// 3. CHILD DETAIL SCREEN (WITH OVERFLOW MENU & START FOCUS SHEET)
// -------------------------------------------------------------
@Composable
fun ChildDetailScreen(
    child: ChildProfile,
    onBack: () -> Unit,
    onCreateFocus: () -> Unit,
    onGenerateQr: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showEditChildModal by remember { mutableStateOf(false) }
    var showRemoveConfirmSheet by remember { mutableStateOf(false) }
    var showStartFocusSheet by remember { mutableStateOf(false) }
    var showFocusQrModal by remember { mutableStateOf(false) }
    var showSchedulesModal by remember { mutableStateOf(false) }
    var showPoliciesModal by remember { mutableStateOf(false) }
    var showActivityModal by remember { mutableStateOf(false) }
    var showDeviceCardModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Back, Name + Live Status, Overflow Menu ⋮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = FocalIcons.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = child.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(active = child.isFocusing)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (child.isFocusing) "Focusing · 1h 24m left" else "Not focusing",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                    )
                }
            }

            Box {
                IconButton(onClick = { showOverflowMenu = true }, modifier = Modifier.size(36.dp)) {
                    Text(text = "⋮", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    modifier = Modifier.background(FocalColors.Surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit child", color = Color.White) },
                        onClick = {
                            showOverflowMenu = false
                            showEditChildModal = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove child", color = FocalColors.Danger) },
                        onClick = {
                            showOverflowMenu = false
                            showRemoveConfirmSheet = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ProgressRing (Time Left or "Not focusing")
        ProgressRing(
            progress = if (child.isFocusing) 0.65f else 0f,
            modifier = Modifier.size(180.dp),
            centerContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (child.isFocusing) "${child.remainingMinutes / 60}h ${child.remainingMinutes % 60}m" else "Not focusing",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = if (child.isFocusing) 32.sp else 18.sp
                        )
                    )
                    if (child.isFocusing) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TIME LEFT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FocalColors.TextSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // StatCards Today's Focus / Screen Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Today's Focus",
                value = "${child.todayFocusMinutes / 60}h ${child.todayFocusMinutes % 60}m",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Screen Time",
                value = "${child.screenTimeMinutes / 60}h ${child.screenTimeMinutes % 60}m",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Rows: Start Focus Now, Start with QR, Schedules, Policy, Activity, Device & Card
        listOf(
            Triple("Start Focus Now", "Launch focus session remotely", FocalIcons.Play) to { showStartFocusSheet = true },
            Triple("Start with QR", "Generate single-use focus QR code", FocalIcons.QrCode) to { showFocusQrModal = true },
            Triple("Schedules", "Manage automated routines", FocalIcons.Calendar) to { showSchedulesModal = true },
            Triple("Policy", "Assigned restriction policy", FocalIcons.Shield) to { showPoliciesModal = true },
            Triple("Activity", "Live usage log & attempt history", FocalIcons.Chart) to { showActivityModal = true },
            Triple("Device & Card", "Paired status & hardware info", FocalIcons.Nfc) to { showDeviceCardModal = true }
        ).forEach { (item, onClickAction) ->
            val (title, subtitle, icon) = item
            FocalListRow(
                title = title,
                subtitle = subtitle,
                icon = icon,
                trailingIcon = FocalIcons.ArrowForward,
                onClick = onClickAction,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
    }

    if (showFocusQrModal) {
        FocusQrGeneratorModal(
            preselectedChildId = child.id,
            onDismiss = { showFocusQrModal = false },
            repository = repository
        )
    }

    // Modal: Start Focus Now Sheet (Pick Policy + Duration -> Remote Start)
    if (showStartFocusSheet) {
        RemoteStartFocusSheet(
            childName = child.name,
            onDismiss = { showStartFocusSheet = false },
            onStartRemoteSession = { policy, duration ->
                FocusEngine.instance.remoteStartFocus(child.id, policy, duration)
                showStartFocusSheet = false
            }
        )
    }

    // Modal: Schedules
    if (showSchedulesModal) {
        ChildSchedulesModal(
            child = child,
            onDismiss = { showSchedulesModal = false },
            repository = repository
        )
    }

    // Modal: Policies
    if (showPoliciesModal) {
        ChildPoliciesModal(
            child = child,
            onDismiss = { showPoliciesModal = false },
            onCreateFocus = onCreateFocus,
            repository = repository
        )
    }

    // Modal: Activity
    if (showActivityModal) {
        ChildActivityModal(
            child = child,
            onDismiss = { showActivityModal = false }
        )
    }

    // Modal: Device & Card
    if (showDeviceCardModal) {
        ChildDeviceCardModal(
            child = child,
            onDismiss = { showDeviceCardModal = false },
            onGenerateQr = onGenerateQr
        )
    }

    // Sheet: Remove Child Confirm (Danger)
    if (showRemoveConfirmSheet) {
        BottomSheet(onDismiss = { showRemoveConfirmSheet = false }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Remove ${child.name}?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will unpair the device and disable remote management. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocalColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                DestructiveButton(
                    text = "Remove Child Profile",
                    onClick = {
                        repository.childrenList.removeAll { it.id == child.id }
                        showRemoveConfirmSheet = false
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryButton(
                    text = "Cancel",
                    onClick = { showRemoveConfirmSheet = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showEditChildModal) {
        EditChildModal(
            child = child,
            onDismiss = { showEditChildModal = false },
            repository = repository
        )
    }
}

// -------------------------------------------------------------
// 4. REMOTE START FOCUS SHEET
// -------------------------------------------------------------
@Composable
fun RemoteStartFocusSheet(
    childName: String,
    onDismiss: () -> Unit,
    onStartRemoteSession: (FocusPolicy, Int) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var selectedPolicy by remember { mutableStateOf(repository.policies.first()) }
    var selectedDurationMinutes by remember { mutableIntStateOf(60) }

    BottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Start Focus on $childName's Phone",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SELECT FOCUS POLICY",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocalColors.TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repository.policies.forEach { policy ->
                    val isSelected = policy.id == selectedPolicy.id
                    FocalCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (isSelected) Color.White else FocalColors.Border,
                        onClick = { selectedPolicy = policy }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = policy.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "${policy.blockedApps.size} apps · ${policy.blockedWebsites.size} sites restricted",
                                    style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                )
                            }
                            if (isSelected) {
                                Icon(imageVector = FocalIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SELECT DURATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = FocalColors.TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 120, 240).forEach { mins ->
                    val isSelected = selectedDurationMinutes == mins
                    val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color.White else FocalColors.Surface)
                            .border(1.dp, if (isSelected) Color.White else FocalColors.Border, RoundedCornerShape(10.dp))
                            .clickable { selectedDurationMinutes = mins }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Start Focus Now",
                onClick = { onStartRemoteSession(selectedPolicy, selectedDurationMinutes) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// 5. CHILD SCHEDULES MODAL & SCHEDULE EDIT SCREEN
// -------------------------------------------------------------
@Composable
fun ChildSchedulesModal(
    child: ChildProfile,
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var showScheduleEditScreen by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleItem?>(null) }

    if (showScheduleEditScreen) {
        ScheduleEditScreen(
            schedule = editingSchedule,
            onDismiss = {
                showScheduleEditScreen = false
                editingSchedule = null
            },
            onSave = { saved ->
                if (editingSchedule != null) {
                    val idx = repository.schedules.indexOfFirst { it.id == saved.id }
                    if (idx >= 0) repository.schedules[idx] = saved
                } else {
                    repository.schedules.add(0, saved)
                }
                showScheduleEditScreen = false
                editingSchedule = null
            },
            onDelete = { scheduleId ->
                repository.schedules.removeAll { it.id == scheduleId }
                showScheduleEditScreen = false
                editingSchedule = null
            },
            repository = repository
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${child.name}'s Focus Schedules",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        IconButton(
                            onClick = {
                                editingSchedule = null
                                showScheduleEditScreen = true
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(imageVector = FocalIcons.Add, contentDescription = "Add Schedule", tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    repository.schedules.forEach { schedule ->
                        var isEnabled by remember(schedule.id) { mutableStateOf(schedule.isEnabled) }
                        FocalListRow(
                            title = schedule.title,
                            subtitle = "${schedule.timeRange} • ${schedule.days}",
                            trailingToggle = isEnabled,
                            onToggleChange = {
                                isEnabled = it
                                repository.toggleSchedule(schedule.id)
                            },
                            onClick = {
                                editingSchedule = schedule
                                showScheduleEditScreen = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    PrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/**
 * Single Screen for Create / Edit Focus Schedule
 */
@Composable
fun ScheduleEditScreen(
    schedule: ScheduleItem?,
    onDismiss: () -> Unit,
    onSave: (ScheduleItem) -> Unit,
    onDelete: (String) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var titleInput by remember { mutableStateOf(schedule?.title ?: "") }
    var selectedDays by remember {
        mutableStateOf(
            schedule?.days?.split(",")?.map { it.trim() }?.toSet() ?: setOf("Mon", "Tue", "Wed", "Thu", "Fri")
        )
    }
    var startTimeInput by remember { mutableStateOf("04:00 PM") }
    var endTimeInput by remember { mutableStateOf("07:00 PM") }
    var selectedPolicy by remember { mutableStateOf(repository.policies.first()) }

    var pendingSavedItem by remember { mutableStateOf<ScheduleItem?>(null) }
    var isSavedSuccess by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
        ) {
            if (isSavedSuccess && pendingSavedItem != null) {
                SuccessState(
                    title = "Schedule Saved",
                    message = "'${pendingSavedItem!!.title}' has been updated successfully.",
                    actionText = "Done",
                    onActionClick = { onSave(pendingSavedItem!!) }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (schedule != null) "Edit Schedule" else "Create Schedule",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FocalTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = "Schedule Name",
                        placeholder = "e.g. Evening Homework Routine"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ACTIVE DAYS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocalColors.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ChipSelector(
                        options = daysOfWeek,
                        selectedOptions = selectedDays,
                        onOptionToggled = { day ->
                            selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FocalTextField(
                            value = startTimeInput,
                            onValueChange = { startTimeInput = it },
                            label = "Start Time",
                            placeholder = "04:00 PM",
                            modifier = Modifier.weight(1f)
                        )
                        FocalTextField(
                            value = endTimeInput,
                            onValueChange = { endTimeInput = it },
                            label = "End Time",
                            placeholder = "07:00 PM",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "POLICY TO ENFORCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FocalColors.TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        repository.policies.forEach { pol ->
                            val isSelected = pol.id == selectedPolicy.id
                            FocalCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = if (isSelected) Color.White else FocalColors.Border,
                                onClick = { selectedPolicy = pol }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pol.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    if (isSelected) {
                                        Icon(imageVector = FocalIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    PrimaryButton(
                        text = "Save Schedule",
                        onClick = {
                            val nameToSave = titleInput.ifBlank { "Focus Routine" }
                            val daysStr = selectedDays.joinToString(", ")
                            val updated = ScheduleItem(
                                id = schedule?.id ?: "sch_${System.currentTimeMillis()}",
                                title = nameToSave,
                                timeRange = "$startTimeInput – $endTimeInput",
                                days = daysStr,
                                mode = selectedPolicy.mode,
                                isEnabled = true,
                                appsCount = selectedPolicy.blockedApps.size,
                                websitesCount = selectedPolicy.blockedWebsites.size
                            )
                            pendingSavedItem = updated
                            isSavedSuccess = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (schedule != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DestructiveButton(
                            text = "Delete Schedule",
                            onClick = { onDelete(schedule.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. POLICIES TAB & POLICY EDIT (APP PICKER & WEBSITE LIST)
// -------------------------------------------------------------
@Composable
fun ControllerFocusPoliciesScreen(
    onCreatePolicyClick: () -> Unit,
    onGenerateQr: (FocusPolicy) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var editingPolicy by remember { mutableStateOf<FocusPolicy?>(null) }
    var showCreatePolicyModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Focus Policies",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            IconButton(
                onClick = { showCreatePolicyModal = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(imageVector = FocalIcons.Add, contentDescription = "Add Policy", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (repository.policies.isEmpty()) {
            EmptyState(
                title = "No Focus Policies",
                message = "Tap '+' to create rule profiles for apps, websites and session duration.",
                actionText = "Create Policy",
                onActionClick = { showCreatePolicyModal = true }
            )
        } else {
            repository.policies.forEach { policy ->
                FocalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { editingPolicy = policy }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = policy.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${policy.blockedApps.size} apps · ${policy.blockedWebsites.size} sites",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = FocalColors.TextSecondary
                                )
                            )
                        }
                        Icon(imageVector = FocalIcons.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showCreatePolicyModal) {
        PolicyEditScreen(
            policy = null,
            onDismiss = { showCreatePolicyModal = false },
            onSave = { newPol ->
                repository.addPolicy(newPol)
                showCreatePolicyModal = false
            },
            onDelete = {},
            repository = repository
        )
    }

    if (editingPolicy != null) {
        PolicyEditScreen(
            policy = editingPolicy,
            onDismiss = { editingPolicy = null },
            onSave = { updated ->
                repository.updatePolicy(updated)
                editingPolicy = null
            },
            onDelete = { id ->
                repository.deletePolicy(id)
                editingPolicy = null
            },
            repository = repository
        )
    }
}

/**
 * Single Screen for Create / Edit Policy (with App Picker & Website List rows)
 */
@Composable
fun PolicyEditScreen(
    policy: FocusPolicy?,
    onDismiss: () -> Unit,
    onSave: (FocusPolicy) -> Unit,
    onDelete: (String) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var policyName by remember { mutableStateOf(policy?.name ?: "") }
    var selectedMode by remember { mutableStateOf(policy?.mode ?: FocusMode.STUDY) }
    var durationMins by remember { mutableIntStateOf(policy?.durationMinutes ?: 120) }

    val selectedApps = remember { mutableStateListOf(*(policy?.blockedApps ?: listOf("Instagram", "YouTube", "TikTok")).toTypedArray()) }
    val selectedWebsites = remember { mutableStateListOf(*(policy?.blockedWebsites ?: listOf("instagram.com", "youtube.com")).toTypedArray()) }

    var showAppPicker by remember { mutableStateOf(false) }
    var showWebsiteList by remember { mutableStateOf(false) }

    var pendingSavedPolicy by remember { mutableStateOf<FocusPolicy?>(null) }
    var isSavedSuccess by remember { mutableStateOf(false) }

    if (showAppPicker) {
        AppPickerScreen(
            selectedApps = selectedApps,
            onDismiss = { showAppPicker = false },
            onSaveApps = { updatedList ->
                selectedApps.clear()
                selectedApps.addAll(updatedList)
                showAppPicker = false
            }
        )
    } else if (showWebsiteList) {
        WebsiteListScreen(
            selectedWebsites = selectedWebsites,
            onDismiss = { showWebsiteList = false },
            onSaveWebsites = { updatedWebsites ->
                selectedWebsites.clear()
                selectedWebsites.addAll(updatedWebsites)
                showWebsiteList = false
            }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            FocalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(4.dp)
            ) {
                if (isSavedSuccess && pendingSavedPolicy != null) {
                    SuccessState(
                        title = "Policy Saved",
                        message = "'${pendingSavedPolicy!!.name}' focus rule profile has been saved.",
                        actionText = "Done",
                        onActionClick = { onSave(pendingSavedPolicy!!) }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (policy != null) "Edit Policy" else "Create Policy",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FocalTextField(
                            value = policyName,
                            onValueChange = { policyName = it },
                            label = "Policy Name",
                            placeholder = "e.g. Homework Mode"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Row: Blocked apps (count)
                        FocalListRow(
                            title = "Blocked apps (${selectedApps.size})",
                            subtitle = selectedApps.joinToString(", ").take(35) + if (selectedApps.joinToString(", ").length > 35) "..." else "",
                            icon = FocalIcons.Smartphone,
                            trailingIcon = FocalIcons.ArrowForward,
                            onClick = { showAppPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row: Blocked websites (count)
                        FocalListRow(
                            title = "Blocked websites (${selectedWebsites.size})",
                            subtitle = selectedWebsites.joinToString(", ").take(35) + if (selectedWebsites.joinToString(", ").length > 35) "..." else "",
                            icon = FocalIcons.Globe,
                            trailingIcon = FocalIcons.ArrowForward,
                            onClick = { showWebsiteList = true },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        PrimaryButton(
                            text = "Save Policy",
                            onClick = {
                                val nameToSave = policyName.ifBlank { "Custom Focus Policy" }
                                val updated = FocusPolicy(
                                    id = policy?.id ?: "pol_${System.currentTimeMillis()}",
                                    childId = policy?.childId ?: "child_1",
                                    name = nameToSave,
                                    mode = selectedMode,
                                    durationMinutes = durationMins,
                                    blockedApps = selectedApps.toList(),
                                    blockedWebsites = selectedWebsites.toList()
                                )
                                pendingSavedPolicy = updated
                                isSavedSuccess = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (policy != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            DestructiveButton(
                                text = "Delete Policy",
                                onClick = { onDelete(policy.id) },
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
// 7. APP PICKER (SEARCH + CATEGORIES + CHECKBOXES + IOS NOTICE)
// -------------------------------------------------------------
@Composable
fun AppPickerScreen(
    selectedApps: List<String>,
    onDismiss: () -> Unit,
    onSaveApps: (List<String>) -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    isIosChild: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val currentSelected = remember { mutableStateListOf(*selectedApps.toTypedArray()) }
    val currentBlockedCategories = remember { mutableStateListOf<String>() }

    val childApps = repository.childInstalledApps.map {
        Triple(it.appName, it.category, FocalIcons.Smartphone)
    }

    val defaultApps = listOf(
        Triple("Instagram", "Social", FocalIcons.Smartphone),
        Triple("TikTok", "Social", FocalIcons.Smartphone),
        Triple("Snapchat", "Social", FocalIcons.Smartphone),
        Triple("YouTube", "Video", FocalIcons.Smartphone),
        Triple("Netflix", "Video", FocalIcons.Smartphone),
        Triple("Twitch", "Video", FocalIcons.Smartphone),
        Triple("PUBG Mobile", "Games", FocalIcons.Smartphone),
        Triple("Roblox", "Games", FocalIcons.Smartphone),
        Triple("Minecraft", "Games", FocalIcons.Smartphone),
        Triple("Free Fire", "Games", FocalIcons.Smartphone),
        Triple("Discord", "Messaging", FocalIcons.Smartphone),
        Triple("WhatsApp", "Messaging", FocalIcons.Smartphone)
    )

    val availableApps = (childApps + defaultApps).distinctBy { it.first }

    val categories = listOf("All", "Social", "Video", "Games", "Shopping", "Messaging")

    val filteredApps = availableApps.filter { (name, category, _) ->
        val matchesCategory = (selectedCategory == "All" || category.equals(selectedCategory, ignoreCase = true))
        val matchesSearch = searchQuery.isBlank() || name.lowercase().contains(searchQuery.lowercase())
        matchesCategory && matchesSearch
    }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Picker (${currentSelected.size} selected)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                FocalTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search Apps",
                    placeholder = "e.g. Instagram, Roblox..."
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White else FocalColors.Surface)
                                .border(1.dp, FocalColors.Border, RoundedCornerShape(10.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else FocalColors.TextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Block Whole Category Toggle
                if (selectedCategory != "All") {
                    val isCatBlocked = currentBlockedCategories.contains(selectedCategory)
                    FocalCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FocalColors.Surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Block all $selectedCategory apps",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = "Newly installed $selectedCategory apps will be blocked automatically",
                                    style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                )
                            }
                            FocalToggle(
                                checked = isCatBlocked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!currentBlockedCategories.contains(selectedCategory)) {
                                            currentBlockedCategories.add(selectedCategory)
                                        }
                                        availableApps.filter { it.second.equals(selectedCategory, ignoreCase = true) }.forEach { (name, _, _) ->
                                            if (!currentSelected.contains(name)) currentSelected.add(name)
                                        }
                                    } else {
                                        currentBlockedCategories.remove(selectedCategory)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // iOS Notice Banner
                if (isIosChild) {
                    FocalCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FocalColors.Surface
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = FocalIcons.Smartphone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "On iOS: Choose apps on Ananya's phone via FamilyActivityPicker in Child Profile settings.",
                                style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Apps List with Grayscale Checkboxes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredApps.forEach { (appName, category, icon) ->
                        val isChecked = currentSelected.contains(appName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(FocalColors.Surface)
                                .border(1.dp, FocalColors.Border, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isChecked) currentSelected.remove(appName) else currentSelected.add(appName)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = appName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary)
                                    )
                                }
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) currentSelected.add(appName) else currentSelected.remove(appName)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color.White,
                                    checkmarkColor = Color.Black,
                                    uncheckedColor = FocalColors.BorderSecondary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = "Save Apps Selection",
                    onClick = { onSaveApps(currentSelected.toList()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 8. WEBSITE LIST SCREEN
// -------------------------------------------------------------
@Composable
fun WebsiteListScreen(
    selectedWebsites: List<String>,
    onDismiss: () -> Unit,
    onSaveWebsites: (List<String>) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var newDomainInput by remember { mutableStateOf("") }
    val websiteList = remember { mutableStateListOf(*selectedWebsites.toTypedArray()) }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blocked Websites (${websiteList.size})",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Domain Add Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocalTextField(
                        value = newDomainInput,
                        onValueChange = { newDomainInput = it },
                        label = "Add Website Domain",
                        placeholder = "e.g. reddit.com",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clean = repository.normalizeDomain(newDomainInput)
                            if (clean.isNotBlank() && !websiteList.contains(clean)) {
                                websiteList.add(clean)
                                newDomainInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    ) {
                        Icon(imageVector = FocalIcons.Add, contentDescription = "Add Website", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    websiteList.forEach { domain ->
                        FocalCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = FocalIcons.Globe, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = domain,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = { websiteList.remove(domain) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = FocalIcons.Close, contentDescription = "Remove", tint = FocalColors.TextSecondary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = "Save Website Rules",
                    onClick = { onSaveWebsites(websiteList.toList()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 9. GENERATE FOCUS QR SCREEN
// -------------------------------------------------------------
@Composable
fun ControllerGenerateQrScreen(
    repository: FocalRepository = FocalRepository.instance
) {
    var selectedChildName by remember { mutableStateOf(repository.childrenList.firstOrNull()?.name ?: "Rahul") }
    var selectedPolicyName by remember { mutableStateOf(repository.policies.firstOrNull()?.name ?: "Study Mode") }
    var selectedDurationMinutes by remember { mutableIntStateOf(60) }

    var countdownSeconds by remember { mutableIntStateOf(272) } // 04:32

    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            countdownSeconds--
        }
    }

    val minutes = countdownSeconds / 60
    val secs = countdownSeconds % 60
    val formattedTime = "Expires in ${if (minutes < 10) "0$minutes" else "$minutes"}:${if (secs < 10) "0$secs" else "$secs"}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Generate Focus QR",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Single-use short-lived focus token",
            style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Selectors Card
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = "CHILD", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repository.childrenList.forEach { c ->
                        val isSelected = c.name == selectedChildName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White else FocalColors.Surface)
                                .border(1.dp, FocalColors.Border, RoundedCornerShape(8.dp))
                                .clickable { selectedChildName = c.name }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = c.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else Color.White))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "POLICY", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repository.policies.take(3).forEach { p ->
                        val isSelected = p.name == selectedPolicyName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White else FocalColors.Surface)
                                .border(1.dp, FocalColors.Border, RoundedCornerShape(8.dp))
                                .clickable { selectedPolicyName = p.name }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = p.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else Color.White), maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QrCodeView
        QrCodeView(
            qrContent = "FOCAL_QR_SIGNED_${selectedChildName}_${selectedPolicyName}_$selectedDurationMinutes",
            size = 200.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall.copy(
                color = FocalColors.Danger,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Regenerate QR",
            onClick = { countdownSeconds = 300 },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// -------------------------------------------------------------
// 10. NFC CARD MANAGER (BLACK CARD VISUAL + PROGRAM NEW CARD)
// -------------------------------------------------------------
@Composable
fun ControllerNfcCardManagerScreen(
    repository: FocalRepository = FocalRepository.instance
) {
    val currentUser = repository.currentUser
    val children = repository.childrenList

    var showAddCardFlow by remember { mutableStateOf(false) }
    var cardToRevoke by remember { mutableStateOf<FocusCard?>(null) }
    var cardToAssign by remember { mutableStateOf<FocusCard?>(null) }

    val myCards = remember(repository.nfcCards.size, repository.nfcCards.map { it.status.name + it.assignedChildId }) {
        repository.nfcCards.filter {
            it.ownerParentId == currentUser.id || it.ownerId == currentUser.id || it.status != CardStatus.IN_STOCK
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NFC Card Manager",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pair, assign & manage hardware focus cards",
                    style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                )
            }

            PrimaryButton(
                text = "Add Card",
                onClick = { showAddCardFlow = true }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (myCards.isEmpty()) {
            FocalCard(modifier = Modifier.fillMaxWidth(), padding = 24.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(imageVector = FocalIcons.Nfc, contentDescription = null, tint = FocalColors.TextSecondary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No FOCAL cards registered", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Claim or write a physical NFC card to allow your child to tap and start focus sessions.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Add Your First Card",
                        onClick = { showAddCardFlow = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            myCards.forEach { card ->
                val assignedChild = children.find { it.id == card.assignedChildId }

                FocalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = FocalIcons.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = card.cardId, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                    Text(text = "Token: ${card.token}", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                                }
                            }

                            StatusBadge(
                                text = when (card.status) {
                                    CardStatus.ASSIGNED -> "ASSIGNED TO ${assignedChild?.name?.uppercase() ?: "CHILD"}"
                                    CardStatus.REGISTERED -> "UNASSIGNED"
                                    CardStatus.REVOKED, CardStatus.DEACTIVATED -> "REVOKED"
                                    else -> "IN STOCK"
                                },
                                badgeType = when (card.status) {
                                    CardStatus.ASSIGNED -> BadgeType.ACTIVE
                                    CardStatus.REGISTERED -> BadgeType.PENDING
                                    else -> BadgeType.IDLE
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (card.status != CardStatus.REVOKED && card.status != CardStatus.DEACTIVATED) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (card.status == CardStatus.ASSIGNED) {
                                    SecondaryButton(
                                        text = "Reassign",
                                        onClick = { cardToAssign = card },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SecondaryButton(
                                        text = "Unpair",
                                        onClick = { repository.unpairCardFromChild(card.cardId) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    PrimaryButton(
                                        text = "Assign to child",
                                        onClick = { cardToAssign = card },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                DestructiveButton(
                                    text = "Remove",
                                    onClick = { cardToRevoke = card },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Text(
                                text = "Card token revoked and permanently deactivated.",
                                style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.Danger)
                            )
                        }
                    }
                }
            }
        }
    }

    // Sheet: Card to Assign to Child
    if (cardToAssign != null) {
        BottomSheet(onDismiss = { cardToAssign = null }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Assign Card ${cardToAssign?.cardId}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select which child can use this card to start focus sessions:",
                    style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                )
                Spacer(modifier = Modifier.height(16.dp))

                children.forEach { child ->
                    FocalCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            cardToAssign?.let { repository.assignCardToChild(it.cardId, child.id) }
                            cardToAssign = null
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(name = child.name, size = 32.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = child.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryButton(text = "Cancel", onClick = { cardToAssign = null }, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // Sheet: Confirm Revoke/Remove Card
    if (cardToRevoke != null) {
        BottomSheet(onDismiss = { cardToRevoke = null }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Remove & Revoke Card ${cardToRevoke?.cardId}?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This card will stop working permanently and cannot be reused by any device. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                )
                Spacer(modifier = Modifier.height(24.dp))
                DestructiveButton(
                    text = "Remove & Revoke Card",
                    onClick = {
                        cardToRevoke?.let { repository.revokeCard(it.cardId) }
                        cardToRevoke = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryButton(
                    text = "Cancel",
                    onClick = { cardToRevoke = null },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showAddCardFlow) {
        AddCardFlowModal(
            onDismiss = { showAddCardFlow = false },
            repository = repository
        )
    }
}

/**
 * Program New Card Flow Modal (Hold blank card -> write token -> success -> assign)
 */
@Composable
fun ProgramNewCardModal(
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var step by remember { mutableIntStateOf(1) } // 1: hold card, 2: writing, 3: success

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Program FOCAL Card",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                when (step) {
                    1 -> {
                        NfcPulseAnimation(detected = false)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Hold blank NFC card against device",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Keep card stationary until token writing finishes",
                            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Write Token NDEF",
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    2 -> {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Writing token NDEF payload...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1500)
                            step = 3
                        }
                    }

                    3 -> {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(FocalColors.Surface)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = FocalIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Card FC0004 Programmed!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "NDEF token successfully written to physical card.",
                            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Assign to Child",
                            onClick = {
                                val firstChildId = repository.childrenList.firstOrNull()?.id
                                repository.programAndAssignNfcCard("FC0004", firstChildId)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 11. CONTROLLER PROFILE SCREEN & ALL PROFILE MODALS
// -------------------------------------------------------------
@Composable
fun ControllerProfileScreen(
    onLogout: () -> Unit,
    onOpenChildrenTab: (() -> Unit)? = null,
    repository: FocalRepository = FocalRepository.instance
) {
    val user = repository.currentUser
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    var showEditNameModal by remember { mutableStateOf(false) }
    var showSubscriptionModal by remember { mutableStateOf(false) }
    var showPinModal by remember { mutableStateOf(false) }
    var showNfcManagerModal by remember { mutableStateOf(false) }
    var showNotificationSettingsModal by remember { mutableStateOf(false) }
    var showAccountAndSecurityModal by remember { mutableStateOf(false) }
    var showContactSupportModal by remember { mutableStateOf(false) }
    var showDeleteAccountModal by remember { mutableStateOf(false) }
    var showComingSoonFeature by remember { mutableStateOf<String?>(null) }

    val subRepo = com.focal.app.data.DemoSubscriptionRepository.instance
    val subDetails = subRepo.getSubscriptionDetails(user)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Controller Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Profile Header Card
        FocalCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    InitialsAvatar(name = user.name, size = 52.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusBadge(text = "FAMILY CONTROLLER", badgeType = BadgeType.ACTIVE)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                SecondaryButton(
                    text = "Edit",
                    onClick = { showEditNameModal = true },
                    modifier = Modifier.widthIn(max = 64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. FOCAL Premium Family Subscription Card
        FocalCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showSubscriptionModal = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Focal Premium Family",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(
                            text = subDetails.statusText,
                            badgeType = when {
                                subDetails.isExpired -> BadgeType.IDLE
                                subDetails.paymentFailed -> BadgeType.PENDING
                                else -> BadgeType.ACTIVE
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (subDetails.isExpired) "Expired — Tap to renew" else "Renews ${subDetails.renewalDate} at ₹${subDetails.monthlyPriceInr}/mo",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                    )
                }
                Icon(imageVector = FocalIcons.ArrowForward, contentDescription = "Manage", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: FAMILY
        Text(
            text = "FAMILY",
            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        FocalListRow(
            title = "Children (${repository.childrenList.size})",
            subtitle = "Manage paired child devices & profiles",
            icon = FocalIcons.Users,
            onClick = { onOpenChildrenTab?.invoke() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
        FocalListRow(
            title = "Parent PIN",
            subtitle = if (repository.parentPin.isNotBlank()) "PIN set ✓ (Tap to change)" else "Set 4–6 digit security PIN",
            icon = FocalIcons.Lock,
            onClick = { showPinModal = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
        FocalListRow(
            title = "Invite Another Parent",
            subtitle = "Co-management & family sharing",
            icon = FocalIcons.Add,
            onClick = { showComingSoonFeature = "Parent Co-management" },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: CARDS
        Text(
            text = "CARDS",
            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        FocalListRow(
            title = "NFC Cards",
            subtitle = "${repository.nfcCards.size} cards registered • Manage hardware tags",
            icon = FocalIcons.Nfc,
            onClick = { showNfcManagerModal = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: SETTINGS
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        FocalListRow(
            title = "Notifications",
            subtitle = "Emergency alerts & session updates",
            icon = FocalIcons.Bell,
            onClick = { showNotificationSettingsModal = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
        FocalListRow(
            title = "Account & Security",
            subtitle = "Name, email, password & security",
            icon = FocalIcons.Settings,
            onClick = { showAccountAndSecurityModal = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: SUPPORT
        Text(
            text = "SUPPORT",
            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        FocalListRow(
            title = "Help & FAQ",
            subtitle = "Setup guides & troubleshooting",
            icon = FocalIcons.Info,
            onClick = { uriHandler.openUri(com.focal.app.config.FocalConfig.FAQ_URL) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
        FocalListRow(
            title = "Contact Support",
            subtitle = "Email support or WhatsApp chat",
            icon = FocalIcons.Message,
            onClick = { showContactSupportModal = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
        FocalListRow(
            title = "Privacy Policy",
            subtitle = "Data handling & privacy guidelines",
            icon = FocalIcons.Shield,
            onClick = { uriHandler.openUri(com.focal.app.config.FocalConfig.PRIVACY_POLICY_URL) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )
        FocalListRow(
            title = "Terms of Service",
            subtitle = "FOCAL platform terms of use",
            icon = FocalIcons.Check,
            onClick = { uriHandler.openUri(com.focal.app.config.FocalConfig.TERMS_OF_SERVICE_URL) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // FOOTER ACTIONS
        SecondaryButton(
            text = "Sign Out",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        DestructiveButton(
            text = "Delete Account",
            onClick = { showDeleteAccountModal = true },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${com.focal.app.config.FocalConfig.APP_VERSION_NAME} (${com.focal.app.config.FocalConfig.APP_BUILD_NUMBER})",
            style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // MODAL DIALOGS
    if (showEditNameModal) {
        EditProfileNameModal(
            onDismiss = { showEditNameModal = false },
            repository = repository
        )
    }

    if (showSubscriptionModal) {
        SubscriptionModal(
            onDismiss = { showSubscriptionModal = false },
            repository = repository
        )
    }

    if (showPinModal) {
        ParentPinModal(
            onDismiss = { showPinModal = false },
            repository = repository
        )
    }

    if (showNfcManagerModal) {
        NfcCardsManagerModal(
            onDismiss = { showNfcManagerModal = false },
            repository = repository
        )
    }

    if (showNotificationSettingsModal) {
        NotificationSettingsModal(
            onDismiss = { showNotificationSettingsModal = false }
        )
    }

    if (showAccountAndSecurityModal) {
        AccountAndSecurityModal(
            onDismiss = { showAccountAndSecurityModal = false },
            repository = repository
        )
    }

    if (showContactSupportModal) {
        ContactSupportModal(
            onDismiss = { showContactSupportModal = false }
        )
    }

    if (showDeleteAccountModal) {
        DeleteAccountConfirmationModal(
            onDismiss = { showDeleteAccountModal = false },
            onDeleted = onLogout,
            repository = repository
        )
    }

    showComingSoonFeature?.let { feature ->
        ComingSoonModal(
            featureName = feature,
            onDismiss = { showComingSoonFeature = null }
        )
    }
}

// -------------------------------------------------------------
// 12. SUBSCRIPTION & BILLING MODAL
// -------------------------------------------------------------
@Composable
fun SubscriptionModal(
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val user = repository.currentUser
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val subRepo = com.focal.app.data.DemoSubscriptionRepository.instance
    val subDetails = subRepo.getSubscriptionDetails(user)
    var showBillingHistory by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focal Premium Family",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (subDetails.isExpired) {
                    FocalCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FocalColors.Danger.copy(alpha = 0.15f),
                        borderColor = FocalColors.Danger
                    ) {
                        Column {
                            Text(
                                text = "Subscription Expired",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = FocalColors.Danger)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Renew to keep managing policies, remote start, and QR codes. Child devices continue enforcing saved rules offline.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                FocalCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FocalColors.Surface
                ) {
                    Column {
                        StatusBadge(
                            text = subDetails.statusText,
                            badgeType = when {
                                subDetails.isExpired -> BadgeType.IDLE
                                subDetails.paymentFailed -> BadgeType.PENDING
                                else -> BadgeType.ACTIVE
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${subDetails.monthlyPriceInr} / month after 3-month trial",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Text(
                            text = "Next billing date: ${subDetails.renewalDate}",
                            style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "INCLUDED IN FAMILY PLAN",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                listOf(
                    "Up to 5 child profiles & paired devices",
                    "Unlimited focus policies & automated schedules",
                    "Real-time protection & device heartbeat status",
                    "Emergency exit request approval workflow",
                    "Focus activity reports & screen time charts",
                    "FOCAL physical NFC card tap support"
                ).forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = FocalIcons.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                FocalCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = Color(0xFF444444)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = FocalIcons.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Physical FOCAL NFC Card",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = "One-time purchase · ₹999 / card",
                                    style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SecondaryButton(
                            text = "Get a FOCAL Card",
                            onClick = { uriHandler.openUri(com.focal.app.config.FocalConfig.PHYSICAL_CARD_CHECKOUT_URL) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = if (subDetails.isExpired) "Renew Subscription" else "Manage Subscription",
                    onClick = { uriHandler.openUri(com.focal.app.config.FocalConfig.PLAY_STORE_SUBSCRIPTIONS_URL) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = "Restore Purchases",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "Billing History",
                        onClick = { showBillingHistory = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showBillingHistory) {
        Dialog(onDismissRequest = { showBillingHistory = false }) {
            FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Billing History",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        IconButton(onClick = { showBillingHistory = false }) {
                            Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    listOf(
                        "2026-09-18" to ("3-Month Free Trial Started" to "₹0"),
                        "2026-12-18" to ("FOCAL Premium Family (Upcoming)" to "₹299")
                    ).forEach { (date, detail) ->
                        val (desc, amt) = detail
                        FocalListRow(
                            title = desc,
                            subtitle = date,
                            trailingText = amt,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(text = "Close", onClick = { showBillingHistory = false }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 13. NFC CARDS MANAGER MODAL
// -------------------------------------------------------------
@Composable
fun NfcCardsManagerModal(
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var showAddCardModal by remember { mutableStateOf(false) }
    var cardToRevoke by remember { mutableStateOf<FocusCard?>(null) }
    var cardToAssign by remember { mutableStateOf<FocusCard?>(null) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NFC Cards Manager",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (repository.nfcCards.isEmpty()) {
                    FocalCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FocalColors.Surface
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            Icon(imageVector = FocalIcons.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Cards Registered Yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add Card' to pair a physical FOCAL card using phone NFC or ACR122U USB reader.",
                                style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    repository.nfcCards.forEach { card ->
                        val assignedChild = repository.childrenList.find { it.id == card.assignedChildId }
                        FocalCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            backgroundColor = FocalColors.Surface
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = FocalIcons.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = card.cardId,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                            )
                                            Text(
                                                text = "Token: ••••${card.token.takeLast(4)} • ${if (assignedChild != null) "Assigned to ${assignedChild.name}" else "Not assigned"}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                            )
                                        }
                                    }
                                    StatusBadge(
                                        text = card.status.name,
                                        badgeType = if (card.status == CardStatus.DEACTIVATED || card.status == CardStatus.REVOKED) BadgeType.IDLE else BadgeType.ACTIVE
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SecondaryButton(
                                        text = if (assignedChild != null) "Reassign" else "Assign",
                                        onClick = { cardToAssign = card },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (assignedChild != null) {
                                        SecondaryButton(
                                            text = "Unpair",
                                            onClick = { repository.unpairCardFromChild(card.cardId) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    DestructiveButton(
                                        text = "Revoke",
                                        onClick = { cardToRevoke = card },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                PrimaryButton(
                    text = "Add Card",
                    onClick = { showAddCardModal = true },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = "Get a FOCAL Card (₹999)",
                    onClick = { uriHandler.openUri(com.focal.app.config.FocalConfig.PHYSICAL_CARD_CHECKOUT_URL) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showAddCardModal) {
        AddCardFlowModal(
            onDismiss = { showAddCardModal = false },
            repository = repository
        )
    }

    if (cardToRevoke != null) {
        Dialog(onDismissRequest = { cardToRevoke = null }) {
            FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                Column {
                    Text(
                        text = "Revoke Card ${cardToRevoke!!.cardId}?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will permanently deactivate the NFC tag token. The card will no longer unlock focus sessions.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DestructiveButton(
                            text = "Revoke Card",
                            onClick = {
                                repository.adminRevokeCard(cardToRevoke!!.cardId)
                                cardToRevoke = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Cancel",
                            onClick = { cardToRevoke = null },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (cardToAssign != null) {
        Dialog(onDismissRequest = { cardToAssign = null }) {
            FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                Column {
                    Text(
                        text = "Assign Card ${cardToAssign!!.cardId}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (repository.childrenList.isEmpty()) {
                        Text("No children profiles found.", style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary))
                    } else {
                        repository.childrenList.forEach { child ->
                            FocalListRow(
                                title = child.name,
                                subtitle = "Assign card to ${child.name}",
                                icon = FocalIcons.Users,
                                onClick = {
                                    repository.programAndAssignNfcCard(cardToAssign!!.cardId, child.id)
                                    cardToAssign = null
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(text = "Cancel", onClick = { cardToAssign = null }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 14. FOCUS QR GENERATOR MODAL
// -------------------------------------------------------------
@Composable
fun FocusQrGeneratorModal(
    preselectedChildId: String? = null,
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var selectedChildId by remember { mutableStateOf(preselectedChildId ?: repository.childrenList.firstOrNull()?.id ?: "") }
    var selectedPolicyId by remember { mutableStateOf(repository.policies.firstOrNull()?.id ?: "") }
    var selectedDuration by remember { mutableIntStateOf(60) }
    var generatedQr by remember { mutableStateOf<QrToken?>(null) }
    var simulatedScanSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Generate Focus QR",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (generatedQr == null) {
                    Text(
                        text = "SELECT CHILD",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    repository.childrenList.forEach { child ->
                        val isSel = child.id == selectedChildId
                        FocalCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            backgroundColor = if (isSel) Color(0xFF1E1E1E) else FocalColors.Surface,
                            borderColor = if (isSel) Color.White else FocalColors.Border,
                            onClick = { selectedChildId = child.id }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InitialsAvatar(name = child.name, size = 32.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = child.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SELECT POLICY",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    repository.policies.forEach { policy ->
                        val isSel = policy.id == selectedPolicyId
                        FocalCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            backgroundColor = if (isSel) Color(0xFF1E1E1E) else FocalColors.Surface,
                            borderColor = if (isSel) Color.White else FocalColors.Border,
                            onClick = { selectedPolicyId = policy.id }
                        ) {
                            Text(text = policy.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SESSION DURATION",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30, 60, 90, 120).forEach { mins ->
                            val isSel = selectedDuration == mins
                            SecondaryButton(
                                text = "${mins}m",
                                onClick = { selectedDuration = mins },
                                borderColor = if (isSel) Color.White else FocalColors.BorderSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryButton(
                        text = "Generate Focus QR",
                        onClick = {
                            val policy = repository.policies.find { it.id == selectedPolicyId }
                            val qr = repository.generateFocusQr(
                                childId = selectedChildId,
                                policyId = selectedPolicyId,
                                policyTitle = "${policy?.name ?: "Focus"} (${selectedDuration / 60}h)",
                                durationMinutes = selectedDuration
                            )
                            generatedQr = qr
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val child = repository.childrenList.find { it.id == generatedQr!!.childId }
                    val fallbackCode = generatedQr!!.token.takeLast(6)

                    if (simulatedScanSuccess) {
                        FocalCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = FocalColors.Surface,
                            borderColor = Color.White
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = FocalIcons.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${child?.name ?: "Child"} started focus ✓",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Focus policy activated on target device.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        PrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Scan QR on ${child?.name ?: "Child"}'s Phone",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            QrCodeView(qrContent = generatedQr!!.token, size = 180.dp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "FALLBACK 6-DIGIT CODE",
                                style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, letterSpacing = 1.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = fallbackCode,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 4.sp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            SecondaryButton(
                                text = "Regenerate QR",
                                onClick = {
                                    val policy = repository.policies.find { it.id == selectedPolicyId }
                                    generatedQr = repository.generateFocusQr(
                                        childId = selectedChildId,
                                        policyId = selectedPolicyId,
                                        policyTitle = "${policy?.name ?: "Focus"} (${selectedDuration / 60}h)",
                                        durationMinutes = selectedDuration
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PrimaryButton(
                                text = "Simulate Child Scan",
                                onClick = {
                                    simulatedScanSuccess = true
                                    val cIdx = repository.childrenList.indexOfFirst { it.id == selectedChildId }
                                    if (cIdx >= 0) {
                                        repository.childrenList[cIdx] = repository.childrenList[cIdx].copy(isFocusing = true, remainingMinutes = selectedDuration)
                                    }
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
// 15. NOTIFICATION SETTINGS MODAL
// -------------------------------------------------------------
@Composable
fun NotificationSettingsModal(
    onDismiss: () -> Unit
) {
    var sessionStartedNotif by remember { mutableStateOf(true) }
    var sessionCompletedNotif by remember { mutableStateOf(true) }
    var deviceOfflineNotif by remember { mutableStateOf(true) }
    var scheduleNotif by remember { mutableStateOf(true) }
    val isSystemPermissionGranted = PlatformPermissionsHelper.isNotificationPermissionGranted()

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notification Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isSystemPermissionGranted) {
                    FocalCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FocalColors.Danger.copy(alpha = 0.15f),
                        borderColor = FocalColors.Danger
                    ) {
                        Column {
                            Text(
                                text = "You won't get alerts",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = FocalColors.Danger)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "System notification permission is currently disabled on this phone.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            SecondaryButton(
                                text = "Open Settings",
                                onClick = { PlatformPermissionsHelper.openNotificationSettings() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "SAFETY ALERTS (LOCKED ON)",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Safety alerts cannot be turned off to guarantee parent protection.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextMuted)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FocalListRow(
                        title = "Emergency Exit Requests",
                        subtitle = "Instant alert when child requests early session exit",
                        icon = FocalIcons.Lock,
                        trailingText = "LOCKED ON",
                        modifier = Modifier.fillMaxWidth()
                    )
                    FocalListRow(
                        title = "Protection Off Alerts",
                        subtitle = "Alert when accessibility or permissions are tampered",
                        icon = FocalIcons.Lock,
                        trailingText = "LOCKED ON",
                        modifier = Modifier.fillMaxWidth()
                    )
                    FocalListRow(
                        title = "Phone Offline Alerts",
                        subtitle = "Alert when child device loses signal >30m",
                        icon = FocalIcons.Lock,
                        trailingText = "LOCKED ON",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "UPDATE ALERTS (TOGGLABLE)",
                        style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FocalListRow(
                        title = "Session Started Alerts",
                        subtitle = "Notify when focus session begins",
                        trailingToggle = sessionStartedNotif,
                        onToggleChange = { sessionStartedNotif = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FocalListRow(
                        title = "Session Completed Alerts",
                        subtitle = "Summary when focus session completes",
                        trailingToggle = sessionCompletedNotif,
                        onToggleChange = { sessionCompletedNotif = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FocalListRow(
                        title = "Schedule Routine Triggers",
                        subtitle = "Notification when automated schedule runs",
                        trailingToggle = scheduleNotif,
                        onToggleChange = { scheduleNotif = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FocalListRow(
                        title = "Device Offline Updates",
                        subtitle = "Routine connectivity reports",
                        trailingToggle = deviceOfflineNotif,
                        onToggleChange = { deviceOfflineNotif = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(text = "Save Preferences", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// -------------------------------------------------------------
// 16. ACCOUNT & SECURITY MODALS
// -------------------------------------------------------------
@Composable
fun AccountAndSecurityModal(
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var showEditNameModal by remember { mutableStateOf(false) }
    var showChangeEmailModal by remember { mutableStateOf(false) }
    var showChangePasswordModal by remember { mutableStateOf(false) }
    var showPinModal by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account & Security",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FocalListRow(
                    title = "Parent Name",
                    subtitle = repository.currentUser.name,
                    icon = FocalIcons.Users,
                    onClick = { showEditNameModal = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
                FocalListRow(
                    title = "Account Email",
                    subtitle = repository.currentUser.email,
                    icon = FocalIcons.Message,
                    onClick = { showChangeEmailModal = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
                FocalListRow(
                    title = "Account Password",
                    subtitle = "••••••••••••",
                    icon = FocalIcons.Lock,
                    onClick = { showChangePasswordModal = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
                FocalListRow(
                    title = "Parent Authorization PIN",
                    subtitle = if (repository.parentPin.isNotBlank()) "PIN set ✓" else "Set 4–6 digit security PIN",
                    icon = FocalIcons.Settings,
                    onClick = { showPinModal = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showEditNameModal) EditProfileNameModal(onDismiss = { showEditNameModal = false }, repository = repository)
    if (showChangeEmailModal) ChangeEmailModal(onDismiss = { showChangeEmailModal = false }, repository = repository)
    if (showChangePasswordModal) ChangePasswordModal(onDismiss = { showChangePasswordModal = false })
    if (showPinModal) ParentPinModal(onDismiss = { showPinModal = false }, repository = repository)
}

@Composable
fun EditProfileNameModal(
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var nameInput by remember { mutableStateOf(repository.currentUser.name) }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column {
                Text(text = "Edit Profile Name", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(16.dp))
                FocalTextField(value = nameInput, onValueChange = { nameInput = it }, label = "Full Name")
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = "Save Name",
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            repository.updateParentName(nameInput.trim())
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ChangeEmailModal(
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var currentPassword by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf(repository.currentUser.email) }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column {
                Text(text = "Change Account Email", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(16.dp))
                FocalTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = "Current Password", isPassword = true)
                Spacer(modifier = Modifier.height(12.dp))
                FocalTextField(value = newEmail, onValueChange = { newEmail = it }, label = "New Email Address")
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = "Update Email",
                    onClick = {
                        if (newEmail.isNotBlank()) {
                            repository.updateParentEmail(newEmail.trim())
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ChangePasswordModal(
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column {
                Text(text = "Change Password", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(16.dp))
                FocalTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = "Current Password", isPassword = true)
                Spacer(modifier = Modifier.height(12.dp))
                FocalTextField(value = newPassword, onValueChange = { newPassword = it }, label = "New Password", isPassword = true)
                Spacer(modifier = Modifier.height(12.dp))
                FocalTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm New Password", isPassword = true)
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(text = "Save New Password", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ParentPinModal(
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var pinInput by remember { mutableStateOf(repository.parentPin) }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column {
                Text(text = "Parent Authorization PIN", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Set a 4–6 digit PIN used to authorize emergency exit approvals or override settings on child device.", style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary))
                Spacer(modifier = Modifier.height(16.dp))
                FocalTextField(value = pinInput, onValueChange = { pinInput = it }, label = "Security PIN", isPassword = true)
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = "Save Security PIN",
                    onClick = {
                        repository.updateParentPin(pinInput)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 17. DELETE ACCOUNT CONFIRMATION MODAL
// -------------------------------------------------------------
@Composable
fun DeleteAccountConfirmationModal(
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var confirmPassword by remember { mutableStateOf("") }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            borderColor = FocalColors.Danger
        ) {
            Column {
                Text(
                    text = "Delete FOCAL Account?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = FocalColors.Danger)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Deleting your account will permanently purge all paired child profiles, focus policies, schedules, and historical telemetry data.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                FocalCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FocalColors.Surface
                ) {
                    Text(
                        text = "⚠️ STORE SUBSCRIPTION NOTICE: Deleting your FOCAL account does NOT automatically cancel your Google Play / App Store subscription. You must cancel active subscriptions in the store.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(
                        text = "Manage Store Subscription",
                        onClick = { uriHandler.openUri(com.focal.app.config.FocalConfig.PLAY_STORE_SUBSCRIPTIONS_URL) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                FocalTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Re-enter Password to Confirm", isPassword = true)

                Spacer(modifier = Modifier.height(24.dp))
                DestructiveButton(
                    text = "Permanently Delete Account",
                    onClick = {
                        repository.deleteParentAccount()
                        onDismiss()
                        onDeleted()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Web deletion policy: ${com.focal.app.config.FocalConfig.ACCOUNT_DELETION_URL}",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextMuted, fontSize = 10.sp),
                    modifier = Modifier.clickable { uriHandler.openUri(com.focal.app.config.FocalConfig.ACCOUNT_DELETION_URL) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 18. CONTACT SUPPORT MODAL
// -------------------------------------------------------------
@Composable
fun ContactSupportModal(
    onDismiss: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contact FOCAL Support",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FocalListRow(
                    title = "Email Support",
                    subtitle = com.focal.app.config.FocalConfig.SUPPORT_EMAIL,
                    icon = FocalIcons.Message,
                    onClick = { uriHandler.openUri("mailto:${com.focal.app.config.FocalConfig.SUPPORT_EMAIL}") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                FocalListRow(
                    title = "WhatsApp Support Chat",
                    subtitle = "Direct messaging support line",
                    icon = FocalIcons.Message,
                    onClick = { uriHandler.openUri(com.focal.app.config.FocalConfig.WHATSAPP_SUPPORT_URL) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// -------------------------------------------------------------
// AUXILIARY MODALS (NOTIFICATIONS & CHILD DETAILS)
// -------------------------------------------------------------
@Composable
fun NotificationsModal(
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Alerts
    var selectedEmergencyReq by remember { mutableStateOf<EmergencyExitRequest?>(null) }

    val notificationsList = repository.notifications
    val filteredList = if (selectedTabIndex == 1) {
        notificationsList.filter { it.type == "EMERGENCY_REQUEST" || it.type == "PROTECTION_OFF" || it.type == "DEVICE_OFFLINE" }
    } else {
        notificationsList
    }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SegmentedTabs(
                    tabs = listOf("All", "Alerts"),
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No notifications in this category",
                            style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredList.forEach { notif ->
                            when (notif.type) {
                                "EMERGENCY_REQUEST" -> {
                                    val req = repository.emergencyRequests.find { notif.message.contains(it.childName) }
                                        ?: repository.emergencyRequests.firstOrNull()

                                    FocalCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        borderColor = FocalColors.Danger,
                                        onClick = { if (req != null) selectedEmergencyReq = req }
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FocalColors.Danger))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(text = notif.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                                }
                                                Text(text = notif.timestamp, style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(text = notif.message, style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary))

                                            if (req != null && req.status == "PENDING") {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    PrimaryButton(
                                                        text = "Approve Exit",
                                                        onClick = { repository.resolveEmergencyRequest(req.id, true) },
                                                        modifier = Modifier.weight(1f).height(40.dp)
                                                    )
                                                    SecondaryButton(
                                                        text = "Decline",
                                                        onClick = { repository.resolveEmergencyRequest(req.id, false) },
                                                        modifier = Modifier.weight(1f).height(40.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                "PROTECTION_OFF" -> {
                                    FocalCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        borderColor = FocalColors.Danger
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(FocalColors.Danger.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(imageVector = FocalIcons.Shield, contentDescription = null, tint = FocalColors.Danger, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = notif.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = FocalColors.Danger))
                                                Text(text = notif.message, style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary))
                                            }
                                            Text(text = notif.timestamp, style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                                        }
                                    }
                                }

                                "DEVICE_OFFLINE" -> {
                                    FocalCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(FocalColors.Surface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(imageVector = FocalIcons.Smartphone, contentDescription = null, tint = FocalColors.TextSecondary, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = notif.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                                Text(text = notif.message, style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary))
                                            }
                                            Text(text = notif.timestamp, style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                                        }
                                    }
                                }

                                else -> {
                                    FocalListRow(
                                        title = notif.title,
                                        subtitle = "${notif.message} • ${notif.timestamp}",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (selectedEmergencyReq != null) {
        EmergencyRequestDetailModal(
            request = selectedEmergencyReq!!,
            onDismiss = { selectedEmergencyReq = null },
            repository = repository
        )
    }
}

@Composable
fun EmergencyRequestDetailModal(
    request: EmergencyExitRequest,
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(name = request.childName, size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = request.childName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            Text(text = "Exit Request • ${request.requestedAt}", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "REASON FOR EXIT REQUEST",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                FocalCard(modifier = Modifier.fillMaxWidth(), backgroundColor = FocalColors.Surface) {
                    Text(
                        text = "\"${request.reason}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = "Approve Exit",
                        onClick = {
                            repository.resolveEmergencyRequest(request.id, true)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = "Decline",
                        onClick = {
                            repository.resolveEmergencyRequest(request.id, false)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChildPoliciesModal(
    child: ChildProfile,
    onDismiss: () -> Unit,
    onCreateFocus: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${child.name}'s Policies",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                repository.policies.forEach { pol ->
                    FocalListRow(
                        title = pol.name,
                        subtitle = "${pol.blockedApps.size} apps · ${pol.blockedWebsites.size} sites",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                PrimaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ChildActivityModal(
    child: ChildProfile,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Week, 1: Month

    val weekBarData = listOf(
        BarChartItem("Mon", 4.5f, 2.0f),
        BarChartItem("Tue", 5.0f, 3.0f),
        BarChartItem("Wed", 3.5f, 4.0f),
        BarChartItem("Thu", 6.0f, 2.0f),
        BarChartItem("Fri", 5.5f, 3.5f),
        BarChartItem("Sat", 2.0f, 5.0f),
        BarChartItem("Sun", 3.0f, 4.5f)
    )

    val monthBarData = listOf(
        BarChartItem("W1", 18.0f, 12.0f),
        BarChartItem("W2", 22.5f, 14.0f),
        BarChartItem("W3", 16.0f, 18.0f),
        BarChartItem("W4", 20.0f, 15.0f)
    )

    val activeBarData = if (selectedTab == 0) weekBarData else monthBarData

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${child.name}'s Insights & Activity",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SegmentedTabs(
                    tabs = listOf("Week", "Month"),
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // StatCards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "Focus Time",
                        value = if (selectedTab == 0) "29h 30m" else "76h 30m",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Screen Time",
                        value = if (selectedTab == 0) "24h 00m" else "59h 00m",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Focus Score",
                        value = "${child.focusScore}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BarChart Focus vs Screen Time
                BarChart(
                    items = activeBarData,
                    modifier = Modifier.fillMaxWidth(),
                    maxHours = if (selectedTab == 0) 8f else 30f
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "TOP BLOCKED APPS",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    Triple("Instagram", "14 attempts blocked", "1h 40m saved"),
                    Triple("YouTube", "8 attempts blocked", "45m saved"),
                    Triple("TikTok", "5 attempts blocked", "30m saved")
                ).forEach { (app, attempts, timeSaved) ->
                    FocalListRow(
                        title = app,
                        subtitle = "$attempts • $timeSaved",
                        icon = FocalIcons.Smartphone,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "RECENT ACTIVITY LOG",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    "Instagram Launch Blocked" to "Today at 04:12 PM",
                    "Study Session Started" to "Today at 04:00 PM via NFC",
                    "YouTube Shorts Intercepted" to "Yesterday at 05:30 PM"
                ).forEach { (act, time) ->
                    FocalListRow(
                        title = act,
                        subtitle = time,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ChildDeviceCardModal(
    child: ChildProfile,
    onDismiss: () -> Unit,
    onGenerateQr: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Device & Card Status",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Device Info Card with Heartbeat & Battery Status
                FocalCard(modifier = Modifier.fillMaxWidth(), backgroundColor = FocalColors.Surface) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = child.deviceName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                                Text(
                                    text = if (child.isOnline) "Heartbeat active • Battery ${repository.lastHeartbeatBatteryLevel}%" else "No signal for 30 min",
                                    style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusDot(active = child.isOnline)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (child.isOnline) "Online" else "Offline",
                                    style = MaterialTheme.typography.labelSmall.copy(color = if (child.isOnline) Color.White else FocalColors.Danger)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "PERMISSIONS STATUS",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val isAcc = PlatformPermissionsHelper.isAccessibilityEnabled()
                val isOvl = PlatformPermissionsHelper.isOverlayEnabled()
                val isUsg = PlatformPermissionsHelper.isUsageAccessEnabled()
                val isNot = PlatformPermissionsHelper.isNotificationPermissionGranted()

                listOf(
                    "Accessibility Service" to isAcc,
                    "Display Over Other Apps" to isOvl,
                    "Usage Access" to isUsg,
                    "Notifications Permission" to isNot
                ).forEach { (perm, granted) ->
                    FocalListRow(
                        title = perm,
                        subtitle = if (granted) "Granted ✓" else "Disabled ⚠️ (Action Required)",
                        icon = if (granted) FocalIcons.Check else FocalIcons.Lock,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "PAIRED HARDWARE CARD",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                var showAddCardModalForChild by remember { mutableStateOf(false) }
                val pairedCard = repository.nfcCards.find { it.assignedChildId == child.id || it.cardId.equals(child.pairedCardId ?: "", ignoreCase = true) }

                FocalCard(modifier = Modifier.fillMaxWidth(), backgroundColor = FocalColors.Surface) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = FocalIcons.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = pairedCard?.cardId ?: child.pairedCardId ?: "No card paired",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                    Text(
                                        text = if (pairedCard != null) "Token: ${pairedCard.token}" else "Card unassigned",
                                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                                    )
                                }
                            }
                            StatusBadge(
                                text = if (pairedCard != null) "PAIRED" else "UNPAIRED",
                                badgeType = if (pairedCard != null) BadgeType.ACTIVE else BadgeType.IDLE
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton(
                                text = if (pairedCard != null) "Add / Change Card" else "Add Card",
                                onClick = { showAddCardModalForChild = true },
                                modifier = Modifier.weight(1f)
                            )
                            if (pairedCard != null) {
                                SecondaryButton(
                                    text = "Unpair",
                                    onClick = { repository.unpairCardFromChild(pairedCard.cardId) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (showAddCardModalForChild) {
                    AddCardFlowModal(
                        preSelectedChildId = child.id,
                        onDismiss = { showAddCardModalForChild = false },
                        repository = repository
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "CARD CUSTODY SETTING",
                    style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                var custodyMode by remember { mutableStateOf(child.cardCustodyMode) }

                FocalListRow(
                    title = "Card Custody",
                    subtitle = custodyMode + if (custodyMode == "I keep the card") " (Strict Mode)" else " (Default)",
                    icon = FocalIcons.Nfc,
                    trailingIcon = FocalIcons.ArrowForward,
                    onClick = {
                        val nextMode = if (custodyMode == "Card stays with child") "I keep the card" else "Card stays with child"
                        custodyMode = nextMode
                        val idx = repository.childrenList.indexOfFirst { it.id == child.id }
                        if (idx >= 0) {
                            repository.childrenList[idx] = repository.childrenList[idx].copy(cardCustodyMode = nextMode)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                SecondaryButton(
                    text = "Pair New Phone",
                    onClick = {
                        onDismiss()
                        onGenerateQr()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun FocusSummaryScreen(
    session: FocusSession,
    onDismiss: () -> Unit
) {
    val durationHours = session.targetDurationMinutes / 60
    val durationMins = session.targetDurationMinutes % 60
    val durationText = if (durationHours > 0) "${durationHours}h ${if (durationMins < 10) "0" else ""}${durationMins}m focused" else "${durationMins}m focused"

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(FocalColors.Surface)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = durationText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${session.policyName} session complete",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FocalColors.TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                PrimaryButton(
                    text = "Done",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AddChildModal(
    onDismiss: () -> Unit,
    onChildAdded: ((String) -> Unit)? = null,
    repository: FocalRepository = FocalRepository.instance
) {
    var childName by remember { mutableStateOf("") }
    var childAge by remember { mutableStateOf("12") }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Child Device",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FocalTextField(
                    value = childName,
                    onValueChange = { childName = it },
                    label = "Child's Name",
                    placeholder = "e.g. Rahul"
                )

                Spacer(modifier = Modifier.height(12.dp))

                FocalTextField(
                    value = childAge,
                    onValueChange = { childAge = it },
                    label = "Child's Age",
                    placeholder = "12"
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Pair Child Device",
                    onClick = {
                        val nameToUse = childName.ifBlank { "Child" }
                        val ageVal = childAge.toIntOrNull() ?: 12
                        repository.addChild(nameToUse, ageVal, "")
                        onChildAdded?.invoke(nameToUse)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CreateFocusPolicyScreen(
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    PolicyEditScreen(
        policy = null,
        onDismiss = onDismiss,
        onSave = { newPol ->
            repository.addPolicy(newPol)
            onDismiss()
        },
        onDelete = {},
        repository = repository
    )
}

@Composable
fun EditChildModal(
    child: ChildProfile,
    onDismiss: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    var nameInput by remember { mutableStateOf(child.name) }
    var ageInput by remember { mutableStateOf(child.age.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        FocalCard(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Child Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = FocalIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FocalTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = "Child's Name",
                    placeholder = "e.g. Rahul"
                )

                Spacer(modifier = Modifier.height(12.dp))

                FocalTextField(
                    value = ageInput,
                    onValueChange = { ageInput = it },
                    label = "Child's Age",
                    placeholder = "14"
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Save Changes",
                    onClick = {
                        val idx = repository.childrenList.indexOfFirst { it.id == child.id }
                        if (idx >= 0) {
                            val newAge = ageInput.toIntOrNull() ?: child.age
                            repository.childrenList[idx] = repository.childrenList[idx].copy(
                                name = nameInput.ifBlank { child.name },
                                age = newAge
                            )
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

