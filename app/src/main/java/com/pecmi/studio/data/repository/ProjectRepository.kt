package com.pecmi.studio.data.repository

import com.pecmi.studio.data.dao.ProjectDao
import com.pecmi.studio.data.entity.ProjectEntity
import com.pecmi.studio.domain.model.CanvasSettings
import com.pecmi.studio.domain.model.Layer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    suspend fun saveProject(
        id: Long = 0,
        title: String,
        width: Int,
        height: Int,
        layers: List<Layer>,
        settings: CanvasSettings,
        thumbnailPath: String? = null
    ): Long {
        val layersAdapter = moshi.adapter(Any::class.java)
        // We convert layers to simple maps/lists for standard serialization
        val layersSerialized = serializeLayers(layers)
        val settingsSerialized = serializeSettings(settings)

        val entity = ProjectEntity(
            id = id,
            title = title,
            width = width,
            height = height,
            layersJson = layersSerialized,
            settingsJson = settingsSerialized,
            thumbnailPath = thumbnailPath,
            updatedAt = System.currentTimeMillis()
        )
        return projectDao.insertProject(entity)
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
    }

    fun serializeLayers(layers: List<Layer>): String {
        val jsonArray = org.json.JSONArray()
        for (layer in layers) {
            val obj = org.json.JSONObject()
            obj.put("id", layer.id)
            obj.put("name", layer.name)
            obj.put("type", layer.type.name)
            obj.put("x", layer.x)
            obj.put("y", layer.y)
            obj.put("scaleX", layer.scaleX)
            obj.put("scaleY", layer.scaleY)
            obj.put("rotation", layer.rotation)
            obj.put("opacity", layer.opacity)
            obj.put("isVisible", layer.isVisible)
            obj.put("isLocked", layer.isLocked)
            obj.put("blendMode", layer.blendMode.name)

            when (layer) {
                is Layer.Text -> {
                    obj.put("text", layer.text)
                    obj.put("textColor", layer.textColor)
                    obj.put("fontSize", layer.fontSize)
                    obj.put("fontFamilyName", layer.fontFamilyName)
                    obj.put("fontPath", layer.fontPath ?: "")
                    obj.put("fontStyleBold", layer.fontStyleBold)
                    obj.put("fontStyleItalic", layer.fontStyleItalic)
                    obj.put("underline", layer.underline)
                    obj.put("strikethrough", layer.strikethrough)
                    obj.put("textTransform", layer.textTransform)
                    obj.put("alignment", layer.alignment)
                    obj.put("boxWidth", layer.boxWidth)
                    obj.put("letterSpacing", layer.letterSpacing)
                    obj.put("lineSpacing", layer.lineSpacing)
                    obj.put("strokeWidth", layer.strokeWidth)
                    obj.put("strokeColor", layer.strokeColor)
                    obj.put("shadowBlur", layer.shadowBlur)
                    obj.put("shadowDx", layer.shadowDx)
                    obj.put("shadowDy", layer.shadowDy)
                    obj.put("shadowColor", layer.shadowColor)
                    obj.put("glowRadius", layer.glowRadius)
                    obj.put("glowColor", layer.glowColor)
                    obj.put("isCurved", layer.isCurved)
                    obj.put("curveRadius", layer.curveRadius)
                    obj.put("hasBackground", layer.hasBackground)
                    obj.put("backgroundColor", layer.backgroundColor)
                    obj.put("backgroundCornerRadius", layer.backgroundCornerRadius)
                    obj.put("backgroundPadding", layer.backgroundPadding)
                    obj.put("isGradientEnabled", layer.isGradientEnabled)
                    val gradArray = org.json.JSONArray()
                    layer.gradientColors.forEach { gradArray.put(it) }
                    obj.put("gradientColors", gradArray)
                    obj.put("presetStyle", layer.presetStyle.name)
                }
                is Layer.Shape -> {
                    obj.put("shapeType", layer.shapeType.name)
                    obj.put("width", layer.width)
                    obj.put("height", layer.height)
                    obj.put("fillColor", layer.fillColor)
                    obj.put("strokeColor", layer.strokeColor)
                    obj.put("strokeWidth", layer.strokeWidth)
                    obj.put("cornerRadius", layer.cornerRadius)
                    obj.put("polygonSides", layer.polygonSides)
                    obj.put("flipH", layer.flipH)
                    obj.put("flipV", layer.flipV)
                    obj.put("shadowBlur", layer.shadowBlur)
                    obj.put("shadowColor", layer.shadowColor)
                    obj.put("isGradientEnabled", layer.isGradientEnabled)
                    val gradArray = org.json.JSONArray()
                    layer.gradientColors.forEach { gradArray.put(it) }
                    obj.put("gradientColors", gradArray)
                }
                is Layer.Image -> {
                    obj.put("imagePath", layer.imagePath ?: "")
                    obj.put("width", layer.width)
                    obj.put("height", layer.height)
                    obj.put("flipH", layer.flipH)
                    obj.put("flipV", layer.flipV)
                    obj.put("brightness", layer.brightness)
                    obj.put("contrast", layer.contrast)
                    obj.put("saturation", layer.saturation)
                    obj.put("hue", layer.hue)
                    obj.put("warmth", layer.warmth)
                    obj.put("blur", layer.blur)
                    obj.put("cornerRadius", layer.cornerRadius)
                    obj.put("borderWidth", layer.borderWidth)
                    obj.put("borderColor", layer.borderColor)
                }
                is Layer.Drawing -> {
                    val strokesArray = org.json.JSONArray()
                    for (stroke in layer.strokes) {
                        val strokeObj = org.json.JSONObject()
                        strokeObj.put("color", stroke.color)
                        strokeObj.put("width", stroke.width)
                        strokeObj.put("alpha", stroke.alpha)
                        strokeObj.put("isEraser", stroke.isEraser)
                        val pointsArray = org.json.JSONArray()
                        for (pt in stroke.points) {
                            val ptObj = org.json.JSONObject()
                            ptObj.put("x", pt.x)
                            ptObj.put("y", pt.y)
                            pointsArray.put(ptObj)
                        }
                        strokeObj.put("points", pointsArray)
                        strokesArray.put(strokeObj)
                    }
                    obj.put("strokes", strokesArray)
                }
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun serializeSettings(settings: CanvasSettings): String {
        val obj = org.json.JSONObject()
        obj.put("width", settings.width)
        obj.put("height", settings.height)
        obj.put("bgColor", settings.backgroundColor)
        obj.put("isTransparent", settings.isTransparent)
        return obj.toString()
    }

    fun deserializeLayers(json: String): List<Layer> {
        val layers = mutableListOf<Layer>()
        try {
            if (json.isBlank() || json == "[]") return emptyList()
            val jsonArray = org.json.JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.optString("type", "TEXT")
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val name = obj.optString("name", "Layer")
                val x = obj.optDouble("x", 0.0).toFloat()
                val y = obj.optDouble("y", 0.0).toFloat()
                val scaleX = obj.optDouble("scaleX", 1.0).toFloat()
                val scaleY = obj.optDouble("scaleY", 1.0).toFloat()
                val rotation = obj.optDouble("rotation", 0.0).toFloat()
                val opacity = obj.optDouble("opacity", 1.0).toFloat()
                val isVisible = obj.optBoolean("isVisible", true)
                val isLocked = obj.optBoolean("isLocked", false)
                val blendModeStr = obj.optString("blendMode", "NORMAL")
                val blendMode = try { com.pecmi.studio.domain.model.CanvasBlendMode.valueOf(blendModeStr) } catch (e: Exception) { com.pecmi.studio.domain.model.CanvasBlendMode.NORMAL }

                when (typeStr) {
                    "TEXT" -> {
                        val text = obj.optString("text", "Text")
                        val textColor = obj.optInt("textColor", 0xFFFFFFFF.toInt())
                        val fontSize = obj.optDouble("fontSize", 48.0).toFloat()
                        val fontFamilyName = obj.optString("fontFamilyName", "Default")
                        val fontPath = obj.optString("fontPath", "").takeIf { it.isNotBlank() }
                        val fontStyleBold = obj.optBoolean("fontStyleBold", false)
                        val fontStyleItalic = obj.optBoolean("fontStyleItalic", false)
                        val underline = obj.optBoolean("underline", false)
                        val strikethrough = obj.optBoolean("strikethrough", false)
                        val textTransform = obj.optInt("textTransform", 0)
                        val alignment = obj.optInt("alignment", 1)
                        val boxWidth = obj.optDouble("boxWidth", 360.0).toFloat()
                        val letterSpacing = obj.optDouble("letterSpacing", 0.0).toFloat()
                        val lineSpacing = obj.optDouble("lineSpacing", 1.0).toFloat()
                        val strokeWidth = obj.optDouble("strokeWidth", 0.0).toFloat()
                        val strokeColor = obj.optInt("strokeColor", 0xFF000000.toInt())
                        val shadowBlur = obj.optDouble("shadowBlur", 0.0).toFloat()
                        val shadowDx = obj.optDouble("shadowDx", 4.0).toFloat()
                        val shadowDy = obj.optDouble("shadowDy", 4.0).toFloat()
                        val shadowColor = obj.optInt("shadowColor", 0x80000000.toInt())
                        val glowRadius = obj.optDouble("glowRadius", 0.0).toFloat()
                        val glowColor = obj.optInt("glowColor", 0x8000FFFF.toInt())
                        val isCurved = obj.optBoolean("isCurved", false)
                        val curveRadius = obj.optDouble("curveRadius", 120.0).toFloat()
                        val hasBackground = obj.optBoolean("hasBackground", false)
                        val backgroundColor = obj.optInt("backgroundColor", 0xCC000000.toInt())
                        val backgroundCornerRadius = obj.optDouble("backgroundCornerRadius", 16.0).toFloat()
                        val backgroundPadding = obj.optDouble("backgroundPadding", 16.0).toFloat()
                        val isGradientEnabled = obj.optBoolean("isGradientEnabled", false)
                        val gradList = mutableListOf<Int>()
                        val gradArray = obj.optJSONArray("gradientColors")
                        if (gradArray != null) {
                            for (g in 0 until gradArray.length()) {
                                gradList.add(gradArray.getInt(g))
                            }
                        }
                        if (gradList.isEmpty()) {
                            gradList.add(0xFF00E5FF.toInt())
                            gradList.add(0xFF7C4DFF.toInt())
                        }
                        val presetStyleStr = obj.optString("presetStyle", "NONE")
                        val presetStyle = try { com.pecmi.studio.domain.model.TextPreset.valueOf(presetStyleStr) } catch (e: Exception) { com.pecmi.studio.domain.model.TextPreset.NONE }

                        layers.add(
                            Layer.Text(
                                id = id,
                                name = name,
                                x = x,
                                y = y,
                                scaleX = scaleX,
                                scaleY = scaleY,
                                rotation = rotation,
                                opacity = opacity,
                                isVisible = isVisible,
                                isLocked = isLocked,
                                blendMode = blendMode,
                                text = text,
                                textColor = textColor,
                                fontSize = fontSize,
                                fontFamilyName = fontFamilyName,
                                fontPath = fontPath,
                                fontStyleBold = fontStyleBold,
                                fontStyleItalic = fontStyleItalic,
                                underline = underline,
                                strikethrough = strikethrough,
                                textTransform = textTransform,
                                alignment = alignment,
                                boxWidth = boxWidth,
                                letterSpacing = letterSpacing,
                                lineSpacing = lineSpacing,
                                strokeWidth = strokeWidth,
                                strokeColor = strokeColor,
                                shadowBlur = shadowBlur,
                                shadowDx = shadowDx,
                                shadowDy = shadowDy,
                                shadowColor = shadowColor,
                                glowRadius = glowRadius,
                                glowColor = glowColor,
                                isCurved = isCurved,
                                curveRadius = curveRadius,
                                hasBackground = hasBackground,
                                backgroundColor = backgroundColor,
                                backgroundCornerRadius = backgroundCornerRadius,
                                backgroundPadding = backgroundPadding,
                                isGradientEnabled = isGradientEnabled,
                                gradientColors = gradList,
                                presetStyle = presetStyle
                            )
                        )
                    }
                    "SHAPE" -> {
                        val shapeTypeStr = obj.optString("shapeType", "RECTANGLE")
                        val shapeType = try { com.pecmi.studio.domain.model.ShapeType.valueOf(shapeTypeStr) } catch (e: Exception) { com.pecmi.studio.domain.model.ShapeType.RECTANGLE }
                        val width = obj.optDouble("width", 300.0).toFloat()
                        val height = obj.optDouble("height", 300.0).toFloat()
                        val fillColor = obj.optInt("fillColor", 0xFF3B82F6.toInt())
                        val strokeColor = obj.optInt("strokeColor", 0xFFFFFFFF.toInt())
                        val strokeWidth = obj.optDouble("strokeWidth", 0.0).toFloat()
                        val cornerRadius = obj.optDouble("cornerRadius", 24.0).toFloat()
                        val polygonSides = obj.optInt("polygonSides", 5)
                        val flipH = obj.optBoolean("flipH", false)
                        val flipV = obj.optBoolean("flipV", false)
                        val shadowBlur = obj.optDouble("shadowBlur", 0.0).toFloat()
                        val shadowColor = obj.optInt("shadowColor", 0x80000000.toInt())
                        val isGradientEnabled = obj.optBoolean("isGradientEnabled", false)
                        val gradList = mutableListOf<Int>()
                        val gradArray = obj.optJSONArray("gradientColors")
                        if (gradArray != null) {
                            for (g in 0 until gradArray.length()) {
                                gradList.add(gradArray.getInt(g))
                            }
                        }
                        if (gradList.isEmpty()) {
                            gradList.add(0xFF3B82F6.toInt())
                            gradList.add(0xFF8B5CF6.toInt())
                        }

                        layers.add(
                            Layer.Shape(
                                id = id,
                                name = name,
                                x = x,
                                y = y,
                                scaleX = scaleX,
                                scaleY = scaleY,
                                rotation = rotation,
                                opacity = opacity,
                                isVisible = isVisible,
                                isLocked = isLocked,
                                blendMode = blendMode,
                                shapeType = shapeType,
                                width = width,
                                height = height,
                                fillColor = fillColor,
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                cornerRadius = cornerRadius,
                                polygonSides = polygonSides,
                                flipH = flipH,
                                flipV = flipV,
                                shadowBlur = shadowBlur,
                                shadowColor = shadowColor,
                                isGradientEnabled = isGradientEnabled,
                                gradientColors = gradList
                            )
                        )
                    }
                    "IMAGE" -> {
                        val imagePath = obj.optString("imagePath", "").takeIf { it.isNotBlank() }
                        val width = obj.optInt("width", 400)
                        val height = obj.optInt("height", 400)
                        val flipH = obj.optBoolean("flipH", false)
                        val flipV = obj.optBoolean("flipV", false)
                        val brightness = obj.optDouble("brightness", 0.0).toFloat()
                        val contrast = obj.optDouble("contrast", 1.0).toFloat()
                        val saturation = obj.optDouble("saturation", 1.0).toFloat()
                        val hue = obj.optDouble("hue", 0.0).toFloat()
                        val warmth = obj.optDouble("warmth", 0.0).toFloat()
                        val blur = obj.optDouble("blur", 0.0).toFloat()
                        val cornerRadius = obj.optDouble("cornerRadius", 0.0).toFloat()
                        val borderWidth = obj.optDouble("borderWidth", 0.0).toFloat()
                        val borderColor = obj.optInt("borderColor", 0xFFFFFFFF.toInt())

                        // Load bitmap from imagePath if available
                        val bitmap = if (!imagePath.isNullOrEmpty() && java.io.File(imagePath).exists()) {
                            try { android.graphics.BitmapFactory.decodeFile(imagePath) } catch (e: Exception) { null }
                        } else null

                        layers.add(
                            Layer.Image(
                                id = id,
                                name = name,
                                x = x,
                                y = y,
                                scaleX = scaleX,
                                scaleY = scaleY,
                                rotation = rotation,
                                opacity = opacity,
                                isVisible = isVisible,
                                isLocked = isLocked,
                                blendMode = blendMode,
                                imagePath = imagePath,
                                bitmap = bitmap,
                                width = width,
                                height = height,
                                flipH = flipH,
                                flipV = flipV,
                                brightness = brightness,
                                contrast = contrast,
                                saturation = saturation,
                                hue = hue,
                                warmth = warmth,
                                blur = blur,
                                cornerRadius = cornerRadius,
                                borderWidth = borderWidth,
                                borderColor = borderColor
                            )
                        )
                    }
                    "DRAWING" -> {
                        val strokesList = mutableListOf<com.pecmi.studio.domain.model.DrawingStroke>()
                        val strokesArray = obj.optJSONArray("strokes")
                        if (strokesArray != null) {
                            for (s in 0 until strokesArray.length()) {
                                val strokeObj = strokesArray.getJSONObject(s)
                                val color = strokeObj.optInt("color", 0xFF000000.toInt())
                                val strokeW = strokeObj.optDouble("width", 10.0).toFloat()
                                val strokeAlpha = strokeObj.optDouble("alpha", 1.0).toFloat()
                                val isEraser = strokeObj.optBoolean("isEraser", false)
                                val ptsList = mutableListOf<com.pecmi.studio.domain.model.DrawingPoint>()
                                val ptsArray = strokeObj.optJSONArray("points")
                                if (ptsArray != null) {
                                    for (p in 0 until ptsArray.length()) {
                                        val ptObj = ptsArray.getJSONObject(p)
                                        val px = ptObj.optDouble("x", 0.0).toFloat()
                                        val py = ptObj.optDouble("y", 0.0).toFloat()
                                        ptsList.add(com.pecmi.studio.domain.model.DrawingPoint(px, py))
                                    }
                                }
                                strokesList.add(
                                    com.pecmi.studio.domain.model.DrawingStroke(
                                        points = ptsList,
                                        color = color,
                                        width = strokeW,
                                        alpha = strokeAlpha,
                                        isEraser = isEraser
                                    )
                                )
                            }
                        }
                        layers.add(
                            Layer.Drawing(
                                id = id,
                                name = name,
                                x = x,
                                y = y,
                                scaleX = scaleX,
                                scaleY = scaleY,
                                rotation = rotation,
                                opacity = opacity,
                                isVisible = isVisible,
                                isLocked = isLocked,
                                blendMode = blendMode,
                                strokes = strokesList
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return layers
    }

    fun deserializeSettings(json: String): CanvasSettings {
        return try {
            val obj = org.json.JSONObject(json)
            val w = obj.optInt("width", 1080)
            val h = obj.optInt("height", 1080)
            val bgColor = obj.optInt("bgColor", 0xFFFFFFFF.toInt())
            val isTransparent = obj.optBoolean("isTransparent", false)
            CanvasSettings(width = w, height = h, backgroundColor = bgColor, isTransparent = isTransparent)
        } catch (e: Exception) {
            CanvasSettings()
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
    }
}
