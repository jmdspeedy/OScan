package com.oscan.android.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun LiveCameraScreen(
    cameraViewModel: CameraViewModel,
    captureState: CameraCaptureState,
    onCaptured: (File) -> Unit,
    onDone: () -> Unit,
    onClose: () -> Unit,
    onImport: () -> Unit,
    shutterFeedbackEnabled: Boolean = true
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRequested = true
        permissionGranted = granted
    }

    if (!permissionGranted) {
        CameraPermissionScreen(
            denied = permissionRequested,
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onImport = onImport,
            onClose = onClose
        )
        return
    }

    val state by cameraViewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val orientation = LocalConfiguration.current.orientation
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    LaunchedEffect(previewView, lifecycleOwner, orientation) {
        previewView?.let { cameraViewModel.bind(lifecycleOwner, it, it.display?.rotation ?: 0) }
    }
    DisposableEffect(Unit) { onDispose(cameraViewModel::unbind) }

    val currentStatus = state.errorMessage ?: captureState.message ?: state.guidance
    var announcedStatus by remember { mutableStateOf("Camera starting") }
    LaunchedEffect(state.isStarting, currentStatus) {
        if (!state.isStarting) {
            delay(1_200)
            announcedStatus = currentStatus
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                cameraViewModel.updatePreviewSize(it.size.width, it.size.height)
            }
        )
        state.corners?.takeIf { it.size == 4 }?.let { corners ->
            val boundary = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxSize()) {
                val mapped = corners.map { androidx.compose.ui.geometry.Offset(it.x, it.y) }
                val path = Path().apply {
                    moveTo(mapped[0].x, mapped[0].y)
                    mapped.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, Color.Black.copy(alpha = .45f), style = Stroke(width = 8f))
                drawPath(path, boundary, style = Stroke(width = 4f))
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .62f)).statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, enabled = !state.isCapturing && !captureState.isProcessing) { Icon(Icons.Default.Close, "Close camera", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onImport, enabled = !state.isCapturing && !captureState.isProcessing) {
                Icon(Icons.Default.PhotoLibrary, null, tint = Color.White)
                Text("Import", color = Color.White)
            }
            IconButton(onClick = cameraViewModel::toggleTorch, enabled = state.torchAvailable) {
                Icon(
                    if (state.torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    if (state.torchAvailable) if (state.torchEnabled) "Turn torch off" else "Turn torch on" else "Torch unavailable",
                    tint = if (state.torchAvailable) Color.White else Color.Gray
                )
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = .68f)).navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(currentStatus, color = Color.White)
            Box(
                Modifier.size(1.dp).semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = announcedStatus
                }
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                Surface(color = Color.White.copy(alpha = .14f), shape = MaterialTheme.shapes.large) {
                    Text("${captureState.capturedCount} pages", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
                Surface(
                    onClick = {
                        if (shutterFeedbackEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val temp = File.createTempFile("capture-", ".jpg", context.cacheDir)
                        cameraViewModel.capture(temp) { it?.let(onCaptured) }
                    },
                    enabled = state.isAvailable && !state.isStarting && !state.isCapturing,
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(72.dp).semantics {
                        contentDescription = "Capture page"
                        stateDescription = if (state.isCapturing) "Capturing" else "Ready"
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isCapturing) CircularProgressIndicator(Modifier.size(28.dp))
                        else Surface(shape = CircleShape, color = Color.Black.copy(alpha = .16f), modifier = Modifier.size(56.dp)) {}
                    }
                }
                Button(onClick = onDone, enabled = captureState.capturedCount > 0 && !captureState.isProcessing) { Text("Done") }
            }
            Box(
                Modifier.size(1.dp).semantics {
                    liveRegion = LiveRegionMode.Assertive
                    contentDescription = if (captureState.capturedCount == 1) "1 page captured" else "${captureState.capturedCount} pages captured"
                }
            )
        }

        if (state.isStarting) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
        if (!state.isAvailable) {
            Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.errorMessage ?: "Camera unavailable", color = Color.White)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onImport) { Text("Import images", color = Color.White) }
            }
        }
    }
}

@Composable
private fun CameraPermissionScreen(denied: Boolean, onRequest: () -> Unit, onImport: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (denied) "Camera access is off" else "Allow camera access", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(
                if (denied) "You can try again or import images without camera access."
                else "OScan needs camera access only while you scan. Processing stays on this device."
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text(if (denied) "Try again" else "Continue") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import images") }
            TextButton(onClick = onClose) { Text("Close") }
        }
    }
}
