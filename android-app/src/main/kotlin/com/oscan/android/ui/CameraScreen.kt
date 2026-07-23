package com.oscan.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.math.hypot
import kotlinx.coroutines.delay

private data class CameraChromeColors(
    val accent: Color,
    val panel: Color,
    val onPanel: Color,
    val floatingSurface: Color
)

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
    val chrome = CameraChromeColors(
        accent = MaterialTheme.colorScheme.primary,
        panel = MaterialTheme.colorScheme.surface,
        onPanel = MaterialTheme.colorScheme.onSurface,
        floatingSurface = MaterialTheme.colorScheme.surface.copy(alpha = .9f)
    )
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val orientation = LocalConfiguration.current.orientation
    val compactControls = orientation == Configuration.ORIENTATION_LANDSCAPE
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

    val shutterProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        shutterProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    val density = LocalDensity.current
    val shutterTranslation = with(density) {
        (if (compactControls) 34.dp else 58.dp).toPx() * (1f - shutterProgress.value)
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
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    cameraViewModel.updatePreviewSize(it.size.width, it.size.height)
                }
        )

        CameraGrid(bottomInset = if (compactControls) 112.dp else 190.dp)

        state.corners?.takeIf { it.size == 4 }?.let { corners ->
            DocumentCornerOverlay(corners, chrome.accent)
        }

        CameraTopControls(
            chrome = chrome,
            torchAvailable = state.torchAvailable,
            torchEnabled = state.torchEnabled,
            controlsEnabled = !state.isCapturing && !captureState.isProcessing,
            onToggleTorch = cameraViewModel::toggleTorch,
            onImport = onImport
        )

        Text(
            text = currentStatus,
            color = chrome.onPanel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = if (compactControls) 122.dp else 210.dp)
                .background(chrome.floatingSurface, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(
            Modifier
                .size(1.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = announcedStatus
                }
        )

        CameraControlDock(
            chrome = chrome,
            compact = compactControls,
            capturedCount = captureState.capturedCount,
            isProcessing = captureState.isProcessing,
            isCapturing = state.isCapturing,
            captureEnabled = state.isAvailable && !state.isStarting && !state.isCapturing && !captureState.isProcessing,
            shutterProgress = shutterProgress.value,
            shutterTranslation = shutterTranslation,
            onCapture = {
                if (shutterFeedbackEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                val temp = File.createTempFile("capture-", ".jpg", context.cacheDir)
                cameraViewModel.capture(temp) { it?.let(onCaptured) }
            },
            onImport = onImport,
            onReview = onDone
        )

        Box(
            Modifier
                .size(1.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Assertive
                    contentDescription = if (captureState.capturedCount == 1) {
                        "1 page captured"
                    } else {
                        "${captureState.capturedCount} pages captured"
                    }
                }
        )

        if (state.isStarting) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
        if (!state.isAvailable) {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.errorMessage ?: "Camera unavailable", color = Color.White)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onImport) { Text("Import images", color = Color.White) }
            }
        }
    }
}

/**
 * Camera-page chrome used while the destination pager is between pages. Keeping
 * CameraX out of this composition prevents the live feed from appearing or
 * rebinding until Scan is the fully settled destination.
 */
@Composable
internal fun CameraTransitionPreview(captureState: CameraCaptureState) {
    val compact = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val chrome = CameraChromeColors(
        accent = MaterialTheme.colorScheme.primary,
        panel = MaterialTheme.colorScheme.surface,
        onPanel = MaterialTheme.colorScheme.onSurface,
        floatingSurface = MaterialTheme.colorScheme.surface.copy(alpha = .9f)
    )
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraGrid(bottomInset = if (compact) 112.dp else 190.dp)
        CameraTopControls(
            chrome = chrome,
            torchAvailable = false,
            torchEnabled = false,
            controlsEnabled = false,
            onToggleTorch = {},
            onImport = {}
        )
        CameraControlDock(
            chrome = chrome,
            compact = compact,
            capturedCount = captureState.capturedCount,
            isProcessing = captureState.isProcessing,
            isCapturing = false,
            captureEnabled = false,
            shutterProgress = 1f,
            shutterTranslation = 0f,
            onCapture = {},
            onImport = {},
            onReview = {}
        )
    }
}

@Composable
private fun BoxScope.CameraTopControls(
    chrome: CameraChromeColors,
    torchAvailable: Boolean,
    torchEnabled: Boolean,
    controlsEnabled: Boolean,
    onToggleTorch: () -> Unit,
    onImport: () -> Unit
) {
    Surface(
        color = chrome.floatingSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 10.dp, start = 24.dp, end = 24.dp)
            .fillMaxWidth()
            .widthIn(max = 360.dp)
    ) {
        Row(
            modifier = Modifier.height(52.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleTorch,
                enabled = controlsEnabled && torchAvailable
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = when {
                        !torchAvailable -> "Torch unavailable"
                        torchEnabled -> "Turn torch off"
                        else -> "Turn torch on"
                    },
                    tint = if (torchAvailable) chrome.onPanel else chrome.onPanel.copy(alpha = .38f)
                )
            }
            Text(
                text = "Auto",
                color = chrome.onPanel,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onImport, enabled = controlsEnabled) {
                Icon(Icons.Default.PhotoLibrary, "Import from gallery", tint = chrome.onPanel)
            }
        }
    }
}

@Composable
private fun CameraGrid(bottomInset: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.fillMaxSize().padding(bottom = bottomInset)) {
        val lineColor = Color.White.copy(alpha = .22f)
        val stroke = 1.dp.toPx()
        for (step in 1..2) {
            val x = size.width * step / 3f
            val y = size.height * step / 3f
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), stroke)
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), stroke)
        }
    }
}

@Composable
private fun DocumentCornerOverlay(corners: List<PreviewPoint>, accent: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val points = corners.map { Offset(it.x, it.y) }
        val shadowStroke = 7.dp.toPx()
        val accentStroke = 3.dp.toPx()
        val segmentLength = 42.dp.toPx()
        points.forEachIndexed { index, corner ->
            val previous = points[(index + points.lastIndex) % points.size]
            val next = points[(index + 1) % points.size]
            val towardPrevious = corner.toward(previous, segmentLength)
            val towardNext = corner.toward(next, segmentLength)
            drawLine(Color.Black.copy(alpha = .42f), corner, towardPrevious, shadowStroke)
            drawLine(Color.Black.copy(alpha = .42f), corner, towardNext, shadowStroke)
            drawLine(accent, corner, towardPrevious, accentStroke)
            drawLine(accent, corner, towardNext, accentStroke)
        }
    }
}

private fun Offset.toward(target: Offset, distance: Float): Offset {
    val length = hypot(target.x - x, target.y - y).coerceAtLeast(1f)
    return Offset(
        x = x + (target.x - x) / length * distance.coerceAtMost(length / 2f),
        y = y + (target.y - y) / length * distance.coerceAtMost(length / 2f)
    )
}

@Composable
private fun BoxScope.CameraControlDock(
    chrome: CameraChromeColors,
    compact: Boolean,
    capturedCount: Int,
    isProcessing: Boolean,
    isCapturing: Boolean,
    captureEnabled: Boolean,
    shutterProgress: Float,
    shutterTranslation: Float,
    onCapture: () -> Unit,
    onImport: () -> Unit,
    onReview: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(chrome.panel.copy(alpha = .9f), chrome.panel),
                    startY = 0f,
                    endY = 520f
                )
            )
    ) {
        ScanModes(chrome, compact)
        CaptureDock(
            chrome = chrome,
            compact = compact,
            capturedCount = capturedCount,
            isProcessing = isProcessing,
            isCapturing = isCapturing,
            captureEnabled = captureEnabled,
            shutterProgress = shutterProgress,
            shutterTranslation = shutterTranslation,
            onCapture = onCapture,
            onImport = onImport,
            onReview = onReview
        )
    }
}

@Composable
private fun ScanModes(chrome: CameraChromeColors, compact: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 36.dp else 52.dp)
            .padding(horizontal = if (compact) 12.dp else 22.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = chrome.onPanel.copy(alpha = .06f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                "DOCUMENT",
                color = chrome.accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 11.sp else 13.sp,
                modifier = Modifier.padding(
                    horizontal = if (compact) 12.dp else 16.dp,
                    vertical = if (compact) 5.dp else 8.dp
                )
            )
        }
        Text("ID CARD", color = chrome.onPanel.copy(alpha = .72f), fontSize = if (compact) 11.sp else 13.sp)
        Text("PHOTO", color = chrome.onPanel.copy(alpha = .72f), fontSize = if (compact) 11.sp else 13.sp)
    }
}

@Composable
private fun CaptureDock(
    chrome: CameraChromeColors,
    compact: Boolean,
    capturedCount: Int,
    isProcessing: Boolean,
    isCapturing: Boolean,
    captureEnabled: Boolean,
    shutterProgress: Float,
    shutterTranslation: Float,
    onCapture: () -> Unit,
    onImport: () -> Unit,
    onReview: () -> Unit
) {
    val dockHeight = if (compact) 76.dp else 128.dp
    val shutterSize = if (compact) 68.dp else 96.dp
    val shutterInnerSize = if (compact) 50.dp else 72.dp
    val sideControlSize = if (compact) 36.dp else 58.dp
    Box(Modifier.fillMaxWidth().height(dockHeight)) {
        Canvas(Modifier.matchParentSize()) {
            val center = size.width / 2f
            val archHalfWidth = (if (compact) 52.dp else 72.dp).toPx()
            val archShoulder = (if (compact) 42.dp else 58.dp).toPx()
            val archHeight = (if (compact) 25.dp else 38.dp).toPx()
            val top = (if (compact) 32.dp else 46.dp).toPx()
            val path = Path().apply {
                moveTo(0f, top)
                lineTo(center - archHalfWidth, top)
                cubicTo(
                    center - archShoulder, top,
                    center - archShoulder, top - archHeight,
                    center, top - archHeight
                )
                cubicTo(
                    center + archShoulder, top - archHeight,
                    center + archShoulder, top,
                    center + archHalfWidth, top
                )
                lineTo(size.width, top)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, chrome.panel)
            drawPath(path, chrome.onPanel.copy(alpha = .14f), style = Stroke(1.dp.toPx()))
        }

        Surface(
            onClick = onImport,
            color = chrome.onPanel.copy(alpha = .09f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = if (compact) 22.dp else 42.dp,
                    bottom = if (compact) 4.dp else 16.dp
                )
                .size(sideControlSize)
                .semantics { contentDescription = "Import from gallery" }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    null,
                    tint = chrome.onPanel,
                    modifier = Modifier.size(if (compact) 20.dp else 26.dp)
                )
            }
        }

        Surface(
            onClick = onReview,
            enabled = capturedCount > 0 && !isProcessing,
            color = chrome.onPanel.copy(alpha = if (capturedCount > 0) .12f else .07f),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = if (compact) 18.dp else 34.dp,
                    bottom = if (compact) 6.dp else 22.dp
                )
                .semantics {
                    contentDescription = if (capturedCount > 0) {
                        "Review $capturedCount captured pages"
                    } else {
                        "No captured pages"
                    }
                }
        ) {
            Text(
                text = if (isProcessing) "Adding…" else "$capturedCount ${if (capturedCount == 1) "page" else "pages"}",
                color = chrome.onPanel.copy(alpha = if (capturedCount > 0) 1f else .58f),
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(
                    horizontal = if (compact) 12.dp else 15.dp,
                    vertical = if (compact) 7.dp else 10.dp
                )
            )
        }

        Surface(
            onClick = onCapture,
            enabled = captureEnabled,
            shape = CircleShape,
            color = chrome.panel,
            border = BorderStroke(if (compact) 5.dp else 7.dp, chrome.accent),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(shutterSize)
                .graphicsLayer {
                    translationY = shutterTranslation
                    scaleX = .55f + shutterProgress * .45f
                    scaleY = .55f + shutterProgress * .45f
                }
                .semantics {
                    contentDescription = "Capture page"
                    stateDescription = when {
                        isCapturing -> "Capturing"
                        captureEnabled -> "Ready"
                        else -> "Unavailable"
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(if (compact) 3.dp else 4.dp, chrome.onPanel),
                    modifier = Modifier.size(shutterInnerSize)
                ) {}
                if (isCapturing) {
                    CircularProgressIndicator(
                        Modifier.size(if (compact) 24.dp else 30.dp),
                        color = chrome.onPanel,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionScreen(
    denied: Boolean,
    onRequest: () -> Unit,
    onImport: () -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (denied) "Camera access is off" else "Allow camera access",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (denied) "You can try again or import images without camera access."
                else "OScan needs camera access only while you scan. Processing stays on this device."
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text(if (denied) "Try again" else "Continue")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text("Import images")
            }
            TextButton(onClick = onClose) { Text("Home") }
        }
    }
}
