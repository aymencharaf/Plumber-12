package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey
    val id: String,
    val nameAr: String,
    val nameFr: String = "",
    val code: String = "",
    val category: String = "PPR", // PPR, PVC, PEHD, Robinetterie, Accessoires
    val size: String = "25mm",
    val unit: String = "قطعة",
    val price: Double = 0.0,
    val image: String = "", // Local URI or File path or empty string for placeholder
    val iconType: String = "fitting",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
