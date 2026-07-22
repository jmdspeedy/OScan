package com.oscan.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val OScanLightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EEFFD),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF4A6267),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE7EC),
    onSecondaryContainer = Color(0xFF051F23),
    tertiary = Color(0xFF545D7E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE1FF),
    onTertiaryContainer = Color(0xFF111A37),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7FAFA),
    onBackground = Color(0xFF171D1E),
    surface = Color(0xFFF7FAFA),
    onSurface = Color(0xFF171D1E),
    surfaceVariant = Color(0xFFDBE4E6),
    onSurfaceVariant = Color(0xFF3F484A),
    outline = Color(0xFF6F797A),
    outlineVariant = Color(0xFFBFC8CA),
    scrim = Color.Black
)

private val OScanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FD8E8),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF9EEFFD),
    secondary = Color(0xFFB1CBD0),
    onSecondary = Color(0xFF1C3438),
    secondaryContainer = Color(0xFF334B4F),
    onSecondaryContainer = Color(0xFFCDE7EC),
    tertiary = Color(0xFFBBC5EB),
    onTertiary = Color(0xFF252F4D),
    tertiaryContainer = Color(0xFF3C4665),
    onTertiaryContainer = Color(0xFFDCE1FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1415),
    onBackground = Color(0xFFDEE3E3),
    surface = Color(0xFF0F1415),
    onSurface = Color(0xFFDEE3E3),
    surfaceVariant = Color(0xFF3F484A),
    onSurfaceVariant = Color(0xFFBFC8CA),
    outline = Color(0xFF899294),
    outlineVariant = Color(0xFF3F484A),
    scrim = Color.Black
)

@Immutable
data class OScanExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarningContainer: Color,
    val warningContainer: Color,
    val workspace: Color,
    val onWorkspace: Color,
    val cropBoundary: Color,
    val cropScrim: Color
)

private val LightExtendedColors = OScanExtendedColors(
    success = Color(0xFF326A3B),
    onSuccess = Color.White,
    successContainer = Color(0xFFB5F2B7),
    onSuccessContainer = Color(0xFF002107),
    warning = Color(0xFF765A00),
    warningContainer = Color(0xFFFFE08A),
    onWarningContainer = Color(0xFF241A00),
    workspace = Color(0xFF101415),
    onWorkspace = Color(0xFFF1F4F4),
    cropBoundary = Color(0xFF4FD8E8),
    cropScrim = Color(0x99000000)
)

private val DarkExtendedColors = OScanExtendedColors(
    success = Color(0xFF99D69D),
    onSuccess = Color(0xFF003911),
    successContainer = Color(0xFF185125),
    onSuccessContainer = Color(0xFFB5F2B7),
    warning = Color(0xFFEBC248),
    warningContainer = Color(0xFF584500),
    onWarningContainer = Color(0xFFFFE08A),
    workspace = Color(0xFF080B0C),
    onWorkspace = Color(0xFFDEE3E3),
    cropBoundary = Color(0xFF4FD8E8),
    cropScrim = Color(0xA3000000)
)

private val LocalOScanExtendedColors = staticCompositionLocalOf { LightExtendedColors }

object OScanTheme {
    val colors: OScanExtendedColors
        @Composable get() = LocalOScanExtendedColors.current
}

private val OScanTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.15.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp)
)

private val OScanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OScanTheme(
    themeChoice: com.oscan.android.data.preferences.ThemeChoice = com.oscan.android.data.preferences.ThemeChoice.SYSTEM,
    darkTheme: Boolean = when (themeChoice) {
        com.oscan.android.data.preferences.ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        com.oscan.android.data.preferences.ThemeChoice.LIGHT -> false
        com.oscan.android.data.preferences.ThemeChoice.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OScanDarkColorScheme else OScanLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalOScanExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OScanTypography,
            shapes = OScanShapes,
            content = content
        )
    }
}
