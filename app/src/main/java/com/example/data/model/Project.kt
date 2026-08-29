package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val clientName: String = "",
    val location: String = "",
    val notes: String = "",
    val workTypeKey: String = "CUSTOM",
    val workTypeNameAr: String = "عمل مخصص",
    val workerName: String = "", // اسم العامل / الفني المرسل
    val managerName: String = "", // اسم المدير / المشرف المسند للمهمة
    val storePhone: String = "", // رقم هاتف المحل لتجهيز الطلب
    val orderStatus: String = "NEW", // NEW, SENT_TO_STORE, PREPARING, DISPATCHED, DELIVERED
    val assignedWorkers: String = "", // قائمة المعرفات أو أسماء العمال المسندين للمشروع
    val createdBy: String = "ADMIN", // معرف الإدارة المنشئة
    val status: String = "NEW", // حالة المشروع العامة: NEW, IN_PROGRESS, COMPLETED, CANCELLED
    val laborCost: Double = 0.0, // أجرة اليد العاملة / مصاريف الفني (المال)
    val paidAmount: Double = 0.0, // المبلغ المدفوع تسقيعاً / التسديدات
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getRemainingLaborCost(): Double = (laborCost - paidAmount).coerceAtLeast(0.0)
    fun isLaborFullyPaid(): Boolean = paidAmount >= laborCost && laborCost > 0
    
    fun isAssignedToWorker(workerUidOrPhoneOrName: String): Boolean {
        if (workerUidOrPhoneOrName.isBlank()) return true
        val target = workerUidOrPhoneOrName.trim().lowercase()
        if (workerName.lowercase().contains(target) || target.contains(workerName.lowercase())) return true
        if (assignedWorkers.lowercase().contains(target)) return true
        val workersList = assignedWorkers.split(",").map { it.trim().lowercase() }
        return workersList.any { it.isNotBlank() && (it == target || target.contains(it) || it.contains(target)) }
    }

    fun getOrderStatusAr(): String = when (orderStatus) {
        "SENT_TO_STORE" -> "📤 أُرسل للمحل"
        "PREPARING" -> "📦 قيد التجهيز بالمحل"
        "DISPATCHED" -> "🚚 تم شحنه للموقع"
        "DELIVERED" -> "✅ تم الاستلام بالموقع"
        else -> "📝 طلب جديد (مسودة)"
    }
}
