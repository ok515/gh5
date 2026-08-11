package com.pecmi.studio.presentation.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.editor.CanvasViewModel
import java.io.File
import java.io.FileOutputStream

import com.pecmi.studio.ui.language.LocalAppStrings

data class FontItem(
    val name: String,
    val category: String,
    val path: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontPickerSheet(
    viewModel: CanvasViewModel,
    selectedLayer: Layer.Text,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val defaultFonts = remember {
        listOf(
            FontItem("Default", "Sans"),
            FontItem("Sans", "Sans"),
            FontItem("Serif", "Serif"),
            FontItem("Monospace", "Monospace"),
            FontItem("Cursive", "Handwriting"),
            FontItem("Cairo Arabic", "Arabic"),
            FontItem("Amiri Calligraphy", "Arabic"),
            FontItem("Tajawal Modern", "Arabic"),
            FontItem("Playfair Display", "Serif"),
            FontItem("Montserrat Bold", "Sans"),
            FontItem("Pacifico Script", "Handwriting"),
            FontItem("Bebas Neue", "Display")
        )
    }

    var customFonts by remember { mutableStateOf<List<FontItem>>(emptyList()) }

    val fontImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = "custom_font_${System.currentTimeMillis()}.ttf"
                val destFile = File(context.filesDir, fileName)
                FileOutputStream(destFile).use { output ->
                    inputStream?.copyTo(output)
                }
                val newItem = FontItem(name = "Custom Font ${customFonts.size + 1}", category = "Custom", path = destFile.absolutePath)
                customFonts = customFonts + newItem
                viewModel.updateLayer(selectedLayer.copy(fontFamilyName = newItem.name, fontPath = newItem.path))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val categories = listOf("All", "Sans", "Serif", "Arabic", "Handwriting", "Display", "Custom")

    val allFonts = (defaultFonts + customFonts).filter { font ->
        (selectedCategory == "All" || font.category.equals(selectedCategory, ignoreCase = true)) &&
                font.name.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.fontStudio, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { fontImportLauncher.launch("*/*") }) {
                    Icon(Icons.Default.Add, contentDescription = strings.importFont)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.searchFonts) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(allFonts) { font ->
                    val isSelected = selectedLayer.fontFamilyName == font.name
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateLayer(
                                    selectedLayer.copy(fontFamilyName = font.name, fontPath = font.path)
                                )
                                onDismiss()
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val previewFamily = when (font.name.lowercase()) {
                                    "serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                    "monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                    "cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
                                    else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                }
                                Text(
                                    font.name,
                                    fontSize = 17.sp,
                                    fontFamily = previewFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text("توليد نص - Sample Text", fontSize = 13.sp, fontFamily = previewFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
