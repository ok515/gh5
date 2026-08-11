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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.AlignVerticalCenter
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.domain.model.TextPreset
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.presentation.dialogs.FontPickerSheet
import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun TextToolbar(
    viewModel: CanvasViewModel,
    selectedTextLayer: Layer.Text?,
    onEditTextContent: (Layer.Text) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var activeToolPanel by remember { mutableStateOf<String?>(null) }
    var showFontPickerSheet by remember { mutableStateOf(false) }

    val textColors = listOf(
        0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF1E88E5.toInt(),
        0xFFE53935.toInt(), 0xFF43A047.toInt(), 0xFFFDD835.toInt(),
        0xFF8E24AA.toInt(), 0xFFFB8C00.toInt(), 0xFF00ACC1.toInt(), 0xFFD81B60.toInt()
    )

    if (showFontPickerSheet && selectedTextLayer != null) {
        FontPickerSheet(
            viewModel = viewModel,
            selectedLayer = selectedTextLayer,
            onDismiss = { showFontPickerSheet = false }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("text_toolbar"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            // Expandable adjustment controls panel above the main tool row
            if (selectedTextLayer != null && activeToolPanel != null) {
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
                            "size" -> {
                                Text("${strings.size}: ${selectedTextLayer.fontSize.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = selectedTextLayer.fontSize,
                                    onValueChange = { viewModel.updateLayer(selectedTextLayer.copy(fontSize = it)) },
                                    valueRange = 12f..200f
                                )
                            }
                            "color" -> {
                                Text(strings.color, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(textColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(color))
                                                .border(
                                                    width = if (selectedTextLayer.textColor == color) 3.dp else 1.dp,
                                                    color = if (selectedTextLayer.textColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    viewModel.updateLayer(selectedTextLayer.copy(textColor = color, isGradientEnabled = false))
                                                }
                                        )
                                    }
                                }
                            }
                            "style" -> {
                                Text(strings.style, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FilterChip(
                                        selected = selectedTextLayer.fontStyleBold,
                                        onClick = { viewModel.updateLayer(selectedTextLayer.copy(fontStyleBold = !selectedTextLayer.fontStyleBold)) },
                                        label = { Text("B") }
                                    )
                                    FilterChip(
                                        selected = selectedTextLayer.fontStyleItalic,
                                        onClick = { viewModel.updateLayer(selectedTextLayer.copy(fontStyleItalic = !selectedTextLayer.fontStyleItalic)) },
                                        label = { Text("I") }
                                    )
                                    FilterChip(
                                        selected = selectedTextLayer.underline,
                                        onClick = { viewModel.updateLayer(selectedTextLayer.copy(underline = !selectedTextLayer.underline)) },
                                        label = { Text("U") }
                                    )
                                }
                            }
                            "stroke" -> {
                                Text("${strings.stroke}: ${selectedTextLayer.strokeWidth.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = selectedTextLayer.strokeWidth,
                                    onValueChange = { viewModel.updateLayer(selectedTextLayer.copy(strokeWidth = it)) },
                                    valueRange = 0f..24f
                                )
                            }
                            "shadow" -> {
                                Text("${strings.shadow}: ${selectedTextLayer.shadowBlur.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = selectedTextLayer.shadowBlur,
                                    onValueChange = { viewModel.updateLayer(selectedTextLayer.copy(shadowBlur = it)) },
                                    valueRange = 0f..30f
                                )
                            }
                            "spacing" -> {
                                Text("${strings.spacing}: ${selectedTextLayer.letterSpacing.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = selectedTextLayer.letterSpacing,
                                    onValueChange = { viewModel.updateLayer(selectedTextLayer.copy(letterSpacing = it)) },
                                    valueRange = -2f..20f
                                )
                            }
                            "curve" -> {
                                FilterChip(
                                    selected = selectedTextLayer.isCurved,
                                    onClick = { viewModel.updateLayer(selectedTextLayer.copy(isCurved = !selectedTextLayer.isCurved)) },
                                    label = { Text(strings.curve) }
                                )
                                if (selectedTextLayer.isCurved) {
                                    Slider(
                                        value = selectedTextLayer.curveRadius,
                                        onValueChange = { viewModel.updateLayer(selectedTextLayer.copy(curveRadius = it)) },
                                        valueRange = 40f..300f
                                    )
                                }
                            }
                            "3d" -> {
                                Text("${strings.rotate3D} X: ${selectedTextLayer.rotX.toInt()}°", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = selectedTextLayer.rotX,
                                    onValueChange = { viewModel.updateLayer(selectedTextLayer.copy(rotX = it)) },
                                    valueRange = -60f..60f
                                )
                                Text("${strings.rotate3D} Y: ${selectedTextLayer.rotY.toInt()}°", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = selectedTextLayer.rotY,
                                    onValueChange = { viewModel.updateLayer(selectedTextLayer.copy(rotY = it)) },
                                    valueRange = -60f..60f
                                )
                            }
                            "reflection" -> {
                                FilterChip(
                                    selected = selectedTextLayer.reflectionEnabled,
                                    onClick = { viewModel.updateLayer(selectedTextLayer.copy(reflectionEnabled = !selectedTextLayer.reflectionEnabled)) },
                                    label = { Text(strings.reflection) }
                                )
                            }
                            "presets" -> {
                                Text(strings.presets, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(TextPreset.entries.toTypedArray()) { preset ->
                                        FilterChip(
                                            selected = selectedTextLayer.presetStyle == preset,
                                            onClick = { viewModel.updateLayer(selectedTextLayer.copy(presetStyle = preset)) },
                                            label = { Text(preset.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Scrollable Horizontal Tools Bar
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item { Spacer(modifier = Modifier.width(8.dp)) }

                // + Add Text
                item {
                    PecmiToolItem(
                        icon = Icons.Default.Add,
                        label = strings.newText,
                        onClick = { viewModel.addTextLayer(strings.addText) }
                    )
                }

                // Quotes
                item {
                    PecmiToolItem(
                        icon = Icons.Default.Style,
                        label = strings.quotes,
                        onClick = { viewModel.toggleQuotesDialog(true) }
                    )
                }

                if (selectedTextLayer != null) {
                    // Edit
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Edit,
                            label = strings.edit,
                            onClick = { onEditTextContent(selectedTextLayer) }
                        )
                    }

                    // Delete
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Delete,
                            label = strings.delete,
                            onClick = { viewModel.deleteLayer(selectedTextLayer.id) }
                        )
                    }

                    // Copy
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.ContentCopy,
                            label = strings.copy,
                            onClick = { viewModel.duplicateLayer(selectedTextLayer.id) }
                        )
                    }

                    // To Front
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Layers,
                            label = strings.toFront,
                            onClick = { viewModel.bringLayerToFront(selectedTextLayer.id) }
                        )
                    }

                    // To Back
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Layers,
                            label = strings.toBack,
                            onClick = { viewModel.sendLayerToBack(selectedTextLayer.id) }
                        )
                    }

                    // Position / Alignment
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.AlignHorizontalCenter,
                            label = strings.relativePosition,
                            onClick = {
                                viewModel.centerLayerHorizontally(selectedTextLayer.id)
                            }
                        )
                    }

                    // Size
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.FormatSize,
                            label = strings.size,
                            selected = activeToolPanel == "size",
                            onClick = { activeToolPanel = if (activeToolPanel == "size") null else "size" }
                        )
                    }

                    // Color
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Palette,
                            label = strings.color,
                            selected = activeToolPanel == "color",
                            onClick = { activeToolPanel = if (activeToolPanel == "color") null else "color" }
                        )
                    }

                    // Font
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.FontDownload,
                            label = strings.font,
                            onClick = { showFontPickerSheet = true }
                        )
                    }

                    // Style (Bold, Italic, Underline)
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.FormatBold,
                            label = strings.style,
                            selected = activeToolPanel == "style",
                            onClick = { activeToolPanel = if (activeToolPanel == "style") null else "style" }
                        )
                    }

                    // Curve
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.RotateRight,
                            label = strings.curve,
                            selected = activeToolPanel == "curve",
                            onClick = { activeToolPanel = if (activeToolPanel == "curve") null else "curve" }
                        )
                    }

                    // Spacing
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.SpaceBar,
                            label = strings.spacing,
                            selected = activeToolPanel == "spacing",
                            onClick = { activeToolPanel = if (activeToolPanel == "spacing") null else "spacing" }
                        )
                    }

                    // Stroke
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Crop,
                            label = strings.stroke,
                            selected = activeToolPanel == "stroke",
                            onClick = { activeToolPanel = if (activeToolPanel == "stroke") null else "stroke" }
                        )
                    }

                    // Shadow
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.AutoAwesome,
                            label = strings.shadow,
                            selected = activeToolPanel == "shadow",
                            onClick = { activeToolPanel = if (activeToolPanel == "shadow") null else "shadow" }
                        )
                    }

                    // 3D Rotate
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.RotateRight,
                            label = strings.rotate3D,
                            selected = activeToolPanel == "3d",
                            onClick = { activeToolPanel = if (activeToolPanel == "3d") null else "3d" }
                        )
                    }

                    // Reflection
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Flip,
                            label = strings.reflection,
                            selected = activeToolPanel == "reflection",
                            onClick = { activeToolPanel = if (activeToolPanel == "reflection") null else "reflection" }
                        )
                    }

                    // Presets
                    item {
                        PecmiToolItem(
                            icon = Icons.Default.Style,
                            label = strings.tabPresets,
                            selected = activeToolPanel == "presets",
                            onClick = { activeToolPanel = if (activeToolPanel == "presets") null else "presets" }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.width(8.dp)) }
            }
        }
    }
}

@Composable
fun PecmiToolItem(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (selected) activeColor else inactiveColor
        )
    }
}

