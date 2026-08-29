package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.MaterialDao
import com.example.data.model.MaterialEntity

@Database(
    entities = [MaterialEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MaterialDatabase : RoomDatabase() {

    abstract fun materialDao(): MaterialDao

    companion object {
        @Volatile
        private var INSTANCE: MaterialDatabase? = null

        fun getDatabase(context: Context): MaterialDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MaterialDatabase::class.java,
                    "materials_catalog_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
