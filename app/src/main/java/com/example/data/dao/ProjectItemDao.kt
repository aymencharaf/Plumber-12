package com.example.data.dao

import androidx.room.*
import com.example.data.model.ProjectItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectItemDao {
    @Query("SELECT * FROM project_items WHERE projectId = :projectId ORDER BY id ASC")
    fun getItemsForProject(projectId: Long): Flow<List<ProjectItem>>

    @Query("SELECT * FROM project_items")
    suspend fun getAllProjectItemsDirect(): List<ProjectItem>

    @Query("DELETE FROM project_items")
    suspend fun deleteAllProjectItemsDirect()

    @Query("SELECT * FROM project_items WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun getItemsForProjectDirect(projectId: Long): List<ProjectItem>

    @Query("SELECT * FROM project_items WHERE projectId = :projectId AND materialKey = :materialKey AND size = :size LIMIT 1")
    suspend fun getItemByKeyAndSize(projectId: Long, materialKey: String, size: String): ProjectItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ProjectItem): Long

    @Update
    suspend fun updateItem(item: ProjectItem)

    @Query("DELETE FROM project_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM project_items WHERE projectId = :projectId")
    suspend fun deleteAllItemsForProject(projectId: Long)

    @Query("UPDATE project_items SET isPurchased = :isPurchased WHERE id = :itemId")
    suspend fun updatePurchaseState(itemId: Long, isPurchased: Boolean)
}
