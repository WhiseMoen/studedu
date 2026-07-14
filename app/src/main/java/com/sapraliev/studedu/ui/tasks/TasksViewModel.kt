package com.sapraliev.studedu.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sapraliev.studedu.core.AppGraph
import com.sapraliev.studedu.data.local.AppDatabase
import com.sapraliev.studedu.data.local.dao.TaskDao
import com.sapraliev.studedu.data.local.entity.TaskEntity
import com.sapraliev.studedu.data.local.entity.TaskSource
import com.sapraliev.studedu.data.repository.EventRepository
import com.sapraliev.studedu.notifications.ReminderScheduler
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

enum class TaskFilter { ALL, DEADLINES }

data class TasksUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val today: LocalDate = LocalDate(2000, 1, 1),
)

class TasksViewModel(
    private val taskDao: TaskDao,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val filter = MutableStateFlow(TaskFilter.ALL)

    val uiState: StateFlow<TasksUiState> =
        combine(taskDao.observeAllTasks(), filter) { tasks, f ->
            TasksUiState(
                tasks = if (f == TaskFilter.DEADLINES) {
                    tasks.filter { it.dueDate != null }
                } else {
                    tasks
                },
                filter = f,
                today = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun setFilter(newFilter: TaskFilter) {
        filter.value = newFilter
    }

    fun addTask(text: String, tags: List<String>, dueDate: LocalDate?) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val now = Clock.System.now()
            taskDao.upsertTask(
                TaskEntity(
                    id = UUID.randomUUID().toString(),
                    userId = EventRepository.LOCAL_USER_ID,
                    text = text.trim(),
                    done = false,
                    tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
                    dueDate = dueDate,
                    source = TaskSource.PERSONAL,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            reminderScheduler.refresh()
        }
    }

    fun setDone(task: TaskEntity, done: Boolean) {
        viewModelScope.launch {
            taskDao.setDone(task.id, done)
            reminderScheduler.refresh()
        }
    }

    /** Мьют напоминаний о дедлайне для конкретной задачи (тонкая настройка тиров — позже). */
    fun setRemindersEnabled(task: TaskEntity, enabled: Boolean) {
        viewModelScope.launch {
            taskDao.setRemindersEnabled(task.id, enabled)
            reminderScheduler.refresh()
        }
    }

    fun delete(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            reminderScheduler.refresh()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TasksViewModel(
                    AppDatabase.get(context.applicationContext).taskDao(),
                    AppGraph.reminderScheduler,
                )
            }
        }
    }
}
