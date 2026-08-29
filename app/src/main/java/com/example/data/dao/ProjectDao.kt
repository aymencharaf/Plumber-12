package com.example.data.dao

import androidx.room.*
import com.example.data.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects")
    suspend fun getAllProjectsDirect(): List<Project>

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjectsDirect()

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<Project?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdDirect(id: Long): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT COUNT(*) FROM projects")
    fun getProjectsCount(): Flow<Int>
}
