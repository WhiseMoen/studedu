package com.sapraliev.studedu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sapraliev.studedu.data.local.entity.EnrollmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface EnrollmentDao {

    @Query("SELECT * FROM enrollments WHERE student_id = :studentId AND active = 1 ORDER BY subject")
    fun observeByStudent(studentId: String): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE active = 1 ORDER BY subject")
    fun observeAllActive(): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE id = :id")
    suspend fun getById(id: String): EnrollmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(enrollment: EnrollmentEntity)

    @Query("UPDATE enrollments SET active = 0, updated_at = :now WHERE id = :id")
    suspend fun deactivate(id: String, now: Instant)

    @Query("DELETE FROM enrollments WHERE id = :id")
    suspend fun delete(id: String)
}
