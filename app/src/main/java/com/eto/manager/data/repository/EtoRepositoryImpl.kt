package com.eto.manager.data.repository

import android.util.Log
import com.eto.manager.data.local.dao.DepartmentDao
import com.eto.manager.data.local.dao.DoctorDao
import com.eto.manager.data.local.dao.TokenDao
import com.eto.manager.data.local.entity.DepartmentEntity
import com.eto.manager.data.local.entity.DoctorEntity
import com.eto.manager.data.local.entity.TokenEntity
import com.eto.manager.data.remote.ApiService
import com.eto.manager.data.remote.ConsultationRequest
import com.eto.manager.data.remote.RetrofitClient
import com.eto.manager.data.remote.SocketManager
import com.eto.manager.data.remote.TokenRequest
import com.eto.manager.domain.model.Department
import com.eto.manager.domain.model.Doctor
import com.eto.manager.domain.model.PaymentStatus
import com.eto.manager.domain.model.Token
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.domain.repository.EtoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EtoRepositoryImpl(
    private val doctorDao: DoctorDao,
    private val departmentDao: DepartmentDao,
    private val tokenDao: TokenDao
) : EtoRepository {

    private val apiService = RetrofitClient.apiService
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Start listening to WebSocket updates to trigger automatic synchronization
        SocketManager.connect {
            repositoryScope.launch {
                Log.d("EtoRepositoryImpl", "Sync triggered by SocketIO event")
                syncWithBackend()
            }
        }
        // Run initial sync on launch
        repositoryScope.launch {
            syncWithBackend()
        }
    }

    private suspend fun syncWithBackend() {
        try {
            // Sync Departments
            val remoteDepts = apiService.getDepartments()
            if (remoteDepts.isNotEmpty()) {
                departmentDao.insertDepartments(remoteDepts)
            }

            // Sync Doctors
            val remoteDocs = apiService.getDoctors()
            if (remoteDocs.isNotEmpty()) {
                doctorDao.insertDoctors(remoteDocs)
            }

            // Sync Tokens (clear local first to reflect exact state in backend queue)
            val remoteTokens = apiService.getTokens()
            tokenDao.clearAllTokens()
            if (remoteTokens.isNotEmpty()) {
                tokenDao.insertTokens(remoteTokens)
            }
            Log.d("EtoRepositoryImpl", "Successfully synced all tables with backend.")
        } catch (e: Exception) {
            Log.e("EtoRepositoryImpl", "Failed to sync data with Node.js backend", e)
        }
    }

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
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.requestToken(
                    TokenRequest(patientName, patientPhone, doctorId, symptoms, isWalkIn)
                )
                // Instantly sync the changes down to local database
                syncWithBackend()
                response.tokenId
            } catch (e: Exception) {
                Log.e("EtoRepositoryImpl", "Error requesting token from backend", e)
                0L
            }
        }
    }

    override suspend fun updateTokenStatus(tokenId: Long, status: String) {
        withContext(Dispatchers.IO) {
            try {
                apiService.updateTokenStatus(tokenId, mapOf("status" to status))
                syncWithBackend()
            } catch (e: Exception) {
                Log.e("EtoRepositoryImpl", "Error updating token status on backend", e)
            }
        }
    }

    override suspend fun recordConsultation(
        tokenId: Long,
        diagnosis: String,
        prescription: String,
        amount: Double
    ) {
        withContext(Dispatchers.IO) {
            try {
                apiService.recordConsultation(
                    tokenId,
                    ConsultationRequest(diagnosis, prescription, amount)
                )
                syncWithBackend()
            } catch (e: Exception) {
                Log.e("EtoRepositoryImpl", "Error recording consultation on backend", e)
            }
        }
    }

    override suspend fun recordPayment(tokenId: Long) {
        withContext(Dispatchers.IO) {
            try {
                apiService.recordPayment(tokenId)
                syncWithBackend()
            } catch (e: Exception) {
                Log.e("EtoRepositoryImpl", "Error recording payment on backend", e)
            }
        }
    }

    override suspend fun seedInitialData() {
        // Handled automatically on backend server startup. We just sync the seeded data.
        withContext(Dispatchers.IO) {
            syncWithBackend()
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            try {
                apiService.clearAll()
                syncWithBackend()
            } catch (e: Exception) {
                Log.e("EtoRepositoryImpl", "Error clearing data on backend", e)
            }
        }
    }

    override suspend fun updateDoctorAvailability(doctorId: String, isAvailable: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                apiService.updateDoctorAvailability(doctorId, mapOf("isAvailable" to isAvailable))
                syncWithBackend()
            } catch (e: Exception) {
                Log.e("EtoRepositoryImpl", "Error updating doctor availability on backend", e)
            }
        }
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
