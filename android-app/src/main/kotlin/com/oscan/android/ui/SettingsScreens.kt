package com.oscan.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import com.oscan.android.R
import com.oscan.android.data.preferences.AccentTheme
import com.oscan.android.data.preferences.CameraLensPreference
import com.oscan.android.data.preferences.JpegQuality
import com.oscan.android.data.preferences.PdfPageSize
import com.oscan.android.data.preferences.ThemeChoice
import com.oscan.android.data.preferences.UserPreferences
import com.oscan.android.data.storage.DocumentFileStore
import com.oscan.android.ui.theme.createColorScheme
import com.oscan.android.ui.theme.swatchColors
import com.oscan.core.model.FilterType
import java.io.File

@Composable
fun AccentTheme.label(): String = when (this) {
    AccentTheme.TEAL -> stringResource(R.string.accent_teal)
    AccentTheme.MINT -> stringResource(R.string.accent_mint)
    AccentTheme.BLUE -> stringResource(R.string.accent_blue)
    AccentTheme.INDIGO -> stringResource(R.string.accent_indigo)
    AccentTheme.PURPLE -> stringResource(R.string.accent_purple)
    AccentTheme.PINK -> stringResource(R.string.accent_pink)
    AccentTheme.CRIMSON -> stringResource(R.string.accent_crimson)
    AccentTheme.AMBER -> stringResource(R.string.accent_amber)
    AccentTheme.LIME -> stringResource(R.string.accent_lime)
    AccentTheme.SLATE -> stringResource(R.string.accent_slate)
}

@Composable
fun JpegQuality.label(): String = when (this) {
    JpegQuality.HIGH -> stringResource(R.string.jpeg_quality_high)
    JpegQuality.MEDIUM -> stringResource(R.string.jpeg_quality_medium)
    JpegQuality.LOW -> stringResource(R.string.jpeg_quality_low)
}

@Composable
fun FilterType.displayName(): String = when (this) {
    FilterType.MAGIC -> stringResource(R.string.treatment_magic)
    FilterType.ORIGINAL -> stringResource(R.string.treatment_photo)
    FilterType.GRAYSCALE -> stringResource(R.string.treatment_grayscale)
    FilterType.BLACK_WHITE -> stringResource(R.string.treatment_bw)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSettingsScreen(
    preferences: UserPreferences,
    onShutterFeedbackChanged: (Boolean) -> Unit,
    onCameraLensChanged: (CameraLensPreference) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_capture_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingSwitchRow(
                title = stringResource(R.string.settings_shutter_feedback),
                subtitle = stringResource(R.string.settings_shutter_feedback_desc),
                checked = preferences.shutterFeedback,
                onCheckedChange = onShutterFeedbackChanged
            )
            HorizontalDivider()
            Text(stringResource(R.string.settings_camera_preference), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCameraLensChanged(CameraLensPreference.BACK) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = preferences.cameraLensPreference == CameraLensPreference.BACK,
                    onClick = { onCameraLensChanged(CameraLensPreference.BACK) }
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.settings_rear_camera), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.settings_rear_camera_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCameraLensChanged(CameraLensPreference.FRONT) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = preferences.cameraLensPreference == CameraLensPreference.FRONT,
                    onClick = { onCameraLensChanged(CameraLensPreference.FRONT) }
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.settings_front_camera), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.settings_front_camera_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancementSettingsScreen(
    preferences: UserPreferences,
    onDefaultTreatmentChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_enhancement_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.settings_default_enhancement), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_default_enhancement_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            val descriptions = mapOf(
                FilterType.MAGIC to stringResource(R.string.treatment_magic_desc),
                FilterType.ORIGINAL to stringResource(R.string.treatment_original_desc),
                FilterType.GRAYSCALE to stringResource(R.string.treatment_grayscale_desc),
                FilterType.BLACK_WHITE to stringResource(R.string.treatment_bw_desc)
            )
            FilterType.entries.forEachIndexed { index, filter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDefaultTreatmentChanged(filter.name) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preferences.defaultTreatment == filter.name,
                        onClick = { onDefaultTreatmentChanged(filter.name) }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (filter == FilterType.MAGIC) {
                                stringResource(R.string.treatment_magic_recommended)
                            } else {
                                filter.displayName()
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            descriptions.getValue(filter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index < FilterType.entries.lastIndex) HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    themeChoice: ThemeChoice,
    accentTheme: AccentTheme,
    onThemeChoiceSelected: (ThemeChoice) -> Unit,
    onAccentThemeSelected: (AccentTheme) -> Unit,
    onBack: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeChoice) {
        ThemeChoice.SYSTEM -> isSystemDark
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_appearance_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Theme Mode Section (Matching Image 2)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleLarge)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryThemeCardOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.settings_theme_light),
                        selected = themeChoice == ThemeChoice.LIGHT,
                        accentTheme = accentTheme,
                        previewMode = PreviewMode.LIGHT,
                        onSelect = { onThemeChoiceSelected(ThemeChoice.LIGHT) }
                    )
                    PrimaryThemeCardOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.settings_theme_dark),
                        selected = themeChoice == ThemeChoice.DARK,
                        accentTheme = accentTheme,
                        previewMode = PreviewMode.DARK,
                        onSelect = { onThemeChoiceSelected(ThemeChoice.DARK) }
                    )
                    PrimaryThemeCardOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.settings_theme_auto),
                        selected = themeChoice == ThemeChoice.SYSTEM,
                        accentTheme = accentTheme,
                        previewMode = PreviewMode.AUTO,
                        onSelect = { onThemeChoiceSelected(ThemeChoice.SYSTEM) }
                    )
                }
            }

            HorizontalDivider()

            // Color Palette Section (Matching Image 1)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.settings_color_palette), style = MaterialTheme.typography.titleLarge)

                val themeRows = AccentTheme.entries.chunked(3)
                themeRows.forEach { rowEntries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowEntries.forEach { option ->
                            PaletteGridItemCard(
                                modifier = Modifier.weight(1f),
                                accentTheme = option,
                                isDark = isDark,
                                selected = accentTheme == option,
                                onSelect = { onAccentThemeSelected(option) }
                            )
                        }
                        repeat(3 - rowEntries.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private enum class PreviewMode { LIGHT, DARK, AUTO }

@Composable
private fun PrimaryThemeCardOption(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    accentTheme: AccentTheme,
    previewMode: PreviewMode,
    onSelect: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onSelect),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            ThemePreviewCanvas(accentTheme = accentTheme, previewMode = previewMode)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ThemePreviewCanvas(accentTheme: AccentTheme, previewMode: PreviewMode) {
    val lightScheme = remember(accentTheme) { createColorScheme(accentTheme, darkTheme = false) }
    val darkScheme = remember(accentTheme) { createColorScheme(accentTheme, darkTheme = true) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        fun drawThemeUI(scheme: androidx.compose.material3.ColorScheme) {
            drawRect(color = scheme.surface)

            val margin = width * 0.12f
            drawRoundRect(
                color = scheme.primary,
                topLeft = androidx.compose.ui.geometry.Offset(margin, height * 0.12f),
                size = androidx.compose.ui.geometry.Size(width - (margin * 2), height * 0.22f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )

            drawRoundRect(
                color = scheme.primaryContainer,
                topLeft = androidx.compose.ui.geometry.Offset(margin, height * 0.40f),
                size = androidx.compose.ui.geometry.Size(width * 0.65f, height * 0.05f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = scheme.secondaryContainer,
                topLeft = androidx.compose.ui.geometry.Offset(margin, height * 0.49f),
                size = androidx.compose.ui.geometry.Size(width * 0.50f, height * 0.05f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = scheme.tertiaryContainer,
                topLeft = androidx.compose.ui.geometry.Offset(margin, height * 0.58f),
                size = androidx.compose.ui.geometry.Size(width * 0.72f, height * 0.05f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )

            drawRoundRect(
                color = scheme.surfaceVariant,
                topLeft = androidx.compose.ui.geometry.Offset(margin, height * 0.78f),
                size = androidx.compose.ui.geometry.Size(width - (margin * 2), height * 0.14f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
            drawCircle(
                color = scheme.primary,
                radius = height * 0.045f,
                center = androidx.compose.ui.geometry.Offset(margin + (height * 0.07f), height * 0.85f)
            )
        }

        when (previewMode) {
            PreviewMode.LIGHT -> drawThemeUI(lightScheme)
            PreviewMode.DARK -> drawThemeUI(darkScheme)
            PreviewMode.AUTO -> {
                val topTrianglePath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width, 0f)
                    lineTo(0f, height)
                    close()
                }
                clipPath(topTrianglePath) {
                    drawThemeUI(lightScheme)
                }

                val bottomTrianglePath = Path().apply {
                    moveTo(width, 0f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                clipPath(bottomTrianglePath) {
                    drawThemeUI(darkScheme)
                }
            }
        }
    }
}

@Composable
private fun PaletteGridItemCard(
    modifier: Modifier = Modifier,
    accentTheme: AccentTheme,
    isDark: Boolean,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val (primary, secondary, tertiary) = accentTheme.swatchColors(isDark)

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onSelect)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = primary,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = secondary,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = tertiary,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }

                if (selected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentThemeOptionCard(
    accentTheme: AccentTheme,
    isDark: Boolean,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val (primary, secondary, tertiary) = accentTheme.swatchColors(isDark)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(selected = selected, onClick = onSelect)
                Column {
                    Text(accentTheme.label(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (selected) "Active color scheme" else "Tap to apply palette",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(primary, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(secondary, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(tertiary, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ThemeChoiceOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    fileStore: DocumentFileStore,
    onCleanCache: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    val docsSizeMb = remember(fileStore) {
        val totalBytes = fileStore.getStorageSize()
        String.format("%.1f MB", totalBytes / (1024.0 * 1024.0))
    }
    val cacheSizeMb = remember(context) {
        val cacheBytes = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        String.format("%.1f MB", cacheBytes / (1024.0 * 1024.0))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_storage_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_local_usage), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.settings_saved_documents))
                        Text(docsSizeMb, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.settings_temporary_cache))
                        Text(cacheSizeMb, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_clean_cache_title))
            }
            Text(stringResource(R.string.settings_clean_cache_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text(stringResource(R.string.settings_clean_cache_confirm)) },
                text = { Text(stringResource(R.string.settings_clean_cache_confirm_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        onCleanCache()
                    }) {
                        Text(stringResource(R.string.action_clean_cache))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PrivacyTip, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.privacy_on_device), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Text(stringResource(R.string.privacy_local_offline), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.privacy_local_body),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(stringResource(R.string.privacy_no_accounts), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.privacy_accounts_body),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(stringResource(R.string.privacy_explicit_sharing), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.privacy_sharing_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HelpTopic(stringResource(R.string.help_camera_title), stringResource(R.string.help_camera_body))
            HelpTopic(stringResource(R.string.help_corners_title), stringResource(R.string.help_corners_body))
            HelpTopic(stringResource(R.string.help_treatment_title), stringResource(R.string.help_treatment_body))
            HelpTopic(stringResource(R.string.help_multipage_title), stringResource(R.string.help_multipage_body))
            HelpTopic(stringResource(R.string.help_folders_title), stringResource(R.string.help_folders_body))
            HelpTopic(stringResource(R.string.help_export_title), stringResource(R.string.help_export_body))
        }
    }
}

@Composable
private fun HelpTopic(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val DEVELOPER_GITHUB_URL = "https://github.com/jmdspeedy"
private const val DEVELOPER_LINKEDIN_URL = "https://www.linkedin.com/in/james-wu-qld/"
private const val DEVELOPER_WEBSITE_URL = "https://www.jameswu.me/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val openLink: (String) -> Unit = { url ->
        try {
            uriHandler.openUri(url)
        } catch (_: Exception) {
            // The device may not have an activity capable of opening web links.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.developer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Text(stringResource(R.string.developer_name), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.developer_role),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.developer_why_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.developer_why_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.developer_connect_title), style = MaterialTheme.typography.titleLarge)
                DeveloperLinkButton(
                    label = stringResource(R.string.developer_github),
                    icon = Icons.Default.Code,
                    onClick = { openLink(DEVELOPER_GITHUB_URL) }
                )
                DeveloperLinkButton(
                    label = stringResource(R.string.developer_linkedin),
                    icon = Icons.Default.Work,
                    onClick = { openLink(DEVELOPER_LINKEDIN_URL) }
                )
                DeveloperLinkButton(
                    label = stringResource(R.string.developer_website),
                    icon = Icons.Default.Language,
                    onClick = { openLink(DEVELOPER_WEBSITE_URL) }
                )
            }
        }
    }
}

@Composable
private fun DeveloperLinkButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

private data class OpenSourceLibrary(
    val name: String,
    val description: String,
    val repoUrl: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val versionName = remember(context) { com.oscan.android.util.AppVersionUtils.getVersionName(context) }
    val versionCode = remember(context) { com.oscan.android.util.AppVersionUtils.getVersionCode(context) }

    val libraries = listOf(
            OpenSourceLibrary(
                name = "Android Jetpack Compose & Material 3",
                description = stringResource(R.string.oss_compose_desc),
                repoUrl = "https://github.com/androidx/androidx",
                icon = Icons.Default.Palette
            ),
            OpenSourceLibrary(
                name = "Room Persistence Library (SQLite)",
                description = stringResource(R.string.oss_room_desc),
                repoUrl = "https://github.com/androidx/androidx",
                icon = Icons.Default.Storage
            ),
            OpenSourceLibrary(
                name = "OpenCV Android Native Library",
                description = stringResource(R.string.oss_opencv_desc),
                repoUrl = "https://github.com/opencv/opencv",
                icon = Icons.Default.AutoAwesome
            ),
            OpenSourceLibrary(
                name = "Microsoft ONNX Runtime Android",
                description = stringResource(R.string.oss_onnx_desc),
                repoUrl = "https://github.com/microsoft/onnxruntime",
                icon = Icons.Default.Psychology
            ),
            OpenSourceLibrary(
                name = "AndroidX CameraX & DataStore",
                description = stringResource(R.string.oss_camerax_desc),
                repoUrl = "https://github.com/androidx/androidx",
                icon = Icons.Default.CameraAlt
            ),
            OpenSourceLibrary(
                name = "Apache PDFBox",
                description = stringResource(R.string.oss_pdfbox_desc),
                repoUrl = "https://github.com/apache/pdfbox",
                icon = Icons.Default.PictureAsPdf
            )
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back_button))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Text(
                        text = "OScan",
                        style = MaterialTheme.typography.headlineLarge
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_version, versionName, versionCode),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.about_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri("https://github.com/jmdspeedy/OScan")
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.about_github))
                    }
                }
            }

            // Open Source Libraries & Notices Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_oss_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.about_oss_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                libraries.forEach { lib ->
                    LibraryCard(
                        library = lib,
                        onOpenRepo = {
                            try {
                                uriHandler.openUri(lib.repoUrl)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(
    library: OpenSourceLibrary,
    onOpenRepo: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = library.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = library.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = library.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenRepo,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.about_repository),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
