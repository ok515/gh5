package com.pecmi.studio.presentation.dialogs

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.ads.AdManager
import com.pecmi.studio.ads.AdMobBannerView
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.storage.ExportFormat
import com.pecmi.studio.ui.language.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

data class QualityPreset(
    val label: String,
    val scale: Float,
    val isLocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    viewModel: CanvasViewModel,
    exportedFile: File?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedFormat by remember { mutableStateOf(ExportFormat.PNG) }
    var scaleFactor by remember { mutableFloatStateOf(1f) }
    var quality by remember { mutableFloatStateOf(100f) }

    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }

    val qualityPresets = listOf(
        QualityPreset(strings.presetNormal, 1f, isLocked = false),
        QualityPreset(strings.preset2K, 3f, isLocked = true),
        QualityPreset(strings.preset4K, 4f, isLocked = true)
    )

    val currentPreset = qualityPresets.find { it.scale == scaleFactor } ?: qualityPresets.first()

    val currentWidth = (uiState.settings.width * scaleFactor).toInt()
    val currentHeight = (uiState.settings.height * scaleFactor).toInt()

    fun getEstimatedSizeString(): String {
        val pixels = currentWidth.toLong() * currentHeight.toLong()
        val bytes = when (selectedFormat) {
            ExportFormat.PNG -> (pixels * 1.8).toLong()
            ExportFormat.JPG -> (pixels * 3.0 * (quality / 100f) * 0.22).toLong()
            ExportFormat.WEBP -> (pixels * 3.0 * (quality / 100f) * 0.16).toLong()
        }
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.US, "~%.1f MB", mb)
        } else {
            String.format(Locale.US, "~%d KB", kb.toInt().coerceAtLeast(10))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("export_dialog_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.exportTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = strings.close, modifier = Modifier.size(16.dp))
                }
            }

            // Compact Banner Ad
            AdMobBannerView(modifier = Modifier.height(40.dp))

            if (isExporting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${(exportProgress * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                // Section 1: File Format Selection
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(strings.exportFormat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExportFormat.entries.forEach { format ->
                            FilterChip(
                                selected = selectedFormat == format,
                                onClick = { selectedFormat = format },
                                label = { Text(format.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }

                // Section 2: Quality Preset Selection (Large prominent layout for all presets including 4K)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.quality, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (currentPreset.isLocked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.requiresAd, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Single Row: Normal (1x), 2K (3x), 4K (4x)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        qualityPresets.forEach { preset ->
                            val isSelected = scaleFactor == preset.scale
                            Surface(
                                onClick = { scaleFactor = preset.scale },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isSelected && preset.isLocked -> MaterialTheme.colorScheme.primaryContainer
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                contentColor = when {
                                    isSelected && preset.isLocked -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(preset.label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                    if (preset.isLocked) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Live Image Dimensions & Estimated File Size Display
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.dimensions}: ${currentWidth} × ${currentHeight} px",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${strings.estimatedSize}: ${getEstimatedSizeString()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Section 4: Compression Quality Slider (JPG/WEBP)
                if (selectedFormat != ExportFormat.PNG) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.compressionRatio, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${quality.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = quality,
                            onValueChange = { quality = it },
                            valueRange = 30f..100f,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                Text(
                    strings.saveLocation,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Section 6: Action Buttons Row (Save & Share)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export & Save Button
                    Button(
                        onClick = {
                            val startExportProcess = {
                                coroutineScope.launch {
                                    isExporting = true
                                    exportProgress = 0.2f
                                    delay(80)
                                    exportProgress = 0.6f
                                    delay(80)
                                    exportProgress = 0.9f
                                    viewModel.exportCanvasImage(selectedFormat, scaleFactor, quality.toInt())
                                    exportProgress = 1.0f
                                    delay(80)
                                    isExporting = false
                                    onDismiss()
                                }
                            }

                            if (currentPreset.isLocked && context is Activity) {
                                AdManager.showRewardedAd(
                                    activity = context,
                                    onRewarded = {
                                        startExportProcess()
                                    },
                                    onDismissedOrFailed = {
                                        Toast.makeText(context, strings.exportCancelledAd, Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                startExportProcess()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (currentPreset.isLocked) strings.exportHighQuality else strings.saveToGallery, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share Button
                    OutlinedButton(
                        onClick = {
                            val startShareProcess = {
                                coroutineScope.launch {
                                    viewModel.exportCanvasImage(selectedFormat, scaleFactor, quality.toInt())
                                    viewModel.shareExportedImage()
                                    onDismiss()
                                }
                            }

                            if (currentPreset.isLocked && context is Activity) {
                                AdManager.showRewardedAd(
                                    activity = context,
                                    onRewarded = {
                                        startShareProcess()
                                    },
                                    onDismissedOrFailed = {
                                        Toast.makeText(context, strings.exportCancelledAd, Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                startShareProcess()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.shareImage, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
