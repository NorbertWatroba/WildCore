package com.wildcore.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "survival_spots")
data class SurvivalSpot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val category: String, // np. "Woda", "Schronienie", "Zasoby"
    val isDiscovered: Boolean = true
)