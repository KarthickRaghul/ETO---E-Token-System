package com.eto.manager.data.remote

import com.eto.manager.data.local.entity.DepartmentEntity
import com.eto.manager.data.local.entity.DoctorEntity
import com.eto.manager.data.local.entity.TokenEntity
import retrofit2.http.*

interface ApiService {

    @GET("api/departments")
    suspend fun getDepartments(): List<DepartmentEntity>

    @GET("api/doctors")
    suspend fun getDoctors(): List<DoctorEntity>

    @PATCH("api/doctors/{id}/availability")
    suspend fun updateDoctorAvailability(
        @Path("id") doctorId: String,
        @Body body: Map<String, Boolean>
    ): Map<String, Any>

    @GET("api/tokens")
    suspend fun getTokens(): List<TokenEntity>

    @POST("api/tokens/request")
    suspend fun requestToken(
        @Body body: TokenRequest
    ): TokenResponse

    @PATCH("api/tokens/{id}/status")
    suspend fun updateTokenStatus(
        @Path("id") tokenId: Long,
        @Body body: Map<String, String>
    ): Map<String, Any>

    @POST("api/tokens/{id}/consultation")
    suspend fun recordConsultation(
        @Path("id") tokenId: Long,
        @Body body: ConsultationRequest
    ): Map<String, Any>

    @POST("api/tokens/{id}/payment")
    suspend fun recordPayment(
        @Path("id") tokenId: Long
    ): Map<String, Any>

    @POST("api/admin/clear")
    suspend fun clearAll(): Map<String, Any>

    @GET("api/profile/patient/{phone}")
    suspend fun getPatientProfile(@Path("phone") phone: String): PatientProfileResponse

    @GET("api/profile/doctor/{id}")
    suspend fun getDoctorProfile(@Path("id") doctorId: String): DoctorProfileResponse

    @GET("api/profile/receptionist/{phoneOrId}")
    suspend fun getReceptionistProfile(@Path("phoneOrId") phoneOrId: String): ReceptionistProfileResponse
}

data class PatientProfileResponse(
    val id: String,
    val first_name: String,
    val last_name: String,
    val email: String?,
    val phone: String,
    val date_of_birth: String?,
    val gender: String?,
    val blood_group: String?,
    val allergies: String?,
    val conditions: String?,
    val appointmentCount: Int,
    val savedHospitalsCount: Int,
    val created_at: String?
)

data class DoctorProfileResponse(
    val id: String,
    val name: String,
    val specialty: String,
    val department_id: String,
    val department_name: String,
    val rating: Float,
    val averageServiceTimeMinutes: Int,
    val isAvailable: Boolean,
    val working_days: String?,
    val consultation_hours: String?,
    val appointment_duration: String?,
    val specialization: String?,
    val qualification: String?,
    val experience: String?,
    val consultation_fee: Double,
    val hospital_name: String?,
    val room_cabin: String?,
    val phone: String?,
    val email: String?
)

data class ReceptionistProfileResponse(
    val id: String,
    val first_name: String,
    val last_name: String,
    val email: String?,
    val phone: String,
    val employee_number: String,
    val designation: String,
    val shift: String,
    val hospital_name: String,
    val department_name: String,
    val working_days: String?,
    val working_hours: String?
)

data class TokenRequest(
    val patientName: String,
    val patientPhone: String,
    val doctorId: String,
    val symptoms: String,
    val isWalkIn: Boolean
)

data class TokenResponse(
    val success: Boolean,
    val tokenId: Long,
    val tokenNumber: String
)

data class ConsultationRequest(
    val diagnosis: String,
    val prescription: String,
    val billAmount: Double
)
