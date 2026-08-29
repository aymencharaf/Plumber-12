package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: String = "",
    val workerName: String = "",
    val action: String = "",
    val projectId: Long = 0,
    val projectName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
