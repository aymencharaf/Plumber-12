package com.example.data.model

data class PlumbingMaterial(
    val key: String,
    val nameAr: String,
    val nameFr: String,
    val category: String, // PPR, PVC, ROBINETTERIE, SANITAIRE, ACCESSOIRES
    val availableSizes: List<String>,
    val defaultSize: String,
    val defaultUnit: String, // قطعة, متر, علبة, كيس, رول, مجموعة, وحدة
    val defaultUnitPrice: Double = 0.0,
    val iconType: String = "fitting",
    val imageUri: String? = null,
    val descriptionAr: String = "",
    val isPipe: Boolean = false
)

data class WorkType(
    val key: String,
    val titleAr: String,
    val titleFr: String,
    val iconEmoji: String,
    val descriptionAr: String,
    val suggestedMaterialKeys: List<String>
)
