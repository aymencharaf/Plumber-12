package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorkAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkAlertDao {
    @Query("SELECT * FROM work_alerts ORDER BY id DESC")
    fun getAllAlerts(): Flow<List<WorkAlert>>

    @Query("SELECT * FROM work_alerts")
    suspend fun getAllWorkAlertsDirect(): List<WorkAlert>

    @Query("DELETE FROM work_alerts")
    suspend fun deleteAllWorkAlertsDirect()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: WorkAlert): Long

    @Update
    suspend fun updateAlert(alert: WorkAlert)

    @Delete
    suspend fun deleteAlert(alert: WorkAlert)

    @Query("DELETE FROM work_alerts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
