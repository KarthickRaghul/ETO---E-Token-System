package com.eto.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val specialty: String,
    val departmentId: String,
    val departmentName: String,
    val rating: Float,
    val averageServiceTimeMinutes: Int,
    val isAvailable: Boolean
)

@Entity(tableName = "departments")
data class DepartmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String
)

@Entity(tableName = "tokens")
data class TokenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tokenNumber: String,
    val patientName: String,
    val patientPhone: String,
    val doctorId: String,
    val doctorName: String,
    val departmentName: String,
    val symptoms: String,
    val status: String, // PENDING, SERVING, COMPLETED, SKIPPED
    val queuePosition: Int,
    val estimatedWaitMinutes: Int,
    val createdAt: Long,
    val diagnosis: String? = null,
    val prescription: String? = null,
    val billAmount: Double = 0.0,
    val paymentStatus: String = "PENDING" // PENDING, PAID
)
