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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isAutoDetected) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Could not auto-detect document edges. Please adjust corners manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
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
                    text = "Invalid corner configuration. Corners must form a convex 4-sided shape.",
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
                modifier = Modifier.fillMaxSize()
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

                var activeHandleIndex by remember { mutableStateOf<Int?>(null) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(containerDimensions, corners) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val touchThreshold = 60.dp.toPx()
                                    val closestIndex = displayPoints.indices.minByOrNull { i ->
                                        hypot(
                                            displayPoints[i].x - startOffset.x,
                                            displayPoints[i].y - startOffset.y
                                        )
                                    }
                                    if (closestIndex != null) {
                                        val dist = hypot(
                                            displayPoints[closestIndex].x - startOffset.x,
                                            displayPoints[closestIndex].y - startOffset.y
                                        )
                                        if (dist <= touchThreshold) {
                                            activeHandleIndex = closestIndex
                                        }
                                    }
                                },
                                onDragEnd = { activeHandleIndex = null },
                                onDragCancel = { activeHandleIndex = null },
                                onDrag = { change, dragAmount ->
                                    val handle = activeHandleIndex ?: return@detectDragGestures
                                    change.consume()
                                    val currentDisplayPoint = displayPoints[handle]
                                    val newDisplayPoint = Point(
                                        currentDisplayPoint.x + dragAmount.x,
                                        currentDisplayPoint.y + dragAmount.y
                                    )
                                    onCornerMoved(handle, newDisplayPoint, containerDimensions)
                                }
                            )
                        }
                ) {
                    val lineColor = if (isValidGeometry) Color(0xFF00E676) else Color.Red
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
                        val handleRadius = 14.dp.toPx()
                        drawCircle(
                            color = Color.White,
                            radius = handleRadius,
                            center = center
                        )
                        drawCircle(
                            color = lineColor,
                            radius = handleRadius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onRetake) {
                Text("Retake")
            }
            OutlinedButton(onClick = onReset) {
                Text("Reset Corners")
            }
            Button(
                onClick = onCropConfirmed,
                enabled = isValidGeometry
            ) {
                Text("Crop & Warp")
            }
        }
    }
}
