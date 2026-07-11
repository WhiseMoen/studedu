package com.sapraliev.studedu.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sapraliev.studedu.data.local.entity.EventType
import com.sapraliev.studedu.data.local.entity.RecurrenceFreq
import com.sapraliev.studedu.data.repository.NewRecurrence
import com.sapraliev.studedu.ui.util.RussianDates
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private enum class RepeatOption(val label: String) {
    NONE("Нет"),
    DAILY("Каждый день"),
    WEEKLY("Каждую неделю"),
    BIWEEKLY("Раз в 2 недели"),
}

private enum class EndOption(val label: String) {
    NEVER("Никогда"),
    COUNT("N раз"),
    UNTIL("До даты"),
}

private val weekdayCodes = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

/**
 * Создание события: заголовок, комментарий, дата и время,
 * тип, правило повторения. Правило хранится как RRULE-модель,
 * копии строк не создаются.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorSheet(
    initialDate: LocalDate,
    enrollmentOptions: List<EnrollmentOption>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        comment: String?,
        type: EventType,
        start: Instant,
        end: Instant,
        recurrence: NewRecurrence?,
        enrollment: EnrollmentOption?,
    ) -> Unit,
) {
    val zone = TimeZone.currentSystemDefault()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(EventType.PERSONAL) }
    var date by remember { mutableStateOf(initialDate) }
    var startHour by remember { mutableStateOf(10) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(11) }
    var endMinute by remember { mutableStateOf(0) }

    var selectedEnrollment by remember { mutableStateOf<EnrollmentOption?>(null) }
    var repeat by remember { mutableStateOf(RepeatOption.NONE) }
    var weekdays by remember { mutableStateOf(setOf(initialDate.dayOfWeek.ordinal)) }
    var endOption by remember { mutableStateOf(EndOption.NEVER) }
    var countText by remember { mutableStateOf("10") }
    var untilDate by remember { mutableStateOf(initialDate) }

    var datePickerFor by remember { mutableStateOf<String?>(null) } // "date" | "until"
    var timePickerFor by remember { mutableStateOf<String?>(null) } // "start" | "end"

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Новое событие", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Тип", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == EventType.PERSONAL,
                    onClick = { type = EventType.PERSONAL },
                    label = { Text("Личное") },
                )
                FilterChip(
                    selected = type == EventType.LESSON,
                    onClick = { type = EventType.LESSON },
                    label = { Text("Занятие") },
                )
                FilterChip(
                    selected = type == EventType.DEADLINE,
                    onClick = { type = EventType.DEADLINE },
                    label = { Text("Дедлайн") },
                )
            }

            if (type == EventType.LESSON) {
                Text("Ученик и предмет", style = MaterialTheme.typography.labelLarge)
                if (enrollmentOptions.isEmpty()) {
                    Text(
                        "Сначала добавь ученика на вкладке «Ученики» — тогда занятие привяжется к оплатам.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        enrollmentOptions.forEach { option ->
                            FilterChip(
                                selected = selectedEnrollment?.enrollmentId == option.enrollmentId,
                                onClick = {
                                    selectedEnrollment = option
                                    if (title.isBlank()) title = option.label
                                },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }
            }

            Text("Когда", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { datePickerFor = "date" }) {
                    Text(RussianDates.dayMonth(date))
                }
                OutlinedButton(onClick = { timePickerFor = "start" }) {
                    Text("%02d:%02d".format(startHour, startMinute))
                }
                Text("—", modifier = Modifier.padding(top = 12.dp))
                OutlinedButton(onClick = { timePickerFor = "end" }) {
                    Text("%02d:%02d".format(endHour, endMinute))
                }
            }

            Text("Повторение", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                RepeatOption.entries.forEach { option ->
                    FilterChip(
                        selected = repeat == option,
                        onClick = { repeat = option },
                        label = { Text(option.label, maxLines = 1) },
                    )
                }
            }

            if (repeat == RepeatOption.WEEKLY || repeat == RepeatOption.BIWEEKLY) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.values().take(7).forEachIndexed { index, day ->
                        FilterChip(
                            selected = index in weekdays,
                            onClick = {
                                weekdays =
                                    if (index in weekdays) weekdays - index else weekdays + index
                            },
                            label = { Text(RussianDates.weekdayShort(day)) },
                        )
                    }
                }
            }

            if (repeat != RepeatOption.NONE) {
                Text("Окончание", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EndOption.entries.forEach { option ->
                        FilterChip(
                            selected = endOption == option,
                            onClick = { endOption = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                when (endOption) {
                    EndOption.COUNT -> OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Сколько раз") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(160.dp),
                    )

                    EndOption.UNTIL -> OutlinedButton(onClick = { datePickerFor = "until" }) {
                        Text("до ${RussianDates.dayMonth(untilDate)}")
                    }

                    EndOption.NEVER -> Unit
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val start = date.atTime(startHour, startMinute).toInstant(zone)
                    var end = date.atTime(endHour, endMinute).toInstant(zone)
                    if (end <= start) end = start + 1.hours

                    val recurrence = when (repeat) {
                        RepeatOption.NONE -> null
                        RepeatOption.DAILY -> NewRecurrence(
                            freq = RecurrenceFreq.DAILY,
                            count = countOrNull(endOption, countText),
                            until = untilOrNull(endOption, untilDate),
                        )

                        RepeatOption.WEEKLY, RepeatOption.BIWEEKLY -> NewRecurrence(
                            freq = RecurrenceFreq.WEEKLY,
                            interval = if (repeat == RepeatOption.BIWEEKLY) 2 else 1,
                            byweekday = weekdays.sorted().map { weekdayCodes[it] },
                            count = countOrNull(endOption, countText),
                            until = untilOrNull(endOption, untilDate),
                        )
                    }
                    onSave(
                        title,
                        comment.takeIf { it.isNotBlank() },
                        type,
                        start,
                        end,
                        recurrence,
                        if (type == EventType.LESSON) selectedEnrollment else null,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить")
            }
        }
    }

    if (datePickerFor != null) {
        val target = datePickerFor
        val initial = if (target == "until") untilDate else date
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atTime(12, 0)
                .toInstant(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        if (target == "until") untilDate = picked else date = picked
                    }
                    datePickerFor = null
                }) { Text("Ок") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerFor = null }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (timePickerFor != null) {
        val target = timePickerFor
        val timeState = rememberTimePickerState(
            initialHour = if (target == "start") startHour else endHour,
            initialMinute = if (target == "start") startMinute else endMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timePickerFor = null },
            title = { Text(if (target == "start") "Начало" else "Конец") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    if (target == "start") {
                        startHour = timeState.hour
                        startMinute = timeState.minute
                        // конец по умолчанию тянется за началом
                        if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
                            endHour = (startHour + 1).coerceAtMost(23)
                            endMinute = startMinute
                        }
                    } else {
                        endHour = timeState.hour
                        endMinute = timeState.minute
                    }
                    timePickerFor = null
                }) { Text("Ок") }
            },
            dismissButton = {
                TextButton(onClick = { timePickerFor = null }) { Text("Отмена") }
            },
        )
    }
}

private fun countOrNull(option: EndOption, text: String): Int? =
    if (option == EndOption.COUNT) text.toIntOrNull()?.coerceIn(1, 500) else null

private fun untilOrNull(option: EndOption, date: LocalDate): LocalDate? =
    if (option == EndOption.UNTIL) date else null
