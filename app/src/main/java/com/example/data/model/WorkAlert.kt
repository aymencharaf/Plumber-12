package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_alerts")
data class WorkAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alertType: String, // طلبية, تحديد موعد, إرسال لوازم, تأكيد استلام
    val senderName: String, // اسم العامل أو المدير
    val recipientRole: String, // المدير أو العامل
    val title: String,
    val details: String,
    val projectName: String = "",
    val status: String = "جديد", // جديد, تمت المعاينة, تم الاستلام/الموافقة
    val timestamp: Long = System.currentTimeMillis()
)
