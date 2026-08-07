package com.eto.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eto.manager.data.local.entity.DepartmentEntity
import com.eto.manager.data.local.entity.DoctorEntity
import com.eto.manager.data.local.entity.TokenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorDao {
    @Query("SELECT * FROM doctors")
    fun getAllDoctors(): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM doctors WHERE departmentId = :deptId")
    fun getDoctorsByDepartment(deptId: String): Flow<List<DoctorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctors(doctors: List<DoctorEntity>)

    @Query("SELECT * FROM doctors WHERE id = :id LIMIT 1")
    suspend fun getDoctorById(id: String): DoctorEntity?

    @Query("UPDATE doctors SET isAvailable = :isAvailable WHERE id = :id")
    suspend fun updateAvailability(id: String, isAvailable: Boolean)
}

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments")
    fun getAllDepartments(): Flow<List<DepartmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartments(departments: List<DepartmentEntity>)
}

@Dao
interface TokenDao {
    @Query("SELECT * FROM tokens ORDER BY createdAt DESC")
    fun getAllTokens(): Flow<List<TokenEntity>>

    @Query("SELECT * FROM tokens WHERE doctorId = :doctorId AND status IN ('PENDING', 'SERVING') ORDER BY id ASC")
    fun getActiveTokensForDoctor(doctorId: String): Flow<List<TokenEntity>>

    @Query("SELECT * FROM tokens WHERE doctorId = :doctorId ORDER BY id ASC")
    fun getAllTokensForDoctor(doctorId: String): Flow<List<TokenEntity>>

    @Query("SELECT * FROM tokens WHERE patientPhone = :phone ORDER BY createdAt DESC")
    fun getTokensByPatientPhone(phone: String): Flow<List<TokenEntity>>

    @Query("SELECT * FROM tokens WHERE status = 'PENDING' ORDER BY id ASC")
    fun getPendingTokens(): Flow<List<TokenEntity>>

    @Query("SELECT * FROM tokens WHERE id = :id")
    suspend fun getTokenById(id: Long): TokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: TokenEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokens(tokens: List<TokenEntity>)

    @Update
    suspend fun updateToken(token: TokenEntity)

    @Query("DELETE FROM tokens")
    suspend fun clearAllTokens()
}
