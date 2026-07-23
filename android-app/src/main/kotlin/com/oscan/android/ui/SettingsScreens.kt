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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSettingsScreen(
    preferences: UserPreferences,
    onAutoCaptureChanged: (Boolean) -> Unit,
    onShutterFeedbackChanged: (Boolean) -> Unit,
    onCameraLensChanged: (CameraLensPreference) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                title = "Auto-capture default",
                subtitle = "Automatically capture when a stable document boundary is detected.",
                checked = preferences.autoCaptureDefault,
                onCheckedChange = onAutoCaptureChanged
            )
            HorizontalDivider()
            SettingSwitchRow(
                title = "Shutter feedback",
                subtitle = "Provide visual and audio/haptic feedback when taking a capture.",
                checked = preferences.shutterFeedback,
                onCheckedChange = onShutterFeedbackChanged
            )
            HorizontalDivider()
            Text("Camera Preference", style = MaterialTheme.typography.titleMedium)
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
                    Text("Rear camera (Recommended)", style = MaterialTheme.typography.bodyLarge)
                    Text("Optimal focus and edge detection", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Front camera", style = MaterialTheme.typography.bodyLarge)
                    Text("Secondary camera input", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                title = { Text("Enhancement settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Default Enhancement Treatment", style = MaterialTheme.typography.titleMedium)
            Text("Choose the default processing applied when scanning pages.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            val descriptions = mapOf(
                FilterType.MAGIC to "Whitens paper and sharpens text while preserving document colours.",
                FilterType.ORIGINAL to "Keeps the full-colour capture without enhancement.",
                FilterType.GRAYSCALE to "Removes colour while retaining smooth tones and fine detail.",
                FilterType.BLACK_WHITE to "Creates a high-contrast binary page for plain text documents."
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
                            if (filter == FilterType.MAGIC) "Magic (Recommended)" else filter.displayName,
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
fun ExportSettingsScreen(
    preferences: UserPreferences,
    onFilenamePatternChanged: (String) -> Unit,
    onPageSizeChanged: (PdfPageSize) -> Unit,
    onJpegQualityChanged: (JpegQuality) -> Unit,
    onBack: () -> Unit
) {
    var pageSizeExpanded by remember { mutableStateOf(false) }
    var qualityExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = preferences.defaultExportFilenamePattern,
                onValueChange = onFilenamePatternChanged,
                label = { Text("Default document filename prefix") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Placeholder date will automatically format into your saved document default name.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider()

            Text("PDF Page Size", style = MaterialTheme.typography.titleMedium)
            Box {
                OutlinedButton(
                    onClick = { pageSizeExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(preferences.defaultPageSize.label)
                }
                DropdownMenu(
                    expanded = pageSizeExpanded,
                    onDismissRequest = { pageSizeExpanded = false }
                ) {
                    PdfPageSize.entries.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.label) },
                            onClick = {
                                onPageSizeChanged(size)
                                pageSizeExpanded = false
                            },
                            trailingIcon = { if (preferences.defaultPageSize == size) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Export Image Quality", style = MaterialTheme.typography.titleMedium)
            Box {
                OutlinedButton(
                    onClick = { qualityExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(preferences.defaultJpegQuality.label)
                }
                DropdownMenu(
                    expanded = qualityExpanded,
                    onDismissRequest = { qualityExpanded = false }
                ) {
                    JpegQuality.entries.forEach { quality ->
                        DropdownMenuItem(
                            text = { Text(quality.label) },
                            onClick = {
                                onJpegQualityChanged(quality)
                                qualityExpanded = false
                            },
                            trailingIcon = { if (preferences.defaultJpegQuality == quality) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
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
                title = { Text("Appearance settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Text("Theme", style = MaterialTheme.typography.titleLarge)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryThemeCardOption(
                        modifier = Modifier.weight(1f),
                        title = "Light",
                        selected = themeChoice == ThemeChoice.LIGHT,
                        accentTheme = accentTheme,
                        previewMode = PreviewMode.LIGHT,
                        onSelect = { onThemeChoiceSelected(ThemeChoice.LIGHT) }
                    )
                    PrimaryThemeCardOption(
                        modifier = Modifier.weight(1f),
                        title = "Dark",
                        selected = themeChoice == ThemeChoice.DARK,
                        accentTheme = accentTheme,
                        previewMode = PreviewMode.DARK,
                        onSelect = { onThemeChoiceSelected(ThemeChoice.DARK) }
                    )
                    PrimaryThemeCardOption(
                        modifier = Modifier.weight(1f),
                        title = "Auto",
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
                Text("Color palette", style = MaterialTheme.typography.titleLarge)

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
                    Text(accentTheme.label, style = MaterialTheme.typography.titleMedium)
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
                title = { Text("Storage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    Text("Local Usage Summary", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Saved Documents")
                        Text(docsSizeMb, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Temporary Cache")
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
                Text("Clean temporary cache")
            }
            Text("Removes leftover temporary preview files and export caches. Your saved local documents remain completely safe.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Clean temporary cache?") },
                text = { Text("Temporary preview files and export caches will be cleared. Saved documents will not be affected.") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        onCleanCache()
                    }) {
                        Text("Clean cache")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel")
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
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    Text("Processing stays on this device", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Text("100% Local & Offline", style = MaterialTheme.typography.titleLarge)
            Text(
                "OScan is engineered as a local-first document manager. Document edges, perspective warping, image enhancement, and PDF generation are executed entirely on your Android device using on-device native libraries.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("No Accounts or Cloud Dependencies", style = MaterialTheme.typography.titleLarge)
            Text(
                "OScan contains no user accounts, no remote server sync, no analytics tracking, no background reporting, and no advertisements. Your document data never leaves your device unless you explicitly share a PDF.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("Explicit System Sharing", style = MaterialTheme.typography.titleLarge)
            Text(
                "When you tap Share PDF, OScan utilizes the standard Android system share sheet. You remain in full control of where and with whom your files are shared.",
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
                title = { Text("Help & User Guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            HelpTopic("Camera & Import", "Point the camera at any paper document or import existing photos. OScan automatically identifies document corners and outlines the scan region.")
            HelpTopic("Adjusting Corners", "Drag the 4 boundary handles on the Crop screen to align exactly with your paper edges. Use Reset to restore initial edge detection.")
            HelpTopic("Original vs Magic", "Choose Magic for high-contrast, clean document text or Original for unretouched color photos.")
            HelpTopic("Multi-Page Scanning", "Capture or import multiple pages into one session. Reorder or replace pages before saving as a single local document.")
            HelpTopic("Folders & Trash", "Organize documents into custom folders. Deleting documents moves them to Trash where they can be restored or permanently removed.")
            HelpTopic("Exporting PDF", "Save multi-page documents as standard PDF files using your preferred page size and compression settings.")
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

    val libraries = remember {
        listOf(
            OpenSourceLibrary(
                name = "Android Jetpack Compose & Material 3",
                description = "Modern declarative UI framework & Material 3 design system",
                repoUrl = "https://github.com/androidx/androidx",
                icon = Icons.Default.Palette
            ),
            OpenSourceLibrary(
                name = "Room Persistence Library (SQLite)",
                description = "Robust local SQLite database object mapping & async reactive queries",
                repoUrl = "https://github.com/androidx/androidx",
                icon = Icons.Default.Storage
            ),
            OpenSourceLibrary(
                name = "OpenCV Android Native Library",
                description = "Computer vision engine for document edge detection & perspective transformation",
                repoUrl = "https://github.com/opencv/opencv",
                icon = Icons.Default.AutoAwesome
            ),
            OpenSourceLibrary(
                name = "Microsoft ONNX Runtime Android",
                description = "On-device ML inference engine for document boundary prediction",
                repoUrl = "https://github.com/microsoft/onnxruntime",
                icon = Icons.Default.Psychology
            ),
            OpenSourceLibrary(
                name = "AndroidX CameraX & DataStore",
                description = "Hardware camera stream integration & reactive preference storage",
                repoUrl = "https://github.com/androidx/androidx",
                icon = Icons.Default.CameraAlt
            ),
            OpenSourceLibrary(
                name = "Apache PDFBox",
                description = "High performance PDF rendering & document creation library",
                repoUrl = "https://github.com/apache/pdfbox",
                icon = Icons.Default.PictureAsPdf
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About OScan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            text = "Version $versionName (Build $versionCode)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = "Local-first document manager for Android",
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
                        Text("View App Repository (GitHub)")
                    }
                }
            }

            // Open Source Libraries & Notices Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Open-Source Libraries & Notices",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "OScan is built using open-source software and frameworks.",
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
                        text = "Repository",
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
