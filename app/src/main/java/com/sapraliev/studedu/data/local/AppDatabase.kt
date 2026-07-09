package com.sapraliev.studedu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sapraliev.studedu.data.local.dao.EventDao
import com.sapraliev.studedu.data.local.dao.ScheduleCacheDao
import com.sapraliev.studedu.data.local.dao.StudentDao
import com.sapraliev.studedu.data.local.dao.TaskDao
import com.sapraliev.studedu.data.local.entity.EventEntity
import com.sapraliev.studedu.data.local.entity.LessonRecordEntity
import com.sapraliev.studedu.data.local.entity.PaymentEntity
import com.sapraliev.studedu.data.local.entity.RecurrenceExceptionEntity
import com.sapraliev.studedu.data.local.entity.RecurrenceRuleEntity
import com.sapraliev.studedu.data.local.entity.StudentEntity
import com.sapraliev.studedu.data.local.entity.TaskEntity
import com.sapraliev.studedu.data.local.entity.UniversityScheduleCacheEntity

/**
 * Локальная база — источник правды для UI (offline-first).
 * Supabase используется только для синхронизации между устройствами.
 */
@Database(
    entities = [
        EventEntity::class,
        RecurrenceRuleEntity::class,
        RecurrenceExceptionEntity::class,
        StudentEntity::class,
        LessonRecordEntity::class,
        PaymentEntity::class,
        TaskEntity::class,
        UniversityScheduleCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun studentDao(): StudentDao
    abstract fun taskDao(): TaskDao
    abstract fun scheduleCacheDao(): ScheduleCacheDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studedu.db",
                ).build().also { instance = it }
            }
    }
}
