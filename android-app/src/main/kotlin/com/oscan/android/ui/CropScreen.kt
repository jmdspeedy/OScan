package com.oscan.android.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oscan.android.ui.theme.OScanTheme
import com.oscan.core.model.CornerPoints
import com.oscan.core.model.ImageDimensions
import com.oscan.core.util.CoordinateTransformer
import kotlinx.coroutines.delay
import org.opencv.core.Point
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val PRECISION_GAIN = 0.30f
private const val PRECISION_SLOW_MILLIS = 180L
private const val PRECISION_MAX_DP_PER_MILLI = 0.12f

internal sealed interface CropDragTarget {
    data class Corner(val index: Int) : CropDragTarget
    data class Edge(val index: Int) : CropDragTarget
}

@Composable
fun CropScreen(
    previewBitmap: Bitmap,
    sourceDimensions: ImageDimensions,
    corners: CornerPoints,
    isValidGeometry: Boolean,
    onCornerMoved: (handleIndex: Int, newDisplayPoint: Point, containerDimensions: ImageDimensions) -> Unit
) {
    val oscanColors = OScanTheme.colors
    var showHint by remember(previewBitmap) { mutableStateOf(true) }

    LaunchedEffect(previewBitmap) {
        delay(4_500)
        showHint = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(oscanColors.workspace)
    ) {
        CropWorkspace(
            modifier = Modifier.fillMaxSize(),
            previewBitmap = previewBitmap,
            sourceDimensions = sourceDimensions,
            corners = corners,
            isValidGeometry = isValidGeometry,
            onCornerMoved = onCornerMoved
        )

        AnimatedVisibility(
            visible = showHint,
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = .68f),
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "Drag an edge  •  Move slowly for precision",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        if (!isValidGeometry) {
            Text(
                "Edges can’t cross",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
                    .background(Color.Black.copy(alpha = .78f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Assertive }
            )
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
    onCornerMoved: (Int, Point, ImageDimensions) -> Unit
) {
    val oscanColors = OScanTheme.colors
    val errorColor = MaterialTheme.colorScheme.error
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var activeTarget by remember { mutableStateOf<CropDragTarget?>(null) }
    var precisionMode by remember { mutableStateOf(false) }
    var precisionFocus by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier.onGloballyPositioned { size = it.size },
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
            val displayPoints = displayCorners.toArray().toList()
            val latestDisplayPoints by rememberUpdatedState(displayPoints)
            val lineColor = if (isValidGeometry) oscanColors.cropBoundary else errorColor

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = "Adjustable crop boundary. Drag a corner or edge. Move slowly for precision."
                        stateDescription = if (isValidGeometry) "Valid crop" else "Invalid crop, edges cross"
                    }
                    .pointerInput(containerDimensions, sourceDimensions, density) {
                        val cornerThreshold = with(density) { 36.dp.toPx() }
                        val edgeThreshold = with(density) { 30.dp.toPx() }
                        var dragTarget: CropDragTarget? = null
                        var dragStart = Offset.Zero
                        var startPoints = emptyList<Point>()
                        var lastPointer = Offset.Zero
                        var lastTime = 0L
                        var slowMillis = 0L
                        var isPrecision = false
                        var precisionPointerAnchor = Offset.Zero
                        var precisionPointsAnchor = emptyList<Point>()
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val target = findCropDragTarget(
                                    latestDisplayPoints,
                                    startOffset,
                                    cornerThreshold,
                                    edgeThreshold
                                )
                                dragTarget = target
                                dragStart = startOffset
                                startPoints = latestDisplayPoints.map { Point(it.x, it.y) }
                                lastPointer = startOffset
                                lastTime = 0L
                                slowMillis = 0L
                                isPrecision = false
                                activeTarget = target
                                precisionMode = false
                                precisionFocus = null
                            },
                            onDragEnd = {
                                activeTarget = null
                                precisionMode = false
                                precisionFocus = null
                            },
                            onDragCancel = {
                                activeTarget = null
                                precisionMode = false
                                precisionFocus = null
                            },
                            onDrag = { change, _ ->
                                val target = dragTarget
                                if (target != null) {
                                    change.consume()

                                    val elapsed = if (lastTime == 0L) 0L else (change.uptimeMillis - lastTime).coerceAtLeast(1L)
                                    val distancePx = hypot(
                                        change.position.x - lastPointer.x,
                                        change.position.y - lastPointer.y
                                    )
                                    slowMillis = updatedPrecisionSlowMillis(
                                        currentSlowMillis = slowMillis,
                                        distancePx = distancePx,
                                        density = density.density,
                                        elapsedMillis = elapsed
                                    )

                                    if (!isPrecision && slowMillis >= PRECISION_SLOW_MILLIS) {
                                        isPrecision = true
                                        precisionMode = true
                                        precisionPointerAnchor = change.position
                                        precisionPointsAnchor = latestDisplayPoints.map { Point(it.x, it.y) }
                                    }

                                    val points = if (isPrecision) precisionPointsAnchor else startPoints
                                    val pointerAnchor = if (isPrecision) precisionPointerAnchor else dragStart
                                    val gain = if (isPrecision) PRECISION_GAIN else 1f
                                    val dx = (change.position.x - pointerAnchor.x) * gain
                                    val dy = (change.position.y - pointerAnchor.y) * gain

                                    when (target) {
                                        is CropDragTarget.Corner -> {
                                            val base = points[target.index]
                                            val moved = Point(base.x + dx, base.y + dy)
                                            onCornerMoved(target.index, moved, containerDimensions)
                                            precisionFocus = Offset(moved.x.toFloat(), moved.y.toFloat())
                                        }
                                        is CropDragTarget.Edge -> {
                                            val (first, second) = edgeCornerIndices(target.index)
                                            val firstPoint = Point(points[first].x + dx, points[first].y + dy)
                                            val secondPoint = Point(points[second].x + dx, points[second].y + dy)
                                            onCornerMoved(first, firstPoint, containerDimensions)
                                            onCornerMoved(second, secondPoint, containerDimensions)
                                            precisionFocus = midpoint(firstPoint, secondPoint)
                                        }
                                    }

                                    lastPointer = change.position
                                    lastTime = change.uptimeMillis
                                }
                            }
                        )
                    }
            ) {
                val path = Path().apply {
                    moveTo(displayPoints[0].x.toFloat(), displayPoints[0].y.toFloat())
                    lineTo(displayPoints[1].x.toFloat(), displayPoints[1].y.toFloat())
                    lineTo(displayPoints[2].x.toFloat(), displayPoints[2].y.toFloat())
                    lineTo(displayPoints[3].x.toFloat(), displayPoints[3].y.toFloat())
                    close()
                }
                drawPath(path, Color.Black.copy(alpha = .88f), style = Stroke(width = 5.dp.toPx()))
                drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))
                drawPath(path, lineColor.copy(alpha = .10f))

                displayPoints.forEachIndexed { index, point ->
                    val center = Offset(point.x.toFloat(), point.y.toFloat())
                    val active = activeTarget == CropDragTarget.Corner(index)
                    if (active) drawCircle(lineColor.copy(alpha = .25f), 22.dp.toPx(), center)
                    drawCircle(oscanColors.workspace, (if (active) 11.dp else 8.dp).toPx(), center)
                    drawCircle(lineColor, (if (active) 11.dp else 8.dp).toPx(), center, style = Stroke(2.dp.toPx()))
                }

                displayPoints.indices.forEach { edgeIndex ->
                    val (first, second) = edgeCornerIndices(edgeIndex)
                    val center = midpoint(displayPoints[first], displayPoints[second])
                    val active = activeTarget == CropDragTarget.Edge(edgeIndex)
                    val tickLength = (if (active) 18.dp else 12.dp).toPx()
                    val vx = (displayPoints[second].x - displayPoints[first].x).toFloat()
                    val vy = (displayPoints[second].y - displayPoints[first].y).toFloat()
                    val magnitude = hypot(vx, vy).coerceAtLeast(1f)
                    val nx = -vy / magnitude
                    val ny = vx / magnitude
                    if (active) drawCircle(lineColor.copy(alpha = .25f), 20.dp.toPx(), center)
                    drawLine(
                        lineColor,
                        Offset(center.x - nx * tickLength / 2, center.y - ny * tickLength / 2),
                        Offset(center.x + nx * tickLength / 2, center.y + ny * tickLength / 2),
                        strokeWidth = (if (active) 4.dp else 3.dp).toPx()
                    )
                }

                if (precisionMode) {
                    precisionFocus?.let { focus ->
                        val loupeCenter = loupeCenter(size, focus, density.density)
                        drawLine(
                            color = lineColor.copy(alpha = .8f),
                            start = loupeCenter,
                            end = focus,
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
                        )
                    }
                }
            }

            if (precisionMode) {
                precisionFocus?.let { focus ->
                    PrecisionLoupe(
                        bitmap = previewBitmap,
                        sourceDimensions = sourceDimensions,
                        containerDimensions = containerDimensions,
                        focus = focus,
                        lineColor = lineColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PrecisionLoupe(
    bitmap: Bitmap,
    sourceDimensions: ImageDimensions,
    containerDimensions: ImageDimensions,
    focus: Offset,
    lineColor: Color
) {
    val density = LocalDensity.current
    val transform = CoordinateTransformer.computeTransform(sourceDimensions, containerDimensions)
    val sourceFocus = CoordinateTransformer.displayToSource(
        Point(focus.x.toDouble(), focus.y.toDouble()),
        transform,
        sourceDimensions
    )
    val width = 140.dp
    val imageHeight = 112.dp
    val totalHeight = 170.dp
    val widthPx = with(density) { width.toPx() }
    val heightPx = with(density) { totalHeight.toPx() }
    val marginPx = with(density) { 14.dp.toPx() }
    val placeRight = focus.y < heightPx + marginPx * 2 && focus.x < containerDimensions.width / 2f
    val x = if (placeRight) containerDimensions.width - widthPx - marginPx else marginPx
    val y = marginPx

    Surface(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(width, totalHeight),
        color = Color(0xEE111718),
        contentColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(width, imageHeight)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val sourceWidth = (size.width / (5f * transform.scale.toFloat())).roundToInt().coerceAtLeast(8)
                    val sourceHeight = (size.height / (5f * transform.scale.toFloat())).roundToInt().coerceAtLeast(8)
                    val left = (sourceFocus.x - sourceWidth / 2.0)
                        .roundToInt().coerceIn(0, (bitmap.width - sourceWidth).coerceAtLeast(0))
                    val top = (sourceFocus.y - sourceHeight / 2.0)
                        .roundToInt().coerceIn(0, (bitmap.height - sourceHeight).coerceAtLeast(0))
                    val safeWidth = sourceWidth.coerceAtMost(bitmap.width - left)
                    val safeHeight = sourceHeight.coerceAtMost(bitmap.height - top)
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        srcOffset = IntOffset(left, top),
                        srcSize = IntSize(safeWidth, safeHeight),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        filterQuality = FilterQuality.None
                    )
                    drawLine(Color.White.copy(alpha = .9f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = .9f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
                    drawLine(lineColor, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2.dp.toPx())
                    drawCircle(lineColor, 4.dp.toPx(), Offset(size.width / 2, size.height / 2))
                }
                Text(
                    "5×",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(9.dp)
                        .background(Color.Black.copy(alpha = .64f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Text(
                "Fine adjust",
                color = lineColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.size(4.dp))
            Canvas(Modifier.size(width = 112.dp, height = 20.dp)) {
                val center = size.width / 2
                repeat(15) { index ->
                    val xTick = index * size.width / 14
                    val tall = index == 7
                    drawLine(
                        if (tall) lineColor else Color.White.copy(alpha = .75f),
                        Offset(xTick, if (tall) 1.dp.toPx() else 6.dp.toPx()),
                        Offset(xTick, size.height - 2.dp.toPx()),
                        strokeWidth = if (tall) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
                drawCircle(lineColor, 2.dp.toPx(), Offset(center, size.height / 2))
            }
        }
    }
}

internal fun findCropDragTarget(
    points: List<Point>,
    touch: Offset,
    cornerThreshold: Float,
    edgeThreshold: Float
): CropDragTarget? {
    if (points.size != 4) return null
    val corner = points.indices.minByOrNull { index ->
        hypot(points[index].x - touch.x, points[index].y - touch.y)
    }
    if (corner != null && hypot(points[corner].x - touch.x, points[corner].y - touch.y) <= cornerThreshold) {
        return CropDragTarget.Corner(corner)
    }
    val edge = points.indices.minByOrNull { index ->
        val (first, second) = edgeCornerIndices(index)
        distanceToSegment(touch, points[first], points[second])
    }
    return edge?.takeIf {
        val (first, second) = edgeCornerIndices(it)
        distanceToSegment(touch, points[first], points[second]) <= edgeThreshold
    }?.let(CropDragTarget::Edge)
}

internal fun distanceToSegment(point: Offset, start: Point, end: Point): Double {
    val dx = end.x - start.x
    val dy = end.y - start.y
    if (dx == 0.0 && dy == 0.0) return hypot(point.x - start.x, point.y - start.y)
    val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
    return hypot(point.x - (start.x + t * dx), point.y - (start.y + t * dy))
}

internal fun updatedPrecisionSlowMillis(
    currentSlowMillis: Long,
    distancePx: Float,
    density: Float,
    elapsedMillis: Long
): Long {
    if (elapsedMillis <= 0L || density <= 0f) return 0L
    val speedDpPerMilli = (distancePx / density) / elapsedMillis
    return if (speedDpPerMilli <= PRECISION_MAX_DP_PER_MILLI) {
        currentSlowMillis + elapsedMillis
    } else {
        0L
    }
}

private fun edgeCornerIndices(edgeIndex: Int): Pair<Int, Int> = when (edgeIndex) {
    0 -> 0 to 1
    1 -> 1 to 2
    2 -> 2 to 3
    else -> 3 to 0
}

private fun midpoint(first: Point, second: Point): Offset = Offset(
    ((first.x + second.x) / 2.0).toFloat(),
    ((first.y + second.y) / 2.0).toFloat()
)

private fun loupeCenter(size: IntSize, focus: Offset, density: Float): Offset {
    val loupeWidth = 140f * density
    val loupeHeight = 170f * density
    val margin = 14f * density
    val placeRight = focus.y < loupeHeight + margin * 2 && focus.x < size.width / 2f
    val left = if (placeRight) size.width - loupeWidth - margin else margin
    return Offset(left + loupeWidth / 2, margin + loupeHeight / 2)
}
