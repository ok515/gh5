package com.pecmi.studio.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.pecmi.studio.domain.model.ImageArtFilter
import com.pecmi.studio.domain.model.ImageOverlayEffect
import kotlin.math.abs

object ImageFilterApplier {

    fun createCombinedColorFilter(
        brightness: Float, // -100 to 100
        contrast: Float,   // 0 to 3
        saturation: Float, // 0 to 3
        hue: Float,        // -180 to 180
        warmth: Float,     // -100 to 100
        sepia: Float = 0f,  // 0 to 1
        grayscale: Float = 0f, // 0 to 1
        invert: Boolean = false,
        exposure: Float = 0f // -2 to 2
    ): ColorMatrixColorFilter {
        val cm = ColorMatrix()

        // Saturation
        cm.setSaturation(saturation.coerceIn(0f, 3f))

        // Contrast
        val contrastMatrix = ColorMatrix().apply {
            val scale = contrast.coerceIn(0f, 3f)
            val translate = (-0.5f * scale + 0.5f) * 255f
            set(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        cm.postConcat(contrastMatrix)

        // Brightness & Exposure
        val expFactor = if (exposure != 0f) Math.pow(2.0, exposure.toDouble()).toFloat() else 1f
        val b = (brightness.coerceIn(-100f, 100f) * 2.55f)
        val brightnessMatrix = ColorMatrix().apply {
            set(
                floatArrayOf(
                    expFactor, 0f, 0f, 0f, b,
                    0f, expFactor, 0f, 0f, b,
                    0f, 0f, expFactor, 0f, b,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        cm.postConcat(brightnessMatrix)

        // Warmth (Temperature)
        if (warmth != 0f) {
            val w = warmth.coerceIn(-100f, 100f) * 0.5f
            val warmthMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, w,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, -w,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            cm.postConcat(warmthMatrix)
        }

        // Sepia
        if (sepia > 0f) {
            val s = sepia.coerceIn(0f, 1f)
            val invS = 1f - s
            val sepiaMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        invS + s * 0.393f, s * 0.769f, s * 0.189f, 0f, 0f,
                        s * 0.349f, invS + s * 0.686f, s * 0.168f, 0f, 0f,
                        s * 0.272f, s * 0.534f, invS + s * 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            cm.postConcat(sepiaMatrix)
        }

        // Grayscale
        if (grayscale > 0f) {
            val g = grayscale.coerceIn(0f, 1f)
            val invG = 1f - g
            val grayMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        invG + g * 0.299f, g * 0.587f, g * 0.114f, 0f, 0f,
                        g * 0.299f, invG + g * 0.587f, g * 0.114f, 0f, 0f,
                        g * 0.299f, g * 0.587f, invG + g * 0.114f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            cm.postConcat(grayMatrix)
        }

        // Invert
        if (invert) {
            val invertMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            cm.postConcat(invertMatrix)
        }

        return ColorMatrixColorFilter(cm)
    }

    private val filterCache = object : android.util.LruCache<Pair<Bitmap, ImageArtFilter>, Bitmap>(16) {
        override fun entryRemoved(evicted: Boolean, key: Pair<Bitmap, ImageArtFilter>?, oldValue: Bitmap?, newValue: Bitmap?) {
            if (evicted && oldValue != null && !oldValue.isRecycled && oldValue != key?.first) {
                try { oldValue.recycle() } catch (e: Exception) {}
            }
        }
    }

    fun applyArtFilter(src: Bitmap, artFilter: ImageArtFilter): Bitmap {
        if (artFilter == ImageArtFilter.NONE) return src

        val cacheKey = Pair(src, artFilter)
        filterCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        when (artFilter) {
            ImageArtFilter.CARTOON -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    val cm = ColorMatrix().apply {
                        setSaturation(1.8f)
                    }
                    colorFilter = ColorMatrixColorFilter(cm)
                }
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            ImageArtFilter.SKETCH, ImageArtFilter.PENCIL -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    val cm = ColorMatrix().apply {
                        setSaturation(0f)
                        // high contrast black & white
                        val contrast = 2.5f
                        val translate = (-0.5f * contrast + 0.5f) * 255f
                        set(
                            floatArrayOf(
                                contrast, 0f, 0f, 0f, translate,
                                0f, contrast, 0f, 0f, translate,
                                0f, 0f, contrast, 0f, translate,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                    }
                    colorFilter = ColorMatrixColorFilter(cm)
                }
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            ImageArtFilter.PIXELATE, ImageArtFilter.MOSAIC -> {
                val sampleSize = (width / 32).coerceAtLeast(8)
                val scaled = Bitmap.createScaledBitmap(src, (width / sampleSize).coerceAtLeast(1), (height / sampleSize).coerceAtLeast(1), false)
                val pixelated = Bitmap.createScaledBitmap(scaled, width, height, false)
                canvas.drawBitmap(pixelated, 0f, 0f, null)
            }
            ImageArtFilter.HDR -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    val cm = ColorMatrix().apply {
                        setSaturation(1.5f)
                    }
                    colorFilter = ColorMatrixColorFilter(cm)
                }
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            ImageArtFilter.BLOOM, ImageArtFilter.OIL_PAINT -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    val cm = ColorMatrix().apply { setSaturation(1.3f) }
                    colorFilter = ColorMatrixColorFilter(cm)
                }
                canvas.drawBitmap(src, 0f, 0f, paint)
            }
            else -> canvas.drawBitmap(src, 0f, 0f, null)
        }

        filterCache.put(cacheKey, output)
        return output
    }

    fun drawOverlayEffect(canvas: Canvas, rect: RectF, overlay: ImageOverlayEffect) {
        if (overlay == ImageOverlayEffect.NONE) return

        when (overlay) {
            ImageOverlayEffect.LENS_FLARE -> {
                val cx = rect.left + rect.width() * 0.3f
                val cy = rect.top + rect.height() * 0.3f
                val radius = rect.width().coerceAtLeast(rect.height()) * 0.6f
                val gradient = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(0x80FFFFFF.toInt(), 0x40FFD600.toInt(), 0x20FF1744.toInt(), 0x00000000),
                    floatArrayOf(0f, 0.3f, 0.6f, 1f),
                    Shader.TileMode.CLAMP
                )
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
                canvas.drawRect(rect, paint)
            }
            ImageOverlayEffect.LIGHT_LEAK -> {
                val gradient = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(0x90FF6D00.toInt(), 0x40FFD600.toInt(), 0x00000000),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
                canvas.drawRect(rect, paint)
            }
            ImageOverlayEffect.BOKEH -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                    color = 0x40FFFFFF.toInt()
                    style = Paint.Style.FILL
                }
                val minDim = minOf(rect.width(), rect.height())
                canvas.drawCircle(rect.centerX() - minDim * 0.1f, rect.centerY() - minDim * 0.15f, minDim * 0.2f, paint)
                canvas.drawCircle(rect.centerX() + minDim * 0.15f, rect.centerY() + minDim * 0.1f, minDim * 0.28f, paint)
                canvas.drawCircle(rect.centerX() - minDim * 0.22f, rect.centerY() + minDim * 0.2f, minDim * 0.15f, paint)
            }
            ImageOverlayEffect.VIGNETTE_DARK -> {
                val radius = rect.width().coerceAtLeast(rect.height()) * 0.7f
                val gradient = RadialGradient(
                    rect.centerX(), rect.centerY(), radius,
                    intArrayOf(0x00000000, 0xAA000000.toInt()),
                    floatArrayOf(0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
                canvas.drawRect(rect, paint)
            }
            else -> {}
        }
    }
}
