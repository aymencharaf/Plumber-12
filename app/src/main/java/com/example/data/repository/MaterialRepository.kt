package com.example.data.repository

import android.content.Context
import com.example.data.dao.MaterialDao
import com.example.data.model.MaterialEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MaterialRepository(private val materialDao: MaterialDao) {

    val allMaterials: Flow<List<MaterialEntity>> = materialDao.getAllMaterials()

    suspend fun initializeLibraryIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        if (materialDao.getMaterialCount() == 0) {
            importMaterialsFromAssets(context)
        }
    }

    suspend fun importMaterialsFromAssets(context: Context) = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("materials.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<MaterialEntity>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val cat = obj.optString("category", "PPR")
                val sizeVal = obj.optString("size", "25mm")
                val rawPrice = obj.optDouble("price", 0.0)
                val priceVal = if (rawPrice > 0) rawPrice else calculateDefaultPrice(cat, sizeVal)

                val item = MaterialEntity(
                    id = obj.optString("id", "mat_$i"),
                    nameAr = obj.optString("name_ar", obj.optString("nameAr", "مادة سباكة")),
                    nameFr = obj.optString("name_fr", obj.optString("nameFr", "")),
                    code = obj.optString("code", "PPR-${i+1}"),
                    category = cat,
                    size = sizeVal,
                    unit = obj.optString("unit", "قطعة"),
                    price = priceVal,
                    image = obj.optString("image", ""),
                    iconType = getIconTypeForCategory(cat),
                    isCustom = false
                )
                list.add(item)
            }

            if (list.isNotEmpty()) {
                materialDao.insertInitialMaterials(list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateDefaultPrice(category: String, size: String): Double {
        val base = when (category.uppercase()) {
            "PPR" -> 180.0
            "PVC" -> 350.0
            "PEHD" -> 450.0
            "ROBINETTERIE", "صمامات" -> 1400.0
            "ACCESSOIRES", "أدوات" -> 280.0
            else -> 250.0
        }
        val sizeMultiplier = when {
            size.contains("110") || size.contains("90") -> 3.0
            size.contains("63") || size.contains("50") -> 2.2
            size.contains("40") || size.contains("32") -> 1.5
            else -> 1.0
        }
        return base * sizeMultiplier
    }

    private fun getIconTypeForCategory(category: String): String {
        return when (category.uppercase()) {
            "PPR" -> "elbow"
            "PVC" -> "pipe"
            "PEHD" -> "pipe"
            "ROBINETTERIE" -> "valve"
            "ACCESSOIRES" -> "fitting"
            else -> "fitting"
        }
    }

    suspend fun insertMaterial(material: MaterialEntity) {
        materialDao.insertMaterial(material)
    }

    suspend fun updateMaterial(material: MaterialEntity) {
        materialDao.updateMaterial(material)
    }

    suspend fun deleteMaterial(id: String) {
        materialDao.deleteMaterialById(id)
    }
}
