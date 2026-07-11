package com.sapraliev.studedu.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sapraliev.studedu.data.local.entity.HiddenLessonMode
import com.sapraliev.studedu.ui.theme.ConflictRed
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(LocalContext.current),
    ),
) {
    val state by viewModel.uiState.collectAsState()
    var groupInput by remember { mutableStateOf("") }

    LaunchedEffect(state.group) {
        if (groupInput.isEmpty() && state.group != null) groupInput = state.group!!
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Настройки",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Расписание вуза", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Московский Политех. Укажи группу — пары появятся в ленте «Сегодня».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = groupInput,
                            onValueChange = { groupInput = it },
                            label = { Text("Группа (напр. 231-321)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = {
                                viewModel.saveGroup(groupInput)
                                viewModel.syncNow()
                            },
                            enabled = groupInput.isNotBlank() &&
                                state.syncStatus != SyncStatus.RUNNING,
                        ) {
                            if (state.syncStatus == SyncStatus.RUNNING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(20.dp).height(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Синк")
                            }
                        }
                    }
                    when (state.syncStatus) {
                        SyncStatus.SUCCESS -> Text(
                            "Расписание обновлено",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )

                        SyncStatus.ERROR -> Text(
                            state.syncError ?: "Ошибка синхронизации",
                            style = MaterialTheme.typography.bodySmall,
                            color = ConflictRed,
                        )

                        else -> state.lastSyncAt?.let {
                            Text(
                                "Последний синк: ${formatSyncTime(it)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Скрытые пары",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (state.hiddenRules.isEmpty()) {
            item {
                Text(
                    "Пока пусто. Нажми на пару в ленте и выбери «не хожу».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(state.hiddenRules, key = { it.id }) { rule ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(rule.subject, style = MaterialTheme.typography.bodyLarge)
                        val details = buildList {
                            rule.lessonType?.let { add("только «$it»") }
                            add(
                                if (rule.mode == HiddenLessonMode.DIM) {
                                    "приглушено"
                                } else {
                                    "скрыто"
                                },
                            )
                        }.joinToString(" · ")
                        Text(
                            details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { viewModel.removeHiddenRule(rule) }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Вернуть пару",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

private fun formatSyncTime(epochMillis: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d.%02d %02d:%02d".format(
        local.dayOfMonth, local.monthNumber, local.hour, local.minute,
    )
}
