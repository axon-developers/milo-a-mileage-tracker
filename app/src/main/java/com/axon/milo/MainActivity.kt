package com.axon.milo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.axon.milo.data.FuelDatabase
import com.axon.milo.data.FuelRepository
import com.axon.milo.ui.screens.MainDashboard
import com.axon.milo.ui.theme.MyApplicationTheme
import com.axon.milo.ui.viewmodel.FuelViewModel
import com.axon.milo.ui.viewmodel.FuelViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Setup offline-first Room database and repository layers
    val database = FuelDatabase.getDatabase(applicationContext)
    val repository = FuelRepository(database.fuelDao)
    
    // Create ViewModel using factory pattern
    val viewModel: FuelViewModel by viewModels {
        FuelViewModelFactory(application, repository)
    }

    enableEdgeToEdge()
    setContent {
      val themeMode by viewModel.themeMode.collectAsState()
      val darkTheme = when (themeMode) {
          "Dark" -> true
          "Light" -> false
          else -> androidx.compose.foundation.isSystemInDarkTheme()
      }
      MyApplicationTheme(darkTheme = darkTheme) {
        MainDashboard(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
