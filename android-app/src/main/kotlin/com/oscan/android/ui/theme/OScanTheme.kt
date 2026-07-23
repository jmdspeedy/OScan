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
        AccentTheme.MINT -> if (darkTheme) Triple(Color(0xFF95D5B2), Color(0xFF74C69D), Color(0xFF52B788)) else Triple(Color(0xFF2D6A4F), Color(0xFF52796F), Color(0xFF40916C))
        AccentTheme.BLUE -> if (darkTheme) Triple(Color(0xFF9ECAFF), Color(0xFFBBC7DB), Color(0xFFD7BEE4)) else Triple(Color(0xFF0061A4), Color(0xFF535F70), Color(0xFF6B5778))
        AccentTheme.INDIGO -> if (darkTheme) Triple(Color(0xFFBAC3FF), Color(0xFFC4C5DD), Color(0xFFE6BAD7)) else Triple(Color(0xFF4355B9), Color(0xFF5B5D72), Color(0xFF77536D))
        AccentTheme.PURPLE -> if (darkTheme) Triple(Color(0xFFD0BCFF), Color(0xFFCCC2DC), Color(0xFFEFB8C8)) else Triple(Color(0xFF6B4EA2), Color(0xFF625B71), Color(0xFF7D5260))
        AccentTheme.PINK -> if (darkTheme) Triple(Color(0xFFFFB1C8), Color(0xFFE5BAD0), Color(0xFFF1BCAC)) else Triple(Color(0xFFB81D6D), Color(0xFF775368), Color(0xFF7E5244))
        AccentTheme.CRIMSON -> if (darkTheme) Triple(Color(0xFFFFB2BE), Color(0xFFE4BDC6), Color(0xFFE9BE99)) else Triple(Color(0xFF9C2A48), Color(0xFF75565E), Color(0xFF78593A))
        AccentTheme.AMBER -> if (darkTheme) Triple(Color(0xFFFFB95B), Color(0xFFD8C4A0), Color(0xFFAFCF97)) else Triple(Color(0xFF825500), Color(0xFF6C5D3F), Color(0xFF4B6545))
        AccentTheme.LIME -> if (darkTheme) Triple(Color(0xFFC0D163), Color(0xFFC8CAAB), Color(0xFFA3D0BF)) else Triple(Color(0xFF566500), Color(0xFF5F6146), Color(0xFF3C6657))
        AccentTheme.SLATE -> if (darkTheme) Triple(Color(0xFFB3CAD6), Color(0xFFBAC8CE), Color(0xFFC5C3EA)) else Triple(Color(0xFF4C616C), Color(0xFF535F64), Color(0xFF5C5B7D))
    }
}

fun createColorScheme(
    accentTheme: AccentTheme,
    darkTheme: Boolean
): ColorScheme {
    return when (accentTheme) {
        AccentTheme.TEAL -> if (darkTheme) OScanDarkColorScheme else OScanLightColorScheme
        AccentTheme.MINT -> if (darkTheme) darkColorScheme(
            primary = Color(0xFF95D5B2), onPrimary = Color(0xFF003822), primaryContainer = Color(0xFF005234), onPrimaryContainer = Color(0xFFB1F1CD),
            secondary = Color(0xFFB4CCBD), onSecondary = Color(0xFF20352A), secondaryContainer = Color(0xFF364B3F), onSecondaryContainer = Color(0xFFCFE8D9),
            tertiary = Color(0xFFA5CFCB), onTertiary = Color(0xFF073734), tertiaryContainer = Color(0xFF234E4B), onTertiaryContainer = Color(0xFFC1EBE7),
            background = Color(0xFF0F1512), onBackground = Color(0xFFDEE4DF), surface = Color(0xFF0F1512), onSurface = Color(0xFFDEE4DF),
            surfaceVariant = Color(0xFF404943), onSurfaceVariant = Color(0xFFC0C9C1), outline = Color(0xFF8A938C)
        ) else lightColorScheme(
            primary = Color(0xFF2D6A4F), onPrimary = Color.White, primaryContainer = Color(0xFFB1F1CD), onPrimaryContainer = Color(0xFF002112),
            secondary = Color(0xFF4D6355), onSecondary = Color.White, secondaryContainer = Color(0xFFCFE8D9), onSecondaryContainer = Color(0xFF0A1F14),
            tertiary = Color(0xFF3C6562), onTertiary = Color.White, tertiaryContainer = Color(0xFFC1EBE7), onTertiaryContainer = Color(0xFF00201E),
            background = Color(0xFFF5FBF5), onBackground = Color(0xFF171D19), surface = Color(0xFFF5FBF5), onSurface = Color(0xFF171D19),
            surfaceVariant = Color(0xFFDCE5DC), onSurfaceVariant = Color(0xFF404943), outline = Color(0xFF707973)
        )
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
        AccentTheme.INDIGO -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFBAC3FF), onPrimary = Color(0xFF0E2589), primaryContainer = Color(0xFF2A3DA0), onPrimaryContainer = Color(0xFFDEE0FF),
            secondary = Color(0xFFC4C5DD), onSecondary = Color(0xFF2D2F42), secondaryContainer = Color(0xFF434559), onSecondaryContainer = Color(0xFFE0E1FA),
            tertiary = Color(0xFFE6BAD7), onTertiary = Color(0xFF45253D), tertiaryContainer = Color(0xFF5E3B55), onTertiaryContainer = Color(0xFFFFD7F3),
            background = Color(0xFF12131C), onBackground = Color(0xFFE3E1EC), surface = Color(0xFF12131C), onSurface = Color(0xFFE3E1EC),
            surfaceVariant = Color(0xFF46464F), onSurfaceVariant = Color(0xFFC6C5D0), outline = Color(0xFF90909A)
        ) else lightColorScheme(
            primary = Color(0xFF4355B9), onPrimary = Color.White, primaryContainer = Color(0xFFDEE0FF), onPrimaryContainer = Color(0xFF00105C),
            secondary = Color(0xFF5B5D72), onSecondary = Color.White, secondaryContainer = Color(0xFFE0E1FA), onSecondaryContainer = Color(0xFF181A2C),
            tertiary = Color(0xFF77536D), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFD7F3), onTertiaryContainer = Color(0xFF2D1127),
            background = Color(0xFFFBF8FF), onBackground = Color(0xFF1B1B21), surface = Color(0xFFFBF8FF), onSurface = Color(0xFF1B1B21),
            surfaceVariant = Color(0xFFE2E1EC), onSurfaceVariant = Color(0xFF46464F), outline = Color(0xFF777680)
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
        AccentTheme.PINK -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFFFB1C8), onPrimary = Color(0xFF650033), primaryContainer = Color(0xFF8E004B), onPrimaryContainer = Color(0xFFFFD9E2),
            secondary = Color(0xFFE5BAD0), onSecondary = Color(0xFF442939), secondaryContainer = Color(0xFF5D3F50), onSecondaryContainer = Color(0xFFFFD8EC),
            tertiary = Color(0xFFF1BCAC), onTertiary = Color(0xFF4A281E), tertiaryContainer = Color(0xFF643E33), onTertiaryContainer = Color(0xFFFFDBCF),
            background = Color(0xFF191114), onBackground = Color(0xFFEFDFE2), surface = Color(0xFF191114), onSurface = Color(0xFFEFDFE2),
            surfaceVariant = Color(0xFF524349), onSurfaceVariant = Color(0xFFD6C2C8), outline = Color(0xFF9E8C93)
        ) else lightColorScheme(
            primary = Color(0xFFB81D6D), onPrimary = Color.White, primaryContainer = Color(0xFFFFD9E2), onPrimaryContainer = Color(0xFF3E0020),
            secondary = Color(0xFF775368), onSecondary = Color.White, secondaryContainer = Color(0xFFFFD8EC), onSecondaryContainer = Color(0xFF2D1124),
            tertiary = Color(0xFF7E5244), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFDBCF), onTertiaryContainer = Color(0xFF311207),
            background = Color(0xFFFFF8F8), onBackground = Color(0xFF22191C), surface = Color(0xFFFFF8F8), onSurface = Color(0xFF22191C),
            surfaceVariant = Color(0xFFF3DDE3), onSurfaceVariant = Color(0xFF524349), outline = Color(0xFF847379)
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
        AccentTheme.LIME -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFC0D163), onPrimary = Color(0xFF2C3400), primaryContainer = Color(0xFF404C00), onPrimaryContainer = Color(0xFFDCEE7C),
            secondary = Color(0xFFC8CAAB), onSecondary = Color(0xFF30321D), secondaryContainer = Color(0xFF464932), onSecondaryContainer = Color(0xFFE4E6C6),
            tertiary = Color(0xFFA3D0BF), onTertiary = Color(0xFF0C372C), tertiaryContainer = Color(0xFF254E42), onTertiaryContainer = Color(0xFFBEECDC),
            background = Color(0xFF13140D), onBackground = Color(0xFFE4E3D7), surface = Color(0xFF13140D), onSurface = Color(0xFFE4E3D7),
            surfaceVariant = Color(0xFF47483B), onSurfaceVariant = Color(0xFFC8C7B6), outline = Color(0xFF919282)
        ) else lightColorScheme(
            primary = Color(0xFF566500), onPrimary = Color.White, primaryContainer = Color(0xFFDCEE7C), onPrimaryContainer = Color(0xFF181E00),
            secondary = Color(0xFF5F6146), onSecondary = Color.White, secondaryContainer = Color(0xFFE4E6C6), onSecondaryContainer = Color(0xFF1B1D09),
            tertiary = Color(0xFF3C6657), onTertiary = Color.White, tertiaryContainer = Color(0xFFBEECDC), onTertiaryContainer = Color(0xFF002117),
            background = Color(0xFFFBFFA8).copy(alpha = 0.1f), onBackground = Color(0xFF1B1C15), surface = Color(0xFFFAFBAA).copy(alpha = 0.08f), onSurface = Color(0xFF1B1C15),
            surfaceVariant = Color(0xFFE4E3D2), onSurfaceVariant = Color(0xFF47483B), outline = Color(0xFF77786A)
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
