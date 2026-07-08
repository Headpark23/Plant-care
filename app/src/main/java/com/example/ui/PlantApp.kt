package com.example.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.PlantAnalysisResult
import com.example.data.Area
import com.example.data.CareLog
import com.example.data.Plant
import com.example.data.SunlightLog
import com.example.viewmodel.AiScanState
import com.example.viewmodel.PlantViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

// --- Navigation Screens ---
sealed interface Screen {
    object Dashboard : Screen
    data class PlantDetail(val plantId: Int) : Screen
    object AddPlant : Screen
    data class AreaDetail(val areaId: Int) : Screen
}

// --- Dynamic Canvas Botanical Drawings ---
@Composable
fun BotanicalIcon(species: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2
        val centerY = h / 2

        // Determine drawing based on name/species
        val lower = species.lowercase()
        when {
            lower.contains("monstera") || lower.contains("fig") -> {
                // Large perforated Leaf (Monstera-style)
                val stemPath = Path().apply {
                    moveTo(centerX, h * 0.9f)
                    quadraticTo(centerX * 1.05f, h * 0.5f, centerX, h * 0.15f)
                }
                drawPath(stemPath, Color(0xFF2E7D32), style = Stroke(width = 4f))

                val leafPath = Path().apply {
                    moveTo(centerX, h * 0.15f)
                    cubicTo(centerX - w * 0.45f, centerY - h * 0.3f, centerX - w * 0.35f, h * 0.8f, centerX, h * 0.85f)
                    cubicTo(centerX + w * 0.35f, h * 0.8f, centerX + w * 0.45f, centerY - h * 0.3f, centerX, h * 0.15f)
                }
                drawPath(leafPath, Color(0xFF4CAF50))

                // Midrib and slits
                drawPath(stemPath, Color(0xFF1B5E20), style = Stroke(width = 2.5f))
                
                // Leaf slits/veins
                drawLine(Color(0xFF1B5E20), Offset(centerX, centerY - h * 0.1f), Offset(centerX - w * 0.25f, centerY - h * 0.2f), strokeWidth = 3f)
                drawLine(Color(0xFF1B5E20), Offset(centerX, centerY - h * 0.1f), Offset(centerX + w * 0.25f, centerY - h * 0.2f), strokeWidth = 3f)
                drawLine(Color(0xFF1B5E20), Offset(centerX, centerY + h * 0.1f), Offset(centerX - w * 0.3f, centerY), strokeWidth = 3f)
                drawLine(Color(0xFF1B5E20), Offset(centerX, centerY + h * 0.1f), Offset(centerX + w * 0.3f, centerY), strokeWidth = 3f)
            }
            lower.contains("cactus") || lower.contains("succulent") -> {
                // Cute Cactus
                // Main body
                drawRoundRect(
                    color = Color(0xFF2E7D32),
                    topLeft = Offset(centerX - w * 0.15f, centerY - h * 0.3f),
                    size = Size(w * 0.3f, h * 0.55f),
                    cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
                )
                // Left arm
                val leftArm = Path().apply {
                    moveTo(centerX - w * 0.12f, centerY + h * 0.05f)
                    lineTo(centerX - w * 0.3f, centerY + h * 0.05f)
                    quadraticTo(centerX - w * 0.35f, centerY - h * 0.15f, centerX - w * 0.3f, centerY - h * 0.15f)
                }
                drawPath(leftArm, Color(0xFF1B5E20), style = Stroke(width = 12f))

                // Right arm
                val rightArm = Path().apply {
                    moveTo(centerX + w * 0.12f, centerY - h * 0.05f)
                    lineTo(centerX + w * 0.3f, centerY - h * 0.05f)
                    quadraticTo(centerX + w * 0.35f, centerY - h * 0.25f, centerX + w * 0.3f, centerY - h * 0.25f)
                }
                drawPath(rightArm, Color(0xFF1B5E20), style = Stroke(width = 12f))

                // Pot
                val potPath = Path().apply {
                    moveTo(centerX - w * 0.22f, centerY + h * 0.25f)
                    lineTo(centerX + w * 0.22f, centerY + h * 0.25f)
                    lineTo(centerX + w * 0.15f, centerY + h * 0.45f)
                    lineTo(centerX - w * 0.15f, centerY + h * 0.45f)
                    close()
                }
                drawPath(potPath, Color(0xFFD84315)) // Terracotta color
            }
            else -> {
                // Elegant Stem/Vining Leaf (Fern/Pothos/Basil style)
                val stemPath = Path().apply {
                    moveTo(centerX - w * 0.1f, h * 0.85f)
                    quadraticTo(centerX, centerY, centerX + w * 0.05f, h * 0.15f)
                }
                drawPath(stemPath, Color(0xFF81C784), style = Stroke(width = 4f))

                // Draw 5 leaves along the stem
                val leafOffsets = listOf(
                    Offset(centerX, h * 0.3f) to -30f,
                    Offset(centerX * 0.9f, h * 0.45f) to 45f,
                    Offset(centerX * 1.1f, h * 0.55f) to -45f,
                    Offset(centerX * 0.85f, h * 0.68f) to 60f,
                    Offset(centerX * 1.15f, h * 0.75f) to -60f
                )

                leafOffsets.forEach { (pos, rotation) ->
                    val path = Path().apply {
                        moveTo(pos.x, pos.y)
                        quadraticTo(pos.x - w * 0.15f, pos.y - h * 0.1f, pos.x - w * 0.22f, pos.y - h * 0.02f)
                        quadraticTo(pos.x - w * 0.1f, pos.y + h * 0.08f, pos.x, pos.y)
                    }
                    drawPath(path, Color(0xFF388E3C))
                }
            }
        }
    }
}

// --- Main App Entry Composable ---
@Composable
fun PlantApp(viewModel: PlantViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentScreenState = remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) }

    // Register active screens back navigation handler
    val navigateBack = {
        when (currentScreenState.value) {
            is Screen.Dashboard -> { /* Do nothing */ }
            else -> currentScreenState.value = Screen.Dashboard
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreenState.value is Screen.Dashboard) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(if (selectedTab == 0) Icons.Filled.LocalFlorist else Icons.Outlined.LocalFlorist, contentDescription = "Plants") },
                        label = { Text("My Garden") },
                        modifier = Modifier.testTag("nav_garden")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(if (selectedTab == 1) Icons.Filled.Place else Icons.Outlined.Place, contentDescription = "Locations") },
                        label = { Text("Locations") },
                        modifier = Modifier.testTag("nav_locations")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(if (selectedTab == 2) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications, contentDescription = "Reminders") },
                        label = { Text("Care Feed") },
                        modifier = Modifier.testTag("nav_care_feed")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(if (selectedTab == 3) Icons.Filled.Luggage else Icons.Outlined.Luggage, contentDescription = "Holiday Plan") },
                        label = { Text("Holiday") },
                        modifier = Modifier.testTag("nav_holiday")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentScreenState.value is Screen.Dashboard) {
                Crossfade(targetState = selectedTab, label = "tab_fade") { tab ->
                    when (tab) {
                        0 -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { currentScreenState.value = it },
                            onTabSelect = { selectedTab = it }
                        )
                        1 -> LocationsScreen(viewModel, onNavigate = { currentScreenState.value = it })
                        2 -> CareFeedScreen(viewModel, onNavigate = { currentScreenState.value = it })
                        3 -> HolidayCareScreen(viewModel = viewModel)
                    }
                }
            } else {
                when (val screen = currentScreenState.value) {
                    is Screen.PlantDetail -> PlantDetailScreen(plantId = screen.plantId, viewModel = viewModel, onBack = navigateBack)
                    is Screen.AreaDetail -> AreaDetailScreen(areaId = screen.areaId, viewModel = viewModel, onBack = navigateBack)
                    is Screen.AddPlant -> AddPlantScreen(viewModel = viewModel, onBack = navigateBack)
                    else -> {}
                }
            }
        }
    }
}

// --- Dashboard Feature Item Helper ---
data class FeatureItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit
)

// --- SUB-SCREEN 1: MY GARDEN / PLANTS DASHBOARD ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: PlantViewModel,
    onNavigate: (Screen) -> Unit,
    onTabSelect: (Int) -> Unit
) {
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val careAlerts by viewModel.careAlerts.collectAsStateWithLifecycle()
    val areas by viewModel.areas.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Needs Care", "Healthy"

    val filteredPlants = plants.filter { plant ->
        val matchesSearch = plant.name.contains(searchQuery, ignoreCase = true) || plant.species.contains(searchQuery, ignoreCase = true)
        val waterDue = viewModel.getDaysRemaining(plant.lastWateredTimestamp ?: plant.addedTimestamp, plant.wateringIntervalDays) <= 0
        val fertilizeDue = viewModel.getDaysRemaining(plant.lastFertilizedTimestamp ?: plant.addedTimestamp, plant.fertilizingIntervalDays) <= 0
        val pruneDue = viewModel.getDaysRemaining(plant.lastPrunedTimestamp ?: plant.addedTimestamp, plant.pruningIntervalDays) <= 0
        val needsCare = waterDue || fertilizeDue || pruneDue

        val matchesFilter = when (selectedFilter) {
            "Needs Care" -> needsCare
            "Healthy" -> !needsCare
            else -> true
        }
        matchesSearch && matchesFilter
    }

    val features = listOf(
        FeatureItem("Add Plant", Icons.Filled.Add, Color(0xFF4CAF50), { onNavigate(Screen.AddPlant) }),
        FeatureItem("Plant ID", Icons.Filled.PhotoCamera, Color(0xFF009688), { onNavigate(Screen.AddPlant) }),
        FeatureItem("Water Schedule", Icons.Filled.WaterDrop, Color(0xFF2196F3), { onTabSelect(2) }),
        FeatureItem("Feed Schedule", Icons.Filled.Eco, Color(0xFFFFB300), { onTabSelect(2) }),
        FeatureItem("Your Plants", Icons.Filled.LocalFlorist, Color(0xFF42A5F5), { selectedFilter = "All" }),
        FeatureItem("Holiday Scheduler", Icons.Filled.Luggage, Color(0xFF9C27B0), { onTabSelect(3) })
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Screen.AddPlant) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("add_plant_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Plant")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Greeting & Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Indoor Oasis",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Botanical Care & Live Health Tracker",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Eco,
                        contentDescription = "Oasis Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overdue Alerts Banner
            if (careAlerts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = "Alert",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Attention Required",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "${careAlerts.size} plant care duties are currently overdue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Quick Access Feature Buttons Grid
            Text(
                text = "Dashboard Control Panel",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                features.chunked(2).forEach { rowFeatures ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowFeatures.forEach { feature ->
                            Card(
                                onClick = feature.onClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(85.dp)
                                    .testTag("feature_btn_${feature.name.lowercase().replace(" ", "_")}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = feature.color.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, feature.color.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(feature.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = feature.icon,
                                            contentDescription = feature.name,
                                            tint = feature.color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Text(
                                        text = feature.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bar"),
                placeholder = { Text("Search plants or species...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filters Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Needs Care", "Healthy").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plants List Grid (Custom implementation using Row/Column chunking to avoid nested-scroll crashes)
            if (filteredPlants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Forest,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching plants found" else "Your garden is empty!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try searching something else" else "Tap the '+' FAB below to scan & add your first plant!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val chunkedPlants = filteredPlants.chunked(2)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunkedPlants.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { plant ->
                                Box(modifier = Modifier.weight(1f)) {
                                    val areaName = areas.find { it.id == plant.areaId }?.name ?: "No Location"
                                    PlantGridCard(
                                        plant = plant,
                                        areaName = areaName,
                                        viewModel = viewModel,
                                        onClick = { onNavigate(Screen.PlantDetail(plant.id)) }
                                    )
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp)) // Cushion for Bottom Bar
        }
    }
}

@Composable
fun PlantGridCard(plant: Plant, areaName: String, viewModel: PlantViewModel, onClick: () -> Unit) {
    val waterDays = viewModel.getDaysRemaining(plant.lastWateredTimestamp ?: plant.addedTimestamp, plant.wateringIntervalDays)
    val fertilizeDays = viewModel.getDaysRemaining(plant.lastFertilizedTimestamp ?: plant.addedTimestamp, plant.fertilizingIntervalDays)
    val pruneDays = viewModel.getDaysRemaining(plant.lastPrunedTimestamp ?: plant.addedTimestamp, plant.pruningIntervalDays)
    
    val needsCare = waterDays <= 0 || fertilizeDays <= 0 || pruneDays <= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("plant_card_${plant.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column {
            // Leaf Drawing Canvas with ambient glow based on health status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .drawBehind {
                        val brush = Brush.verticalGradient(
                            colors = if (needsCare) {
                                listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))
                            } else {
                                listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                            }
                        )
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset.Zero,
                            size = size,
                            cornerRadius = CornerRadius(0f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                BotanicalIcon(
                    species = plant.species,
                    modifier = Modifier.size(70.dp)
                )

                // Location Badge Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = areaName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White
                    )
                }

                // Alert overlay if urgent
                if (needsCare) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.NotificationsActive,
                            contentDescription = "Action Due",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            // Text Info Panel
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = plant.species,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Light),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Short status summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Watering",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                waterDays < 0 -> "Overdue (-${-waterDays}d)"
                                waterDays == 0 -> "Today"
                                else -> "${waterDays}d left"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (waterDays <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (waterDays <= 0) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (waterDays <= 0) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            tint = if (waterDays <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}


// --- SUB-SCREEN 2: LOCATIONS & SUNLIGHT MONITOR ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(viewModel: PlantViewModel, onNavigate: (Screen) -> Unit) {
    val areas by viewModel.areas.collectAsStateWithLifecycle()
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    var areaName by remember { mutableStateOf("") }
    var sunlightExposure by remember { mutableStateOf("Bright Indirect Light") }
    var areaNotes by remember { mutableStateOf("") }

    val exposures = listOf("Full Sun", "Bright Indirect Light", "Partial Shade", "Low Light")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.testTag("add_area_fab")
            ) {
                Icon(Icons.Filled.AddLocation, contentDescription = "Add Location")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "My Locations",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Analyze diurnal sunlight variations & locate similar plants",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (areas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No spots assigned. Create locations below to track sunlight exposure!")
                }
            } else {
                areas.forEach { area ->
                    val plantCount = plants.count { it.areaId == area.id }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { onNavigate(Screen.AreaDetail(area.id)) }
                            .testTag("area_card_${area.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.WbSunny,
                                        contentDescription = null,
                                        tint = when (area.sunlightExposure) {
                                            "Full Sun" -> Color(0xFFFBC02D)
                                            "Bright Indirect Light" -> Color(0xFFFF9800)
                                            "Partial Shade" -> Color(0xFF4CAF50)
                                            else -> Color(0xFF78909C)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = area.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Exposure: ${area.sunlightExposure}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$plantCount plants placed here",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "View Spot details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Add Area Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Oasis Location") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = areaName,
                            onValueChange = { areaName = it },
                            label = { Text("Location Name (e.g. Balcony, Desk)") },
                            modifier = Modifier.fillMaxWidth().testTag("area_name_input")
                        )

                        Text("Sunlight Level:", style = MaterialTheme.typography.labelMedium)
                        
                        exposures.forEach { exp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { sunlightExposure = exp },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = sunlightExposure == exp,
                                    onClick = { sunlightExposure = exp }
                                )
                                Text(exp)
                            }
                        }

                        OutlinedTextField(
                            value = areaNotes,
                            onValueChange = { areaNotes = it },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        modifier = Modifier.testTag("save_area_button"),
                        onClick = {
                            if (areaName.isNotBlank()) {
                                viewModel.insertArea(areaName, sunlightExposure, areaNotes.takeIf { it.isNotBlank() })
                                showAddDialog = false
                                areaName = ""
                                sunlightExposure = "Bright Indirect Light"
                                areaNotes = ""
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


// --- SUB-SCREEN 3: CARE FEED / PUSH REMINDERS CENTRE ---
@Composable
fun CareFeedScreen(viewModel: PlantViewModel, onNavigate: (Screen) -> Unit) {
    val context = LocalContext.current
    val careAlerts by viewModel.careAlerts.collectAsStateWithLifecycle()
    val notificationLogs by viewModel.systemNotificationLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Care Feed & Reminders",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Trigger background push alerts for fertilizing & pruning schedules",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger Local notifications button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Push Notification Center",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Request notifications of upcoming or overdue watering, fertilizing, or pruning. Runs a physical check using local OS notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Button(
                    onClick = {
                        viewModel.triggerLocalCareNotifications(context)
                        Toast.makeText(context, "Scanning plant logs and pushing system reminders...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_push_button")
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger System Care Alerts")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System logs
        if (notificationLogs.isNotEmpty()) {
            Text(
                text = "Latest Notification Activity Log",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    notificationLogs.forEach { log ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(log, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Care alerts listing
        Text(
            text = "Pending Plant Care Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (careAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.TaskAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Outstanding care complete! All plants healthy.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            careAlerts.forEach { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { onNavigate(Screen.PlantDetail(alert.plant.id)) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (alert.type) {
                                            "Water" -> Color(0xFFE3F2FD)
                                            "Fertilize" -> Color(0xFFE8F5E9)
                                            else -> Color(0xFFFFF3E0)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (alert.type) {
                                        "Water" -> Icons.Filled.WaterDrop
                                        "Fertilize" -> Icons.Filled.EnergySavingsLeaf
                                        else -> Icons.Filled.ContentCut
                                    },
                                    contentDescription = null,
                                    tint = when (alert.type) {
                                        "Water" -> Color(0xFF1E88E5)
                                        "Fertilize" -> Color(0xFF4CAF50)
                                        else -> Color(0xFFFB8C00)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = alert.plant.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = alert.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}


// --- DETAIL SCREEN 1: PLANT DETAILS & LOGGER ---
@Composable
fun PlantDetailScreen(plantId: Int, viewModel: PlantViewModel, onBack: () -> Unit) {
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val areas by viewModel.areas.collectAsStateWithLifecycle()
    val plant = plants.find { it.id == plantId }

    if (plant == null) {
        onBack()
        return
    }

    val areaName = areas.find { it.id == plant.areaId }?.name ?: "No Location"
    val careLogs by viewModel.getCareLogsForPlant(plantId).collectAsStateWithLifecycle(initialValue = emptyList())

    val waterDays = viewModel.getDaysRemaining(plant.lastWateredTimestamp ?: plant.addedTimestamp, plant.wateringIntervalDays)
    val fertilizeDays = viewModel.getDaysRemaining(plant.lastFertilizedTimestamp ?: plant.addedTimestamp, plant.fertilizingIntervalDays)
    val pruneDays = viewModel.getDaysRemaining(plant.lastPrunedTimestamp ?: plant.addedTimestamp, plant.pruningIntervalDays)

    val sdf = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Plant Care Hub",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(
                onClick = {
                    viewModel.deletePlant(plant)
                    onBack()
                },
                modifier = Modifier.testTag("delete_plant_button")
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Plant", tint = MaterialTheme.colorScheme.error)
            }
        }

        // Hero Image/Canvas card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            BotanicalIcon(
                species = plant.species,
                modifier = Modifier.size(130.dp)
            )
        }

        // Primary info
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = plant.name,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plant.species,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = areaName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sunlight and Info chips
            plant.careTips?.let { tips ->
                val tipsList = tips.split(";")
                if (tipsList.isNotEmpty()) {
                    Text(
                        text = "AId-Generated Care Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            tipsList.forEach { tip ->
                                if (tip.isNotBlank()) {
                                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = tip, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Care Actions
            Text(
                text = "Log Plant Care Routine",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Water Button
                Button(
                    onClick = { viewModel.recordCare(plant, "WATER") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (waterDays <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("water_now_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = null)
                        Text("Water", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (waterDays <= 0) "Overdue" else "${waterDays}d left",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                        )
                    }
                }

                // Fertilize Button
                Button(
                    onClick = { viewModel.recordCare(plant, "FERTILIZE") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (fertilizeDays <= 0) Color(0xFFC2185B) else Color(0xFF4CAF50)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fertilize_now_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.EnergySavingsLeaf, contentDescription = null)
                        Text("Fertilize", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (fertilizeDays <= 0) "Overdue" else "${fertilizeDays}d left",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                        )
                    }
                }

                // Prune Button
                Button(
                    onClick = { viewModel.recordCare(plant, "PRUNE") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pruneDays <= 0) Color(0xFFE65100) else Color(0xFF8D6E63)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prune_now_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ContentCut, contentDescription = null)
                        Text("Prune", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (pruneDays <= 0) "Overdue" else "${pruneDays}d left",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Historical Log listing
            Text(
                text = "Care Log History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (careLogs.isEmpty()) {
                Text(
                    "No care entries recorded yet. Tap the care action buttons above to record your first session!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                careLogs.forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (log.careType) {
                                            "WATER" -> Color(0xFFE3F2FD)
                                            "FERTILIZE" -> Color(0xFFE8F5E9)
                                            else -> Color(0xFFFFF3E0)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (log.careType) {
                                        "WATER" -> Icons.Filled.WaterDrop
                                        "FERTILIZE" -> Icons.Filled.EnergySavingsLeaf
                                        else -> Icons.Filled.ContentCut
                                    },
                                    contentDescription = null,
                                    tint = when (log.careType) {
                                        "WATER" -> Color(0xFF1E88E5)
                                        "FERTILIZE" -> Color(0xFF4CAF50)
                                        else -> Color(0xFFFB8C00)
                                    },
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (log.careType) {
                                    "WATER" -> "Watered"
                                    "FERTILIZE" -> "Fertilized"
                                    else -> "Pruned"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = sdf.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


// --- DETAIL SCREEN 2: AREA DETAILS & DIURNAL LIGHT MONITOR ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaDetailScreen(areaId: Int, viewModel: PlantViewModel, onBack: () -> Unit) {
    val areas by viewModel.areas.collectAsStateWithLifecycle()
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val area = areas.find { it.id == areaId }

    if (area == null) {
        onBack()
        return
    }

    val plantsInArea = plants.filter { it.areaId == areaId }
    val sunlightLogs by viewModel.getSunlightLogsForArea(areaId).collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddLogDialog by remember { mutableStateOf(false) }
    var selectedTimeOfDay by remember { mutableStateOf("Morning") }
    var selectedIntensity by remember { mutableStateOf("Bright") }

    val times = listOf("Morning", "Noon", "Afternoon", "Evening")
    val intensities = listOf("Direct Sun", "Bright", "Partial Shade", "Low Light", "Shade")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Custom App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Sunlight tracker",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(
                onClick = {
                    viewModel.deleteArea(area)
                    onBack()
                },
                modifier = Modifier.testTag("delete_area_button")
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Spot", tint = MaterialTheme.colorScheme.error)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = area.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Sustained Sunlight Type: ${area.sunlightExposure}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    area.notes?.let {
                        Text(
                            text = "Descriptor: $it",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Diurnal Sunlight Monitor / Logs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Diurnal Sunlight Log Tracker",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Button(
                    onClick = { showAddLogDialog = true },
                    modifier = Modifier.testTag("log_sunlight_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Sunlight")
                }
            }

            Text(
                text = "Observe and record spot light parameters during different times of the day to ensure optimal plant placement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Visual summary chart
            if (sunlightLogs.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sunlight Distribution Daily Log",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Visual grid representation
                        times.forEach { time ->
                            val activeLogs = sunlightLogs.filter { it.timeOfDay == time }
                            val currentIntensity = activeLogs.firstOrNull()?.intensity ?: "Not Logged"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (time) {
                                            "Morning" -> Icons.Filled.WbTwilight
                                            "Noon" -> Icons.Filled.WbSunny
                                            "Afternoon" -> Icons.Filled.WbCloudy
                                            else -> Icons.Filled.NightsStay
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (currentIntensity) {
                                                "Direct Sun" -> Color(0xFFFFF9C4)
                                                "Bright" -> Color(0xFFFFF59D)
                                                "Partial Shade" -> Color(0xFFC8E6C9)
                                                "Low Light" -> Color(0xFFCFD8DC)
                                                else -> Color(0xFFECEFF1)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = currentIntensity,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when (currentIntensity) {
                                                "Direct Sun" -> Color(0xFFF57F17)
                                                "Bright" -> Color(0xFFFBC02D)
                                                "Partial Shade" -> Color(0xFF388E3C)
                                                "Low Light" -> Color(0xFF37474F)
                                                else -> Color(0xFF78909C)
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No light parameters logged yet. Log sunlight intensities throughout the day to construct the diurnal tracker!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Plants placed in this area
            Text(
                text = "Plants In This Area (${plantsInArea.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (plantsInArea.isEmpty()) {
                Text(
                    "No plants placed in this location yet. Create plants and link them here to keep track!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                plantsInArea.forEach { plant ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onBack(); onBack() /* Navigate to detail directly */ },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            BotanicalIcon(species = plant.species, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(plant.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(plant.species, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // Log Sunlight Dialog
    if (showAddLogDialog) {
        AlertDialog(
            onDismissRequest = { showAddLogDialog = false },
            title = { Text("Log Spot Sunlight Parameters") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Time of Day:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        times.forEach { t ->
                            FilterChip(
                                selected = selectedTimeOfDay == t,
                                onClick = { selectedTimeOfDay = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    Text("Select Observed Sunlight Intensity:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intensities.forEach { i ->
                            FilterChip(
                                selected = selectedIntensity == i,
                                onClick = { selectedIntensity = i },
                                label = { Text(i) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag("save_sunlight_log_button"),
                    onClick = {
                        viewModel.addSunlightLog(areaId, selectedTimeOfDay, selectedIntensity)
                        showAddLogDialog = false
                    }
                ) {
                    Text("Save Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLogDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// --- SCREEN 3: ADD PLANT & AI SPECIES DETECTOR ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantScreen(viewModel: PlantViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val areas by viewModel.areas.collectAsStateWithLifecycle()
    val aiScanState by viewModel.aiScanState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var selectedAreaId by remember { mutableStateOf<Int?>(null) }
    var wateringIntervalDays by remember { mutableStateOf("7") }
    var fertilizingIntervalDays by remember { mutableStateOf("30") }
    var pruningIntervalDays by remember { mutableStateOf("90") }
    var careTipsText by remember { mutableStateOf("") }

    // Dropdown state for locations selection
    var locationMenuExpanded by remember { mutableStateOf(false) }

    // Custom animation state for scanning bar
    val scanningTransition = rememberInfiniteTransition(label = "scanner_anim")
    val scanningProgress by scanningTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner_progress"
    )

    // Camera Result launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.identifyPlantWithAI(bitmap)
        }
    }

    // Camera Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to identify plants using camera", Toast.LENGTH_LONG).show()
        }
    }

    // Gallery Result launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.identifyPlantWithAI(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load gallery photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Mock images list for users to test scanning easily
    val mockPlantsList = listOf(
        Pair("Fiddle Leaf Fig", "https://images.unsplash.com/photo-1597055181300-e3633a207518?q=80&w=600"),
        Pair("Snake Plant", "https://images.unsplash.com/photo-1599599810769-bcde5a160d32?q=80&w=600"),
        Pair("Water Basil", "https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=600")
    )

    // Pre-fill fields once AI succeeds
    LaunchedEffect(aiScanState) {
        if (aiScanState is AiScanState.Success) {
            val res = (aiScanState as AiScanState.Success).result
            species = res.speciesName
            wateringIntervalDays = res.wateringIntervalDays.toString()
            fertilizingIntervalDays = res.fertilizingIntervalDays.toString()
            pruningIntervalDays = res.pruningIntervalDays.toString()
            careTipsText = res.careTips
            if (name.isBlank()) {
                name = res.speciesName.substringBefore(" (")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.clearAiScan()
                onBack()
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Add Plant Oasis",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // AI SPECIES SCANNER WORKSPACE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Species Detector",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Capture a photo or click a mock botanical species card to automatically identify schedules using Gemini 3.5-Flash.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val permissionCheck = ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                )
                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("camera_scan_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo")
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gallery_scan_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated Scan Demo / Mock Catalog
                    Text(
                        text = "Quick Demo: Tap mock species cards to test scan",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Monstera Deliciosa", "Desert Cactus", "Sweet Orchid").forEach { mockPlant ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        // Simulate camera result by generating a mock analysis directly in ViewModel
                                        val mockResult = when (mockPlant) {
                                            "Monstera Deliciosa" -> PlantAnalysisResult(
                                                speciesName = "Monstera deliciosa (Split-leaf Philodendron)",
                                                wateringIntervalDays = 10,
                                                fertilizingIntervalDays = 30,
                                                pruningIntervalDays = 90,
                                                sunlightExposureNeeded = "Bright Indirect Light",
                                                careTips = "Let soil dry completely;Wipe leaves with damp cloth;Avoid direct burning sun"
                                            )
                                            "Desert Cactus" -> PlantAnalysisResult(
                                                speciesName = "Prickly Pear Cactus (Opuntia)",
                                                wateringIntervalDays = 21,
                                                fertilizingIntervalDays = 60,
                                                pruningIntervalDays = 180,
                                                sunlightExposureNeeded = "Full Sun",
                                                careTips = "Water extremely sparsely;Place in South facing windows;Use cactus succulent grit soil"
                                            )
                                            else -> PlantAnalysisResult(
                                                speciesName = "Moth Orchid (Phalaenopsis)",
                                                wateringIntervalDays = 8,
                                                fertilizingIntervalDays = 14,
                                                pruningIntervalDays = 45,
                                                sunlightExposureNeeded = "Partial Shade",
                                                careTips = "Never let roots stand in water;Mist roots on hot days;Prune spikes above node after bloom"
                                            )
                                        }
                                        viewModel.clearAiScan()
                                        // Set result
                                        viewModel.setAiScanState(AiScanState.Success(mockResult))
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    BotanicalIcon(species = mockPlant, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mockPlant,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Scan Loading Animation / Result Panel
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = aiScanState !is AiScanState.Idle,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                when (val state = aiScanState) {
                                    is AiScanState.Loading -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                                        ) {
                                            // Futurist scanning visualizer
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.LightGray.copy(alpha = 0.2f))
                                                    .drawBehind {
                                                        // Scanning green bar animation
                                                        val barY = size.height * scanningProgress
                                                        drawLine(
                                                            color = Color(0xFF4CAF50),
                                                            start = Offset(0f, barY),
                                                            end = Offset(size.width, barY),
                                                            strokeWidth = 6f
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.FilterCenterFocus,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Gemini AI Analyzing...", style = MaterialTheme.typography.titleSmall)
                                            Text("Extracting optimal moisture/pruning parameters", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    is AiScanState.Success -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = "Success",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    "Identification Succeeded!",
                                                    style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    "Schedules and botanical tips are pre-filled below.",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                    is AiScanState.Error -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.Error,
                                                contentDescription = "Error",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    "Scan Error",
                                                    style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = state.message,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }

            // PLANT METADATA FORM
            Text(
                text = "Plant Details Form",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Plant Nickname (e.g. Freddy the Fig)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("plant_name_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Species/Scientific Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("plant_species_input"),
                singleLine = true
            )

            // Location Dropdown selection
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = locationMenuExpanded,
                    onExpandedChange = { locationMenuExpanded = !locationMenuExpanded }
                ) {
                    val activeAreaName = areas.find { it.id == selectedAreaId }?.name ?: "No Specific Location Assigned"
                    OutlinedTextField(
                        value = activeAreaName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Home Spot/Location") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("location_dropdown"),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    ExposedDropdownMenu(
                        expanded = locationMenuExpanded,
                        onDismissRequest = { locationMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Location Assigned") },
                            onClick = {
                                selectedAreaId = null
                                locationMenuExpanded = false
                            }
                        )
                        areas.forEach { area ->
                            DropdownMenuItem(
                                text = { Text("${area.name} (Sunlight: ${area.sunlightExposure})") },
                                onClick = {
                                    selectedAreaId = area.id
                                    locationMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // watering/fertilizing/pruning parameters
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = wateringIntervalDays,
                    onValueChange = { wateringIntervalDays = it },
                    label = { Text("Water Interval (Days)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("water_days_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = fertilizingIntervalDays,
                    onValueChange = { fertilizingIntervalDays = it },
                    label = { Text("Fertilize (Days)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fertilize_days_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pruningIntervalDays,
                    onValueChange = { pruningIntervalDays = it },
                    label = { Text("Prune (Days)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prune_days_input"),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = careTipsText,
                onValueChange = { careTipsText = it },
                label = { Text("Botanical Care Tips (semicolon-separated)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                placeholder = { Text("Keep soil damp;Mist daily") }
            )

            Button(
                onClick = {
                    if (name.isNotBlank() && species.isNotBlank()) {
                        viewModel.insertPlant(
                            name = name,
                            species = species,
                            areaId = selectedAreaId,
                            wateringIntervalDays = wateringIntervalDays.toIntOrNull() ?: 7,
                            fertilizingIntervalDays = fertilizingIntervalDays.toIntOrNull() ?: 30,
                            pruningIntervalDays = pruningIntervalDays.toIntOrNull() ?: 90,
                            careTips = careTipsText.takeIf { it.isNotBlank() }
                        )
                        viewModel.clearAiScan()
                        onBack()
                    } else {
                        Toast.makeText(context, "Please enter Plant Nickname & Species", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
                    .testTag("save_plant_button")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Plant to Oasis")
            }
        }
    }
}
