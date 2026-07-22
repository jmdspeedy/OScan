package com.oscan.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import com.oscan.core.util.CoordinateTransformer
import com.oscan.android.ui.theme.OScanTheme
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
    val oscanColors = OScanTheme.colors
    val errorColor = MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(oscanColors.workspace)
    ) {
        if (!isAutoDetected) {
            Surface(
                color = oscanColors.warningContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Edges need a quick check. Place each handle on a document corner.",
                    style = MaterialTheme.typography.bodySmall,
                    color = oscanColors.onWarningContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (!isValidGeometry) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "The crop boundary cannot cross itself. Adjust the highlighted corners.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    containerSize = coordinates.size
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Scan Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            if (containerSize.width > 0 && containerSize.height > 0) {
                val containerDimensions = ImageDimensions(containerSize.width, containerSize.height)
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
                        .pointerInput(containerDimensions, sourceDimensions) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val points = latestDisplayPoints
                                    val touchThreshold = 72.dp.toPx()
                                    val closestIndex = points.indices.minByOrNull { i ->
                                        hypot(
                                            points[i].x - startOffset.x,
                                            points[i].y - startOffset.y
                                        )
                                    }
                                    if (closestIndex != null) {
                                        val dist = hypot(
                                            points[closestIndex].x - startOffset.x,
                                            points[closestIndex].y - startOffset.y
                                        )
                                        if (dist <= touchThreshold) {
                                            activeHandleIndex = closestIndex
                                        }
                                    }
                                },
                                onDragEnd = { activeHandleIndex = null },
                                onDragCancel = { activeHandleIndex = null },
                                onDrag = { change, _ ->
                                    val handle = activeHandleIndex ?: return@detectDragGestures
                                    change.consume()
                                    val newDisplayPoint = Point(
                                        change.position.x.toDouble(),
                                        change.position.y.toDouble()
                                    )
                                    onCornerMoved(handle, newDisplayPoint, containerDimensions)
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

                    // Polygon edge lines
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Polygon semi-transparent overlay fill
                    drawPath(
                        path = path,
                        color = lineColor.copy(alpha = 0.15f)
                    )

                    // Corner handle circles
                    for (i in displayPoints.indices) {
                        val pt = displayPoints[i]
                        val center = Offset(pt.x.toFloat(), pt.y.toFloat())
                        val isActive = activeHandleIndex == i
                        val handleRadius = (if (isActive) 22.dp else 17.dp).toPx()
                        drawCircle(
                            color = oscanColors.workspace,
                            radius = handleRadius,
                            center = center
                        )
                        drawCircle(
                            color = lineColor,
                            radius = handleRadius,
                            center = center,
                            style = Stroke(width = (if (isActive) 5.dp else 3.dp).toPx())
                        )
                        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = center)
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onRetake) {
                Text("Choose another")
            }
            OutlinedButton(onClick = onReset) {
                Text("Reset")
            }
            Button(
                onClick = onCropConfirmed,
                enabled = isValidGeometry
            ) {
                Text("Continue")
            }
        }
    }
}
