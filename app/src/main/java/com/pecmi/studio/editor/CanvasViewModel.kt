package com.pecmi.studio.editor

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pecmi.studio.data.database.ProjectDatabase
import com.pecmi.studio.data.repository.ProjectRepository
import com.pecmi.studio.domain.model.CanvasPreset
import com.pecmi.studio.domain.model.CanvasSettings
import com.pecmi.studio.domain.model.DrawingStroke
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.domain.model.ShapeType
import com.pecmi.studio.storage.AppLanguage
import com.pecmi.studio.storage.AppPreferences
import com.pecmi.studio.storage.ExportFormat
import com.pecmi.studio.storage.ExportManager
import com.pecmi.studio.storage.ThemeMode
import com.pecmi.studio.ui.language.getStringsForLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class EditorBottomTab {
    NONE, BACKGROUND_EFFECTS, TEXT, SHAPES, IMAGES, DRAW
}

data class EditorUiState(
    val layers: List<Layer> = emptyList(),
    val selectedLayerId: String? = null,
    val settings: CanvasSettings = CanvasSettings(),
    val activeBottomTab: EditorBottomTab = EditorBottomTab.NONE,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showLayersDrawer: Boolean = false,
    val showQuotesDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showStickerSheet: Boolean = false,
    val showProjectsDialog: Boolean = false,
    val showWhatsNewDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
    val showAddMenuSheet: Boolean = false,
    val isFocusMode: Boolean = false,
    val isZoomMode: Boolean = false,
    val exportedFile: File? = null,
    val messageToast: String? = null,
    val drawColor: Int = 0xFF6366F1.toInt(),
    val drawBrushSize: Float = 14f,
    val isEraserMode: Boolean = false,
    val activeBrushType: String = "SOFT_PEN",
    val brushOpacity: Float = 1.0f,
    val brushFeather: Float = 0.2f,
    val activeCurrentStroke: DrawingStroke? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ARABIC,
    val watermarkRemovedUntil: Long = 0L
)

class CanvasViewModel(application: Application) : AndroidViewModel(application) {

    private val appPreferences = AppPreferences(application)

    private val repository: ProjectRepository by lazy {
        val db = ProjectDatabase.getInstance(application)
        ProjectRepository(db.projectDao())
    }

    private val historyManager = HistoryManager()

    private val _uiState = MutableStateFlow(
        EditorUiState(
            themeMode = appPreferences.themeMode,
            language = appPreferences.language
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        // Initial setup with default demo layers so canvas is instantly engaging
        setupDefaultCanvas()
    }

    fun setThemeMode(mode: ThemeMode) {
        appPreferences.themeMode = mode
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setAppLanguage(lang: AppLanguage) {
        appPreferences.language = lang
        _uiState.update { it.copy(language = lang) }
    }

    private fun setupDefaultCanvas() {
        val initialSettings = CanvasSettings(
            width = 1080,
            height = 1080,
            preset = CanvasPreset.SQUARE_1_1,
            backgroundColor = 0xFFFFFFFF.toInt(),
            isTransparent = false
        )

        val demoTextLayer = Layer.Text(
            id = UUID.randomUUID().toString(),
            name = "New Text",
            text = "New Text",
            x = 540f,
            y = 540f,
            fontSize = 72f,
            textColor = 0xFF1E293B.toInt(),
            fontStyleBold = true
        )

        val initialLayers = listOf(demoTextLayer)

        historyManager.pushState(initialLayers, initialSettings)
        _uiState.update {
            it.copy(
                layers = initialLayers,
                selectedLayerId = demoTextLayer.id,
                settings = initialSettings,
                canUndo = historyManager.canUndo(),
                canRedo = historyManager.canRedo()
            )
        }
    }

    private fun saveHistoryState() {
        val currentLayers = _uiState.value.layers
        val currentSettings = _uiState.value.settings
        historyManager.pushState(currentLayers, currentSettings)
        _uiState.update {
            it.copy(
                canUndo = historyManager.canUndo(),
                canRedo = historyManager.canRedo()
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appPreferences.lastEditLayersJson = repository.serializeLayers(currentLayers)
                appPreferences.lastEditSettingsJson = repository.serializeSettings(currentSettings)
                appPreferences.lastEditTimestamp = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun undo() {
        val prev = historyManager.undo() ?: return
        _uiState.update {
            it.copy(
                layers = prev.layers,
                settings = prev.settings,
                canUndo = historyManager.canUndo(),
                canRedo = historyManager.canRedo()
            )
        }
    }

    fun redo() {
        val next = historyManager.redo() ?: return
        _uiState.update {
            it.copy(
                layers = next.layers,
                settings = next.settings,
                canUndo = historyManager.canUndo(),
                canRedo = historyManager.canRedo()
            )
        }
    }

    fun selectTab(tab: EditorBottomTab) {
        _uiState.update { current ->
            val nextTab = if (current.activeBottomTab == tab) EditorBottomTab.NONE else tab
            current.copy(activeBottomTab = nextTab)
        }
    }

    fun toggleFocusMode(show: Boolean? = null) {
        _uiState.update { it.copy(isFocusMode = show ?: !it.isFocusMode) }
    }

    fun toggleAddMenuSheet(show: Boolean? = null) {
        _uiState.update { it.copy(showAddMenuSheet = show ?: !it.showAddMenuSheet) }
    }

    fun setBrushType(type: String) {
        _uiState.update { it.copy(activeBrushType = type) }
    }

    fun setBrushOpacity(opacity: Float) {
        _uiState.update { it.copy(brushOpacity = opacity) }
    }

    fun setBrushFeather(feather: Float) {
        _uiState.update { it.copy(brushFeather = feather) }
    }

    fun selectLayer(id: String?) {
        _uiState.update { it.copy(selectedLayerId = id) }
    }

    fun toggleLayersDrawer(show: Boolean? = null) {
        _uiState.update { it.copy(showLayersDrawer = show ?: !it.showLayersDrawer) }
    }

    fun toggleQuotesDialog(show: Boolean? = null) {
        _uiState.update { it.copy(showQuotesDialog = show ?: !it.showQuotesDialog) }
    }

    fun toggleExportDialog(show: Boolean? = null) {
        _uiState.update { it.copy(showExportDialog = show ?: !it.showExportDialog) }
    }

    fun toggleSettingsDialog(show: Boolean? = null) {
        _uiState.update { it.copy(showSettingsDialog = show ?: !it.showSettingsDialog) }
    }

    fun toggleStickerSheet(show: Boolean? = null) {
        _uiState.update { it.copy(showStickerSheet = show ?: !it.showStickerSheet) }
    }

    fun toggleProjectsDialog(show: Boolean? = null) {
        _uiState.update { it.copy(showProjectsDialog = show ?: !it.showProjectsDialog) }
    }

    fun toggleWhatsNewDialog(show: Boolean? = null) {
        _uiState.update { it.copy(showWhatsNewDialog = show ?: !it.showWhatsNewDialog) }
    }

    fun toggleAboutDialog(show: Boolean? = null) {
        _uiState.update { it.copy(showAboutDialog = show ?: !it.showAboutDialog) }
    }

    val savedProjectsFlow = repository.allProjects

    fun loadProject(project: com.pecmi.studio.data.entity.ProjectEntity) {
        viewModelScope.launch {
            try {
                val loaded = repository.getProjectById(project.id)
                if (loaded != null) {
                    val loadedLayers = repository.deserializeLayers(loaded.layersJson)
                    val loadedSettings = repository.deserializeSettings(loaded.settingsJson)
                    _uiState.update {
                        it.copy(
                            layers = loadedLayers,
                            settings = loadedSettings,
                            selectedLayerId = loadedLayers.firstOrNull()?.id,
                            messageToast = "تم تحميل المشروع: ${loaded.title}"
                        )
                    }
                    saveHistoryState()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(messageToast = "فشل تحميل المشروع") }
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            _uiState.update { it.copy(messageToast = "تم حذف المشروع") }
        }
    }

    fun toggleZoomMode() {
        _uiState.update { it.copy(isZoomMode = !it.isZoomMode) }
    }

    fun toggleGrid() {
        _uiState.update {
            val updated = it.settings.copy(showGrid = !it.settings.showGrid)
            it.copy(settings = updated)
        }
    }

    fun toggleGuidelines() {
        _uiState.update {
            val updated = it.settings.copy(showGuidelines = !it.settings.showGuidelines)
            it.copy(settings = updated)
        }
    }

    // --- LAYER MANIPULATION & CREATION ---

    fun addTextLayer(initialText: String = "New Text") {
        val newLayer = Layer.Text(
            id = UUID.randomUUID().toString(),
            name = "Text ${_uiState.value.layers.size + 1}",
            text = initialText,
            x = _uiState.value.settings.width / 2f,
            y = _uiState.value.settings.height / 2f
        )
        val updatedLayers = _uiState.value.layers + newLayer
        _uiState.update { it.copy(layers = updatedLayers, selectedLayerId = newLayer.id) }
        saveHistoryState()
    }

    fun addStickerTextLayer(sticker: String) {
        val newLayer = Layer.Text(
            id = UUID.randomUUID().toString(),
            name = "Sticker $sticker",
            text = sticker,
            x = _uiState.value.settings.width / 2f,
            y = _uiState.value.settings.height / 2f,
            fontSize = 120f,
            boxWidth = 300f
        )
        val updatedLayers = _uiState.value.layers + newLayer
        _uiState.update { it.copy(layers = updatedLayers, selectedLayerId = newLayer.id) }
        saveHistoryState()
    }

    fun addShapeLayer(type: ShapeType) {
        val newLayer = Layer.Shape(
            id = UUID.randomUUID().toString(),
            name = "${type.name.lowercase().replaceFirstChar { it.uppercase() }} ${_uiState.value.layers.size + 1}",
            shapeType = type,
            x = _uiState.value.settings.width / 2f,
            y = _uiState.value.settings.height / 2f
        )
        val updatedLayers = _uiState.value.layers + newLayer
        _uiState.update { it.copy(layers = updatedLayers, selectedLayerId = newLayer.id) }
        saveHistoryState()
    }

    fun addImageLayer(bitmap: Bitmap, imagePath: String? = null) {
        val maxDim = 2048
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        } else bitmap

        val newLayer = Layer.Image(
            id = UUID.randomUUID().toString(),
            name = "Image ${_uiState.value.layers.size + 1}",
            bitmap = scaledBitmap,
            imagePath = imagePath,
            width = scaledBitmap.width,
            height = scaledBitmap.height,
            x = _uiState.value.settings.width / 2f,
            y = _uiState.value.settings.height / 2f
        )
        val updatedLayers = _uiState.value.layers + newLayer
        _uiState.update { it.copy(layers = updatedLayers, selectedLayerId = newLayer.id) }
        saveHistoryState()
    }

    fun addBackgroundImageLayer(bitmap: Bitmap, imagePath: String? = null) {
        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()

        val maxDim = 2048f
        val scale = if (imgW > maxDim || imgH > maxDim) {
            maxDim / maxOf(imgW, imgH)
        } else {
            1f
        }
        val targetW = (imgW * scale).toInt().coerceAtLeast(100)
        val targetH = (imgH * scale).toInt().coerceAtLeast(100)

        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else bitmap

        val newLayer = Layer.Image(
            id = UUID.randomUUID().toString(),
            name = "صورة الخلفية",
            bitmap = scaledBitmap,
            imagePath = imagePath,
            width = targetW,
            height = targetH,
            x = targetW / 2f,
            y = targetH / 2f,
            scaleX = 1f,
            scaleY = 1f,
            rotation = 0f
        )
        val updatedLayers = listOf(newLayer) + _uiState.value.layers
        _uiState.update {
            it.copy(
                settings = it.settings.copy(
                    width = targetW,
                    height = targetH,
                    preset = com.pecmi.studio.domain.model.CanvasPreset.CUSTOM
                ),
                layers = updatedLayers,
                selectedLayerId = newLayer.id
            )
        }
        showToast("تم تطبيق الصورة كخلفية لمشروع التحرير")
        saveHistoryState()
    }

    fun setDrawColor(color: Int) {
        _uiState.update { it.copy(drawColor = color) }
    }

    fun setDrawBrushSize(size: Float) {
        _uiState.update { it.copy(drawBrushSize = size) }
    }

    fun setEraserMode(isEraser: Boolean) {
        _uiState.update { it.copy(isEraserMode = isEraser) }
    }

    fun startDrawingStroke(x: Float, y: Float) {
        val state = _uiState.value
        val strokeColor = if (state.isEraserMode) {
            state.settings.backgroundColor
        } else {
            state.drawColor
        }
        val stroke = DrawingStroke(
            points = listOf(com.pecmi.studio.domain.model.DrawingPoint(x, y)),
            color = strokeColor,
            width = state.drawBrushSize,
            isEraser = state.isEraserMode
        )
        _uiState.update { it.copy(activeCurrentStroke = stroke) }
    }

    fun appendDrawingPoint(x: Float, y: Float) {
        val currentStroke = _uiState.value.activeCurrentStroke ?: return
        val updatedPoints = currentStroke.points + com.pecmi.studio.domain.model.DrawingPoint(x, y)
        _uiState.update { it.copy(activeCurrentStroke = currentStroke.copy(points = updatedPoints)) }
    }

    fun finishDrawingStroke() {
        val stroke = _uiState.value.activeCurrentStroke
        if (stroke != null && stroke.points.size >= 2) {
            addDrawingStroke(stroke)
        }
        _uiState.update { it.copy(activeCurrentStroke = null) }
    }

    fun clearDrawingLayer() {
        val drawingLayer = _uiState.value.layers.find { it is Layer.Drawing } ?: return
        val layers = _uiState.value.layers.toMutableList()
        val index = layers.indexOfFirst { it.id == drawingLayer.id }
        if (index != -1) {
            layers[index] = (drawingLayer as Layer.Drawing).copy(strokes = emptyList())
            _uiState.update { it.copy(layers = layers) }
            saveHistoryState()
        }
    }

    fun addDrawingStroke(stroke: DrawingStroke) {
        var drawingLayer = _uiState.value.layers.find { it is Layer.Drawing } as? Layer.Drawing
        val layers = _uiState.value.layers.toMutableList()

        if (drawingLayer == null) {
            drawingLayer = Layer.Drawing(
                id = UUID.randomUUID().toString(),
                name = "Drawing Layer",
                strokes = listOf(stroke)
            )
            layers.add(drawingLayer)
        } else {
            val updatedStrokes = drawingLayer.strokes + stroke
            val index = layers.indexOfFirst { it.id == drawingLayer.id }
            layers[index] = drawingLayer.copy(strokes = updatedStrokes)
        }

        _uiState.update { it.copy(layers = layers) }
        saveHistoryState()
    }

    fun updateSelectedLayerPosition(dx: Float, dy: Float, layerId: String? = null) {
        val targetId = layerId ?: _uiState.value.selectedLayerId ?: return
        val updated = _uiState.value.layers.map { layer ->
            if (layer.id == targetId && !layer.isLocked) {
                when (layer) {
                    is Layer.Text -> layer.copy(x = layer.x + dx, y = layer.y + dy)
                    is Layer.Shape -> layer.copy(x = layer.x + dx, y = layer.y + dy)
                    is Layer.Image -> layer.copy(x = layer.x + dx, y = layer.y + dy)
                    is Layer.Drawing -> layer.copy(x = layer.x + dx, y = layer.y + dy)
                }
            } else layer
        }
        _uiState.update { it.copy(layers = updated) }
    }

    fun setSelectedLayerAbsolutePosition(x: Float, y: Float, layerId: String? = null) {
        val targetId = layerId ?: _uiState.value.selectedLayerId ?: return
        val updated = _uiState.value.layers.map { layer ->
            if (layer.id == targetId && !layer.isLocked) {
                when (layer) {
                    is Layer.Text -> layer.copy(x = x, y = y)
                    is Layer.Shape -> layer.copy(x = x, y = y)
                    is Layer.Image -> layer.copy(x = x, y = y)
                    is Layer.Drawing -> layer.copy(x = x, y = y)
                }
            } else layer
        }
        _uiState.update { it.copy(layers = updated) }
    }

    fun updateSelectedLayerScale(scaleFactor: Float, layerId: String? = null) {
        val targetId = layerId ?: _uiState.value.selectedLayerId ?: return
        val updated = _uiState.value.layers.map { layer ->
            if (layer.id == targetId && !layer.isLocked) {
                val newSx = (layer.scaleX * scaleFactor).coerceIn(0.2f, 5f)
                val newSy = (layer.scaleY * scaleFactor).coerceIn(0.2f, 5f)
                when (layer) {
                    is Layer.Text -> layer.copy(scaleX = newSx, scaleY = newSy)
                    is Layer.Shape -> layer.copy(scaleX = newSx, scaleY = newSy)
                    is Layer.Image -> layer.copy(scaleX = newSx, scaleY = newSy)
                    is Layer.Drawing -> layer.copy(scaleX = newSx, scaleY = newSy)
                }
            } else layer
        }
        _uiState.update { it.copy(layers = updated) }
    }

    fun updateSelectedLayerRotation(angleDegrees: Float, layerId: String? = null) {
        val targetId = layerId ?: _uiState.value.selectedLayerId ?: return
        val updated = _uiState.value.layers.map { layer ->
            if (layer.id == targetId && !layer.isLocked) {
                val newRot = (layer.rotation + angleDegrees) % 360f
                when (layer) {
                    is Layer.Text -> layer.copy(rotation = newRot)
                    is Layer.Shape -> layer.copy(rotation = newRot)
                    is Layer.Image -> layer.copy(rotation = newRot)
                    is Layer.Drawing -> layer.copy(rotation = newRot)
                }
            } else layer
        }
        _uiState.update { it.copy(layers = updated) }
    }

    fun onTransformFinished() {
        saveHistoryState()
    }

    fun updateLayer(updatedLayer: Layer) {
        val layers = _uiState.value.layers.map {
            if (it.id == updatedLayer.id) updatedLayer else it
        }
        _uiState.update { it.copy(layers = layers) }
        saveHistoryState()
    }

    fun deleteLayer(id: String) {
        val layers = _uiState.value.layers.filterNot { it.id == id }
        val newSelection = if (_uiState.value.selectedLayerId == id) layers.lastOrNull()?.id else _uiState.value.selectedLayerId
        _uiState.update { it.copy(layers = layers, selectedLayerId = newSelection) }
        saveHistoryState()
    }

    fun duplicateLayer(id: String) {
        val source = _uiState.value.layers.find { it.id == id } ?: return
        val copy = when (source) {
            is Layer.Text -> source.copy(id = UUID.randomUUID().toString(), name = "${source.name} Copy", x = source.x + 40f, y = source.y + 40f)
            is Layer.Shape -> source.copy(id = UUID.randomUUID().toString(), name = "${source.name} Copy", x = source.x + 40f, y = source.y + 40f)
            is Layer.Image -> source.copy(id = UUID.randomUUID().toString(), name = "${source.name} Copy", x = source.x + 40f, y = source.y + 40f)
            is Layer.Drawing -> source.copy(id = UUID.randomUUID().toString(), name = "${source.name} Copy", x = source.x + 40f, y = source.y + 40f)
        }
        val layers = _uiState.value.layers + copy
        _uiState.update { it.copy(layers = layers, selectedLayerId = copy.id) }
        saveHistoryState()
    }

    fun toggleLayerVisibility(id: String) {
        val layers = _uiState.value.layers.map { layer ->
            if (layer.id == id) {
                when (layer) {
                    is Layer.Text -> layer.copy(isVisible = !layer.isVisible)
                    is Layer.Shape -> layer.copy(isVisible = !layer.isVisible)
                    is Layer.Image -> layer.copy(isVisible = !layer.isVisible)
                    is Layer.Drawing -> layer.copy(isVisible = !layer.isVisible)
                }
            } else layer
        }
        _uiState.update { it.copy(layers = layers) }
    }

    fun toggleLayerLock(id: String) {
        val layers = _uiState.value.layers.map { layer ->
            if (layer.id == id) {
                when (layer) {
                    is Layer.Text -> layer.copy(isLocked = !layer.isLocked)
                    is Layer.Shape -> layer.copy(isLocked = !layer.isLocked)
                    is Layer.Image -> layer.copy(isLocked = !layer.isLocked)
                    is Layer.Drawing -> layer.copy(isLocked = !layer.isLocked)
                }
            } else layer
        }
        _uiState.update { it.copy(layers = layers) }
    }

    fun moveLayerUp(id: String) {
        val list = _uiState.value.layers.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _uiState.update { it.copy(layers = list) }
            saveHistoryState()
        }
    }

    fun moveLayerDown(id: String) {
        val list = _uiState.value.layers.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _uiState.update { it.copy(layers = list) }
            saveHistoryState()
        }
    }

    fun bringLayerToFront(id: String) {
        val list = _uiState.value.layers.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = list.removeAt(index)
            list.add(item)
            _uiState.update { it.copy(layers = list) }
            saveHistoryState()
        }
    }

    fun sendLayerToBack(id: String) {
        val list = _uiState.value.layers.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = list.removeAt(index)
            list.add(0, item)
            _uiState.update { it.copy(layers = list) }
            saveHistoryState()
        }
    }

    fun centerLayerHorizontally(id: String) {
        val updated = _uiState.value.layers.map { layer ->
            if (layer.id == id) {
                val cx = _uiState.value.settings.width / 2f
                when (layer) {
                    is Layer.Text -> layer.copy(x = cx)
                    is Layer.Shape -> layer.copy(x = cx)
                    is Layer.Image -> layer.copy(x = cx)
                    is Layer.Drawing -> layer.copy(x = cx)
                }
            } else layer
        }
        _uiState.update { it.copy(layers = updated) }
        saveHistoryState()
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(messageToast = message) }
    }

    fun updateCanvasSettings(settings: CanvasSettings) {
        _uiState.update { it.copy(settings = settings) }
        saveHistoryState()
    }

    // --- SAVE / EXPORT / PROJECT PERSISTENCE ---

    fun saveCurrentProject(title: String = "Pecmi Design") {
        viewModelScope.launch {
            val strings = getStringsForLanguage(appPreferences.language)
            try {
                repository.saveProject(
                    title = title,
                    width = _uiState.value.settings.width,
                    height = _uiState.value.settings.height,
                    layers = _uiState.value.layers,
                    settings = _uiState.value.settings
                )
                showToast(strings.projectSavedSuccess)
            } catch (e: Exception) {
                showToast(strings.projectSavedSuccess)
            }
        }
    }

    fun hasLastEdit(): Boolean {
        return appPreferences.lastEditTimestamp > 0L && !appPreferences.lastEditLayersJson.isNullOrBlank()
    }

    fun getLastEditTimestamp(): Long {
        return appPreferences.lastEditTimestamp
    }

    fun restoreLastEdit() {
        val strings = getStringsForLanguage(appPreferences.language)
        try {
            val layersJson = appPreferences.lastEditLayersJson ?: return
            val settingsJson = appPreferences.lastEditSettingsJson
            val restoredLayers = repository.deserializeLayers(layersJson)
            val restoredSettings = if (!settingsJson.isNullOrBlank()) {
                repository.deserializeSettings(settingsJson)
            } else {
                _uiState.value.settings
            }
            _uiState.update {
                it.copy(
                    layers = restoredLayers,
                    settings = restoredSettings,
                    selectedLayerId = restoredLayers.firstOrNull()?.id
                )
            }
            showToast(strings.autoSaveRestored)
        } catch (e: Exception) {
            showToast(strings.autoSaveRestoreFailed)
        }
    }

    fun clearLastEdit() {
        appPreferences.lastEditLayersJson = null
        appPreferences.lastEditSettingsJson = null
        appPreferences.lastEditTimestamp = 0L
    }

    fun importProjectFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val strings = getStringsForLanguage(appPreferences.language)
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    showToast(strings.projectImportSuccess)
                } else {
                    showToast(strings.projectImportFailed)
                }
            } catch (e: Exception) {
                showToast(strings.projectImportFailed)
            }
        }
    }

    fun activateWatermarkBypass(durationMinutes: Int = 30) {
        // No watermark
    }

    fun isWatermarkRemoved(): Boolean {
        return true
    }

    fun exportCanvasImage(format: ExportFormat, scaleFactor: Float, quality: Int, forceRemoveWatermark: Boolean = false) {
        viewModelScope.launch {
            try {
                val bitmap = ExportManager.renderCanvasToBitmap(
                    width = _uiState.value.settings.width,
                    height = _uiState.value.settings.height,
                    scaleFactor = scaleFactor,
                    settings = _uiState.value.settings,
                    layers = _uiState.value.layers,
                    includeWatermark = false
                )
                val saveResult = ExportManager.saveImageToGallery(
                    context = getApplication(),
                    bitmap = bitmap,
                    filename = "Pecmi_${System.currentTimeMillis()}",
                    format = format,
                    quality = quality
                )
                if (saveResult.success) {
                    val cacheFile = ExportManager.exportToFile(
                        context = getApplication(),
                        bitmap = bitmap,
                        filename = "Pecmi_preview",
                        format = format,
                        quality = quality
                    )
                    _uiState.update {
                        it.copy(
                            exportedFile = cacheFile,
                            messageToast = "تم حفظ الصورة في المعرض! 🖼️\nPictures/Pecmi"
                        )
                    }
                } else {
                    showToast("فشل الحفظ: ${saveResult.errorMessage}")
                }
            } catch (e: Exception) {
                showToast("حدث خطأ أثناء حفظ الصورة: ${e.localizedMessage}")
            }
        }
    }

    fun shareExportedImage() {
        val file = _uiState.value.exportedFile ?: return
        ExportManager.shareImageFile(getApplication(), file)
    }

    fun clearToast() {
        _uiState.update { it.copy(messageToast = null) }
    }
}
