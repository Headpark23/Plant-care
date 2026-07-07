package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    
    // --- Plants ---
    @Query("SELECT * FROM plants ORDER BY addedTimestamp DESC")
    fun getAllPlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :id LIMIT 1")
    fun getPlantById(id: Int): Flow<Plant?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: Plant): Long

    @Update
    suspend fun updatePlant(plant: Plant)

    @Delete
    suspend fun deletePlant(plant: Plant)

    @Query("SELECT * FROM plants WHERE areaId = :areaId")
    fun getPlantsByArea(areaId: Int): Flow<List<Plant>>

    // --- Areas ---
    @Query("SELECT * FROM areas ORDER BY name ASC")
    fun getAllAreas(): Flow<List<Area>>

    @Query("SELECT * FROM areas WHERE id = :id LIMIT 1")
    suspend fun getAreaById(id: Int): Area?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: Area): Long

    @Delete
    suspend fun deleteArea(area: Area)

    // --- Care Logs ---
    @Query("SELECT * FROM care_logs WHERE plantId = :plantId ORDER BY timestamp DESC")
    fun getCareLogsForPlant(plantId: Int): Flow<List<CareLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCareLog(log: CareLog): Long

    @Query("DELETE FROM care_logs WHERE plantId = :plantId")
    suspend fun deleteCareLogsForPlant(plantId: Int)

    // --- Sunlight Logs ---
    @Query("SELECT * FROM sunlight_logs WHERE areaId = :areaId ORDER BY timestamp DESC")
    fun getSunlightLogsByArea(areaId: Int): Flow<List<SunlightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSunlightLog(log: SunlightLog): Long

    @Query("DELETE FROM sunlight_logs WHERE areaId = :areaId")
    suspend fun deleteSunlightLogsByArea(areaId: Int)
}
