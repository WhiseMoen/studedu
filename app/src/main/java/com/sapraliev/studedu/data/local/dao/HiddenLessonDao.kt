package com.sapraliev.studedu.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sapraliev.studedu.data.local.entity.HiddenLessonRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenLessonDao {

    @Query(
        """
        SELECT * FROM hidden_lesson_rules
        WHERE provider = :provider AND group_name = :group
        ORDER BY subject, lesson_type
        """
    )
    fun observeRules(provider: String, group: String): Flow<List<HiddenLessonRuleEntity>>

    /** Для экрана настроек «Скрытые пары». */
    @Query("SELECT * FROM hidden_lesson_rules ORDER BY subject")
    fun observeAllRules(): Flow<List<HiddenLessonRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: HiddenLessonRuleEntity)

    @Delete
    suspend fun delete(rule: HiddenLessonRuleEntity)
}
