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

fun Token.getDisplayQueueNumber(allTokens: List<Token>): String {
    if (this.status == TokenStatus.COMPLETED || this.status == TokenStatus.SKIPPED) {
        val digitsOnly = this.tokenNumber.filter { it.isDigit() }
        return if (digitsOnly.isNotEmpty()) digitsOnly else this.id.toString()
    }
    
    val doctorTokens = allTokens.filter { 
        it.doctorId == this.doctorId && 
        it.status != TokenStatus.COMPLETED && 
        it.status != TokenStatus.SKIPPED 
    }
    
    val servingToken = doctorTokens.find { it.status == TokenStatus.SERVING }
    val approvedTokens = doctorTokens.filter { it.status == TokenStatus.APPROVED }.sortedBy { it.id }
    val pendingTokens = doctorTokens.filter { it.status == TokenStatus.PENDING }.sortedBy { it.id }
    
    return when (this.status) {
        TokenStatus.SERVING -> "1"
        TokenStatus.APPROVED -> {
            val index = approvedTokens.indexOfFirst { it.id == this.id }
            val servingOffset = if (servingToken != null) 1 else 0
            if (index >= 0) (index + 1 + servingOffset).toString() else "1"
        }
        TokenStatus.PENDING -> {
            val index = pendingTokens.indexOfFirst { it.id == this.id }
            val servingOffset = if (servingToken != null) 1 else 0
            if (index >= 0) (index + 1 + servingOffset + approvedTokens.size).toString() else (approvedTokens.size + servingOffset + 1).toString()
        }
        else -> {
            val digitsOnly = this.tokenNumber.filter { it.isDigit() }
            if (digitsOnly.isNotEmpty()) digitsOnly else this.id.toString()
        }
    }
}
