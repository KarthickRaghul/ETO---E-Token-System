package com.eto.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eto.manager.data.local.entity.*
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

    @androidx.room.Transaction
    suspend fun replaceAllTokens(tokens: List<TokenEntity>) {
        clearAllTokens()
        insertTokens(tokens)
    }
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)
}

@Dao
interface HospitalDao {
    @Query("SELECT * FROM hospitals")
    fun getAllHospitals(): Flow<List<HospitalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHospitals(hospitals: List<HospitalEntity>)
}

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getPatientById(id: String): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<PatientEntity>)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY appointmentDate DESC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<AppointmentEntity>)
}

@Dao
interface QueueEntryDao {
    @Query("SELECT * FROM queue_entries WHERE doctorId = :doctorId")
    fun getQueueEntriesForDoctor(doctorId: String): Flow<List<QueueEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueEntries(entries: List<QueueEntryEntity>)
}

@Dao
interface ConsultationDao {
    @Query("SELECT * FROM consultations WHERE tokenId = :tokenId")
    suspend fun getConsultationForToken(tokenId: String): ConsultationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsultations(consultations: List<ConsultationEntity>)
}

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions WHERE consultationId = :consultationId")
    suspend fun getPrescriptionForConsultation(consultationId: String): PrescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptions(prescriptions: List<PrescriptionEntity>)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE tokenId = :tokenId")
    suspend fun getBillForToken(tokenId: String): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<BillEntity>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)
}
