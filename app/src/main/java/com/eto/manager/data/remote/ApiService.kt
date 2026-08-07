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
}

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
