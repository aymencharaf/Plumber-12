package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_materials")
data class CustomMaterial(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nameAr: String,
    val nameFr: String = "",
    val category: String = "PPR",
    val defaultSize: String = "25mm",
    val defaultUnit: String = "قطعة",
    val defaultPrice: Double = 0.0,
    val iconType: String = "custom",
    val imageUri: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
