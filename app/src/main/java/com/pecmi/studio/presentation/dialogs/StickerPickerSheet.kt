package com.pecmi.studio.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pecmi.studio.editor.CanvasViewModel
import com.pecmi.studio.storage.AppLanguage
import com.pecmi.studio.ui.language.LocalAppStrings
import com.pecmi.studio.ui.language.LocalAppStrings

data class StickerCategory(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val icon: String,
    val items: List<String>
)

object StickerLibraryData {
    val categories = listOf(
        StickerCategory("emoji", "إيموجي", "Emoji", "😊", listOf("😊", "😎", "🔥", "❤️", "🎉", "🚀", "💡", "🌟", "👑", "👍", "💖", "😍", "🥳", "🎯", "💎", "🦄", "⚡", "🏆", "🌈", "🎨", "🎁", "🌸", "🎈", "⭐", "🍕", "🍔")),
        StickerCategory("arrows", "أسهم", "Arrows", "➡️", listOf("⬅️", "➡️", "⬆️", "⬇️", "↗️", "↘️", "🔄", "↕️", "➔", "➜", "➽", "➹", "🞃", "🞁", "🏹", "➡", "➦", "➾", "↩️", "↪️", "↔️")),
        StickerCategory("geo", "أشكال هندسية", "Geometric", "📐", listOf("⏺️", "⏹️", "🔲", "🔺", "🔻", "🔷", "🔶", "🔘", "🟢", "🔴", "🟡", "🔵", "🟣", "⬛", "⬜", "🟨", "🟧", "🟥")),
        StickerCategory("frames", "إطارات", "Frames", "🖼️", listOf("🖼️", "🔲", "🔳", "📜", "🔖", "💳", "🏷️", "🧧", "🪞", "◽", "🏁", "🚩")),
        StickerCategory("light", "تأثيرات ضوئية", "Light Effects", "✨", listOf("✨", "🌟", "💥", "💫", "⚡", "☀️", "🌤️", "🕯️", "💡", "🔮", "🎆", "🎇", "🔆")),
        StickerCategory("lines", "خطوط وزخارف", "Lines", "✒️", listOf("〰️", "➖", "〽️", "✒️", "🖋️", "📜", "🌿", "🌸", "⚜️", "🪶", "🌀", "💠", "♾️")),
        StickerCategory("social", "تواصل اجتماعي", "Social", "💬", listOf("💬", "📱", "📧", "🌐", "📸", "📹", "🔔", "🎵", "🎧", "✉️", "📢", "📍", "🔗", "📻", "📡")),
        StickerCategory("business", "أعمال", "Business", "💼", listOf("💼", "📊", "📈", "💰", "💳", "🏷️", "🤝", "🎯", "🚀", "🏢", "📜", "🏆", "⚖️", "🧾", "💸")),
        StickerCategory("nature", "الطبيعة", "Nature", "🌿", listOf("🌱", "🌿", "☘️", "🍀", "🌲", "🌳", "🌴", "🌵", "🌾", "🌺", "🌻", "🌹", "🌷", "🌸", "🍃", "🍁", "🌊", "🍄")),
        StickerCategory("animals", "الحيوانات", "Animals", "🐱", listOf("🐱", "🐶", "🦁", "🐯", "🐻", "🦊", "🐰", "🐼", "🦅", "🦉", "🦋", "🐝", "🐬", "🦄", "🐺", "🐴")),
        StickerCategory("food", "الطعام", "Food", "🍕", listOf("🍕", "🍔", "🍟", "🌭", "🍿", "🍩", "🍦", "🍰", "☕", "🧃", "🍓", "🍎", "🍣", "🌮", "🍜", "🍹", "🥐")),
        StickerCategory("travel", "السفر", "Travel", "✈️", listOf("✈️", "🚗", "🚀", "⛵", "🧳", "🗺️", "🧭", "🏖️", "⛰️", "🏕️", "🗽", "🗼", "🏝️", "🎡")),
        StickerCategory("celebrations", "الاحتفالات", "Celebrations", "🎈", listOf("🎉", "🎊", "🎈", "🎂", "🎁", "🎆", "🎇", "🍾", "🥂", "🎗️", "🎭", "🎪")),
        StickerCategory("hearts", "القلوب", "Hearts", "❤️", listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "❣", "💕", "💞", "💓", "💗", "💖", "💘", "💝")),
        StickerCategory("stars", "النجوم", "Stars", "⭐", listOf("⭐", "🌟", "✨", "💫", "🌃", "🌠", "✴️", "✳️", "❇️", "🔯")),
        StickerCategory("flame", "اللهب", "Flame", "🔥", listOf("🔥", "💥", "🌋", "🧨", "⚡", "🕯️", "♨️")),
        StickerCategory("clouds", "السحب", "Clouds", "☁️", listOf("☁️", "⛅", "🌩️", "🌧️", "🌫️", "🌤️", "⛈️", "🌨️")),
        StickerCategory("bubbles", "الفقاعات", "Bubbles", "💬", listOf("💬", "💭", "🗯️", "🫧", "📢", "🗨️", "💡")),
        StickerCategory("pro_arrows", "أسهم احترافية", "Pro Arrows", "➔", listOf("➔", "➜", "➣", "➢", "➟", "➠", "➡", "➥", "➦", "➧", "➨", "➲", "➳")),
        StickerCategory("badges", "الشارات Badges", "Badges", "🏅", listOf("🏅", "🥇", "🥈", "🥉", "🎖️", "🎗️", "💮", "🔰", "🏷️", "🛡️", "🔒", "👑", "🏆")),
        StickerCategory("seals", "الأختام", "Seals", "🏵️", listOf("💮", "🏵️", "🏷️", "📜", "✒️", "🔏", "🔐", "🔒", "🖋️", "👑", "🔖")),
        StickerCategory("ui", "أيقونات UI", "UI Icons", "⚙️", listOf("🔍", "⚙️", "🏠", "👤", "🛒", "📂", "📌", "🗑️", "✏️", "🔒", "🔓", "➕", "➖", "❌", "✔️", "ℹ️", "❓", "❗")),
        StickerCategory("arabic", "ملصقات عربية", "Arabic", "﷽", listOf("﷽", "﷼", "ﷲ", "ﷻ", "🕌", "☪️", "🕋", "📿", "📖", "🌙", "☀️")),
        StickerCategory("ramadan", "ملصقات رمضان", "Ramadan", "🌙", listOf("🌙", "🕌", "🏮", "📿", "🕋", "🌟", "📜", "☕", "🌴")),
        StickerCategory("eid", "ملصقات العيد", "Eid", "🎁", listOf("🎈", "🎉", "🎁", "🐏", "🌙", "🍰", "🛍️", "🎆", "🍬", "🕌", "🐑"))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerSheet(
    viewModel: CanvasViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("emoji") }
    
    val favorites = remember { mutableStateListOf<String>() }
    val recentlyUsed = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("sticker_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.stickerLibrary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.close)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                placeholder = { Text(strings.searchStickers, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == "favs",
                        onClick = { selectedCategoryId = "favs" },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("${strings.favorites} (${favorites.size})", fontSize = 12.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategoryId == "recent",
                        onClick = { selectedCategoryId = "recent" },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(strings.recentlyUsed, fontSize = 12.sp) }
                    )
                }
                items(StickerLibraryData.categories) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id },
                        label = { Text("${category.icon} ${if (uiState.language == AppLanguage.ARABIC) category.nameAr else category.nameEn}", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display Items Grid
            val currentItems = when {
                searchQuery.isNotEmpty() -> {
                    StickerLibraryData.categories.flatMap { it.items }.filter { it.contains(searchQuery) }.distinct()
                }
                selectedCategoryId == "favs" -> favorites
                selectedCategoryId == "recent" -> recentlyUsed
                else -> StickerLibraryData.categories.find { it.id == selectedCategoryId }?.items ?: emptyList()
            }

            if (currentItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        strings.noStickersFound,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(currentItems) { sticker ->
                        val isFav = favorites.contains(sticker)
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!recentlyUsed.contains(sticker)) {
                                        recentlyUsed.add(0, sticker)
                                    }
                                    viewModel.addStickerTextLayer(sticker)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                sticker,
                                fontSize = 32.sp
                            )
                            IconButton(
                                onClick = {
                                    if (isFav) favorites.remove(sticker) else favorites.add(sticker)
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                            ) {
                                Icon(
                                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Fav",
                                    tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
