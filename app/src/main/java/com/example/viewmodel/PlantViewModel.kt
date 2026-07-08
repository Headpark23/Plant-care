package com.example.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.MainActivity
import com.example.api.GeminiClient
import com.example.api.PlantAnalysisResult
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AiScanState {
    object Idle : AiScanState
    object Loading : AiScanState
    data class Success(val result: PlantAnalysisResult) : AiScanState
    data class Error(val message: String) : AiScanState
}

class PlantViewModel(private val repository: PlantRepository) : ViewModel() {

    // --- State Streams ---
    val plants: StateFlow<List<Plant>> = repository.allPlants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val areas: StateFlow<List<Area>> = repository.allAreas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiScanState = MutableStateFlow<AiScanState>(AiScanState.Idle)
    val aiScanState: StateFlow<AiScanState> = _aiScanState.asStateFlow()

    // --- Active states for specific screens ---
    private val _selectedPlant = MutableStateFlow<Plant?>(null)
    val selectedPlant: StateFlow<Plant?> = _selectedPlant.asStateFlow()

    private val _selectedArea = MutableStateFlow<Area?>(null)
    val selectedArea: StateFlow<Area?> = _selectedArea.asStateFlow()

    private val _systemNotificationLogs = MutableStateFlow<List<String>>(emptyList())
    val systemNotificationLogs: StateFlow<List<String>> = _systemNotificationLogs.asStateFlow()

    // --- Helper lists of Care Alerts ---
    val careAlerts: StateFlow<List<CareAlert>> = plants.map { plantList ->
        val alerts = mutableListOf<CareAlert>()
        val now = System.currentTimeMillis()
        plantList.forEach { plant ->
            val daysWater = getDaysRemaining(plant.lastWateredTimestamp ?: plant.addedTimestamp, plant.wateringIntervalDays)
            val daysFertilize = getDaysRemaining(plant.lastFertilizedTimestamp ?: plant.addedTimestamp, plant.fertilizingIntervalDays)
            val daysPrune = getDaysRemaining(plant.lastPrunedTimestamp ?: plant.addedTimestamp, plant.pruningIntervalDays)

            if (daysWater <= 0) {
                alerts.add(CareAlert(plant, "Water", "Watering is overdue by ${-daysWater} days."))
            }
            if (daysFertilize <= 0) {
                alerts.add(CareAlert(plant, "Fertilize", "Fertilizing is overdue by ${-daysFertilize} days."))
            }
            if (daysPrune <= 0) {
                alerts.add(CareAlert(plant, "Prune", "Pruning is overdue by ${-daysPrune} days."))
            }
        }
        alerts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Create initial areas if empty, to give the user a rich initial experience!
        viewModelScope.launch {
            repository.allAreas.first().let { currentAreas ->
                if (currentAreas.isEmpty()) {
                    insertArea("Living Room", "Bright Indirect Light", "Receives moderate afternoon light from the west window.")
                    insertArea("East Balcony", "Full Sun", "Flooded with intense morning direct sunlight.")
                    insertArea("Bedroom Corner", "Low Light", "Minimal ambient light, shaded spots.")
                    insertArea("Kitchen Windowsill", "Partial Shade", "Gentle morning sun with dappled shade.")
                }
            }
            // Create some default plants to make the dashboard look gorgeous on first load!
            repository.allPlants.first().let { currentPlants ->
                if (currentPlants.isEmpty()) {
                    // Find Living Room and East Balcony area IDs to link
                    val updatedAreas = repository.allAreas.first()
                    val livingRoomId = updatedAreas.find { it.name == "Living Room" }?.id
                    val balconyId = updatedAreas.find { it.name == "East Balcony" }?.id
                    val kitchenId = updatedAreas.find { it.name == "Kitchen Windowsill" }?.id

                    insertPlant(
                        name = "Fiddle Leaf Fig",
                        species = "Ficus lyrata",
                        areaId = livingRoomId,
                        wateringIntervalDays = 7,
                        fertilizingIntervalDays = 30,
                        pruningIntervalDays = 90,
                        lastWateredTimestamp = System.currentTimeMillis() - (4 * 24 * 60 * 60 * 1000L), // watered 4 days ago
                        lastFertilizedTimestamp = System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000L), // fertilized 15 days ago
                        lastPrunedTimestamp = System.currentTimeMillis() - (45 * 24 * 60 * 60 * 1000L),
                        careTips = "Keep away from cold drafts;Rotate 90 degrees every month;Wipe leaves with a damp cloth to remove dust."
                    )

                    insertPlant(
                        name = "Sweet Basil",
                        species = "Ocimum basilicum",
                        areaId = balconyId,
                        wateringIntervalDays = 2,
                        fertilizingIntervalDays = 14,
                        pruningIntervalDays = 15,
                        // This plant is overdue for watering & pruning to highlight notifications!
                        lastWateredTimestamp = System.currentTimeMillis() - (4 * 24 * 60 * 60 * 1000L), // watered 4 days ago (interval 2) -> due!
                        lastFertilizedTimestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L),
                        lastPrunedTimestamp = System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L), // due!
                        careTips = "Pinch off flower spikes to encourage bushier growth;Water generously whenever soil top-inch is dry;Needs 6+ hours of sun."
                    )

                    insertPlant(
                        name = "Golden Pothos",
                        species = "Epipremnum aureum",
                        areaId = kitchenId,
                        wateringIntervalDays = 10,
                        fertilizingIntervalDays = 45,
                        pruningIntervalDays = 60,
                        lastWateredTimestamp = System.currentTimeMillis() - (12 * 24 * 60 * 60 * 1000L), // Overdue!
                        lastFertilizedTimestamp = System.currentTimeMillis() - (50 * 24 * 60 * 60 * 1000L), // Overdue!
                        lastPrunedTimestamp = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L),
                        careTips = "Extremely resilient;Vine cuttings can easily root in water;Can survive in low light but variegation pops in bright spots."
                    )
                }
            }
        }
    }

    // --- Plant Actions ---

    fun selectPlant(plant: Plant?) {
        _selectedPlant.value = plant
    }

    fun selectArea(area: Area?) {
        _selectedArea.value = area
    }

    fun insertPlant(
        name: String,
        species: String,
        areaId: Int?,
        wateringIntervalDays: Int,
        fertilizingIntervalDays: Int,
        pruningIntervalDays: Int,
        lastWateredTimestamp: Long? = System.currentTimeMillis(),
        lastFertilizedTimestamp: Long? = System.currentTimeMillis(),
        lastPrunedTimestamp: Long? = System.currentTimeMillis(),
        imageUri: String? = null,
        careTips: String? = null
    ) {
        viewModelScope.launch {
            val plant = Plant(
                name = name,
                species = species,
                areaId = areaId,
                wateringIntervalDays = wateringIntervalDays,
                fertilizingIntervalDays = fertilizingIntervalDays,
                pruningIntervalDays = pruningIntervalDays,
                lastWateredTimestamp = lastWateredTimestamp,
                lastFertilizedTimestamp = lastFertilizedTimestamp,
                lastPrunedTimestamp = lastPrunedTimestamp,
                imageUri = imageUri,
                careTips = careTips
            )
            repository.insertPlant(plant)
        }
    }

    fun updatePlant(plant: Plant) {
        viewModelScope.launch {
            repository.updatePlant(plant)
            if (_selectedPlant.value?.id == plant.id) {
                _selectedPlant.value = plant
            }
        }
    }

    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            repository.deletePlant(plant)
            if (_selectedPlant.value?.id == plant.id) {
                _selectedPlant.value = null
            }
        }
    }

    fun recordCare(plant: Plant, careType: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updatedPlant = when (careType) {
                "WATER" -> plant.copy(lastWateredTimestamp = now)
                "FERTILIZE" -> plant.copy(lastFertilizedTimestamp = now)
                "PRUNE" -> plant.copy(lastPrunedTimestamp = now)
                else -> plant
            }
            repository.updatePlant(updatedPlant)
            repository.insertCareLog(CareLog(plantId = plant.id, careType = careType, timestamp = now))
            
            // Sync selection state if active
            if (_selectedPlant.value?.id == plant.id) {
                _selectedPlant.value = updatedPlant
            }
        }
    }

    fun getCareLogsForPlant(plantId: Int): Flow<List<CareLog>> {
        return repository.getCareLogsForPlant(plantId)
    }

    // --- Area Actions ---

    fun insertArea(name: String, sunlightExposure: String, notes: String? = null) {
        viewModelScope.launch {
            repository.insertArea(Area(name = name, sunlightExposure = sunlightExposure, notes = notes))
        }
    }

    fun deleteArea(area: Area) {
        viewModelScope.launch {
            // Unlink plants in this area
            val plantsInArea = plants.value.filter { it.areaId == area.id }
            plantsInArea.forEach { plant ->
                repository.updatePlant(plant.copy(areaId = null))
            }
            repository.deleteArea(area)
            if (_selectedArea.value?.id == area.id) {
                _selectedArea.value = null
            }
        }
    }

    // --- Sunlight Tracker Actions ---

    fun addSunlightLog(areaId: Int, timeOfDay: String, intensity: String) {
        viewModelScope.launch {
            repository.insertSunlightLog(
                SunlightLog(areaId = areaId, timeOfDay = timeOfDay, intensity = intensity)
            )
        }
    }

    fun getSunlightLogsForArea(areaId: Int): Flow<List<SunlightLog>> {
        return repository.getSunlightLogsByArea(areaId)
    }

    // --- Gemini AI Analysis ---

    fun setAiScanState(state: AiScanState) {
        _aiScanState.value = state
    }

    fun clearAiScan() {
        _aiScanState.value = AiScanState.Idle
    }

    fun identifyPlantWithAI(bitmap: Bitmap) {
        viewModelScope.launch {
            _aiScanState.value = AiScanState.Loading
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                _aiScanState.value = AiScanState.Error("Gemini API Key is not set in Secrets. Please configure your GEMINI_API_KEY inside AI Studio's Secrets panel.")
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    GeminiClient.analyzePlantImage(bitmap, apiKey)
                }
                if (result != null) {
                    _aiScanState.value = AiScanState.Success(result)
                } else {
                    _aiScanState.value = AiScanState.Error("Unable to identify species. Please try with a clearer photo.")
                }
            } catch (e: Exception) {
                _aiScanState.value = AiScanState.Error("API Error: ${e.localizedMessage ?: "Failed to connect to Gemini API"}")
            }
        }
    }

    // --- System Notifications Triggering (Push Notification Simulation) ---

    fun triggerLocalCareNotifications(context: Context) {
        viewModelScope.launch {
            val overduePlants = plants.value.filter { plant ->
                val waterOverdue = getDaysRemaining(plant.lastWateredTimestamp ?: plant.addedTimestamp, plant.wateringIntervalDays) <= 0
                val fertilizeOverdue = getDaysRemaining(plant.lastFertilizedTimestamp ?: plant.addedTimestamp, plant.fertilizingIntervalDays) <= 0
                val pruneOverdue = getDaysRemaining(plant.lastPrunedTimestamp ?: plant.addedTimestamp, plant.pruningIntervalDays) <= 0
                waterOverdue || fertilizeOverdue || pruneOverdue
            }

            if (overduePlants.isEmpty()) {
                _systemNotificationLogs.value = listOf("All your plants are healthy! No notifications to trigger right now.")
                return@launch
            }

            val logsList = mutableListOf<String>()
            overduePlants.forEachIndexed { index, plant ->
                val waterOverdue = getDaysRemaining(plant.lastWateredTimestamp ?: plant.addedTimestamp, plant.wateringIntervalDays) <= 0
                val fertilizeOverdue = getDaysRemaining(plant.lastFertilizedTimestamp ?: plant.addedTimestamp, plant.fertilizingIntervalDays) <= 0
                val pruneOverdue = getDaysRemaining(plant.lastPrunedTimestamp ?: plant.addedTimestamp, plant.pruningIntervalDays) <= 0

                val dueTasks = mutableListOf<String>()
                if (waterOverdue) dueTasks.add("water 💧")
                if (fertilizeOverdue) dueTasks.add("fertilize 🌱")
                if (pruneOverdue) dueTasks.add("prune ✂️")

                val title = "FloraCare Reminder: ${plant.name}"
                val content = "It's time to ${dueTasks.joinToString(" and ")} your ${plant.species}!"

                sendSystemNotification(context, index, title, content)
                logsList.add("Notification sent for '${plant.name}': \"$content\"")
            }
            _systemNotificationLogs.value = logsList
        }
    }

    private fun sendSystemNotification(context: Context, notificationId: Int, title: String, content: String) {
        val channelId = "floracare_notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FloraCare Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when plants are due for care"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback standard icon
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // --- State Calculation Helpers ---

    fun getDaysRemaining(lastTimestamp: Long?, intervalDays: Int): Int {
        if (lastTimestamp == null) return 0
        val elapsedMs = System.currentTimeMillis() - lastTimestamp
        val elapsedDays = (elapsedMs / (1000 * 60 * 60 * 24L)).toInt()
        return intervalDays - elapsedDays
    }
}

data class CareAlert(
    val plant: Plant,
    val type: String, // "Water", "Fertilize", "Prune"
    val description: String
)

class PlantViewModelFactory(private val repository: PlantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
