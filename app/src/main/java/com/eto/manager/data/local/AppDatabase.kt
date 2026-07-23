package com.eto.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eto.manager.data.local.dao.DepartmentDao
import com.eto.manager.data.local.dao.DoctorDao
import com.eto.manager.data.local.dao.TokenDao
import com.eto.manager.data.local.entity.DepartmentEntity
import com.eto.manager.data.local.entity.DoctorEntity
import com.eto.manager.data.local.entity.TokenEntity

@Database(
    entities = [DoctorEntity::class, DepartmentEntity::class, TokenEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao
    abstract fun departmentDao(): DepartmentDao
    abstract fun tokenDao(): TokenDao

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
