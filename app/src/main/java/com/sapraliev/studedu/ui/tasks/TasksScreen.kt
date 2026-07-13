package com.sapraliev.studedu.ui.tasks

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sapraliev.studedu.data.local.entity.TaskEntity
import com.sapraliev.studedu.ui.theme.ConflictRed
import com.sapraliev.studedu.ui.theme.LocalNeuShadows
import com.sapraliev.studedu.ui.theme.neumorphic
import com.sapraliev.studedu.ui.util.RussianDates
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel(
        factory = TasksViewModel.factory(LocalContext.current),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    var addOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { addOpen = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.neumorphic(LocalNeuShadows.current, cornerRadius = 28.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить задачу")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Заметки и дедлайны",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.filter == TaskFilter.ALL,
                        onClick = { viewModel.setFilter(TaskFilter.ALL) },
                        label = { Text("Все") },
                    )
                    FilterChip(
                        selected = state.filter == TaskFilter.DEADLINES,
                        onClick = { viewModel.setFilter(TaskFilter.DEADLINES) },
                        label = { Text("Дедлайны") },
                    )
                }
            }
            if (state.tasks.isEmpty()) {
                item {
                    Text(
                        "Пусто. Задача с датой — это дедлайн, без даты — заметка.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(state.tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    today = state.today,
                    onToggle = { viewModel.setDone(task, it) },
                    onDelete = { viewModel.delete(task) },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (addOpen) {
        AddTaskDialog(
            onDismiss = { addOpen = false },
            onSave = { text, tags, due ->
                viewModel.addTask(text, tags, due)
                addOpen = false
            },
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    today: LocalDate,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val overdue = !task.done && task.dueDate != null && task.dueDate < today
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.done, onCheckedChange = onToggle)
            Column(Modifier.weight(1f)) {
                Text(
                    task.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                val meta = buildList {
                    task.dueDate?.let { add("до ${RussianDates.dayMonth(it)}") }
                    if (task.tags.isNotEmpty()) add(task.tags.joinToString(" ") { "#$it" })
                }.joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) ConflictRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (text: String, tags: List<String>, due: LocalDate?) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<LocalDate?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст") },
                )
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Теги через запятую") },
                    singleLine = true,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { pickerOpen = true }) {
                        Text(due?.let { "до ${RussianDates.dayMonth(it)}" } ?: "Без дедлайна")
                    }
                    if (due != null) {
                        TextButton(onClick = { due = null }) { Text("Убрать дату") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isNotBlank()) {
                    onSave(text, tagsText.split(","), due)
                }
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )

    if (pickerOpen) {
        val zone = TimeZone.currentSystemDefault()
        val initial = due ?: kotlinx.datetime.Clock.System.now().toLocalDateTime(zone).date
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atTime(12, 0)
                .toInstant(TimeZone.UTC).toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        due = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                    }
                    pickerOpen = false
                }) { Text("Ок") }
            },
            dismissButton = {
                TextButton(onClick = { pickerOpen = false }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
