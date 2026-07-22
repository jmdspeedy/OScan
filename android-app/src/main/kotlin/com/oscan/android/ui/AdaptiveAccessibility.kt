package com.oscan.android.ui

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class OScanWindowWidthClass { COMPACT, MEDIUM, EXPANDED }

fun oscanWindowWidthClass(widthDp: Int): OScanWindowWidthClass = when {
    widthDp < 600 -> OScanWindowWidthClass.COMPACT
    widthDp < 840 -> OScanWindowWidthClass.MEDIUM
    else -> OScanWindowWidthClass.EXPANDED
}

@Composable
fun rememberOScanWindowWidthClass(): OScanWindowWidthClass =
    oscanWindowWidthClass(LocalConfiguration.current.screenWidthDp)

@Immutable
data class OScanAccessibilitySettings(
    val reducedMotion: Boolean,
    val highContrastText: Boolean
)

val LocalOScanAccessibilitySettings = staticCompositionLocalOf {
    OScanAccessibilitySettings(reducedMotion = false, highContrastText = false)
}

@Composable
fun currentOScanAccessibilitySettings(): OScanAccessibilitySettings {
    val context = LocalContext.current
    val animatorScale = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    return OScanAccessibilitySettings(
        reducedMotion = animatorScale == 0f,
        highContrastText = runCatching {
            Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
        }.getOrDefault(false)
    )
}

/**
 * Keeps a group of actions reachable when the window is narrow or text is enlarged.
 * Buttons retain their natural 48dp Material target instead of being compressed into a row.
 */
@Composable
fun AdaptiveActionGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    forceStacked: Boolean = false,
    content: @Composable () -> Unit
) {
    val fontScale = LocalConfiguration.current.fontScale
    BoxWithConstraints(modifier) {
        val stacked = forceStacked || maxWidth < 420.dp || fontScale >= 1.5f
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = { content() }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                content = { content() }
            )
        }
    }
}
