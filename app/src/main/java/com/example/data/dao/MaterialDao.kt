package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {

    @Query("SELECT * FROM materials ORDER BY category ASC, nameAr ASC")
    fun getAllMaterials(): Flow<List<MaterialEntity>>

    @Query("SELECT COUNT(*) FROM materials")
    suspend fun getMaterialCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialMaterials(materials: List<MaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: MaterialEntity)

    @Update
    suspend fun updateMaterial(material: MaterialEntity)

    @Delete
    suspend fun deleteMaterial(material: MaterialEntity)

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun deleteMaterialById(id: String)
}
