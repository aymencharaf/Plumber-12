package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.math.ceil

@Entity(
    tableName = "project_items",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"])
    ]
)
data class ProjectItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val projectId: Long,

    val materialKey: String,

    val materialNameAr: String,

    val materialNameFr: String = "",

    val category: String = "CUSTOM",

    val size: String = "",

    val quantity: Double = 1.0,

    val unit: String = "قطعة",

    val unitPrice: Double = 0.0,

    val standardPipeLengthMeters: Double = 4.0,

    val isPurchased: Boolean = false,

    val notes: String = "",

    val iconType: String = "elbow",

    val imageUri: String? = null,

    val createdAt: Long = System.currentTimeMillis()
) {
    fun getPipesCount(): Int? {
        return if (
            unit == "متر" &&
            standardPipeLengthMeters > 0
        ) {
            ceil(
                quantity / standardPipeLengthMeters
            ).toInt()
        } else {
            null
        }
    }

    fun getTotalPrice(): Double {
        return quantity * unitPrice
    }

    fun getTotalPriceRounded(): Double {
        return (quantity * unitPrice * 100.0).toLong() / 100.0
    }
}
