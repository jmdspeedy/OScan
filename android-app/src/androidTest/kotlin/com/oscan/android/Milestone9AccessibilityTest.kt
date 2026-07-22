package com.oscan.android

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.oscan.android.ui.AdaptiveActionGroup
import com.oscan.android.ui.CropScreen
import com.oscan.android.ui.theme.OScanTheme
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import org.junit.Rule
import org.junit.Test
import org.opencv.core.Point

class Milestone9AccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun actionsRemainReachableAt320DpAnd200PercentText() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                OScanTheme {
                    Box(Modifier.size(width = 320.dp, height = 640.dp)) {
                        AdaptiveActionGroup {
                            androidx.compose.material3.Button(onClick = {}) { androidx.compose.material3.Text("Primary action") }
                            androidx.compose.material3.OutlinedButton(onClick = {}) { androidx.compose.material3.Text("Secondary action") }
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Primary action").assertIsDisplayed()
        composeRule.onNodeWithText("Secondary action").assertIsDisplayed()
    }

    @Test
    fun cropExplainsDirectManipulationAndValidity() {
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        composeRule.setContent {
            OScanTheme {
                CropScreen(
                    previewBitmap = bitmap,
                    sourceDimensions = ImageDimensions(200, 300),
                    corners = CornerPoints(
                        topLeft = Point(20.0, 20.0),
                        topRight = Point(180.0, 20.0),
                        bottomRight = Point(180.0, 280.0),
                        bottomLeft = Point(20.0, 280.0)
                    ),
                    isValidGeometry = true,
                    onCornerMoved = { _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Drag an edge  •  Move slowly for precision").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Adjustable crop boundary. Drag a corner or edge. Move slowly for precision."
        ).assertExists()
    }
}
