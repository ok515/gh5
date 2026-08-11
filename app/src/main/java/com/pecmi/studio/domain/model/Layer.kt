package com.pecmi.studio.domain.model

import android.graphics.Bitmap
import androidx.compose.ui.graphics.BlendMode

enum class LayerType {
    TEXT, IMAGE, SHAPE, DRAWING, STICKER
}

enum class ShapeType {
    RECTANGLE, CIRCLE, TRIANGLE, STAR, POLYGON, ARROW, HEART, RIBBON, BEZIER
}

enum class CanvasBlendMode {
    NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN, COLOR_DODGE, COLOR_BURN, SOFT_LIGHT, HARD_LIGHT, DIFFERENCE, EXCLUSION, PLUS;

    fun toComposeBlendMode(): BlendMode {
        return when (this) {
            NORMAL -> BlendMode.SrcOver
            MULTIPLY -> BlendMode.Multiply
            SCREEN -> BlendMode.Screen
            OVERLAY -> BlendMode.Overlay
            DARKEN -> BlendMode.Darken
            LIGHTEN -> BlendMode.Lighten
            COLOR_DODGE -> BlendMode.ColorDodge
            COLOR_BURN -> BlendMode.ColorBurn
            SOFT_LIGHT -> BlendMode.Softlight
            HARD_LIGHT -> BlendMode.Hardlight
            DIFFERENCE -> BlendMode.Difference
            EXCLUSION -> BlendMode.Exclusion
            PLUS -> BlendMode.Plus
        }
    }
}

enum class TextPreset {
    NONE, NEON, GOLD, SILVER, GLASS, CHROME, EMBOSS, POP3D
}

enum class ImageArtFilter {
    NONE, OIL_PAINT, SKETCH, CARTOON, PENCIL, PIXELATE, MOSAIC, HDR, BLOOM
}

enum class ImageBlurType {
    NONE, GAUSSIAN, MOTION, RADIAL
}

enum class ImageOverlayEffect {
    NONE, LENS_FLARE, LIGHT_LEAK, BOKEH, VIGNETTE_DARK, DUST_TEXTURE
}

data class DrawingPoint(val x: Float, val y: Float)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val color: Int,
    val width: Float,
    val alpha: Float = 1f,
    val isEraser: Boolean = false
)

sealed class Layer(
    open val id: String,
    open val name: String,
    open val type: LayerType,
    open val x: Float = 0f,
    open val y: Float = 0f,
    open val scaleX: Float = 1f,
    open val scaleY: Float = 1f,
    open val rotation: Float = 0f,
    open val opacity: Float = 1f,
    open val isVisible: Boolean = true,
    open val isLocked: Boolean = false,
    open val blendMode: CanvasBlendMode = CanvasBlendMode.NORMAL
) {
    data class Text(
        override val id: String,
        override val name: String = "Text",
        override val x: Float = 200f,
        override val y: Float = 300f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
        override val rotation: Float = 0f,
        override val opacity: Float = 1f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val blendMode: CanvasBlendMode = CanvasBlendMode.NORMAL,

        val text: String = "Double tap to edit",
        val textColor: Int = 0xFFFFFFFF.toInt(),
        val fontSize: Float = 48f,
        val fontFamilyName: String = "Default",
        val fontPath: String? = null, // Custom imported TTF/OTF file path
        val fontStyleBold: Boolean = false,
        val fontStyleItalic: Boolean = false,
        val underline: Boolean = false,
        val strikethrough: Boolean = false,
        val textTransform: Int = 0, // 0: None, 1: UPPERCASE, 2: lowercase, 3: Capitalize
        val alignment: Int = 1, // 0: Left, 1: Center, 2: Right
        val boxWidth: Float = 360f, // Width of bounding text box for word wrapping
        val letterSpacing: Float = 0f,
        val wordSpacing: Float = 0f,
        val lineSpacing: Float = 1f,

        val strokeWidth: Float = 0f,
        val strokeColor: Int = 0xFF000000.toInt(),
        val shadowBlur: Float = 0f,
        val shadowDx: Float = 4f,
        val shadowDy: Float = 4f,
        val shadowColor: Int = 0x80000000.toInt(),
        val innerShadowBlur: Float = 0f,
        val innerShadowDx: Float = 2f,
        val innerShadowDy: Float = 2f,
        val innerShadowColor: Int = 0x80000000.toInt(),
        val glowRadius: Float = 0f,
        val glowColor: Int = 0x8000FFFF.toInt(),

        val isCurved: Boolean = false,
        val curveRadius: Float = 120f,
        val rotX: Float = 0f,
        val rotY: Float = 0f,
        val skewX: Float = 0f,
        val reflectionEnabled: Boolean = false,
        val flipH: Boolean = false,
        val flipV: Boolean = false,

        val hasBackground: Boolean = false,
        val backgroundColor: Int = 0xCC000000.toInt(),
        val backgroundCornerRadius: Float = 16f,
        val backgroundPadding: Float = 16f,
        val bgStrokeWidth: Float = 0f,
        val bgStrokeColor: Int = 0xFFFFFFFF.toInt(),

        val isGradientEnabled: Boolean = false,
        val gradientColors: List<Int> = listOf(0xFF00E5FF.toInt(), 0xFF7C4DFF.toInt()),
        val presetStyle: TextPreset = TextPreset.NONE
    ) : Layer(id, name, LayerType.TEXT, x, y, scaleX, scaleY, rotation, opacity, isVisible, isLocked, blendMode)

    data class Image(
        override val id: String,
        override val name: String = "Image",
        override val x: Float = 100f,
        override val y: Float = 100f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
        override val rotation: Float = 0f,
        override val opacity: Float = 1f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val blendMode: CanvasBlendMode = CanvasBlendMode.NORMAL,

        val imagePath: String? = null,
        @Transient val bitmap: Bitmap? = null,
        val width: Int = 400,
        val height: Int = 400,
        val flipH: Boolean = false,
        val flipV: Boolean = false,

        val skewX: Float = 0f,
        val skewY: Float = 0f,
        val rotX: Float = 0f,
        val rotY: Float = 0f,

        val brightness: Float = 0f, // -100 to 100
        val contrast: Float = 1f,   // 0 to 3
        val saturation: Float = 1f, // 0 to 3
        val hue: Float = 0f,        // -180 to 180
        val warmth: Float = 0f,     // -100 to 100
        val exposure: Float = 0f,   // -2 to 2
        val gamma: Float = 1f,      // 0.2 to 3
        val vibrance: Float = 0f,   // -100 to 100
        val sepia: Float = 0f,      // 0 to 1
        val grayscale: Float = 0f,  // 0 to 1
        val invert: Boolean = false,
        val threshold: Float = -1f, // -1 disabled, 0 to 255
        val blur: Float = 0f,       // 0 to 25
        val blurType: ImageBlurType = ImageBlurType.NONE,
        val sharpen: Float = 0f,    // 0 to 10
        val vignette: Float = 0f,   // 0 to 1

        val artFilter: ImageArtFilter = ImageArtFilter.NONE,
        val overlayEffect: ImageOverlayEffect = ImageOverlayEffect.NONE,

        val cornerRadius: Float = 0f,
        val borderWidth: Float = 0f,
        val borderColor: Int = 0xFFFFFFFF.toInt(),
        val shadowBlur: Float = 0f,
        val shadowColor: Int = 0x80000000.toInt()
    ) : Layer(id, name, LayerType.IMAGE, x, y, scaleX, scaleY, rotation, opacity, isVisible, isLocked, blendMode)

    data class Shape(
        override val id: String,
        override val name: String = "Shape",
        override val x: Float = 150f,
        override val y: Float = 150f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
        override val rotation: Float = 0f,
        override val opacity: Float = 1f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val blendMode: CanvasBlendMode = CanvasBlendMode.NORMAL,

        val shapeType: ShapeType = ShapeType.RECTANGLE,
        val width: Float = 300f,
        val height: Float = 300f,
        val fillColor: Int = 0xFF3B82F6.toInt(),
        val strokeColor: Int = 0xFFFFFFFF.toInt(),
        val strokeWidth: Float = 0f,
        val cornerRadius: Float = 24f,
        val polygonSides: Int = 5,
        val flipH: Boolean = false,
        val flipV: Boolean = false,
        val shadowBlur: Float = 0f,
        val shadowColor: Int = 0x80000000.toInt(),
        val shadowDx: Float = 0f,
        val shadowDy: Float = 0f,
        val isGradientEnabled: Boolean = false,
        val gradientColors: List<Int> = listOf(0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt())
    ) : Layer(id, name, LayerType.SHAPE, x, y, scaleX, scaleY, rotation, opacity, isVisible, isLocked, blendMode)

    data class Drawing(
        override val id: String,
        override val name: String = "Drawing",
        override val x: Float = 0f,
        override val y: Float = 0f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
        override val rotation: Float = 0f,
        override val opacity: Float = 1f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
        override val blendMode: CanvasBlendMode = CanvasBlendMode.NORMAL,

        val strokes: List<DrawingStroke> = emptyList()
    ) : Layer(id, name, LayerType.DRAWING, x, y, scaleX, scaleY, rotation, opacity, isVisible, isLocked, blendMode)
}
