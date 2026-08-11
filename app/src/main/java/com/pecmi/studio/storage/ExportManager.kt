package com.pecmi.studio.storage

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import com.pecmi.studio.domain.model.CanvasSettings
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.graphics.CanvasRenderer
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat(val extension: String, val compressFormat: Bitmap.CompressFormat) {
    PNG("png", Bitmap.CompressFormat.PNG),
    JPG("jpg", Bitmap.CompressFormat.JPEG),
    WEBP("webp", if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP)
}

data class SaveResult(
    val uri: Uri?,
    val filePath: String,
    val success: Boolean,
    val errorMessage: String? = null
)

object ExportManager {

    fun renderCanvasToBitmap(
        width: Int,
        height: Int,
        scaleFactor: Float = 1f,
        settings: CanvasSettings,
        layers: List<Layer>,
        includeWatermark: Boolean = false
    ): Bitmap {
        val targetWidth = (width * scaleFactor).toInt().coerceAtLeast(1)
        val targetHeight = (height * scaleFactor).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val nativeCanvas = AndroidCanvas(bitmap)

        // Set High Quality Anti-Aliasing, Dithering and Filtering flags on native canvas
        nativeCanvas.drawFilter = android.graphics.PaintFlagsDrawFilter(
            0,
            Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG
        )

        val targetRect = android.graphics.RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())

        // Fill background directly at target canvas resolution
        if (!settings.isTransparent) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                color = settings.backgroundColor
                style = Paint.Style.FILL
            }
            nativeCanvas.drawRect(targetRect, bgPaint)

            if (settings.bgGradientColors != null && settings.bgGradientColors.size >= 2) {
                val bgGradPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = android.graphics.LinearGradient(
                        0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(),
                        settings.bgGradientColors.toIntArray(), null, android.graphics.Shader.TileMode.CLAMP
                    )
                }
                nativeCanvas.drawRect(targetRect, bgGradPaint)
            }
        }

        val canvasDrawScope = CanvasDrawScope()
        val composeCanvas = Canvas(nativeCanvas)

        canvasDrawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = androidx.compose.ui.geometry.Size(targetWidth.toFloat(), targetHeight.toFloat())
        ) {
            // Re-render each layer natively at full target size (no post-bitmap scaling)
            for (layer in layers) {
                CanvasRenderer.drawLayer(this, layer, isSelected = false, renderScale = scaleFactor)
            }
        }

        return bitmap
    }

    fun saveImageToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: ExportFormat,
        quality: Int = 100
    ): SaveResult {
        val fullFileName = "$filename.${format.extension}"
        val mimeType = when (format) {
            ExportFormat.PNG -> "image/png"
            ExportFormat.JPG -> "image/jpeg"
            ExportFormat.WEBP -> "image/webp"
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Pecmi")
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return SaveResult(null, "", false, "Failed to create MediaStore record")

                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(format.compressFormat, quality.coerceIn(10, 100), outputStream)
                }

                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                val displayPath = "Pictures/Pecmi/$fullFileName"
                return SaveResult(imageUri, displayPath, true)
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "Pecmi")
                if (!appDir.exists()) appDir.mkdirs()

                val file = File(appDir, fullFileName)
                FileOutputStream(file).use { stream ->
                    bitmap.compress(format.compressFormat, quality.coerceIn(10, 100), stream)
                    stream.flush()
                }

                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf(mimeType),
                    null
                )

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                return SaveResult(uri, file.absolutePath, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return SaveResult(null, "", false, e.localizedMessage ?: "Unknown save error")
        }
    }

    fun exportToFile(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: ExportFormat,
        quality: Int = 100
    ): File {
        val exportDir = File(context.cacheDir, "exported_images")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, "$filename.${format.extension}")
        val stream = FileOutputStream(file)
        bitmap.compress(format.compressFormat, quality.coerceIn(10, 100), stream)
        stream.flush()
        stream.close()
        return file
    }

    fun shareImageFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(shareIntent, "مشاركة الصورة عبر Pecmi").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openGalleryFolder(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
