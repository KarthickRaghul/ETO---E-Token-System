package com.eto.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eto.manager.data.local.dao.*
import com.eto.manager.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        HospitalEntity::class,
        DepartmentEntity::class,
        PatientEntity::class,
        DoctorEntity::class,
        AppointmentEntity::class,
        TokenEntity::class,
        QueueEntryEntity::class,
        ConsultationEntity::class,
        PrescriptionEntity::class,
        BillEntity::class,
        NotificationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao
    abstract fun departmentDao(): DepartmentDao
    abstract fun tokenDao(): TokenDao
    abstract fun userDao(): UserDao
    abstract fun hospitalDao(): HospitalDao
    abstract fun patientDao(): PatientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun queueEntryDao(): QueueEntryDao
    abstract fun consultationDao(): ConsultationDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun billDao(): BillDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eto_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
