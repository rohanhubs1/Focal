package com.focal.app.ui.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.FocusMode
import com.focal.app.model.FocusPolicy
import com.focal.app.model.FocusPolicyData
import com.focal.app.model.InstalledApp
import com.focal.app.model.currentTimeMillis
import com.focal.app.protection.ProtectionEngine
import com.focal.app.protection.ProtectionStateRepository
import com.focal.app.ui.child.DetailScaffold
import com.focal.app.ui.child.SectionOverline
import com.focal.app.ui.child.components.GlassCard
import com.focal.app.ui.child.components.GlowArcs
import com.focal.app.ui.child.components.PrimaryButton
import com.focal.app.ui.child.components.SecondaryButton
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.child.components.FocalSwitch
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography

// =============================================================================
// SCREEN 1 — POLICIES TAB (PC1_policies.png)
// =============================================================================

@Composable
fun ParentPoliciesScreen(
    onOpenNewPolicy: () -> Unit,
    onOpenEditPolicy: (FocusPolicy) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val policiesList = repository.policies

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GlowArcs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .padding(bottom = 110.dp) // Clearance for floating ParentBottomNav
        ) {
            // Header Row: Title & Subtitle + round 48dp White "+" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Policies",
                        style = typography.display.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "What gets blocked during focus.",
                        style = typography.caption.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        )
                    )
                }

                // Round 48dp WHITE "+" button with ambient glow
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = size.minDimension / 1.7f,
                                center = Offset(size.width / 2f, size.height / 2f)
                            )
                        }
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onOpenNewPolicy() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Add,
                        contentDescription = "New Policy",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (policiesList.isEmpty()) {
                // Empty state
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 32.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No policies yet",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Create a focus policy to choose which apps and websites get locked.",
                            style = typography.body.copy(
                                fontSize = 13.5.sp,
                                color = ChildColors.Secondary
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(
                            text = "Create a policy",
                            onClick = onOpenNewPolicy,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        )
                    }
                }
            } else {
                // One GlassCard per policy
                policiesList.forEach { policy ->
                    val usage = repository.getPolicyUsage(policy.id)

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEditPolicy(policy) },
                        radius = 24.dp,
                        padding = 18.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 52dp shield icon circle (#171717 fill, 1dp #262626 border)
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(ChildColors.IconCircleBg)
                                    .border(1.dp, ChildColors.BorderStrong, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = policy.name,
                                    style = typography.headline.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 2
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "${policy.blockedApps.size} apps · ${policy.blockedWebsites.size} websites",
                                    style = typography.caption.copy(
                                        fontSize = 13.sp,
                                        color = ChildColors.Secondary
                                    ),
                                    maxLines = 2
                                )

                                // "used by" chips under it (24dp tall, #2A2A2A border, 11sp)
                                if (usage.isUsed) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        usage.children.forEach { child ->
                                            Box(
                                                modifier = Modifier
                                                    .height(24.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF0E0E0E))
                                                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = child.name,
                                                    style = typography.caption.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.White
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        usage.schedules.forEach { sch ->
                                            Box(
                                                modifier = Modifier
                                                    .height(24.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF0E0E0E))
                                                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${sch.title} schedule",
                                                    style = typography.caption.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.White
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Icon(
                                imageVector = FocalIcons.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF6B6B6B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Info GlassCard at the end: lock icon + disclaimer
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 18.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ChildColors.IconCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Text(
                        text = "Settings, uninstalling and FOCAL itself are always locked during focus. Calls always work.",
                        style = typography.body.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = ChildColors.Secondary
                        )
                    )
                }
            }
        }
    }
}

// =============================================================================
// SCREEN 2 — POLICY EDITOR (PC2_policy_editor.png)
// =============================================================================

@Composable
fun PolicyEditorScreen(
    policy: FocusPolicy?,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onOpenAppPicker: (currentApps: List<String>) -> Unit,
    onOpenWebsites: (currentWebsites: List<String>) -> Unit,
    currentSelectedApps: List<String>?,
    currentSelectedWebsites: List<String>?,
    repository: FocalRepository = FocalRepository.instance,
    focusEngine: FocusEngine = FocusEngine.instance
) {
    val typography = LocalChildTypography.current
    val isNew = (policy == null)

    var policyName by remember(policy) {
        mutableStateOf(policy?.name ?: "")
    }

    // Working copies of apps and websites
    val selectedApps = remember(policy, currentSelectedApps) {
        mutableStateListOf<String>().apply {
            if (currentSelectedApps != null) {
                addAll(currentSelectedApps)
            } else if (policy != null) {
                addAll(policy.blockedApps)
            } else {
                addAll(listOf("Instagram", "YouTube", "TikTok"))
            }
        }
    }

    val selectedWebsites = remember(policy, currentSelectedWebsites) {
        mutableStateListOf<String>().apply {
            if (currentSelectedWebsites != null) {
                addAll(currentSelectedWebsites)
            } else if (policy != null) {
                addAll(policy.blockedWebsites)
            } else {
                addAll(listOf("instagram.com", "youtube.com"))
            }
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReplaceSheet by remember { mutableStateOf(false) }

    // Check if dirty
    val hasUnsavedChanges = remember(policyName, selectedApps.toList(), selectedWebsites.toList()) {
        if (isNew) {
            policyName.isNotBlank() || selectedApps.size != 3 || selectedWebsites.size != 2
        } else {
            policyName != policy.name ||
                    selectedApps.toList() != policy.blockedApps ||
                    selectedWebsites.toList() != policy.blockedWebsites
        }
    }

    fun handleBack() {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    fun handleSave() {
        val trimmed = policyName.trim()
        if (trimmed.isBlank()) {
            PlatformPermissionsHelper.showToast("Please enter a policy name")
            return
        }

        // Unique name check
        val duplicate = repository.policies.find {
            it.name.equals(trimmed, ignoreCase = true) && (isNew || it.id != policy?.id)
        }
        if (duplicate != null) {
            PlatformPermissionsHelper.showToast("A policy named '$trimmed' already exists")
            return
        }

        if (selectedApps.isEmpty() && selectedWebsites.isEmpty()) {
            PlatformPermissionsHelper.showToast("Select at least 1 app or 1 website")
            return
        }

        val updated = FocusPolicy(
            id = policy?.id ?: "pol_${currentTimeMillis()}",
            childId = policy?.childId ?: "child_1",
            name = trimmed,
            mode = policy?.mode ?: FocusMode.STUDY,
            durationMinutes = policy?.durationMinutes ?: 60,
            blockedApps = selectedApps.toList(),
            blockedWebsites = selectedWebsites.toList(),
            blockedCategories = policy?.blockedCategories ?: emptyList(),
            scheduleDays = policy?.scheduleDays ?: listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
            scheduleStartTime = policy?.scheduleStartTime ?: "16:00",
            scheduleEndTime = policy?.scheduleEndTime ?: "17:00",
            emergencyExitAllowed = policy?.emergencyExitAllowed ?: true,
            parentApprovalRequired = policy?.parentApprovalRequired ?: true
        )

        if (isNew) {
            repository.addPolicy(updated)
            PlatformPermissionsHelper.showToast("Policy '${updated.name}' created")
        } else {
            repository.updatePolicy(updated)
            PlatformPermissionsHelper.showToast("Policy '${updated.name}' saved")
        }

        // Real-time synchronization if a child is currently focusing under this policy
        if (focusEngine.currentSession?.policyId == updated.id) {
            val resolvedPkgs = updated.blockedApps.map { ProtectionEngine.APP_NAME_TO_PACKAGE[it] ?: it }.toSet()
            ProtectionStateRepository.instance.updateFocusActive(
                active = true,
                endsAtElapsed = ProtectionStateRepository.instance.state.value.focusEndsAtElapsed,
                policy = FocusPolicyData(
                    blockedApps = updated.blockedApps,
                    blockedPackages = resolvedPkgs,
                    blockedWebsites = updated.blockedWebsites
                )
            )
        }

        onSaveSuccess()
    }

    fun handleDelete() {
        if (isNew || policy == null) return

        if (repository.policies.size <= 1) {
            PlatformPermissionsHelper.showToast("You need at least one policy")
            return
        }

        val usage = repository.getPolicyUsage(policy.id)
        if (usage.isUsed) {
            showReplaceSheet = true
        } else {
            showDeleteConfirmDialog = true
        }
    }

    DetailScaffold(
        title = if (isNew) "New policy" else "Edit policy",
        onBack = { handleBack() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // NAME Section
            SectionOverline("NAME")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 18.dp,
                padding = 16.dp
            ) {
                BasicTextField(
                    value = policyName,
                    onValueChange = { policyName = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (policyName.isBlank()) {
                                Text(
                                    text = "Policy name",
                                    style = TextStyle(
                                        color = Color(0xFF6B6B6B),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            // BLOCKED DURING FOCUS Section
            SectionOverline("BLOCKED DURING FOCUS")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                // Row 1: Apps
                val appsSubtitle = if (selectedApps.isEmpty()) {
                    "None yet"
                } else {
                    val firstThree = selectedApps.take(3).joinToString(", ")
                    val remaining = selectedApps.size - 3
                    if (remaining > 0) "$firstThree +$remaining" else firstThree
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAppPicker(selectedApps.toList()) }
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ChildColors.IconCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Smartphone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apps · ${selectedApps.size}",
                            style = typography.headline.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            maxLines = 2
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = appsSubtitle,
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            ),
                            maxLines = 2
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        imageVector = FocalIcons.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF6B6B6B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 1dp indented divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 76.dp)
                        .height(1.dp)
                        .background(Color(0xFF1F1F1F))
                )

                // Row 2: Websites
                val sitesSubtitle = if (selectedWebsites.isEmpty()) {
                    "None yet"
                } else {
                    val firstTwo = selectedWebsites.take(2).joinToString(", ")
                    val remaining = selectedWebsites.size - 2
                    if (remaining > 0) "$firstTwo +$remaining" else firstTwo
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWebsites(selectedWebsites.toList()) }
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ChildColors.IconCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Globe,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Websites · ${selectedWebsites.size}",
                            style = typography.headline.copy(
                                 fontSize = 15.sp,
                                 fontWeight = FontWeight.SemiBold,
                                 color = Color.White
                            ),
                            maxLines = 2
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = sitesSubtitle,
                            style = typography.caption.copy(
                                fontSize = 12.sp,
                                color = ChildColors.Secondary
                            ),
                            maxLines = 2
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        imageVector = FocalIcons.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF6B6B6B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // USED BY Section (Edit mode only, hidden if unused)
            if (!isNew && policy != null) {
                val usage = repository.getPolicyUsage(policy.id)
                if (usage.isUsed) {
                    Spacer(Modifier.height(24.dp))
                    SectionOverline("USED BY")

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        radius = 24.dp,
                        padding = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val itemsList = mutableListOf<Pair<String, String>>()
                            usage.children.forEach { child ->
                                itemsList.add(child.name to "Default policy")
                            }
                            usage.schedules.forEach { sch ->
                                itemsList.add(sch.title to "Schedule")
                            }

                            itemsList.forEachIndexed { index, (title, subtitle) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = typography.headline.copy(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            ),
                                            maxLines = 2
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = subtitle,
                                            style = typography.caption.copy(
                                                fontSize = 12.sp,
                                                color = ChildColors.Secondary
                                            ),
                                            maxLines = 2
                                        )
                                    }
                                }

                                if (index < itemsList.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .height(1.dp)
                                            .background(Color(0xFF1F1F1F))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            // Save Button
            PrimaryButton(
                text = "Save policy",
                onClick = { handleSave() },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            )

            // Red Delete Policy button (edit mode only)
            if (!isNew) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { handleDelete() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete policy",
                        style = typography.headline.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF453A)
                        )
                    )
                }
            }
        }
    }

    // Discard Changes Dialog
    if (showDiscardDialog) {
        Dialog(onDismissRequest = { showDiscardDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Discard changes?",
                        style = typography.headline.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "You have unsaved changes that will be lost.",
                        style = typography.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryButton(
                            text = "Keep editing",
                            onClick = { showDiscardDialog = false },
                            modifier = Modifier.weight(1f).height(46.dp)
                        )
                        PrimaryButton(
                            text = "Discard",
                            onClick = {
                                showDiscardDialog = false
                                onBack()
                            },
                            modifier = Modifier.weight(1f).height(46.dp)
                        )
                    }
                }
            }
        }
    }

    // Delete Policy Confirmation Dialog (when NOT in use)
    if (showDeleteConfirmDialog && policy != null) {
        Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Delete policy?",
                        style = typography.headline.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to delete '${policy.name}'? This cannot be undone.",
                        style = typography.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryButton(
                            text = "Cancel",
                            onClick = { showDeleteConfirmDialog = false },
                            modifier = Modifier.weight(1f).height(46.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color(0xFFFF453A))
                                .clickable {
                                    showDeleteConfirmDialog = false
                                    repository.deletePolicy(policy.id)
                                    PlatformPermissionsHelper.showToast("Policy deleted")
                                    onBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete",
                                style = typography.headline.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Replace Policy Bottom Sheet (when policy IS in use)
    if (showReplaceSheet && policy != null) {
        ReplacePolicySheet(
            policyToDelete = policy,
            onDismiss = { showReplaceSheet = false },
            onReplacedAndDeleted = {
                showReplaceSheet = false
                onBack()
            },
            repository = repository
        )
    }
}

// =============================================================================
// SCREEN 3 — APP PICKER (PC3_app_picker.png)
// =============================================================================

@Composable
fun AppPickerScreen(
    initialSelectedApps: List<String>,
    onBack: () -> Unit,
    onSaveApps: (List<String>) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current

    val currentSelected = remember(initialSelectedApps) {
        mutableStateListOf<String>().apply { addAll(initialSelectedApps) }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Categories
    val categories = listOf("All", "Social", "Video", "Games", "Shopping", "Browsers", "Other")

    // Filter out unblockable apps (Phone, Dialer, Settings, FOCAL, system UI, emergency, launcher)
    val blockableApps = remember(repository.childInstalledApps) {
        repository.childInstalledApps.filter { repository.isAppBlockable(it) }
    }

    // Sort: selected apps sorted to top when the screen opens
    val sortedInitialApps = remember {
        val initiallySelected = initialSelectedApps.toSet()
        blockableApps.sortedWith(
            compareByDescending<InstalledApp> { it.appName in initiallySelected }
                .thenBy { it.appName.lowercase() }
        )
    }

    // Subtitle dynamically based on children names
    val childrenSubtitle = remember(repository.childrenList) {
        val list = repository.childrenList
        when (list.size) {
            0 -> "Apps on this phone."
            1 -> "Apps on ${list[0].name}'s phone."
            2 -> "Apps on ${list[0].name}'s and ${list[1].name}'s phones."
            else -> "Apps on ${list.take(2).joinToString(", ") { it.name }} and others' phones."
        }
    }

    // Filter by search & category
    val filteredApps = remember(searchQuery, selectedCategory, sortedInitialApps) {
        sortedInitialApps.filter { app ->
            val matchesCat = (selectedCategory == "All") ||
                    app.category.equals(selectedCategory, ignoreCase = true) ||
                    (selectedCategory == "Browsers" && app.category.startsWith("Browser", ignoreCase = true))

            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.category.contains(searchQuery, ignoreCase = true)

            matchesCat && matchesSearch
        }
    }

    // Category auto-block tracking
    val blockedCategories = remember { mutableStateListOf<String>() }

    DetailScaffold(
        title = "Blocked apps",
        onBack = onBack,
        actions = {
            // StatusPill "{n} selected" top-right
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0E0E0E))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${currentSelected.size} selected",
                    style = typography.caption.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = childrenSubtitle,
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(16.dp))

            // Search Bar (GlassCard radius 18, search icon, placeholder "Search apps")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 18.dp,
                padding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FocalIcons.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF6B6B6B),
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isBlank()) {
                                    Text(
                                        text = "Search apps",
                                        style = TextStyle(
                                            color = Color(0xFF6B6B6B),
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (searchQuery.isNotBlank()) {
                        Icon(
                            imageVector = FocalIcons.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF6B6B6B),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Category Chips, horizontal scroll, single-select
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = (selectedCategory == cat)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color.White else Color(0xFF0E0E0E))
                            .border(
                                1.dp,
                                if (isSelected) Color.White else Color(0xFF2A2A2A),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        )
                    }
                }
            }

            // Auto-block category row when a specific category is selected
            if (selectedCategory != "All") {
                Spacer(Modifier.height(14.dp))
                val isCatBlocked = blockedCategories.contains(selectedCategory)

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 20.dp,
                    padding = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Block all $selectedCategory apps",
                                style = typography.headline.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Newly installed $selectedCategory apps will be blocked automatically",
                                style = typography.caption.copy(
                                    fontSize = 12.sp,
                                    color = ChildColors.Secondary
                                )
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        FocalSwitch(
                            checked = isCatBlocked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (!blockedCategories.contains(selectedCategory)) {
                                        blockedCategories.add(selectedCategory)
                                    }
                                    // Auto check all apps in this category
                                    blockableApps
                                        .filter {
                                            it.category.equals(selectedCategory, ignoreCase = true) ||
                                                    (selectedCategory == "Browsers" && it.category.startsWith("Browser", ignoreCase = true))
                                        }
                                        .forEach { app ->
                                            if (!currentSelected.contains(app.appName)) {
                                                currentSelected.add(app.appName)
                                            }
                                        }
                                } else {
                                    blockedCategories.remove(selectedCategory)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // App list in ONE GlassCard with dividers
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps found",
                            style = typography.caption.copy(
                                fontSize = 14.sp,
                                color = ChildColors.Secondary
                            )
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filteredApps.forEachIndexed { index, app ->
                            val isChecked = currentSelected.contains(app.appName)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            currentSelected.remove(app.appName)
                                        } else {
                                            currentSelected.add(app.appName)
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 36dp rounded tile with app letter / icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF171717))
                                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.appName.firstOrNull()?.toString() ?: "?",
                                        style = typography.headline.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = typography.headline.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (app.childOwnerName.isNotBlank()) "${app.category} · ${app.childOwnerName}" else app.category,
                                        style = typography.caption.copy(
                                            fontSize = 12.sp,
                                            color = ChildColors.Secondary
                                        ),
                                        maxLines = 2
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                // Checkbox on the right (24dp, radius 7; checked = white fill + black check; unchecked = #3A3A3A outline)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (isChecked) Color.White else Color.Transparent)
                                        .border(
                                            width = if (isChecked) 1.dp else 1.5.dp,
                                            color = if (isChecked) Color.White else Color(0xFF3A3A3A),
                                            shape = RoundedCornerShape(7.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecked) {
                                        Icon(
                                            imageVector = FocalIcons.Check,
                                            contentDescription = "Checked",
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            if (index < filteredApps.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 68.dp)
                                        .height(1.dp)
                                        .background(Color(0xFF1F1F1F))
                                    )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // PrimaryButton "Save · {n} apps" pinned bottom
            PrimaryButton(
                text = "Save · ${currentSelected.size} apps",
                onClick = {
                    onSaveApps(currentSelected.toList())
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            )
        }
    }
}

// =============================================================================
// SCREEN 4 — WEBSITES (PC4_websites.png)
// =============================================================================

@Composable
fun WebsitesScreen(
    initialSelectedWebsites: List<String>,
    onBack: () -> Unit,
    onSaveWebsites: (List<String>) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current

    val blockedWebsites = remember(initialSelectedWebsites) {
        mutableStateListOf<String>().apply { addAll(initialSelectedWebsites) }
    }

    var websiteInput by remember { mutableStateOf("") }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var undoBatch by remember { mutableStateOf<List<String>?>(null) }

    fun normalizeAndAdd(raw: String) {
        val clean = repository.normalizeDomain(raw)
        if (clean.isBlank()) return

        if (!repository.isValidDomainFormat(clean)) {
            inlineError = "Enter a website like reddit.com"
            return
        }

        if (blockedWebsites.contains(clean)) {
            inlineError = "Already blocked"
            return
        }

        inlineError = null
        blockedWebsites.add(0, clean)
        websiteInput = ""
    }

    fun quickAdd(category: String, sites: List<String>) {
        val addedList = mutableListOf<String>()
        sites.forEach { site ->
            val clean = repository.normalizeDomain(site)
            if (clean.isNotBlank() && !blockedWebsites.contains(clean)) {
                blockedWebsites.add(0, clean)
                addedList.add(clean)
            }
        }
        if (addedList.isNotEmpty()) {
            undoBatch = addedList
        } else {
            PlatformPermissionsHelper.showToast("All $category websites already added")
        }
    }

    DetailScaffold(
        title = "Blocked websites",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Blocked in every browser on their phones.",
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(18.dp))

            // Input Row: GlassCard text field + 50dp round WHITE "+" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    radius = 18.dp,
                    padding = 14.dp
                ) {
                    BasicTextField(
                        value = websiteInput,
                        onValueChange = {
                            websiteInput = it
                            inlineError = null
                        },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (websiteInput.isBlank()) {
                                    Text(
                                        text = "Add a website, e.g. reddit.com",
                                        style = TextStyle(
                                            color = Color(0xFF6B6B6B),
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(10.dp))

                // 50dp round WHITE "+" button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = size.minDimension / 1.7f,
                                center = Offset(size.width / 2f, size.height / 2f)
                            )
                        }
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { normalizeAndAdd(websiteInput) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FocalIcons.Add,
                        contentDescription = "Add Website",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (inlineError != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = inlineError!!,
                    style = typography.caption.copy(
                        fontSize = 12.sp,
                        color = Color(0xFFA3A3A3)
                    ),
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // QUICK ADD Section
            SectionOverline("QUICK ADD")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Social
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF0E0E0E))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(17.dp))
                        .clickable {
                            quickAdd(
                                "Social",
                                listOf("instagram.com", "facebook.com", "snapchat.com", "x.com", "reddit.com")
                            )
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Social",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    )
                }

                // Video
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF0E0E0E))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(17.dp))
                        .clickable {
                            quickAdd(
                                "Video",
                                listOf("youtube.com", "netflix.com", "primevideo.com", "hotstar.com")
                            )
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Video",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    )
                }

                // Games
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF0E0E0E))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(17.dp))
                        .clickable {
                            quickAdd(
                                "Games",
                                listOf("roblox.com", "poki.com", "crazygames.com")
                            )
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Games",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    )
                }

                // Shopping
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF0E0E0E))
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(17.dp))
                        .clickable {
                            quickAdd(
                                "Shopping",
                                listOf("amazon.in", "flipkart.com", "myntra.com")
                            )
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Shopping",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    )
                }
            }

            // Undo Banner
            if (undoBatch != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF171717))
                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Added ${undoBatch!!.size} websites",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Undo",
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.clickable {
                                undoBatch?.let { batch ->
                                    blockedWebsites.removeAll(batch)
                                }
                                undoBatch = null
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // BLOCKED · {n} Section
            SectionOverline("BLOCKED · ${blockedWebsites.size}")

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 24.dp,
                padding = 0.dp
            ) {
                if (blockedWebsites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No websites blocked yet",
                            style = typography.caption.copy(
                                fontSize = 14.sp,
                                color = ChildColors.Secondary
                            )
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        blockedWebsites.forEachIndexed { index, domain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Globe,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(Modifier.width(14.dp))

                                Text(
                                    text = domain,
                                    style = typography.headline.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(Modifier.width(8.dp))

                                Icon(
                                    imageVector = FocalIcons.Close,
                                    contentDescription = "Remove",
                                    tint = Color(0xFF6B6B6B),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { blockedWebsites.remove(domain) }
                                )
                            }

                            if (index < blockedWebsites.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 52.dp)
                                        .height(1.dp)
                                        .background(Color(0xFF1F1F1F))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // PrimaryButton "Save websites" pinned bottom
            PrimaryButton(
                text = "Save websites",
                onClick = {
                    onSaveWebsites(blockedWebsites.toList())
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            )
        }
    }
}

// =============================================================================
// REPLACE POLICY BOTTOM SHEET (When deleting a policy currently in use)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacePolicySheet(
    policyToDelete: FocusPolicy,
    onDismiss: () -> Unit,
    onReplacedAndDeleted: () -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val otherPolicies = remember(repository.policies, policyToDelete) {
        repository.policies.filter { it.id != policyToDelete.id }
    }

    var selectedReplacementId by remember {
        mutableStateOf(otherPolicies.firstOrNull()?.id ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF141414),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF333333))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Replace '${policyToDelete.name}' with:",
                style = typography.headline.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "This policy is currently used by your children or automated schedules. Choose another policy to assign to them.",
                style = typography.caption.copy(
                    fontSize = 13.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(20.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 20.dp,
                padding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    otherPolicies.forEachIndexed { index, other ->
                        val isSelected = (other.id == selectedReplacementId)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReplacementId = other.id }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = other.name,
                                    style = typography.headline.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${other.blockedApps.size} apps · ${other.blockedWebsites.size} websites",
                                    style = typography.caption.copy(
                                        fontSize = 12.sp,
                                        color = ChildColors.Secondary
                                    )
                                )
                            }

                            // Radio selection indicator
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 6.dp else 1.5.dp,
                                        color = if (isSelected) Color.White else Color(0xFF444444),
                                        shape = CircleShape
                                    )
                            )
                        }

                        if (index < otherPolicies.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF1F1F1F))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = "Replace and delete",
                onClick = {
                    if (selectedReplacementId.isNotBlank()) {
                        repository.deletePolicyWithReplacement(policyToDelete.id, selectedReplacementId)
                        PlatformPermissionsHelper.showToast("Reassigned and deleted '${policyToDelete.name}'")
                        onReplacedAndDeleted()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
        }
    }
}

// =============================================================================
// ROOT POLICIES FLOW (Manages full-screen navigation without bottom nav)
// =============================================================================

sealed class PolicyFlowState {
    object List : PolicyFlowState()
    data class Editor(val policy: FocusPolicy?, val isNew: Boolean) : PolicyFlowState()
    data class AppPicker(val policy: FocusPolicy?, val isNew: Boolean) : PolicyFlowState()
    data class Websites(val policy: FocusPolicy?, val isNew: Boolean) : PolicyFlowState()
}

@Composable
fun ParentPoliciesRootFlow(
    onEditingStateChanged: (Boolean) -> Unit = {},
    triggerNewPolicy: Int = 0,
    backTrigger: Int = 0,
    repository: FocalRepository = FocalRepository.instance
) {
    var flowState by remember { mutableStateOf<PolicyFlowState>(PolicyFlowState.List) }
    var activePolicy by remember { mutableStateOf<FocusPolicy?>(null) }
    var pendingApps by remember { mutableStateOf<List<String>?>(null) }
    var pendingWebsites by remember { mutableStateOf<List<String>?>(null) }

    // External trigger to open New Policy editor
    var lastTrigger by remember { mutableStateOf(triggerNewPolicy) }
    LaunchedEffect(triggerNewPolicy) {
        if (triggerNewPolicy > 0 && triggerNewPolicy != lastTrigger) {
            lastTrigger = triggerNewPolicy
            activePolicy = null
            pendingApps = null
            pendingWebsites = null
            flowState = PolicyFlowState.Editor(null, isNew = true)
        }
    }

    // External back trigger to step backwards in the policy sub-navigation
    var lastBackTrigger by remember { mutableStateOf(backTrigger) }
    LaunchedEffect(backTrigger) {
        if (backTrigger > 0 && backTrigger != lastBackTrigger) {
            lastBackTrigger = backTrigger
            when (val s = flowState) {
                is PolicyFlowState.AppPicker -> {
                    flowState = PolicyFlowState.Editor(activePolicy, isNew = s.isNew)
                }
                is PolicyFlowState.Websites -> {
                    flowState = PolicyFlowState.Editor(activePolicy, isNew = s.isNew)
                }
                is PolicyFlowState.Editor -> {
                    flowState = PolicyFlowState.List
                }
                is PolicyFlowState.List -> {
                    // Already at root list
                }
            }
        }
    }

    // Notify whether bottom nav should be hidden
    LaunchedEffect(flowState) {
        onEditingStateChanged(flowState !is PolicyFlowState.List)
    }

    when (val state = flowState) {
        is PolicyFlowState.List -> {
            ParentPoliciesScreen(
                onOpenNewPolicy = {
                    activePolicy = null
                    pendingApps = null
                    pendingWebsites = null
                    flowState = PolicyFlowState.Editor(null, isNew = true)
                },
                onOpenEditPolicy = { policy ->
                    activePolicy = policy
                    pendingApps = policy.blockedApps
                    pendingWebsites = policy.blockedWebsites
                    flowState = PolicyFlowState.Editor(policy, isNew = false)
                },
                repository = repository
            )
        }

        is PolicyFlowState.Editor -> {
            PolicyEditorScreen(
                policy = activePolicy,
                currentSelectedApps = pendingApps,
                currentSelectedWebsites = pendingWebsites,
                onBack = {
                    flowState = PolicyFlowState.List
                    activePolicy = null
                    pendingApps = null
                    pendingWebsites = null
                },
                onSaveSuccess = {
                    flowState = PolicyFlowState.List
                    activePolicy = null
                    pendingApps = null
                    pendingWebsites = null
                },
                onOpenAppPicker = { apps ->
                    pendingApps = apps
                    flowState = PolicyFlowState.AppPicker(activePolicy, state.isNew)
                },
                onOpenWebsites = { sites ->
                    pendingWebsites = sites
                    flowState = PolicyFlowState.Websites(activePolicy, state.isNew)
                },
                repository = repository
            )
        }

        is PolicyFlowState.AppPicker -> {
            AppPickerScreen(
                initialSelectedApps = pendingApps ?: activePolicy?.blockedApps ?: emptyList(),
                onBack = {
                    flowState = PolicyFlowState.Editor(activePolicy, state.isNew)
                },
                onSaveApps = { updatedApps ->
                    pendingApps = updatedApps
                    flowState = PolicyFlowState.Editor(activePolicy, state.isNew)
                },
                repository = repository
            )
        }

        is PolicyFlowState.Websites -> {
            WebsitesScreen(
                initialSelectedWebsites = pendingWebsites ?: activePolicy?.blockedWebsites ?: emptyList(),
                onBack = {
                    flowState = PolicyFlowState.Editor(activePolicy, state.isNew)
                },
                onSaveWebsites = { updatedWebsites ->
                    pendingWebsites = updatedWebsites
                    flowState = PolicyFlowState.Editor(activePolicy, state.isNew)
                },
                repository = repository
            )
        }
    }
}
