package com.pecmi.studio.presentation.toolbars

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.domain.model.ImageArtFilter
import com.pecmi.studio.domain.model.ImageOverlayEffect
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.presentation.dialogs.ExifAndPaletteDialog
import com.pecmi.studio.presentation.dialogs.ImageCropDialog
import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun ImageToolbar(
    viewModel: CanvasViewModel,
    selectedImageLayer: Layer.Image?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var activeSubTab by remember { mutableIntStateOf(0) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showExifDialog by remember { mutableStateOf(false) }

    val subTabs = listOf("Adjustments", "Filters & Art", "Crop & Transform", "Overlays & FX", "Tools & Info")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.addImageLayer(bitmap, it.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (showCropDialog && selectedImageLayer != null) {
        ImageCropDialog(
            viewModel = viewModel,
            imageLayer = selectedImageLayer,
            onDismiss = { showCropDialog = false }
        )
    }

    if (showExifDialog && selectedImageLayer != null) {
        ExifAndPaletteDialog(
            imageLayer = selectedImageLayer,
            onSelectColor = { color ->
                viewModel.updateLayer(selectedImageLayer.copy(borderColor = color))
            },
            onDismiss = { showExifDialog = false }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("image_toolbar"),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            // Main Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    label = { Text("+ ${strings.gallery}") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                )

                if (selectedImageLayer != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = { showCropDialog = true }) {
                            Icon(Icons.Default.Crop, contentDescription = strings.crop)
                        }
                        IconButton(onClick = { viewModel.duplicateLayer(selectedImageLayer.id) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = strings.copy)
                        }
                        IconButton(onClick = { viewModel.deleteLayer(selectedImageLayer.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (selectedImageLayer != null) {
                ScrollableTabRow(
                    selectedTabIndex = activeSubTab,
                    edgePadding = 8.dp,
                    containerColor = Color.Transparent
                ) {
                    subTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = activeSubTab == index,
                            onClick = { activeSubTab = index },
                            text = { Text(title, fontSize = 11.sp) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (activeSubTab) {
                        0 -> { // Adjustments
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${strings.brightness}: ${selectedImageLayer.brightness.toInt()}", fontSize = 11.sp)
                                Slider(
                                    value = selectedImageLayer.brightness,
                                    onValueChange = { viewModel.updateLayer(selectedImageLayer.copy(brightness = it)) },
                                    valueRange = -100f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${strings.contrast}: %.2f".format(selectedImageLayer.contrast), fontSize = 11.sp)
                                Slider(
                                    value = selectedImageLayer.contrast,
                                    onValueChange = { viewModel.updateLayer(selectedImageLayer.copy(contrast = it)) },
                                    valueRange = 0.2f..2.5f,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${strings.saturation}: %.2f".format(selectedImageLayer.saturation), fontSize = 11.sp)
                                Slider(
                                    value = selectedImageLayer.saturation,
                                    onValueChange = { viewModel.updateLayer(selectedImageLayer.copy(saturation = it)) },
                                    valueRange = 0f..2.5f,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${strings.warmth}: ${selectedImageLayer.warmth.toInt()}", fontSize = 11.sp)
                                Slider(
                                    value = selectedImageLayer.warmth,
                                    onValueChange = { viewModel.updateLayer(selectedImageLayer.copy(warmth = it)) },
                                    valueRange = -100f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        1 -> { // Filters & Art
                            Text("Artistic Presets", fontSize = 11.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ImageArtFilter.entries.toTypedArray()) { filter ->
                                    FilterChip(
                                        selected = selectedImageLayer.artFilter == filter,
                                        onClick = { viewModel.updateLayer(selectedImageLayer.copy(artFilter = filter)) },
                                        label = { Text(filter.name) }
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedImageLayer.sepia > 0f,
                                    onClick = { viewModel.updateLayer(selectedImageLayer.copy(sepia = if (selectedImageLayer.sepia > 0f) 0f else 0.8f)) },
                                    label = { Text("Sepia") }
                                )
                                FilterChip(
                                    selected = selectedImageLayer.grayscale > 0f,
                                    onClick = { viewModel.updateLayer(selectedImageLayer.copy(grayscale = if (selectedImageLayer.grayscale > 0f) 0f else 1f)) },
                                    label = { Text("Grayscale") }
                                )
                                FilterChip(
                                    selected = selectedImageLayer.invert,
                                    onClick = { viewModel.updateLayer(selectedImageLayer.copy(invert = !selectedImageLayer.invert)) },
                                    label = { Text("Invert") }
                                )
                            }
                        }

                        2 -> { // Crop & Transform
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = { showCropDialog = true },
                                    label = { Text(strings.crop) },
                                    leadingIcon = { Icon(Icons.Default.Crop, contentDescription = null) }
                                )

                                IconButton(onClick = { viewModel.updateLayer(selectedImageLayer.copy(flipH = !selectedImageLayer.flipH)) }) {
                                    Icon(Icons.Default.Flip, contentDescription = "Flip")
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${strings.cornerRadius}: ${selectedImageLayer.cornerRadius.toInt()}", fontSize = 11.sp)
                                Slider(
                                    value = selectedImageLayer.cornerRadius,
                                    onValueChange = { viewModel.updateLayer(selectedImageLayer.copy(cornerRadius = it)) },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${strings.stroke}: ${selectedImageLayer.borderWidth.toInt()}", fontSize = 11.sp)
                                Slider(
                                    value = selectedImageLayer.borderWidth,
                                    onValueChange = { viewModel.updateLayer(selectedImageLayer.copy(borderWidth = it)) },
                                    valueRange = 0f..24f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        3 -> { // Overlays & FX
                            Text("Overlays", fontSize = 11.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ImageOverlayEffect.entries.toTypedArray()) { overlay ->
                                    FilterChip(
                                        selected = selectedImageLayer.overlayEffect == overlay,
                                        onClick = { viewModel.updateLayer(selectedImageLayer.copy(overlayEffect = overlay)) },
                                        label = { Text(overlay.name) }
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${strings.shadow}: ${selectedImageLayer.shadowBlur.toInt()}", fontSize = 11.sp)
                                Slider(
                                    value = selectedImageLayer.shadowBlur,
                                    onValueChange = { viewModel.updateLayer(selectedImageLayer.copy(shadowBlur = it)) },
                                    valueRange = 0f..30f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        4 -> { // Tools & Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = { showExifDialog = true },
                                    label = { Text("EXIF Info") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                                )

                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.updateLayer(
                                            selectedImageLayer.copy(
                                                brightness = 0f, contrast = 1f, saturation = 1f, warmth = 0f,
                                                sepia = 0f, grayscale = 0f, invert = false, artFilter = ImageArtFilter.NONE,
                                                overlayEffect = ImageOverlayEffect.NONE, cornerRadius = 0f, borderWidth = 0f
                                            )
                                        )
                                    },
                                    label = { Text("Reset") },
                                    leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
