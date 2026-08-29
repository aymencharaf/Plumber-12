package com.example.data.dao

import androidx.room.*
import com.example.data.model.AuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE workerId = :workerId ORDER BY timestamp DESC")
    fun getAuditLogsByWorker(workerId: String): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getAuditLogsByProject(projectId: Long): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)
}
