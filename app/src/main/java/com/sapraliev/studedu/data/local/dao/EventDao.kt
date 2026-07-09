package com.sapraliev.studedu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sapraliev.studedu.data.local.entity.EventEntity
import com.sapraliev.studedu.data.local.entity.RecurrenceExceptionEntity
import com.sapraliev.studedu.data.local.entity.RecurrenceRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface EventDao {

    // ---------- события ----------

    /**
     * Все события, чьё первое вхождение попадает в диапазон, плюс все
     * повторяющиеся (их вхождения в диапазоне генерирует доменный слой).
     */
    @Query(
        """
        SELECT * FROM events
        WHERE (start_at < :to AND end_at > :from)
           OR recurrence_rule_id IS NOT NULL
        ORDER BY start_at
        """
    )
    fun observeEventsAround(from: Instant, to: Instant): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE student_id = :studentId ORDER BY start_at DESC")
    fun observeEventsForStudent(studentId: String): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    // ---------- правила повторения ----------

    @Query("SELECT * FROM recurrence_rules WHERE id = :id")
    suspend fun getRuleById(id: String): RecurrenceRuleEntity?

    @Query("SELECT * FROM recurrence_rules")
    fun observeAllRules(): Flow<List<RecurrenceRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: RecurrenceRuleEntity)

    @Delete
    suspend fun deleteRule(rule: RecurrenceRuleEntity)

    // ---------- исключения серий ----------

    @Query("SELECT * FROM recurrence_exceptions WHERE event_id = :eventId")
    suspend fun getExceptionsForEvent(eventId: String): List<RecurrenceExceptionEntity>

    @Query("SELECT * FROM recurrence_exceptions")
    fun observeAllExceptions(): Flow<List<RecurrenceExceptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertException(exception: RecurrenceExceptionEntity)

    @Delete
    suspend fun deleteException(exception: RecurrenceExceptionEntity)

    // ---------- составные операции ----------

    /** Создание повторяющегося события: правило + событие одной транзакцией. */
    @Transaction
    suspend fun insertEventWithRule(rule: RecurrenceRuleEntity, event: EventEntity) {
        upsertRule(rule)
        upsertEvent(event)
    }
}
