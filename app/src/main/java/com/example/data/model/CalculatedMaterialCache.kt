package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculated_materials_cache")
data class CalculatedMaterialCache(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val calculationType: String, // "PIPE_LENGTH", "AREA_ESTIMATE", "WATER_POINTS", "HYDRAULIC_FLOW"
    val pipeCategory: String, // "PPR", "PVC", "PEX", "BRASS", "GENERAL"
    val size: String,
    val inputValuesSummary: String,
    val estimatedPipesCount: Double,
    val estimatedFittingsCount: Int,
    val estimatedGlueOrSolder: String,
    val generatedMaterialsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
