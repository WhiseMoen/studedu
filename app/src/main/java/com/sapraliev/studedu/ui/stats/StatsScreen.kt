package com.sapraliev.studedu.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        FeatherIcons.ArrowLeft,
                        contentDescription = "Назад",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Статистика по ученикам",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Итого по всем ученикам",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    StatsPeriodLine("Этот месяц", state.totalMonthLessons, state.totalMonthPaid, state.totalMonthCharged)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    StatsPeriodLine("Прошлый месяц", state.totalPrevLessons, state.totalPrevPaid, state.totalPrevCharged)
                }
            }
        }

        if (state.students.isEmpty()) {
            item {
                Text(
                    "Пока нет учеников",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        items(state.students, key = { it.studentId }) { row ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        row.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    StatsPeriodLine("Этот месяц", row.monthLessons, row.monthPaid, row.monthCharged)
                    StatsPeriodLine("Прошлый месяц", row.prevLessons, row.prevPaid, row.prevCharged)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatsPeriodLine(label: String, lessons: Int, paid: Double, charged: Double) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            "занятий $lessons · получено ${formatMoney(paid)} · начислено ${formatMoney(charged)}",
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
