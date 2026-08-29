package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings_cache")
data class AppSettingCache(
    @PrimaryKey val settingKey: String,
    val settingValue: String,
    val updatedAt: Long = System.currentTimeMillis()
)
