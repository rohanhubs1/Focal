package com.focal.app.ui.controller

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.focal.app.data.ActivityEvent
import com.focal.app.data.ActivityEventType
import com.focal.app.data.ActivityRepository
import com.focal.app.data.FocalRepository
import com.focal.app.data.getDisplayDetail
import com.focal.app.data.getDisplayTitle
import com.focal.app.ui.child.SectionOverline
import com.focal.app.ui.child.components.GlassCard
import com.focal.app.ui.child.components.GlowArcs
import com.focal.app.ui.child.components.PrimaryButton
import com.focal.app.ui.child.components.SecondaryButton
import com.focal.app.ui.components.FocalIcons
import com.focal.app.ui.theme.ChildColors
import com.focal.app.ui.theme.LocalChildTypography

/**
 * Tab 2: Focus Tab Root Screen ("Start focus")
 * Structural foundation for Step P2.
 */
@Composable
fun ParentStartFocusRootScreen(
    onStartFocusForChild: (childId: String) -> Unit = {},
    onAddChildClick: () -> Unit = {},
    repository: FocalRepository = FocalRepository.instance
) {
    ParentFocusTabScreen(
        onAddChildClick = onAddChildClick,
        repository = repository
    )
}

/**
 * Tab 3: Activity Tab Root Screen ("Activity")
 * Displays real-time timeline of child focus and protection events matching PA2_activity.png.
 */
@Composable
fun ParentActivityScreen(
    initialFilter: String? = null,
    activityRepository: ActivityRepository = ActivityRepository.instance,
    repository: FocalRepository = FocalRepository.instance
) {
    val typography = LocalChildTypography.current
    val rawEvents by activityRepository.events.collectAsState()

    // Mark events as read on opening
    LaunchedEffect(Unit) {
        activityRepository.markAllAsRead()
    }

    // Available filter chips: "All", dynamic child names, "Requests"
    val childrenNames = remember(repository.childrenList.size) {
        repository.childrenList.map { it.name }
    }
    val filterOptions = remember(childrenNames) {
        listOf("All") + childrenNames + listOf("Requests")
    }
    var selectedFilter by remember {
        mutableStateOf(initialFilter ?: "All")
    }

    // Filter events
    val filteredEvents = remember(rawEvents, selectedFilter, repository.childrenList) {
        when (selectedFilter) {
            "All" -> rawEvents
            "Requests" -> rawEvents.filter {
                it.type == ActivityEventType.EXIT_REQUESTED ||
                it.type == ActivityEventType.EXIT_APPROVED ||
                it.type == ActivityEventType.EXIT_DECLINED
            }
            else -> {
                val matchedChild = repository.childrenList.find { it.name.equals(selectedFilter, ignoreCase = true) }
                if (matchedChild != null) {
                    rawEvents.filter { it.childId == matchedChild.id }
                } else {
                    rawEvents
                }
            }
        }
    }

    // Spam prevention grouping: repeated APP_BLOCKED for same app within 10 minutes collapsed
    val aggregatedEvents = remember(filteredEvents) {
        if (filteredEvents.isEmpty()) emptyList()
        else {
            val result = mutableListOf<ActivityEvent>()
            for (event in filteredEvents) {
                val last = result.lastOrNull()
                if (last != null &&
                    last.type == ActivityEventType.APP_BLOCKED &&
                    event.type == ActivityEventType.APP_BLOCKED &&
                    last.childId == event.childId &&
                    last.metadata["app"] == event.metadata["app"] &&
                    kotlin.math.abs(last.timestampMillis - event.timestampMillis) <= 10 * 60 * 1000L
                ) {
                    val count = (last.metadata["count"]?.toIntOrNull() ?: 1) + 1
                    result[result.lastIndex] = last.copy(
                        metadata = last.metadata + ("count" to count.toString())
                    )
                } else {
                    result.add(event)
                }
            }
            result
        }
    }

    // Group by dayGroup ("TODAY", "YESTERDAY", "12 OCT", etc.)
    val groupedEvents = remember(aggregatedEvents) {
        aggregatedEvents.groupBy { it.dayGroup }
    }

    var pendingRequestToResolve by remember { mutableStateOf<ActivityEvent?>(null) }

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
                .padding(bottom = 110.dp)
        ) {
            // Header Title: 34sp bold Geist white
            Text(
                text = "Activity",
                style = typography.display.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(4.dp))

            // Subtitle: 14sp secondary
            Text(
                text = "What happened on your children's phones.",
                style = typography.caption.copy(
                    fontSize = 14.sp,
                    color = ChildColors.Secondary
                )
            )

            Spacer(Modifier.height(18.dp))

            // Filter chips (single-select horizontal scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { option ->
                    val isSelected = (option == selectedFilter)
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color(0xFF0E0E0E))
                            .then(
                                if (!isSelected) Modifier.border(1.dp, Color(0xFF2A2A2A), CircleShape)
                                else Modifier
                            )
                            .clickable { selectedFilter = option }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            style = typography.headline.copy(
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (aggregatedEvents.isEmpty()) {
                Spacer(Modifier.height(30.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 24.dp,
                    padding = 28.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No activity yet",
                            style = typography.headline.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Events from your children's phones will show here.",
                            style = typography.caption.copy(
                                fontSize = 13.5.sp,
                                color = ChildColors.Secondary
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Render each group under SectionOverline inside ONE GlassCard
                groupedEvents.forEach { (dayGroup, eventsInGroup) ->
                    SectionOverline(text = dayGroup)

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        radius = 24.dp,
                        padding = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            eventsInGroup.forEachIndexed { index, event ->
                                val childInitials = remember(event.childId, repository.childrenList.size) {
                                    val child = repository.childrenList.find { it.id == event.childId }
                                    if (child != null) {
                                        val parts = child.name.trim().split(" ").filter { it.isNotBlank() }
                                        if (parts.size >= 2) "${parts.first().firstOrNull() ?: ""}${parts.last().firstOrNull() ?: ""}".uppercase()
                                        else child.name.take(2).uppercase().ifEmpty { "CH" }
                                    } else {
                                        when (event.childId) {
                                            "child_1" -> "AR"
                                            "child_2" -> "KI"
                                            else -> "CH"
                                        }
                                    }
                                }

                                val isPendingExit = (event.type == ActivityEventType.EXIT_REQUESTED && event.isPending)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isPendingExit) {
                                            pendingRequestToResolve = event
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 36dp child initials avatar
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF171717))
                                            .border(1.dp, Color(0xFF2A2A2A), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = childInitials,
                                            style = typography.caption.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        val isProtectionOff = (event.type == ActivityEventType.PROTECTION_OFF)
                                        Text(
                                            text = event.getDisplayTitle(),
                                            style = typography.headline.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isProtectionOff) Color(0xFFFF453A) else Color.White
                                            ),
                                            maxLines = 2
                                        )

                                        Spacer(Modifier.height(2.dp))

                                        Text(
                                            text = event.getDisplayDetail(),
                                            style = typography.caption.copy(
                                                fontSize = 12.sp,
                                                color = ChildColors.Secondary
                                            ),
                                            maxLines = 2
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Time on right
                                    Text(
                                        text = event.timeClock,
                                        style = typography.caption.copy(
                                            fontSize = 12.sp,
                                            color = Color(0xFF6B6B6B)
                                        )
                                    )
                                }

                                // 1dp #1F1F1F divider between rows (except after last row)
                                if (index < eventsInGroup.size - 1) {
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
                }
            }
        }
    }

    // Modal to resolve pending emergency exit request if tapped in Activity tab
    if (pendingRequestToResolve != null) {
        val targetEvent = pendingRequestToResolve!!
        val requestId = targetEvent.metadata["requestId"] ?: "req_1"
        Dialog(onDismissRequest = { pendingRequestToResolve = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Emergency Exit Request",
                        style = typography.headline.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = targetEvent.details.ifBlank { targetEvent.metadata["reason"] ?: "Requested exit" },
                        style = typography.body.copy(
                            fontSize = 14.sp,
                            color = ChildColors.Secondary
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryButton(
                            text = "Allow exit",
                            onClick = {
                                repository.resolveEmergencyRequest(requestId, true)
                                pendingRequestToResolve = null
                            },
                            modifier = Modifier.weight(1f).height(44.dp)
                        )

                        SecondaryButton(
                            text = "Decline",
                            onClick = {
                                repository.resolveEmergencyRequest(requestId, false)
                                pendingRequestToResolve = null
                            },
                            modifier = Modifier.weight(1f).height(44.dp)
                        )
                    }
                }
            }
        }
    }
}
