package com.sapraliev.studedu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sapraliev.studedu.data.local.entity.ScheduledReminderEntity

@Dao
interface ReminderDao {

    @Query("SELECT * FROM scheduled_reminders")
    suspend fun getAll(): List<ScheduledReminderEntity>

    @Query("DELETE FROM scheduled_reminders")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduledReminderEntity>)

    /** Пересчёт окна планирования: старый учёт всегда заменяется новым целиком. */
    @Transaction
    suspend fun replaceAll(items: List<ScheduledReminderEntity>) {
        clear()
        insertAll(items)
    }
}
