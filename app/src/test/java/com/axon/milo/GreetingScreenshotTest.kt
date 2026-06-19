package com.axon.milo

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.axon.milo.data.FuelDatabase
import com.axon.milo.data.FuelRepository
import com.axon.milo.ui.screens.MainDashboard
import com.axon.milo.ui.theme.MyApplicationTheme
import com.axon.milo.ui.viewmodel.FuelViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val database = Room.inMemoryDatabaseBuilder(application, FuelDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repository = FuelRepository(database.fuelDao)
    val viewModel = FuelViewModel(application, repository)

    composeTestRule.setContent {
      MyApplicationTheme {
        MainDashboard(viewModel = viewModel)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
