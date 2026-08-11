package com.pecmi.studio.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.domain.model.CanvasSettings
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.editor.EditorBottomTab
import com.pecmi.studio.graphics.CanvasRenderer
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private enum class ActiveHandleMode {
    NONE, MOVE_BODY, ROTATE, SCALE_TOP_LEFT, SCALE_TOP_RIGHT, SCALE_BOTTOM_LEFT, SCALE_BOTTOM_RIGHT, RESIZE_LEFT, RESIZE_RIGHT, RESIZE_TOP, RESIZE_BOTTOM, PAN_CANVAS
}

@Composable
fun CanvasView(
    viewModel: CanvasViewModel,
    settings: CanvasSettings,
    layers: List<Layer>,
    selectedLayerId: String?,
    isZoomMode: Boolean,
    onDoubleTapLayer: (Layer) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val uiState by viewModel.uiState.collectAsState()
    val isDrawTabActive = uiState.activeBottomTab == EditorBottomTab.DRAW
    val selectedLayer = layers.find { it.id == selectedLayerId }
    val isDrawingLayerSelected = selectedLayer is Layer.Drawing

    // Infinite Canvas viewport state
    var viewPan by remember { mutableStateOf(Offset.Zero) }
    var viewZoomScale by remember { mutableFloatStateOf(1.0f) }

    // Live tooltip info pill state
    var activeTransformTooltip by remember { mutableStateOf<String?>(null) }

    // Snap alignment guide indicators
    var snapGuideVerticals by remember { mutableStateOf<List<Float>>(emptyList()) }
    var snapGuideHorizontals by remember { mutableStateOf<List<Float>>(emptyList()) }

    // Gesture tracking state
    var activeHandleMode by remember { mutableStateOf(ActiveHandleMode.NONE) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapLayerId by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .testTag("canvas_view_container"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        val pageW = settings.width.toFloat().coerceAtLeast(100f)
        val pageH = settings.height.toFloat().coerceAtLeast(100f)

        val vpCenterX = containerWidthPx / 2f
        val vpCenterY = containerHeightPx / 2f

        // Base fit zoom to maximize design paper inside viewport
        val fitScale = remember(containerWidthPx, containerHeightPx, pageW, pageH) {
            val fitW = (containerWidthPx - 24f) / pageW
            val fitH = (containerHeightPx - 24f) / pageH
            minOf(fitW, fitH).coerceIn(0.15f, 4.0f)
        }

        val effectiveZoom = (fitScale * viewZoomScale).coerceIn(0.05f, 15.0f)

        val currentLayers by rememberUpdatedState(layers)
        val currentSelectedLayerId by rememberUpdatedState(selectedLayerId)
        val currentIsZoomMode by rememberUpdatedState(isZoomMode)
        val currentIsDrawTabActive by rememberUpdatedState(isDrawTabActive)
        val currentEffectiveZoom by rememberUpdatedState(effectiveZoom)
        val currentViewPan by rememberUpdatedState(viewPan)
        val currentPageW by rememberUpdatedState(pageW)
        val currentPageH by rememberUpdatedState(pageH)
        val currentVpCenterX by rememberUpdatedState(vpCenterX)
        val currentVpCenterY by rememberUpdatedState(vpCenterY)

        // World <-> Screen Helpers
        fun screenToWorld(screenOffset: Offset): Offset {
            val wx = (screenOffset.x - currentVpCenterX - currentViewPan.x) / currentEffectiveZoom + currentPageW / 2f
            val wy = (screenOffset.y - currentVpCenterY - currentViewPan.y) / currentEffectiveZoom + currentPageH / 2f
            return Offset(wx, wy)
        }

        fun worldToScreen(worldOffset: Offset): Offset {
            val sx = (worldOffset.x - currentPageW / 2f) * currentEffectiveZoom + currentVpCenterX + currentViewPan.x
            val sy = (worldOffset.y - currentPageH / 2f) * currentEffectiveZoom + currentVpCenterY + currentViewPan.y
            return Offset(sx, sy)
        }

        fun isPointInsideLayer(worldPos: Offset, layer: Layer): Boolean {
            val (w, h) = CanvasRenderer.getLayerDimensions(layer)
            val (centerX, centerY) = CanvasRenderer.getLayerCenter(layer)
            val halfW = (w * layer.scaleX) / 2f
            val halfH = (h * layer.scaleY) / 2f
            val touchMargin = if (layer is Layer.Text) 64f else 48f

            val dx = worldPos.x - centerX
            val dy = worldPos.y - centerY

            val rad = -layer.rotation * (PI / 180.0)
            val localX = dx * cos(rad) - dy * sin(rad)
            val localY = dx * sin(rad) + dy * cos(rad)

            val minHalfW = if (layer is Layer.Text) 64f else 40f
            val minHalfH = if (layer is Layer.Text) 64f else 40f

            val effectiveHalfW = maxOf(halfW + touchMargin, minHalfW)
            val effectiveHalfH = maxOf(halfH + touchMargin, minHalfH)

            return Math.abs(localX) <= effectiveHalfW && Math.abs(localY) <= effectiveHalfH
        }

        fun checkHandleHit(worldPos: Offset, layer: Layer, effZoom: Float): ActiveHandleMode {
            val (w, h) = CanvasRenderer.getLayerDimensions(layer)
            val (centerX, centerY) = CanvasRenderer.getLayerCenter(layer)
            val halfW = (w * layer.scaleX) / 2f + 12f
            val halfH = (h * layer.scaleY) / 2f + 12f

            val effectiveScale = effZoom.coerceAtLeast(0.2f)
            val rotationStemLen = (36f / effectiveScale).coerceIn(20f, 90f)

            val dx = worldPos.x - centerX
            val dy = worldPos.y - centerY

            val rad = -layer.rotation * (PI / 180.0)
            val localX = (dx * cos(rad) - dy * sin(rad)).toFloat()
            val localY = (dx * sin(rad) + dy * cos(rad)).toFloat()

            val handleRadiusThreshold = (48f / effectiveScale).coerceIn(32f, 80f)

            // Rotation Stem
            val rotDist = hypot(localX - 0f, localY - (-halfH - rotationStemLen))
            if (rotDist <= handleRadiusThreshold) return ActiveHandleMode.ROTATE

            // Corner Handles
            val tlDist = hypot(localX - (-halfW), localY - (-halfH))
            if (tlDist <= handleRadiusThreshold) return ActiveHandleMode.SCALE_TOP_LEFT

            val trDist = hypot(localX - halfW, localY - (-halfH))
            if (trDist <= handleRadiusThreshold) return ActiveHandleMode.SCALE_TOP_RIGHT

            val blDist = hypot(localX - (-halfW), localY - halfH)
            if (blDist <= handleRadiusThreshold) return ActiveHandleMode.SCALE_BOTTOM_LEFT

            val brDist = hypot(localX - halfW, localY - halfH)
            if (brDist <= handleRadiusThreshold) return ActiveHandleMode.SCALE_BOTTOM_RIGHT

            // Side Handles
            val leftDist = hypot(localX - (-halfW), localY - 0f)
            if (leftDist <= handleRadiusThreshold) return ActiveHandleMode.RESIZE_LEFT

            val rightDist = hypot(localX - halfW, localY - 0f)
            if (rightDist <= handleRadiusThreshold) return ActiveHandleMode.RESIZE_RIGHT

            val topDist = hypot(localX - 0f, localY - (-halfH))
            if (topDist <= handleRadiusThreshold) return ActiveHandleMode.RESIZE_TOP

            val bottomDist = hypot(localX - 0f, localY - halfH)
            if (bottomDist <= handleRadiusThreshold) return ActiveHandleMode.RESIZE_BOTTOM

            return ActiveHandleMode.NONE
        }

        // Main Gesture Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val touchScreenOffset = down.position
                        val worldPos = screenToWorld(touchScreenOffset)

                        if (currentIsZoomMode) {
                            // Canvas Zoom & Pan mode
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break

                                if (pressed.size >= 2) {
                                    val p1 = pressed[0].position
                                    val p2 = pressed[1].position
                                    val prevP1 = pressed[0].previousPosition
                                    val prevP2 = pressed[1].previousPosition

                                    val currentDist = hypot(p1.x - p2.x, p1.y - p2.y)
                                    val prevDist = hypot(prevP1.x - prevP2.x, prevP1.y - prevP2.y)
                                    if (prevDist > 1f && currentDist > 1f) {
                                        val zoomFactor = currentDist / prevDist
                                        viewZoomScale = (viewZoomScale * zoomFactor).coerceIn(0.1f, 8.0f)
                                    }

                                    val panDx = (p1.x - prevP1.x + p2.x - prevP2.x) / 2f
                                    val panDy = (p1.y - prevP1.y + p2.y - prevP2.y) / 2f
                                    viewPan += Offset(panDx, panDy)
                                } else if (pressed.size == 1) {
                                    val p = pressed[0]
                                    val panDelta = p.position - p.previousPosition
                                    viewPan += panDelta
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } else if (currentIsDrawTabActive) {
                            // Freehand draw stroke mode
                            viewModel.startDrawingStroke(worldPos.x, worldPos.y)
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break
                                for (change in pressed) {
                                    val ptWorld = screenToWorld(change.position)
                                    viewModel.appendDrawingPoint(ptWorld.x, ptWorld.y)
                                    change.consume()
                                }
                            }
                            viewModel.finishDrawingStroke()
                        } else {
                            // Pecmi layer interaction mode
                            val selLayer = currentLayers.find { it.id == currentSelectedLayerId }
                            var hitHandle = ActiveHandleMode.NONE

                            if (selLayer != null && !selLayer.isLocked) {
                                hitHandle = checkHandleHit(worldPos, selLayer, currentEffectiveZoom)
                            }

                            var hitLayer: Layer? = null
                            if (hitHandle != ActiveHandleMode.NONE) {
                                activeHandleMode = hitHandle
                                hitLayer = selLayer
                            } else {
                                for (layer in currentLayers.reversed()) {
                                    if (!layer.isVisible || layer.isLocked) continue
                                    if (isPointInsideLayer(worldPos, layer)) {
                                        hitLayer = layer
                                        break
                                    }
                                }

                                if (hitLayer != null) {
                                    if (currentSelectedLayerId != hitLayer.id) {
                                        viewModel.selectLayer(hitLayer.id)
                                    }
                                    activeHandleMode = ActiveHandleMode.MOVE_BODY

                                    val now = System.currentTimeMillis()
                                    if (hitLayer is Layer.Text && (now - lastTapTime) < 300L && lastTapLayerId == hitLayer.id) {
                                        onDoubleTapLayer(hitLayer)
                                    }
                                    lastTapTime = now
                                    lastTapLayerId = hitLayer.id
                                } else {
                                    // Touch on background deselects elements without moving canvas
                                    viewModel.selectLayer(null)
                                    activeHandleMode = ActiveHandleMode.NONE
                                }
                            }

                            if (activeHandleMode != ActiveHandleMode.NONE && hitLayer != null) {
                                down.consume()
                                var lastWorldPos = worldPos
                                var dragX = hitLayer.x
                                var dragY = hitLayer.y

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.isEmpty()) break

                                        if (pressed.size >= 2) {
                                            val p1 = screenToWorld(pressed[0].position)
                                            val p2 = screenToWorld(pressed[1].position)
                                            val prevP1 = screenToWorld(pressed[0].previousPosition)
                                            val prevP2 = screenToWorld(pressed[1].previousPosition)

                                            val prevDist = hypot(prevP1.x - prevP2.x, prevP1.y - prevP2.y).coerceAtLeast(1f)
                                            val currDist = hypot(p1.x - p2.x, p1.y - p2.y).coerceAtLeast(1f)
                                            val scaleFactor = currDist / prevDist

                                            val prevAngle = atan2(prevP2.y - prevP1.y, prevP2.x - prevP1.x)
                                            val currAngle = atan2(p2.y - p1.y, p2.x - p1.x)
                                            val angleDiffDeg = ((currAngle - prevAngle) * 180f / PI).toFloat()

                                            val prevCenter = Offset((prevP1.x + prevP2.x) / 2f, (prevP1.y + prevP2.y) / 2f)
                                            val currCenter = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                                            viewModel.updateSelectedLayerPosition(currCenter.x - prevCenter.x, currCenter.y - prevCenter.y, hitLayer.id)
                                            viewModel.updateSelectedLayerScale(scaleFactor, hitLayer.id)
                                            viewModel.updateSelectedLayerRotation(angleDiffDeg, hitLayer.id)

                                            pressed.forEach { it.consume() }
                                            lastWorldPos = currCenter
                                        } else {
                                            val currPointer = pressed.first()
                                            val currWorld = screenToWorld(currPointer.position)

                                            when (activeHandleMode) {
                                                ActiveHandleMode.MOVE_BODY -> {
                                                    val dx = currWorld.x - lastWorldPos.x
                                                    val dy = currWorld.y - lastWorldPos.y

                                                    dragX += dx
                                                    dragY += dy

                                                    val allLayers = viewModel.uiState.value.layers
                                                    val currSelected = allLayers.find { it.id == hitLayer.id } ?: hitLayer

                                                    val currOffsets = computeLayerRelativeOffsets(currSelected)
                                                    val snapThreshold = 14f / currentEffectiveZoom

                                                    val currPointsX = listOf(currOffsets.left, currOffsets.center, currOffsets.right)

                                                    // --- Vertical Snapping (X Axis -> Max 1 Vertical Line) ---
                                                    data class XCandidate(val delta: Float, val guideX: Float, val targetDragX: Float, val label: String)
                                                    val xCandidates = mutableListOf<XCandidate>()

                                                    // 1. Page X references
                                                    val pageXRefs = listOf(
                                                        Pair(pageW / 2f, "منتصف الصفحة أفقياً"),
                                                        Pair(0f, "حافة الصفحة اليسرى"),
                                                        Pair(pageW, "حافة الصفحة اليمنى")
                                                    )
                                                    for ((refX, label) in pageXRefs) {
                                                        for (offset in currPointsX) {
                                                            val currX = dragX + offset
                                                            val delta = kotlin.math.abs(currX - refX)
                                                            if (delta < snapThreshold) {
                                                                val targetDragX = refX - offset
                                                                xCandidates.add(XCandidate(delta, refX, targetDragX, label))
                                                            }
                                                        }
                                                    }

                                                    // 2. Other Layers X references
                                                    for (other in allLayers) {
                                                        if (other.id == currSelected.id || !other.isVisible) continue
                                                        val otherOffsets = computeLayerRelativeOffsets(other)
                                                        val otherXRefs = listOf(
                                                            Pair(other.x + otherOffsets.center, "محاذاة المنتصف أفقياً"),
                                                            Pair(other.x + otherOffsets.left, "محاذاة الحافة اليسرى"),
                                                            Pair(other.x + otherOffsets.right, "محاذاة الحافة اليمنى")
                                                        )
                                                        for ((refX, label) in otherXRefs) {
                                                            for (offset in currPointsX) {
                                                                val currX = dragX + offset
                                                                val delta = kotlin.math.abs(currX - refX)
                                                                if (delta < snapThreshold) {
                                                                    val targetDragX = refX - offset
                                                                    xCandidates.add(XCandidate(delta, refX, targetDragX, label))
                                                                }
                                                            }
                                                        }
                                                    }

                                                    val bestX = xCandidates.minByOrNull { it.delta }
                                                    var targetDragX = dragX
                                                    var tooltipX: String? = null

                                                    if (bestX != null) {
                                                        targetDragX = bestX.targetDragX
                                                        snapGuideVerticals = listOf(bestX.guideX)
                                                        tooltipX = bestX.label
                                                    } else {
                                                        snapGuideVerticals = emptyList()
                                                    }

                                                    // --- Horizontal Snapping (Y Axis -> Max 1 Horizontal Line) ---
                                                    val currPointsY = listOf(currOffsets.top, currOffsets.middle, currOffsets.bottom)
                                                    data class YCandidate(val delta: Float, val guideY: Float, val targetDragY: Float, val label: String)
                                                    val yCandidates = mutableListOf<YCandidate>()

                                                    // 1. Page Y references
                                                    val pageYRefs = listOf(
                                                        Pair(pageH / 2f, "منتصف الصفحة عمودياً"),
                                                        Pair(0f, "حافة الصفحة العليا"),
                                                        Pair(pageH, "حافة الصفحة السفلى")
                                                    )
                                                    for ((refY, label) in pageYRefs) {
                                                        for (offset in currPointsY) {
                                                            val currY = dragY + offset
                                                            val delta = kotlin.math.abs(currY - refY)
                                                            if (delta < snapThreshold) {
                                                                val targetDragY = refY - offset
                                                                yCandidates.add(YCandidate(delta, refY, targetDragY, label))
                                                            }
                                                        }
                                                    }

                                                    // 2. Other Layers Y references
                                                    for (other in allLayers) {
                                                        if (other.id == currSelected.id || !other.isVisible) continue
                                                        val otherOffsets = computeLayerRelativeOffsets(other)
                                                        val otherYRefs = listOf(
                                                            Pair(other.y + otherOffsets.middle, "محاذاة المنتصف عمودياً"),
                                                            Pair(other.y + otherOffsets.top, "محاذاة الحافة العليا"),
                                                            Pair(other.y + otherOffsets.bottom, "محاذاة الحافة السفلى")
                                                        )
                                                        for ((refY, label) in otherYRefs) {
                                                            for (offset in currPointsY) {
                                                                val currY = dragY + offset
                                                                val delta = kotlin.math.abs(currY - refY)
                                                                if (delta < snapThreshold) {
                                                                    val targetDragY = refY - offset
                                                                    yCandidates.add(YCandidate(delta, refY, targetDragY, label))
                                                                }
                                                            }
                                                        }
                                                    }

                                                    val bestY = yCandidates.minByOrNull { it.delta }
                                                    var targetDragY = dragY
                                                    var tooltipY: String? = null

                                                    if (bestY != null) {
                                                        targetDragY = bestY.targetDragY
                                                        snapGuideHorizontals = listOf(bestY.guideY)
                                                        tooltipY = bestY.label
                                                    } else {
                                                        snapGuideHorizontals = emptyList()
                                                    }

                                                    // Set Tooltip Text
                                                    if (bestX?.guideX == pageW / 2f && bestY?.guideY == pageH / 2f && targetDragX == pageW / 2f - currOffsets.center && targetDragY == pageH / 2f - currOffsets.middle) {
                                                        activeTransformTooltip = "منتصف الصفحة تماماً"
                                                    } else if (tooltipX != null && tooltipY != null) {
                                                        activeTransformTooltip = "$tooltipX • $tooltipY"
                                                    } else if (tooltipX != null) {
                                                        activeTransformTooltip = tooltipX
                                                    } else if (tooltipY != null) {
                                                        activeTransformTooltip = tooltipY
                                                    } else {
                                                        activeTransformTooltip = null
                                                    }

                                                    viewModel.setSelectedLayerAbsolutePosition(targetDragX, targetDragY, hitLayer.id)
                                                }
                                                ActiveHandleMode.ROTATE -> {
                                                    val currSelected = currentLayers.find { it.id == hitLayer.id }
                                                    if (currSelected != null) {
                                                        val prevAngleRad = atan2(lastWorldPos.y - currSelected.y, lastWorldPos.x - currSelected.x)
                                                        val currAngleRad = atan2(currWorld.y - currSelected.y, currWorld.x - currSelected.x)
                                                        var diff = ((currAngleRad - prevAngleRad) * 180f / PI).toFloat()

                                                        if (diff != 0f) {
                                                            viewModel.updateSelectedLayerRotation(diff, hitLayer.id)
                                                            activeTransformTooltip = "الدوران: ${currSelected.rotation.toInt()}°"
                                                        }
                                                    }
                                                }
                                                ActiveHandleMode.SCALE_TOP_LEFT,
                                                ActiveHandleMode.SCALE_TOP_RIGHT,
                                                ActiveHandleMode.SCALE_BOTTOM_LEFT,
                                                ActiveHandleMode.SCALE_BOTTOM_RIGHT -> {
                                                    val currSelected = currentLayers.find { it.id == hitLayer.id }
                                                    if (currSelected != null) {
                                                        val prevDist = hypot(lastWorldPos.x - currSelected.x, lastWorldPos.y - currSelected.y).coerceAtLeast(1f)
                                                        val newDist = hypot(currWorld.x - currSelected.x, currWorld.y - currSelected.y).coerceAtLeast(1f)
                                                        val scaleRatio = newDist / prevDist

                                                        if (scaleRatio != 1f) {
                                                            viewModel.updateSelectedLayerScale(scaleRatio, hitLayer.id)
                                                            val (w, h) = CanvasRenderer.getLayerDimensions(currSelected)
                                                            activeTransformTooltip = "الحجم: ${(w * currSelected.scaleX).toInt()} × ${(h * currSelected.scaleY).toInt()} px"
                                                        }
                                                    }
                                                }
                                                ActiveHandleMode.RESIZE_LEFT,
                                                ActiveHandleMode.RESIZE_RIGHT -> {
                                                    val currSelected = currentLayers.find { it.id == hitLayer.id }
                                                    if (currSelected != null) {
                                                        val dx = currWorld.x - lastWorldPos.x
                                                        val dy = currWorld.y - lastWorldPos.y
                                                        val rad = -currSelected.rotation * (PI / 180.0)
                                                        val localDx = (dx * cos(rad) - dy * sin(rad)).toFloat()

                                                        if (currSelected is Layer.Text) {
                                                            val widthChange = if (activeHandleMode == ActiveHandleMode.RESIZE_RIGHT) localDx * 2f else -localDx * 2f
                                                            val newWidth = (currSelected.boxWidth + widthChange).coerceIn(80f, 3000f)
                                                            viewModel.updateLayer(currSelected.copy(boxWidth = newWidth))
                                                            activeTransformTooltip = "العرض: ${newWidth.toInt()} px"
                                                        } else if (currSelected is Layer.Shape) {
                                                            val widthChange = if (activeHandleMode == ActiveHandleMode.RESIZE_RIGHT) localDx * 2f else -localDx * 2f
                                                            val newWidth = (currSelected.width + widthChange).coerceIn(20f, 3000f)
                                                            viewModel.updateLayer(currSelected.copy(width = newWidth))
                                                            activeTransformTooltip = "العرض: ${newWidth.toInt()} px"
                                                        } else {
                                                            val scaleChange = if (activeHandleMode == ActiveHandleMode.RESIZE_RIGHT) 1f + (localDx / 200f) else 1f - (localDx / 200f)
                                                            viewModel.updateSelectedLayerScale(scaleChange, hitLayer.id)
                                                        }
                                                    }
                                                }
                                                ActiveHandleMode.RESIZE_TOP,
                                                ActiveHandleMode.RESIZE_BOTTOM -> {
                                                    val currSelected = currentLayers.find { it.id == hitLayer.id }
                                                    if (currSelected != null) {
                                                        val dx = currWorld.x - lastWorldPos.x
                                                        val dy = currWorld.y - lastWorldPos.y
                                                        val rad = -currSelected.rotation * (PI / 180.0)
                                                        val localDy = (dx * sin(rad) + dy * cos(rad)).toFloat()

                                                        if (currSelected is Layer.Shape) {
                                                            val heightChange = if (activeHandleMode == ActiveHandleMode.RESIZE_BOTTOM) localDy * 2f else -localDy * 2f
                                                            val newHeight = (currSelected.height + heightChange).coerceIn(20f, 3000f)
                                                            viewModel.updateLayer(currSelected.copy(height = newHeight))
                                                            activeTransformTooltip = "الارتفاع: ${newHeight.toInt()} px"
                                                        } else {
                                                            val scaleChange = if (activeHandleMode == ActiveHandleMode.RESIZE_BOTTOM) 1f + (localDy / 200f) else 1f - (localDy / 200f)
                                                            viewModel.updateSelectedLayerScale(scaleChange, hitLayer.id)
                                                        }
                                                    }
                                                }
                                                else -> {}
                                            }
                                            lastWorldPos = currWorld
                                            currPointer.consume()
                                        }
                                    }
                                } finally {
                                    activeHandleMode = ActiveHandleMode.NONE
                                    snapGuideVerticals = emptyList()
                                    snapGuideHorizontals = emptyList()
                                    activeTransformTooltip = null
                                }
                            }
                        }
                    }
                }
        ) {
            // Infinite Workspace Canvas Renderer
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("interactive_canvas_surface")
            ) {
                val canvasWidthPx = pageW
                val canvasHeightPx = pageH

                // Translate & Scale view to fit infinite workspace around export paper bounds
                val matrix = android.graphics.Matrix()
                matrix.postTranslate(-canvasWidthPx / 2f, -canvasHeightPx / 2f)
                matrix.postScale(effectiveZoom, effectiveZoom)
                matrix.postTranslate(vpCenterX + viewPan.x, vpCenterY + viewPan.y)

                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    nativeCanvas.save()
                    nativeCanvas.concat(matrix)

                    val pageRect = android.graphics.RectF(0f, 0f, canvasWidthPx, canvasHeightPx)

                    // 0. Draw subtle outer canvas paper shadow
                    val shadowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0x33000000
                        style = android.graphics.Paint.Style.FILL
                        maskFilter = android.graphics.BlurMaskFilter(16f / effectiveZoom, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }
                    val shadowRect = android.graphics.RectF(
                        pageRect.left + 4f / effectiveZoom,
                        pageRect.top + 6f / effectiveZoom,
                        pageRect.right + 12f / effectiveZoom,
                        pageRect.bottom + 12f / effectiveZoom
                    )
                    nativeCanvas.drawRect(shadowRect, shadowPaint)

                    // 1. CLIP ALL LAYER DRAWINGS STRICTLY TO THE CANVAS FRAME RECTANGLE
                    nativeCanvas.clipRect(pageRect)

                    // 2. Draw Export Paper Bounds Background
                    val paperBgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = settings.backgroundColor
                        style = android.graphics.Paint.Style.FILL
                    }

                    if (settings.isTransparent) {
                        CanvasRenderer.drawCheckerboard(this, canvasWidthPx, canvasHeightPx)
                    } else {
                        nativeCanvas.drawRect(pageRect, paperBgPaint)
                    }

                    // 3. Draw Grid & Guidelines inside export bounds
                    if (settings.showGrid) {
                        CanvasRenderer.drawGrid(this, canvasWidthPx, canvasHeightPx, settings.gridSize.toFloat())
                    }
                    if (settings.showGuidelines) {
                        CanvasRenderer.drawGuidelines(this, canvasWidthPx, canvasHeightPx)
                    }

                    // 4. Render Layers
                    for (layer in layers) {
                        val isSelected = layer.id == selectedLayerId
                        CanvasRenderer.drawLayer(this, layer, isSelected = isSelected, viewScale = effectiveZoom)
                    }

                    // 6. Draw active drawing stroke live
                    uiState.activeCurrentStroke?.let { activeStroke ->
                        CanvasRenderer.drawSingleStroke(nativeCanvas, activeStroke)
                    }

                    // 7. Draw Export Bounds Outline Border
                    val paperBorderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFF8B5CF6.toInt()
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 2f / effectiveZoom
                    }
                    nativeCanvas.drawRect(pageRect, paperBorderPaint)

                    // 8. Draw Smart Alignment Snapping Guide Lines
                    if (snapGuideVerticals.isNotEmpty() || snapGuideHorizontals.isNotEmpty()) {
                        val guidePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFFEC4899.toInt() // Vibrant Pink
                            strokeWidth = 3.5f / effectiveZoom
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f / effectiveZoom, 8f / effectiveZoom), 0f)
                        }

                        snapGuideVerticals.forEach { vx ->
                            nativeCanvas.drawLine(vx, 0f, vx, canvasHeightPx, guidePaint)
                        }
                        snapGuideHorizontals.forEach { hy ->
                            nativeCanvas.drawLine(0f, hy, canvasWidthPx, hy, guidePaint)
                        }
                    }

                    nativeCanvas.restore()
                }
            }

            // Floating Active Transform Tooltip Pill
            AnimatedVisibility(
                visible = activeTransformTooltip != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = activeTransformTooltip ?: "",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Floating Canvas Zoom & Reset Controls Panel
            Surface(
                shape = CircleShape,
                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 76.dp, end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = { viewZoomScale = (viewZoomScale * 1.25f).coerceAtMost(8.0f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "${(viewZoomScale * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { viewZoomScale = (viewZoomScale / 1.25f).coerceAtLeast(0.1f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewPan = Offset.Zero
                            viewZoomScale = 1.0f
                            viewModel.showToast("تم إعادة ضبط عرض اللوحة")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.CenterFocusWeak,
                            contentDescription = "Reset View",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private data class LayerRelativeOffsets(
    val left: Float,
    val center: Float,
    val right: Float,
    val top: Float,
    val middle: Float,
    val bottom: Float
)

private fun computeLayerRelativeOffsets(layer: Layer): LayerRelativeOffsets {
    return when (layer) {
        is Layer.Text, is Layer.Shape, is Layer.Image -> {
            val (w, h) = CanvasRenderer.getLayerDimensions(layer)
            val halfW = (w * layer.scaleX) / 2f
            val halfH = (h * layer.scaleY) / 2f

            if (layer.rotation != 0f) {
                val rad = layer.rotation * (Math.PI / 180.0)
                val cosA = kotlin.math.cos(rad)
                val sinA = kotlin.math.sin(rad)

                val cornersX = floatArrayOf(-halfW, halfW, -halfW, halfW)
                val cornersY = floatArrayOf(-halfH, -halfH, halfH, halfH)

                var minX = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE

                for (i in 0 until 4) {
                    val wx = (cornersX[i] * cosA - cornersY[i] * sinA).toFloat()
                    val wy = (cornersX[i] * sinA + cornersY[i] * cosA).toFloat()
                    if (wx < minX) minX = wx
                    if (wx > maxX) maxX = wx
                    if (wy < minY) minY = wy
                    if (wy > maxY) maxY = wy
                }

                LayerRelativeOffsets(
                    left = minX,
                    center = 0f,
                    right = maxX,
                    top = minY,
                    middle = 0f,
                    bottom = maxY
                )
            } else {
                LayerRelativeOffsets(
                    left = -halfW,
                    center = 0f,
                    right = halfW,
                    top = -halfH,
                    middle = 0f,
                    bottom = halfH
                )
            }
        }
        is Layer.Drawing -> {
            if (layer.strokes.isEmpty()) {
                LayerRelativeOffsets(-100f * layer.scaleX, 0f, 100f * layer.scaleX, -100f * layer.scaleY, 0f, 100f * layer.scaleY)
            } else {
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                for (stroke in layer.strokes) {
                    for (pt in stroke.points) {
                        if (pt.x < minX) minX = pt.x
                        if (pt.y < minY) minY = pt.y
                        if (pt.x > maxX) maxX = pt.x
                        if (pt.y > maxY) maxY = pt.y
                    }
                }
                if (minX == Float.MAX_VALUE) {
                    LayerRelativeOffsets(-100f * layer.scaleX, 0f, 100f * layer.scaleX, -100f * layer.scaleY, 0f, 100f * layer.scaleY)
                } else {
                    val scaledMinX = minX * layer.scaleX
                    val scaledMaxX = maxX * layer.scaleX
                    val scaledMinY = minY * layer.scaleY
                    val scaledMaxY = maxY * layer.scaleY

                    if (layer.rotation != 0f) {
                        val rad = layer.rotation * (Math.PI / 180.0)
                        val cosA = kotlin.math.cos(rad)
                        val sinA = kotlin.math.sin(rad)

                        val cornersX = floatArrayOf(scaledMinX, scaledMaxX, scaledMinX, scaledMaxX)
                        val cornersY = floatArrayOf(scaledMinY, scaledMinY, scaledMaxY, scaledMaxY)

                        var rMinX = Float.MAX_VALUE
                        var rMaxX = -Float.MAX_VALUE
                        var rMinY = Float.MAX_VALUE
                        var rMaxY = -Float.MAX_VALUE

                        for (i in 0 until 4) {
                            val wx = (cornersX[i] * cosA - cornersY[i] * sinA).toFloat()
                            val wy = (cornersX[i] * sinA + cornersY[i] * cosA).toFloat()
                            if (wx < rMinX) rMinX = wx
                            if (wx > rMaxX) rMaxX = wx
                            if (wy < rMinY) rMinY = wy
                            if (wy > rMaxY) rMaxY = wy
                        }

                        val localCX = (scaledMinX + scaledMaxX) / 2f
                        val localCY = (scaledMinY + scaledMaxY) / 2f
                        val rCenterX = (localCX * cosA - localCY * sinA).toFloat()
                        val rCenterY = (localCX * sinA + localCY * cosA).toFloat()

                        LayerRelativeOffsets(
                            left = rMinX,
                            center = rCenterX,
                            right = rMaxX,
                            top = rMinY,
                            middle = rCenterY,
                            bottom = rMaxY
                        )
                    } else {
                        LayerRelativeOffsets(
                            left = scaledMinX,
                            center = (scaledMinX + scaledMaxX) / 2f,
                            right = scaledMaxX,
                            top = scaledMinY,
                            middle = (scaledMinY + scaledMaxY) / 2f,
                            bottom = scaledMaxY
                        )
                    }
                }
            }
        }
    }
}

