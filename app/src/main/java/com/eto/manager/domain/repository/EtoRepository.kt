package com.eto.manager.domain.repository

import com.eto.manager.domain.model.Department
import com.eto.manager.domain.model.Doctor
import com.eto.manager.domain.model.Hospital
import com.eto.manager.domain.model.Token
import kotlinx.coroutines.flow.Flow

interface EtoRepository {
    fun getDoctors(): Flow<List<Doctor>>
    fun getDepartments(): Flow<List<Department>>
    fun getHospitals(): Flow<List<Hospital>>
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
    suspend fun updatePatientProfile(phone: String, body: Map<String, String>): Boolean
    suspend fun getLabReports(phone: String): List<com.eto.manager.data.remote.LabReportResponse>
    suspend fun refreshData()
    suspend fun getPatientProfile(phone: String): com.eto.manager.data.remote.PatientProfileResponse
    suspend fun getDoctorProfile(doctorId: String): com.eto.manager.data.remote.DoctorProfileResponse
    suspend fun getReceptionistProfile(phoneOrId: String): com.eto.manager.data.remote.ReceptionistProfileResponse
}
