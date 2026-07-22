package com.oscan.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.oscan.android.ui.theme.OScanTheme
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import com.oscan.core.util.CoordinateTransformer
import org.opencv.core.Point
import kotlin.math.hypot

@Composable
fun CropScreen(
    previewBitmap: Bitmap,
    sourceDimensions: ImageDimensions,
    corners: CornerPoints,
    isAutoDetected: Boolean,
    isValidGeometry: Boolean,
    onCornerMoved: (handleIndex: Int, newDisplayPoint: Point, containerDimensions: ImageDimensions) -> Unit,
    onReset: () -> Unit,
    onRetake: () -> Unit,
    onCropConfirmed: () -> Unit
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var activeHandleIndex by remember { mutableStateOf<Int?>(null) }
    var selectedHandleIndex by remember { mutableIntStateOf(0) }
    val oscanColors = OScanTheme.colors

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(oscanColors.workspace)
    ) {
        val twoPane = maxWidth >= 600.dp
        val workspace: @Composable (Modifier) -> Unit = { modifier ->
            CropWorkspace(
                modifier = modifier,
                previewBitmap = previewBitmap,
                sourceDimensions = sourceDimensions,
                corners = corners,
                isValidGeometry = isValidGeometry,
                activeHandleIndex = activeHandleIndex,
                onActiveHandleChanged = { activeHandleIndex = it },
                onContainerSizeChanged = { containerSize = it },
                onCornerMoved = onCornerMoved
            )
        }
        val controls: @Composable (Modifier) -> Unit = { modifier ->
            CropControlPanel(
                modifier = modifier,
                sourceDimensions = sourceDimensions,
                corners = corners,
                containerSize = containerSize,
                isAutoDetected = isAutoDetected,
                isValidGeometry = isValidGeometry,
                selectedHandleIndex = selectedHandleIndex,
                onSelectedHandleChanged = { selectedHandleIndex = it },
                onCornerMoved = onCornerMoved,
                onReset = onReset,
                onRetake = onRetake,
                onCropConfirmed = onCropConfirmed
            )
        }

        if (twoPane) {
            Row(Modifier.fillMaxSize()) {
                workspace(Modifier.weight(1f).fillMaxHeight())
                controls(
                    Modifier
                        .widthIn(min = 300.dp, max = 360.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                workspace(Modifier.weight(1f).fillMaxWidth())
                controls(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
private fun CropWorkspace(
    modifier: Modifier,
    previewBitmap: Bitmap,
    sourceDimensions: ImageDimensions,
    corners: CornerPoints,
    isValidGeometry: Boolean,
    activeHandleIndex: Int?,
    onActiveHandleChanged: (Int?) -> Unit,
    onContainerSizeChanged: (IntSize) -> Unit,
    onCornerMoved: (Int, Point, ImageDimensions) -> Unit
) {
    val oscanColors = OScanTheme.colors
    val errorColor = MaterialTheme.colorScheme.error
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier.onGloballyPositioned {
            size = it.size
            onContainerSizeChanged(it.size)
        },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = previewBitmap.asImageBitmap(),
            contentDescription = "Page image to crop",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        if (size.width > 0 && size.height > 0) {
            val containerDimensions = ImageDimensions(size.width, size.height)
            val transform = CoordinateTransformer.computeTransform(sourceDimensions, containerDimensions)
            val displayCorners = CoordinateTransformer.cornersToDisplay(corners, transform)
            val displayPoints = listOf(
                displayCorners.topLeft,
                displayCorners.topRight,
                displayCorners.bottomRight,
                displayCorners.bottomLeft
            )
            val latestDisplayPoints by rememberUpdatedState(displayPoints)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "Adjustable crop boundary"
                        stateDescription = if (isValidGeometry) "Valid crop" else "Invalid crop, edges cross"
                    }
                    .pointerInput(containerDimensions, sourceDimensions) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val points = latestDisplayPoints
                                val touchThreshold = 72.dp.toPx()
                                val closestIndex = points.indices.minByOrNull { i ->
                                    hypot(points[i].x - startOffset.x, points[i].y - startOffset.y)
                                }
                                if (closestIndex != null) {
                                    val distance = hypot(
                                        points[closestIndex].x - startOffset.x,
                                        points[closestIndex].y - startOffset.y
                                    )
                                    if (distance <= touchThreshold) onActiveHandleChanged(closestIndex)
                                }
                            },
                            onDragEnd = { onActiveHandleChanged(null) },
                            onDragCancel = { onActiveHandleChanged(null) },
                            onDrag = { change, _ ->
                                val handle = activeHandleIndex ?: return@detectDragGestures
                                change.consume()
                                onCornerMoved(
                                    handle,
                                    Point(change.position.x.toDouble(), change.position.y.toDouble()),
                                    containerDimensions
                                )
                            }
                        )
                    }
            ) {
                val lineColor = if (isValidGeometry) oscanColors.cropBoundary else errorColor
                val path = Path().apply {
                    moveTo(displayPoints[0].x.toFloat(), displayPoints[0].y.toFloat())
                    lineTo(displayPoints[1].x.toFloat(), displayPoints[1].y.toFloat())
                    lineTo(displayPoints[2].x.toFloat(), displayPoints[2].y.toFloat())
                    lineTo(displayPoints[3].x.toFloat(), displayPoints[3].y.toFloat())
                    close()
                }
                drawPath(path, Color.Black.copy(alpha = .9f), style = Stroke(width = 6.dp.toPx()))
                drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx()))
                drawPath(path, lineColor.copy(alpha = .15f))
                displayPoints.forEachIndexed { index, point ->
                    val center = Offset(point.x.toFloat(), point.y.toFloat())
                    val radius = (if (activeHandleIndex == index) 22.dp else 17.dp).toPx()
                    drawCircle(oscanColors.workspace, radius, center)
                    drawCircle(lineColor, radius, center, style = Stroke(width = 3.dp.toPx()))
                    drawCircle(lineColor, 4.dp.toPx(), center)
                }
            }
        }
    }
}

@Composable
private fun CropControlPanel(
    modifier: Modifier,
    sourceDimensions: ImageDimensions,
    corners: CornerPoints,
    containerSize: IntSize,
    isAutoDetected: Boolean,
    isValidGeometry: Boolean,
    selectedHandleIndex: Int,
    onSelectedHandleChanged: (Int) -> Unit,
    onCornerMoved: (Int, Point, ImageDimensions) -> Unit,
    onReset: () -> Unit,
    onRetake: () -> Unit,
    onCropConfirmed: () -> Unit
) {
    val colors = OScanTheme.colors
    val density = LocalDensity.current
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!isAutoDetected) {
            Surface(color = colors.warningContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Edges need a quick check. Place each handle on a document corner.",
                    color = colors.onWarningContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        Surface(
            color = if (isValidGeometry) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }
        ) {
            Text(
                if (isValidGeometry) "Crop boundary is valid."
                else "The crop boundary crosses itself. Adjust a corner before continuing.",
                color = if (isValidGeometry) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }

        Text("Adjust a corner", style = MaterialTheme.typography.titleMedium)
        val labels = listOf("Top left", "Top right", "Bottom right", "Bottom left")
        labels.chunked(2).forEachIndexed { rowIndex, rowLabels ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowLabels.forEachIndexed { columnIndex, label ->
                    val index = rowIndex * 2 + columnIndex
                    OutlinedButton(
                        onClick = { onSelectedHandleChanged(index) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (selectedHandleIndex == index) "$label selected" else label) }
                }
            }
        }

        if (containerSize.width > 0 && containerSize.height > 0) {
            val dimensions = ImageDimensions(containerSize.width, containerSize.height)
            val transform = CoordinateTransformer.computeTransform(sourceDimensions, dimensions)
            val display = CoordinateTransformer.cornersToDisplay(corners, transform).toArray()[selectedHandleIndex]
            val step = with(density) { 8.dp.toPx() }.toDouble()
            fun nudge(dx: Double, dy: Double) {
                onCornerMoved(
                    selectedHandleIndex,
                    Point(
                        (display.x + dx).coerceIn(0.0, containerSize.width.toDouble()),
                        (display.y + dy).coerceIn(0.0, containerSize.height.toDouble())
                    ),
                    dimensions
                )
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { nudge(0.0, -step) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, "Move ${labels[selectedHandleIndex]} up")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { nudge(-step, 0.0) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Move ${labels[selectedHandleIndex]} left")
                    }
                    Spacer(Modifier.size(48.dp))
                    IconButton(onClick = { nudge(step, 0.0) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.KeyboardArrowRight, "Move ${labels[selectedHandleIndex]} right")
                    }
                }
                IconButton(onClick = { nudge(0.0, step) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, "Move ${labels[selectedHandleIndex]} down")
                }
            }
        }

        AdaptiveActionGroup {
            OutlinedButton(onClick = onRetake) { Text("Choose another") }
            OutlinedButton(onClick = onReset) { Text("Reset") }
            Button(onClick = onCropConfirmed, enabled = isValidGeometry) { Text("Continue") }
        }
    }
}
