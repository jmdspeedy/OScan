package com.oscan.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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
import com.oscan.android.data.preferences.AccentTheme
import com.oscan.android.ui.LocalOScanAccessibilitySettings
import com.oscan.android.ui.currentOScanAccessibilitySettings

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

fun AccentTheme.swatchColors(darkTheme: Boolean): Triple<Color, Color, Color> {
    return when (this) {
        AccentTheme.TEAL -> if (darkTheme) Triple(Color(0xFF4FD8E8), Color(0xFFB1CBD0), Color(0xFFBBC5EB)) else Triple(Color(0xFF006874), Color(0xFF4A6267), Color(0xFF545D7E))
        AccentTheme.BLUE -> if (darkTheme) Triple(Color(0xFF9ECAFF), Color(0xFFBBC7DB), Color(0xFFD7BEE4)) else Triple(Color(0xFF0061A4), Color(0xFF535F70), Color(0xFF6B5778))
        AccentTheme.EMERALD -> if (darkTheme) Triple(Color(0xFF78DAA3), Color(0xFFB3CCB9), Color(0xFFA3CDDC)) else Triple(Color(0xFF006E44), Color(0xFF4D6354), Color(0xFF3C6472))
        AccentTheme.PURPLE -> if (darkTheme) Triple(Color(0xFFD0BCFF), Color(0xFFCCC2DC), Color(0xFFEFB8C8)) else Triple(Color(0xFF6B4EA2), Color(0xFF625B71), Color(0xFF7D5260))
        AccentTheme.AMBER -> if (darkTheme) Triple(Color(0xFFFFB95B), Color(0xFFD8C4A0), Color(0xFFAFCF97)) else Triple(Color(0xFF825500), Color(0xFF6C5D3F), Color(0xFF4B6545))
        AccentTheme.CRIMSON -> if (darkTheme) Triple(Color(0xFFFFB2BE), Color(0xFFE4BDC6), Color(0xFFE9BE99)) else Triple(Color(0xFF9C2A48), Color(0xFF75565E), Color(0xFF78593A))
        AccentTheme.SLATE -> if (darkTheme) Triple(Color(0xFFB3CAD6), Color(0xFFBAC8CE), Color(0xFFC5C3EA)) else Triple(Color(0xFF4C616C), Color(0xFF535F64), Color(0xFF5C5B7D))
    }
}

fun createColorScheme(
    accentTheme: AccentTheme,
    darkTheme: Boolean
): ColorScheme {
    return when (accentTheme) {
        AccentTheme.TEAL -> if (darkTheme) OScanDarkColorScheme else OScanLightColorScheme
        AccentTheme.BLUE -> if (darkTheme) darkColorScheme(
            primary = Color(0xFF9ECAFF), onPrimary = Color(0xFF003258), primaryContainer = Color(0xFF00497D), onPrimaryContainer = Color(0xFFD1E4FF),
            secondary = Color(0xFFBBC7DB), onSecondary = Color(0xFF253140), secondaryContainer = Color(0xFF3B4858), onSecondaryContainer = Color(0xFFD7E3F7),
            tertiary = Color(0xFFD7BEE4), onTertiary = Color(0xFF3B2947), tertiaryContainer = Color(0xFF523F5F), onTertiaryContainer = Color(0xFFF2DAFF),
            background = Color(0xFF101418), onBackground = Color(0xFFE1E2E8), surface = Color(0xFF101418), onSurface = Color(0xFFE1E2E8),
            surfaceVariant = Color(0xFF43474E), onSurfaceVariant = Color(0xFFC3C6CF), outline = Color(0xFF8D9199)
        ) else lightColorScheme(
            primary = Color(0xFF0061A4), onPrimary = Color.White, primaryContainer = Color(0xFFD1E4FF), onPrimaryContainer = Color(0xFF001D36),
            secondary = Color(0xFF535F70), onSecondary = Color.White, secondaryContainer = Color(0xFFD7E3F7), onSecondaryContainer = Color(0xFF101C2B),
            tertiary = Color(0xFF6B5778), onTertiary = Color.White, tertiaryContainer = Color(0xFFF2DAFF), onTertiaryContainer = Color(0xFF251431),
            background = Color(0xFFF8F9FF), onBackground = Color(0xFF191C20), surface = Color(0xFFF8F9FF), onSurface = Color(0xFF191C20),
            surfaceVariant = Color(0xFFDFE2EC), onSurfaceVariant = Color(0xFF43474E), outline = Color(0xFF73777F)
        )
        AccentTheme.EMERALD -> if (darkTheme) darkColorScheme(
            primary = Color(0xFF78DAA3), onPrimary = Color(0xFF003921), primaryContainer = Color(0xFF005232), onPrimaryContainer = Color(0xFF94F7BE),
            secondary = Color(0xFFB3CCB9), onSecondary = Color(0xFF203527), secondaryContainer = Color(0xFF364B3D), onSecondaryContainer = Color(0xFFCFE9D5),
            tertiary = Color(0xFFA3CDDC), onTertiary = Color(0xFF033542), tertiaryContainer = Color(0xFF234C5A), onTertiaryContainer = Color(0xFFBFE9F9),
            background = Color(0xFF0F1511), onBackground = Color(0xFFDEE4DF), surface = Color(0xFF0F1511), onSurface = Color(0xFFDEE4DF),
            surfaceVariant = Color(0xFF404943), onSurfaceVariant = Color(0xFFC0C9C1), outline = Color(0xFF8A938C)
        ) else lightColorScheme(
            primary = Color(0xFF006E44), onPrimary = Color.White, primaryContainer = Color(0xFF94F7BE), onPrimaryContainer = Color(0xFF002111),
            secondary = Color(0xFF4D6354), onSecondary = Color.White, secondaryContainer = Color(0xFFCFE9D5), onSecondaryContainer = Color(0xFF0A1F13),
            tertiary = Color(0xFF3C6472), onTertiary = Color.White, tertiaryContainer = Color(0xFFBFE9F9), onTertiaryContainer = Color(0xFF001F29),
            background = Color(0xFFF5FBF5), onBackground = Color(0xFF171D19), surface = Color(0xFFF5FBF5), onSurface = Color(0xFF171D19),
            surfaceVariant = Color(0xFFDCE5DC), onSurfaceVariant = Color(0xFF404943), outline = Color(0xFF707973)
        )
        AccentTheme.PURPLE -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF3B1E70), primaryContainer = Color(0xFF523588), onPrimaryContainer = Color(0xFFEADDFF),
            secondary = Color(0xFFCCC2DC), onSecondary = Color(0xFF332D41), secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
            tertiary = Color(0xFFEFB8C8), onTertiary = Color(0xFF492532), tertiaryContainer = Color(0xFF633B48), onTertiaryContainer = Color(0xFFFFD8E4),
            background = Color(0xFF141218), onBackground = Color(0xFFE6E0E9), surface = Color(0xFF141218), onSurface = Color(0xFFE6E0E9),
            surfaceVariant = Color(0xFF49454F), onSurfaceVariant = Color(0xFFCAC4D0), outline = Color(0xFF938F99)
        ) else lightColorScheme(
            primary = Color(0xFF6B4EA2), onPrimary = Color.White, primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF24005B),
            secondary = Color(0xFF625B71), onSecondary = Color.White, secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1E192B),
            tertiary = Color(0xFF7D5260), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFD8E4), onTertiaryContainer = Color(0xFF31111D),
            background = Color(0xFFFEF7FF), onBackground = Color(0xFF1D1B20), surface = Color(0xFFFEF7FF), onSurface = Color(0xFF1D1B20),
            surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454F), outline = Color(0xFF79747E)
        )
        AccentTheme.AMBER -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFFFB95B), onPrimary = Color(0xFF452B00), primaryContainer = Color(0xFF633F00), onPrimaryContainer = Color(0xFFFFDDA6),
            secondary = Color(0xFFD8C4A0), onSecondary = Color(0xFF3B2F15), secondaryContainer = Color(0xFF53452A), onSecondaryContainer = Color(0xFFF5E1BB),
            tertiary = Color(0xFFAFCF97), onTertiary = Color(0xFF1E3610), tertiaryContainer = Color(0xFF344C30), onTertiaryContainer = Color(0xFFCCEBC2),
            background = Color(0xFF16130E), onBackground = Color(0xFFEAE1D9), surface = Color(0xFF16130E), onSurface = Color(0xFFEAE1D9),
            surfaceVariant = Color(0xFF4F4539), onSurfaceVariant = Color(0xFFD2C4B4), outline = Color(0xFF9B8F80)
        ) else lightColorScheme(
            primary = Color(0xFF825500), onPrimary = Color.White, primaryContainer = Color(0xFFFFDDA6), onPrimaryContainer = Color(0xFF2A1800),
            secondary = Color(0xFF6C5D3F), onSecondary = Color.White, secondaryContainer = Color(0xFFF5E1BB), onSecondaryContainer = Color(0xFF241A04),
            tertiary = Color(0xFF4B6545), onTertiary = Color.White, tertiaryContainer = Color(0xFFCCEBC2), onTertiaryContainer = Color(0xFF082107),
            background = Color(0xFFFFF8F3), onBackground = Color(0xFF1F1B16), surface = Color(0xFFFFF8F3), onSurface = Color(0xFF1F1B16),
            surfaceVariant = Color(0xFFEFE0CF), onSurfaceVariant = Color(0xFF4F4539), outline = Color(0xFF817567)
        )
        AccentTheme.CRIMSON -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFFFB2BE), onPrimary = Color(0xFF600020), primaryContainer = Color(0xFF7D1032), onPrimaryContainer = Color(0xFFFFD9DF),
            secondary = Color(0xFFE4BDC6), onSecondary = Color(0xFF432930), secondaryContainer = Color(0xFF5C3F46), onSecondaryContainer = Color(0xFFFADCE2),
            tertiary = Color(0xFFE9BE99), onTertiary = Color(0xFF442B10), tertiaryContainer = Color(0xFF5E4225), onTertiaryContainer = Color(0xFFFFDDBE),
            background = Color(0xFF181112), onBackground = Color(0xFFF0DFE1), surface = Color(0xFF181112), onSurface = Color(0xFFF0DFE1),
            surfaceVariant = Color(0xFF524345), onSurfaceVariant = Color(0xFFD6C2C4), outline = Color(0xFF9F8C8E)
        ) else lightColorScheme(
            primary = Color(0xFF9C2A48), onPrimary = Color.White, primaryContainer = Color(0xFFFFD9DF), onPrimaryContainer = Color(0xFF3F0015),
            secondary = Color(0xFF75565E), onSecondary = Color.White, secondaryContainer = Color(0xFFFADCE2), onSecondaryContainer = Color(0xFF2B151C),
            tertiary = Color(0xFF78593A), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFDDBE), onTertiaryContainer = Color(0xFF2B1702),
            background = Color(0xFFFFF8F7), onBackground = Color(0xFF22191B), surface = Color(0xFFFFF8F7), onSurface = Color(0xFF22191B),
            surfaceVariant = Color(0xFFF4DDDF), onSurfaceVariant = Color(0xFF524345), outline = Color(0xFF857375)
        )
        AccentTheme.SLATE -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFB3CAD6), onPrimary = Color(0xFF1E333D), primaryContainer = Color(0xFF344954), onPrimaryContainer = Color(0xFFCFE6F3),
            secondary = Color(0xFFBAC8CE), onSecondary = Color(0xFF253237), secondaryContainer = Color(0xFF3B484C), onSecondaryContainer = Color(0xFFD6E4EA),
            tertiary = Color(0xFFC5C3EA), onTertiary = Color(0xFF2D2D4D), tertiaryContainer = Color(0xFF444364), onTertiaryContainer = Color(0xFFE2E0FF),
            background = Color(0xFF101416), onBackground = Color(0xFFE1E3E5), surface = Color(0xFF101416), onSurface = Color(0xFFE1E3E5),
            surfaceVariant = Color(0xFF41484C), onSurfaceVariant = Color(0xFFC1C7CE), outline = Color(0xFF8B9297)
        ) else lightColorScheme(
            primary = Color(0xFF4C616C), onPrimary = Color.White, primaryContainer = Color(0xFFCFE6F3), onPrimaryContainer = Color(0xFF071E27),
            secondary = Color(0xFF535F64), onSecondary = Color.White, secondaryContainer = Color(0xFFD6E4EA), onSecondaryContainer = Color(0xFF101D21),
            tertiary = Color(0xFF5C5B7D), onTertiary = Color.White, tertiaryContainer = Color(0xFFE2E0FF), onTertiaryContainer = Color(0xFF191836),
            background = Color(0xFFF6F9FB), onBackground = Color(0xFF181C1E), surface = Color(0xFFF6F9FB), onSurface = Color(0xFF181C1E),
            surfaceVariant = Color(0xFFDDE3EA), onSurfaceVariant = Color(0xFF41484C), outline = Color(0xFF71787D)
        )
    }
}

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
    accentTheme: AccentTheme = AccentTheme.TEAL,
    darkTheme: Boolean = when (themeChoice) {
        com.oscan.android.data.preferences.ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        com.oscan.android.data.preferences.ThemeChoice.LIGHT -> false
        com.oscan.android.data.preferences.ThemeChoice.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val accessibilitySettings = currentOScanAccessibilitySettings()
    val baseColorScheme = createColorScheme(accentTheme, darkTheme)
    val colorScheme = if (accessibilitySettings.highContrastText) {
        baseColorScheme.copy(
            onBackground = if (darkTheme) Color.White else Color.Black,
            onSurface = if (darkTheme) Color.White else Color.Black,
            onSurfaceVariant = if (darkTheme) Color.White else Color(0xFF202425),
            outline = if (darkTheme) Color.White else Color.Black
        )
    } else {
        baseColorScheme
    }
    val extendedColors = (if (darkTheme) DarkExtendedColors else LightExtendedColors).copy(
        cropBoundary = colorScheme.primary
    )
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

    CompositionLocalProvider(
        LocalOScanExtendedColors provides extendedColors,
        LocalOScanAccessibilitySettings provides accessibilitySettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OScanTypography,
            shapes = OScanShapes,
            content = content
        )
    }
}
