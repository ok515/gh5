package com.pecmi.studio.domain.model

enum class CanvasPreset(val displayName: String, val width: Int, val height: Int, val aspectRatio: String) {
    SQUARE_1_1("Square (1:1)", 1080, 1080, "1:1"),
    PORTRAIT_4_5("Post / Portrait (4:5)", 1080, 1350, "4:5"),
    STORY_9_16("Story / Reel (9:16)", 1080, 1920, "9:16"),
    LANDSCAPE_16_9("Landscape (16:9)", 1920, 1080, "16:9"),
    YOUTUBE_THUMBNAIL("YouTube Thumbnail", 1280, 720, "16:9"),
    FACEBOOK_COVER("Facebook Cover", 1200, 630, "1.91:1"),
    A4_DOCUMENT("A4 Document", 1240, 1754, "A4"),
    PROFILE_PIC("Profile Picture (800x800)", 800, 800, "1:1"),
    TWITTER_HEADER("Twitter / X Header", 1500, 500, "3:1"),
    CUSTOM("Custom Size", 1080, 1080, "Custom")
}

data class CanvasSettings(
    val width: Int = 1080,
    val height: Int = 1080,
    val preset: CanvasPreset = CanvasPreset.SQUARE_1_1,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val isTransparent: Boolean = false,
    val showGrid: Boolean = false,
    val gridSize: Int = 40,
    val showGuidelines: Boolean = false,
    val snapToGrid: Boolean = true,
    val zoomScale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,

    // Background Canvas Effects
    val bgVignette: Float = 0f,
    val bgNoise: Float = 0f,
    val bgBrightness: Float = 0f,
    val bgContrast: Float = 1f,
    val bgHue: Float = 0f,
    val bgGradientColors: List<Int>? = null
)
