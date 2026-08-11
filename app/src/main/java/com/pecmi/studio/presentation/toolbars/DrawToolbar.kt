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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.domain.model.ImageArtFilter
import com.pecmi.studio.domain.model.ImageOverlayEffect
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun DrawToolbar(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val selectedLayer = uiState.layers.find { it.id == uiState.selectedLayerId }
    val imageLayer = selectedLayer as? Layer.Image

    val drawColors = listOf(
        0xFF6366F1.toInt(), 0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt(),
        0xFFEC4899.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(),
        0xFFEF4444.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt()
    )

    val brushTypes: List<Pair<String, String>> = listOf(
        "SOFT_PEN" to "✏️ Soft Pen",
        "CALLIGRAPHY" to "🖋️ Calligraphy",
        "MARKER" to "🖊️ Marker",
        "AIR_BRUSH" to "💨 Airbrush",
        "NEON" to "💡 Neon",
        "GLOW" to "✨ Glow",
        "TEXTURE" to "🎨 Texture"
    )

    val artFilterList: List<Pair<ImageArtFilter, String>> = listOf(
        ImageArtFilter.NONE to "None",
        ImageArtFilter.HDR to "HDR",
        ImageArtFilter.SKETCH to "Sketch",
        ImageArtFilter.CARTOON to "Cartoon",
        ImageArtFilter.OIL_PAINT to "Oil Paint",
        ImageArtFilter.PENCIL to "Pencil",
        ImageArtFilter.PIXELATE to "Pixelate",
        ImageArtFilter.MOSAIC to "Mosaic",
        ImageArtFilter.BLOOM to "Bloom"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("effects_draw_toolbar"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Category Tabs Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${strings.draw} ✏️", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.stroke} 🖌️", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Filters 🎨", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("${strings.adjustments} 🎛️", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("${strings.tabEffects} ✨", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    // TAB 0: Drawing
                    0 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !uiState.isEraserMode,
                                    onClick = { viewModel.setEraserMode(false) },
                                    label = { Text("✏️ ${strings.draw}") }
                                )
                                FilterChip(
                                    selected = uiState.isEraserMode,
                                    onClick = { viewModel.setEraserMode(true) },
                                    label = { Text("🧹 Eraser") }
                                )
                            }

                            AssistChip(
                                onClick = { viewModel.clearDrawingLayer() },
                                label = { Text(strings.delete) },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                            )
                        }

                        if (!uiState.isEraserMode) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(drawColors) { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(color))
                                            .border(
                                                width = if (uiState.drawColor == color) 3.dp else 1.dp,
                                                color = if (uiState.drawColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.setDrawColor(color) }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${strings.stroke}: ${uiState.drawBrushSize.toInt()}px", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = uiState.drawBrushSize,
                                onValueChange = { viewModel.setDrawBrushSize(it) },
                                valueRange = 2f..100f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // TAB 1: Brushes
                    1 -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(brushTypes) { item ->
                                FilterChip(
                                    selected = uiState.activeBrushType == item.first,
                                    onClick = { viewModel.setBrushType(item.first) },
                                    label = { Text(item.second, fontSize = 11.sp) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${strings.opacity}: ${(uiState.brushOpacity * 100).toInt()}%", fontSize = 11.sp, modifier = Modifier.width(130.dp))
                            Slider(
                                value = uiState.brushOpacity,
                                onValueChange = { viewModel.setBrushOpacity(it) },
                                valueRange = 0.1f..1.0f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // TAB 2: Filters
                    2 -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(artFilterList) { item ->
                                FilterChip(
                                    selected = imageLayer?.artFilter == item.first,
                                    onClick = {
                                        if (imageLayer != null) {
                                            viewModel.updateLayer(imageLayer.copy(artFilter = item.first))
                                        }
                                    },
                                    label = { Text(item.second, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // TAB 3: Adjustments
                    3 -> {
                        if (imageLayer != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${strings.brightness}: ${imageLayer.brightness.toInt()}", fontSize = 11.sp, modifier = Modifier.width(140.dp))
                                    Slider(
                                        value = imageLayer.brightness,
                                        onValueChange = { viewModel.updateLayer(imageLayer.copy(brightness = it)) },
                                        valueRange = -100f..100f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${strings.contrast}: ${"%.1f".format(imageLayer.contrast)}", fontSize = 11.sp, modifier = Modifier.width(140.dp))
                                    Slider(
                                        value = imageLayer.contrast,
                                        onValueChange = { viewModel.updateLayer(imageLayer.copy(contrast = it)) },
                                        valueRange = 0f..3f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${strings.saturation}: ${"%.1f".format(imageLayer.saturation)}", fontSize = 11.sp, modifier = Modifier.width(140.dp))
                                    Slider(
                                        value = imageLayer.saturation,
                                        onValueChange = { viewModel.updateLayer(imageLayer.copy(saturation = it)) },
                                        valueRange = 0f..3f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // TAB 4: Special FX
                    4 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = imageLayer?.overlayEffect == ImageOverlayEffect.LENS_FLARE,
                                        onClick = {
                                            if (imageLayer != null) viewModel.updateLayer(imageLayer.copy(overlayEffect = ImageOverlayEffect.LENS_FLARE))
                                        },
                                        label = { Text("Lens Flare ☀️") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = imageLayer?.overlayEffect == ImageOverlayEffect.LIGHT_LEAK,
                                        onClick = {
                                            if (imageLayer != null) viewModel.updateLayer(imageLayer.copy(overlayEffect = ImageOverlayEffect.LIGHT_LEAK))
                                        },
                                        label = { Text("Light Leak 🌅") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = imageLayer?.overlayEffect == ImageOverlayEffect.BOKEH,
                                        onClick = {
                                            if (imageLayer != null) viewModel.updateLayer(imageLayer.copy(overlayEffect = ImageOverlayEffect.BOKEH))
                                        },
                                        label = { Text("Bokeh ✨") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = imageLayer?.overlayEffect == ImageOverlayEffect.VIGNETTE_DARK,
                                        onClick = {
                                            if (imageLayer != null) viewModel.updateLayer(imageLayer.copy(overlayEffect = ImageOverlayEffect.VIGNETTE_DARK))
                                        },
                                        label = { Text("Vignette 🌑") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
