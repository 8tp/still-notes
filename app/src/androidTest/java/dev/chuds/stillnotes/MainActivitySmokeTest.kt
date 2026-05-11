package dev.chuds.stillnotes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun listScreen_rendersEmptyStateStrings() {
        composeRule.onNodeWithText("notes").assertIsDisplayed()
        composeRule.onNodeWithText("tap new to start a note").assertIsDisplayed()
    }

    @Test
    fun tappingNew_opensEditor() {
        composeRule.onNodeWithText("new").performClick()
        // "preview" is the edit-mode toggle verb only visible inside the note editor.
        composeRule.onNodeWithText("preview").assertIsDisplayed()
    }
}
