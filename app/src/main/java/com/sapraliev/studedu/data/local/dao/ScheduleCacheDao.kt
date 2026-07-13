package com.sapraliev.studedu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sapraliev.studedu.data.local.entity.UniversityScheduleCacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/** Read-only кэш пар вуза. Пишет сюда только слой синхронизации ScheduleProvider. */
@Dao
interface ScheduleCacheDao {

    @Query(
        """
        SELECT * FROM university_schedule_cache
        WHERE start_at < :to AND end_at > :from
        ORDER BY start_at
        """
    )
    fun observeLessons(from: Instant, to: Instant): Flow<List<UniversityScheduleCacheEntity>>

    @Query("SELECT MAX(synced_at) FROM university_schedule_cache WHERE provider = :provider")
    suspend fun lastSyncedAt(provider: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<UniversityScheduleCacheEntity>)

    @Query("DELETE FROM university_schedule_cache WHERE provider = :provider AND group_name = :group")
    suspend fun clearForGroup(provider: String, group: String)

    /** Полная перезапись кэша группы при синке. */
    @Transaction
    suspend fun replaceForGroup(
        provider: String,
        group: String,
        lessons: List<UniversityScheduleCacheEntity>,
    ) {
        clearForGroup(provider, group)
        insertAll(lessons)
    }
}
