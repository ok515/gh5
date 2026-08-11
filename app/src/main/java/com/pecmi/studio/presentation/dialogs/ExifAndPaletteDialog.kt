package com.pecmi.studio.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.domain.model.Layer

import com.pecmi.studio.ui.language.LocalAppStrings

@Composable
fun ExifAndPaletteDialog(
    imageLayer: Layer.Image,
    onSelectColor: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val bitmap = imageLayer.bitmap

    val extractedColors = remember(bitmap) {
        if (bitmap != null) {
            val w = bitmap.width
            val h = bitmap.height
            val samples = mutableListOf<Int>()
            for (x in listOf(w / 4, w / 2, (w * 3) / 4)) {
                for (y in listOf(h / 4, h / 2, (h * 3) / 4)) {
                    samples.add(bitmap.getPixel(x, y))
                }
            }
            samples.distinct()
        } else {
            listOf(0xFF1E293B.toInt(), 0xFF3B82F6.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(), 0xFFEF4444.toInt())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.imageInfoTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${strings.dimensions}: ${imageLayer.width} x ${imageLayer.height} px", fontSize = 12.sp)
                        Text("Aspect Ratio: %.2f".format(imageLayer.width.toFloat() / imageLayer.height.coerceAtLeast(1)), fontSize = 12.sp)
                        Text("Format: ARGB_8888 (High Quality)", fontSize = 12.sp)
                        Text("${strings.opacity}: ${(imageLayer.opacity * 100).toInt()}%", fontSize = 12.sp)
                    }
                }

                Text(strings.extractedPalette, style = MaterialTheme.typography.titleSmall)
                Text(strings.tapSwatchToCopy, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(extractedColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(1.dp, Color.White, CircleShape)
                                .clickable {
                                    onSelectColor(color)
                                    onDismiss()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}
