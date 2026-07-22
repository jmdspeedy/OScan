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
                            TextButton(
                                onClick = viewModel::onCropConfirmed,
                                enabled = current.isValidGeometry
                            ) { Text("Done") }
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
                        sourceDimensions = current.sourceDimensions,
                        corners = current.corners,
                        isValidGeometry = current.isValidGeometry,
                        onCornerMoved = viewModel::onCornerMoved
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
                    PreviewScreen(
                        croppedBitmap = current.croppedBitmap,
                        selectedFilter = current.selectedFilter,
                        onFilterSelected = viewModel::onFilterSelected,
                        onBackToCrop = viewModel::onBackToCrop,
                        onExportPdfRequested = viewModel::saveEdits,
                        primaryActionLabel = "Save page"
                    )
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
