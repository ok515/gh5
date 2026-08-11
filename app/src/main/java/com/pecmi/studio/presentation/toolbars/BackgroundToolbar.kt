package com.pecmi.studio.presentation.toolbars

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.domain.model.CanvasPreset
import com.pecmi.studio.domain.model.CanvasSettings
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun BackgroundToolbar(
    viewModel: CanvasViewModel,
    settings: CanvasSettings,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val presetColors = listOf(
        0xFFFFFFFF.toInt(), 0xFFF8FAFC.toInt(), 0xFFF1F5F9.toInt(),
        0xFF0F172A.toInt(), 0xFF1E293B.toInt(), 0xFF3B82F6.toInt(),
        0xFF8B5CF6.toInt(), 0xFFEC4899.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt()
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("background_toolbar"),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Preset Ratios
            Text(strings.aspectRatio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CanvasPreset.entries) { preset ->
                    FilterChip(
                        selected = settings.preset == preset,
                        onClick = {
                            viewModel.updateCanvasSettings(
                                settings.copy(
                                    preset = preset,
                                    width = preset.width,
                                    height = preset.height
                                )
                            )
                        },
                        label = { Text(preset.displayName, fontSize = 11.sp) }
                    )
                }
            }

            // Solid Background Colors & Transparent Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.color, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                AssistChip(
                    onClick = {
                        viewModel.updateCanvasSettings(settings.copy(isTransparent = !settings.isTransparent))
                    },
                    label = { Text(if (settings.isTransparent) "🏁 ${strings.transparentBackground}" else "🏁 ${strings.color}") },
                    colors = if (settings.isTransparent) AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) else AssistChipDefaults.assistChipColors()
                )
            }

            if (!settings.isTransparent) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(presetColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    width = if (settings.backgroundColor == color) 3.dp else 1.dp,
                                    color = if (settings.backgroundColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateCanvasSettings(
                                        settings.copy(backgroundColor = color, isTransparent = false)
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}
