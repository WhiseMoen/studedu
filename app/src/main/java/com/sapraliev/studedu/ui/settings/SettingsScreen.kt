package com.sapraliev.studedu.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sapraliev.studedu.data.local.entity.HiddenLessonMode
import com.sapraliev.studedu.data.settings.ThemeMode
import com.sapraliev.studedu.ui.theme.LocalNeuShadows
import com.sapraliev.studedu.ui.theme.neumorphic
import compose.icons.FeatherIcons
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.ChevronRight
import compose.icons.feathericons.ExternalLink
import compose.icons.feathericons.RefreshCw
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.User
import compose.icons.feathericons.X
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var groupInput by remember { mutableStateOf("") }
    var editLinks by remember { mutableStateOf(false) }
    var lkInput by remember { mutableStateOf("") }
    var sdoInput by remember { mutableStateOf("") }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
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

        // ---------- расписание вуза ----------
        item {
            SettingsCard(title = "Расписание вуза") {
                Text(
                    "Московский Политех. Добавь свою группу — а рядом можно держать " +
                        "группы потока и переключаться между ними.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = groupInput,
                    onValueChange = { groupInput = it },
                    label = { Text("Группа (напр. 231-321)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        viewModel.addGroup(groupInput)
                        groupInput = ""
                    },
                    enabled = groupInput.isNotBlank() && state.syncStatus != SyncStatus.RUNNING,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Добавить группу")
                }

                if (state.groups.isNotEmpty()) {
                    Text(
                        "Мои группы",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.groups.forEach { group ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = group == state.activeGroup,
                                    onClick = { viewModel.switchGroup(group) },
                                    label = { Text(group) },
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.removeGroup(group) }) {
                                    Icon(
                                        FeatherIcons.Trash2,
                                        contentDescription = "Удалить группу $group",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.syncNow() },
                        enabled = state.activeGroup != null &&
                            state.syncStatus != SyncStatus.RUNNING,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.syncStatus == SyncStatus.RUNNING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                FeatherIcons.RefreshCw,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Обновить расписание")
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
                        color = MaterialTheme.colorScheme.error,
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

        // ---------- ссылки вуза ----------
        item {
            SettingsCard(title = "Вуз под рукой") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val lkInteraction = remember { MutableInteractionSource() }
                    val lkPressed by lkInteraction.collectIsPressedAsState()
                    OutlinedButton(
                        onClick = { openUrl(state.lkUrl) },
                        interactionSource = lkInteraction,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .neumorphic(LocalNeuShadows.current, cornerRadius = 20.dp, pressed = lkPressed),
                    ) {
                        Icon(
                            FeatherIcons.User,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Личный кабинет", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val sdoInteraction = remember { MutableInteractionSource() }
                    val sdoPressed by sdoInteraction.collectIsPressedAsState()
                    OutlinedButton(
                        onClick = { openUrl(state.sdoUrl) },
                        interactionSource = sdoInteraction,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .neumorphic(LocalNeuShadows.current, cornerRadius = 20.dp, pressed = sdoPressed),
                    ) {
                        Icon(
                            FeatherIcons.BookOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("СДО", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (!editLinks) {
                    TextButton(onClick = {
                        lkInput = state.lkUrl
                        sdoInput = state.sdoUrl
                        editLinks = true
                    }) {
                        Icon(
                            FeatherIcons.ExternalLink,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Изменить ссылки (для другого вуза)")
                    }
                } else {
                    OutlinedTextField(
                        value = lkInput,
                        onValueChange = { lkInput = it },
                        label = { Text("Ссылка на личный кабинет") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = sdoInput,
                        onValueChange = { sdoInput = it },
                        label = { Text("Ссылка на СДО / LMS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val saveInteraction = remember { MutableInteractionSource() }
                        val savePressed by saveInteraction.collectIsPressedAsState()
                        Button(
                            onClick = {
                                viewModel.saveLinks(lkInput, sdoInput)
                                editLinks = false
                            },
                            interactionSource = saveInteraction,
                            modifier = Modifier.neumorphic(
                                LocalNeuShadows.current,
                                cornerRadius = 20.dp,
                                pressed = savePressed,
                            ),
                        ) { Text("Сохранить") }
                        TextButton(onClick = { editLinks = false }) { Text("Отмена") }
                    }
                }
            }
        }

        // ---------- тема ----------
        item {
            SettingsCard(title = "Тема") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "Системная"
                                        ThemeMode.LIGHT -> "Светлая"
                                        ThemeMode.DARK -> "Тёмная"
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }

        // ---------- статистика ----------
        item {
            SettingsCard(title = "Статистика по ученикам") {
                StatsRow(
                    "Этот месяц",
                    state.stats.monthPaid,
                    state.stats.monthCharged,
                    state.stats.monthLessons,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                StatsRow(
                    "Прошлый месяц",
                    state.stats.prevPaid,
                    state.stats.prevCharged,
                    state.stats.prevLessons,
                )
                if (state.stats.topStudents.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Топ месяца по оплатам", style = MaterialTheme.typography.labelLarge)
                    state.stats.topStudents.forEach { (name, paid) ->
                        Text(
                            "  · $name — ${formatMoney(paid)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ---------- скрытые пары ----------
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
                                if (rule.mode == HiddenLessonMode.DIM) "приглушено" else "скрыто",
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
                            FeatherIcons.X,
                            contentDescription = "Вернуть пару",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun StatsRow(label: String, paid: Double, charged: Double, lessons: Int) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            "получено ${formatMoney(paid)} · начислено ${formatMoney(charged)} · занятий $lessons",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatMoney(amount: Double): String {
    val rounded = if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        "%.2f".format(amount)
    }
    return "$rounded ₽"
}

private fun formatSyncTime(epochMillis: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d.%02d %02d:%02d".format(
        local.dayOfMonth, local.monthNumber, local.hour, local.minute,
    )
}
