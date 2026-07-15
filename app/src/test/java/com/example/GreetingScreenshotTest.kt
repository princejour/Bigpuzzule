package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GameScreenScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun game_screen_screenshot() {
    composeTestRule.setContent { 
        MyApplicationTheme { 
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = { TopBar() },
                bottomBar = { BottomBar() }
            ) { innerPadding ->
                GameScreen(modifier = Modifier.padding(innerPadding))
            }
        } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/game_screen.png")
  }
}
