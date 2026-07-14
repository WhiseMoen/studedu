package com.sapraliev.studedu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sapraliev.studedu.data.local.dao.EnrollmentDao
import com.sapraliev.studedu.data.local.dao.EventDao
import com.sapraliev.studedu.data.local.dao.HiddenLessonDao
import com.sapraliev.studedu.data.local.dao.ReminderDao
import com.sapraliev.studedu.data.local.dao.ScheduleCacheDao
import com.sapraliev.studedu.data.local.dao.StudentDao
import com.sapraliev.studedu.data.local.dao.TaskDao
import com.sapraliev.studedu.data.local.entity.EnrollmentEntity
import com.sapraliev.studedu.data.local.entity.EventEntity
import com.sapraliev.studedu.data.local.entity.HiddenLessonRuleEntity
import com.sapraliev.studedu.data.local.entity.LessonRecordEntity
import com.sapraliev.studedu.data.local.entity.PaymentEntity
import com.sapraliev.studedu.data.local.entity.RecurrenceExceptionEntity
import com.sapraliev.studedu.data.local.entity.RecurrenceRuleEntity
import com.sapraliev.studedu.data.local.entity.ScheduledReminderEntity
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
        EnrollmentEntity::class,
        ScheduledReminderEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun studentDao(): StudentDao
    abstract fun taskDao(): TaskDao
    abstract fun scheduleCacheDao(): ScheduleCacheDao
    abstract fun hiddenLessonDao(): HiddenLessonDao
    abstract fun enrollmentDao(): EnrollmentDao
    abstract fun reminderDao(): ReminderDao

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

        /**
         * v2 → v3: enrollments (ученик × предмет), enrollment_id в events /
         * lesson_records / payments, lesson_record_id в payments;
         * из students уходят subject и ставки (переехали в enrollments).
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `enrollments` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `student_id` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `price_per_lesson` REAL,
                        `billing_mode` TEXT NOT NULL,
                        `monthly_fee` REAL,
                        `active` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`student_id`) REFERENCES `students`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_enrollments_student_id` " +
                        "ON `enrollments` (`student_id`)"
                )
                db.execSQL("ALTER TABLE `events` ADD COLUMN `enrollment_id` TEXT")
                db.execSQL("ALTER TABLE `lesson_records` ADD COLUMN `enrollment_id` TEXT")
                db.execSQL("ALTER TABLE `payments` ADD COLUMN `enrollment_id` TEXT")
                db.execSQL("ALTER TABLE `payments` ADD COLUMN `lesson_record_id` TEXT")

                // students: пересоздание без subject/price_per_lesson/monthly_fee.
                db.execSQL(
                    """
                    CREATE TABLE `students_new` (
                        `id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `contact` TEXT,
                        `active` INTEGER NOT NULL,
                        `notes` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO students_new " +
                        "(id, user_id, name, contact, active, notes, created_at, updated_at) " +
                        "SELECT id, user_id, name, contact, active, notes, created_at, updated_at " +
                        "FROM students"
                )
                db.execSQL("DROP TABLE `students`")
                db.execSQL("ALTER TABLE `students_new` RENAME TO `students`")
            }
        }

        /** v3 → v4: напоминания — мьют на событие/задачу + учёт запланированных будильников. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `events` ADD COLUMN `reminders_enabled` INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE `tasks` ADD COLUMN `reminders_enabled` INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scheduled_reminders` (
                        `request_code` INTEGER NOT NULL,
                        `trigger_at` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        PRIMARY KEY(`request_code`)
                    )
                    """.trimIndent()
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
