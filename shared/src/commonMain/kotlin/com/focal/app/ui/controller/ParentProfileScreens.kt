package com.focal.app.ui.controller

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.focal.app.config.FocalConfig
import com.focal.app.config.FocalPricingConfig
import com.focal.app.data.DemoSubscriptionRepository
import com.focal.app.data.FocalRepository
import com.focal.app.model.CardRegistrationInspection
import com.focal.app.model.CardStatus
import com.focal.app.model.ChildProfile
import com.focal.app.model.FocusCard
import com.focal.app.model.currentTimeMillis
import com.focal.app.ui.child.DetailScaffold
import com.focal.app.ui.child.SectionOverline
import com.focal.app.ui.child.components.*
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.components.PlatformPermissionsHelper
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.ChildRadius
import com.focal.app.ui.theme.LocalChildTypography
import focal.shared.generated.resources.Res
import focal.shared.generated.resources.focal_logo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Navigation destination enum for Parent Profile sub-pages.
 */
enum class ParentProfileSubScreen {
    FOCAL_CARD,
    PLAN,
    CHILDREN_LIST,
    PARENT_PIN,
    INVITE_PARENT,
    NOTIFICATIONS,
    ACCOUNT_SECURITY,
    HELP,
    CONTACT_SUPPORT,
    PRIVACY_POLICY,
    TERMS_OF_SERVICE,
    DELETE_ACCOUNT
}

/**
 * Root Flow Coordinator for the Profile Tab and all its detail pages.
 * Hides bottom nav on all sub-screens and sheets.
 */
@Composable
fun ParentProfileRootFlow(
    onLogout: () -> Unit,
    onSelectChild: (ChildProfile) -> Unit,
    onAddChild: () -> Unit,
    onGoToFocusTab: () -> Unit,
    onSubScreenChanged: (Boolean) -> Unit,
    backTrigger: Int = 0,
    repository: FocalRepository = FocalRepository.instance,
    modifier: Modifier = Modifier
) {
    var currentSubScreen by remember { mutableStateOf<ParentProfileSubScreen?>(null) }

    LaunchedEffect(currentSubScreen) {
        onSubScreenChanged(currentSubScreen != null)
    }

    LaunchedEffect(backTrigger) {
        if (backTrigger > 0 && currentSubScreen != null) {
            currentSubScreen = null
        }
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
        modifier = modifier
    ) { subScreen ->
        when (subScreen) {
            null -> ParentProfileTabScreen(
                onNavigateSubScreen = { currentSubScreen = it },
                onLogout = onLogout,
                repository = repository
            )
            ParentProfileSubScreen.FOCAL_CARD -> ParentFocalCardScreen(
                onBack = { currentSubScreen = null },
                onOpenPlan = { currentSubScreen = ParentProfileSubScreen.PLAN },
                repository = repository
            )
            ParentProfileSubScreen.PLAN -> ParentPlanScreen(
                onBack = { currentSubScreen = null },
                onGoToFocusTab = onGoToFocusTab,
                repository = repository
            )
            ParentProfileSubScreen.CHILDREN_LIST -> ParentChildrenListScreen(
                onBack = { currentSubScreen = null },
                onSelectChild = onSelectChild,
                onAddChild = onAddChild,
                repository = repository
            )
            ParentProfileSubScreen.PARENT_PIN -> ParentPinScreen(
                onBack = { currentSubScreen = null },
                repository = repository
            )
            ParentProfileSubScreen.INVITE_PARENT -> InviteParentScreen(
                onBack = { currentSubScreen = null },
                repository = repository
            )
            ParentProfileSubScreen.NOTIFICATIONS -> ParentNotificationsScreen(
                onBack = { currentSubScreen = null }
            )
            ParentProfileSubScreen.ACCOUNT_SECURITY -> AccountSecurityScreen(
                onBack = { currentSubScreen = null },
                onSignOutAll = onLogout,
                repository = repository
            )
            ParentProfileSubScreen.HELP -> ParentHelpScreen(
                onBack = { currentSubScreen = null },
                onOpenContactSupport = { currentSubScreen = ParentProfileSubScreen.CONTACT_SUPPORT }
            )
            ParentProfileSubScreen.CONTACT_SUPPORT -> ContactSupportScreen(
                onBack = { currentSubScreen = null }
            )
            ParentProfileSubScreen.PRIVACY_POLICY -> LegalDocumentScreen(
                title = "Privacy policy",
                url = FocalConfig.PRIVACY_POLICY_URL,
                onBack = { currentSubScreen = null }
            )
            ParentProfileSubScreen.TERMS_OF_SERVICE -> LegalDocumentScreen(
                title = "Terms of service",
                url = FocalConfig.TERMS_URL,
                onBack = { currentSubScreen = null }
            )
            ParentProfileSubScreen.DELETE_ACCOUNT -> DeleteAccountScreen(
                onBack = { currentSubScreen = null },
                onAccountDeleted = onLogout,
                repository = repository
            )
        }
    }
}

// =========================================================================
// SCREEN 1 — PROFILE TAB (PF1_profile.png)
// =========================================================================
@Composable
fun ParentProfileTabScreen(
    onNavigateSubScreen: (ParentProfileSubScreen) -> Unit,
    onLogout: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val user = repository.controllerUser
    val subRepo = DemoSubscriptionRepository.instance
    val subDetails = subRepo.getSubscriptionDetails(user)
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val linkedChildrenNames = remember(repository.childrenList.size) {
        if (repository.childrenList.isEmpty()) "No children linked"
        else repository.childrenList.joinToString(", ") { it.name }
    }

    val cardCount = repository.nfcCards.size
    val cardSubtitle = if (cardCount > 0) {
        "$cardCount family card · works on every child's phone"
    } else {
        "No card yet"
    }

    val pinSubtitle = if (repository.parentPin.isNotBlank()) {
        "Set · unlocks a child's phone for 10 min"
    } else {
        "Not set"
    }

    val billingStatusLine = remember(subDetails) {
        when {
            subDetails.isExpired -> "Plan ended · renew to keep protection"
            subDetails.isTrial -> "Free trial · ${subDetails.trialDaysRemaining} days left · then ${FocalPricingConfig.PLAN_PRICE_PER_MONTH}"
            else -> "Renews ${subDetails.renewalDate} · ${FocalPricingConfig.PLAN_PRICE_PER_MONTH}"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ChildColors.Background)
    ) {
        // Ambient background glow arcs
        GlowArcs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 110.dp) // Clearance for floating ParentBottomNav
        ) {
            // Header Title
            Text(
                text = "Profile",
                style = typography.display.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Account GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 18.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChildAvatar(
                        name = user.name.ifBlank { "Priya Sharma" },
                        size = 60.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name.ifBlank { "Priya Sharma" },
                            style = typography.title.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user.email.ifBlank { "priya@gmail.com" },
                            style = typography.caption.copy(
                                fontSize = 13.sp,
                                color = ChildColors.Secondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    StatusPill(
                        text = "Edit",
                        type = StatusPillType.UPCOMING,
                        modifier = Modifier.clickable { onNavigateSubScreen(ParentProfileSubScreen.ACCOUNT_SECURITY) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Plan GlassCard (with faint ambient glow top-right)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ChildRadius.Card))
                    .clickable { onNavigateSubScreen(ParentProfileSubScreen.PLAN) }
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
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
                                .background(ChildColors.IconCircleBg)
                                .border(1.dp, ChildColors.BorderStrong, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.Crown,
                                contentDescription = "Plan",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FOCAL Family",
                                style = typography.title.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = billingStatusLine,
                                style = typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = ChildColors.Secondary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = FocalIcons.ChevronRight,
                            contentDescription = null,
                            tint = ChildColors.Tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // SECTION: FAMILY
            SectionOverline("FAMILY")
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                ChildListRow(
                    title = "Children",
                    subtitle = linkedChildrenNames,
                    icon = { Icon(FocalIcons.Users, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.CHILDREN_LIST) }
                )
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp, modifier = Modifier.padding(start = 74.dp))
                ChildListRow(
                    title = "Parent PIN",
                    subtitle = pinSubtitle,
                    icon = { Icon(FocalIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.PARENT_PIN) }
                )
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp, modifier = Modifier.padding(start = 74.dp))
                ChildListRow(
                    title = "Invite another parent",
                    subtitle = "Share control of the family",
                    icon = { Icon(FocalIcons.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.INVITE_PARENT) }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // SECTION: CARDS
            SectionOverline("CARDS")
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                ChildListRow(
                    title = "FOCAL cards",
                    subtitle = cardSubtitle,
                    icon = { Icon(FocalIcons.Card, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.FOCAL_CARD) }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // SECTION: SETTINGS
            SectionOverline("SETTINGS")
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                ChildListRow(
                    title = "Notifications",
                    subtitle = "Requests, alerts, focus updates",
                    icon = { Icon(FocalIcons.Bell, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.NOTIFICATIONS) }
                )
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp, modifier = Modifier.padding(start = 74.dp))
                ChildListRow(
                    title = "Account & security",
                    subtitle = "Name, email, password",
                    icon = { Icon(FocalIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.ACCOUNT_SECURITY) }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // SECTION: SUPPORT
            SectionOverline("SUPPORT")
            Spacer(modifier = Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                ChildListRow(
                    title = "Help & FAQ",
                    subtitle = "Setup guides, troubleshooting",
                    icon = { Icon(FocalIcons.HelpCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.HELP) }
                )
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp, modifier = Modifier.padding(start = 74.dp))
                ChildListRow(
                    title = "Contact support",
                    subtitle = "Email or WhatsApp",
                    icon = { Icon(FocalIcons.Mail, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.CONTACT_SUPPORT) }
                )
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp, modifier = Modifier.padding(start = 74.dp))
                ChildListRow(
                    title = "Privacy policy",
                    icon = { Icon(FocalIcons.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.PRIVACY_POLICY) }
                )
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp, modifier = Modifier.padding(start = 74.dp))
                ChildListRow(
                    title = "Terms of service",
                    icon = { Icon(FocalIcons.Document, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { onNavigateSubScreen(ParentProfileSubScreen.TERMS_OF_SERVICE) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sign out button
            SecondaryButton(
                text = "Sign out",
                onClick = { showSignOutConfirm = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Delete account red text button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateSubScreen(ParentProfileSubScreen.DELETE_ACCOUNT) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Delete account",
                    style = typography.body.copy(
                        color = Color(0xFFFF453A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Version footer
            Text(
                text = "Version 1.2.0 (108)",
                style = typography.caption.copy(
                    color = ChildColors.Tertiary,
                    fontSize = 12.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // Sign out confirmation dialog
    if (showSignOutConfirm) {
        Dialog(onDismissRequest = { showSignOutConfirm = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                padding = 24.dp
            ) {
                Text(
                    text = "Sign out?",
                    style = typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your children's phones stay protected.",
                    style = typography.body.copy(fontSize = 14.sp, color = ChildColors.Secondary)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = { showSignOutConfirm = false },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Sign out",
                        onClick = {
                            showSignOutConfirm = false
                            onLogout()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 2 — FOCAL CARD (PF2_cards.png)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentFocalCardScreen(
    onBack: () -> Unit,
    onOpenPlan: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    var showAddCardSheet by remember { mutableStateOf(false) }
    var selectedCardForMenu by remember { mutableStateOf<FocusCard?>(null) }
    var cardToRename by remember { mutableStateOf<FocusCard?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var cardToDisable by remember { mutableStateOf<FocusCard?>(null) }
    var cardToRemove by remember { mutableStateOf<FocusCard?>(null) }

    val allChildrenNames = remember(repository.childrenList.size) {
        if (repository.childrenList.isEmpty()) "No children linked"
        else repository.childrenList.joinToString(", ") { it.name }
    }

    val cards = repository.nfcCards

    DetailScaffold(
        title = "FOCAL card",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "One card for the whole family. Tap it on any child's phone to start or end focus there.",
                style = typography.body.copy(
                    color = ChildColors.Secondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (cards.isEmpty()) {
                // Empty state
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 24.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        FocalCardHero(
                            cardWidth = 180.dp,
                            cardHeight = 112.dp,
                            logoSize = 56.dp,
                            tiltAngle = 0f,
                            pulsingRings = false
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No card yet",
                            style = typography.title.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap a card on any child's phone to start or end focus.",
                            style = typography.caption.copy(color = ChildColors.Secondary, textAlign = TextAlign.Center)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PrimaryButton(
                                text = "+ Add a card",
                                onClick = { showAddCardSheet = true },
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryButton(
                                text = "Get a card",
                                onClick = onOpenPlan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // Cards list
                cards.forEach { card ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        padding = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini card thumbnail (64x42dp)
                            Box(
                                modifier = Modifier
                                    .size(width = 64.dp, height = 42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF1E1E1E), Color(0xFF0D0D0D))
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.focal_logo),
                                    contentDescription = "FOCAL Card",
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${card.cardId} · ${card.label.ifBlank { "Family card" }}",
                                    style = typography.title.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                val lastUsedText = if (!card.isEnabled || card.status == CardStatus.DISABLED) {
                                    "Disabled"
                                } else if (card.lastUsed.isNotBlank()) {
                                    "Last used ${card.lastUsed.lowercase()} · ${card.lastUsedChildName ?: "Aarav"}"
                                } else {
                                    "Not used yet"
                                }
                                Text(
                                    text = lastUsedText,
                                    style = typography.caption.copy(
                                        fontSize = 12.sp,
                                        color = if (!card.isEnabled || card.status == CardStatus.DISABLED) Color(0xFFFF453A) else ChildColors.Secondary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 44dp round More button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { selectedCardForMenu = card },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FocalIcons.MoreHorizontal,
                                    contentDescription = "Card options",
                                    tint = ChildColors.Secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Works on InfoCard
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = 18.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Works on",
                            style = typography.body.copy(fontSize = 14.sp, color = ChildColors.Secondary)
                        )
                        Text(
                            text = allChildrenNames,
                            style = typography.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card lost? Alert GlassCard
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
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
                                .background(ChildColors.IconCircleBg)
                                .border(1.dp, ChildColors.BorderStrong, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FocalIcons.InfoCircleAlert,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Card lost?",
                                style = typography.title.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Tap ··· → Disable. It stops working on every phone right away. Then add a new card.",
                                style = typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = ChildColors.Secondary,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom action button: + Add a spare card
                PrimaryButton(
                    text = "+ Add a spare card",
                    onClick = { showAddCardSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Card Action Menu Sheet
    selectedCardForMenu?.let { card ->
        ModalBottomSheet(
            onDismissRequest = { selectedCardForMenu = null },
            containerColor = Color(0xFF0F0F0F),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF333333))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "${card.cardId} · ${card.label}",
                    style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Rename row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedCardForMenu = null
                            renameInput = card.label
                            cardToRename = card
                        }
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rename",
                        style = typography.body.copy(fontSize = 16.sp, color = Color.White)
                    )
                }

                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)

                // Disable / Enable row
                if (card.isEnabled && card.status != CardStatus.DISABLED) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedCardForMenu = null
                                cardToDisable = card
                            }
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Disable",
                            style = typography.body.copy(fontSize = 16.sp, color = Color(0xFFFF453A), fontWeight = FontWeight.SemiBold)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedCardForMenu = null
                                repository.enableCard(card.cardId)
                                PlatformPermissionsHelper.showToast("${card.cardId} enabled")
                            }
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable",
                            style = typography.body.copy(fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)

                // Remove row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedCardForMenu = null
                            cardToRemove = card
                        }
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remove",
                        style = typography.body.copy(fontSize = 16.sp, color = Color(0xFFFF453A), fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }

    // Rename dialog
    cardToRename?.let { card ->
        Dialog(onDismissRequest = { cardToRename = null }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(text = "Rename ${card.cardId}", style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(14.dp))
                BasicTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, ChildColors.Border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { cardToRename = null }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Save",
                        onClick = {
                            if (renameInput.isNotBlank()) {
                                repository.renameCard(card.cardId, renameInput.trim())
                            }
                            cardToRename = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Disable confirmation dialog
    cardToDisable?.let { card ->
        Dialog(onDismissRequest = { cardToDisable = null }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(text = "Disable ${card.cardId}?", style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "It stops working on every phone right away.",
                    style = typography.body.copy(fontSize = 14.sp, color = ChildColors.Secondary)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { cardToDisable = null }, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(ChildRadius.Pill))
                            .background(Color(0xFFFF453A))
                            .clickable {
                                repository.disableCard(card.cardId)
                                cardToDisable = null
                                PlatformPermissionsHelper.showToast("${card.cardId} disabled")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Disable", style = typography.body.copy(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }

    // Remove confirmation dialog
    cardToRemove?.let { card ->
        Dialog(onDismissRequest = { cardToRemove = null }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(text = "Remove ${card.cardId}?", style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Remove this card from the family? It will no longer work on any child's phone.",
                    style = typography.body.copy(fontSize = 14.sp, color = ChildColors.Secondary)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { cardToRemove = null }, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(ChildRadius.Pill))
                            .background(Color(0xFFFF453A))
                            .clickable {
                                repository.removeCard(card.cardId)
                                cardToRemove = null
                                PlatformPermissionsHelper.showToast("${card.cardId} removed")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Remove", style = typography.body.copy(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }

    // Add card sheet
    if (showAddCardSheet) {
        AddCardSheet(
            onDismiss = { showAddCardSheet = false },
            onCardAdded = { newCard ->
                showAddCardSheet = false
                PlatformPermissionsHelper.showToast("${newCard.cardId} added to family")
            },
            repository = repository
        )
    }
}

// =========================================================================
// SCREEN 3 — ADD CARD SHEET (PF3_add_card.png)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardSheet(
    onDismiss: () -> Unit,
    onCardAdded: (FocusCard) -> Unit,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val coroutineScope = rememberCoroutineScope()
    var resultStatus by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val allChildrenFormatted = remember(repository.childrenList.size) {
        when (repository.childrenList.size) {
            0 -> "all children"
            1 -> repository.childrenList.first().name
            2 -> "${repository.childrenList[0].name} and ${repository.childrenList[1].name}"
            else -> repository.childrenList.dropLast(1).joinToString(", ") { it.name } + " and " + repository.childrenList.last().name
        }
    }

    val processTagScan: (String) -> Unit = { rawPayload ->
        val inspection = repository.inspectCardForRegistration(rawPayload, repository.controllerUser.id)
        when (inspection) {
            is CardRegistrationInspection.UnownedInStock -> {
                val newCard = repository.registerCardToParent(
                    inspection.card.token,
                    repository.controllerUser.id,
                    repository.controllerUser.name
                )
                isSuccess = true
                resultStatus = "✓ Card added"
                coroutineScope.launch {
                    delay(700)
                    onCardAdded(newCard)
                }
            }
            is CardRegistrationInspection.BlankTag -> {
                val newCard = repository.writeAndRegisterBlankCard(
                    repository.controllerUser.id,
                    repository.controllerUser.name
                )
                isSuccess = true
                resultStatus = "✓ Card added"
                coroutineScope.launch {
                    delay(700)
                    onCardAdded(newCard)
                }
            }
            is CardRegistrationInspection.AlreadyOwnedByMe -> {
                resultStatus = "This card is already added"
            }
            is CardRegistrationInspection.OwnedByOther -> {
                resultStatus = "This card belongs to another family"
            }
            is CardRegistrationInspection.Revoked -> {
                resultStatus = "This card has been deactivated"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B0B0B),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 12.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF333333))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Overline: ADD CARD
            Text(
                text = "ADD CARD",
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChildColors.Tertiary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Title: Hold the new card near this phone
            Text(
                text = "Hold the new card near this phone",
                style = typography.display.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Center Illustration: tilted FocalCardHero with 2 pulsing concentric rings
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Demo tap trigger
                        val nextToken = "NEW_FAMILY_CARD_${currentTimeMillis()}"
                        processTagScan(nextToken)
                    },
                contentAlignment = Alignment.Center
            ) {
                FocalCardHero(
                    cardWidth = 210.dp,
                    cardHeight = 132.dp,
                    logoSize = 64.dp,
                    tiltAngle = -8f,
                    pulsingRings = !isSuccess,
                    ringCount = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reader StatusPill
            val pillText = resultStatus ?: "● Listening · FOCAL reader connected"
            StatusPill(
                text = pillText,
                type = if (isSuccess) StatusPillType.ACTIVE else StatusPillType.UPCOMING
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Info GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ChildColors.IconCircleBg)
                            .border(1.dp, ChildColors.BorderStrong, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Users,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "This card will work on all your children's phones: $allChildrenFormatted.",
                        style = typography.caption.copy(
                            fontSize = 13.sp,
                            color = ChildColors.Secondary,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // SecondaryButton: Cancel
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =========================================================================
// SCREEN 4 — YOUR PLAN (PF4_plan.png)
// =========================================================================
enum class PlanFocusModeOption {
    NFC_CARD,
    QR_FOCUS
}

@Composable
fun ParentPlanScreen(
    onBack: () -> Unit,
    onGoToFocusTab: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val subRepo = DemoSubscriptionRepository.instance
    val user = repository.controllerUser
    val subDetails = subRepo.getSubscriptionDetails(user)
    var selectedOption by remember { mutableStateOf(PlanFocusModeOption.NFC_CARD) }

    DetailScaffold(
        title = "Your plan",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = FocalPricingConfig.PLAN_TRIAL_SUMMARY,
                style = typography.body.copy(
                    color = ChildColors.Secondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Status GlassCard
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(ChildColors.IconCircleBg)
                            .border(1.dp, ChildColors.BorderStrong, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.Crown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (subDetails.isTrial) "Free trial" else "FOCAL Family",
                            style = typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (subDetails.isTrial) "${subDetails.trialDaysRemaining} days left" else "Renews ${subDetails.renewalDate}",
                            style = typography.caption.copy(fontSize = 13.sp, color = ChildColors.Secondary)
                        )
                    }
                    StatusPill(
                        text = if (subDetails.isExpired) "Ended" else "Active",
                        type = if (subDetails.isExpired) StatusPillType.OFF else StatusPillType.ACTIVE
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionOverline("HOW YOUR CHILD STARTS FOCUS")
            Spacer(modifier = Modifier.height(12.dp))

            // Option 1: NFC focus card
            PlanOptionCard(
                isSelected = selectedOption == PlanFocusModeOption.NFC_CARD,
                onClick = { selectedOption = PlanFocusModeOption.NFC_CARD }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlanRadioButton(isSelected = selectedOption == PlanFocusModeOption.NFC_CARD)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "NFC focus card",
                                style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.padding(start = 32.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "PHYSICAL CARD",
                                    style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                                )
                            }
                        }
                    }

                    // Tilted mini card art on the right
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.focal_logo),
                            contentDescription = "Card illustration",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(start = 32.dp)
                ) {
                    Text(
                        text = FocalPricingConfig.NFC_CARD_PRICE_FORMATTED,
                        style = typography.display.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "one-time",
                        style = typography.body.copy(fontSize = 13.sp, color = ChildColors.Secondary),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.padding(start = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlanBulletRow("Tap to start and end focus")
                    PlanBulletRow("Works offline")
                    PlanBulletRow("Includes 3 months free app access")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option 2: QR focus
            PlanOptionCard(
                isSelected = selectedOption == PlanFocusModeOption.QR_FOCUS,
                onClick = { selectedOption = PlanFocusModeOption.QR_FOCUS }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlanRadioButton(isSelected = selectedOption == PlanFocusModeOption.QR_FOCUS)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "QR focus",
                                style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.padding(start = 32.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "NO HARDWARE",
                                    style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                                )
                            }
                        }
                    }

                    // White QR tile on the right
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FocalIcons.QrCode,
                            contentDescription = "QR illustration",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(start = 32.dp)
                ) {
                    Text(
                        text = "₹0",
                        style = typography.display.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "to start",
                        style = typography.body.copy(fontSize = 13.sp, color = ChildColors.Secondary),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.padding(start = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlanBulletRow("Scan a QR to start and end focus")
                    PlanBulletRow("Print it or show it on your phone")
                    PlanBulletRow("Includes 3 months free app access")
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // PrimaryButton changes with selection
            if (selectedOption == PlanFocusModeOption.NFC_CARD) {
                PrimaryButton(
                    text = "Get the NFC card",
                    onClick = { uriHandler.openUri(FocalConfig.CARD_ORDER_URL) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PrimaryButton(
                    text = "Use QR focus",
                    onClick = onGoToFocusTab,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer note
            Text(
                text = "App plan billed through Google Play",
                style = typography.caption.copy(
                    fontSize = 12.sp,
                    color = ChildColors.Tertiary,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // When trial/plan needs payment: Google Play Billing trigger
            if (subDetails.isExpired || subDetails.paymentFailed) {
                Spacer(modifier = Modifier.height(16.dp))
                SecondaryButton(
                    text = "Subscribe · ${FocalPricingConfig.PLAN_PRICE_PER_MONTH}",
                    onClick = {
                        coroutineScope.launch {
                            subRepo.launchGooglePlayBillingFlow()
                            PlatformPermissionsHelper.showToast("Connecting to Google Play Billing...")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (isSelected) Color.White else ChildColors.Border
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val shape = RoundedCornerShape(ChildRadius.Card)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(ChildColors.CardGradientStart, ChildColors.CardGradientEnd)
                )
            )
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun PlanRadioButton(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(
                1.5.dp,
                if (isSelected) Color.White else Color(0xFF4A4A4A),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun PlanBulletRow(text: String) {
    val typography = LocalChildTypography.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = FocalIcons.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = typography.caption.copy(
                fontSize = 13.sp,
                color = ChildColors.Secondary
            )
        )
    }
}

// =========================================================================
// TEXT-ONLY PAGE 1: ChildrenListScreen
// =========================================================================
@Composable
fun ParentChildrenListScreen(
    onBack: () -> Unit,
    onSelectChild: (ChildProfile) -> Unit,
    onAddChild: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current

    DetailScaffold(
        title = "Children",
        onBack = onBack,
        actions = {
            // White round 44dp + button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onAddChild),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.Add,
                    contentDescription = "Add child",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Manage your children's profiles, linked phones and focus schedules.",
                style = typography.body.copy(fontSize = 14.sp, color = ChildColors.Secondary)
            )

            Spacer(modifier = Modifier.height(20.dp))

            repository.childrenList.forEach { child ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectChild(child) },
                    padding = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChildAvatar(name = child.name, size = 52.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = child.name,
                                style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = child.displayGrade,
                                style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (child.isFocusing) Color.White else Color(0xFF6B6B6B))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val statusText = if (child.isFocusing) {
                                    "Focusing · ${child.remainingMinutes}m left"
                                } else {
                                    "Not focusing · ${child.todayFocusMinutes / 60}h ${child.todayFocusMinutes % 60}m today"
                                }
                                Text(
                                    text = statusText,
                                    style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = FocalIcons.ChevronRight,
                            contentDescription = null,
                            tint = ChildColors.Tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// TEXT-ONLY PAGE 2: ParentPinScreen
// =========================================================================
private enum class PinSetupStep {
    ENTER_CURRENT,
    ENTER_NEW,
    CONFIRM_NEW
}

@Composable
fun ParentPinScreen(
    onBack: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val haptic = LocalHapticFeedback.current
    val hasExistingPin = remember { repository.parentPin.isNotBlank() }

    var step by remember {
        mutableStateOf(if (hasExistingPin) PinSetupStep.ENTER_CURRENT else PinSetupStep.ENTER_NEW)
    }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    val enteredDigits = when (step) {
        PinSetupStep.ENTER_CURRENT -> currentPinInput
        PinSetupStep.ENTER_NEW -> newPinInput
        PinSetupStep.CONFIRM_NEW -> confirmPinInput
    }

    val headerTitle = when (step) {
        PinSetupStep.ENTER_CURRENT -> "Enter current PIN"
        PinSetupStep.ENTER_NEW -> "Set new Parent PIN"
        PinSetupStep.CONFIRM_NEW -> "Confirm new PIN"
    }

    val headerSubtitle = when (step) {
        PinSetupStep.ENTER_CURRENT -> "Enter your 4-digit PIN to verify your identity."
        PinSetupStep.ENTER_NEW -> "This PIN pauses protection for 10 minutes on any child's phone."
        PinSetupStep.CONFIRM_NEW -> "Re-enter the 4-digit PIN to confirm."
    }

    DetailScaffold(
        title = "Parent PIN",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Lock badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF171717))
                    .border(1.dp, Color(0xFF2A2A2A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FocalIcons.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = headerTitle,
                style = typography.title.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = headerSubtitle,
                style = typography.body.copy(fontSize = 14.sp, color = ChildColors.Secondary, textAlign = TextAlign.Center),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4 PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredDigits.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) Color.White else Color.Transparent)
                            .border(1.5.dp, if (isFilled) Color.White else Color(0xFF6B6B6B), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Error message display
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = typography.caption.copy(color = Color(0xFFFF453A), fontSize = 13.sp)
                )
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Forgot PIN button (only on ENTER_CURRENT)
            if (step == PinSetupStep.ENTER_CURRENT) {
                Text(
                    text = "Forgot PIN?",
                    style = typography.caption.copy(color = ChildColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    modifier = Modifier
                        .clickable { showForgotPasswordDialog = true }
                        .padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Keypad 3x4 layout
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "DEL")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f).height(60.dp))
                        } else if (key == "DEL") {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        errorMessage = null
                                        when (step) {
                                            PinSetupStep.ENTER_CURRENT -> if (currentPinInput.isNotEmpty()) currentPinInput = currentPinInput.dropLast(1)
                                            PinSetupStep.ENTER_NEW -> if (newPinInput.isNotEmpty()) newPinInput = newPinInput.dropLast(1)
                                            PinSetupStep.CONFIRM_NEW -> if (confirmPinInput.isNotEmpty()) confirmPinInput = confirmPinInput.dropLast(1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FocalIcons.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color(0xFF0E0E0E))
                                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(30.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        errorMessage = null
                                        when (step) {
                                            PinSetupStep.ENTER_CURRENT -> {
                                                if (currentPinInput.length < 4) {
                                                    val next = currentPinInput + key
                                                    currentPinInput = next
                                                    if (next.length == 4) {
                                                        if (next == repository.parentPin) {
                                                            step = PinSetupStep.ENTER_NEW
                                                        } else {
                                                            errorMessage = "Wrong PIN, try again"
                                                            currentPinInput = ""
                                                        }
                                                    }
                                                }
                                            }
                                            PinSetupStep.ENTER_NEW -> {
                                                if (newPinInput.length < 4) {
                                                    val next = newPinInput + key
                                                    newPinInput = next
                                                    if (next.length == 4) {
                                                        step = PinSetupStep.CONFIRM_NEW
                                                    }
                                                }
                                            }
                                            PinSetupStep.CONFIRM_NEW -> {
                                                if (confirmPinInput.length < 4) {
                                                    val next = confirmPinInput + key
                                                    confirmPinInput = next
                                                    if (next.length == 4) {
                                                        if (next == newPinInput) {
                                                            repository.updateParentPin(next)
                                                            PlatformPermissionsHelper.showToast("PIN updated")
                                                            onBack()
                                                        } else {
                                                            errorMessage = "PINs didn't match. Try again."
                                                            newPinInput = ""
                                                            confirmPinInput = ""
                                                            step = PinSetupStep.ENTER_NEW
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    style = typography.title.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Forgot PIN dialog (password verification)
    if (showForgotPasswordDialog) {
        Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(text = "Verify password", style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Enter your account password to reset the Parent PIN.", style = typography.body.copy(fontSize = 13.sp, color = ChildColors.Secondary))
                Spacer(modifier = Modifier.height(14.dp))
                BasicTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, ChildColors.Border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { showForgotPasswordDialog = false }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Continue",
                        onClick = {
                            showForgotPasswordDialog = false
                            currentPinInput = ""
                            newPinInput = ""
                            confirmPinInput = ""
                            step = PinSetupStep.ENTER_NEW
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// TEXT-ONLY PAGE 3: InviteParentScreen
// =========================================================================
@Composable
fun InviteParentScreen(
    onBack: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val clipboardManager = LocalClipboardManager.current
    val inviteCode = "focal.app/invite/family_priya_84920"
    var coParents by remember { mutableStateOf(listOf("Rohan Sharma · Co-parent")) }
    var coParentToRemove by remember { mutableStateOf<String?>(null) }

    DetailScaffold(
        title = "Invite another parent",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "They'll be able to see and control all your children.",
                style = typography.body.copy(fontSize = 14.sp, color = ChildColors.Secondary)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Invite code card
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
                Text(
                    text = "INVITE LINK",
                    style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChildColors.Tertiary, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = inviteCode,
                    style = typography.title.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = "Share invite",
                    onClick = {
                        clipboardManager.setText(AnnotatedString(inviteCode))
                        PlatformPermissionsHelper.showToast("Invite link copied to clipboard")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionOverline("CO-PARENTS")
            Spacer(modifier = Modifier.height(8.dp))

            if (coParents.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
                    Text(
                        text = "No other parents added yet.",
                        style = typography.body.copy(fontSize = 13.sp, color = ChildColors.Secondary)
                    )
                }
            } else {
                coParents.forEach { coParent ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ChildColors.IconCircleBg)
                                        .border(1.dp, ChildColors.BorderStrong, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "RS", style = typography.title.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = coParent,
                                    style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                )
                            }
                            Text(
                                text = "Remove",
                                style = typography.caption.copy(fontSize = 13.sp, color = Color(0xFFFF453A), fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .clickable { coParentToRemove = coParent }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    coParentToRemove?.let { coParent ->
        Dialog(onDismissRequest = { coParentToRemove = null }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(text = "Remove co-parent?", style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "They will lose access to all children and focus controls.", style = typography.body.copy(fontSize = 13.sp, color = ChildColors.Secondary))
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { coParentToRemove = null }, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(ChildRadius.Pill))
                            .background(Color(0xFFFF453A))
                            .clickable {
                                coParents = coParents - coParent
                                coParentToRemove = null
                                PlatformPermissionsHelper.showToast("Co-parent removed")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Remove", style = typography.body.copy(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

// =========================================================================
// TEXT-ONLY PAGE 4: NotificationsScreen
// =========================================================================
@Composable
fun ParentNotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    var focusStartedEnded by remember { mutableStateOf(true) }
    var dailySummary by remember { mutableStateOf(true) }
    var weeklyReport by remember { mutableStateOf(false) }

    DetailScaffold(
        title = "Notifications",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            SectionOverline("FOCUS NOTIFICATIONS")
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Focus started and ended", style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                        Text(text = "Alerts when children start or finish sessions", style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary))
                    }
                    FocalSwitch(checked = focusStartedEnded, onCheckedChange = { focusStartedEnded = it })
                }
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Daily summary at 8 PM", style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                        Text(text = "Total focus time and screen time", style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary))
                    }
                    FocalSwitch(checked = dailySummary, onCheckedChange = { dailySummary = it })
                }
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Weekly report on Sunday", style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                        Text(text = "Insights and progress across the week", style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary))
                    }
                    FocalSwitch(checked = weeklyReport, onCheckedChange = { weeklyReport = it })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionOverline("SECURITY & ALERTS")
            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChildColors.IconCircleBg)
                            .border(1.dp, ChildColors.BorderStrong, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(FocalIcons.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Always on", style = typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChildColors.Tertiary, letterSpacing = 1.sp))
                        Text(text = "Critical security alerts cannot be silenced", style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary))
                    }
                }
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)
                AlwaysOnAlertRow("Emergency exit requests")
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)
                AlwaysOnAlertRow("New phone requests")
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)
                AlwaysOnAlertRow("Protection turned off")
            }
        }
    }
}

@Composable
private fun AlwaysOnAlertRow(title: String) {
    val typography = LocalChildTypography.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = typography.body.copy(fontSize = 15.sp, color = Color.White))
        Icon(FocalIcons.Check, contentDescription = null, tint = Color(0xFFA3A3A3), modifier = Modifier.size(16.dp))
    }
}

// =========================================================================
// TEXT-ONLY PAGE 5: AccountSecurityScreen
// =========================================================================
@Composable
fun AccountSecurityScreen(
    onBack: () -> Unit,
    onSignOutAll: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(repository.controllerUser.name) }
    var editingEmail by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf(repository.controllerUser.email) }
    var editingPassword by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var showSignOutAllConfirm by remember { mutableStateOf(false) }

    DetailScaffold(
        title = "Account & security",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                // Name Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Name", style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary))
                        Text(text = repository.controllerUser.name.ifBlank { "Priya Sharma" }, style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                    }
                    StatusPill(text = "Edit", type = StatusPillType.UPCOMING, modifier = Modifier.clickable { editingName = true })
                }
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)
                // Email Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Email", style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary))
                        Text(text = repository.controllerUser.email.ifBlank { "priya@gmail.com" }, style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                    }
                    StatusPill(text = "Edit", type = StatusPillType.UPCOMING, modifier = Modifier.clickable { editingEmail = true })
                }
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)
                // Password Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Password", style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary))
                        Text(text = "••••••••", style = typography.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
                    }
                    StatusPill(text = "Change", type = StatusPillType.UPCOMING, modifier = Modifier.clickable { editingPassword = true })
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SecondaryButton(
                text = "Sign out of all devices",
                onClick = { showSignOutAllConfirm = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Edit Name Dialog
    if (editingName) {
        Dialog(onDismissRequest = { editingName = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(text = "Edit name", style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(14.dp))
                BasicTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, ChildColors.Border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { editingName = false }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Save",
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                repository.controllerUser = repository.controllerUser.copy(name = nameInput.trim())
                            }
                            editingName = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Edit Email Dialog
    if (editingEmail) {
        Dialog(onDismissRequest = { editingEmail = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(text = "Edit email", style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(14.dp))
                BasicTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, ChildColors.Border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { editingEmail = false }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Save",
                        onClick = {
                            if (emailInput.isNotBlank()) {
                                repository.updateParentEmail(emailInput.trim())
                            }
                            editingEmail = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Change Password Dialog
    if (editingPassword) {
        Dialog(onDismissRequest = { editingPassword = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(text = "Change password", style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(14.dp))
                BasicTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, ChildColors.Border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { editingPassword = false }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Save",
                        onClick = {
                            editingPassword = false
                            PlatformPermissionsHelper.showToast("Password updated")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Sign out all devices confirm
    if (showSignOutAllConfirm) {
        Dialog(onDismissRequest = { showSignOutAllConfirm = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(text = "Sign out everywhere?", style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "This will sign out all sessions on all phones and tablets.", style = typography.body.copy(fontSize = 13.sp, color = ChildColors.Secondary))
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { showSignOutAllConfirm = false }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "Sign out",
                        onClick = {
                            showSignOutAllConfirm = false
                            onSignOutAll()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// TEXT-ONLY PAGE 6: HelpScreen
// =========================================================================
@Composable
fun ParentHelpScreen(
    onBack: () -> Unit,
    onOpenContactSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current

    val faqs = listOf(
        "How does the family card work?" to "One physical FOCAL card works across every child's phone in your family. Tap it on any phone to start or end focus sessions immediately.",
        "How do I add a child?" to "Tap Add child in your Children list or on Home. Enter their name and grade, then show them the pairing QR or 6-digit code on their phone.",
        "What if my child turns off protection?" to "FOCAL immediately alerts your phone, logs the event in your Activity tab, and prompts your child to restore permissions before they can access blocked apps.",
        "How do I end focus early?" to "You can tap 'End focus now' from the child's page in your app, tap the family card on their phone, or approve their emergency exit request.",
        "How do I change what's blocked?" to "Open the Policies tab to create or edit focus policies. You can choose which apps and websites are blocked or allowed during focus sessions."
    )

    DetailScaffold(
        title = "Help & FAQ",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            faqs.forEach { (question, answer) ->
                var isExpanded by remember { mutableStateOf(false) }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { isExpanded = !isExpanded },
                    padding = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = question,
                            style = typography.title.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) FocalIcons.ChevronUp else FocalIcons.ChevronDown,
                            contentDescription = null,
                            tint = ChildColors.Secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = answer,
                            style = typography.body.copy(fontSize = 13.sp, color = ChildColors.Secondary, lineHeight = 19.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                text = "Contact support",
                onClick = onOpenContactSupport,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =========================================================================
// TEXT-ONLY PAGE 7: ContactSupportScreen
// =========================================================================
@Composable
fun ContactSupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val uriHandler = LocalUriHandler.current

    DetailScaffold(
        title = "Contact support",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "We're here to help you keep your family focused.",
                style = typography.body.copy(fontSize = 14.sp, color = ChildColors.Secondary)
            )

            Spacer(modifier = Modifier.height(20.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                ChildListRow(
                    title = "Email support",
                    subtitle = FocalConfig.SUPPORT_EMAIL,
                    icon = { Icon(FocalIcons.Mail, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { uriHandler.openUri("mailto:${FocalConfig.SUPPORT_EMAIL}") }
                )
                HorizontalDivider(color = ChildColors.ListDivider, thickness = 1.dp)
                ChildListRow(
                    title = "WhatsApp support",
                    subtitle = "Chat with our support team",
                    icon = { Icon(FocalIcons.Message, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                    onClick = { uriHandler.openUri(FocalConfig.SUPPORT_WHATSAPP) }
                )
            }
        }
    }
}

// =========================================================================
// TEXT-ONLY PAGE 8: LegalDocumentScreen (Privacy Policy & Terms)
// =========================================================================
@Composable
fun LegalDocumentScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    val uriHandler = LocalUriHandler.current

    DetailScaffold(
        title = title,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
                Text(
                    text = "FOCAL values your family's privacy and data protection.",
                    style = typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "We never sell your data or display advertisements. All policy rules and schedules are strictly enforced on your children's devices with end-to-end security.",
                    style = typography.body.copy(fontSize = 13.sp, color = ChildColors.Secondary, lineHeight = 19.sp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                PrimaryButton(
                    text = "View full document online",
                    onClick = { uriHandler.openUri(url) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =========================================================================
// TEXT-ONLY PAGE 9: DeleteAccountScreen
// =========================================================================
@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    repository: FocalRepository = FocalRepository.instance,
    modifier: Modifier = Modifier
) {
    val typography = LocalChildTypography.current
    var confirmInput by remember { mutableStateOf("") }
    var showPasswordConfirmDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    val isDeleteEnabled = confirmInput.trim() == "DELETE"

    DetailScaffold(
        title = "Delete account",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "This deletes your family, children, policies, schedules and cards. Your children's phones will stop being protected.",
                style = typography.body.copy(fontSize = 14.sp, color = ChildColors.Secondary, lineHeight = 20.sp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
                Text(
                    text = "Type DELETE to confirm",
                    style = typography.caption.copy(fontSize = 12.sp, color = ChildColors.Secondary)
                )
                Spacer(modifier = Modifier.height(10.dp))
                BasicTextField(
                    value = confirmInput,
                    onValueChange = { confirmInput = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, ChildColors.Border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Red PrimaryButton-shaped delete button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(ChildRadius.Pill))
                    .background(if (isDeleteEnabled) Color(0xFFFF453A) else Color(0x33FF453A))
                    .clickable(enabled = isDeleteEnabled) {
                        showPasswordConfirmDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Delete account",
                    style = typography.body.copy(
                        color = if (isDeleteEnabled) Color.White else Color(0x66FFFFFF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    if (showPasswordConfirmDialog) {
        Dialog(onDismissRequest = { showPasswordConfirmDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), padding = 24.dp) {
                Text(
                    text = "Final confirmation",
                    style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your password to permanently delete your account.",
                    style = typography.body.copy(fontSize = 13.sp, color = ChildColors.Secondary)
                )
                Spacer(modifier = Modifier.height(14.dp))
                BasicTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161616))
                        .border(1.dp, ChildColors.Border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(text = "Cancel", onClick = { showPasswordConfirmDialog = false }, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(ChildRadius.Pill))
                            .background(Color(0xFFFF453A))
                            .clickable {
                                showPasswordConfirmDialog = false
                                repository.deleteParentAccount()
                                onAccountDeleted()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Delete", style = typography.body.copy(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
