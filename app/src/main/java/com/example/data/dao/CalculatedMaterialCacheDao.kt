package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CalculatedMaterialCache
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculatedMaterialCacheDao {
    @Query("SELECT * FROM calculated_materials_cache ORDER BY timestamp DESC")
    fun getAllCalculatedCaches(): Flow<List<CalculatedMaterialCache>>

    @Query("SELECT * FROM calculated_materials_cache ORDER BY timestamp DESC")
    suspend fun getAllCalculatedCachesDirect(): List<CalculatedMaterialCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: CalculatedMaterialCache): Long

    @Query("DELETE FROM calculated_materials_cache WHERE id = :id")
    suspend fun deleteCacheById(id: Long)

    @Query("DELETE FROM calculated_materials_cache")
    suspend fun clearAllCaches()
}
