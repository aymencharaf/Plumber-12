package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AppSettingCacheDao
import com.example.data.dao.AppointmentDao
import com.example.data.dao.AuditLogDao
import com.example.data.dao.CalculatedMaterialCacheDao
import com.example.data.dao.CustomMaterialDao
import com.example.data.dao.MaterialDao
import com.example.data.dao.ProjectDao
import com.example.data.dao.ProjectItemDao
import com.example.data.dao.TeamUserDao
import com.example.data.dao.WorkAlertDao
import com.example.data.model.AppSettingCache
import com.example.data.model.Appointment
import com.example.data.model.AuditLog
import com.example.data.model.CalculatedMaterialCache
import com.example.data.model.CustomMaterial
import com.example.data.model.MaterialEntity
import com.example.data.model.Project
import com.example.data.model.ProjectItem
import com.example.data.model.TeamUser
import com.example.data.model.WorkAlert

@Database(
    entities = [Project::class, ProjectItem::class, CustomMaterial::class, MaterialEntity::class, Appointment::class, WorkAlert::class, TeamUser::class, AuditLog::class, CalculatedMaterialCache::class, AppSettingCache::class],
    version = 10,
    exportSchema = false
)
abstract class PlumberDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun projectItemDao(): ProjectItemDao
    abstract fun customMaterialDao(): CustomMaterialDao
    abstract fun materialDao(): MaterialDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun workAlertDao(): WorkAlertDao
    abstract fun teamUserDao(): TeamUserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun calculatedMaterialCacheDao(): CalculatedMaterialCacheDao
    abstract fun appSettingCacheDao(): AppSettingCacheDao

    companion object {
        @Volatile
        private var INSTANCE: PlumberDatabase? = null

        fun getDatabase(context: Context): PlumberDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlumberDatabase::class.java,
                    "plumber_app_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
