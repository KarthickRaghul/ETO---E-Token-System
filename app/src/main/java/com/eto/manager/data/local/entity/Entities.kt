package com.eto.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String?,
    val phone: String?,
    val role: String,
    val firstName: String,
    val lastName: String,
    val profilePhotoUrl: String?,
    val isActive: Boolean
)

@Entity(tableName = "hospitals")
data class HospitalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val registrationNumber: String,
    val description: String?,
    val phone: String?,
    val email: String?,
    val city: String?,
    val state: String?
)

@Entity(tableName = "departments")
data class DepartmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val hospitalId: String = "e4b77f98-5c1a-4fdf-9737-1234567890ab"
)

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val patientNumber: String,
    val dateOfBirth: String?,
    val gender: String?,
    val bloodGroup: String?
)

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val specialty: String,
    val departmentId: String,
    val departmentName: String,
    val rating: Float,
    val averageServiceTimeMinutes: Int,
    val isAvailable: Boolean,
    val hospitalId: String = "e4b77f98-5c1a-4fdf-9737-1234567890ab"
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val doctorId: String,
    val departmentId: String,
    val appointmentDate: String,
    val appointmentTime: String,
    val appointmentType: String,
    val status: String,
    val symptoms: String?
)

@Entity(tableName = "tokens")
data class TokenEntity(
    @PrimaryKey val id: Long = 0,
    val tokenNumber: String,
    val patientName: String,
    val patientPhone: String,
    val doctorId: String,
    val doctorName: String,
    val departmentName: String,
    val symptoms: String,
    val status: String, // PENDING, APPROVED, SERVING, COMPLETED, SKIPPED
    val queuePosition: Int,
    val estimatedWaitMinutes: Int,
    val createdAt: Long,
    val diagnosis: String? = null,
    val prescription: String? = null,
    val billAmount: Double = 0.0,
    val paymentStatus: String = "PENDING" // PENDING, PAID
)

@Entity(tableName = "queue_entries")
data class QueueEntryEntity(
    @PrimaryKey val id: String,
    val tokenId: String,
    val doctorId: String,
    val position: Int,
    val status: String,
    val joinedAt: String
)

@Entity(tableName = "consultations")
data class ConsultationEntity(
    @PrimaryKey val id: String,
    val tokenId: String,
    val patientId: String,
    val doctorId: String,
    val startedAt: String,
    val completedAt: String?,
    val diagnosis: String?,
    val notes: String?,
    val consultationFee: Double
)

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey val id: String,
    val consultationId: String,
    val patientId: String,
    val doctorId: String,
    val prescriptionNumber: String,
    val instructions: String?
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    val billNumber: String,
    val patientId: String,
    val tokenId: String?,
    val totalAmount: Double,
    val status: String,
    val createdAt: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)
