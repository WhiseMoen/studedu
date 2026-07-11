package com.sapraliev.studedu.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sapraliev.studedu.data.local.entity.EventType
import com.sapraliev.studedu.ui.theme.ConflictRed
import com.sapraliev.studedu.ui.theme.EventPalette
import com.sapraliev.studedu.ui.theme.LocalNeuShadows
import com.sapraliev.studedu.ui.theme.neumorphic
import com.sapraliev.studedu.ui.util.RussianDates
import kotlin.time.Duration
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.factory(LocalContext.current),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    val enrollmentOptions by viewModel.enrollmentOptions.collectAsState()
    var editorOpen by remember { mutableStateOf(false) }
    var personalAction by remember { mutableStateOf<ScheduleCard.Personal?>(null) }
    var universityAction by remember { mutableStateOf<ScheduleCard.University?>(null) }
    var markDoneCard by remember { mutableStateOf<ScheduleCard.Personal?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editorOpen = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.neumorphic(LocalNeuShadows.current, cornerRadius = 28.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить событие")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            TodayHeader(state)
            ModeAndDateRow(state, viewModel)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.days.forEach { section ->
                    if (state.mode == ViewMode.WEEK) {
                        item(key = "header-${section.date}") {
                            Text(
                                text = RussianDates.fullDate(section.date),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    if (section.cards.isEmpty() && state.mode == ViewMode.DAY) {
                        item(key = "empty-${section.date}") { EmptyDay(state.universityGroup) }
                    }
                    items(section.cards, key = { it.key }) { card ->
                        ScheduleCardView(
                            card = card,
                            now = state.now,
                            onClick = {
                                when (card) {
                                    is ScheduleCard.Personal -> personalAction = card
                                    is ScheduleCard.University -> universityAction = card
                                }
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (editorOpen) {
        EventEditorSheet(
            initialDate = state.selectedDate,
            enrollmentOptions = enrollmentOptions,
            onDismiss = { editorOpen = false },
            onSave = { title, comment, type, start, end, recurrence, enrollment ->
                viewModel.createEvent(title, comment, type, start, end, recurrence, enrollment)
                editorOpen = false
            },
        )
    }

    personalAction?.let { card ->
        PersonalActionsDialog(
            card = card,
            onDismiss = { personalAction = null },
            onMarkDone = if (card.occurrence.enrollmentId != null) {
                {
                    markDoneCard = card
                    personalAction = null
                }
            } else {
                null
            },
            onCancelOccurrence = {
                viewModel.cancelOccurrence(card)
                personalAction = null
            },
            onDeleteEvent = {
                viewModel.deleteEvent(card)
                personalAction = null
            },
        )
    }

    markDoneCard?.let { card ->
        MarkDoneDialog(
            card = card,
            onDismiss = { markDoneCard = null },
            onConfirm = { amount, topics, homework ->
                viewModel.markLessonDone(card, amount, topics, homework)
                markDoneCard = null
            },
        )
    }

    universityAction?.let { card ->
        UniversityActionsDialog(
            card = card,
            onDismiss = { universityAction = null },
            onHide = { onlyThisType, dim ->
                viewModel.hideLesson(card, onlyThisType, dim)
                universityAction = null
            },
        )
    }
}

@Composable
private fun TodayHeader(state: TodayUiState) {
    val zone = TimeZone.currentSystemDefault()
    val nowLocal = state.now.toLocalDateTime(zone)
    val shadows = LocalNeuShadows.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.neumorphic(shadows),
        ) {
            Text(
                text = "%02d:%02d".format(nowLocal.hour, nowLocal.minute),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = RussianDates.fullDate(state.selectedDate),
                style = MaterialTheme.typography.titleMedium,
            )
            val until = state.untilNext
            if (until != null && state.nextTitle != null) {
                Text(
                    text = "через ${formatDuration(until)} — ${state.nextTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "ближайших событий нет",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ModeAndDateRow(state: TodayUiState, viewModel: TodayViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = state.mode == ViewMode.DAY,
            onClick = { viewModel.setMode(ViewMode.DAY) },
            label = { Text("День") },
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = state.mode == ViewMode.WEEK,
            onClick = { viewModel.setMode(ViewMode.WEEK) },
            label = { Text("Неделя") },
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { viewModel.shiftDate(if (state.mode == ViewMode.DAY) -1 else -7) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
        }
        TextButton(onClick = { viewModel.goToday() }) { Text("Сегодня") }
        IconButton(onClick = { viewModel.shiftDate(if (state.mode == ViewMode.DAY) 1 else 7) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Вперёд")
        }
    }
}

@Composable
private fun ScheduleCardView(card: ScheduleCard, now: Instant, onClick: () -> Unit) {
    val zone = TimeZone.currentSystemDefault()
    val start = card.start.toLocalDateTime(zone)
    val end = card.end.toLocalDateTime(zone)
    val hasConflict = card.conflictTitles.isNotEmpty()
    val isPast = card.end < now
    val dark = isSystemInDarkTheme()

    val dimmed = card is ScheduleCard.University && card.dimmed
    val baseColor = when (card) {
        is ScheduleCard.University -> EventPalette.university(dark)
        is ScheduleCard.Personal -> when (card.occurrence.type) {
            EventType.PERSONAL -> EventPalette.personal(dark)
            EventType.LESSON -> EventPalette.lesson(dark)
            EventType.DEADLINE -> EventPalette.deadline(dark)
        }
    }
    val cardColor = when {
        dimmed -> baseColor.copy(alpha = 0.35f)
        isPast -> baseColor.copy(alpha = 0.45f)
        else -> baseColor
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (hasConflict) BorderStroke(2.dp, ConflictRed) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d:%02d".format(start.hour, start.minute),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "%02d:%02d".format(end.hour, end.minute),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        RoundedCornerShape(2.dp),
                    ),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        card.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (card is ScheduleCard.Personal && card.occurrence.isMoved) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "перенесено",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (dimmed) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "не хожу",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                when (card) {
                    is ScheduleCard.Personal -> card.occurrence.comment?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is ScheduleCard.University -> {
                        val subtitle = listOfNotNull(
                            card.lesson.lessonType,
                            card.lesson.place,
                            card.lesson.teacher,
                        ).joinToString(" · ")
                        if (subtitle.isNotEmpty()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
          