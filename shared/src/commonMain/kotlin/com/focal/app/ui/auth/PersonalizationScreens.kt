package com.focal.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focal.app.data.FocalLocalStorage
import com.focal.app.theme.FocalColors
import com.focal.app.ui.components.*

// Data holder for personalization answers
object PersonalizationState {
    // Parent Answers
    var parentGoal by mutableStateOf("")
    val parentWorryApps = mutableStateListOf<String>()
    var parentChildrenCount by mutableStateOf("1")
    var parentAgeGroup by mutableStateOf("10–13")

    // Child Answers
    val childDistractionApps = mutableStateListOf<String>()
    var childGoal by mutableStateOf("")
    var childHardestTime by mutableStateOf("")
}

// -------------------------------------------------------------
// PARENT PERSONALIZATION FLOW (3 Screens + Plan Summary)
// -------------------------------------------------------------
@Composable
fun ParentPersonalizationFlow(
    onComplete: () -> Unit,
    onBackToRoleSelect: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1, 2, 3, 4 (Plan Summary)

    when (step) {
        1 -> ParentP1Screen(
            onNext = { step = 2 },
            onBack = onBackToRoleSelect
        )
        2 -> ParentP2Screen(
            onNext = { step = 3 },
            onBack = { step = 1 }
        )
        3 -> ParentP3Screen(
            onNext = { step = 4 },
            onBack = { step = 2 }
        )
        4 -> ParentPlanSummaryScreen(
            onContinue = {
                FocalLocalStorage.saveBoolean("KEY_PERSONALIZATION_DONE", true)
                onComplete()
            },
            onBack = { step = 3 }
        )
    }
}

@Composable
private fun ParentP1Screen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val options = listOf(
        "Better study focus",
        "Less social media",
        "Healthier sleep",
        "Less gaming",
        "Overall less screen time"
    )

    PersonalizationLayout(
        stepTitle = "1 of 3",
        title = "What's your main goal for your child?",
        caption = "We'll suggest focus policies based on your main goal.",
        onBack = onBack
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                val isSelected = PersonalizationState.parentGoal == option
                SelectableOptionRow(
                    text = option,
                    isSelected = isSelected,
                    isMulti = false,
                    onClick = {
                        PersonalizationState.parentGoal = option
                        FocalLocalStorage.saveString("KEY_PARENT_GOAL", option)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        PrimaryButton(
            text = "Next",
            enabled = PersonalizationState.parentGoal.isNotBlank(),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}

@Composable
private fun ParentP2Screen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val options = listOf(
        "Instagram", "YouTube", "Shorts/Reels", "Games",
        "Snapchat", "WhatsApp", "Browser", "Other"
    )

    PersonalizationLayout(
        stepTitle = "2 of 3",
        title = "Which apps worry you most?",
        caption = "Select all that apply. You can change these later.",
        onBack = onBack
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                val isSelected = PersonalizationState.parentWorryApps.contains(option)
                SelectableOptionRow(
                    text = option,
                    isSelected = isSelected,
                    isMulti = true,
                    onClick = {
                        if (isSelected) {
                            PersonalizationState.parentWorryApps.remove(option)
                        } else {
                            PersonalizationState.parentWorryApps.add(option)
                        }
                        FocalLocalStorage.saveString("KEY_PARENT_WORRY_APPS", PersonalizationState.parentWorryApps.joinToString(","))
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        PrimaryButton(
            text = "Next",
            enabled = PersonalizationState.parentWorryApps.isNotEmpty(),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}

@Composable
private fun ParentP3Screen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val childCountOptions = listOf("1", "2", "3+")
    val ageGroupOptions = listOf("6–9", "10–13", "14–17")

    PersonalizationLayout(
        stepTitle = "3 of 3",
        title = "About your family",
        caption = "This helps us tailor schedule defaults and guidelines.",
        onBack = onBack
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("NUMBER OF CHILDREN", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                childCountOptions.forEach { count ->
                    val isSelected = PersonalizationState.parentChildrenCount == count
                    FocalCard(
                        modifier = Modifier.weight(1f),
                        borderColor = if (isSelected) Color.White else FocalColors.Border,
                        onClick = {
                            PersonalizationState.parentChildrenCount = count
                            FocalLocalStorage.saveString("KEY_PARENT_CHILDREN_COUNT", count)
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(text = count, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("AGE GROUP", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            ageGroupOptions.forEach { age ->
                val isSelected = PersonalizationState.parentAgeGroup == age
                SelectableOptionRow(
                    text = "$age years old",
                    isSelected = isSelected,
                    isMulti = false,
                    onClick = {
                        PersonalizationState.parentAgeGroup = age
                        FocalLocalStorage.saveString("KEY_PARENT_AGE_GROUP", age)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        PrimaryButton(
            text = "Generate Focal Plan",
            enabled = PersonalizationState.parentChildrenCount.isNotBlank() && PersonalizationState.parentAgeGroup.isNotBlank(),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}

@Composable
private fun ParentPlanSummaryScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            StatusBadge(text = "TAILORED PLAN", badgeType = BadgeType.ACTIVE)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your FOCAL Plan",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Based on your goal: ${PersonalizationState.parentGoal.ifEmpty { "Better study focus" }}",
                style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Policy Proposal Card
            FocalCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = FocalIcons.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Suggested First Policy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Study Mode", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Blocks ${PersonalizationState.parentWorryApps.take(3).joinToString(", ").ifEmpty { "Instagram, YouTube, Games" }} during active focus.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Schedule Proposal Card
            FocalCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = FocalIcons.Calendar, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Suggested Focus Schedule", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Weekdays 4:00 PM – 7:00 PM", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "School-age routine for age group ${PersonalizationState.parentAgeGroup}.",
                        style = MaterialTheme.typography.bodySmall.copy(color = FocalColors.TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You can customize or change all of this later in your Parent Dashboard.",
                style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = "Continue to Create Account",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// CHILD PERSONALIZATION FLOW (3 Screens + Plan Summary)
// -------------------------------------------------------------
@Composable
fun ChildPersonalizationFlow(
    onComplete: () -> Unit,
    onBackToRoleSelect: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1, 2, 3, 4 (Plan Summary)

    when (step) {
        1 -> ChildC1Screen(
            onNext = { step = 2 },
            onBack = onBackToRoleSelect
        )
        2 -> ChildC2Screen(
            onNext = { step = 3 },
            onBack = { step = 1 }
        )
        3 -> ChildC3Screen(
            onNext = { step = 4 },
            onBack = { step = 2 }
        )
        4 -> ChildPlanSummaryScreen(
            onContinue = {
                FocalLocalStorage.saveBoolean("KEY_PERSONALIZATION_DONE", true)
                onComplete()
            },
            onBack = { step = 3 }
        )
    }
}

@Composable
private fun ChildC1Screen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val options = listOf(
        "Instagram", "YouTube", "Shorts/Reels", "Games",
        "Snapchat", "WhatsApp", "Browsing", "Other"
    )

    PersonalizationLayout(
        stepTitle = "1 of 3",
        title = "What distracts you the most?",
        caption = "Select apps that pull your attention away.",
        onBack = onBack
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                val isSelected = PersonalizationState.childDistractionApps.contains(option)
                SelectableOptionRow(
                    text = option,
                    isSelected = isSelected,
                    isMulti = true,
                    onClick = {
                        if (isSelected) {
                            PersonalizationState.childDistractionApps.remove(option)
                        } else {
                            PersonalizationState.childDistractionApps.add(option)
                        }
                        FocalLocalStorage.saveString("KEY_CHILD_DISTRACTION_APPS", PersonalizationState.childDistractionApps.joinToString(","))
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        PrimaryButton(
            text = "Next",
            enabled = PersonalizationState.childDistractionApps.isNotEmpty(),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}

@Composable
private fun ChildC2Screen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val options = listOf(
        "Studying",
        "Homework on time",
        "Sleeping earlier",
        "Reading",
        "Sports/hobbies"
    )

    PersonalizationLayout(
        stepTitle = "2 of 3",
        title = "What do you want to get better at?",
        caption = "Choose your main focus goal.",
        onBack = onBack
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                val isSelected = PersonalizationState.childGoal == option
                SelectableOptionRow(
                    text = option,
                    isSelected = isSelected,
                    isMulti = false,
                    onClick = {
                        PersonalizationState.childGoal = option
                        FocalLocalStorage.saveString("KEY_CHILD_GOAL", option)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        PrimaryButton(
            text = "Next",
            enabled = PersonalizationState.childGoal.isNotBlank(),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}

@Composable
private fun ChildC3Screen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val options = listOf(
        "Morning",
        "After school",
        "Evening",
        "Late night"
    )

    PersonalizationLayout(
        stepTitle = "3 of 3",
        title = "When is it hardest to focus?",
        caption = "Tell us when distractions hit hardest.",
        onBack = onBack
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                val isSelected = PersonalizationState.childHardestTime == option
                SelectableOptionRow(
                    text = option,
                    isSelected = isSelected,
                    isMulti = false,
                    onClick = {
                        PersonalizationState.childHardestTime = option
                        FocalLocalStorage.saveString("KEY_CHILD_HARDEST_TIME", option)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        PrimaryButton(
            text = "See Your Focus Plan",
            enabled = PersonalizationState.childHardestTime.isNotBlank(),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}

@Composable
private fun ChildPlanSummaryScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val topDistractions = PersonalizationState.childDistractionApps.take(2).joinToString(" & ").ifEmpty { "YouTube & Games" }
    val goal = PersonalizationState.childGoal.ifEmpty { "studying" }
    val time = PersonalizationState.childHardestTime.lowercase().ifEmpty { "after school" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            StatusBadge(text = "YOUR FOCUS PLAN", badgeType = BadgeType.ACTIVE)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Focus Plan",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Focus $time, keep $topDistractions away while $goal.",
                style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            FocalCard(modifier = Modifier.fillMaxWidth(), padding = 18.dp) {
                Column {
                    Text("WHAT YOU TOLD US", style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• Goal: $goal", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Hardest Time: ${PersonalizationState.childHardestTime}", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Main Distractions: ${PersonalizationState.childDistractionApps.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Your parent sets the final rules — this helps them know what you need.",
                style = MaterialTheme.typography.labelSmall.copy(color = FocalColors.TextSecondary, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = "Connect to your parent",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// REUSABLE PERSONALIZATION LAYOUT COMPONENTS
// -------------------------------------------------------------
@Composable
private fun PersonalizationLayout(
    stepTitle: String,
    title: String,
    caption: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = FocalIcons.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                StatusBadge(text = stepTitle, badgeType = BadgeType.ACTIVE)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium.copy(color = FocalColors.TextSecondary)
            )

            Spacer(modifier = Modifier.height(24.dp))

            content()
        }
    }
}

@Composable
private fun SelectableOptionRow(
    text: String,
    isSelected: Boolean,
    isMulti: Boolean,
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
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(if (isMulti) RoundedCornerShape(6.dp) else CircleShape)
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .border(1.5.dp, if (isSelected) Color.White else FocalColors.Border, if (isMulti) RoundedCornerShape(6.dp) else CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = FocalIcons.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
