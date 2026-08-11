package com.pecmi.studio

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.editor.EditorBottomTab
import com.pecmi.studio.presentation.BottomNavigationBar
import com.pecmi.studio.presentation.CanvasView
import com.pecmi.studio.presentation.FloatingContextBar
import com.pecmi.studio.presentation.TopBar
import com.pecmi.studio.presentation.dialogs.AddMenuSheet
import com.pecmi.studio.presentation.dialogs.CanvasSettingsDialog
import com.pecmi.studio.presentation.dialogs.ConsentDialog
import com.pecmi.studio.presentation.dialogs.ExportDialog
import com.pecmi.studio.presentation.dialogs.LayersDrawer
import com.pecmi.studio.presentation.dialogs.QuotesSheet
import com.pecmi.studio.presentation.dialogs.TextEditDialog
import com.pecmi.studio.presentation.toolbars.BackgroundToolbar
import com.pecmi.studio.presentation.toolbars.DrawToolbar
import com.pecmi.studio.presentation.toolbars.ImageToolbar
import com.pecmi.studio.presentation.toolbars.ShapeToolbar
import com.pecmi.studio.presentation.toolbars.TextToolbar
import com.pecmi.studio.storage.AppLanguage
import com.pecmi.studio.ui.language.LocalAppStrings
import com.pecmi.studio.ui.language.getStringsForLanguage
import com.pecmi.studio.ui.theme.PecmiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CanvasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.pecmi.studio.util.CrashLogger.init(applicationContext)
        com.pecmi.studio.ads.AdManager.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val appStrings = remember(uiState.language) { getStringsForLanguage(uiState.language) }
            val layoutDirection = if (uiState.language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr

            PecmiTheme(themeMode = uiState.themeMode) {
                CompositionLocalProvider(
                    LocalAppStrings provides appStrings,
                    LocalLayoutDirection provides layoutDirection
                ) {
                    PecmiApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun PecmiApp(viewModel: CanvasViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appPreferences = remember { com.pecmi.studio.storage.AppPreferences(context.applicationContext) }

    var showConsentDialog by remember { mutableStateOf(!appPreferences.hasAcceptedConsent) }

    var editingTextLayer by remember { mutableStateOf<Layer.Text?>(null) }
    var showRestoreDialog by remember { mutableStateOf(viewModel.hasLastEdit()) }

    if (showRestoreDialog) {
        com.pecmi.studio.presentation.dialogs.RestoreDialog(
            timestamp = viewModel.getLastEditTimestamp(),
            onRestore = {
                viewModel.restoreLastEdit()
                showRestoreDialog = false
            },
            onStartNew = {
                viewModel.clearLastEdit()
                showRestoreDialog = false
            }
        )
    }

    var isPickingBackground by remember { mutableStateOf(false) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    if (isPickingBackground) {
                        viewModel.addBackgroundImageLayer(bitmap, it.toString())
                    } else {
                        viewModel.addImageLayer(bitmap, it.toString())
                    }
                    viewModel.selectTab(EditorBottomTab.IMAGES)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isPickingBackground = false
            }
        }
    }

    // Toast feedback listener
    LaunchedEffect(uiState.messageToast) {
        uiState.messageToast?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val selectedLayer = uiState.layers.find { it.id == uiState.selectedLayerId }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pecmi_root_scaffold"),
        topBar = {
            AnimatedVisibility(
                visible = !uiState.isFocusMode,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                TopBar(
                    viewModel = viewModel,
                    layerCount = uiState.layers.size,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    isZoomMode = uiState.isZoomMode,
                    onEditSelectedLayer = {
                        if (selectedLayer is Layer.Text) {
                            editingTextLayer = selectedLayer
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !uiState.isFocusMode,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Active Tool Panel
                    AnimatedVisibility(
                        visible = uiState.activeBottomTab != EditorBottomTab.NONE,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        when (uiState.activeBottomTab) {
                            EditorBottomTab.BACKGROUND_EFFECTS -> BackgroundToolbar(viewModel = viewModel, settings = uiState.settings)
                            EditorBottomTab.TEXT -> TextToolbar(
                                viewModel = viewModel,
                                selectedTextLayer = selectedLayer as? Layer.Text,
                                onEditTextContent = { textLayer -> editingTextLayer = textLayer }
                            )
                            EditorBottomTab.SHAPES -> ShapeToolbar(
                                viewModel = viewModel,
                                selectedShapeLayer = selectedLayer as? Layer.Shape
                            )
                            EditorBottomTab.IMAGES -> ImageToolbar(
                                viewModel = viewModel,
                                selectedImageLayer = selectedLayer as? Layer.Image
                            )
                            EditorBottomTab.DRAW -> DrawToolbar(viewModel = viewModel)
                            EditorBottomTab.NONE -> {}
                        }
                    }

                    // Bottom Navigation Bar
                    BottomNavigationBar(
                        activeTab = uiState.activeBottomTab,
                        onTabSelected = { tab -> viewModel.selectTab(tab) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Interactive Canvas
            CanvasView(
                viewModel = viewModel,
                settings = uiState.settings,
                layers = uiState.layers,
                selectedLayerId = uiState.selectedLayerId,
                isZoomMode = uiState.isZoomMode,
                onDoubleTapLayer = { layer ->
                    if (layer is Layer.Text) {
                        editingTextLayer = layer
                    }
                }
            )

            // Floating Quick Context Action Bar for Selected Elements
            if (!uiState.isFocusMode && selectedLayer != null && uiState.activeBottomTab == EditorBottomTab.NONE) {
                FloatingContextBar(
                    viewModel = viewModel,
                    selectedLayer = selectedLayer,
                    onEditText = if (selectedLayer is Layer.Text) { { editingTextLayer = selectedLayer } } else null,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }

            // Smart Floating (+) Add Button & Symmetrical Save Button
            if (!uiState.isFocusMode) {
                val strings = LocalAppStrings.current
                FloatingActionButton(
                    onClick = { viewModel.toggleAddMenuSheet(true) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp, start = 16.dp)
                        .testTag("smart_add_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.add,
                        modifier = Modifier.size(28.dp)
                    )
                }

                FloatingActionButton(
                    onClick = { viewModel.toggleExportDialog(true) },
                    shape = CircleShape,
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                        .testTag("smart_save_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = strings.save,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Exit Focus Mode Overlay Button
            if (uiState.isFocusMode) {
                IconButton(
                    onClick = { viewModel.toggleFocusMode(false) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(
                            brush = Brush.radialGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Exit Focus Mode",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Modal Sheets and Dialogs
    if (uiState.showAddMenuSheet) {
        AddMenuSheet(
            viewModel = viewModel,
            onPickImageRequested = {
                isPickingBackground = false
                galleryPickerLauncher.launch("image/*")
            },
            onPickBackgroundImageRequested = {
                isPickingBackground = true
                galleryPickerLauncher.launch("image/*")
            },
            onDismiss = { viewModel.toggleAddMenuSheet(false) }
        )
    }

    if (uiState.showLayersDrawer) {
        LayersDrawer(
            viewModel = viewModel,
            layers = uiState.layers,
            selectedLayerId = uiState.selectedLayerId,
            onDismiss = { viewModel.toggleLayersDrawer(false) }
        )
    }

    if (uiState.showQuotesDialog) {
        QuotesSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleQuotesDialog(false) }
        )
    }

    if (uiState.showExportDialog) {
        ExportDialog(
            viewModel = viewModel,
            exportedFile = uiState.exportedFile,
            onDismiss = { viewModel.toggleExportDialog(false) }
        )
    }

    if (uiState.showSettingsDialog) {
        CanvasSettingsDialog(
            viewModel = viewModel,
            settings = uiState.settings,
            onDismiss = { viewModel.toggleSettingsDialog(false) }
        )
    }

    if (uiState.showStickerSheet) {
        com.pecmi.studio.presentation.dialogs.StickerPickerSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleStickerSheet(false) }
        )
    }

    if (uiState.showProjectsDialog) {
        com.pecmi.studio.presentation.dialogs.ProjectsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.toggleProjectsDialog(false) }
        )
    }

    if (showConsentDialog) {
        ConsentDialog(
            onAccept = {
                appPreferences.hasAcceptedConsent = true
                appPreferences.isFirstRun = false
                showConsentDialog = false
            }
        )
    }

    if (uiState.showWhatsNewDialog) {
        com.pecmi.studio.presentation.dialogs.WhatsNewDialog(
            onDismiss = { viewModel.toggleWhatsNewDialog(false) }
        )
    }

    if (uiState.showAboutDialog) {
        com.pecmi.studio.presentation.dialogs.AboutDialog(
            onDismiss = { viewModel.toggleAboutDialog(false) }
        )
    }

    editingTextLayer?.let { textLayer ->
        TextEditDialog(
            initialText = textLayer.text,
            onConfirm = { updatedText ->
                viewModel.updateLayer(textLayer.copy(text = updatedText))
                editingTextLayer = null
            },
            onDismiss = { editingTextLayer = null }
        )
    }
}
