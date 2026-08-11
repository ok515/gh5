package com.pecmi.studio.presentation.toolbars

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.domain.model.ShapeType
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.editor.EditorBottomTab
import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun ShapeToolbar(
    viewModel: CanvasViewModel,
    selectedShapeLayer: Layer.Shape?,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var activeToolPanel by remember { mutableStateOf<String?>(null) }

    val shapeColors = listOf(
        0xFF1E88E5.toInt(), 0xFFE53935.toInt(), 0xFF43A047.toInt(),
        0xFFFDD835.toInt(), 0xFF8E24AA.toInt(), 0xFFFB8C00.toInt(),
        0xFFFFFFFF.toInt(), 0xFF000000.toInt()
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shape_toolbar"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            // Adjustments panel
            if (activeToolPanel != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (activeToolPanel) {
                            "type" -> {
                                Text(strings.addShape, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(ShapeType.entries) { shapeType ->
                                        FilterChip(
                                            selected = selectedShapeLayer?.shapeType == shapeType,
                                            onClick = {
                                                if (selectedShapeLayer != null) {
                                                    viewModel.updateLayer(selectedShapeLayer.copy(shapeType = shapeType))
                                                } else {
                                                    viewModel.addShapeLayer(shapeType)
                                                }
                                            },
                                            label = { Text(shapeType.name, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                            "color" -> {
                                if (selectedShapeLayer != null) {
                                    Text(strings.color, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(shapeColors) { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(color))
                                                    .border(
                                                        width = if (selectedShapeLayer.fillColor == color) 3.dp else 1.dp,
                                                        color = if (selectedShapeLayer.fillColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        viewModel.updateLayer(selectedShapeLayer.copy(fillColor = color))
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                            "stroke" -> {
                                if (selectedShapeLayer != null) {
                                    Text("${strings.stroke}: ${selectedShapeLayer.strokeWidth.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = selectedShapeLayer.strokeWidth,
                                        onValueChange = { viewModel.updateLayer(selectedShapeLayer.copy(strokeWidth = it)) },
                                        valueRange = 0f..30f
                                    )
                                }
                            }
                            "radius" -> {
                                if (selectedShapeLayer != null) {
                                    Text("${strings.cornerRadius}: ${selectedShapeLayer.cornerRadius.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = selectedShapeLayer.cornerRadius,
                                        onValueChange = { viewModel.updateLayer(selectedShapeLayer.copy(cornerRadius = it)) },
                                        valueRange = 0f..100f
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Pecmi Object Tools Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item { Spacer(modifier = Modifier.width(8.dp)) }

                item {
                    PecmiToolItem(
                        icon = Icons.Default.Pets,
                        label = strings.stickers,
                        onClick = {
                            viewModel.toggleStickerSheet(true)
                        }
                    )
                }

                item {
                    PecmiToolItem(
                        icon = Icons.Default.Image,
                        label = strings.gallery,
                        onClick = {
                            viewModel.selectTab(EditorBottomTab.IMAGES)
                        }
                    )
                }

                item {
                    PecmiToolItem(
                        icon = Icons.Default.Brush,
                        label = strings.draw,
                        onClick = {
                            viewModel.selectTab(EditorBottomTab.DRAW)
                        }
                    )
                }

                item {
                    PecmiToolItem(
                        icon = Icons.Default.Category,
                        label = strings.tabShapes,
                        selected = activeToolPanel == "type",
                        onClick = {
                            if (selectedShapeLayer == null) {
                                viewModel.addShapeLayer(ShapeType.RECTANGLE)
                            }
                            activeToolPanel = if (activeToolPanel == "type") null else "type"
                        }
                    )
                }

                item {
                    PecmiToolItem(
                        icon = Icons.Default.Polyline,
                        label = strings.bezier,
                        onClick = {
                            viewModel.addShapeLayer(ShapeType.TRIANGLE)
                        }
                    )
                }

                item {
                    PecmiToolItem(
                        icon = Icons.Default.TrendingFlat,
                        label = strings.arrow,
                        onClick = {
                            viewModel.addShapeLayer(ShapeType.ARROW)
                        }
                    )
                }

                if (selectedShapeLayer != null) {
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Palette,
                            label = strings.color,
                            selected = activeToolPanel == "color",
                            onClick = { activeToolPanel = if (activeToolPanel == "color") null else "color" }
                        )
                    }

                    item {
                        PecmiToolItem(
                            icon = Icons.Default.CropSquare,
                            label = strings.stroke,
                            selected = activeToolPanel == "stroke",
                            onClick = { activeToolPanel = if (activeToolPanel == "stroke") null else "stroke" }
                        )
                    }

                    item {
                        PecmiToolItem(
                            icon = Icons.Default.CropSquare,
                            label = strings.cornerRadius,
                            selected = activeToolPanel == "radius",
                            onClick = { activeToolPanel = if (activeToolPanel == "radius") null else "radius" }
                        )
                    }

                    item {
                        PecmiToolItem(
                            icon = Icons.Default.ContentCopy,
                            label = strings.copy,
                            onClick = { viewModel.duplicateLayer(selectedShapeLayer.id) }
                        )
                    }

                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Delete,
                            label = strings.delete,
                            onClick = { viewModel.deleteLayer(selectedShapeLayer.id) }
                        )
                    }

                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Layers,
                            label = strings.toFront,
                            onClick = { viewModel.bringLayerToFront(selectedShapeLayer.id) }
                        )
                    }

                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Layers,
                            label = strings.toBack,
                            onClick = { viewModel.sendLayerToBack(selectedShapeLayer.id) }
                        )
                    }

                    item {
                        PecmiToolItem(
                            icon = Icons.Default.AlignHorizontalCenter,
                            label = strings.relativePosition,
                            onClick = { viewModel.centerLayerHorizontally(selectedShapeLayer.id) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.width(8.dp)) }
            }
        }
    }
}

