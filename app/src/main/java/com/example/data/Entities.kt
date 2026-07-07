package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "areas")
data class Area(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sunlightExposure: String, // e.g. "Bright Indirect Light", "Full Sun", etc.
    val notes: String? = null
)

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val species: String,
    val areaId: Int?, // Reference to Area.id
    val wateringIntervalDays: Int,
    val fertilizingIntervalDays: Int,
    val pruningIntervalDays: Int,
    val lastWateredTimestamp: Long? = null,
    val lastFertilizedTimestamp: Long? = null,
    val lastPrunedTimestamp: Long? = null,
    val imageUri: String? = null, // Path to captured image, or stock plant name
    val careTips: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "care_logs")
data class CareLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val careType: String, // "WATER", "FERTILIZE", "PRUNE"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sunlight_logs")
data class SunlightLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val areaId: Int,
    val timeOfDay: String, // e.g., "Morning", "Noon", "Afternoon"
    val intensity: String, // e.g., "Bright", "Partial Shade", "Low Light", "Direct Sun"
    val timestamp: Long = System.currentTimeMillis()
)
