package com.pecmi.studio.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.editor.CanvasViewModel

@Composable
fun FloatingContextBar(
    viewModel: CanvasViewModel,
    selectedLayer: Layer?,
    onEditText: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedLayer != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
        modifier = modifier.testTag("floating_context_bar")
    ) {
        if (selectedLayer == null) return@AnimatedVisibility

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
            tonalElevation = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (selectedLayer is Layer.Text && onEditText != null) {
                    IconButton(
                        onClick = onEditText,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Text",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Duplicate
                IconButton(
                    onClick = { viewModel.duplicateLayer(selectedLayer.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate Layer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Lock / Unlock
                IconButton(
                    onClick = { viewModel.toggleLayerLock(selectedLayer.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (selectedLayer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock Layer",
                        tint = if (selectedLayer.isLocked) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Rotate 90
                IconButton(
                    onClick = { viewModel.updateSelectedLayerRotation(90f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate 90",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .size(height = 20.dp, width = 1.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Move Up / Bring to Front
                IconButton(
                    onClick = { viewModel.bringLayerToFront(selectedLayer.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipToFront,
                        contentDescription = "Bring to Front",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Move Down / Send to Back
                IconButton(
                    onClick = { viewModel.sendLayerToBack(selectedLayer.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipToBack,
                        contentDescription = "Send to Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Flip Horizontal
                IconButton(
                    onClick = {
                        val updated = when (selectedLayer) {
                            is Layer.Text -> selectedLayer.copy(flipH = !selectedLayer.flipH)
                            is Layer.Shape -> selectedLayer.copy(flipH = !selectedLayer.flipH)
                            is Layer.Image -> selectedLayer.copy(flipH = !selectedLayer.flipH)
                            else -> null
                        }
                        if (updated != null) viewModel.updateLayer(updated)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Flip Horizontal",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .size(height = 20.dp, width = 1.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Delete
                IconButton(
                    onClick = { viewModel.deleteLayer(selectedLayer.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Layer",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}
