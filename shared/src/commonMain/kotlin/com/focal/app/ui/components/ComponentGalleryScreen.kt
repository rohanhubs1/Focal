package com.focal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.theme.FocalColors
import com.focal.app.theme.FocalSpacing

@Composable
fun ComponentGalleryScreen(
    onBack: () -> Unit = {}
) {
    var textInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var toggleState by remember { mutableStateOf(true) }
    var selectedDays by remember { mutableStateOf(setOf("Mon", "Wed", "Fri")) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var navIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        FocalTopBar(
            title = "Component Gallery (Debug)",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(FocalSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. BUTTONS
            Text("1. BUTTONS", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            FocalButton(text = "Primary Button", onClick = {})
            FocalButton(text = "Secondary Button", onClick = {}, variant = FocalButtonVariant.SECONDARY)
            FocalButton(text = "Danger Button", onClick = {}, variant = FocalButtonVariant.DANGER)
            FocalButton(text = "Loading Button", onClick = {}, isLoading = true)

            // 2. CARDS & STAT CARDS
            Text("2. CARDS & STAT CARDS", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            FocalCard {
                Text("Standard FocalCard Surface", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                Text("Background #0E0E0E, Border 1dp #1F1F1F, Radius 16dp", style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(label = "FOCUS HOURS", value = "5h 12m", deltaCaption = "+42m vs avg", modifier = Modifier.weight(1f))
                StatCard(label = "FOCUS SCORE", value = "84", deltaCaption = "Optimal flow", modifier = Modifier.weight(1f))
            }

            // 3. LIST ROWS
            Text("3. LIST ROWS", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            FocalListRow(
                title = "Camera & Sensor Protection",
                subtitle = "Monitored by Focal Engine",
                icon = FocalIcons.Shield,
                onClick = {}
            )
            FocalListRow(
                title = "Daily Screen Time Report",
                subtitle = "Nightly email summary",
                icon = FocalIcons.Bell,
                trailingToggleChecked = toggleState,
                onToggleChanged = { toggleState = it }
            )
            FocalListRow(
                title = "Paired FOCAL Card",
                subtitle = "Hardware NFC Token",
                icon = FocalIcons.Nfc,
                trailingText = "FC0001"
            )

            // 4. TEXT FIELDS
            Text("4. TEXT FIELDS", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            FocalTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = "Child Name",
                icon = FocalIcons.Users
            )
            FocalTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = "Parent Password",
                isPassword = true
            )

            // 5. TOGGLES & STATUS DOTS & AVATAR
            Text("5. TOGGLE, DOTS & AVATARS", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FocalToggle(checked = toggleState, onCheckedChange = { toggleState = it })
                StatusDot(isActive = true)
                Text("Active", style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                StatusDot(isActive = false)
                Text("Idle", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary))
                InitialsAvatar(name = "Rahul Sharma")
            }

            // 6. CHIP SELECTOR
            Text("6. CHIP SELECTOR (DAYS OF WEEK)", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            ChipSelector(
                options = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                selectedOptions = selectedDays,
                onOptionToggled = { day ->
                    selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                }
            )

            // 7. SEGMENTED TABS
            Text("7. SEGMENTED TABS", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            SegmentedTabs(
                options = listOf("Week", "Month"),
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // 8. BAR CHART (TWO SERIES)
            Text("8. BAR CHART (TWO SERIES)", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            BarChart(
                items = listOf(
                    BarChartItem("Mon", 4.5f, 6.2f),
                    BarChartItem("Tue", 5.0f, 4.8f),
                    BarChartItem("Wed", 6.2f, 3.5f),
                    BarChartItem("Thu", 3.8f, 7.0f),
                    BarChartItem("Fri", 5.5f, 5.0f),
                    BarChartItem("Sat", 2.0f, 8.0f),
                    BarChartItem("Sun", 4.0f, 6.0f)
                )
            )

            // 9. BANNERS
            Text("9. BANNERS", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            Banner(
                title = "Focus Schedule Active",
                message = "Study Session rule set is currently active."
            )
            Banner(
                title = "Protection Disabled Alert",
                message = "Accessibility permission has been revoked on Rahul's Galaxy Tab S9.",
                isProtectionOff = true,
                onActionClick = {}
            )

            // 10. LOADING SKELETON
            Text("10. LOADING SKELETON", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            LoadingSkeleton(height = 70.dp)

            // 11. STATES (EMPTY, SUCCESS, ERROR)
            Text("11. STATES", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            EmptyState(title = "No Focus Policies", message = "Create your first policy to restrict distracting apps.")
            SuccessState(title = "Device Paired", message = "Child device successfully linked.", onActionClick = {})
            ErrorState(title = "Connection Error", description = "Could not sync focus rules.", onRetry = {})

            // 12. QR CODE VIEW & NFC PULSE
            Text("12. QR CODE & NFC ANIMATION", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QrCodeView(qrContent = "PAIRING:839204", size = 140.dp)
                NfcPulseAnimation(detected = true)
            }

            // 13. BOTTOM NAV PREVIEW
            Text("13. BOTTOM NAVIGATION", style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            FocalBottomNav(
                items = listOf(
                    FocalNavItem("Home", FocalIcons.Home),
                    FocalNavItem("Children", FocalIcons.Users),
                    FocalNavItem("Policies", FocalIcons.Shield),
                    FocalNavItem("Profile", FocalIcons.Smartphone)
                ),
                selectedIndex = navIndex,
                onSelectTab = { navIndex = it }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
