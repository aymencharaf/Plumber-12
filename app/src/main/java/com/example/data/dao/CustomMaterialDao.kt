package com.example.data.dao

import androidx.room.*
import com.example.data.model.CustomMaterial
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomMaterialDao {
    @Query("SELECT * FROM custom_materials ORDER BY id DESC")
    fun getAllCustomMaterials(): Flow<List<CustomMaterial>>

    @Query("SELECT * FROM custom_materials")
    suspend fun getAllCustomMaterialsDirect(): List<CustomMaterial>

    @Query("DELETE FROM custom_materials")
    suspend fun deleteAllCustomMaterialsDirect()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomMaterial(material: CustomMaterial): Long

    @Query("DELETE FROM custom_materials WHERE id = :id")
    suspend fun deleteCustomMaterial(id: Long)
}
