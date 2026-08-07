package com.eto.manager.domain.repository

import com.eto.manager.domain.model.Department
import com.eto.manager.domain.model.Doctor
import com.eto.manager.domain.model.Token
import kotlinx.coroutines.flow.Flow

interface EtoRepository {
    fun getDoctors(): Flow<List<Doctor>>
    fun getDepartments(): Flow<List<Department>>
    fun getTokens(): Flow<List<Token>>
    fun getActiveTokensForDoctor(doctorId: String): Flow<List<Token>>
    fun getAllTokensForDoctor(doctorId: String): Flow<List<Token>>
    fun getTokensForPatient(phone: String): Flow<List<Token>>
    
    suspend fun requestToken(
        patientName: String,
        patientPhone: String,
        doctorId: String,
        symptoms: String,
        isWalkIn: Boolean = false
    ): Long

    suspend fun updateTokenStatus(tokenId: Long, status: String)
    suspend fun recordConsultation(tokenId: Long, diagnosis: String, prescription: String, amount: Double)
    suspend fun recordPayment(tokenId: Long)
    suspend fun seedInitialData()
    suspend fun clearAll()
    suspend fun updateDoctorAvailability(doctorId: String, isAvailable: Boolean)
}
