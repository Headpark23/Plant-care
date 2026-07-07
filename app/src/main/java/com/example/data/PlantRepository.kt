package com.example.data

import kotlinx.coroutines.flow.Flow

class PlantRepository(private val plantDao: PlantDao) {

    val allPlants: Flow<List<Plant>> = plantDao.getAllPlants()
    val allAreas: Flow<List<Area>> = plantDao.getAllAreas()

    fun getPlantById(id: Int): Flow<Plant?> = plantDao.getPlantById(id)

    suspend fun insertPlant(plant: Plant): Long = plantDao.insertPlant(plant)

    suspend fun updatePlant(plant: Plant) = plantDao.updatePlant(plant)

    suspend fun deletePlant(plant: Plant) {
        plantDao.deleteCareLogsForPlant(plant.id)
        plantDao.deletePlant(plant)
    }

    fun getPlantsByArea(areaId: Int): Flow<List<Plant>> = plantDao.getPlantsByArea(areaId)

    suspend fun getAreaById(id: Int): Area? = plantDao.getAreaById(id)

    suspend fun insertArea(area: Area): Long = plantDao.insertArea(area)

    suspend fun deleteArea(area: Area) {
        plantDao.deleteSunlightLogsByArea(area.id)
        // Set areaId of plants in this area to null
        // (Handled manually or database cascading if set up, we will handle it in the VM logic)
        plantDao.deleteArea(area)
    }

    // --- Care Logs ---
    fun getCareLogsForPlant(plantId: Int): Flow<List<CareLog>> = plantDao.getCareLogsForPlant(plantId)

    suspend fun insertCareLog(log: CareLog): Long = plantDao.insertCareLog(log)

    // --- Sunlight Logs ---
    fun getSunlightLogsByArea(areaId: Int): Flow<List<SunlightLog>> = plantDao.getSunlightLogsByArea(areaId)

    suspend fun insertSunlightLog(log: SunlightLog): Long = plantDao.insertSunlightLog(log)
}
