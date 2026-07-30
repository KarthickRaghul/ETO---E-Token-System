package com.eto.manager.data.repository

import com.eto.manager.data.local.dao.DepartmentDao
import com.eto.manager.data.local.dao.DoctorDao
import com.eto.manager.data.local.dao.TokenDao
import com.eto.manager.data.local.entity.DepartmentEntity
import com.eto.manager.data.local.entity.DoctorEntity
import com.eto.manager.data.local.entity.TokenEntity
import com.eto.manager.domain.model.Department
import com.eto.manager.domain.model.Doctor
import com.eto.manager.domain.model.PaymentStatus
import com.eto.manager.domain.model.Token
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.domain.repository.EtoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class EtoRepositoryImpl(
    private val doctorDao: DoctorDao,
    private val departmentDao: DepartmentDao,
    private val tokenDao: TokenDao
) : EtoRepository {

    override fun getDoctors(): Flow<List<Doctor>> {
        return doctorDao.getAllDoctors().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getDepartments(): Flow<List<Department>> {
        return departmentDao.getAllDepartments().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTokens(): Flow<List<Token>> {
        return tokenDao.getAllTokens().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getActiveTokensForDoctor(doctorId: String): Flow<List<Token>> {
        return tokenDao.getActiveTokensForDoctor(doctorId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllTokensForDoctor(doctorId: String): Flow<List<Token>> {
        return tokenDao.getAllTokensForDoctor(doctorId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTokensForPatient(phone: String): Flow<List<Token>> {
        return tokenDao.getTokensByPatientPhone(phone).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun requestToken(
        patientName: String,
        patientPhone: String,
        doctorId: String,
        symptoms: String,
        isWalkIn: Boolean
    ): Long {
        val doctor = doctorDao.getDoctorById(doctorId) ?: throw IllegalArgumentException("Doctor not found")
        
        // Find existing active tokens for this doctor to count position
        val activeTokens = tokenDao.getActiveTokensForDoctor(doctorId).first()
        val queueCount = activeTokens.size

        // Generate token number format: e.g. #GEN-102
        val prefix = doctor.specialty.take(3).uppercase()
        val num = 101 + activeTokens.size + (System.currentTimeMillis() % 10).toInt() // mock increment
        val tokenNum = "#$prefix-$num"

        val serviceTime = doctor.averageServiceTimeMinutes
        val estWait = queueCount * serviceTime

        val entity = TokenEntity(
            tokenNumber = tokenNum,
            patientName = patientName,
            patientPhone = patientPhone,
            doctorId = doctorId,
            doctorName = doctor.name,
            departmentName = doctor.departmentName,
            symptoms = symptoms,
            status = if (isWalkIn) "APPROVED" else "PENDING", // Walk-in is approved by default
            queuePosition = queueCount + 1,
            estimatedWaitMinutes = estWait,
            createdAt = System.currentTimeMillis()
        )
        return tokenDao.insertToken(entity)
    }

    override suspend fun updateTokenStatus(tokenId: Long, status: String) {
        val token = tokenDao.getTokenById(tokenId)
        if (token != null) {
            tokenDao.updateToken(token.copy(status = status))
        }
    }

    override suspend fun recordConsultation(
        tokenId: Long,
        diagnosis: String,
        prescription: String,
        amount: Double
    ) {
        val token = tokenDao.getTokenById(tokenId)
        if (token != null) {
            tokenDao.updateToken(
                token.copy(
                    diagnosis = diagnosis,
                    prescription = prescription,
                    billAmount = amount,
                    status = "COMPLETED",
                    paymentStatus = "PENDING"
                )
            )
        }
    }

    override suspend fun recordPayment(tokenId: Long) {
        val token = tokenDao.getTokenById(tokenId)
        if (token != null) {
            tokenDao.updateToken(token.copy(paymentStatus = "PAID"))
        }
    }

    override suspend fun seedInitialData() {
        val currentDepts = departmentDao.getAllDepartments().first()
        if (currentDepts.isEmpty()) {
            val depts = listOf(
                DepartmentEntity("1", "Cardiology", "Heart & Cardiovascular Care", "favorite"),
                DepartmentEntity("2", "Pediatrics", "Child Specialist & Growth Care", "child_care"),
                DepartmentEntity("3", "Dermatology", "Skin, Hair, Nail Treatment", "face"),
                DepartmentEntity("4", "General Medicine", "Primary Consult & Family Health", "medical_services")
            )
            departmentDao.insertDepartments(depts)
        }

        val currentDocs = doctorDao.getAllDoctors().first()
        if (currentDocs.isEmpty()) {
            val docs = listOf(
                DoctorEntity("d1", "Dr. Sarah Jenkins", "Cardiologist", "1", "Cardiology", 4.9f, 15, true),
                DoctorEntity("d2", "Dr. Robert Chen", "Pediatrician", "2", "Pediatrics", 4.8f, 12, true),
                DoctorEntity("d3", "Dr. Amanda Ross", "Dermatologist", "3", "Dermatology", 4.7f, 20, true),
                DoctorEntity("d4", "Dr. James Carter", "Physician", "4", "General Medicine", 4.6f, 10, true)
            )
            doctorDao.insertDoctors(docs)
        }
    }

    override suspend fun clearAll() {
        tokenDao.clearAllTokens()
    }

    // Mapper Functions
    private fun DoctorEntity.toDomain() = Doctor(
        id = id,
        name = name,
        specialty = specialty,
        departmentId = departmentId,
        departmentName = departmentName,
        rating = rating,
        averageServiceTimeMinutes = averageServiceTimeMinutes,
        isAvailable = isAvailable
    )

    private fun DepartmentEntity.toDomain() = Department(
        id = id,
        name = name,
        description = description,
        iconName = iconName
    )

    private fun TokenEntity.toDomain() = Token(
        id = id,
        tokenNumber = tokenNumber,
        patientName = patientName,
        patientPhone = patientPhone,
        doctorId = doctorId,
        doctorName = doctorName,
        departmentName = departmentName,
        symptoms = symptoms,
        status = when (status) {
            "APPROVED" -> TokenStatus.APPROVED
            "SERVING" -> TokenStatus.SERVING
            "COMPLETED" -> TokenStatus.COMPLETED
            "SKIPPED" -> TokenStatus.SKIPPED
            else -> TokenStatus.PENDING
        },
        queuePosition = queuePosition,
        estimatedWaitMinutes = estimatedWaitMinutes,
        createdAt = createdAt,
        diagnosis = diagnosis,
        prescription = prescription,
        billAmount = billAmount,
        paymentStatus = if (paymentStatus == "PAID") PaymentStatus.PAID else PaymentStatus.PENDING
    )
}
