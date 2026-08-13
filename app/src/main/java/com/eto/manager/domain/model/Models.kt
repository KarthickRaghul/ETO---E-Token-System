package com.eto.manager.domain.model

data class Doctor(
    val id: String,
    val name: String,
    val specialty: String,
    val departmentId: String,
    val departmentName: String,
    val rating: Float,
    val averageServiceTimeMinutes: Int,
    val isAvailable: Boolean,
    val hospitalId: String
)

data class Department(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String
)

data class Hospital(
    val id: String,
    val name: String,
    val registrationNumber: String,
    val description: String?,
    val phone: String?,
    val email: String?,
    val city: String?,
    val state: String?,
    val latitude: Double,
    val longitude: Double
)

data class Token(
    val id: Long,
    val tokenNumber: String,
    val patientName: String,
    val patientPhone: String,
    val doctorId: String,
    val doctorName: String,
    val departmentName: String,
    val symptoms: String,
    val status: TokenStatus,
    val queuePosition: Int,
    val estimatedWaitMinutes: Int,
    val createdAt: Long,
    val diagnosis: String? = null,
    val prescription: String? = null,
    val billAmount: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val hospitalName: String
)

enum class TokenStatus {
    PENDING,
    APPROVED,
    SERVING,
    COMPLETED,
    SKIPPED
}

enum class PaymentStatus {
    PENDING,
    PAID
}
