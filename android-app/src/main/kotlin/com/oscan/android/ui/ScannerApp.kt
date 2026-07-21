package com.oscan.android.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerApp(
    viewModel: ScannerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // System Photo Picker contract launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // System SAF document creation launcher for saving PDF
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.onExportPdfDestinationSelected(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OScan Scanner") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ScannerUiState.Empty -> {
                    StartScreen(
                        onChooseImageClicked = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    )
                }

                is ScannerUiState.Detecting -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Detecting document corners...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is ScannerUiState.CropReady -> {
                    CropScreen(
                        previewBitmap = state.previewBitmap,
                        sourceDimensions = state.sourceDimensions,
                        corners = state.corners,
                        isAutoDetected = state.isAutoDetected,
                        isValidGeometry = state.isValidGeometry,
                        onCornerMoved = { handleIndex, newDisplayPoint, containerDimensions ->
                            viewModel.onCornerMoved(handleIndex, newDisplayPoint, containerDimensions)
                        },
                        onReset = { viewModel.onResetCorners() },
                        onRetake = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onCropConfirmed = { viewModel.onCropConfirmed() }
                    )
                }

                is ScannerUiState.ProcessingCrop -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cropping and enhancing image...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is ScannerUiState.PreviewReady -> {
                    PreviewScreen(
                        croppedBitmap = state.croppedBitmap,
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = { viewModel.onFilterSelected(it) },
                        onBackToCrop = { viewModel.onBackToCrop() },
                        onExportPdfRequested = {
                            createPdfLauncher.launch("Scanned_Document.pdf")
                        }
                    )
                }

                is ScannerUiState.Exporting -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Generating single-page PDF...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is ScannerUiState.ExportSuccess -> {
                    ExportSuccessScreen(
                        pdfUri = state.pdfUri,
                        onShareClicked = { viewModel.sharePdf(context, state.pdfUri) },
                        onScanAnotherClicked = { viewModel.onResetToEmpty() }
                    )
                }

                is ScannerUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "An error occurred",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.dismissError() }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}
