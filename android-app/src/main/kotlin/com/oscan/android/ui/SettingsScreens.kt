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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.oscan.android.data.preferences.CameraLensPreference
import com.oscan.android.data.preferences.JpegQuality
import com.oscan.android.data.preferences.PdfPageSize
import com.oscan.android.data.preferences.ThemeChoice
import com.oscan.android.data.preferences.UserPreferences
import com.oscan.android.data.storage.DocumentFileStore
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
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDefaultTreatmentChanged("MAGIC") }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = preferences.defaultTreatment == "MAGIC",
                    onClick = { onDefaultTreatmentChanged("MAGIC") }
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Magic (Recommended)", style = MaterialTheme.typography.titleMedium)
                    Text("Optimizes document contrast, cleans background, and sharpens text locally.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDefaultTreatmentChanged("ORIGINAL") }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = preferences.defaultTreatment == "ORIGINAL",
                    onClick = { onDefaultTreatmentChanged("ORIGINAL") }
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Original photo", style = MaterialTheme.typography.titleMedium)
                    Text("Keeps natural full-color capture without enhancement.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
    onThemeChoiceSelected: (ThemeChoice) -> Unit,
    onBack: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Theme preference", style = MaterialTheme.typography.titleMedium)
            
            ThemeChoiceOption(
                title = "System default",
                subtitle = "Follow system dark and light mode settings",
                selected = themeChoice == ThemeChoice.SYSTEM,
                onSelect = { onThemeChoiceSelected(ThemeChoice.SYSTEM) }
            )
            ThemeChoiceOption(
                title = "Light theme",
                subtitle = "Clean light background and surfaces",
                selected = themeChoice == ThemeChoice.LIGHT,
                onSelect = { onThemeChoiceSelected(ThemeChoice.LIGHT) }
            )
            ThemeChoiceOption(
                title = "Dark theme",
                subtitle = "Cool dark charcoal background and surfaces",
                selected = themeChoice == ThemeChoice.DARK,
                onSelect = { onThemeChoiceSelected(ThemeChoice.DARK) }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
                }
            }
            Text("OScan", style = MaterialTheme.typography.headlineMedium)
            Text("Version 1.0.0 (Build 1)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Local-first document manager for Android", style = MaterialTheme.typography.bodyLarge)

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Open-Source Libraries & Notices", style = MaterialTheme.typography.titleMedium)
                Text("• Android Jetpack Compose & Material 3", style = MaterialTheme.typography.bodyMedium)
                Text("• Room Persistence Library (SQLite)", style = MaterialTheme.typography.bodyMedium)
                Text("• OpenCV Android Native Library", style = MaterialTheme.typography.bodyMedium)
                Text("• Microsoft ONNX Runtime Android", style = MaterialTheme.typography.bodyMedium)
                Text("• AndroidX CameraX & DataStore", style = MaterialTheme.typography.bodyMedium)
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
