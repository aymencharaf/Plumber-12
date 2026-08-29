package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppSettingCache
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingCacheDao {
    @Query("SELECT * FROM app_settings_cache WHERE settingKey = :key")
    suspend fun getSettingByKey(key: String): AppSettingCache?

    @Query("SELECT * FROM app_settings_cache")
    fun getAllSettings(): Flow<List<AppSettingCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetting(setting: AppSettingCache)

    @Query("DELETE FROM app_settings_cache WHERE settingKey = :key")
    suspend fun deleteSetting(key: String)
}
