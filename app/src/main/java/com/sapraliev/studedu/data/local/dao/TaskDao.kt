package com.sapraliev.studedu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sapraliev.studedu.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY done, due_date IS NULL, due_date, created_at DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE due_date IS NOT NULL AND done = 0 AND due_date <= :until
        ORDER BY due_date
        """
    )
    fun observeUpcomingDeadlines(until: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    /** Незавершённые дедлайны с включёнными напоминаниями — для планировщика. */
    @Query(
        "SELECT * FROM tasks WHERE due_date IS NOT NULL AND done = 0 AND reminders_enabled = 1"
    )
    suspend fun getRemindableDeadlines(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean)

    @Query("UPDATE tasks SET reminders_enabled = :enabled WHERE id = :id")
    suspend fun setRemindersEnabled(id: String, enabled: Boolean)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}
