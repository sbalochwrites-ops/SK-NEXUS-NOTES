package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.NoteEntity
import com.example.ui.components.NoteCard
import com.example.ui.theme.MyApplicationTheme
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun note_card_screenshot() {
    val sampleNote = NoteEntity(
      id = 1L,
      title = "Sample Note",
      content = "This is a sample note content for testing.",
      category = "Work",
      tags = "Design, Compose",
      isPinned = true,
      updatedAt = System.currentTimeMillis()
    )
    composeTestRule.setContent {
      MyApplicationTheme {
        NoteCard(
          note = sampleNote,
          onClick = {},
          onPinClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/note_card.png")
  }
}

