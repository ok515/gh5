package com.pecmi.studio.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.domain.model.ShapeType
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.editor.EditorBottomTab

import com.pecmi.studio.ui.language.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuSheet(
    viewModel: CanvasViewModel,
    onPickImageRequested: () -> Unit,
    onPickBackgroundImageRequested: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.addNewElement,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AddMenuItemCard(
                        title = strings.newTextTitle,
                        icon = Icons.Default.TextFields,
                        gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
                        onClick = {
                            viewModel.addTextLayer(strings.newTextTitle)
                            onDismiss()
                        }
                    )
                }
                item {
                    AddMenuItemCard(
                        title = strings.imageTitle,
                        icon = Icons.Default.Image,
                        gradient = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
                        onClick = {
                            onPickImageRequested()
                            onDismiss()
                        }
                    )
                }
                item {
                    AddMenuItemCard(
                        title = strings.shapeTitle,
                        icon = Icons.Default.Category,
                        gradient = listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
                        onClick = {
                            viewModel.addShapeLayer(ShapeType.RECTANGLE)
                            onDismiss()
                        }
                    )
                }
                item {
                    AddMenuItemCard(
                        title = strings.stickersTitle,
                        icon = Icons.Default.Pets,
                        gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                        onClick = {
                            viewModel.toggleStickerSheet(true)
                            onDismiss()
                        }
                    )
                }
                item {
                    AddMenuItemCard(
                        title = strings.drawTitle,
                        icon = Icons.Default.Gesture,
                        gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                        onClick = {
                            viewModel.selectTab(EditorBottomTab.DRAW)
                            onDismiss()
                        }
                    )
                }
                item {
                    AddMenuItemCard(
                        title = strings.quotesTitle,
                        icon = Icons.Default.FormatQuote,
                        gradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                        onClick = {
                            viewModel.toggleQuotesDialog(true)
                            onDismiss()
                        }
                    )
                }
                item {
                    AddMenuItemCard(
                        title = strings.useImageAsBackground,
                        icon = Icons.Default.Wallpaper,
                        gradient = listOf(Color(0xFF0EA5E9), Color(0xFF2563EB)),
                        onClick = {
                            onPickBackgroundImageRequested()
                            onDismiss()
                        }
                    )
                }
                item {
                    AddMenuItemCard(
                        title = strings.specialStarTitle,
                        icon = Icons.Default.AutoAwesome,
                        gradient = listOf(Color(0xFFEAB308), Color(0xFFCA8A04)),
                        onClick = {
                            viewModel.addShapeLayer(ShapeType.STAR)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMenuItemCard(
    title: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
