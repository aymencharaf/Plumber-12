package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serviceType: String, // فحص, صيانة, تركيب, أعمال أخرى
    val customerName: String,
    val phoneNumber: String,
    val address: String,
    val scheduledTime: String,
    val notes: String = "",
    val status: String = "قيد الانتظار", // قيد الانتظار, تم الإنجاز, ملغى
    val createdAt: Long = System.currentTimeMillis()
)
