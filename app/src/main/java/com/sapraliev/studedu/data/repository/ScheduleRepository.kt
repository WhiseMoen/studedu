package com.sapraliev.studedu.data.repository

import com.sapraliev.studedu.data.local.dao.HiddenLessonDao
import com.sapraliev.studedu.data.local.dao.ScheduleCacheDao
import com.sapraliev.studedu.data.local.entity.HiddenLessonRuleEntity
import com.sapraliev.studedu.data.local.entity.UniversityScheduleCacheEntity
import com.sapraliev.studedu.domain.schedule.ScheduleProvider
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Расписание вуза: read-only кэш в Room + синк через ScheduleProvider.
 * UI всегда читает кэш — источник дёргается только явным syncNow().
 */
class ScheduleRepository(
    private val provider: ScheduleProvider,
    private val cacheDao: ScheduleCacheDao,
    private val hiddenLessonDao: HiddenLessonDao,
) {

    fun observeLessons(from: Instant, to: Instant): Flow<List<UniversityScheduleCacheEntity>> =
        cacheDao.observeLessons(from, to)

    fun observeHiddenRules(group: String): Flow<List<HiddenLessonRuleEntity>> =
        hiddenLessonDao.observeRules(provider.id, group)

    fun observeAllHiddenRules(): Flow<List<HiddenLessonRuleEntity>> =
        hiddenLessonDao.observeAllRules()

    /**
     * Полный синк группы: неделя назад + 30 дней вперёд.
     * Кэш группы перезаписывается атомарно; при ошибке старый кэш цел.
     * @throws com.sapraliev.studedu.domain.schedule.ScheduleSyncException
     */
    suspend fun syncNow(group: String) {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(zone).date
        val entries = provider.getSchedule(
            group = group,
            from = today.minus(DatePeriod(days = 7)),
            to = today.plus(DatePeriod(days = 30)),
        )
        val now = Clock.System.now()
        val cached = entries.map { entry ->
            UniversityScheduleCacheEntity(
                id = UUID.randomUUID().toString(),
                userId = EventRepository.LOCAL_USER_ID,
                provider = provider.id,
                group = group,
                subject = entry.subject,
                teacher = entry.teacher,
                place = entry.place,
                lessonType = entry.lessonType,
                startAt = entry.start,
                endAt = entry.end,
                syncedAt = now,
            )
        }
        cacheDao.replaceForGroup(provider.id, group, cached)
    }

    suspend fun hideLesson(
        group: String,
        subject: String,
        lessonType: String?,
        dim: Boolean,
    ) {
        hiddenLessonDao.upsert(
            HiddenLessonRuleEntity(
                id = UUID.randomUUID().toString(),
                userId = EventRepository.LOCAL_USER_ID,
                provider = provider.id,
                group = group,
                subject = subject,
                lessonType = lessonType,
                mode = if (dim) {
                    com.sapraliev.studedu.data.local.entity.HiddenLessonMode.DIM
                } else {
                    com.sapraliev.studedu.data.local.entity.HiddenLessonMode.HIDE
                },
                createdAt = Clock.System.now(),
            ),
        )
    }

    suspend fun removeHiddenRule(rule: HiddenLessonRuleEntity) {
        hiddenLessonDao.delete(rule)
    }
}
