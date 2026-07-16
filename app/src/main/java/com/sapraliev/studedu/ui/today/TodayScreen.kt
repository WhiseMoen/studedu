package com.sapraliev.studedu.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sapraliev.studedu.data.local.entity.EventType
import com.sapraliev.studedu.ui.theme.ConflictRed
import com.sapraliev.studedu.ui.theme.EventPalette
import com.sapraliev.studedu.ui.theme.LocalNeuShadows
import com.sapraliev.studedu.ui.theme.NeuShadows
import com.sapraliev.studedu.ui.theme.StudentTintPalette
import com.sapraliev.studedu.ui.theme.neumorphic
import com.sapraliev.studedu.ui.util.RussianDates
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.ArrowRight
import compose.icons.feathericons.Plus
import kotlin.time.Duration
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalFoundationApi::class)
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
            ) {
                Icon(FeatherIcons.Plus, contentDescription = "Добавить событие")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            item(key = "hero") {
                HeroSection(state, modifier = Modifier.fillParentMaxHeight(0.85f))
            }
            stickyHeader(key = "mode-row") {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = if (state.mode == ViewMode.MONTH) {
                                RussianDates.monthYear(state.selectedDate)
                            } else {
                                RussianDates.fullDate(state.selectedDate)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        ModeAndDateRow(state, viewModel)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            if (state.mode == ViewMode.MONTH) {
                item(key = "month-grid") {
                    MonthGrid(state = state, onDayClick = viewModel::selectDay)
                }
            } else {
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
                            modifier = Modifier.padding(vertical = 5.dp),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (editorOpen) {
        EventEditorSheet(
            initialDate = state.selectedDate,
            enrollmentOptions = enrollmentOptions,
            onDismiss = { editorOpen = false },
            onCreateStudent = { name, contact, subject, price, fee, onCreated ->
                viewModel.createStudentForEvent(name, contact, subject, price, fee, onCreated)
            },
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
            onToggleReminders = {
                viewModel.setEventRemindersEnabled(card, !card.occurrence.remindersEnabled)
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

/**
 * «Обложка» ленты «Сегодня»: огромные неоморфные часы по центру экрана,
 * дата словами и ближайшее событие. Первый item в LazyColumn — лента
 * дня открывается прокруткой вниз.
 */
@Composable
private fun HeroSection(state: TodayUiState, modifier: Modifier = Modifier) {
    val zone = TimeZone.currentSystemDefault()
    val nowLocal = state.now.toLocalDateTime(zone)
    val shadows = LocalNeuShadows.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.neumorphic(shadows, cornerRadius = 32.dp, blur = 20.dp, offset = 8.dp),
        ) {
            EmbossedClockText(
                text = RussianDates.time(nowLocal.hour, nowLocal.minute),
                shadows = shadows,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 22.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = RussianDates.heroLine(nowLocal.date),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        val until = state.untilNext
        val nextStart = state.nextStart
        if (until != null && state.nextTitle != null && nextStart != null) {
            val startLocal = nextStart.toLocalDateTime(zone)
            Text(
                text = state.nextTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "${RussianDates.time(startLocal.hour, startLocal.minute)} · через ${formatDuration(until)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "сегодня свободно",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Цифры часов с эффектом гравировки: под основным текстом — размытые
 * копии в цветах теней неоморфизма (светлая сверху-слева, тёмная
 * снизу-справа), чтобы цифры визуально «продолжали» выпуклость Surface,
 * а не лежали на ней плоским слоем.
 */
@Composable
private fun EmbossedClockText(text: String, shadows: NeuShadows, modifier: Modifier = Modifier) {
    Box(modifier) {
        Text(
            text = text,
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = shadows.dark,
            modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .blur(3.dp),
        )
        Text(
            text = text,
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = shadows.light,
            modifier = Modifier
                .offset(x = (-2).dp, y = (-2).dp)
                .blur(3.dp),
        )
        Text(
            text = text,
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ModeAndDateRow(state: TodayUiState, viewModel: TodayViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = true,
            onClick = { viewModel.cycleMode() },
            label = { Text(modeLabel(state.mode)) },
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { viewModel.shiftDate(-1) }) {
            Icon(
                FeatherIcons.ArrowLeft,
                contentDescription = "Назад",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { viewModel.goToday() }) {
            Text("Сегодня", maxLines = 1, softWrap = false)
        }
        IconButton(onClick = { viewModel.shiftDate(1) }) {
            Icon(
                FeatherIcons.ArrowRight,
                contentDescription = "Вперёд",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun modeLabel(mode: ViewMode): String = when (mode) {
    ViewMode.DAY -> "День"
    ViewMode.WEEK -> "Неделя"
    ViewMode.MONTH -> "Месяц"
}

/** Сетка месяца: свой грид (без библиотек), понедельник — первый день недели. */
@Composable
private fun MonthGrid(state: TodayUiState, onDayClick: (LocalDate) -> Unit) {
    val days = state.days
    if (days.isEmpty()) return
    val firstDate = days.first().date
    val leadingBlanks = firstDate.dayOfWeek.ordinal
    val weekdayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(LocalNeuShadows.current, cornerRadius = 24.dp, blur = 12.dp, offset = 5.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                weekdayLabels.forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            val cells: List<DaySection?> = List(leadingBlanks) { null } + days
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { section ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                        ) {
                            if (section != null) {
                                MonthDayCell(
                                    section = section,
                                    isSelected = section.date == state.selectedDate,
                                    isToday = section.date == state.now.toLocalDateTime(TimeZone.currentSystemDefault()).date,
                                    onClick = { onDayClick(section.date) },
                                )
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    section: DaySection,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val hasConflict = section.cards.any { it.conflictTitles.isNotEmpty() }
    val hasEvents = section.cards.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    isToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    else -> Color.Transparent
                },
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            section.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
        )
        if (hasEvents) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .size(6.dp)
                    .background(
                        if (hasConflict) ConflictRed else MaterialTheme.colorScheme.primary,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun ScheduleCardView(
    card: ScheduleCard,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = TimeZone.currentSystemDefault()
    val start = card.start.toLocalDateTime(zone)
    val end = card.end.toLocalDateTime(zone)
    val hasConflict = card.conflictTitles.isNotEmpty()
    val isPast = card.end < now

    val dimmed = card is ScheduleCard.University && card.dimmed
    val studentTint = (card as? ScheduleCard.Personal)?.studentTint
    val baseColor = when {
        studentTint != null -> StudentTintPalette.colorFor(studentTint)
        card is ScheduleCard.University -> EventPalette.university()
        card is ScheduleCard.Personal -> when (card.occurrence.type) {
            EventType.PERSONAL -> EventPalette.personal()
            EventType.LESSON -> EventPalette.lesson()
            EventType.DEADLINE -> EventPalette.deadline()
        }
        else -> EventPalette.personal()
    }
    val washAlpha = when {
        dimmed -> 0.14f
        isPast -> 0.18f
        else -> 0.32f
    }

    // Неоморфная карточка: поверхность в тон фона (как сетка месяца), а цвет
    // типа/оттенок ученика — лёгкой подложкой поверх, не сплошной заливкой,
    // чтобы объём давали тени, а не контраст цвета (см. Neumorphic.kt).
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.background,
        border = if (hasConflict) BorderStroke(2.dp, ConflictRed) else null,
        modifier = modifier
            .fillMaxWidth()
            .neumorphic(LocalNeuShadows.current, cornerRadius = 20.dp, blur = 10.dp, offset = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .background(baseColor.copy(alpha = washAlpha))
                .padding(14.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    RussianDates.time(start.hour, start.minute),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    RussianDates.time(end.hour, end.minute),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (hasConflict) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Пересекается: ${card.conflictTitles.joinToString()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ConflictRed,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDay(group: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Свободный день",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (group == null) {
                "Добавь событие кнопкой «+» или укажи группу вуза в настройках"
            } else {
                "Добавь событие кнопкой «+»"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PersonalActionsDialog(
    card: ScheduleCard.Personal,
    onDismiss: () -> Unit,
    onMarkDone: (() -> Unit)?,
    onCancelOccurrence: () -> Unit,
    onToggleReminders: () -> Unit,
    onDeleteEvent: () -> Unit,
) {
    val isSeries = card.occurrence.originalStart != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(card.occurrence.title) },
        text = {
            Text(
                if (isSeries) "Это вхождение повторяющейся серии." else "Что сделать с событием?",
            )
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (onMarkDone != null) {
                    TextButton(onClick = onMarkDone) { Text("Проведено ✓") }
                }
                if (isSeries) {
                    TextButton(onClick = onCancelOccurrence) { Text("Отменить это вхождение") }
                }
                TextButton(onClick = onToggleReminders) {
                    Text(
                        if (card.occurrence.remindersEnabled) {
                            "Отключить уведомления"
                        } else {
                            "Включить уведомления"
                        },
                    )
                }
                TextButton(onClick = onDeleteEvent) {
                    Text(
                        if (isSeries) "Удалить всю серию" else "Удалить событие",
                        color = ConflictRed,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        },
    )
}

@Composable
private fun MarkDoneDialog(
    card: ScheduleCard.Personal,
    onDismiss: () -> Unit,
    onConfirm: (amountOverride: Double?, topics: String?, homework: String?) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf("") }
    var homework by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Проведено: ${card.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { text ->
                        amountText = text.filter { it.isDigit() || it == '.' }.take(9)
                    },
                    label = { Text("Сумма, ₽ (пусто — по ставке)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = topics,
                    onValueChange = { topics = it },
                    label = { Text("Пройденные темы") },
                )
                OutlinedTextField(
                    value = homework,
                    onValueChange = { homework = it },
                    label = { Text("Домашнее задание") },
                )
                Text(
                    "Начисление зафиксирует сумму: смена ставки потом не изменит эту запись. 0 — бесплатное занятие.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(amountText.toDoubleOrNull(), topics, homework)
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun UniversityActionsDialog(
    card: ScheduleCard.University,
    onDismiss: () -> Unit,
    onHide: (onlyThisType: Boolean, dim: Boolean) -> Unit,
) {
    val type = card.lesson.lessonType
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(card.lesson.subject) },
        text = {
            Text(
                "«Не хожу» освобождает это время: пара перестаёт участвовать " +
                    "в конфликтах, и на него можно ставить занятия.",
            )
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { onHide(false, false) }) {
                    Text("Скрыть предмет полностью")
                }
                if (type != null) {
                    TextButton(onClick = { onHide(true, false) }) {
                        Text("Скрыть только «$type»")
                    }
                }
                TextButton(onClick = { onHide(false, true) }) {
                    Text("Показывать приглушённо")
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        },
    )
}

private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
        hours > 0 -> "$hours ч"
        else -> "$minutes мин"
    }
}
