package com.sapraliev.studedu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Этап 0: вкладки-заглушки. Каждая станет реальным экраном на этапах 1–5.

@Composable
private fun StubScreen(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun TodayScreen() = StubScreen(
    title = "Сегодня",
    subtitle = "Этап 1: лента событий, часы, конфликты с парами вуза",
)

@Composable
fun TasksScreen() = StubScreen(
    title = "Заметки + Дедлайны",
    subtitle = "Этап 4: задачи, теги, дедлайны",
)

@Composable
fun StudentsScreen() = StubScreen(
    title = "Ученики",
    subtitle = "Этап 3: карточки учеников, оплаты, дз",
)

@Composable
fun SettingsScreen() = StubScreen(
    title = "Настройки",
    subtitle = "Этап 5: вуз и группа, тема, синхронизация",
)
