package com.pecmi.studio.presentation

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.editor.EditorBottomTab
import com.pecmi.studio.storage.ThemeMode
import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun TopBar(
    viewModel: CanvasViewModel,
    layerCount: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    isZoomMode: Boolean,
    onEditSelectedLayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val hasSelectedLayer = uiState.selectedLayerId != null
    val selectedLayer = uiState.layers.find { it.id == uiState.selectedLayerId }

    var showSaveMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.addImageLayer(bitmap, it.toString())
                    viewModel.selectTab(EditorBottomTab.IMAGES)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.addImageLayer(it, "camera_photo_${System.currentTimeMillis()}")
            viewModel.selectTab(EditorBottomTab.IMAGES)
        }
    }

    val plpPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importProjectFromUri(it) }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_bar_surface"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
                // Main Responsive Top Bar Container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- LEFT: BRAND & UNDO/REDO CONTROLS ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Pecmi Logo Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleAboutDialog(true) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.appTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Undo Button
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("undo_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = strings.undo,
                                tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Redo Button
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = canRedo,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("redo_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = strings.redo,
                                tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // --- CENTER: CONTEXTUAL QUICK LAYER ACTIONS (Dynamically weight-allocated) ---
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasSelectedLayer && selectedLayer != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    // Quick Edit
                                    IconButton(
                                        onClick = onEditSelectedLayer,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = strings.edit,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Quick Duplicate
                                    IconButton(
                                        onClick = { viewModel.duplicateLayer(selectedLayer.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = strings.duplicate,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Quick Lock Toggle
                                    IconButton(
                                        onClick = { viewModel.toggleLayerLock(selectedLayer.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (selectedLayer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = strings.lockLayer,
                                            tint = if (selectedLayer.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Quick Delete
                                    IconButton(
                                        onClick = { viewModel.deleteLayer(selectedLayer.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = strings.delete,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- RIGHT: PRIMARY ACTIONS & ANCHORED THREE-DOTS MENU ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Layers Drawer Toggle
                        IconButton(
                            onClick = { viewModel.toggleLayersDrawer(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("layers_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (layerCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(text = "$layerCount", fontSize = 10.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = strings.layers,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Save / Export Quick Action
                        IconButton(
                            onClick = { viewModel.toggleExportDialog(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("export_button")
                        ) {
                            Icon(
                                Icons.Default.IosShare,
                                contentDescription = strings.exportShare,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // More Options (3 Dots) Menu - ALWAYS ANCHORED & VISIBLE
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("more_menu_button")
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = strings.settings,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                // Save Project
                                DropdownMenuItem(
                                    text = { Text(strings.saveProject) },
                                    leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        viewModel.saveCurrentProject()
                                        showMoreMenu = false
                                    }
                                )

                                // Export Image
                                DropdownMenuItem(
                                    text = { Text(strings.saveImage) },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        viewModel.toggleExportDialog(true)
                                        showMoreMenu = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // Grid Toggle
                                DropdownMenuItem(
                                    text = { Text(if (uiState.settings.showGrid) strings.hideGrid else strings.showGrid) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.GridView,
                                            contentDescription = null,
                                            tint = if (uiState.settings.showGrid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleGrid()
                                        showMoreMenu = false
                                    }
                                )

                                // Guidelines Toggle
                                DropdownMenuItem(
                                    text = { Text(if (uiState.settings.showGuidelines) strings.hideGuidelines else strings.showGuidelines) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.GridView,
                                            contentDescription = null,
                                            tint = if (uiState.settings.showGuidelines) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleGuidelines()
                                        showMoreMenu = false
                                    }
                                )

                                // Theme Toggle (Light / Dark)
                                val isDark = uiState.themeMode == ThemeMode.DARK
                                DropdownMenuItem(
                                    text = { Text(if (isDark) strings.themeLight else strings.themeDark) },
                                    leadingIcon = {
                                        Icon(
                                            if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        val nextMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                                        viewModel.setThemeMode(nextMode)
                                        showMoreMenu = false
                                    }
                                )

                                // Canvas Preset & Settings
                                DropdownMenuItem(
                                    text = { Text(strings.canvasSettings) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        viewModel.toggleSettingsDialog(true)
                                        showMoreMenu = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // Import Image
                                DropdownMenuItem(
                                    text = { Text(strings.gallery) },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                    onClick = {
                                        galleryPickerLauncher.launch("image/*")
                                        showMoreMenu = false
                                    }
                                )

                                // Camera Photo
                                DropdownMenuItem(
                                    text = { Text(strings.camera) },
                                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                    onClick = {
                                        cameraPhotoLauncher.launch(null)
                                        showMoreMenu = false
                                    }
                                )

                                // Open PLP Project File
                                DropdownMenuItem(
                                    text = { Text(strings.openPlp) },
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                                    onClick = {
                                        plpPickerLauncher.launch("*/*")
                                        showMoreMenu = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // Restore AutoSave
                                DropdownMenuItem(
                                    text = { Text(strings.restoreAutoSave) },
                                    onClick = {
                                        viewModel.restoreLastEdit()
                                        showMoreMenu = false
                                    }
                                )

                                // Quotes Dialog
                                DropdownMenuItem(
                                    text = { Text(strings.quotes) },
                                    leadingIcon = { Icon(Icons.Default.FormatQuote, contentDescription = null) },
                                    onClick = {
                                        viewModel.toggleQuotesDialog(true)
                                        showMoreMenu = false
                                    }
                                )

                                // Projects Gallery
                                DropdownMenuItem(
                                    text = { Text(strings.projectsGallery) },
                                    onClick = {
                                        viewModel.toggleProjectsDialog(true)
                                        showMoreMenu = false
                                    }
                                )

                                // About App
                                DropdownMenuItem(
                                    text = { Text(strings.aboutTitle) },
                                    onClick = {
                                        viewModel.toggleAboutDialog(true)
                                        showMoreMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }
}
