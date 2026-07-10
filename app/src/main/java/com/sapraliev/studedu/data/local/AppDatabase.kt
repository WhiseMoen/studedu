package com.sapraliev.studedu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sapraliev.studedu.data.local.dao.EventDao
import com.sapraliev.studedu.data.local.dao.HiddenLessonDao
import com.sapraliev.studedu.data.local.dao.ScheduleCacheDao
import com.sapraliev.studedu.data.local.dao.StudentDao
import com.sapraliev.studedu.data.local.dao.TaskDao
import com.sapraliev.studedu.data.local.entity.EventEntity
import com.sapraliev.studedu.data.local.entity.HiddenLessonRuleEntity
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
        HiddenLessonRuleEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun studentDao(): StudentDao
    abstract fun taskDao(): TaskDao
    abstract fun scheduleCacheDao(): ScheduleCacheDao
    abstract fun hiddenLessonDao(): HiddenLessonDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** v1 → v2: правила скрытия пар. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hidden_lesson_rules` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `group_name` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `lesson_type` TEXT,
                        `mode` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_hidden_lesson_rules_provider_group_name` " +
                        "ON `hidden_lesson_rules` (`provider`, `group_name`)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studedu.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
