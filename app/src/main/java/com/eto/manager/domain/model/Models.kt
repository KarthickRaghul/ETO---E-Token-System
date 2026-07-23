package com.eto.manager.domain.model

data class Doctor(
    val id: String,
    val name: String,
    val specialty: String,
    val departmentId: String,
    val departmentName: String,
    val rating: Float,
    val averageServiceTimeMinutes: Int,
    val isAvailable: Boolean
)

data class Department(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String
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
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING
)

enum class TokenStatus {
    PENDING,
    SERVING,
    COMPLETED,
    SKIPPED
}

enum class PaymentStatus {
    PENDING,
    PAID
}
