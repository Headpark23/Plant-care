package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.PlantRepository
import com.example.ui.PlantApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PlantViewModel
import com.example.viewmodel.PlantViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize Database & Repository
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = PlantRepository(database.plantDao())
    val factory = PlantViewModelFactory(repository)

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val viewModel: PlantViewModel = viewModel(factory = factory)
          PlantApp(viewModel = viewModel)
        }
      }
    }
  }
}
