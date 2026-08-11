package com.pecmi.studio.graphics

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Camera
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.pecmi.studio.domain.model.DrawingStroke
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.domain.model.ShapeType
import com.pecmi.studio.domain.model.TextPreset
import com.pecmi.studio.effects.ImageFilterApplier
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CanvasRenderer {

    private val typefaceCache = java.util.concurrent.ConcurrentHashMap<String, Typeface>()

    private fun getTypeface(fontPath: String?, fontFamilyName: String): Typeface {
        val key = fontPath ?: fontFamilyName.lowercase()
        return typefaceCache.getOrPut(key) {
            try {
                if (!fontPath.isNullOrEmpty() && File(fontPath).exists()) {
                    Typeface.createFromFile(fontPath)
                } else {
                    when (fontFamilyName.lowercase()) {
                        "serif" -> Typeface.SERIF
                        "sans" -> Typeface.SANS_SERIF
                        "monospace" -> Typeface.MONOSPACE
                        "cursive" -> Typeface.create("cursive", Typeface.NORMAL)
                        else -> Typeface.DEFAULT
                    }
                }
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
    }

    private var cachedCheckerboardPaint: android.graphics.Paint? = null

    private fun getCheckerboardPaint(): android.graphics.Paint {
        if (cachedCheckerboardPaint == null) {
            val sz = 32
            val bmp = android.graphics.Bitmap.createBitmap(sz * 2, sz * 2, android.graphics.Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(bmp)
            val light = 0xFFE2E8F0.toInt()
            val dark = 0xFFCBD5E1.toInt()
            val p = android.graphics.Paint()
            p.color = light
            c.drawRect(0f, 0f, sz * 2f, sz * 2f, p)
            p.color = dark
            c.drawRect(0f, 0f, sz.toFloat(), sz.toFloat(), p)
            c.drawRect(sz.toFloat(), sz.toFloat(), sz * 2f, sz * 2f, p)
            val shader = android.graphics.BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            cachedCheckerboardPaint = android.graphics.Paint().apply {
                this.shader = shader
            }
        }
        return cachedCheckerboardPaint!!
    }

    fun drawCheckerboard(drawScope: DrawScope, canvasWidth: Float, canvasHeight: Float) {
        drawScope.drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(0f, 0f, canvasWidth, canvasHeight, getCheckerboardPaint())
        }
    }

    fun drawGrid(drawScope: DrawScope, width: Float, height: Float, gridSize: Float) {
        if (gridSize <= 0f) return
        drawScope.drawIntoCanvas { canvas ->
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x3394A3B8.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 1f
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 8f), 0f)
            }
            val gridPath = Path()
            var x = gridSize
            while (x < width) {
                gridPath.moveTo(x, 0f)
                gridPath.lineTo(x, height)
                x += gridSize
            }
            var y = gridSize
            while (y < height) {
                gridPath.moveTo(0f, y)
                gridPath.lineTo(width, y)
                y += gridSize
            }
            canvas.nativeCanvas.drawPath(gridPath, gridPaint)
        }
    }

    fun drawGuidelines(drawScope: DrawScope, width: Float, height: Float) {
        val centerColor = Color(0xCC8B5CF6)
        val thirdsColor = Color(0x666366F1)
        val safeAreaColor = Color(0x44EC4899)

        val centerDash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        val thirdsDash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        val safeDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        // 1. Primary Center Guidelines (Vertical & Horizontal Center)
        drawScope.drawLine(centerColor, Offset(width / 2f, 0f), Offset(width / 2f, height), strokeWidth = 2.5f, pathEffect = centerDash)
        drawScope.drawLine(centerColor, Offset(0f, height / 2f), Offset(width, height / 2f), strokeWidth = 2.5f, pathEffect = centerDash)

        // 2. Rule of Thirds Guidelines (Adapts dynamically to any aspect ratio & dimensions)
        val wThird = width / 3f
        val hThird = height / 3f

        drawScope.drawLine(thirdsColor, Offset(wThird, 0f), Offset(wThird, height), strokeWidth = 1.5f, pathEffect = thirdsDash)
        drawScope.drawLine(thirdsColor, Offset(2f * wThird, 0f), Offset(2f * wThird, height), strokeWidth = 1.5f, pathEffect = thirdsDash)

        drawScope.drawLine(thirdsColor, Offset(0f, hThird), Offset(width, hThird), strokeWidth = 1.5f, pathEffect = thirdsDash)
        drawScope.drawLine(thirdsColor, Offset(0f, 2f * hThird), Offset(width, 2f * hThird), strokeWidth = 1.5f, pathEffect = thirdsDash)

        // 3. Safe Area Margin Guideline (5% inset from all outer canvas borders)
        val marginX = width * 0.05f
        val marginY = height * 0.05f
        drawScope.drawRect(
            color = safeAreaColor,
            topLeft = Offset(marginX, marginY),
            size = Size(width - 2f * marginX, height - 2f * marginY),
            style = Stroke(width = 1.5f, pathEffect = safeDash)
        )
    }

    fun drawLayer(
        drawScope: DrawScope,
        layer: Layer,
        isSelected: Boolean = false,
        viewScale: Float = 1f,
        renderScale: Float = 1f
    ) {
        if (!layer.isVisible) return

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()

            // Apply base position & 2D transform (scaled by renderScale)
            nativeCanvas.translate(layer.x * renderScale, layer.y * renderScale)
            nativeCanvas.rotate(layer.rotation)
            nativeCanvas.scale(layer.scaleX, layer.scaleY)

            // 3D Rotation / Skew Matrix
            val (rotX, rotY, skewX) = when (layer) {
                is Layer.Text -> Triple(layer.rotX, layer.rotY, layer.skewX)
                is Layer.Image -> Triple(layer.rotX, layer.rotY, layer.skewX)
                else -> Triple(0f, 0f, 0f)
            }

            if (rotX != 0f || rotY != 0f || skewX != 0f) {
                val camera = Camera()
                val matrix = Matrix()
                camera.save()
                camera.rotateX(rotX)
                camera.rotateY(rotY)
                camera.getMatrix(matrix)
                camera.restore()
                if (skewX != 0f) {
                    matrix.preSkew(skewX, 0f)
                }
                nativeCanvas.concat(matrix)
            }

            when (layer) {
                is Layer.Text -> drawTextLayer(drawScope, nativeCanvas, layer, renderScale)
                is Layer.Shape -> drawShapeLayer(drawScope, nativeCanvas, layer, renderScale)
                is Layer.Image -> drawImageLayer(drawScope, nativeCanvas, layer, renderScale)
                is Layer.Drawing -> drawDrawingLayer(drawScope, nativeCanvas, layer, renderScale)
            }

            nativeCanvas.restore()

            if (isSelected) {
                drawSelectionBoundingBox(drawScope, layer, viewScale)
            }
        }
    }

    private fun drawTextLayer(
        drawScope: DrawScope,
        canvas: android.graphics.Canvas,
        layer: Layer.Text,
        renderScale: Float = 1f
    ) {
        canvas.save()

        // Flip transformations
        val scaleH = if (layer.flipH) -1f else 1f
        val scaleV = if (layer.flipV) -1f else 1f
        if (scaleH != 1f || scaleV != 1f) {
            canvas.scale(scaleH, scaleV)
        }

        // Font Typeface
        val typeface = getTypeface(layer.fontPath, layer.fontFamilyName)

        // Text Content Transformation
        val processedText = when (layer.textTransform) {
            1 -> layer.text.uppercase()
            2 -> layer.text.lowercase()
            3 -> layer.text.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            else -> layer.text
        }

        val scaledFontSize = layer.fontSize * renderScale
        val textPaint = android.text.TextPaint(
            Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.SUBPIXEL_TEXT_FLAG
        ).apply {
            textSize = scaledFontSize
            color = layer.textColor
            this.typeface = typeface
            isFakeBoldText = layer.fontStyleBold
            textSkewX = if (layer.fontStyleItalic) -0.25f else 0f
            isUnderlineText = layer.underline
            isStrikeThruText = layer.strikethrough
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            if (layer.letterSpacing != 0f) {
                letterSpacing = layer.letterSpacing * 0.05f
            }
        }

        // Preset styling
        applyTextPreset(textPaint, layer.presetStyle, scaledFontSize, renderScale)

        val scaledBoxWidth = (layer.boxWidth * renderScale).toInt().coerceAtLeast((40 * renderScale).toInt())
        val layoutAlignment = when (layer.alignment) {
            0 -> android.text.Layout.Alignment.ALIGN_NORMAL
            2 -> android.text.Layout.Alignment.ALIGN_OPPOSITE
            else -> android.text.Layout.Alignment.ALIGN_CENTER
        }

        // Create StaticLayout for dynamic word wrapping and multiline bounds at full target scale
        val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.text.StaticLayout.Builder.obtain(processedText, 0, processedText.length, textPaint, scaledBoxWidth)
                .setAlignment(layoutAlignment)
                .setLineSpacing(0f, layer.lineSpacing)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.text.StaticLayout(
                processedText, textPaint, scaledBoxWidth,
                layoutAlignment, layer.lineSpacing, 0f, true
            )
        }

        val layoutHeight = staticLayout.height.toFloat()

        // Gradient Fill
        if (layer.isGradientEnabled && layer.gradientColors.size >= 2) {
            val shader = LinearGradient(
                -scaledBoxWidth / 2f, -layoutHeight / 2f, scaledBoxWidth / 2f, layoutHeight / 2f,
                layer.gradientColors.toIntArray(), null, Shader.TileMode.CLAMP
            )
            textPaint.shader = shader
        }

        // Shadow & Glow
        if (layer.glowRadius > 0f) {
            textPaint.setShadowLayer(layer.glowRadius * renderScale, 0f, 0f, layer.glowColor)
        } else if (layer.shadowBlur > 0f || layer.shadowDx != 0f || layer.shadowDy != 0f) {
            textPaint.setShadowLayer(
                (layer.shadowBlur * renderScale).coerceAtLeast(1f),
                layer.shadowDx * renderScale,
                layer.shadowDy * renderScale,
                layer.shadowColor
            )
        }

        // Background Box
        if (layer.hasBackground) {
            val pad = layer.backgroundPadding * renderScale
            val cornerRad = layer.backgroundCornerRadius * renderScale
            val bgRect = RectF(-scaledBoxWidth / 2f - pad, -layoutHeight / 2f - pad, scaledBoxWidth / 2f + pad, layoutHeight / 2f + pad)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                color = layer.backgroundColor
                style = Paint.Style.FILL
                alpha = (layer.opacity * ((layer.backgroundColor ushr 24) / 255f) * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRoundRect(bgRect, cornerRad, cornerRad, bgPaint)

            if (layer.bgStrokeWidth > 0f) {
                val bgStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                    color = layer.bgStrokeColor
                    style = Paint.Style.STROKE
                    strokeWidth = layer.bgStrokeWidth * renderScale
                }
                canvas.drawRoundRect(bgRect, cornerRad, cornerRad, bgStrokePaint)
            }
        }

        // Curved Text vs Wrapped Layout
        if (layer.isCurved) {
            val path = Path()
            val radius = (layer.curveRadius * renderScale).coerceAtLeast(20f * renderScale)
            val rect = RectF(-radius, -radius, radius, radius)
            path.addArc(rect, 180f, 180f)

            if (layer.strokeWidth > 0f) {
                val strokePaint = Paint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = layer.strokeWidth * renderScale
                    color = layer.strokeColor
                }
                canvas.drawTextOnPath(processedText, path, 0f, 0f, strokePaint)
            }
            canvas.drawTextOnPath(processedText, path, 0f, 0f, textPaint)
        } else {
            canvas.save()
            canvas.translate(-scaledBoxWidth / 2f, -layoutHeight / 2f)

            // Optional Stroke
            if (layer.strokeWidth > 0f) {
                val strokeTextPaint = android.text.TextPaint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = layer.strokeWidth * renderScale
                    color = layer.strokeColor
                }
                val strokeLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.text.StaticLayout.Builder.obtain(processedText, 0, processedText.length, strokeTextPaint, scaledBoxWidth)
                        .setAlignment(layoutAlignment)
                        .setLineSpacing(0f, layer.lineSpacing)
                        .setIncludePad(true)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    android.text.StaticLayout(
                        processedText, strokeTextPaint, scaledBoxWidth,
                        layoutAlignment, layer.lineSpacing, 0f, true
                    )
                }
                strokeLayout.draw(canvas)
            }

            staticLayout.draw(canvas)
            canvas.restore()

            // Reflection Effect
            if (layer.reflectionEnabled) {
                val camera = Camera()
                val matrix = Matrix()
                canvas.save()
                camera.save()
                camera.rotateX(180f)
                camera.getMatrix(matrix)
                camera.restore()
                matrix.preTranslate(0f, -layoutHeight / 2f)
                matrix.postTranslate(0f, layoutHeight / 2f + 12f * renderScale)
                canvas.concat(matrix)

                val reflPaint = android.text.TextPaint(textPaint).apply {
                    alpha = (textPaint.alpha * 0.3f).toInt()
                }
                val reflLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.text.StaticLayout.Builder.obtain(processedText, 0, processedText.length, reflPaint, scaledBoxWidth)
                        .setAlignment(layoutAlignment)
                        .setLineSpacing(0f, layer.lineSpacing)
                        .setIncludePad(true)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    android.text.StaticLayout(
                        processedText, reflPaint, scaledBoxWidth,
                        layoutAlignment, layer.lineSpacing, 0f, true
                    )
                }
                canvas.save()
                canvas.translate(-scaledBoxWidth / 2f, -layoutHeight / 2f)
                reflLayout.draw(canvas)
                canvas.restore()
                canvas.restore()
            }
        }

        canvas.restore()
    }

    private fun applyTextPreset(paint: Paint, preset: TextPreset, fontSize: Float, renderScale: Float = 1f) {
        when (preset) {
            TextPreset.NEON -> {
                paint.color = 0xFF00E5FF.toInt()
                paint.setShadowLayer(24f * renderScale, 0f, 0f, 0xFF00E5FF.toInt())
            }
            TextPreset.GOLD -> {
                val goldGradient = LinearGradient(
                    0f, 0f, 0f, fontSize,
                    intArrayOf(0xFFFFD700.toInt(), 0xFFFFA000.toInt(), 0xFF8D6E63.toInt(), 0xFFFFD700.toInt()),
                    floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP
                )
                paint.shader = goldGradient
            }
            TextPreset.SILVER -> {
                val silverGradient = LinearGradient(
                    0f, 0f, 0f, fontSize,
                    intArrayOf(0xFFECEFF1.toInt(), 0xFFB0BEC5.toInt(), 0xFF37474F.toInt(), 0xFFECEFF1.toInt()),
                    floatArrayOf(0f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP
                )
                paint.shader = silverGradient
            }
            TextPreset.GLASS -> {
                paint.color = 0x80FFFFFF.toInt()
                paint.setShadowLayer(12f * renderScale, 2f * renderScale, 2f * renderScale, 0x40000000.toInt())
            }
            TextPreset.CHROME -> {
                val chromeGradient = LinearGradient(
                    0f, 0f, 0f, fontSize,
                    intArrayOf(0xFF00E5FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF1A237E.toInt(), 0xFF00E5FF.toInt()),
                    floatArrayOf(0f, 0.4f, 0.6f, 1f), Shader.TileMode.CLAMP
                )
                paint.shader = chromeGradient
            }
            TextPreset.EMBOSS -> {
                paint.setShadowLayer(6f * renderScale, -3f * renderScale, -3f * renderScale, 0xFFFFFFFF.toInt())
            }
            TextPreset.POP3D -> {
                paint.setShadowLayer(14f * renderScale, 8f * renderScale, 8f * renderScale, 0xFF000000.toInt())
            }
            else -> {}
        }
    }

    private fun drawShapeLayer(
        drawScope: DrawScope,
        canvas: android.graphics.Canvas,
        layer: Layer.Shape,
        renderScale: Float = 1f
    ) {
        canvas.save()

        // Flip transformations
        val scaleH = if (layer.flipH) -1f else 1f
        val scaleV = if (layer.flipV) -1f else 1f
        if (scaleH != 1f || scaleV != 1f) {
            canvas.scale(scaleH, scaleV)
        }

        val scaledWidth = layer.width * renderScale
        val scaledHeight = layer.height * renderScale
        val scaledCornerRadius = layer.cornerRadius * renderScale
        val scaledStrokeWidth = layer.strokeWidth * renderScale

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            style = Paint.Style.FILL
            color = layer.fillColor
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            if (layer.shadowBlur > 0f || layer.shadowDx != 0f || layer.shadowDy != 0f) {
                setShadowLayer(
                    (layer.shadowBlur * renderScale).coerceAtLeast(1f),
                    layer.shadowDx * renderScale,
                    layer.shadowDy * renderScale,
                    layer.shadowColor
                )
            }
        }

        if (layer.isGradientEnabled && layer.gradientColors.size >= 2) {
            val shader = LinearGradient(
                -scaledWidth / 2f, -scaledHeight / 2f, scaledWidth / 2f, scaledHeight / 2f,
                layer.gradientColors.toIntArray(), null, Shader.TileMode.CLAMP
            )
            fillPaint.shader = shader
        }

        val strokePaint = if (scaledStrokeWidth > 0f) {
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = scaledStrokeWidth
                color = layer.strokeColor
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            }
        } else null

        val halfW = scaledWidth / 2f
        val halfH = scaledHeight / 2f

        when (layer.shapeType) {
            ShapeType.RECTANGLE -> {
                val rect = RectF(-halfW, -halfH, halfW, halfH)
                canvas.drawRoundRect(rect, scaledCornerRadius, scaledCornerRadius, fillPaint)
                strokePaint?.let { canvas.drawRoundRect(rect, scaledCornerRadius, scaledCornerRadius, it) }
            }
            ShapeType.CIRCLE -> {
                val radius = minOf(halfW, halfH)
                canvas.drawCircle(0f, 0f, radius, fillPaint)
                strokePaint?.let { canvas.drawCircle(0f, 0f, radius, it) }
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(0f, -halfH)
                    lineTo(halfW, halfH)
                    lineTo(-halfW, halfH)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            ShapeType.STAR -> {
                val path = createStarPath(halfW, halfH, 5)
                canvas.drawPath(path, fillPaint)
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            ShapeType.POLYGON -> {
                val path = createPolygonPath(minOf(halfW, halfH), layer.polygonSides)
                canvas.drawPath(path, fillPaint)
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            ShapeType.ARROW -> {
                val path = Path().apply {
                    moveTo(-halfW, -halfH / 3f)
                    lineTo(0f, -halfH / 3f)
                    lineTo(0f, -halfH)
                    lineTo(halfW, 0f)
                    lineTo(0f, halfH)
                    lineTo(0f, halfH / 3f)
                    lineTo(-halfW, halfH / 3f)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            ShapeType.HEART -> {
                val path = createHeartPath(halfW, halfH)
                canvas.drawPath(path, fillPaint)
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            ShapeType.RIBBON -> {
                val path = Path().apply {
                    moveTo(-halfW, -halfH)
                    lineTo(halfW, -halfH)
                    lineTo(halfW * 0.8f, 0f)
                    lineTo(halfW, halfH)
                    lineTo(-halfW, halfH)
                    lineTo(-halfW * 0.8f, 0f)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                strokePaint?.let { canvas.drawPath(path, it) }
            }
            ShapeType.BEZIER -> {
                val path = Path().apply {
                    moveTo(-halfW, halfH)
                    cubicTo(-halfW / 2f, -halfH, halfW / 2f, halfH * 1.5f, halfW, -halfH)
                }
                val bezierStrokePaint = Paint(fillPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 8f * renderScale
                }
                canvas.drawPath(path, bezierStrokePaint)
            }
        }
        canvas.restore()
    }

    private fun drawImageLayer(
        drawScope: DrawScope,
        canvas: android.graphics.Canvas,
        layer: Layer.Image,
        renderScale: Float = 1f
    ) {
        val baseBitmap = layer.bitmap ?: return

        canvas.save()
        val scaleX = if (layer.flipH) -1f else 1f
        val scaleY = if (layer.flipV) -1f else 1f
        canvas.scale(scaleX, scaleY)

        val processedBitmap = ImageFilterApplier.applyArtFilter(baseBitmap, layer.artFilter)

        val scaledWidth = layer.width * renderScale
        val scaledHeight = layer.height * renderScale
        val scaledCornerRadius = layer.cornerRadius * renderScale
        val scaledBorderWidth = layer.borderWidth * renderScale

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            colorFilter = ImageFilterApplier.createCombinedColorFilter(
                layer.brightness, layer.contrast, layer.saturation, layer.hue, layer.warmth,
                sepia = layer.sepia, grayscale = layer.grayscale, invert = layer.invert, exposure = layer.exposure
            )
            if (layer.shadowBlur > 0f) {
                setShadowLayer(layer.shadowBlur * renderScale, 4f * renderScale, 4f * renderScale, layer.shadowColor)
            }
        }

        val destRect = RectF(-scaledWidth / 2f, -scaledHeight / 2f, scaledWidth / 2f, scaledHeight / 2f)

        // Clip rounded corners if set
        if (scaledCornerRadius > 0f) {
            val clipPath = Path().apply {
                addRoundRect(destRect, scaledCornerRadius, scaledCornerRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
        }

        canvas.drawBitmap(processedBitmap, null, destRect, paint)

        // Border Stroke
        if (scaledBorderWidth > 0f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = scaledBorderWidth
                color = layer.borderColor
            }
            canvas.drawRoundRect(destRect, scaledCornerRadius, scaledCornerRadius, borderPaint)
        }

        // Overlay Effect (Lens flare, Light leak, Vignette)
        ImageFilterApplier.drawOverlayEffect(canvas, destRect, layer.overlayEffect)

        canvas.restore()
    }

    fun drawSingleStroke(
        canvas: android.graphics.Canvas,
        stroke: DrawingStroke,
        layerOpacity: Float = 1f,
        renderScale: Float = 1f
    ) {
        if (stroke.points.size < 2) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke.width * renderScale
            color = stroke.color
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            alpha = (stroke.alpha * layerOpacity * 255).toInt().coerceIn(0, 255)
        }

        val path = Path()
        path.moveTo(stroke.points[0].x * renderScale, stroke.points[0].y * renderScale)
        for (i in 1 until stroke.points.size) {
            path.lineTo(stroke.points[i].x * renderScale, stroke.points[i].y * renderScale)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawDrawingLayer(
        drawScope: DrawScope,
        canvas: android.graphics.Canvas,
        layer: Layer.Drawing,
        renderScale: Float = 1f
    ) {
        for (stroke in layer.strokes) {
            drawSingleStroke(canvas, stroke, layer.opacity, renderScale)
        }
    }

    fun getLayerCenter(layer: Layer): Pair<Float, Float> {
        return when (layer) {
            is Layer.Drawing -> {
                if (layer.strokes.isEmpty()) return Pair(layer.x, layer.y)
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                for (stroke in layer.strokes) {
                    for (pt in stroke.points) {
                        if (pt.x < minX) minX = pt.x
                        if (pt.y < minY) minY = pt.y
                        if (pt.x > maxX) maxX = pt.x
                        if (pt.y > maxY) maxY = pt.y
                    }
                }
                if (minX == Float.MAX_VALUE) return Pair(layer.x, layer.y)
                val localCX = (minX + maxX) / 2f * layer.scaleX
                val localCY = (minY + maxY) / 2f * layer.scaleY
                if (layer.rotation != 0f) {
                    val rad = layer.rotation * (Math.PI / 180.0)
                    val cosA = kotlin.math.cos(rad)
                    val sinA = kotlin.math.sin(rad)
                    val rx = (localCX * cosA - localCY * sinA).toFloat()
                    val ry = (localCX * sinA + localCY * cosA).toFloat()
                    Pair(layer.x + rx, layer.y + ry)
                } else {
                    Pair(layer.x + localCX, layer.y + localCY)
                }
            }
            else -> Pair(layer.x, layer.y)
        }
    }

    private fun drawSelectionBoundingBox(
        drawScope: DrawScope,
        layer: Layer,
        viewScale: Float = 1f
    ) {
        val (w, h) = getLayerDimensions(layer)
        val halfW = (w * layer.scaleX) / 2f + 10f
        val halfH = (h * layer.scaleY) / 2f + 10f

        val effectiveScale = viewScale.coerceAtLeast(0.2f)

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            val (centerX, centerY) = getLayerCenter(layer)
            nativeCanvas.translate(centerX, centerY)
            nativeCanvas.rotate(layer.rotation)

            val strokeWidthVal = (2.5f / effectiveScale).coerceIn(1f, 6f)
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthVal
                color = 0xFF8B5CF6.toInt() // Premium Violet accent color
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f / effectiveScale, 10f / effectiveScale), 0f)
            }
            val rect = RectF(-halfW, -halfH, halfW, halfH)
            nativeCanvas.drawRect(rect, boxPaint)

            // Draw Control Handles Paints
            val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = 0xFF8B5CF6.toInt()
            }
            val sideHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = 0xFF10B981.toInt() // Emerald green for side handles
            }
            val handleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (2f / effectiveScale).coerceIn(1f, 5f)
                color = 0xFFFFFFFF.toInt()
            }

            val handleRadius = (10f / effectiveScale).coerceIn(6f, 28f)
            val rotationStemLen = (32f / effectiveScale).coerceIn(16f, 80f)

            // Corner Handles (Scale)
            val cornerHandles = arrayOf(
                Pair(-halfW, -halfH), // Top-Left
                Pair(halfW, -halfH),  // Top-Right
                Pair(-halfW, halfH),  // Bottom-Left
                Pair(halfW, halfH)    // Bottom-Right
            )

            // Side Handles (Width/Height Adjust)
            val sidePillW = (14f / effectiveScale).coerceIn(8f, 36f)
            val sidePillH = (6f / effectiveScale).coerceIn(4f, 16f)

            // Draw Rotation Stem & Handle
            nativeCanvas.drawLine(0f, -halfH, 0f, -halfH - rotationStemLen, handleBorderPaint)
            nativeCanvas.drawCircle(0f, -halfH - rotationStemLen, handleRadius * 1.1f, handlePaint)
            nativeCanvas.drawCircle(0f, -halfH - rotationStemLen, handleRadius * 1.1f, handleBorderPaint)

            // Draw Corner Handles
            for (hPos in cornerHandles) {
                nativeCanvas.drawCircle(hPos.first, hPos.second, handleRadius, handlePaint)
                nativeCanvas.drawCircle(hPos.first, hPos.second, handleRadius, handleBorderPaint)
            }

            // Draw Side Handles (Left, Right, Top, Bottom)
            val sideHandles = arrayOf(
                Triple(-halfW, 0f, true),  // Left
                Triple(halfW, 0f, true),   // Right
                Triple(0f, -halfH, false), // Top
                Triple(0f, halfH, false)   // Bottom
            )

            for (s in sideHandles) {
                val cx = s.first
                val cy = s.second
                val isVertical = s.third
                val pillRect = if (isVertical) {
                    RectF(cx - sidePillH / 2f, cy - sidePillW / 2f, cx + sidePillH / 2f, cy + sidePillW / 2f)
                } else {
                    RectF(cx - sidePillW / 2f, cy - sidePillH / 2f, cx + sidePillW / 2f, cy + sidePillH / 2f)
                }
                nativeCanvas.drawRoundRect(pillRect, 6f, 6f, sideHandlePaint)
                nativeCanvas.drawRoundRect(pillRect, 6f, 6f, handleBorderPaint)
            }

            // Draw center anchor dot
            nativeCanvas.drawCircle(0f, 0f, (3.5f / effectiveScale).coerceIn(2f, 10f), handlePaint)

            nativeCanvas.restore()
        }
    }

    fun getLayerDimensions(layer: Layer): Pair<Float, Float> {
        return when (layer) {
            is Layer.Text -> {
                val layoutWidth = layer.boxWidth.toInt().coerceAtLeast(40)
                val processedText = when (layer.textTransform) {
                    1 -> layer.text.uppercase()
                    2 -> layer.text.lowercase()
                    3 -> layer.text.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                    else -> layer.text
                }
                val textPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = layer.fontSize
                    typeface = getTypeface(layer.fontPath, layer.fontFamilyName)
                    isFakeBoldText = layer.fontStyleBold
                    textSkewX = if (layer.fontStyleItalic) -0.25f else 0f
                    if (layer.letterSpacing != 0f) letterSpacing = layer.letterSpacing * 0.05f
                }
                val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.text.StaticLayout.Builder.obtain(processedText, 0, processedText.length, textPaint, layoutWidth)
                        .setLineSpacing(0f, layer.lineSpacing)
                        .setIncludePad(true)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    android.text.StaticLayout(processedText, textPaint, layoutWidth, android.text.Layout.Alignment.ALIGN_CENTER, layer.lineSpacing, 0f, true)
                }
                val layoutHeight = staticLayout.height.toFloat().coerceAtLeast(30f)
                Pair(layer.boxWidth + layer.backgroundPadding * 2, layoutHeight + layer.backgroundPadding * 2)
            }
            is Layer.Shape -> Pair(layer.width, layer.height)
            is Layer.Image -> Pair(layer.width.toFloat(), layer.height.toFloat())
            is Layer.Drawing -> {
                if (layer.strokes.isEmpty()) return Pair(200f, 200f)
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                var maxStrokeW = 10f
                for (stroke in layer.strokes) {
                    if (stroke.width > maxStrokeW) maxStrokeW = stroke.width
                    for (pt in stroke.points) {
                        if (pt.x < minX) minX = pt.x
                        if (pt.y < minY) minY = pt.y
                        if (pt.x > maxX) maxX = pt.x
                        if (pt.y > maxY) maxY = pt.y
                    }
                }
                if (minX == Float.MAX_VALUE) return Pair(200f, 200f)
                val w = (maxX - minX).coerceAtLeast(40f) + maxStrokeW
                val h = (maxY - minY).coerceAtLeast(40f) + maxStrokeW
                Pair(w, h)
            }
        }
    }

    private fun createStarPath(rOuter: Float, rInner: Float, points: Int): Path {
        val path = Path()
        val angleStep = PI / points
        var angle = -PI / 2.0

        path.moveTo((cos(angle) * rOuter).toFloat(), (sin(angle) * rOuter).toFloat())
        angle += angleStep

        for (i in 0 until points * 2 - 1) {
            val r = if (i % 2 == 0) rInner * 0.45f else rOuter
            path.lineTo((cos(angle) * r).toFloat(), (sin(angle) * r).toFloat())
            angle += angleStep
        }
        path.close()
        return path
    }

    private fun createPolygonPath(radius: Float, sides: Int): Path {
        val path = Path()
        val angleStep = (2 * PI) / sides
        var angle = -PI / 2.0

        path.moveTo((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
        for (i in 1 until sides) {
            angle += angleStep
            path.lineTo((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat())
        }
        path.close()
        return path
    }

    private fun createHeartPath(w: Float, h: Float): Path {
        val path = Path()
        path.moveTo(0f, h * 0.35f)
        path.cubicTo(-w * 0.5f, -h * 0.2f, -w * 0.8f, h * 0.4f, 0f, h)
        path.cubicTo(w * 0.8f, h * 0.4f, w * 0.5f, -h * 0.2f, 0f, h * 0.35f)
        path.close()
        return path
    }
}
