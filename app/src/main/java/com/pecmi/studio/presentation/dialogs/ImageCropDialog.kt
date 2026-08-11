package com.pecmi.studio.presentation.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.editor.CanvasViewModel

import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun ImageCropDialog(
    viewModel: CanvasViewModel,
    imageLayer: Layer.Image,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedRatio by remember { mutableStateOf("1:1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.cropRatioTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.selectCropRatio, style = MaterialTheme.typography.bodyMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1:1", "4:3", "16:9", "9:16", "Circle").forEach { ratio ->
                        AssistChip(
                            onClick = { selectedRatio = ratio },
                            label = { Text(ratio) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val baseW = imageLayer.bitmap?.width ?: imageLayer.width
                    val baseH = imageLayer.bitmap?.height ?: imageLayer.height

                    val (newW, newH) = when (selectedRatio) {
                        "1:1", "Circle" -> Pair(minOf(baseW, baseH), minOf(baseW, baseH))
                        "4:3" -> Pair(baseW, (baseW * 3) / 4)
                        "16:9" -> Pair(baseW, (baseW * 9) / 16)
                        "9:16" -> Pair((baseH * 9) / 16, baseH)
                        else -> Pair(baseW, baseH)
                    }

                    viewModel.updateLayer(imageLayer.copy(width = newW, height = newH, cornerRadius = if (selectedRatio == "Circle") newW / 2f else imageLayer.cornerRadius))
                    onDismiss()
                }
            ) {
                Text(strings.applyCrop)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
