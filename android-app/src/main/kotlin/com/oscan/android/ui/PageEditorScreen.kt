package com.oscan.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.oscan.android.ui.theme.OScanTheme
import com.oscan.core.model.FilterType
import com.oscan.core.model.ImageDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEditorScreen(
    viewModel: PageEditorViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state) {
        if (state is PageEditorUiState.Saved) {
            onDismiss()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit page ${viewModel.page.position + 1}") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel")
                    }
                },
                actions = {
                    when (val current = state) {
                        is PageEditorUiState.CropReady -> {
                            IconButton(onClick = viewModel::onResetCorners) {
                                Icon(Icons.Default.Refresh, "Reset corners")
                            }
                        }
                        is PageEditorUiState.PreviewReady -> {
                            Button(
                                onClick = viewModel::saveEdits,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                        else -> {}
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                is PageEditorUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PageEditorUiState.CropReady -> {
                    CropScreen(
                        previewBitmap = current.previewBitmap,
                        sourceDimensions = ImageDimensions(viewModel.page.width, viewModel.page.height),
                        corners = current.corners,
                        isAutoDetected = true,
                        isValidGeometry = current.isValidGeometry,
                        onCornerMoved = viewModel::onCornerMoved,
                        onReset = viewModel::onResetCorners,
                        onRetake = onDismiss,
                        onCropConfirmed = viewModel::onCropConfirmed
                    )
                }
                is PageEditorUiState.Processing -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(current.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is PageEditorUiState.PreviewReady -> {
                    Column(
                        Modifier.fillMaxSize().background(OScanTheme.colors.workspace)
                    ) {
                        Box(
                            Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = current.croppedBitmap.asImageBitmap(),
                                contentDescription = "Cropped Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Treatment", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = current.selectedFilter == FilterType.ORIGINAL,
                                        onClick = { viewModel.onFilterSelected(FilterType.ORIGINAL) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                    ) {
                                        Text("Original")
                                    }
                                    SegmentedButton(
                                        selected = current.selectedFilter == FilterType.MAGIC,
                                        onClick = { viewModel.onFilterSelected(FilterType.MAGIC) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                    ) {
                                        Text("Magic")
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = viewModel::onBackToCrop,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Re-adjust crop")
                                }
                            }
                        }
                    }
                }
                is PageEditorUiState.Error -> {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissError,
                        title = { Text("Error") },
                        text = { Text(current.message) },
                        confirmButton = {
                            TextButton(onClick = viewModel::dismissError) {
                                Text("OK")
                            }
                        }
                    )
                }
                is PageEditorUiState.Saved -> {
                    // Handled in LaunchedEffect
                }
            }
        }
    }
}
