package com.oscan.android.ui

import com.oscan.android.R

import android.Manifest
import android.media.MediaActionSound
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CameraChromeColors(
    val accent: Color,
    val panel: Color,
    val onPanel: Color,
    val floatingSurface: Color
)

private enum class CameraScanMode {
    Document,
    IdCard
}

@Composable
private fun CameraScanMode.label(): String = when (this) {
    CameraScanMode.Document -> stringResource(R.string.camera_mode_document)
    CameraScanMode.IdCard -> stringResource(R.string.camera_mode_id_card)
}

@Composable
fun LiveCameraScreen(
    cameraViewModel: CameraViewModel,
    captureState: CameraCaptureState,
    onCaptured: (File) -> Unit,
    onDone: () -> Unit,
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


    val state by cameraViewModel.uiState.collectAsState()
    val chrome = CameraChromeColors(
        accent = MaterialTheme.colorScheme.primary,
        panel = MaterialTheme.colorScheme.surface,
        onPanel = MaterialTheme.colorScheme.onSurface,
        floatingSurface = MaterialTheme.colorScheme.surface.copy(alpha = .9f)
    )
    val haptics = LocalHapticFeedback.current
    val accessibilitySettings = LocalOScanAccessibilitySettings.current
    val shutterSound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }
    DisposableEffect(shutterSound) { onDispose(shutterSound::release) }
    var captureFeedbackEvent by remember { mutableIntStateOf(0) }
    val captureFlashAlpha = remember { Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val orientation = LocalConfiguration.current.orientation
    val compactControls = orientation == Configuration.ORIENTATION_LANDSCAPE
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    LaunchedEffect(previewView, lifecycleOwner, orientation) {
        previewView?.let { cameraViewModel.bind(lifecycleOwner, it, it.display?.rotation ?: 0) }
    }
    DisposableEffect(Unit) { onDispose(cameraViewModel::unbind) }

    val currentStatus = state.errorMessage ?: captureState.message ?: state.guidance
    var announcedStatus by remember { mutableStateOf(context.getString(R.string.camera_starting)) }
    LaunchedEffect(state.isStarting, currentStatus) {
        if (!state.isStarting) {
            delay(1_200)
            announcedStatus = currentStatus
        }
    }

    LaunchedEffect(captureFeedbackEvent) {
        if (captureFeedbackEvent == 0 || accessibilitySettings.reducedMotion) return@LaunchedEffect
        captureFlashAlpha.snapTo(.16f)
        captureFlashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 160))
    }

    LaunchedEffect(captureState.capturedCount, captureState.isProcessing) {
        if (captureState.capturedCount > 0 && !captureState.isProcessing) {
            onDone()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (permissionGranted) {
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

            if (captureFlashAlpha.value > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = if (compactControls) 76.dp else 100.dp)
                        .background(Color.White.copy(alpha = captureFlashAlpha.value))
                )
            }

            CameraGrid(bottomInset = if (compactControls) 76.dp else 100.dp)

            state.corners?.takeIf { it.size == 4 }?.let { corners ->
                DocumentCornerOverlay(corners, chrome.accent)
            }
        } else {
            CameraPermissionCard(
                denied = permissionRequested,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (compactControls) 76.dp else 100.dp)
            )
        }

        CameraTopControls(
            chrome = chrome,
            torchAvailable = state.torchAvailable && permissionGranted,
            torchEnabled = state.torchEnabled,
            controlsEnabled = permissionGranted && !state.isCapturing && !captureState.isProcessing,
            onToggleTorch = cameraViewModel::toggleTorch
        )

        if (permissionGranted) {
            Text(
                text = currentStatus,
                color = chrome.onPanel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = if (compactControls) 90.dp else 116.dp)
                    .background(chrome.floatingSurface, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

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
            isCapturing = state.isCapturing,
            captureEnabled = permissionGranted && state.isAvailable && !state.isStarting &&
                !state.isCapturing && !captureState.isProcessing && captureState.capturedCount == 0,
            onCapture = {
                if (!permissionGranted) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    val temp = File.createTempFile("capture-", ".jpg", context.cacheDir)
                    cameraViewModel.capture(temp) { capturedFile ->
                        capturedFile?.let {
                            if (shutterFeedbackEnabled) {
                                shutterSound.play(MediaActionSound.SHUTTER_CLICK)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            captureFeedbackEvent++
                            onCaptured(it)
                        }
                    }
                }
            },
            onImport = onImport
        )

        if (permissionGranted && state.isStarting) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
        if (permissionGranted && !state.isAvailable) {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.errorMessage ?: stringResource(R.string.error_camera_unavailable), color = Color.White)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onImport) { Text(stringResource(R.string.action_import_images), color = Color.White) }
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
internal fun CameraTransitionPreview() {
    val compact = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val chrome = CameraChromeColors(
        accent = MaterialTheme.colorScheme.primary,
        panel = MaterialTheme.colorScheme.surface,
        onPanel = MaterialTheme.colorScheme.onSurface,
        floatingSurface = MaterialTheme.colorScheme.surface.copy(alpha = .9f)
    )
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraGrid(bottomInset = if (compact) 76.dp else 100.dp)
        CameraTopControls(
            chrome = chrome,
            torchAvailable = false,
            torchEnabled = false,
            controlsEnabled = false,
            onToggleTorch = {}
        )
        CameraControlDock(
            chrome = chrome,
            compact = compact,
            isCapturing = false,
            captureEnabled = false,
            onCapture = {},
            onImport = {}
        )
    }
}

@Composable
private fun BoxScope.CameraTopControls(
    chrome: CameraChromeColors,
    torchAvailable: Boolean,
    torchEnabled: Boolean,
    controlsEnabled: Boolean,
    onToggleTorch: () -> Unit
) {
    val flashDescription = when {
        !torchAvailable -> stringResource(R.string.camera_flash_unavailable)
        torchEnabled -> stringResource(R.string.camera_flash_off_action)
        else -> stringResource(R.string.camera_flash_on_action)
    }
    val flashState = if (torchEnabled) stringResource(R.string.state_on) else stringResource(R.string.state_off)
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(end = 10.dp)
            .size(48.dp)
            .clickable(
                enabled = controlsEnabled && torchAvailable,
                onClick = onToggleTorch
            )
            .semantics {
                contentDescription = flashDescription
                stateDescription = flashState
            }
    ) {
        Surface(
            color = chrome.floatingSurface,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = null,
                    tint = if (torchAvailable) chrome.onPanel else chrome.onPanel.copy(alpha = .38f),
                    modifier = Modifier.size(20.dp)
                )
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
    isCapturing: Boolean,
    captureEnabled: Boolean,
    onCapture: () -> Unit,
    onImport: () -> Unit
) {
    CaptureDock(
        chrome = chrome,
        compact = compact,
        isCapturing = isCapturing,
        captureEnabled = captureEnabled,
        onCapture = onCapture,
        onImport = onImport,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}

/**
 * Mode selector button located on the bottom dock of the camera screen.
 * Formatted as a fixed-length horizontal button positioned below the dock line,
 * with a tiny "MODE" label placed outside on top of the button container.
 * Tapping cycles through available [CameraScanMode] options with a bounce scale animation
 * and a vertical slide text transition.
 *
 * @param chrome Color tokens for the camera UI chrome.
 * @param compact True if the layout is in compact (landscape) mode.
 * @param modifier Layout modifier applied to the outer Column container.
 */
@Composable
private fun ScanModeButton(
    chrome: CameraChromeColors,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(CameraScanMode.Document) }
    val scanModeDescription = stringResource(R.string.camera_scan_mode, selectedMode.label())
    val scope = rememberCoroutineScope()
    val buttonScale = remember { Animatable(1f) }

    val buttonWidth = if (compact) 88.dp else 104.dp
    val buttonHeight = if (compact) 28.dp else 34.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.camera_mode_label),
            color = chrome.onPanel.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 7.sp else 8.sp,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Surface(
            onClick = {
                scope.launch {
                    buttonScale.animateTo(0.92f, animationSpec = tween(70))
                    buttonScale.animateTo(1f, animationSpec = tween(120))
                }
                val entries = CameraScanMode.entries
                val nextIndex = (entries.indexOf(selectedMode) + 1) % entries.size
                selectedMode = entries[nextIndex]
            },
            color = chrome.onPanel.copy(alpha = .08f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .size(width = buttonWidth, height = buttonHeight)
                .graphicsLayer {
                    scaleX = buttonScale.value
                    scaleY = buttonScale.value
                }
                .semantics {
                    contentDescription = scanModeDescription
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = selectedMode,
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn(tween(140))) togetherWith
                            (slideOutVertically { height -> -height / 2 } + fadeOut(tween(140)))
                    },
                    label = "ScanModeTransition"
                ) { targetMode ->
                    Text(
                        text = targetMode.label(),
                        color = chrome.accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (compact) 9.sp else 10.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureDock(
    chrome: CameraChromeColors,
    compact: Boolean,
    isCapturing: Boolean,
    captureEnabled: Boolean,
    onCapture: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dockHeight = if (compact) 76.dp else 100.dp
    val shutterSize = if (compact) 56.dp else 68.dp
    val shutterInnerSize = if (compact) 40.dp else 50.dp
    val sideControlSize = if (compact) 40.dp else 48.dp
    val importDescription = stringResource(R.string.camera_import_gallery)
    val captureDescription = stringResource(R.string.camera_capture_page)
    val captureStateDescription = when {
        isCapturing -> stringResource(R.string.camera_capturing)
        captureEnabled -> stringResource(R.string.state_ready)
        else -> stringResource(R.string.state_unavailable)
    }

    Box(modifier.fillMaxWidth().height(dockHeight)) {
        Canvas(Modifier.matchParentSize()) {
            val center = size.width / 2f
            val shutterRadius = shutterSize.toPx() / 2f
            val notchRadius = shutterRadius + (if (compact) 6.dp else 8.dp).toPx()
            val top = shutterRadius
            val path = Path().apply {
                moveTo(0f, top)
                lineTo(center - notchRadius, top)
                arcTo(
                    rect = Rect(
                        left = center - notchRadius,
                        top = top - notchRadius,
                        right = center + notchRadius,
                        bottom = top + notchRadius
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false
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
                    bottom = if (compact) 6.dp else 12.dp
                )
                .size(sideControlSize)
                .semantics { contentDescription = importDescription }
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

        ScanModeButton(
            chrome = chrome,
            compact = compact,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = if (compact) 14.dp else 24.dp,
                    bottom = if (compact) 6.dp else 10.dp
                )
        )

        Surface(
            onClick = onCapture,
            enabled = captureEnabled,
            shape = CircleShape,
            color = chrome.panel,
            border = BorderStroke(if (compact) 4.dp else 5.dp, chrome.accent),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(shutterSize)
                .semantics {
                    contentDescription = captureDescription
                    stateDescription = captureStateDescription
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(if (compact) 2.dp else 3.dp, chrome.onPanel),
                    modifier = Modifier.size(shutterInnerSize)
                ) {}
            }
        }

    }
}

@Composable
private fun CameraPermissionCard(
    denied: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 340.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = stringResource(R.string.camera_access),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = if (denied) stringResource(R.string.camera_access_off) else stringResource(R.string.camera_allow_access),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (denied) stringResource(R.string.camera_permission_denied_body)
                    else stringResource(R.string.camera_permission_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onRequest,
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (denied) stringResource(R.string.action_try_again) else stringResource(R.string.action_continue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}
