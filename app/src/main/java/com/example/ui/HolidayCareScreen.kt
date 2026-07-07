package com.example.ui

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.Area
import com.example.data.Plant
import com.example.viewmodel.PlantViewModel
import java.text.SimpleDateFormat
import java.util.*

// Care task model for plan projection
data class ProjectedTask(
    val plantName: String,
    val species: String,
    val areaName: String,
    val type: String, // "Water", "Fertilize", "Prune"
    val date: Date,
    val dateString: String,
    val relativeDay: Int, // 1-indexed day of vacation
    val tips: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayCareScreen(
    viewModel: PlantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val areas by viewModel.areas.collectAsStateWithLifecycle()

    var sitterName by remember { mutableStateOf("Plant Sitter") }
    
    // Default: Holiday starts tomorrow and lasts 7 days
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    val defaultStartDate = calendar.time
    calendar.add(Calendar.DAY_OF_YEAR, 7)
    val defaultEndDate = calendar.time

    var startDate by remember { mutableStateOf(defaultStartDate) }
    var endDate by remember { mutableStateOf(defaultEndDate) }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }
    val dayOnlyFormat = remember { SimpleDateFormat("EEE, MMM dd", Locale.getDefault()) }

    // Helper to open DatePickerDialog
    fun showDatePicker(initialDate: Date, onDateSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance().apply { time = initialDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDateSelected(selectedCal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Projected plan calculation
    val projectedTasks = remember(plants, areas, startDate, endDate) {
        val tasks = mutableListOf<ProjectedTask>()
        val startMs = startDate.time
        val endMs = endDate.time

        if (startMs <= endMs && plants.isNotEmpty()) {
            val oneDayMs = 24 * 60 * 60 * 1000L
            
            plants.forEach { plant ->
                val plantArea = areas.find { it.id == plant.areaId }?.name ?: "Unknown Location"
                val tipsList = plant.careTips?.split(";")?.filter { it.isNotBlank() } ?: emptyList()

                // Project Water
                val lastWatered = plant.lastWateredTimestamp ?: plant.addedTimestamp
                val waterIntervalMs = plant.wateringIntervalDays * oneDayMs
                var currentWater = lastWatered + waterIntervalMs
                while (currentWater <= endMs) {
                    if (currentWater >= startMs) {
                        val relativeDay = (((currentWater - startMs) / oneDayMs) + 1).toInt()
                        tasks.add(
                            ProjectedTask(
                                plantName = plant.name,
                                species = plant.species,
                                areaName = plantArea,
                                type = "Water",
                                date = Date(currentWater),
                                dateString = dayOnlyFormat.format(Date(currentWater)),
                                relativeDay = relativeDay,
                                tips = tipsList
                            )
                        )
                    }
                    currentWater += waterIntervalMs
                }

                // Project Fertilize
                val lastFertilized = plant.lastFertilizedTimestamp ?: plant.addedTimestamp
                val fertilizeIntervalMs = plant.fertilizingIntervalDays * oneDayMs
                var currentFertilize = lastFertilized + fertilizeIntervalMs
                while (currentFertilize <= endMs) {
                    if (currentFertilize >= startMs) {
                        val relativeDay = (((currentFertilize - startMs) / oneDayMs) + 1).toInt()
                        tasks.add(
                            ProjectedTask(
                                plantName = plant.name,
                                species = plant.species,
                                areaName = plantArea,
                                type = "Fertilize",
                                date = Date(currentFertilize),
                                dateString = dayOnlyFormat.format(Date(currentFertilize)),
                                relativeDay = relativeDay,
                                tips = tipsList
                            )
                        )
                    }
                    currentFertilize += fertilizeIntervalMs
                }

                // Project Prune
                val lastPruned = plant.lastPrunedTimestamp ?: plant.addedTimestamp
                val pruneIntervalMs = plant.pruningIntervalDays * oneDayMs
                var currentPrune = lastPruned + pruneIntervalMs
                while (currentPrune <= endMs) {
                    if (currentPrune >= startMs) {
                        val relativeDay = (((currentPrune - startMs) / oneDayMs) + 1).toInt()
                        tasks.add(
                            ProjectedTask(
                                plantName = plant.name,
                                species = plant.species,
                                areaName = plantArea,
                                type = "Prune",
                                date = Date(currentPrune),
                                dateString = dayOnlyFormat.format(Date(currentPrune)),
                                relativeDay = relativeDay,
                                tips = tipsList
                            )
                        )
                    }
                    currentPrune += pruneIntervalMs
                }
            }
        }
        tasks.sortedWith(compareBy<ProjectedTask> { it.date }.thenBy { it.plantName })
    }

    // Generate Shareable Text
    val shareableText = remember(projectedTasks, sitterName, startDate, endDate, plants) {
        val duration = (((endDate.time - startDate.time) / (24 * 60 * 60 * 1000L)) + 1).coerceAtLeast(1)
        val sb = StringBuilder()
        sb.append("🌿 HOLIDAY PLANT CARE PLAN 🌿\n")
        sb.append("For: ${sitterName.ifBlank { "Plant Sitter" }}\n")
        sb.append("Dates: ${dateFormat.format(startDate)} to ${dateFormat.format(endDate)} ($duration days)\n\n")
        sb.append("Hi! Thank you so much for looking after my plants while I'm away! Here is the custom care plan:\n\n")

        sb.append("📅 CARE SCHEDULE:\n")
        if (projectedTasks.isEmpty()) {
            sb.append("- No tasks projected. Please inspect plants and water if soil feels dry!\n")
        } else {
            val grouped = projectedTasks.groupBy { it.dateString }
            grouped.forEach { (dateStr, dayTasks) ->
                sb.append("• $dateStr (Day ${dayTasks.first().relativeDay}):\n")
                dayTasks.forEach { t ->
                    sb.append("  [ ] ${t.type}: ${t.plantName} (${t.species}) in ${t.areaName}\n")
                }
                sb.append("\n")
            }
        }

        sb.append("🏡 PLANT PROFILES & SPECIFIC TIPS:\n")
        plants.forEach { plant ->
            val plantArea = areas.find { it.id == plant.areaId }?.name ?: "Unknown Location"
            sb.append("• ${plant.name} (${plant.species}) - $plantArea\n")
            sb.append("  - Water: Every ${plant.wateringIntervalDays} days\n")
            sb.append("  - Fertilize: Every ${plant.fertilizingIntervalDays} days\n")
            
            val tips = plant.careTips?.split(";")?.filter { it.isNotBlank() } ?: emptyList()
            if (tips.isNotEmpty()) {
                sb.append("  - Sitter Tips:\n")
                tips.forEach { tip ->
                    sb.append("    * $tip\n")
                }
            }
            sb.append("\n")
        }
        sb.append("Thank you again! Please let me know if you have any questions. 💚")
        sb.toString()
    }

    // Share Functions
    val shareViaWhatsApp = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, shareableText)
        }
        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed. Opening system share sheet...", Toast.LENGTH_SHORT).show()
            val systemShare = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareableText)
            }
            context.startActivity(Intent.createChooser(systemShare, "Share Holiday Care Plan"))
        }
    }

    val shareViaEmail = {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "🌿 Holiday Plant Caring Instructions")
            putExtra(Intent.EXTRA_TEXT, shareableText)
        }
        try {
            context.startActivity(emailIntent)
        } catch (e: Exception) {
            val systemShare = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "🌿 Holiday Plant Caring Instructions")
                putExtra(Intent.EXTRA_TEXT, shareableText)
            }
            context.startActivity(Intent.createChooser(systemShare, "Send Email"))
        }
    }

    val shareViaSMS = {
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", shareableText)
        }
        try {
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            val systemShare = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareableText)
            }
            context.startActivity(Intent.createChooser(systemShare, "Send SMS"))
        }
    }

    val copyToClipboard = {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Holiday Plant Care Plan", shareableText)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Care plan copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    val shareGeneral = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "🌿 Holiday Plant Caring Instructions")
            putExtra(Intent.EXTRA_TEXT, shareableText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Holiday Care Plan"))
    }

    var currentSubTab by remember { mutableStateOf(0) } // 0 = Itinerary, 1 = Plant Profiles

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER ---
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Holiday Care Planner",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                )
                Text(
                    text = "Going away? Generate and share custom day-by-day care schedules with your sitter.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // --- PLAN CONFIGURATION CARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("holiday_config_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Plan Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    // Sitter Name Field
                    OutlinedTextField(
                        value = sitterName,
                        onValueChange = { sitterName = it },
                        label = { Text("Sitter's Name") },
                        placeholder = { Text("e.g. Sarah") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sitter_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Date Selection Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Date
                        Button(
                            onClick = { showDatePicker(startDate) { startDate = it } },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_start_date"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Starts", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(startDate),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // End Date
                        Button(
                            onClick = { showDatePicker(endDate) { endDate = it } },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_end_date"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ends", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(endDate),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Duration and Validation check
                    val durationDays = (((endDate.time - startDate.time) / (24 * 60 * 60 * 1000L)) + 1).coerceAtLeast(1)
                    if (startDate.time > endDate.time) {
                        Text(
                            text = "Error: Start date must be before or equal to End date.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Holiday duration: $durationDays ${if (durationDays == 1L) "day" else "days"}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // --- QUICK ACTION SHARING HUB ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sharing_hub_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Share Care Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Platform grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // WhatsApp
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { shareViaWhatsApp() }
                                .padding(8.dp)
                                .testTag("btn_share_whatsapp")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF25D366),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = "WhatsApp",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                        }

                        // Email
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { shareViaEmail() }
                                .padding(8.dp)
                                .testTag("btn_share_email")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE57373),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = "Email",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text("Email", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                        }

                        // SMS / Text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { shareViaSMS() }
                                .padding(8.dp)
                                .testTag("btn_share_sms")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF64B5F6),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Sms,
                                        contentDescription = "SMS",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text("Text/SMS", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                        }

                        // General System Share
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { shareGeneral() }
                                .padding(8.dp)
                                .testTag("btn_share_general")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text("Share Sheet", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                    // Copy to Clipboard Button
                    OutlinedButton(
                        onClick = copyToClipboard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_copy_clipboard"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Full Plan Text", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- SUB-TABS (ITINERARY vs PLANT PROFILES) ---
        item {
            TabRow(
                selectedTabIndex = currentSubTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentSubTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Tab(
                    selected = currentSubTab == 0,
                    onClick = { currentSubTab = 0 },
                    text = { Text("Care Itinerary (${projectedTasks.size} tasks)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_itinerary")
                )
                Tab(
                    selected = currentSubTab == 1,
                    onClick = { currentSubTab = 1 },
                    text = { Text("Plant Profiles (${plants.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_profiles")
                )
            }
        }

        // --- DYNAMIC CONTENT LIST ---
        if (plants.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalFlorist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No plants in your garden yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Go to 'My Garden' to register your plants and generate a holiday care timeline.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            if (currentSubTab == 0) {
                // ITINERARY LIST
                if (projectedTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No scheduled care needed during this holiday range! Feel free to enjoy your trip.",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    val groupedTasks = projectedTasks.groupBy { it.dateString }
                    groupedTasks.forEach { (dateStr, dayTasks) ->
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Date Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            text = "Day ${dayTasks.first().relativeDay}",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = dateStr,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                // Day's Tasks
                                dayTasks.forEach { task ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Dynamic Action Icon
                                            val icon = when (task.type) {
                                                "Water" -> Icons.Filled.WaterDrop
                                                "Fertilize" -> Icons.Filled.Grass
                                                "Prune" -> Icons.Filled.ContentCut
                                                else -> Icons.Filled.Eco
                                            }
                                            val iconColor = when (task.type) {
                                                "Water" -> Color(0xFF1E88E5)
                                                "Fertilize" -> Color(0xFF43A047)
                                                "Prune" -> Color(0xFF8D6E63)
                                                else -> MaterialTheme.colorScheme.primary
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = iconColor.copy(alpha = 0.12f),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = iconColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = task.plantName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${task.type} • ${task.areaName}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // PLANT PROFILES LIST
                items(plants) { plant ->
                    val plantArea = areas.find { it.id == plant.areaId }?.name ?: "Unknown Location"
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Miniature drawing or species icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BotanicalIcon(
                                        species = plant.species,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .padding(2.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = plant.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${plant.species} • $plantArea",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            // Care Frequencies
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Watering", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Every ${plant.wateringIntervalDays} days", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fertilizing", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Every ${plant.fertilizingIntervalDays} days", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            // Care Tips for Sitter
                            val tips = plant.careTips?.split(";")?.filter { it.isNotBlank() } ?: emptyList()
                            if (tips.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Instructions for Helper:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    tips.forEach { tip ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = tip.trim(),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Extra bottom spacing to ensure perfect scrolling clearance
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
